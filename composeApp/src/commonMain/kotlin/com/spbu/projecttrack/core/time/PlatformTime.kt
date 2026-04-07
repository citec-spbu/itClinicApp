package com.spbu.projecttrack.core.time

expect object PlatformTime {
    fun currentTimeMillis(): Long
}
