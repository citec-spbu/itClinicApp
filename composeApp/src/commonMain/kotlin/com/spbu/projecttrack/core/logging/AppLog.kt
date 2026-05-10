package com.spbu.projecttrack.core.logging

expect object AppLog {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable)
}
