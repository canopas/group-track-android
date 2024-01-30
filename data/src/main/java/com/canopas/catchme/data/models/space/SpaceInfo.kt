package com.canopas.catchme.data.models.space

import androidx.annotation.Keep
import com.canopas.catchme.data.models.user.UserInfo

@Keep
data class SpaceInfo(
    val space: ApiSpace,
    val members: List<UserInfo>
)
