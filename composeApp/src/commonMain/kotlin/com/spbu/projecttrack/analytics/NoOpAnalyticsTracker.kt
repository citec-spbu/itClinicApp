package com.spbu.projecttrack.analytics

import com.spbu.projecttrack.analytics.model.AnalyticsEvent

/** Используется в тестах и когда аналитика отключена. */
class NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
    override fun identify(userId: String, properties: Map<String, Any>) = Unit
    override fun reset() = Unit
    override suspend fun flush() = Unit
}
