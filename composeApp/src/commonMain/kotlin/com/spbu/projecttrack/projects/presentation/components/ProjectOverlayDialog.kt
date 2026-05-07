package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spbu.projecttrack.core.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val ProjectOverlayDialogAnimationDurationMs = 220L
private const val ProjectOverlayDialogBackdropMaxAlpha = 0.86f

@Composable
internal fun ProjectOverlayDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 350.dp,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderColor: androidx.compose.ui.graphics.Color = AppColors.Color1,
    containerColor: androidx.compose.ui.graphics.Color = AppColors.White,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    backgroundContent: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.(dismiss: () -> Unit) -> Unit,
) {
    var renderDialog by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val backdropAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(ProjectOverlayDialogAnimationDurationMs.toInt()),
        label = "project_overlay_backdrop_alpha"
    )
    val panelAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(ProjectOverlayDialogAnimationDurationMs.toInt()),
        label = "project_overlay_panel_alpha"
    )
    val panelScale by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.94f,
        animationSpec = tween(ProjectOverlayDialogAnimationDurationMs.toInt()),
        label = "project_overlay_panel_scale"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            renderDialog = true
            contentVisible = false
            withFrameNanos { }
            contentVisible = true
        } else if (renderDialog) {
            contentVisible = false
            delay(ProjectOverlayDialogAnimationDurationMs)
            renderDialog = false
        }
    }

    fun dismissWithAnimation() {
        if (!contentVisible) return
        contentVisible = false
        scope.launch {
            delay(ProjectOverlayDialogAnimationDurationMs)
            onDismiss()
        }
    }

    if (!renderDialog) return

    val backdropInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Color.Black.copy(
                    alpha = ProjectOverlayDialogBackdropMaxAlpha * backdropAlpha
                )
            )
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = ::dismissWithAnimation
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = maxWidth)
                .wrapContentHeight()
                .graphicsLayer {
                    alpha = panelAlpha
                    scaleX = panelScale
                    scaleY = panelScale
                }
                .clip(shape)
                .background(
                    color = containerColor,
                    shape = shape
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            backgroundContent()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                containerColor.copy(alpha = 0.12f),
                                containerColor.copy(alpha = 0.76f),
                                containerColor.copy(alpha = 0.12f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(contentPadding)
            ) {
                content(::dismissWithAnimation)
            }
        }
    }
}
