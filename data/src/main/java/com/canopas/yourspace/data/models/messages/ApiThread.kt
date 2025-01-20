package com.canopas.yourspace.data.models.messages

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Keep
data class ApiThread(
    val id: String = UUID.randomUUID().toString(),
    val admin_id: String = "",
    val space_id: String = "",
    val last_message: String = "",
    val member_ids: List<String> = emptyList(),
    val seen_by_ids: List<String> = emptyList(),
    val archived_for: Map<String, Long> = emptyMap<String, Long>(),
    val created_at: Long = System.currentTimeMillis(),
    val last_message_at: Date? = null
) {
    @get:Exclude
    val isGroup: Boolean
        get() = member_ids.size > 2
}

@Keep
data class ApiThreadMessage(
    val id: String = UUID.randomUUID().toString(),
    val thread_id: String = "",
    val sender_id: String = "",
    val message: String = "",
    val seen_by: List<String> = emptyList(),
    @ServerTimestamp
    val created_at: Date? = null
) {
    @get:Exclude
    val isSent: Boolean
        get() = created_at != null

    @get:Exclude
    val createdAtMs: Long
        get() = created_at?.time ?: Date().time

    @get:Exclude
    val formattedTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(createdAtMs))
}
