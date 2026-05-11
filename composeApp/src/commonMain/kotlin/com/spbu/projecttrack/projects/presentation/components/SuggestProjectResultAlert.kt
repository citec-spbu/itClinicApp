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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import com.spbu.projecttrack.core.theme.appPalette
import org.jetbrains.compose.resources.painterResource
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.spbu_logo
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SuggestProjectResultAlert(
    isVisible: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val okLabel = localizedString("Ок", "OK")
    ProjectOverlayDialog(
        isVisible = isVisible,
        onDismiss = onDismiss,
        modifier = modifier,
        maxWidth = 320.dp,
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
                    text = title,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = AppColors.Color2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = message,
                fontFamily = AppFonts.OpenSans,
                fontWeight = FontWeight.Normal,
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
                        shape = RoundedCornerShape(15.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = AppColors.BorderColor,
                        shape = RoundedCornerShape(15.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = okInteraction,
                        onClick = dismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = okLabel,
                    fontFamily = AppFonts.OpenSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AppColors.White
                )
            }
        }
    }
}
