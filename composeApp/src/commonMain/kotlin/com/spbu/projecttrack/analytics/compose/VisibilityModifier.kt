package com.spbu.projecttrack.analytics.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.spbu.projecttrack.analytics.events.BlockEvents
import com.spbu.projecttrack.analytics.model.BlockType
import com.spbu.projecttrack.core.time.PlatformTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun LayoutCoordinates.rootCoordinates(): LayoutCoordinates {
    var current = this
    while (current.parentLayoutCoordinates != null) {
        current = current.parentLayoutCoordinates!!
    }
    return current
}

@Composable
fun TrackVisibility(
    blockId: String,
    blockType: BlockType,
    screenName: String,
    analyticsContext: AnalyticsContext,
    modifier: Modifier = Modifier,
    visibilityThreshold: Float = 0.5f,
    minDurationMs: Long = 2_000L,
    focusVisibilityThreshold: Float = 0.6f,
    focusMinDurationMs: Long = 1_500L,
    onViewed: ((BlockVisibilitySnapshot) -> Unit)? = null,
    onIgnored: ((BlockVisibilitySnapshot) -> Unit)? = null,
    onFocus: ((BlockVisibilitySnapshot) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var currentRatio by remember { mutableFloatStateOf(0f) }
    var maxVisibleRatio by remember { mutableFloatStateOf(0f) }
    var totalVisibleMs by remember { mutableLongStateOf(0L) }
    var visibleSince by remember { mutableStateOf<Long?>(null) }
    var hasReported by remember { mutableStateOf(false) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }
    var focusVisibleMs by remember { mutableLongStateOf(0L) }
    var focusVisibleSince by remember { mutableStateOf<Long?>(null) }
    var hasFocusReported by remember { mutableStateOf(false) }
    var focusJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            val bounds = coords.boundsInWindow()
            val windowSize = coords.rootCoordinates().size

            if (bounds.isEmpty || windowSize.width == 0 || windowSize.height == 0) {
                currentRatio = 0f
                return@onGloballyPositioned
            }

            val visTop = maxOf(bounds.top, 0f)
            val visBottom = minOf(bounds.bottom, windowSize.height.toFloat())
            val visLeft = maxOf(bounds.left, 0f)
            val visRight = minOf(bounds.right, windowSize.width.toFloat())

            val visArea = maxOf(0f, visBottom - visTop) * maxOf(0f, visRight - visLeft)
            val totalArea = coords.size.width.toFloat() * coords.size.height.toFloat()

            currentRatio = if (totalArea > 0f) {
                (visArea / totalArea).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    ) {
        content()
    }

    LaunchedEffect(blockId) {
        snapshotFlow { currentRatio }.collect { ratio ->
            maxVisibleRatio = maxOf(maxVisibleRatio, ratio)
            val now = PlatformTime.currentTimeMillis()

            if (ratio >= visibilityThreshold) {
                if (visibleSince == null) {
                    visibleSince = now
                }

                if (!hasReported) {
                    debounceJob?.cancel()
                    debounceJob = launch {
                        delay(minDurationMs)

                        val since = visibleSince ?: return@launch
                        val visibleDurationMs = totalVisibleMs + (PlatformTime.currentTimeMillis() - since)

                        if (!hasReported) {
                            val snapshot = BlockVisibilitySnapshot(
                                blockId = blockId,
                                blockType = blockType,
                                screenName = screenName,
                                durationMs = visibleDurationMs,
                                maxVisibleRatio = maxVisibleRatio,
                            )
                            hasReported = true
                            analyticsContext.tracker.track(
                                BlockEvents.viewed(
                                    blockId = snapshot.blockId,
                                    blockType = snapshot.blockType,
                                    visibleRatio = snapshot.maxVisibleRatio,
                                    durationMs = snapshot.durationMs,
                                    screenName = snapshot.screenName,
                                    sessionId = analyticsContext.sessionId,
                                    userId = analyticsContext.userId,
                                )
                            )
                            onViewed?.invoke(snapshot)
                        }
                    }
                }
            } else {
                debounceJob?.cancel()

                visibleSince?.let { since ->
                    totalVisibleMs += now - since
                    visibleSince = null
                }
            }

            if (ratio >= focusVisibilityThreshold) {
                if (focusVisibleSince == null) {
                    focusVisibleSince = now
                }

                if (!hasFocusReported) {
                    focusJob?.cancel()
                    val remainingMs = (focusMinDurationMs - focusVisibleMs).coerceAtLeast(0L)
                    focusJob = launch {
                        delay(remainingMs)

                        val since = focusVisibleSince ?: return@launch
                        val visibleDurationMs = focusVisibleMs + (PlatformTime.currentTimeMillis() - since)

                        if (!hasFocusReported && currentRatio >= focusVisibilityThreshold && visibleDurationMs >= focusMinDurationMs) {
                            val snapshot = BlockVisibilitySnapshot(
                                blockId = blockId,
                                blockType = blockType,
                                screenName = screenName,
                                durationMs = visibleDurationMs,
                                maxVisibleRatio = maxVisibleRatio,
                            )
                            hasFocusReported = true
                            onFocus?.invoke(snapshot)
                        }
                    }
                }
            } else {
                focusJob?.cancel()

                focusVisibleSince?.let { since ->
                    focusVisibleMs += now - since
                    focusVisibleSince = null
                }
            }
        }
    }

    DisposableEffect(blockId) {
        onDispose {
            debounceJob?.cancel()
            focusJob?.cancel()

            val finalVisibleMs = visibleSince?.let { since ->
                totalVisibleMs + (PlatformTime.currentTimeMillis() - since)
            } ?: totalVisibleMs
            val finalFocusVisibleMs = focusVisibleSince?.let { since ->
                focusVisibleMs + (PlatformTime.currentTimeMillis() - since)
            } ?: focusVisibleMs

            visibleSince?.let { since ->
                totalVisibleMs = totalVisibleMs + (PlatformTime.currentTimeMillis() - since)
            }
            focusVisibleSince?.let { since ->
                focusVisibleMs = focusVisibleMs + (PlatformTime.currentTimeMillis() - since)
            }

            if (!hasFocusReported && finalFocusVisibleMs >= focusMinDurationMs && maxVisibleRatio >= focusVisibilityThreshold) {
                onFocus?.invoke(
                    BlockVisibilitySnapshot(
                        blockId = blockId,
                        blockType = blockType,
                        screenName = screenName,
                        durationMs = finalFocusVisibleMs,
                        maxVisibleRatio = maxVisibleRatio,
                    )
                )
            }

            if (!hasReported && totalVisibleMs > 0) {
                if (maxVisibleRatio >= visibilityThreshold && finalVisibleMs >= minDurationMs) {
                    val snapshot = BlockVisibilitySnapshot(
                        blockId = blockId,
                        blockType = blockType,
                        screenName = screenName,
                        durationMs = finalVisibleMs,
                        maxVisibleRatio = maxVisibleRatio,
                    )
                    analyticsContext.tracker.track(
                        BlockEvents.viewed(
                            blockId = snapshot.blockId,
                            blockType = snapshot.blockType,
                            visibleRatio = snapshot.maxVisibleRatio,
                            durationMs = snapshot.durationMs,
                            screenName = snapshot.screenName,
                            sessionId = analyticsContext.sessionId,
                            userId = analyticsContext.userId,
                        )
                    )
                    onViewed?.invoke(snapshot)
                } else if (maxVisibleRatio > 0f) {
                    val snapshot = BlockVisibilitySnapshot(
                        blockId = blockId,
                        blockType = blockType,
                        screenName = screenName,
                        durationMs = finalVisibleMs,
                        maxVisibleRatio = maxVisibleRatio,
                    )
                    analyticsContext.tracker.track(
                        BlockEvents.ignored(
                            blockId = snapshot.blockId,
                            blockType = snapshot.blockType,
                            maxVisibleRatio = snapshot.maxVisibleRatio,
                            screenName = snapshot.screenName,
                            sessionId = analyticsContext.sessionId,
                            userId = analyticsContext.userId,
                        )
                    )
                    onIgnored?.invoke(snapshot)
                }
            }
        }
    }
}
