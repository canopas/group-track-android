package com.canopas.yourspace.data.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.Blob
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
* Represents a sender key record for Signal Protocol implementation.
 * @property id Unique identifier for the record.
 * @property address The sender's address in Signal Protocol format, relatively spaceId for our use case.
 * @property device_id The sender's device ID(must be positive).
 * @property distribution_id The distribution ID for the sender key. - A random UUID.
 * @property record The actual sender key record.
 * @property created_at The timestamp when the record was created.
* */
@Keep
@JsonClass(generateAdapter = true)
data class ApiSenderKeyRecord(
    val id: String = UUID.randomUUID().toString(),
    val address: String = "",
    val device_id: Int = 0,
    val distribution_id: String = "",
    val record: Blob = Blob.fromBytes(ByteArray(0)),
    val created_at: Long = System.currentTimeMillis()
) {
    init {
        require(device_id > 0) { "Device ID must be non-negative." }
    }
}
