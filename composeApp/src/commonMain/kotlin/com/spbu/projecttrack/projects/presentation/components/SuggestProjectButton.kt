package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spbu.projecttrack.core.settings.localizedString
import com.spbu.projecttrack.core.theme.AppColors
import com.spbu.projecttrack.core.theme.AppFonts
import projecttrack.composeapp.generated.resources.*

@Composable
fun SuggestProjectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    val resolvedText = text ?: localizedString("Предложить проект", "Suggest a project")
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 700f),
        label = "suggest_btn_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .wrapContentWidth()
            .height(40.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f),
                clip = false
            )
            .background(
                color = AppColors.Color3,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 2.dp,
                color = AppColors.BorderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = resolvedText,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = AppColors.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
