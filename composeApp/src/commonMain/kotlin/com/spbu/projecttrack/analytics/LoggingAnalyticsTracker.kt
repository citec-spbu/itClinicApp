package com.spbu.projecttrack.analytics

import com.spbu.projecttrack.analytics.model.AnalyticsEvent
import com.spbu.projecttrack.core.logging.AppLog

/** Логирует события в консоль — используется в debug-сборках. */
class LoggingAnalyticsTracker : AnalyticsTracker {

    override fun track(event: AnalyticsEvent) {
        AppLog.d(
            "Analytics",
            "[${event.name}] props=${event.properties} session=${event.sessionId} user=${event.userId}",
        )
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        AppLog.d("Analytics", "[identify] userId=$userId props=$properties")
    }

    override fun reset() {
        AppLog.d("Analytics", "[reset]")
    }

    override suspend fun flush() {
        AppLog.d("Analytics", "[flush]")
    }
}
