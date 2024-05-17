package com.canopas.yourspace.data.models.user

import androidx.annotation.Keep
import com.canopas.yourspace.data.models.location.ApiLocation

@Keep
data class UserInfo(
    val user: ApiUser,
    val location: ApiLocation? = null,
    val isLocationEnable: Boolean = true,
    val session: ApiUserSession? = null
)
