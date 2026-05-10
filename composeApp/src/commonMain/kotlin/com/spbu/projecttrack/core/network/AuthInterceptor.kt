package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.core.auth.AuthManager
import com.spbu.projecttrack.core.logging.AppLog
import io.ktor.client.plugins.api.*

/**
 * Плагин для автоматического добавления токена авторизации к запросам
 */
val AuthInterceptor = createClientPlugin("AuthInterceptor") {
    onRequest { request, _ ->
        // Если есть токен, добавляем его в заголовок Authorization
        val token = AuthManager.getToken()
        val path = request.url.build().encodedPath
        if (!token.isNullOrBlank()) {
            request.headers.append("Authorization", "Bearer $token")
            if (path.contains("/user/")) {
                AppLog.d("AuthInterceptor", "Auth header set for $path (len=${token.length})")
            }
        } else {
            if (path.contains("/user/")) {
                AppLog.d("AuthInterceptor", "No token for $path")
            }
        }
    }
}






