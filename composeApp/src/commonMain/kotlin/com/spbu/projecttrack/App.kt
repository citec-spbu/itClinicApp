package com.spbu.projecttrack

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

// Common App - platform specific implementations in androidMain/iosMain
@Composable
expect fun App(onLaunchReady: () -> Unit = {})
