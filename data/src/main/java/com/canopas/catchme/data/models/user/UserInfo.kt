package com.canopas.catchme.data.models.user

import com.canopas.catchme.data.models.location.ApiLocation

data class UserInfo(
    val user: ApiUser,
    val location: ApiLocation? = null,
    val isLocationEnable: Boolean = true
)