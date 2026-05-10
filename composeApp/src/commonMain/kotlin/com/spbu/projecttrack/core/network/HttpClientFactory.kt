package com.spbu.projecttrack.core.network

import io.ktor.client.*

expect object HttpClientFactory {
    fun create(): HttpClient
}

