package com.spbu.projecttrack.analytics.events

import com.spbu.projecttrack.analytics.model.AnalyticsEvent

object ScreenEvents {

    fun viewed(
        screenName: String,
        sessionId: String,
        userId: String?,
        referrer: String? = null,
    ) = AnalyticsEvent(
        name = "screen_viewed",
        properties = buildMap {
            put("screen_name", screenName)
            put("\$screen_name", screenName)
            put("source_screen", screenName)
            referrer?.let { put("referrer", it) }
        },
        sessionId = sessionId,
        userId = userId,
    )

    fun left(
        screenName: String,
        timeOnScreenMs: Long,
        sessionId: String,
        userId: String?,
    ) = AnalyticsEvent(
        name = "screen_left",
        properties = mapOf(
            "screen_name"        to screenName,
            "\$screen_name"      to screenName,
            "source_screen"      to screenName,
            "time_on_screen_ms"  to timeOnScreenMs,
        ),
        sessionId = sessionId,
        userId = userId,
    )
}
