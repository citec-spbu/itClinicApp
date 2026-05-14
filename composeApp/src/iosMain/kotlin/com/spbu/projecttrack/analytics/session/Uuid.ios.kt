package com.spbu.projecttrack.analytics.session

import platform.Foundation.NSUUID

actual fun generateUuid(): String = NSUUID().UUIDString
