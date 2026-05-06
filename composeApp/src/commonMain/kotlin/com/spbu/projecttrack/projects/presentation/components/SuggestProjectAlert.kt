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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.logging.AppLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.close_icon
import projecttrack.composeapp.generated.resources.spbu_logo

private const val SuggestProjectAlertExitDurationMs = 180L

@Composable
fun SuggestProjectAlert(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit, // name, email
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var renderDialog by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    val logTag = "SuggestProjectAlert"
    val scope = rememberCoroutineScope()
    val dimAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 0.18f else 0f,
        animationSpec = tween(220),
        label = "suggest_project_dim_alpha"
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            name = ""
            email = ""
            renderDialog = true
            contentVisible = true
            AppLog.d(logTag, "Opened")
        } else if (renderDialog) {
            contentVisible = false
            delay(SuggestProjectAlertExitDurationMs)
            renderDialog = false
            AppLog.d(logTag, "Closed")
        }
    }

    fun dismissWithAnimation() {
        if (!contentVisible) return
        AppLog.d(logTag, "Dismiss")
        contentVisible = false
        scope.launch {
            delay(SuggestProjectAlertExitDurationMs)
            name = ""
            email = ""
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
                exit = fadeOut(animationSpec = tween(SuggestProjectAlertExitDurationMs.toInt())) + scaleOut(
                    targetScale = 0.97f,
                    animationSpec = tween(SuggestProjectAlertExitDurationMs.toInt())
                )
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 340.dp)
                        .wrapContentHeight()
                        .background(
                            color = AppColors.White,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = AppColors.Color1,
                            shape = RoundedCornerShape(20.dp)
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
                            .height(120.dp)
                            .alpha(1f)
                            .align(Alignment.TopCenter),
                        contentScale = ContentScale.Fit
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Заказчикам",
                                fontFamily = AppFonts.OpenSansBold,
                                fontSize = 24.sp,
                                color = AppColors.Color2,
                                modifier = Modifier.align(Alignment.Center)
                            )

                        val closeInteraction = remember { MutableInteractionSource() }
                        val closePressed by closeInteraction.collectIsPressedAsState()
                        val closeScale by animateFloatAsState(
                            targetValue = if (closePressed) 0.88f else 1f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 760f),
                            label = "suggest_project_close_scale"
                        )

                        Image(
                            painter = painterResource(Res.drawable.close_icon),
                            contentDescription = "Закрыть",
                            modifier = Modifier
                                .size(24.dp)
                                .scale(closeScale)
                                .align(Alignment.CenterEnd)
                                .clickable(
                                    interactionSource = closeInteraction,
                                    indication = null,
                                    onClick = ::dismissWithAnimation
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Если у Вас есть запрос на сотрудничество и создание проекта, заполните онлайн-заявку. Наш представитель свяжется с вами в ближайшее время",
                        fontFamily = AppFonts.OpenSansRegular,
                        fontSize = 12.sp,
                        lineHeight = 13.sp,
                        color = AppColors.Color1,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    CenteredTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "Имя"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CenteredTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Почта"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val submitInteraction = remember { MutableInteractionSource() }
                    val submitPressed by submitInteraction.collectIsPressedAsState()
                    val submitScale by animateFloatAsState(
                        targetValue = if (submitPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 760f),
                        label = "suggest_project_submit_scale"
                    )

                    Box(
                        modifier = Modifier
                            .scale(submitScale)
                            .width(150.dp)
                            .height(30.dp)
                            .background(
                                color = AppColors.Color3,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = AppColors.BorderColor,
                                shape = RoundedCornerShape(15.dp)
                            )
                            .clickable(
                                indication = null,
                                interactionSource = submitInteraction,
                                onClick = {
                                    val cleanedName = name.trim()
                                    val cleanedEmail = email.trim()
                                    AppLog.d(
                                        logTag,
                                        "Submit click: nameLen=${cleanedName.length}, emailLen=${cleanedEmail.length}"
                                    )
                                    onSubmit(cleanedName, cleanedEmail)
                                    dismissWithAnimation()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Отправить",
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

@Composable
private fun CenteredTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.width(250.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .height(36.dp)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = AppFonts.OpenSansRegular,
                fontSize = 12.sp,
                color = if (value.isEmpty()) AppColors.Color1 else AppColors.Color2,
                textAlign = TextAlign.Center
            ),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = AppFonts.OpenSansRegular,
                            fontSize = 12.sp,
                            color = AppColors.Color1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        innerTextField()
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Color2)
        )
    }
}
