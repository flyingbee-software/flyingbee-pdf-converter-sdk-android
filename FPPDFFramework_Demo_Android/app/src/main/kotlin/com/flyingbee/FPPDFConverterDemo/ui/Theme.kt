package com.flyingbee.FPPDFConverterDemo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Theme : light/dynamic-free Material3 palette. The demo intentionally keeps
// a plain blue accent so it looks the same on every device (parity with the
// iOS demo, which uses the system tint).
// ---------------------------------------------------------------------------

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A66C2),
    secondary = Color(0xFF1976D2),
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EA8DC),
    secondary = Color(0xFF90CAF9),
)

@Composable
fun FPPDFDemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
