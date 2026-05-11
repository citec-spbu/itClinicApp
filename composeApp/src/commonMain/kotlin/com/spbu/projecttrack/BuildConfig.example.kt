package com.spbu.projecttrack

/**
 * Template for the local `BuildConfig.kt`.
 *
 * Copy this file to `BuildConfig.kt`, fill in your values, and keep the real
 * file uncommitted.
 */
object BuildConfigExample {
    const val TEST_TOKEN = "your_test_token_here"
    const val USE_LOCAL_API = true
    const val PRODUCTION_BASE_URL = "https://citec.spb.ru/api"
    const val AUTH_PRODUCTION_BASE_URL = "https://citec.spb.ru/auth"
    const val LOCAL_PORT = 8000
    const val AUTH_LOCAL_PORT = 3000

    /**
     * LAN IP of the developer machine for real devices.
     *
     * GitHub OAuth requires the auth host to match the registered callback
     * host, so using the actual LAN IP works better than `10.0.2.2`.
     */
    const val LOCAL_HOST_IP = ""
    const val METRIC_PRODUCTION_BASE_URL = "https://metrics.example.com"
    const val METRIC_LOCAL_PORT = 4173
    const val MOBILE_AUTH_REDIRECT_URL = "itclinicapp://auth/callback"
    const val GITHUB_CLIENT_ID = "your_github_client_id"
    const val GITHUB_CLIENT_SECRET = "your_github_client_secret"
}
