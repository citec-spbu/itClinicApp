package com.spbu.projecttrack

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController(onLaunchReady: () -> Unit = {}) = ComposeUIViewController {
    App(onLaunchReady = onLaunchReady)
}
