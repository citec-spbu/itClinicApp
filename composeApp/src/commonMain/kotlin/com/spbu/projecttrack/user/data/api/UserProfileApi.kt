package com.spbu.projecttrack.user.data.api

import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.network.ApiConfig
import com.spbu.projecttrack.user.data.model.UserProfileResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class UserProfileApi(private val client: HttpClient) {

    private val logTag = "UserProfileApi"
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun buildHttpError(status: HttpStatusCode): Exception {
        return IllegalStateException("HTTP ${status.value} ${status.description}")
    }

    suspend fun getProfile(): Result<UserProfileResponse> {
        return try {
            val endpoint = ApiConfig.AuthRequired.USER_PROFILE
            val url = "${ApiConfig.baseUrl}$endpoint"

            AppLog.d(logTag, "GET $url")

            val response = client.get(url) {
                accept(ContentType.Application.Json)
            }

            val bodyText = response.bodyAsText()
            AppLog.d(logTag, "Status: ${response.status}")
            AppLog.d(logTag, "Body: ${bodyText.take(500)}")

            if (!response.status.isSuccess()) {
                return Result.failure(buildHttpError(response.status))
            }

            val parsed = json.decodeFromString(UserProfileResponse.serializer(), bodyText)
            Result.success(parsed)
        } catch (e: Exception) {
            AppLog.e(logTag, "Request failed", e)
            Result.failure(e)
        }
    }
}
