package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedJourneyRoute
import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import org.signal.libsignal.protocol.groups.GroupCipher
import timber.log.Timber
import java.util.Base64
import java.util.UUID

fun String.toBytes(): ByteArray = Base64.getDecoder().decode(this)

fun ByteArray.encodeToString(): String = Base64.getEncoder().encodeToString(this)

/**
 * Converts [EncryptedLocationJourney] to [LocationJourney] using [groupCipher].
 * Returns null if *any* required decryption step fails.
 */
fun EncryptedLocationJourney.toDecryptedLocationJourney(
    groupCipher: GroupCipher
): LocationJourney? {
    // Decrypt required points
    val fromLat = groupCipher.decryptPoint(from_latitude.toBytes()) ?: return null
    val fromLong = groupCipher.decryptPoint(from_longitude.toBytes()) ?: return null

    // Decrypt optional points
    val toLat = to_latitude?.toBytes()?.let { groupCipher.decryptPoint(it) }
    val toLong = to_longitude?.toBytes()?.let { groupCipher.decryptPoint(it) }

    // Decrypt route list; short-circuit if any route fails
    val decryptedRoutes = routes.map { route ->
        val lat = groupCipher.decryptPoint(route.latitude.toBytes()) ?: return null
        val long = groupCipher.decryptPoint(route.longitude.toBytes()) ?: return null
        JourneyRoute(lat, long)
    }

    // Construct the decrypted journey
    return LocationJourney(
        id = id,
        user_id = user_id,
        from_latitude = fromLat,
        from_longitude = fromLong,
        to_latitude = toLat,
        to_longitude = toLong,
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
 * Converts [LocationJourney] to [EncryptedLocationJourney] using [groupCipher].
 * Returns null if *any* required encryption step fails.
 */
fun LocationJourney.toEncryptedLocationJourney(
    groupCipher: GroupCipher,
    distributionId: UUID
): EncryptedLocationJourney? {
    // Encrypt required points
    val fromLat = groupCipher.encryptPoint(distributionId, from_latitude) ?: return null
    val fromLong = groupCipher.encryptPoint(distributionId, from_longitude) ?: return null

    // Encrypt optional points
    val toLat = to_latitude?.let { groupCipher.encryptPoint(distributionId, it) }
    val toLong = to_longitude?.let { groupCipher.encryptPoint(distributionId, it) }

    // Encrypt route list; short-circuit if any route fails
    val encryptedRoutes = routes.map { route ->
        val lat = groupCipher.encryptPoint(distributionId, route.latitude) ?: return null
        val long = groupCipher.encryptPoint(distributionId, route.longitude) ?: return null
        EncryptedJourneyRoute(lat, long)
    }

    // Construct the encrypted journey
    return EncryptedLocationJourney(
        id = id,
        user_id = user_id,
        from_latitude = fromLat,
        from_longitude = fromLong,
        to_latitude = toLat,
        to_longitude = toLong,
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
        decrypt(data).toString(Charsets.UTF_8).toDoubleOrNull()
    } catch (e: Exception) {
        Timber.e(e, "Failed to decrypt double")
        null
    }
}

fun GroupCipher.encryptPoint(distributionId: UUID, data: Double): String? {
    return try {
        encrypt(distributionId, data.toString().toByteArray(Charsets.UTF_8))
            .serialize()
            .encodeToString()
    } catch (e: Exception) {
        Timber.e(e, "Failed to encrypt double")
        null
    }
}
