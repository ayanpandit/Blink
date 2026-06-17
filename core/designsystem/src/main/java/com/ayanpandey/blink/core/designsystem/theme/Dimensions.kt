package com.ayanpandey.blink.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val doubleExtraLarge: Dp = 48.dp,
)

val LocalDimensions = androidx.compose.runtime.staticCompositionLocalOf { Dimensions() }
