package com.canopas.catchme.data.models.space

class ApiSpace(
    val id: String,
    val admin_id: String,
    val name: String,
    val created_at: Long? = System.currentTimeMillis()
)

const val SPACE_MEMBER_ROLE_ADMIN = 1
const val SPACE_MEMBER_ROLE_MEMBER = 2

class ApiSpaceMember(
    val id: String,
    val space_id: String,
    val user_id: String,
    val role: Int,
    val location_enabled: Boolean,
    val created_at: Long? = System.currentTimeMillis()
)

class ApiSpaceInvitation(
    val id: String,
    val space_id: String,
    val code: String,
    val created_at: Long? = System.currentTimeMillis()
)
