package com.spbu.projecttrack.core.auth

import com.spbu.projecttrack.core.network.AuthApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

class MobileAuthApi(
    private val client: HttpClient
) {
    val loginUrl: String
        get() = "${AuthApiConfig.baseUrl}/mobile/githubauthenticate"

    suspend fun primeLoginPrerequisites(): Result<Unit> {
        if (!AuthApiConfig.usesLocalApi) return Result.success(Unit)

        return runCatching {
            client.get(AuthApiConfig.baseUrl)
            Unit
        }
    }

    suspend fun exchangeCode(code: String): Result<MobileAuthSession> {
        return runCatching {
            val response = client.post("${AuthApiConfig.baseUrl}/mobile/exchange") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(MobileAuthCodeRequest(code = code))
            }
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status}")
            }
            response.body()
        }
    }

    suspend fun restoreSession(refreshToken: String): Result<MobileAuthSession> {
        return runCatching {
            val response = client.post("${AuthApiConfig.baseUrl}/mobile/session") {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(MobileAuthRefreshRequest(refreshToken = refreshToken))
            }
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status}")
            }
            response.body()
        }
    }

    suspend fun logout(): Result<Unit> {
        return runCatching {
            val response = client.get("${AuthApiConfig.baseUrl}/logout")
            if (!response.status.isSuccess()) {
                error("HTTP ${response.status}")
            }
            Unit
        }
    }
}

@Serializable
data class MobileAuthSession(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
private data class MobileAuthCodeRequest(
    val code: String,
)

@Serializable
private data class MobileAuthRefreshRequest(
    val refreshToken: String,
)
