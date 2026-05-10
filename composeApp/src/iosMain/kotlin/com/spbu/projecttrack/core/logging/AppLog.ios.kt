package com.spbu.projecttrack.core.logging

import platform.Foundation.NSLog

actual object AppLog {
    actual fun d(tag: String, message: String) {
        NSLog("DEBUG [$tag] $message")
    }

    actual fun e(tag: String, message: String) {
        NSLog("ERROR [$tag] $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        val details = throwable.message ?: "no message"
        NSLog("ERROR [$tag] $message\n$details")
    }
}
