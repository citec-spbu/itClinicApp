package com.spbu.projecttrack.user.data.api

import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.network.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.encodeURLPath
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class UserRoleApi(private val client: HttpClient) {

    private val logTag = "UserRoleApi"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getSuggestions(query: String): Result<List<String>> {
        return try {
            val normalizedQuery = query.trim()
            val pathSuffix = if (normalizedQuery.isBlank()) "/" else "/${normalizedQuery.encodeURLPath()}"
            val url = "${ApiConfig.baseUrl}${ApiConfig.Public.USER_ROLE}$pathSuffix"
            AppLog.d(logTag, "GET $url")

            val response = client.get(url) {
                accept(ContentType.Application.Json)
            }

            val bodyText = response.bodyAsText()
            AppLog.d(logTag, "Status: ${response.status.value} ${response.status.description}")
            AppLog.d(logTag, "Response body: ${bodyText.take(500)}")

            if (!response.status.isSuccess()) {
                return Result.failure(
                    IllegalStateException("HTTP ${response.status.value} ${response.status.description}: $bodyText")
                )
            }

            Result.success(json.decodeFromString<List<String>>(bodyText))
        } catch (e: Exception) {
            AppLog.e(logTag, "Failed to load role suggestions", e)
            Result.failure(e)
        }
    }
}
