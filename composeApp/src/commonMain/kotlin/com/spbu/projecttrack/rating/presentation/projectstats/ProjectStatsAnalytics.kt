package com.spbu.projecttrack.rating.presentation.projectstats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.spbu.projecttrack.analytics.analyticsHashOrNull
import com.spbu.projecttrack.analytics.stableAnalyticsHash
import com.spbu.projecttrack.analytics.compose.AnalyticsContext
import com.spbu.projecttrack.analytics.compose.BlockTapSnapshot
import com.spbu.projecttrack.analytics.compose.BlockVisibilitySnapshot
import com.spbu.projecttrack.analytics.model.AnalyticsEvent
import com.spbu.projecttrack.analytics.model.BlockType
import com.spbu.projecttrack.core.time.PlatformTime
import com.spbu.projecttrack.rating.presentation.settings.StatsScreenSection

private const val ProjectStatsScreenName = "project_stats_screen"

internal class ProjectStatsAnalyticsSession(
    private val analyticsContext: AnalyticsContext,
    private val projectIdHash: String,
    private val screenName: String = ProjectStatsScreenName,
    initialRepositoryId: String,
    initialStartIsoDate: String,
    initialEndIsoDate: String,
    initialRapidThresholdMinutes: Int,
) {
    private val openedAtMs = PlatformTime.currentTimeMillis()
    private val impressedBlocks = linkedSetOf<String>()
    private val focusedBlocks = linkedSetOf<String>()
    private val tappedBlocks = linkedSetOf<String>()
    private var maxScrollPercent = 0
    private var currentRepositoryIdHash = stableAnalyticsHash(initialRepositoryId)
    private var currentDateRangeKey = "$initialStartIsoDate|$initialEndIsoDate"
    private var currentRapidThresholdMinutes = initialRapidThresholdMinutes

    fun onScrollTracked(depthPercent: Int) {
        maxScrollPercent = maxOf(maxScrollPercent, depthPercent)
    }

    fun onBlockViewed(snapshot: BlockVisibilitySnapshot) {
        impressedBlocks += snapshot.blockId
    }

    fun onBlockFocused(
        snapshot: BlockVisibilitySnapshot,
        position: Int,
    ) {
        if (!focusedBlocks.add(snapshot.blockId)) return
        analyticsContext.tracker.track(
            AnalyticsEvent(
                name = "metric_block_focus",
                properties = mapOf(
                    "screen" to screenName,
                    "project_id_hash" to projectIdHash,
                    "block_id" to snapshot.blockId,
                    "block_type" to snapshot.blockType.key,
                    "position" to position,
                    "duration_ms" to snapshot.durationMs,
                    "max_visible_percent" to (snapshot.maxVisibleRatio * 100).toInt().coerceIn(0, 100),
                ),
                sessionId = analyticsContext.sessionId,
                userId = analyticsContext.userId,
            )
        )
    }

    fun onBlockTapped(
        snapshot: BlockTapSnapshot,
        position: Int,
    ) {
        tappedBlocks += snapshot.blockId
        track(
            name = "metric_block_tap",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
                "block_id" to snapshot.blockId,
                "block_type" to snapshot.blockType.key,
                "position" to position,
                "action" to snapshot.action,
            ),
        )
    }

    fun onRepositorySelected(repositoryId: String) {
        val repositoryIdHash = stableAnalyticsHash(repositoryId)
        if (repositoryIdHash == currentRepositoryIdHash) return
        currentRepositoryIdHash = repositoryIdHash
        track(
            name = "stats_repository_changed",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
                "repository_id_hash" to repositoryIdHash,
            ),
        )
    }

    fun onDateRangeSelected(
        startIsoDate: String,
        endIsoDate: String,
    ) {
        val dateRangeKey = "$startIsoDate|$endIsoDate"
        if (dateRangeKey == currentDateRangeKey) return
        currentDateRangeKey = dateRangeKey
        track(
            name = "stats_date_range_changed",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
                "start_iso_date" to startIsoDate,
                "end_iso_date" to endIsoDate,
            ),
        )
    }

    fun onRapidThresholdChanged(
        days: Int,
        hours: Int,
        minutes: Int,
    ) {
        val totalMinutes = (days.coerceAtLeast(0) * 24 * 60) +
            (hours.coerceAtLeast(0) * 60) +
            minutes.coerceAtLeast(0)
        if (totalMinutes == currentRapidThresholdMinutes) return
        currentRapidThresholdMinutes = totalMinutes
        track(
            name = "stats_rapid_threshold_changed",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
                "days" to days.coerceAtLeast(0),
                "hours" to hours.coerceAtLeast(0),
                "minutes" to minutes.coerceAtLeast(0),
                "total_minutes" to totalMinutes,
            ),
        )
    }

    fun onDetailsOpened(section: StatsScreenSection) {
        track(
            name = "stats_detail_opened",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
                "section_id" to section.id,
            ),
        )
    }

    fun onSettingsOpened() {
        track(
            name = "stats_settings_opened",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
            ),
        )
    }

    fun onExportRequested(
        format: String,
        scope: String,
        sectionId: String? = null,
        participantId: String? = null,
    ) {
        track(
            name = "stats_export_requested",
            properties = buildMap {
                put("screen", screenName)
                put("project_id_hash", projectIdHash)
                put("format", format)
                put("scope", scope)
                sectionId?.let { put("section_id", it) }
                analyticsHashOrNull(participantId)?.let { put("participant_id_hash", it) }
            },
        )
    }

    fun onMemberStatsOpened(memberId: String?) {
        track(
            name = "project_stats_member_opened",
            properties = buildMap {
                put("screen", screenName)
                put("project_id_hash", projectIdHash)
                analyticsHashOrNull(memberId)?.let { put("member_id_hash", it) }
            },
        )
    }

    fun onScreenClosed() {
        track(
            name = "project_stats_screen_close",
            properties = mapOf(
                "screen" to screenName,
                "project_id_hash" to projectIdHash,
                "session_duration_ms" to (PlatformTime.currentTimeMillis() - openedAtMs),
                "max_scroll_percent" to maxScrollPercent,
                "impressed_blocks_count" to impressedBlocks.size,
                "focused_blocks_count" to focusedBlocks.size,
                "tapped_blocks_count" to tappedBlocks.size,
            ),
        )
    }

    private fun track(
        name: String,
        properties: Map<String, Any>,
    ) {
        analyticsContext.tracker.track(
            AnalyticsEvent(
                name = name,
                properties = properties,
                sessionId = analyticsContext.sessionId,
                userId = analyticsContext.userId,
            )
        )
    }
}

@Composable
internal fun rememberProjectStatsAnalyticsSession(
    analyticsContext: AnalyticsContext,
    projectId: String,
    initialRepositoryId: String,
    initialStartIsoDate: String,
    initialEndIsoDate: String,
    initialRapidThresholdMinutes: Int,
): ProjectStatsAnalyticsSession {
    val projectIdHash = remember(projectId) { stableAnalyticsHash(projectId) }
    val session = remember(analyticsContext, projectIdHash) {
        ProjectStatsAnalyticsSession(
            analyticsContext = analyticsContext,
            projectIdHash = projectIdHash,
            initialRepositoryId = initialRepositoryId,
            initialStartIsoDate = initialStartIsoDate,
            initialEndIsoDate = initialEndIsoDate,
            initialRapidThresholdMinutes = initialRapidThresholdMinutes,
        )
    }

    DisposableEffect(session) {
        onDispose {
            session.onScreenClosed()
        }
    }

    return session
}

internal data class MetricBlockSpec(
    val blockId: String,
    val blockType: BlockType,
    val position: Int,
)
