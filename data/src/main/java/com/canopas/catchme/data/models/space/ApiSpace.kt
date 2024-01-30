package com.canopas.catchme.data.models.space

import androidx.annotation.Keep
import java.util.UUID
import java.util.concurrent.TimeUnit

@Keep
data class ApiSpace(
    val id: String = UUID.randomUUID().toString(),
    val admin_id: String = "",
    val name: String = "",
    val created_at: Long? = System.currentTimeMillis()
)

const val SPACE_MEMBER_ROLE_ADMIN = 1
const val SPACE_MEMBER_ROLE_MEMBER = 2

@Keep
data class ApiSpaceMember(
    val id: String = UUID.randomUUID().toString(),
    val space_id: String = "",
    val user_id: String = "",
    val role: Int = SPACE_MEMBER_ROLE_MEMBER,
    val location_enabled: Boolean = true,
    val created_at: Long? = System.currentTimeMillis()
)

@Keep
data class ApiSpaceInvitation(
    val id: String = UUID.randomUUID().toString(),
    val space_id: String = "",
    val code: String = "",
    val created_at: Long? = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() {
            if (created_at == null) return true
            val currentTimeMillis = System.currentTimeMillis()
            val twoDaysMillis = TimeUnit.DAYS.toMillis(2)

            val differenceMillis = currentTimeMillis - this.created_at

            return differenceMillis > twoDaysMillis
        }
}
