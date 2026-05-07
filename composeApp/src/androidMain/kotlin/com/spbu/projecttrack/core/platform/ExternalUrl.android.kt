package com.spbu.projecttrack.core.platform

import android.content.Intent
import android.net.Uri
import com.spbu.projecttrack.AppContextHolder

actual fun openExternalUrl(url: String) {
    val context = AppContextHolder.applicationContext ?: return
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
