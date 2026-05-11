package com.spbu.projecttrack.core.network

expect object DeviceInfo {
    fun isEmulator(): Boolean
    
    /**
     * Returns the host address the app should use during local development.
     */
    fun getLocalHostAddress(): String
}
