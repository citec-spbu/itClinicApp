package com.spbu.projecttrack.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openExternalUrl(url: String) {
    NSURL.URLWithString(url)?.let { targetUrl ->
        UIApplication.sharedApplication.openURL(
            targetUrl,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null,
        )
    }
}
