package com.canopas.yourspace.data.models.messages

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Keep
data class ApiThread(
    val id: String = UUID.randomUUID().toString(),
    val admin_id: String = "",
    val space_id: String = "",
    val created_at: Long = System.currentTimeMillis()
)

@Keep
data class ApiThreadMember(
    val id: String = UUID.randomUUID().toString(),
    val thread_id: String = "",
    val user_id: String = "",
    val created_at: Long = System.currentTimeMillis()
)

@Keep
data class ApiThreadMessage(
    val id: String = UUID.randomUUID().toString(),
    val thread_id: String = "",
    val sender_id: String = "",
    val message: String = "",
    val read_by: List<String> = emptyList(),
    val created_at: Long = System.currentTimeMillis()
) {
    @get:Exclude
    val formattedTime: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(created_at))

}
