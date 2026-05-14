package com.spbu.projecttrack.analytics.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.spbu.projecttrack.analytics.events.BlockEvents
import com.spbu.projecttrack.analytics.model.BlockType

/**
 * Modifier для отслеживания тапов по блоку.
 * Не блокирует другие жесты — работает совместно с clickable{}.
 */
fun Modifier.trackTap(
    blockId: String,
    blockType: BlockType,
    screenName: String,
    analyticsContext: AnalyticsContext,
    action: String = "tap",
    onTapTracked: ((BlockTapSnapshot) -> Unit)? = null,
): Modifier = this.pointerInput(blockId) {
    detectTapGestures(
        onTap = {
            analyticsContext.tracker.track(
                BlockEvents.tapped(
                    blockId = blockId,
                    blockType = blockType,
                    screenName = screenName,
                    sessionId = analyticsContext.sessionId,
                    userId = analyticsContext.userId,
                )
            )
            onTapTracked?.invoke(
                BlockTapSnapshot(
                    blockId = blockId,
                    blockType = blockType,
                    screenName = screenName,
                    action = action,
                )
            )
        }
    )
}
