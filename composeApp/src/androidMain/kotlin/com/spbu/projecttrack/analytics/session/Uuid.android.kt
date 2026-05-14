package com.spbu.projecttrack.analytics.session

import java.util.UUID

actual fun generateUuid(): String = UUID.randomUUID().toString()
