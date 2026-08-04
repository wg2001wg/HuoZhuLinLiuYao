package com.example.huozhulin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8B1A1A),
    onPrimary = Color.White,
    secondary = Color(0xFFB5892E),
    surfaceVariant = Color(0xFFF3E9D2),
    background = Color(0xFFFCF8F0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0A0A0),
    secondary = Color(0xFFD8B36A),
    surfaceVariant = Color(0xFF2A2320),
    background = Color(0xFF1B1714)
)

@androidx.compose.runtime.Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
