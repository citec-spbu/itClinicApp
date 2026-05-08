package com.spbu.projecttrack.debug

object AndroidSplashDebugLock {
    @Volatile
    private var keepSystemSplashOnScreen = false

    fun engage() {
        keepSystemSplashOnScreen = true
    }

    fun release() {
        keepSystemSplashOnScreen = false
    }

    fun shouldKeepSystemSplashOnScreen(): Boolean = keepSystemSplashOnScreen
}
