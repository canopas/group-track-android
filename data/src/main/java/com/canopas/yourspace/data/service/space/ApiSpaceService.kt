package com.canopas.yourspace.data.service.space

import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.ApiSpaceMember
import com.canopas.yourspace.data.models.space.EncryptedDistribution
import com.canopas.yourspace.data.models.space.GroupKeysDoc
import com.canopas.yourspace.data.models.space.MemberKeyData
import com.canopas.yourspace.data.models.space.SPACE_MEMBER_ROLE_ADMIN
import com.canopas.yourspace.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_GROUP_KEYS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiSpaceService @Inject constructor(
    private val db: FirebaseFirestore,
    private val authService: AuthService,
    private val apiUserService: ApiUserService,
    private val placeService: ApiPlaceService,
    private val bufferedSenderKeyStore: BufferedSenderKeyStore
) {
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceGroupKeysDoc(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)
            .document(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    suspend fun createSpace(spaceName: String): String {
        val spaceId = UUID.randomUUID().toString()
        val docRef = spaceRef.document(spaceId)
        val userId = authService.currentUser?.id ?: ""

        val space = ApiSpace(
            id = spaceId,
            name = spaceName,
            admin_id = userId
        )
        docRef.set(space).await()

        // Initialize the single group_keys doc to a default structure:
        val emptyGroupKeys = GroupKeysDoc()
        spaceGroupKeysDoc(spaceId).set(emptyGroupKeys).await()

        joinSpace(spaceId, SPACE_MEMBER_ROLE_ADMIN)
        return spaceId
    }

    private suspend fun getSpaceMembers(spaceId: String): List<ApiSpaceMember> {
        return spaceMemberRef(spaceId).get().await().toObjects(ApiSpaceMember::class.java)
    }

    suspend fun joinSpace(spaceId: String, role: Int = SPACE_MEMBER_ROLE_MEMBER) {
        val user = authService.currentUser ?: return
        spaceMemberRef(spaceId)
            .document(user.id).also {
                val member = ApiSpaceMember(
                    space_id = spaceId,
                    user_id = user.id,
                    role = role,
                    identity_key_public = user.identity_key_public,
                    location_enabled = true
                )
                it.set(member).await()
            }

        apiUserService.addSpaceId(user.id, spaceId)

        // Update the "docUpdatedAt" so others see membership changed
        val docRef = spaceGroupKeysDoc(spaceId)
        docRef.update("docUpdatedAt", System.currentTimeMillis()).await()

        // Distribute sender key to all members
        distributeSenderKeyToSpaceMembers(spaceId, user.id)
    }

    /**
     * Create a sender key distribution for the current/joining user, and encrypt the distribution key
     * for each member using their public key(ECDH).
     **/
    private suspend fun distributeSenderKeyToSpaceMembers(spaceId: String, senderUserId: String) {
        val deviceId = UUID.randomUUID().mostSignificantBits and 0x7FFFFFFF
        val groupAddress = SignalProtocolAddress(spaceId, deviceId.toInt())
        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        val distributionMessage = sessionBuilder.create(groupAddress, UUID.randomUUID())
        val distributionBytes = distributionMessage.serialize()

        val apiSpaceMembers = getSpaceMembers(spaceId)
        val distributions = mutableListOf<EncryptedDistribution>()

        for (member in apiSpaceMembers) {
            val publicBlob = member.identity_key_public ?: continue
            val publicKey = try {
                val publicKeyBytes = publicBlob.toBytes()
                if (publicKeyBytes.size != 33) { // Expected size for compressed EC public key
                    Timber.e("Invalid public key size for member ${member.user_id}")
                    continue
                }
                Curve.decodePoint(publicKeyBytes, 0)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode public key for member ${member.user_id}")
                continue
            }

            // Encrypt distribution using member's public key
            distributions.add(EphemeralECDHUtils.encrypt(member.user_id, distributionBytes, publicKey))
        }

        db.runTransaction { transaction ->
            val docRef = spaceGroupKeysDoc(spaceId)
            val snapshot = transaction.get(docRef)
            val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: GroupKeysDoc()
            val oldMemberKeyData = groupKeysDoc.memberKeys[senderUserId] ?: MemberKeyData()
            val newMemberKeyData = oldMemberKeyData.copy(
                memberDeviceId = deviceId.toInt(),
                distributions = distributions,
                dataUpdatedAt = System.currentTimeMillis()
            )
            val updates = mapOf(
                "memberKeys.$senderUserId" to newMemberKeyData,
                "docUpdatedAt" to System.currentTimeMillis()
            )
            transaction.update(docRef, updates)
        }.await()
        Timber.d("Sender key distribution updated for $senderUserId in space $spaceId")
    }

    suspend fun enableLocation(spaceId: String, userId: String, enable: Boolean) {
        spaceMemberRef(spaceId)
            .whereEqualTo("user_id", userId).get()
            .await().documents.firstOrNull()
            ?.reference?.update("location_enabled", enable)?.await()
    }

    suspend fun isMember(spaceId: String, userId: String): Boolean {
        val query = spaceMemberRef(spaceId)
            .whereEqualTo("user_id", userId)
        val result = query.get().await()
        return result.documents.isNotEmpty()
    }

    suspend fun getSpace(spaceId: String) =
        spaceRef.document(spaceId).get().await().toObject(ApiSpace::class.java)

    fun getSpaceFlow(spaceId: String) =
        spaceRef.document(spaceId).snapshotFlow(ApiSpace::class.java)

    fun getSpaceMemberByUserId(userId: String) =
        db.collectionGroup(FIRESTORE_COLLECTION_SPACE_MEMBERS).whereEqualTo("user_id", userId)
            .snapshotFlow(ApiSpaceMember::class.java)

    fun getMemberBySpaceId(spaceId: String) =
        spaceMemberRef(spaceId).snapshotFlow(ApiSpaceMember::class.java)

    private suspend fun deleteMembers(spaceId: String) {
        spaceMemberRef(spaceId).get().await().documents.forEach { doc ->
            doc.reference.delete().await()
        }
    }

    suspend fun deleteSpace(spaceId: String) {
        deleteMembers(spaceId)
        spaceRef.document(spaceId).delete().await()
    }

    suspend fun removeUserFromSpace(spaceId: String, userId: String) {
        placeService.removedUserFromExistingPlaces(spaceId, userId)
        spaceMemberRef(spaceId)
            .whereEqualTo("user_id", userId).get().await().documents.forEach {
                it.reference.delete().await()
            }
    }

    suspend fun updateSpace(space: ApiSpace) {
        spaceRef.document(space.id).set(space).await()
    }

    suspend fun changeAdmin(spaceId: String, newAdminId: String) {
        try {
            val spaceRef = spaceRef.document(spaceId)
            spaceRef.update("admin_id", newAdminId).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to change admin")
            throw e
        }
    }

    suspend fun generateAndDistributeSenderKeysForExistingSpaces(spaceIds: List<String>) {
        val userId = authService.currentUser?.id ?: return
        spaceIds.forEach { spaceId ->
            try {
                val emptyGroupKeys = GroupKeysDoc()
                spaceGroupKeysDoc(spaceId).set(emptyGroupKeys).await()
                distributeSenderKeyToSpaceMembers(spaceId, userId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to distribute sender key for space $spaceId")
            }
        }
    }

    suspend fun rotateSenderKey(spaceId: String) {
        val user = authService.currentUser ?: return
        val deviceId = UUID.randomUUID().mostSignificantBits.toInt()

        val groupAddress = SignalProtocolAddress(spaceId, deviceId)
        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        val newDistributionMessage = sessionBuilder.create(groupAddress, UUID.randomUUID())
        val newDistributionBytes = newDistributionMessage.serialize()

        val apiSpaceMembers = getSpaceMembers(spaceId)
        val memberIds = apiSpaceMembers.map { it.user_id }.toSet()
        val newDistributions = mutableListOf<EncryptedDistribution>()

        for (member in apiSpaceMembers) {
            val publicBlob = member.identity_key_public ?: continue
            val publicKey = Curve.decodePoint(publicBlob.toBytes(), 0)

            newDistributions.add(EphemeralECDHUtils.encrypt(member.user_id, newDistributionBytes, publicKey))
        }

        db.runTransaction { transaction ->
            val docRef = spaceGroupKeysDoc(spaceId)
            val snapshot = transaction.get(docRef)
            val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: GroupKeysDoc()

            val oldKeyData = groupKeysDoc.memberKeys[user.id] ?: MemberKeyData()

            // Filter out distributions for members who are no longer in the space
            val filteredOldDistributions = oldKeyData.distributions.filter { it.recipientId in memberIds }

            val rotatedKeyData = oldKeyData.copy(
                memberDeviceId = deviceId,
                distributions = newDistributions + filteredOldDistributions,
                dataUpdatedAt = System.currentTimeMillis()
            )

            val updates = mapOf(
                "memberKeys.${user.id}" to rotatedKeyData,
                "docUpdatedAt" to System.currentTimeMillis()
            )

            transaction.update(docRef, updates)
        }.await()

        Timber.d("Key rotation completed for space: $spaceId")
    }

    fun getUserSpacesToRotateKeys(): List<String> {
        val user = authService.currentUser ?: return emptyList()
        if (user.identity_key_public == null) {
            Timber.e("User identity key public is null, can't rotate keys")
            return emptyList()
        }
        return user.space_ids ?: emptyList()
    }
}
