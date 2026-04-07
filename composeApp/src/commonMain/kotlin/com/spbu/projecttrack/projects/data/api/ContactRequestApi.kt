package com.spbu.projecttrack.projects.data.api

import com.spbu.projecttrack.core.network.ApiConfig
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.projects.data.model.ContactRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

class ContactRequestApi(private val client: HttpClient) {

    private val logTag = "ContactRequestApi"

    private fun buildHttpError(status: HttpStatusCode): Exception {
        return IllegalStateException("HTTP ${status.value} ${status.description}")
    }

    suspend fun sendRequest(name: String, email: String): Result<Unit> {
        return try {
            val endpoint = ApiConfig.Public.EMAIL_SEND_REQUEST
            val url = "${ApiConfig.baseUrl}$endpoint"

            AppLog.d(logTag, "POST $url")
            AppLog.d(logTag, "Body: name=$name, email=$email")

            val response = client.post(url) {
                accept(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(ContactRequest(name = name, email = email))
            }

            val bodyText = response.bodyAsText()
            AppLog.d(logTag, "Status: ${response.status}")
            AppLog.d(logTag, "Body: ${bodyText.take(500)}")

            if (!response.status.isSuccess()) {
                return Result.failure(buildHttpError(response.status))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            AppLog.e(logTag, "Request failed", e)
            Result.failure(e)
        }
    }
}
