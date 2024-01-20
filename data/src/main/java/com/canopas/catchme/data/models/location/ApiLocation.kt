package com.canopas.catchme.data.models.location

import java.util.UUID

data class ApiLocation(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val created_at: Long? = System.currentTimeMillis()
)
