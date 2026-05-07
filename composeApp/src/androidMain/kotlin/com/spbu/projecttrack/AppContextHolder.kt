package com.spbu.projecttrack

import android.content.Context

object AppContextHolder {
    var applicationContext: Context? = null
        private set

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}
