package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.close_icon
import projecttrack.composeapp.generated.resources.spbu_logo
import androidx.compose.material3.Text

private const val SuggestProjectResultAlertExitDurationMs = 180L

@Composable
fun SuggestProjectResultAlert(
    isVisible: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renderDialog by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val dimAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 0.18f else 0f,
        animationSpec = tween(220),
        label = "suggest_project_result_dim_alpha"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            renderDialog = true
            contentVisible = true
        } else if (renderDialog) {
            contentVisible = false
            delay(SuggestProjectResultAlertExitDurationMs)
            renderDialog = false
        }
    }

    fun dismissWithAnimation() {
        if (!contentVisible) return
        contentVisible = false
        scope.launch {
            delay(SuggestProjectResultAlertExitDurationMs)
            onDismiss()
        }
    }

    if (!renderDialog) return

    Dialog(
        onDismissRequest = ::dismissWithAnimation,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val backdropInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
        ) {
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .clickable(
                    interactionSource = backdropInteraction,
                    indication = null,
                    onClick = ::dismissWithAnimation
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(220)) + scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(220)
                ),
                exit = fadeOut(animationSpec = tween(SuggestProjectResultAlertExitDurationMs.toInt())) + scaleOut(
                    targetScale = 0.97f,
                    animationSpec = tween(SuggestProjectResultAlertExitDurationMs.toInt())
                )
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth(0.88f)
                        .widthIn(max = 320.dp)
                        .wrapContentHeight()
                        .background(
                            color = AppColors.White,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = AppColors.Color1,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Image(
                        painter = painterResource(Res.drawable.spbu_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .alpha(0.95f)
                            .align(Alignment.TopCenter),
                        contentScale = ContentScale.Fit
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = title,
                                fontFamily = AppFonts.OpenSansBold,
                                fontSize = 22.sp,
                                color = AppColors.Color2,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = message,
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        lineHeight = 13.sp,
                        color = AppColors.Color1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val okInteraction = remember { MutableInteractionSource() }
                    val okPressed by okInteraction.collectIsPressedAsState()
                    val okScale by animateFloatAsState(
                        targetValue = if (okPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 760f),
                        label = "suggest_project_result_ok_scale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(okScale)
                            .width(150.dp)
                            .height(30.dp)
                            .background(
                                color = AppColors.Color3,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = AppColors.BorderColor,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = okInteraction,
                                onClick = ::dismissWithAnimation
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ок",
                            fontFamily = AppFonts.OpenSansSemiBold,
                            fontSize = 12.sp,
                            color = AppColors.White
                        )
                        }
                    }
                }
            }
        }
    }
}
