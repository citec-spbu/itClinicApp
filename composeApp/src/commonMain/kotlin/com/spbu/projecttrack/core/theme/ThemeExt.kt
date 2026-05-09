package com.spbu.projecttrack.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.spbu.projecttrack.core.settings.LocalSettingsPalette
import com.spbu.projecttrack.core.settings.SettingsPalette

/**
 * Quick accessor for the current [SettingsPalette] inside any @Composable.
 * Usage: val palette = appPalette()
 */
@Composable
@ReadOnlyComposable
fun appPalette(): SettingsPalette = LocalSettingsPalette.current

/**
 * Subtle border / divider color – the "Color1" role.
 * Light mode: 0xFFBDBDBD  |  Dark mode: 0xFF3A3A3C
 */
val SettingsPalette.subtleBorder: Color
    get() = if (background.red < 0.2f) Color(0xFF3A3A3C) else Color(0xFFBDBDBD)

/**
 * Unselected-tab / placeholder text color – lighter than primary, darker than invisible.
 * Light mode: 0xFFBDBDBD  |  Dark mode: 0xFF6B6B6F
 */
val SettingsPalette.dimText: Color
    get() = if (background.red < 0.2f) Color(0xFF6B6B6F) else Color(0xFFBDBDBD)
