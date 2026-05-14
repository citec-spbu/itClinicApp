package com.spbu.projecttrack.analytics

import com.spbu.projecttrack.analytics.model.AnalyticsEvent

/**
 * Fan-out трекер — отправляет каждое событие во все зарегистрированные трекеры.
 * Используется для одновременной работы с PostHog + LoggingTracker.
 */
class CompositeAnalyticsTracker(
    private val trackers: List<AnalyticsTracker>,
) : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        trackers.forEach { it.track(event) }
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        trackers.forEach { it.identify(userId, properties) }
    }

    override fun reset() {
        trackers.forEach { it.reset() }
    }

    override suspend fun flush() {
        trackers.forEach { it.flush() }
    }
}
