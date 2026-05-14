package com.spbu.projecttrack.analytics.firebase

import com.spbu.projecttrack.analytics.AnalyticsTracker
import com.spbu.projecttrack.analytics.model.AnalyticsEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FirebaseAnalyticsTracker : AnalyticsTracker {
    private var lastUserId: String? = null

    override fun track(event: AnalyticsEvent) {
        val mappedEvent = mapFirebaseEvent(event) ?: return
        syncUserId(event.userId)
        PlatformFirebaseAnalytics.logEvent(mappedEvent.name, mappedEvent.payload)
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        syncUserId(userId)
        properties.forEach { (name, value) ->
            sanitizeUserPropertyName(name)?.let { safeName ->
                PlatformFirebaseAnalytics.setUserProperty(safeName, sanitizeUserPropertyValue(value))
            }
        }
    }

    override fun reset() {
        lastUserId = null
        PlatformFirebaseAnalytics.resetAnalyticsData()
    }

    override suspend fun flush() = Unit

    private fun syncUserId(userId: String?) {
        if (lastUserId == userId) return
        PlatformFirebaseAnalytics.setUserId(userId)
        lastUserId = userId
    }
}

@Serializable
data class FirebaseAnalyticsPayload(
    val strings: Map<String, String> = emptyMap(),
    val longs: Map<String, Long> = emptyMap(),
    val doubles: Map<String, Double> = emptyMap(),
)

data class FirebaseMappedEvent(
    val name: String,
    val payload: FirebaseAnalyticsPayload,
)

private val firebaseJson = Json { encodeDefaults = true }

internal fun FirebaseAnalyticsPayload.toJson(): String = firebaseJson.encodeToString(this)

private fun mapFirebaseEvent(event: AnalyticsEvent): FirebaseMappedEvent? {
    val eventName = mapFirebaseEventName(event.name) ?: return null
    val strings = linkedMapOf<String, String>()
    val longs = linkedMapOf<String, Long>()
    val doubles = linkedMapOf<String, Double>()

    event.properties.forEach { (key, value) ->
        val safeKey = sanitizeFirebaseEventName(key) ?: return@forEach
        when (value) {
            is Int -> longs[safeKey] = value.toLong()
            is Long -> longs[safeKey] = value
            is Float -> doubles[safeKey] = value.toDouble()
            is Double -> doubles[safeKey] = value
            is Boolean -> longs[safeKey] = if (value) 1L else 0L
            else -> strings[safeKey] = sanitizeFirebaseStringValue(value.toString())
        }
    }

    if (!strings.containsKey("analytics_session_id")) {
        strings["analytics_session_id"] = sanitizeFirebaseStringValue(event.sessionId)
    }

    if (eventName == "screen_view") {
        if (!strings.containsKey("screen_class")) {
            strings["screen_class"] = "ComposeScreen"
        }
    }

    return FirebaseMappedEvent(
        name = eventName,
        payload = FirebaseAnalyticsPayload(
            strings = strings,
            longs = longs,
            doubles = doubles,
        ),
    )
}

private fun mapFirebaseEventName(name: String): String? = when (name) {
    "screen_viewed" -> "screen_view"
    else -> sanitizeFirebaseEventName(name)
}

private fun sanitizeFirebaseEventName(name: String): String? {
    if (name.startsWith("$")) return null

    val normalized = buildString(name.length) {
        name.forEach { char ->
            append(
                when {
                    char.isLetterOrDigit() || char == '_' -> char
                    else -> '_'
                }
            )
        }
    }
        .trim('_')
        .ifBlank { return null }

    val prefixed = if (normalized.first().isLetter()) normalized else "p_$normalized"
    val withoutReservedPrefix = when {
        prefixed.startsWith("firebase_") -> "app_${prefixed.removePrefix("firebase_")}"
        prefixed.startsWith("google_") -> "app_${prefixed.removePrefix("google_")}"
        prefixed.startsWith("ga_") -> "app_${prefixed.removePrefix("ga_")}"
        else -> prefixed
    }

    return withoutReservedPrefix.take(40)
}

private fun sanitizeFirebaseStringValue(value: String): String = value.take(100)

private fun sanitizeUserPropertyName(name: String): String? {
    val safe = sanitizeFirebaseEventName(name) ?: return null
    return safe.take(24)
}

private fun sanitizeUserPropertyValue(value: Any?): String? {
    return value?.toString()?.take(36)
}

internal expect object PlatformFirebaseAnalytics {
    fun logEvent(name: String, payload: FirebaseAnalyticsPayload)
    fun setUserId(userId: String?)
    fun setUserProperty(name: String, value: String?)
    fun resetAnalyticsData()
}
