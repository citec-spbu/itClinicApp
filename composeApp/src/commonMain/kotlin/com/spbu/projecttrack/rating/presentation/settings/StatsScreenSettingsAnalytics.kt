package com.spbu.projecttrack.rating.presentation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.spbu.projecttrack.analytics.stableAnalyticsHash
import com.spbu.projecttrack.analytics.compose.rememberAnalyticsContext
import com.spbu.projecttrack.analytics.model.AnalyticsEvent

@Composable
internal fun TrackStatsScreenSettingsClose(
    target: StatsScreenSettingsTarget,
    initialSectionIds: List<String>,
    currentSectionIds: List<String>,
    analyticsProjectId: String?,
) {
    val analytics = rememberAnalyticsContext()
    val latestSectionIds by rememberUpdatedState(currentSectionIds)
    val projectIdHash = remember(analyticsProjectId) { analyticsProjectId?.let(::stableAnalyticsHash) }
    val settingsScreenName = remember(target) {
        when (target) {
            StatsScreenSettingsTarget.Project -> "project_stats_settings_screen"
            StatsScreenSettingsTarget.User -> "user_stats_settings_screen"
        }
    }

    DisposableEffect(target, initialSectionIds, projectIdHash, settingsScreenName) {
        onDispose {
            val finalIds = latestSectionIds
            if (finalIds == initialSectionIds) return@onDispose

            val initialSet = initialSectionIds.toSet()
            val finalSet = finalIds.toSet()
            val added = finalIds.filterNot(initialSet::contains)
            val removed = initialSectionIds.filterNot(finalSet::contains)
            val commonInitialOrder = initialSectionIds.filter(finalSet::contains)
            val commonFinalOrder = finalIds.filter(initialSet::contains)
            val orderChanged = commonInitialOrder != commonFinalOrder
            val visibilityChanged = added.isNotEmpty() || removed.isNotEmpty()

            analytics.tracker.track(
                AnalyticsEvent(
                    name = "stats_screen_settings_changed",
                    properties = buildMap {
                        put("screen", settingsScreenName)
                        put("stats_target", target.name.lowercase())
                        projectIdHash?.let { put("project_id_hash", it) }
                        put("initial_section_ids", initialSectionIds.joinToString(","))
                        put("updated_section_ids", finalIds.joinToString(","))
                        put("added_section_ids", added.joinToString(","))
                        put("removed_section_ids", removed.joinToString(","))
                        put("order_changed", orderChanged)
                        put("visibility_changed", visibilityChanged)
                        put("changed_blocks_count", (added + removed).distinct().size)
                    },
                    sessionId = analytics.sessionId,
                    userId = analytics.userId,
                )
            )
        }
    }
}
