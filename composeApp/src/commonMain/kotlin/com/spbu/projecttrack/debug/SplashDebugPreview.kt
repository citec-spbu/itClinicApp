package com.spbu.projecttrack.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.spbu_logo
import androidx.compose.ui.geometry.Offset

object SplashDebugPreviewConfig {
    /**
     * Keeps the splash screen visible in debug builds.
     * Dismiss it with a long press after launch.
     */
    const val enabled = false
}

fun shouldShowSplashDebugPreview(isDebugBuild: Boolean): Boolean {
    return isDebugBuild && SplashDebugPreviewConfig.enabled
}

@Composable
fun AppLaunchSplashScreen(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.White)
    ) {
        Image(
            painter = painterResource(Res.drawable.spbu_logo),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        Text(
            text = "Citec",
            modifier = Modifier.align(Alignment.Center),
            color = AppColors.Color3,
            fontFamily = AppFonts.Philosopher,
            fontSize = 54.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(
                    color = AppColors.Black.copy(alpha = 0.18f),
                    offset = Offset(0f, 4f),
                    blurRadius = 10f,
                )
            ),
        )
    }
}

@Composable
fun SplashDebugPreviewOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onDismiss) {
                detectTapGestures(onLongPress = { onDismiss() })
            }
    ) {
        AppLaunchSplashScreen()
    }
}
