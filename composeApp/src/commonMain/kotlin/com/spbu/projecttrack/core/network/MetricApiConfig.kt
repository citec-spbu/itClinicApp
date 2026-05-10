package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.BuildConfig

/**
 * Конфигурация API для метрик
 */
object MetricApiConfig {
    private val USE_LOCAL_API = BuildConfig.USE_LOCAL_API
    private val PRODUCTION_BASE_URL = BuildConfig.METRIC_PRODUCTION_BASE_URL
    private val LOCAL_PORT = BuildConfig.METRIC_LOCAL_PORT

    val baseUrl: String
        get() = if (USE_LOCAL_API) {
            val host = NetworkSettings.getEffectiveHostIP()
            "http://$host:$LOCAL_PORT"
        } else {
            PRODUCTION_BASE_URL
        }
}
