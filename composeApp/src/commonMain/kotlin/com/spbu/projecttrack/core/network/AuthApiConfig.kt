package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.BuildConfig

object AuthApiConfig {
    private val USE_LOCAL_API = BuildConfig.USE_LOCAL_API
    private val PRODUCTION_BASE_URL = BuildConfig.AUTH_PRODUCTION_BASE_URL
    private val LOCAL_PORT = BuildConfig.AUTH_LOCAL_PORT
    private val LOCAL_HOST_IP = BuildConfig.LOCAL_HOST_IP.trim()

    val baseUrl: String
        get() = if (USE_LOCAL_API) {
            // GitHub OAuth callback returns to a fixed web host, so auth must start
            // from the same host as the registered callback URL. Using 10.0.2.2 on
            // Android emulators breaks signed cookie continuity across the redirect.
            val host = if (LOCAL_HOST_IP.isNotEmpty()) {
                LOCAL_HOST_IP
            } else {
                NetworkSettings.getEffectiveHostIP()
            }
            "http://$host:$LOCAL_PORT/auth"
        } else {
            PRODUCTION_BASE_URL.trimEnd('/')
        }

    val usesLocalApi: Boolean
        get() = USE_LOCAL_API
}
