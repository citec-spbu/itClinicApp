package com.spbu.projecttrack.core.network

import android.os.Build

actual object DeviceInfo {
    actual fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val product = Build.PRODUCT.orEmpty()

        val isEmulator = (
            fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("sdk_gphone")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || device.contains("emulator")
                || device.contains("emu64")
                || product.contains("sdk_gphone")
                || product.contains("sdk")
                || (brand.startsWith("generic") && device.startsWith("generic"))
                || "google_sdk" == product
            )
        
        println("🔍 Device Info:")
        println("   FINGERPRINT: $fingerprint")
        println("   MODEL: $model")
        println("   MANUFACTURER: $manufacturer")
        println("   BRAND: $brand")
        println("   DEVICE: $device")
        println("   PRODUCT: $product")
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
