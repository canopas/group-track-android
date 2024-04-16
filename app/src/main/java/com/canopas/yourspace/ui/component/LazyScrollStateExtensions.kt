package com.canopas.yourspace.ui.component

import androidx.compose.foundation.lazy.LazyListState

internal fun LazyListState.reachedBottom(): Boolean {
    val visibleItemsInfo = layoutInfo.visibleItemsInfo
    return if (layoutInfo.totalItemsCount == 0) {
        false
    } else {
        val lastVisibleItem = visibleItemsInfo.last()
        val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset

        (
            lastVisibleItem.index + 1 == layoutInfo.totalItemsCount &&
                lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight
            )
    }
}
