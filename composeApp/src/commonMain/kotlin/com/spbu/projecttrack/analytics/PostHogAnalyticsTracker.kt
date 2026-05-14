package com.spbu.projecttrack.analytics

import com.spbu.projecttrack.analytics.model.AnalyticsEvent
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.time.PlatformTime
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * PostHog трекер через Ktor HTTP API.
 * Работает на обеих платформах без платформо-специфичных SDK.
 *
 * Батчит события и отправляет каждые [flushIntervalMs] мс или когда буфер достигает [maxBatchSize].
 */
class PostHogAnalyticsTracker(
    private val apiKey: String,
    private val host: String = "https://eu.i.posthog.com",
    private val httpClient: HttpClient,
    private val flushIntervalMs: Long = 30_000L,
    private val maxBatchSize: Int = 20,
) : AnalyticsTracker {

    private val scope = CoroutineScope(SupervisorJob())
    private val mutex = Mutex()
    private val buffer = mutableListOf<AnalyticsEvent>()
    private var distinctId: String = "anonymous"
    private var flushJob: Job? = null

    init {
        startPeriodicFlush()
    }

    private fun startPeriodicFlush() {
        flushJob = scope.launch {
            while (isActive) {
                delay(flushIntervalMs)
                flushInternal()
            }
        }
    }

    override fun track(event: AnalyticsEvent) {
        scope.launch {
            val shouldFlush = mutex.withLock {
                buffer.add(event)
                buffer.size >= maxBatchSize
            }

            if (shouldFlush) {
                flushInternal()
            }
        }
    }

    override fun identify(userId: String, properties: Map<String, Any>) {
        distinctId = userId
        scope.launch {
            sendBatch(
                listOf(
                    buildJsonObject {
                        put("type", "identify")
                        put("distinct_id", userId)
                        put("timestamp", isoTimestamp(PlatformTime.currentTimeMillis()))
                        put(
                            "${'$'}set",
                            buildJsonObject {
                                properties.forEach { (k, v) -> put(k, v.toJsonPrimitive()) }
                            }
                        )
                    }
                )
            )
        }
    }

    override fun reset() {
        distinctId = "anonymous"
        scope.launch {
            mutex.withLock { buffer.clear() }
        }
    }

    override suspend fun flush() = flushInternal()

    private suspend fun flushInternal() {
        val batch = mutex.withLock {
            if (buffer.isEmpty()) return
            val copy = buffer.toList()
            buffer.clear()
            copy
        }
        sendBatch(batch.map { it.toPostHogJson(distinctId) })
    }

    private suspend fun sendBatch(events: List<JsonObject>) {
        if (events.isEmpty()) return
        try {
            val body = buildJsonObject {
                put("api_key", apiKey)
                putJsonArray("batch") {
                    events.forEach { add(it) }
                }
            }
            val response = httpClient.post("$host/batch/") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(JsonObject.serializer(), body))
            }
            if (response.status.value !in 200..299) {
                AppLog.d(
                    "PostHogAnalytics",
                    "Batch send failed: HTTP ${response.status.value} ${response.body<String>()}",
                )
            }
        } catch (e: Exception) {
            AppLog.d("PostHogAnalytics", "Batch send failed: ${e.message}")
        }
    }

    private fun AnalyticsEvent.toPostHogJson(distinctId: String): JsonObject =
        buildJsonObject {
            put("type", "capture")
            put("event", name)
            put("timestamp", isoTimestamp(timestamp))
            put(
                "properties",
                buildJsonObject {
                    put("distinct_id", userId ?: distinctId)
                    put("session_id", sessionId)
                    properties.forEach { (k, v) -> put(k, v.toJsonPrimitive()) }
                }
            )
        }

    private fun Any.toJsonPrimitive(): JsonPrimitive = when (this) {
        is String  -> JsonPrimitive(this)
        is Int     -> JsonPrimitive(this)
        is Long    -> JsonPrimitive(this)
        is Float   -> JsonPrimitive(this)
        is Double  -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else       -> JsonPrimitive(this.toString())
    }

    /** Конвертирует Unix ms в ISO-8601 строку. */
    private fun isoTimestamp(ms: Long): String {
        val seconds = ms / 1000
        val millis  = ms % 1000
        // Простая ISO-8601 UTC строка без внешних зависимостей
        return buildString {
            val epochDays = seconds / 86400
            val timeOfDay = seconds % 86400
            val h = timeOfDay / 3600
            val m = (timeOfDay % 3600) / 60
            val s = timeOfDay % 60

            // Вычисляем дату из Unix epoch (2000-01-01 = день 10957)
            val z = epochDays + 719468
            val era = (if (z >= 0) z else z - 146096) / 146097
            val doe = z - era * 146097
            val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
            val y = yoe + era * 400
            val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
            val mp = (5 * doy + 2) / 153
            val d = doy - (153 * mp + 2) / 5 + 1
            val month = mp + (if (mp < 10) 3 else -9)
            val year = y + (if (month <= 2) 1 else 0)

            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(d.toString().padStart(2, '0'))
            append('T')
            append(h.toString().padStart(2, '0'))
            append(':')
            append(m.toString().padStart(2, '0'))
            append(':')
            append(s.toString().padStart(2, '0'))
            append('.')
            append(millis.toString().padStart(3, '0'))
            append('Z')
        }
    }
}
