package com.spbu.projecttrack.core.platform

import androidx.compose.runtime.Composable
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
@Composable
actual fun isDebugBuild(): Boolean = Platform.isDebugBinary
