package com.spbu.projecttrack.core.network

import android.os.Build

actual object DeviceInfo {
    actual fun isEmulator(): Boolean {
        val isEmulator = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("sdk_gphone")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.DEVICE.contains("emulator")
                || Build.DEVICE.contains("emu64")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("sdk")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
        
        println("🔍 Device Info:")
        println("   FINGERPRINT: ${Build.FINGERPRINT}")
        println("   MODEL: ${Build.MODEL}")
        println("   MANUFACTURER: ${Build.MANUFACTURER}")
        println("   BRAND: ${Build.BRAND}")
        println("   DEVICE: ${Build.DEVICE}")
        println("   PRODUCT: ${Build.PRODUCT}")
        println("   isEmulator: $isEmulator")
        
        return isEmulator
    }
    
    actual fun getLocalHostAddress(): String {
        val emulator = isEmulator()
        return if (emulator) {
            println("✅ Эмулятор обнаружен → используем 10.0.2.2")
            "10.0.2.2"
        } else {
            println("📱 Реальное устройство → автоопределение IP")
            val hostIP = LocalDevConfig.getHostIP()
            println("📱 IP хоста: $hostIP")
            hostIP
        }
    }
}
