package com.spbu.projecttrack.analytics

import com.spbu.projecttrack.analytics.model.AnalyticsEvent

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
    fun identify(userId: String, properties: Map<String, Any> = emptyMap())
    fun reset()
    suspend fun flush()
}
