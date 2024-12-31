package com.canopas.yourspace.data.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.Blob
import com.squareup.moshi.JsonClass
import java.util.UUID

@Keep
@JsonClass(generateAdapter = true)
data class ApiSenderKeyRecord(
    val id: String = UUID.randomUUID().toString(),
    val address: String = "",
    val deviceId: Int = 0,
    val distributionId: String = "",
    val record: Blob = Blob.fromBytes(ByteArray(0)),
    val createdAt: Long = System.currentTimeMillis()
)
