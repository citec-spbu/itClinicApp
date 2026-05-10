package com.spbu.projecttrack.core.theme

import androidx.compose.runtime.Composable

/** Android: статус/навигационная панель под цвет фона и контраст иконок. iOS: заглушка. */
@Composable
expect fun SystemBarAppearance(isDark: Boolean)
