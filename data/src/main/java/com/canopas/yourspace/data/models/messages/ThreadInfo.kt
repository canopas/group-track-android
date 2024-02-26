package com.canopas.yourspace.data.models.messages

import com.canopas.yourspace.data.models.user.ApiUser

data class ThreadInfo(
    val thread: ApiThread,
    val messages: List<ApiThreadMessage>
)