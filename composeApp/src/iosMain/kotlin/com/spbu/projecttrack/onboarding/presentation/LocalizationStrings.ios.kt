package com.spbu.projecttrack.onboarding.presentation

import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

@Composable
actual fun getLocalizedAppName(): String {
    val languageCode = NSLocale.currentLocale.languageCode
    return when (languageCode) {
        "ru" -> "Citec"
        else -> "Citec"
    }
}

@Composable
actual fun getLocalizedAuthText(): String {
    val languageCode = NSLocale.currentLocale.languageCode
    return when (languageCode) {
        "ru" -> "Авторизация через Git"
        else -> "Login with Git"
    }
}

@Composable
actual fun getLocalizedContinueText(): String {
    val languageCode = NSLocale.currentLocale.languageCode
    return when (languageCode) {
        "ru" -> "Продолжить без авторизации"
        else -> "Continue without authorization"
    }
}









