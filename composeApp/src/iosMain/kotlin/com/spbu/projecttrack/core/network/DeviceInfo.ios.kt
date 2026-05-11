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
            // Use an explicit IPv4 loopback on the simulator to avoid IPv6 localhost resolution issues.
            "127.0.0.1"
        } else {
            LocalDevConfig.LOCAL_MACHINE_IP
        }
    }
}
