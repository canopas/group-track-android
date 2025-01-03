package com.canopas.yourspace.data.storage.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sender_keys")
data class SenderKeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "device_id") val deviceId: Int,
    @ColumnInfo(name = "distribution_id") val distributionId: String,
    @ColumnInfo(name = "record") val record: ByteArray,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SenderKeyEntity

        if (id != other.id) return false
        if (address != other.address) return false
        if (deviceId != other.deviceId) return false
        if (distributionId != other.distributionId) return false
        if (!record.contentEquals(other.record)) return false
        if (createdAt != other.createdAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + deviceId
        result = 31 * result + distributionId.hashCode()
        result = 31 * result + record.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
