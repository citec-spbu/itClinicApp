package com.spbu.projecttrack.projects.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun MyProjectButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val myProjectLabel = localizedString("Мой проект", "My project")
    Box(
        modifier = modifier
            .wrapContentWidth() // Динамическая ширина
            .height(40.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.25f)
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
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = myProjectLabel,
            fontFamily = AppFonts.OpenSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = AppColors.White
        )
    }
}
