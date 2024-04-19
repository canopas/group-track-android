package com.canopas.yourspace.data.models.place

import androidx.annotation.Keep
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

const val GEOFENCE_PLACE_RADIUS_DEFAULT = 100.0

@Keep
data class ApiPlace(
    val id: String = UUID.randomUUID().toString(),
    val created_by: String = "",
    val space_id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = GEOFENCE_PLACE_RADIUS_DEFAULT,
    @ServerTimestamp
    val created_at: Date? = null
)

@Keep
data class ApiPlaceMemberSetting(
    val place_id: String = "",
    val user_id: String = "",
    val alert_enable: Boolean = true,
    val arrival_alert_for: List<String> = emptyList(),
    val leave_alert_for: List<String> = emptyList()
)
