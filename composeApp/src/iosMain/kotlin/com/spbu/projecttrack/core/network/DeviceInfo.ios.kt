package com.spbu.projecttrack.core.network

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSHomeDirectory
import platform.UIKit.UIDevice

actual object DeviceInfo {
    actual fun isEmulator(): Boolean {
        val environment = NSProcessInfo.processInfo.environment
        val homeDirectory = NSHomeDirectory()
        return environment["SIMULATOR_DEVICE_NAME"] != null ||
            environment["SIMULATOR_UDID"] != null ||
            homeDirectory.contains("/CoreSimulator/Devices/") ||
            UIDevice.currentDevice.name.contains("Simulator", ignoreCase = true)
    }
    
    actual fun getLocalHostAddress(): String {
        return if (isEmulator()) {
            // Для iOS симулятора используем IPv4 loopback, чтобы не упираться в IPv6 localhost.
            "127.0.0.1"
        } else {
            // Для реального устройства - используем настроенный IP
            LocalDevConfig.LOCAL_MACHINE_IP
        }
    }
}
