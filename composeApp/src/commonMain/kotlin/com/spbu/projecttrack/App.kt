package com.spbu.projecttrack

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*

@Composable
expect fun App(onLaunchReady: () -> Unit = {})
