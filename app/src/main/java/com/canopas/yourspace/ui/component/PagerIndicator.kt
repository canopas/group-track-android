package com.canopas.yourspace.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun PagerIndicator(
    count: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = AppTheme.colorScheme.primary,
    inactiveColor: Color = AppTheme.colorScheme.containerLow,
    indicatorWidth: Dp = 7.dp,
    activeIndicatorWidth: Dp = 20.dp,
    indicatorHeight: Dp = indicatorWidth,
    spacing: Dp = 10.dp,
    indicatorShape: Shape = RoundedCornerShape(50)
) {
    Box(
        modifier = modifier.padding(top = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(count) {
                val width =
                    if (pagerState.currentPage == it) activeIndicatorWidth else indicatorWidth
                val indicatorModifier = Modifier
                    .size(width = width, height = indicatorHeight)
                    .background(color = inactiveColor, shape = indicatorShape)
                Box(indicatorModifier)
            }
        }

        Box(
            Modifier
                .offset {
                    val scrollPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    IntOffset(
                        x = ((spacing + indicatorWidth) * scrollPosition).roundToPx(),
                        y = 0
                    )
                }
                .size(width = activeIndicatorWidth, height = indicatorHeight)
                .background(
                    color = activeColor,
                    shape = indicatorShape
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview
@Composable
fun PreviewIndicator() {
    val pagerState = rememberPagerState(
        initialPage = 2,
        pageCount = { 3 }
    )

    PagerIndicator(3, pagerState)
}
