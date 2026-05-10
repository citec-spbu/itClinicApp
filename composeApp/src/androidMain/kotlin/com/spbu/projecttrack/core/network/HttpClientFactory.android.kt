package com.spbu.projecttrack.core.network

import io.ktor.client.engine.okhttp.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

actual object HttpClientFactory {
    
    actual fun create(): HttpClient {
        return HttpClient(OkHttp) {
            // Настройка таймаутов
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000 // 30 секунд
                connectTimeoutMillis = 15_000 // 15 секунд
                socketTimeoutMillis = 30_000  // 30 секунд
            }
            
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }
            
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL // Больше информации для отладки
            }

            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            
            // Автоматическое добавление токена авторизации
            install(AuthInterceptor)
            
            // Настройка движка OkHttp
            engine {
                config {
                    connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        }
    }
}


