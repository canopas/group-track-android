package com.canopas.yourspace.ui.component

import androidx.compose.foundation.lazy.LazyListState

internal fun LazyListState.reachedBottom(): Boolean {
    val isScrollEnd =
        layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

    val totalItemsNumber = layoutInfo.totalItemsCount
    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

    return isScrollEnd && lastVisibleItemIndex >= totalItemsNumber - 1
}
