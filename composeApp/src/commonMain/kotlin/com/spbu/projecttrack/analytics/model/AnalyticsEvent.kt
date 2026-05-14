package com.spbu.projecttrack.analytics.model

import com.spbu.projecttrack.core.time.PlatformTime

data class AnalyticsEvent(
    val name: String,
    val properties: Map<String, Any> = emptyMap(),
    val timestamp: Long = PlatformTime.currentTimeMillis(),
    val sessionId: String,
    val userId: String?,
)
