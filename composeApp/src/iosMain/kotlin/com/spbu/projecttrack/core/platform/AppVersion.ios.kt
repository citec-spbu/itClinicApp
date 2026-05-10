package com.spbu.projecttrack.core.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSBundle

@Composable
actual fun appVersionName(): String {
    val info = NSBundle.mainBundle.infoDictionary ?: return ""
    val version = info["CFBundleShortVersionString"] as? String ?: return ""
    val build = info["CFBundleVersion"] as? String ?: return "v$version"
    return "v$version (build $build)"
}
