package com.spbu.projecttrack.core.auth

import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthDeepLinkBridge {
    private val _redirects = MutableStateFlow<String?>(null)
    val redirects: StateFlow<String?> = _redirects.asStateFlow()

    fun emit(url: String) {
        _redirects.value = url
    }

    fun clear(url: String? = null) {
        if (url == null || _redirects.value == url) {
            _redirects.value = null
        }
    }

    fun extractCode(url: String): String? {
        return runCatching { Url(url).parameters["code"] }.getOrNull()
    }
}

fun handleIncomingAuthRedirect(url: String) {
    AuthDeepLinkBridge.emit(url)
}
