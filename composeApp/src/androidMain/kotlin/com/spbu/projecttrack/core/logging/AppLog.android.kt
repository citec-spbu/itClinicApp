package com.spbu.projecttrack.core.logging

import android.util.Log

actual object AppLog {
    actual fun d(tag: String, message: String) {
        runSafely { Log.i(tag, message) }
        println("[$tag] $message")
    }

    actual fun e(tag: String, message: String) {
        runSafely { Log.e(tag, message) }
        println("[$tag] ERROR: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        runSafely { Log.e(tag, message, throwable) }
        println("[$tag] ERROR: $message | ${throwable.message}")
    }

    private inline fun runSafely(block: () -> Unit) {
        try {
            block()
        } catch (_: RuntimeException) {
            // Local JVM tests do not mock android.util.Log.
        }
    }
}
