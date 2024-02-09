package com.canopas.yourspace.data.utils

import androidx.annotation.Keep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Keep
data class AppDispatcher(
    val IO: CoroutineDispatcher = Dispatchers.IO,
    val MAIN: CoroutineDispatcher = Dispatchers.Main
)
