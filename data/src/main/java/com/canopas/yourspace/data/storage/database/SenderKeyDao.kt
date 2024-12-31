package com.canopas.yourspace.data.storage.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord

@Dao
interface SenderKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSenderKey(senderKeyEntity: SenderKeyEntity)

    @Query(
        """
        SELECT * FROM sender_keys 
        WHERE address = :address 
          AND device_id = :deviceId 
          AND distribution_id = :distributionId 
        LIMIT 1
    """
    )
    suspend fun getSenderKey(
        address: String,
        deviceId: Int,
        distributionId: String
    ): SenderKeyEntity?

    suspend fun getSenderKeyRecord(
        address: String,
        deviceId: Int,
        distributionId: String
    ): SenderKeyRecord? {
        val entity = getSenderKey(address, deviceId, distributionId)
        return entity?.let {
            try {
                SenderKeyRecord(it.record)
            } catch (e: Exception) {
                null
            }
        }
    }

    @Query(
        """
        DELETE FROM sender_keys 
        WHERE address = :address 
          AND device_id = :deviceId 
          AND distribution_id = :distributionId
    """
    )
    suspend fun deleteSenderKey(
        address: String,
        deviceId: Int,
        distributionId: String
    )
}
