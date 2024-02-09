package com.canopas.yourspace.data.models.space

import androidx.annotation.Keep
import com.canopas.yourspace.data.models.user.UserInfo

@Keep
data class SpaceInfo(
    val space: ApiSpace,
    val members: List<UserInfo>
)
