package com.spbu.projecttrack.analytics.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.spbu.projecttrack.analytics.AnalyticsTracker
import com.spbu.projecttrack.analytics.analyticsUserId
import com.spbu.projecttrack.analytics.session.AnalyticsSession

@Composable
fun BindAnalyticsIdentity(
    rawUserId: String?,
    analyticsTracker: AnalyticsTracker,
    analyticsSession: AnalyticsSession,
) {
    val safeUserId = remember(rawUserId) {
        rawUserId?.trim()?.takeIf { it.isNotEmpty() }?.let(::analyticsUserId)
    }
    var hasAppliedIdentity by remember { mutableStateOf(false) }

    LaunchedEffect(safeUserId, analyticsTracker, analyticsSession) {
        val trackedUserId = analyticsSession.userId

        if (safeUserId != null) {
            if (!hasAppliedIdentity || trackedUserId != safeUserId) {
                analyticsSession.setUserId(safeUserId)
                analyticsTracker.identify(safeUserId)
            }
        } else if (trackedUserId != null) {
            analyticsSession.setUserId(null)
            analyticsTracker.flush()
            analyticsTracker.reset()
        }

        hasAppliedIdentity = true
    }
}
