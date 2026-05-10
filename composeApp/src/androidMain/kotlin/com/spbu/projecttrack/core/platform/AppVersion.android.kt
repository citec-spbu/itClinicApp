package com.spbu.projecttrack.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun appVersionName(): String {
    val context = LocalContext.current
    return try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val buildNumber = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        "v${info.versionName} (build $buildNumber)"
    } catch (e: Exception) {
        ""
    }
}
