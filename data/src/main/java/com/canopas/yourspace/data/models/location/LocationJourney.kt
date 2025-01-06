package com.canopas.yourspace.data.models.location

import android.location.Location
import androidx.annotation.Keep
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.Blob
import com.squareup.moshi.JsonClass
import org.signal.libsignal.protocol.groups.GroupCipher
import java.util.UUID

@Keep
@JsonClass(generateAdapter = true)
data class LocationJourney(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val from_latitude: Double = 0.0,
    val from_longitude: Double = 0.0,
    var to_latitude: Double? = null,
    var to_longitude: Double? = null,
    val route_distance: Double? = null,
    val route_duration: Long? = null,
    val routes: List<JourneyRoute> = emptyList(),
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val type: JourneyType? = null
)

@Keep
@JsonClass(generateAdapter = true)
data class EncryptedLocationJourney(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val from_latitude: Blob = Blob.fromBytes(ByteArray(0)), // Encrypted latitude - from
    val from_longitude: Blob = Blob.fromBytes(ByteArray(0)), // Encrypted longitude - from
    val to_latitude: Blob? = null, // Encrypted latitude - to
    val to_longitude: Blob? = null, // Encrypted longitude - to
    val route_distance: Double? = null,
    val route_duration: Long? = null,
    val routes: List<EncryptedJourneyRoute> = emptyList(), // Encrypted journey routes
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val type: JourneyType? = null
)

@Keep
data class JourneyRoute(val latitude: Double = 0.0, val longitude: Double = 0.0)

@Keep
@JsonClass(generateAdapter = true)
data class EncryptedJourneyRoute(
    val latitude: Blob = Blob.fromBytes(ByteArray(0)), // Encrypted latitude
    val longitude: Blob = Blob.fromBytes(ByteArray(0)) // Encrypted longitude
)

fun Location.toRoute(): JourneyRoute {
    return JourneyRoute(latitude, longitude)
}

fun JourneyRoute.toLatLng() = LatLng(latitude, longitude)
fun LocationJourney.toRoute(): List<LatLng> {
    if (isSteadyJourney()) {
        return emptyList()
    } else if (isMovingJourney()) {
        val result = listOf(
            LatLng(
                from_latitude,
                from_longitude
            )
        ) + routes.map { it.toLatLng() } + listOf(
            LatLng(to_latitude ?: 0.0, to_longitude ?: 0.0)
        )
        return result
    } else {
        return emptyList()
    }
}

fun LocationJourney.isSteadyJourney(): Boolean {
    if (type != null) {
        return type == JourneyType.STEADY
    }
    return to_latitude == null || to_longitude == null
}

fun LocationJourney.isMovingJourney(): Boolean {
    if (type != null) {
        return type == JourneyType.MOVING
    }
    return to_latitude != null && to_longitude != null
}

fun LocationJourney.toLocationFromSteadyJourney() = Location("").apply {
    latitude = this@toLocationFromSteadyJourney.from_latitude
    longitude = this@toLocationFromSteadyJourney.from_longitude
}

fun LocationJourney.toLocationFromMovingJourney() = Location("").apply {
    latitude = this@toLocationFromMovingJourney.to_latitude ?: 0.0
    longitude = this@toLocationFromMovingJourney.to_longitude ?: 0.0
}

fun Location.toLocationJourney(userId: String, journeyId: String) = LocationJourney(
    id = journeyId,
    user_id = userId,
    from_latitude = latitude,
    from_longitude = longitude
)

enum class JourneyType {
    MOVING,
    STEADY
}

/**
 * Convert an [EncryptedLocationJourney] to a [LocationJourney] using the provided [GroupCipher]
 */
fun EncryptedLocationJourney.toDecryptedLocationJourney(groupCipher: GroupCipher): LocationJourney {
    val decryptedFromLat = groupCipher.decrypt(from_latitude.toBytes())
    val decryptedFromLong = groupCipher.decrypt(from_longitude.toBytes())
    val decryptedToLat = to_latitude?.let { groupCipher.decrypt(it.toBytes()) }
    val decryptedToLong = to_longitude?.let { groupCipher.decrypt(it.toBytes()) }

    val decryptedRoutes = routes.map {
        JourneyRoute(
            latitude = groupCipher.decrypt(it.latitude.toBytes())
                .toString(Charsets.UTF_8).toDouble(),
            longitude = groupCipher.decrypt(it.longitude.toBytes())
                .toString(Charsets.UTF_8).toDouble()
        )
    }

    return LocationJourney(
        id = id,
        user_id = user_id,
        from_latitude = decryptedFromLat.toString(Charsets.UTF_8).toDouble(),
        from_longitude = decryptedFromLong.toString(Charsets.UTF_8).toDouble(),
        to_latitude = decryptedToLat?.toString(Charsets.UTF_8)?.toDouble(),
        to_longitude = decryptedToLong?.toString(Charsets.UTF_8)?.toDouble(),
        route_distance = route_distance,
        route_duration = route_duration,
        routes = decryptedRoutes,
        created_at = created_at,
        updated_at = updated_at
    )
}

/**
 * Convert a [LocationJourney] to an [EncryptedLocationJourney] using the provided [GroupCipher]
 */
fun LocationJourney.toEncryptedLocationJourney(
    groupCipher: GroupCipher,
    distributionId: UUID
): EncryptedLocationJourney {
    val encryptedFromLat = groupCipher.encrypt(
        distributionId,
        from_latitude.toString().toByteArray(Charsets.UTF_8)
    )
    val encryptedFromLong = groupCipher.encrypt(
        distributionId,
        from_longitude.toString().toByteArray(Charsets.UTF_8)
    )
    val encryptedToLat = to_latitude?.let {
        groupCipher.encrypt(
            distributionId,
            it.toString().toByteArray(Charsets.UTF_8)
        )
    }
    val encryptedToLong = to_longitude?.let {
        groupCipher.encrypt(
            distributionId,
            it.toString().toByteArray(Charsets.UTF_8)
        )
    }

    val encryptedRoutes = routes.map {
        EncryptedJourneyRoute(
            latitude = Blob.fromBytes(
                groupCipher.encrypt(
                    distributionId,
                    it.latitude.toString().toByteArray(Charsets.UTF_8)
                ).serialize()
            ),
            longitude = Blob.fromBytes(
                groupCipher.encrypt(
                    distributionId,
                    it.longitude.toString().toByteArray(Charsets.UTF_8)
                ).serialize()
            )
        )
    }

    return EncryptedLocationJourney(
        id = id,
        user_id = user_id,
        from_latitude = Blob.fromBytes(encryptedFromLat.serialize()),
        from_longitude = Blob.fromBytes(encryptedFromLong.serialize()),
        to_latitude = encryptedToLat?.let { Blob.fromBytes(it.serialize()) },
        to_longitude = encryptedToLong?.let { Blob.fromBytes(it.serialize()) },
        route_distance = route_distance,
        route_duration = route_duration,
        routes = encryptedRoutes,
        created_at = created_at,
        updated_at = updated_at
    )
}
