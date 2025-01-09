package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedJourneyRoute
import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import com.google.firebase.firestore.Blob
import org.signal.libsignal.protocol.groups.GroupCipher
import timber.log.Timber
import java.util.UUID

/**
 * Convert an [EncryptedLocationJourney] to a [LocationJourney] using the provided [GroupCipher]
 */
fun EncryptedLocationJourney.toDecryptedLocationJourney(groupCipher: GroupCipher): LocationJourney {
    val decryptedFromLat = groupCipher.decrypt(from_latitude)
    val decryptedFromLong = groupCipher.decrypt(from_longitude)
    val decryptedToLat = to_latitude?.let { groupCipher.decrypt(it) }
    val decryptedToLong = to_longitude?.let { groupCipher.decrypt(it) }

    val decryptedRoutes = routes.map {
        JourneyRoute(
            latitude = groupCipher.decrypt(it.latitude),
            longitude = groupCipher.decrypt(it.longitude)
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
): EncryptedLocationJourney {
    val encryptedFromLat = groupCipher.encrypt(distributionId, from_latitude)
    val encryptedFromLong = groupCipher.encrypt(distributionId, from_longitude)
    val encryptedToLat = to_latitude?.let { groupCipher.encrypt(distributionId, it) }
    val encryptedToLong = to_longitude?.let { groupCipher.encrypt(distributionId, it) }

    val encryptedRoutes = routes.map {
        EncryptedJourneyRoute(
            latitude = groupCipher.encrypt(distributionId, it.latitude),
            longitude = groupCipher.encrypt(distributionId, it.longitude)
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

fun GroupCipher.decrypt(data: Blob): Double {
    return try {
        decrypt(data.toBytes()).toString(Charsets.UTF_8).toDouble()
    } catch (e: Exception) {
        Timber.e(e, "Failed to decrypt double")
        0.0
    }
}

fun GroupCipher.encrypt(distributionId: UUID, data: Double): Blob {
    return try {
        Blob.fromBytes(encrypt(distributionId, data.toString().toByteArray(Charsets.UTF_8)).serialize())
    } catch (e: Exception) {
        Timber.e(e, "Failed to encrypt double")
        Blob.fromBytes(ByteArray(0))
    }
}
