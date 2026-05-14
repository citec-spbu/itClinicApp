package com.spbu.projecttrack.analytics.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.spbu.projecttrack.analytics.events.ScrollEvents
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
private val DEPTH_MILESTONES = listOf(25, 50, 75, 100)

/**
 * Отслеживает глубину прокрутки LazyColumn.
 * Отправляет scroll_depth_reached при пересечении 25/50/75/100%.
 * Каждый milestone отправляется ровно один раз за сессию экрана.
 *
 * Используй [derivedStateOf] — пересчёт только при реальном изменении глубины.
 */
@Composable
fun TrackScrollDepth(
    screenName: String,
    scrollState: LazyListState,
    analyticsContext: AnalyticsContext,
    onDepthTracked: ((Int) -> Unit)? = null,
) {
    val reportedMilestones = remember { mutableSetOf<Int>() }

    val depthPercent by remember(scrollState) {
        derivedStateOf {
            val info = scrollState.layoutInfo
            if (info.totalItemsCount == 0) return@derivedStateOf 0
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            ((lastVisible + 1).toFloat() / info.totalItemsCount * 100)
                .toInt()
                .coerceIn(0, 100)
        }
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { depthPercent }
            .distinctUntilChanged()
            .collect { depth ->
                DEPTH_MILESTONES
                    .filter { milestone -> depth >= milestone && milestone !in reportedMilestones }
                    .forEach { milestone ->
                        reportedMilestones.add(milestone)
                        onDepthTracked?.invoke(milestone)
                        analyticsContext.tracker.track(
                            ScrollEvents.depthReached(
                                screenName = screenName,
                                depthPercent = milestone,
                                sessionId = analyticsContext.sessionId,
                                userId = analyticsContext.userId,
                            )
                        )
                    }
            }
    }

    DisposableEffect(screenName) {
        onDispose {
            val maxDepth = reportedMilestones.maxOrNull() ?: 0
            analyticsContext.tracker.track(
                ScrollEvents.sessionSummary(
                    screenName = screenName,
                    maxDepthPercent = maxDepth,
                    sessionId = analyticsContext.sessionId,
                    userId = analyticsContext.userId,
                )
            )
        }
    }
}
