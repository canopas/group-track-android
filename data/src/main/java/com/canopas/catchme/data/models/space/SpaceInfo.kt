package com.canopas.catchme.data.models.space

import com.canopas.catchme.data.models.user.UserInfo

data class SpaceInfo(
    val space: ApiSpace,
    val members: List<UserInfo>
)
