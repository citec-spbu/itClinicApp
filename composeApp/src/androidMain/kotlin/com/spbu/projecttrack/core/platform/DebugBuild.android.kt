package com.spbu.projecttrack.core.platform

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun isDebugBuild(): Boolean {
    val applicationInfo = LocalContext.current.applicationInfo
    return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
