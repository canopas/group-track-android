package com.canopas.yourspace.data.storage.bufferedkeystore

import com.canopas.yourspace.data.models.user.ApiSenderKeyRecord
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.database.SenderKeyDao
import com.canopas.yourspace.data.storage.database.SenderKeyEntity
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USER_SENDER_KEY_RECORD
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import org.signal.libsignal.protocol.groups.state.SenderKeyStore
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * An in-memory sender key store with persistent and server backup.
 */
@Singleton
class BufferedSenderKeyStore @Inject constructor(
    db: FirebaseFirestore,
    @Named("sender_key_dao") private val senderKeyDao: SenderKeyDao,
    private val userPreferences: UserPreferences,
    private val appDispatcher: AppDispatcher
) : SenderKeyStore {

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)

    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.ifBlank { "null" })
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceSenderKeyRecordRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId)
            .document(userId.ifBlank { "null" })
            .collection(FIRESTORE_COLLECTION_USER_SENDER_KEY_RECORD)

    private val inMemoryStore: MutableMap<StoreKey, SenderKeyRecord> = mutableMapOf()

    private suspend fun saveSenderKeyToServer(senderKeyRecord: ApiSenderKeyRecord) {
        try {
            val currentUser = userPreferences.currentUser ?: return
            val uniqueDocId = "${senderKeyRecord.device_id}-${senderKeyRecord.distribution_id}"
            spaceSenderKeyRecordRef(senderKeyRecord.address, currentUser.id)
                .document(uniqueDocId).set(senderKeyRecord).await()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save sender key to server: $senderKeyRecord")
        }
    }

    override fun storeSenderKey(
        sender: SignalProtocolAddress,
        distributionId: UUID,
        record: SenderKeyRecord
    ) {
        val currentUser = userPreferences.currentUser ?: return
        if (currentUser.isFreeUser) {
            return
        }
        val key = StoreKey(sender, distributionId, sender.deviceId)
        if (inMemoryStore.containsKey(key)) {
            return
        }
        inMemoryStore[key] = record

        CoroutineScope(appDispatcher.IO).launch {
            senderKeyDao.insertSenderKey(
                SenderKeyEntity(
                    address = sender.name,
                    deviceId = sender.deviceId,
                    distributionId = distributionId.toString(),
                    record = record.serialize()
                )
            )

            val senderKeyRecord = ApiSenderKeyRecord(
                address = sender.name,
                device_id = sender.deviceId,
                distribution_id = distributionId.toString(),
                record = Blob.fromBytes(record.serialize())
            )
            saveSenderKeyToServer(senderKeyRecord)
        }
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? {
        val currentUser = userPreferences.currentUser ?: return null
        if (currentUser.isFreeUser) {
            return null
        }
        val key = StoreKey(sender, distributionId, sender.deviceId)

        return inMemoryStore[key] ?: kotlin.run {
            val deferred = CompletableDeferred<SenderKeyRecord?>()
            CoroutineScope(appDispatcher.IO).launch {
                try {
                    val senderKeyRecord = senderKeyDao.getSenderKeyRecord(
                        address = sender.name,
                        deviceId = sender.deviceId,
                        distributionId = distributionId.toString()
                    )?.also {
                        inMemoryStore[key] = it
                    } ?: fetchSenderKeyFromServer(sender, distributionId)?.also {
                        inMemoryStore[key] = it
                    }
                    deferred.complete(senderKeyRecord)
                } catch (e: Exception) {
                    deferred.completeExceptionally(e)
                }
            }
            runBlocking {
                try {
                    deferred.await()
                } catch (e: Exception) {
                    Timber.e(e, "Error loading sender key")
                    null
                }
            }
        }
    }

    private suspend fun fetchSenderKeyFromServer(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? {
        val currentUser = userPreferences.currentUser ?: return null
        return try {
            spaceSenderKeyRecordRef(sender.name.toString(), currentUser.id)
                .whereEqualTo("device_id", sender.deviceId)
                .whereEqualTo("distribution_id", distributionId.toString())
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject(ApiSenderKeyRecord::class.java)
                ?.let { apiSenderKeyRecord ->
                    try {
                        SenderKeyRecord(apiSenderKeyRecord.record.toBytes())
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to deserialize sender key record")
                        null
                    }
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch sender key from server for sender: $sender")
            null
        }
    }

    data class StoreKey(val address: SignalProtocolAddress, val distributionId: UUID, val senderDeviceId: Int)
}
