package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CodeEditorColorScheme = darkColorScheme(
    primary = AccentColor,
    secondary = AccentColor,
    tertiary = AccentColor,
    background = ActivityBarBackground,
    surface = SidebarBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextNormal,
    onSurface = TextNormal
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for code editor vibe
    dynamicColor: Boolean = false, // Disable dynamic colors
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CodeEditorColorScheme,
        typography = Typography,
        content = content
    )
}
