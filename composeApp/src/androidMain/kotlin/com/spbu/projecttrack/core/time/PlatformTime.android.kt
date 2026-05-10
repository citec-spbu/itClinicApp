package com.spbu.projecttrack.core.time

actual object PlatformTime {
    actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
