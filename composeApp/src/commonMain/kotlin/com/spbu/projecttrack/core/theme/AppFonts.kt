package com.spbu.projecttrack.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import projecttrack.composeapp.generated.resources.Res
import projecttrack.composeapp.generated.resources.opensans_bold
import projecttrack.composeapp.generated.resources.opensans_light
import projecttrack.composeapp.generated.resources.opensans_medium
import projecttrack.composeapp.generated.resources.opensans_regular
import projecttrack.composeapp.generated.resources.opensans_semibold
import projecttrack.composeapp.generated.resources.philosopher_bold

object AppFonts {
    val OpenSansLight: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.opensans_light)
        )

    val OpenSansRegular: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.opensans_regular)
        )

    val OpenSansMedium: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.opensans_medium)
        )

    val OpenSansSemiBold: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.opensans_semibold)
        )

    val OpenSansBold: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.opensans_bold)
        )

    val OpenSans: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.opensans_light, weight = FontWeight.Light),
            Font(Res.font.opensans_regular, weight = FontWeight.Normal),
            Font(Res.font.opensans_medium, weight = FontWeight.Medium),
            Font(Res.font.opensans_semibold, weight = FontWeight.SemiBold),
            Font(Res.font.opensans_bold, weight = FontWeight.Bold)
        )

    val Philosopher: FontFamily
        @Composable get() = FontFamily(
            Font(Res.font.philosopher_bold, weight = FontWeight.Bold)
        )
}
