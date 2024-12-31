package com.canopas.yourspace.data.storage.bufferedkeystore

import com.canopas.yourspace.data.models.user.ApiSenderKeyRecord
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USER_SENDER_KEY_RECORD
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * An in-memory sender key store that is intended to be used temporarily while decrypting messages.
 */
@Singleton
class BufferedSenderKeyStore @Inject constructor(
    db: FirebaseFirestore,
    private val userPreferences: UserPreferences
) : SignalServiceSenderKeyStore {

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceSenderKeyRecordRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId)
            .document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_USER_SENDER_KEY_RECORD)

    private val store: MutableMap<StoreKey, SenderKeyRecord> = HashMap()

    /** All of the keys that have been created or updated during operation. */
    private val updatedKeys: MutableMap<StoreKey, SenderKeyRecord> = mutableMapOf()

    /** All of the distributionId's whose sharing has been cleared during operation. */
    private val clearSharedWith: MutableSet<SignalProtocolAddress> = mutableSetOf()

    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID,
        record: SenderKeyRecord
    ) {
        val key = StoreKey(sender, distributionId)
        store[key] = record
        updatedKeys[key] = record

        runBlocking {
            val currentUser = userPreferences.currentUser ?: return@runBlocking
            val senderKeyRecord = ApiSenderKeyRecord(
                address = sender.name,
                deviceId = sender.deviceId,
                distributionId = distributionId.toString(),
                record = Blob.fromBytes(record.serialize())
            )
            spaceSenderKeyRecordRef(
                distributionId.toString(),
                currentUser.id
            ).document().set(senderKeyRecord).await()
        }
    }

    override fun loadSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID
    ): SenderKeyRecord? {
        return store[StoreKey(sender, distributionId)]
            ?: runBlocking {
                val currentUser = userPreferences.currentUser ?: return@runBlocking null
                val fromServer: SenderKeyRecord? =
                    spaceSenderKeyRecordRef(
                        distributionId.toString(),
                        currentUser.id
                    ).document().get().await().toObject(ApiSenderKeyRecord::class.java)?.let {
                        try {
                            SenderKeyRecord(it.record.toBytes())
                        } catch (e: Exception) {
                            Timber.e(e, "Error while loading sender key record")
                            null
                        }
                    }
                if (fromServer != null) {
                    store[StoreKey(sender, distributionId)] = fromServer
                }

                fromServer
            }
    }

    override fun getSenderKeySharedWith(distributionId: DistributionId?): MutableSet<SignalProtocolAddress> {
        error("Should not happen during the intended usage pattern of this class")
    }

    override fun markSenderKeySharedWith(
        distributionId: DistributionId?,
        addresses: Collection<SignalProtocolAddress?>?
    ) {
        error("Should not happen during the intended usage pattern of this class")
    }

    override fun clearSenderKeySharedWith(addresses: Collection<SignalProtocolAddress?>?) {
        addresses?.forEach { address ->
            address?.let { clearSharedWith.add(it) }
        }
    }

    private fun UUID.toDistributionId() = DistributionId.from(this)

    data class StoreKey(
        val address: SignalProtocolAddress,
        val distributionId: UUID
    )
}
