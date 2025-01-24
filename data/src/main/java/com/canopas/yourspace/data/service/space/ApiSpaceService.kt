package com.canopas.yourspace.data.service.space

import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.ApiSpaceMember
import com.canopas.yourspace.data.models.space.EncryptedDistribution
import com.canopas.yourspace.data.models.space.GroupKeysDoc
import com.canopas.yourspace.data.models.space.MemberKeyData
import com.canopas.yourspace.data.models.space.SPACE_MEMBER_ROLE_ADMIN
import com.canopas.yourspace.data.models.space.SPACE_MEMBER_ROLE_MEMBER
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.toBytes
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_GROUP_KEYS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FieldValue
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

    suspend fun createSpace(spaceName: String, enableEncryption: Boolean = false): String {
        val spaceId = UUID.randomUUID().toString()
        val docRef = spaceRef.document(spaceId)
        val currentUser = authService.currentUser ?: throw IllegalStateException("No authenticated user")

        if (enableEncryption && currentUser.isFreeUser) {
            // Redirect to subscription page
            throw IllegalStateException("User must be a premium user to enable encryption")
        }

        val space = ApiSpace(
            id = spaceId,
            name = spaceName,
            admin_id = currentUser.id
        )
        docRef.set(space).await()

        if (enableEncryption && currentUser.isPremiumUser) {
            // Initialize the single group_keys doc to a default structure:
            val emptyGroupKeys = GroupKeysDoc()
            spaceGroupKeysDoc(spaceId).set(emptyGroupKeys).await()
        }

        joinSpace(spaceId, SPACE_MEMBER_ROLE_ADMIN, enableEncryption = enableEncryption)
        return spaceId
    }

    private suspend fun getSpaceMembers(spaceId: String): List<ApiSpaceMember> {
        return spaceMemberRef(spaceId).get().await().toObjects(ApiSpaceMember::class.java)
    }

    suspend fun joinSpace(spaceId: String, role: Int = SPACE_MEMBER_ROLE_MEMBER, enableEncryption: Boolean? = null) {
        val user = authService.currentUser ?: throw IllegalStateException("No authenticated user")
        val isEncryptionEnabled = enableEncryption ?: kotlin.run {
            getSpace(spaceId)?.is_encryption_enabled ?: throw IllegalStateException("Space not found")
        }

        when {
            isEncryptionEnabled && user.isFreeUser -> {
                // Redirect to subscription page
                throw SubscriptionRequiredException("User must be a premium user to join an encrypted space")
            }
            !isEncryptionEnabled && user.isPremiumUser -> {
                // Notify/Alert the user that they are joining an unencrypted space
                // and their locations will be shared unencrypted
                // On click of continue button, proceed to join the space and if user wants to leave any unencrypted space
                // then they can do so from the settings page or help them navigate to the settings page
                // Join the space and without distributing sender key

                joinSpace(spaceId, user, role)
            }
            isEncryptionEnabled && user.isPremiumUser -> {
                // Join the space and distribute sender key
                joinSpace(spaceId, user, role)

                // Update the "docUpdatedAt" so others see membership changed
                val docRef = spaceGroupKeysDoc(spaceId)
                docRef.update("doc_updated_at", System.currentTimeMillis()).await()

                // Distribute sender key to all members
                distributeSenderKeyToSpaceMembers(spaceId, user.id)
            }
            else -> {
                // Join the space and without distributing sender key
                joinSpace(spaceId, user, role)
            }
        }
    }

    private suspend fun joinSpace(
        spaceId: String,
        user: ApiUser,
        role: Int
    ) {
        try {
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to join space $spaceId")
            throw e
        }
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
                    Timber.e("Invalid public key size for a space member")
                    continue
                }
                Curve.decodePoint(publicKeyBytes, 0)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode public key for a space member")
                continue
            }

            // Encrypt distribution using member's public key
            distributions.add(EphemeralECDHUtils.encrypt(member.user_id, distributionBytes, publicKey))
        }

        db.runTransaction { transaction ->
            val docRef = spaceGroupKeysDoc(spaceId)
            val snapshot = transaction.get(docRef)
            val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: GroupKeysDoc()
            val oldMemberKeyData = groupKeysDoc.member_keys[senderUserId] ?: MemberKeyData()
            val newMemberKeyData = oldMemberKeyData.copy(
                member_device_id = deviceId.toInt(),
                distributions = distributions,
                data_updated_at = System.currentTimeMillis()
            )
            val updates = mapOf(
                "member_keys.$senderUserId" to newMemberKeyData,
                "doc_updated_at" to System.currentTimeMillis()
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

        // Update the "docUpdatedAt" so others see membership changed and remove sender key for the removed user
        val docRef = spaceGroupKeysDoc(spaceId)
        docRef.update(
            mapOf(
                "doc_updated_at" to System.currentTimeMillis(),
                "member_keys.$userId" to FieldValue.delete()
            )
        ).await()
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
}

/**
 * Exception thrown when a user tries to perform an action that requires a subscription.
 */
class SubscriptionRequiredException(message: String) : Exception(message)
