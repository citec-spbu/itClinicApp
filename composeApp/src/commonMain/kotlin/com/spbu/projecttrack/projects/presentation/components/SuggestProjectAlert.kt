package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.logging.AppLog
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.close_icon
import projecttrack.composeapp.generated.resources.spbu_logo

@Composable
fun SuggestProjectAlert(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val logTag = "SuggestProjectAlert"
    val customersTitle = localizedString("Заказчикам", "For customers")
    val closeLabel = localizedString("Закрыть", "Close")
    val descriptionText = localizedString(
        "Если у Вас есть запрос на сотрудничество и создание проекта, заполните онлайн-заявку. Наш представитель свяжется с вами в ближайшее время",
        "If you have a collaboration request and want to start a project, fill out the online form. Our representative will contact you soon.",
    )
    val namePlaceholder = localizedString("Имя", "Name")
    val emailPlaceholder = localizedString("Почта", "Email")
    val sendLabel = localizedString("Отправить", "Send")

    LaunchedEffect(isVisible) {
        if (isVisible) {
            name = ""
            email = ""
            AppLog.d(logTag, "Opened")
        } else {
            AppLog.d(logTag, "Closed")
        }
    }

    ProjectOverlayDialog(
        isVisible = isVisible,
        onDismiss = {
            AppLog.d(logTag, "Dismiss")
            name = ""
            email = ""
            onDismiss()
        },
        modifier = modifier,
        maxWidth = 340.dp,
        shape = RoundedCornerShape(20.dp),
        borderColor = AppColors.Color1,
        containerColor = AppColors.White,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        backgroundContent = {
            Image(
                painter = painterResource(Res.drawable.spbu_logo),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(appPalette().spbuBackdropLogoAlpha),
                alignment = Alignment.Center,
                contentScale = ContentScale.FillWidth
            )
        }
    ) { dismiss ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = customersTitle,
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
                    contentDescription = closeLabel,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(closeScale)
                        .align(Alignment.CenterEnd)
                        .clickable(
                            interactionSource = closeInteraction,
                            indication = null,
                            onClick = dismiss
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = descriptionText,
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
                placeholder = namePlaceholder
            )

            Spacer(modifier = Modifier.height(10.dp))

            CenteredTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = emailPlaceholder
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
                            dismiss()
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = sendLabel,
                    fontFamily = AppFonts.OpenSansSemiBold,
                    fontSize = 12.sp,
                    color = AppColors.White
                )
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
