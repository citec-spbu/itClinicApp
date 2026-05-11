package com.spbu.projecttrack.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.spbu.projecttrack.core.settings.LocalSettingsPalette
import com.spbu.projecttrack.core.settings.SettingsPalette

@Composable
@ReadOnlyComposable
fun appPalette(): SettingsPalette = LocalSettingsPalette.current

val SettingsPalette.subtleBorder: Color
    get() = if (background.red < 0.2f) Color(0xFF3A3A3C) else Color(0xFFBDBDBD)

val SettingsPalette.dimText: Color
    get() = if (background.red < 0.2f) Color(0xFF6B6B6F) else Color(0xFFBDBDBD)
