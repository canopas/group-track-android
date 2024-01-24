package com.canopas.catchme.data.models.space

import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.user.UserInfo

data class SpaceInfo(
    val space: ApiSpace,
    val admin: ApiUser?,
    val members: List<ApiUser>
) {

}