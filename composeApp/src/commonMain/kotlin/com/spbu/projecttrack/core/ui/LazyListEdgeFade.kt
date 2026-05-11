package com.spbu.projecttrack.core.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.lazyListEdgeFadeMask(
    listState: LazyListState,
    topInset: Dp = 0.dp,
    topFadeHeight: Dp = 28.dp,
    bottomFadeHeight: Dp = 56.dp,
): Modifier {
    return graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithContent {
        drawContent()
        val canScrollUp = listState.canScrollBackward
        val canScrollDown = listState.canScrollForward
        val topInsetPx = topInset.toPx()
        val topFadeHeightPx = topFadeHeight.toPx()
        val bottomFadeHeightPx = bottomFadeHeight.toPx()

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = if (canScrollUp) 0f else 1f),
                    Color.Black,
                ),
                startY = topInsetPx,
                endY = topInsetPx + topFadeHeightPx,
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black,
                    Color.Black.copy(alpha = if (canScrollDown) 0f else 1f),
                ),
                startY = size.height - bottomFadeHeightPx,
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }
}
