package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedJourneyRoute
import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import org.signal.libsignal.protocol.groups.GroupCipher
import timber.log.Timber
import java.util.Base64
import java.util.UUID

fun String.toBytes(): ByteArray {
    return Base64.getDecoder().decode(this)
}

fun ByteArray.encodeToString(): String {
    return Base64.getEncoder().encodeToString(this)
}

/**
 * Convert an [EncryptedLocationJourney] to a [LocationJourney] using the provided [GroupCipher]
 */
fun EncryptedLocationJourney.toDecryptedLocationJourney(groupCipher: GroupCipher): LocationJourney? {
    val decryptedFromLat = groupCipher.decryptPoint(from_latitude.toBytes()) ?: return null
    val decryptedFromLong = groupCipher.decryptPoint(from_longitude.toBytes()) ?: return null
    val decryptedToLat = to_latitude?.let { groupCipher.decryptPoint(it.toBytes()) }
    val decryptedToLong = to_longitude?.let { groupCipher.decryptPoint(it.toBytes()) }

    val decryptedRoutes = routes.map {
        JourneyRoute(
            latitude = groupCipher.decryptPoint(it.latitude.toBytes()) ?: return null,
            longitude = groupCipher.decryptPoint(it.longitude.toBytes()) ?: return null
        )
    }

    return LocationJourney(
        id = id,
        user_id = user_id,
        from_latitude = decryptedFromLat,
        from_longitude = decryptedFromLong,
        to_latitude = decryptedToLat,
        to_longitude = decryptedToLong,
        route_distance = route_distance,
        route_duration = route_duration,
        routes = decryptedRoutes,
        created_at = created_at,
        updated_at = updated_at,
        type = type,
        key_id = key_id
    )
}

/**
 * Convert a [LocationJourney] to an [EncryptedLocationJourney] using the provided [GroupCipher]
 */
fun LocationJourney.toEncryptedLocationJourney(
    groupCipher: GroupCipher,
    distributionId: UUID
): EncryptedLocationJourney? {
    val encryptedFromLat = groupCipher.encryptPoint(distributionId, from_latitude) ?: return null
    val encryptedFromLong = groupCipher.encryptPoint(distributionId, from_longitude) ?: return null
    val encryptedToLat = to_latitude?.let { groupCipher.encryptPoint(distributionId, it) }
    val encryptedToLong = to_longitude?.let { groupCipher.encryptPoint(distributionId, it) }

    val encryptedRoutes = routes.map {
        EncryptedJourneyRoute(
            latitude = groupCipher.encryptPoint(distributionId, it.latitude) ?: return null,
            longitude = groupCipher.encryptPoint(distributionId, it.longitude) ?: return null
        )
    }

    return EncryptedLocationJourney(
        id = id,
        user_id = user_id,
        from_latitude = encryptedFromLat,
        from_longitude = encryptedFromLong,
        to_latitude = encryptedToLat,
        to_longitude = encryptedToLong,
        route_distance = route_distance,
        route_duration = route_duration,
        routes = encryptedRoutes,
        created_at = created_at,
        updated_at = updated_at,
        type = type,
        key_id = key_id
    )
}

fun GroupCipher.decryptPoint(data: ByteArray): Double? {
    return try {
        decrypt(data).encodeToString().toDouble()
    } catch (e: Exception) {
        Timber.e(e, "Failed to decrypt double")
        null
    }
}

fun GroupCipher.encryptPoint(distributionId: UUID, data: Double): String? {
    return try {
        (encrypt(distributionId, data.toString().toBytes()).serialize()).encodeToString()
    } catch (e: Exception) {
        Timber.e(e, "Failed to encrypt double")
        null
    }
}
