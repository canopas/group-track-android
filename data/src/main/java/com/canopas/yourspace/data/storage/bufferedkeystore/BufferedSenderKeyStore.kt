package com.canopas.yourspace.data.storage.bufferedkeystore

import com.canopas.yourspace.data.storage.database.SenderKeyDao
import com.canopas.yourspace.data.storage.database.SenderKeyEntity
import kotlinx.coroutines.runBlocking
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * An in-memory sender key store that is intended to be used temporarily while decrypting messages.
 */
@Singleton
class BufferedSenderKeyStore @Inject constructor(
    @Named("sender_key_dao") private val senderKeyDao: SenderKeyDao
) : SignalServiceSenderKeyStore {

    private val store: MutableMap<StoreKey, SenderKeyRecord> = HashMap()

    /** All of the keys that have been created or updated during operation. */
    private val updatedKeys: MutableMap<StoreKey, SenderKeyRecord> = mutableMapOf()

    /** All of the distributionId's whose sharing has been cleared during operation. */
    private val clearSharedWith: MutableSet<SignalProtocolAddress> = mutableSetOf()

    override fun storeSenderKey(sender: SignalProtocolAddress, distributionId: UUID, record: SenderKeyRecord) {
        val key = StoreKey(sender, distributionId)
        store[key] = record
        updatedKeys[key] = record

        runBlocking {
            senderKeyDao.insertSenderKey(
                senderKeyEntity = SenderKeyEntity(
                    address = sender.name,
                    deviceId = sender.deviceId,
                    distributionId = distributionId.toString(),
                    record = record.serialize()
                )
            )
        }
    }

    override fun loadSenderKey(sender: SignalProtocolAddress, distributionId: UUID): SenderKeyRecord? {
        return store[StoreKey(sender, distributionId)]
            ?: runBlocking {
                val fromDatabase: SenderKeyRecord? =
                    senderKeyDao.getSenderKeyRecord(
                        address = sender.name,
                        deviceId = sender.deviceId,
                        distributionId = distributionId.toString()
                    )

                if (fromDatabase != null) {
                    store[StoreKey(sender, distributionId)] = fromDatabase
                }

                fromDatabase
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
