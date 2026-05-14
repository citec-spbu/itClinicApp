package com.spbu.projecttrack.analytics.events

import com.spbu.projecttrack.analytics.model.AnalyticsEvent

object ScrollEvents {

    fun depthReached(
        screenName: String,
        depthPercent: Int,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "scroll_depth_reached",
        properties = mapOf(
            "screen_name"   to screenName,
            "\$screen_name" to screenName,
            "source_screen" to screenName,
            "depth_percent" to depthPercent,
        ),
        sessionId = sessionId,
        userId = userId,
    )

    fun sessionSummary(
        screenName: String,
        maxDepthPercent: Int,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "scroll_session_summary",
        properties = mapOf(
            "screen_name"       to screenName,
            "\$screen_name"     to screenName,
            "source_screen"     to screenName,
            "max_depth_percent" to maxDepthPercent,
        ),
        sessionId = sessionId,
        userId = userId,
    )
}
