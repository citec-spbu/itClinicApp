package com.spbu.projecttrack.core.network

import com.spbu.projecttrack.BuildConfig

object ApiConfig {
    private val USE_LOCAL_API = BuildConfig.USE_LOCAL_API
    private val PRODUCTION_BASE_URL = BuildConfig.PRODUCTION_BASE_URL
    private val LOCAL_PORT = BuildConfig.LOCAL_PORT
    
    /**
     * Resolves the API base URL for emulators, real devices, and manual host overrides.
     */
    val baseUrl: String
        get() = if (USE_LOCAL_API) {
            val host = NetworkSettings.getEffectiveHostIP()
            "http://$host:$LOCAL_PORT"
        } else {
            PRODUCTION_BASE_URL
        }
    
    val isLocalApi: Boolean
        get() = USE_LOCAL_API
    
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== API Configuration ===")
            appendLine("Use Local API: $USE_LOCAL_API")
            appendLine("Is Emulator: ${DeviceInfo.isEmulator()}")
            appendLine("Local Host: ${DeviceInfo.getLocalHostAddress()}")
            appendLine("Base URL: $baseUrl")
            appendLine("=======================")
        }
    }
    
    object AuthRequired {
        const val USER_PROJECT_STATUS = "/user/projectstatus"
        const val USER_PROFILE = "/user/profile"
        const val USER_ME = "/user"
        
        const val REQUEST_CREATE = "/request"
        const val REQUEST_EDIT = "/request"
        const val REQUEST_AVAILABLE = "/request/available"
        const val REQUEST_DELETE = "/request/{id}"
        
        const val PROJECT_RESULTS_CHANGE = "/project/results/change-file"
        const val PROJECT_RESULTS_DELETE = "/project/results/delete-file"
        const val PROJECT_RESULTS_UPLOAD = "/project/results/upload-file"
        
        const val PROJECT_LINKS_ADD = "/project/links"
        const val PROJECT_LINKS_DELETE = "/project/links/{id}"
        
        const val PROFILE_EDIT_ACCOUNT = "/profile/account"
        const val PROFILE_EDIT_PERSONAL = "/profile/personal"
        
        const val MEMBER_EDIT = "/member"
    }
    
    object Public {
        const val PROJECT_ACTIVE = "/project/active"
        const val PROJECT_NEW = "/project/new"
        const val PROJECT_FINDMANY = "/project/findmany"
        const val PROJECT_DETAIL = "/project/{slug}"
        const val USER_ROLE = "/user/role"
        const val TAGS_ALL = "/tag"
        const val EMAIL_SEND_REQUEST = "/email"
    }
}
