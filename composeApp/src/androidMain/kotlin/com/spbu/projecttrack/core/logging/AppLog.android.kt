package com.spbu.projecttrack.core.logging

import android.util.Log

actual object AppLog {
    actual fun d(tag: String, message: String) {
        Log.i(tag, message)
        println("[$tag] $message")
    }

    actual fun e(tag: String, message: String) {
        Log.e(tag, message)
        println("[$tag] ERROR: $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        println("[$tag] ERROR: $message | ${throwable.message}")
    }
}
