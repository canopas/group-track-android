package com.canopas.yourspace.data.models.location

import androidx.annotation.Keep
import com.google.firebase.firestore.Blob
import com.squareup.moshi.JsonClass
import java.util.UUID

@Keep
@JsonClass(generateAdapter = true)
data class ApiLocation(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val created_at: Long = System.currentTimeMillis()
)

@Keep
@JsonClass(generateAdapter = true)
data class EncryptedApiLocation(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val encrypted_latitude: Blob = Blob.fromBytes(ByteArray(0)), // Base64 encoded encrypted latitude
    val encrypted_longitude: Blob = Blob.fromBytes(ByteArray(0)), // Base64 encoded encrypted longitude
    val created_at: Long = System.currentTimeMillis()
)

fun ApiLocation.toLocation() = android.location.Location("").apply {
    latitude = this@toLocation.latitude
    longitude = this@toLocation.longitude
}

fun android.location.Location.toApiLocation(userId: String) = ApiLocation(
    user_id = userId,
    latitude = latitude,
    longitude = longitude
)
