package com.canopas.catchme.data.models.user

import androidx.annotation.Keep
import com.canopas.catchme.data.models.location.ApiLocation

@Keep
data class UserInfo(
    val user: ApiUser,
    val location: ApiLocation? = null,
    val isLocationEnable: Boolean = true
)
