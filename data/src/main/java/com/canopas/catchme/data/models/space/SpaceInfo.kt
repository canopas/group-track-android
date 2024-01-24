package com.canopas.catchme.data.models.space

import com.canopas.catchme.data.models.user.ApiUser

data class SpaceInfo(
    val space: ApiSpace,
    val admin: ApiUser?,
    val members: List<ApiUser>
)
