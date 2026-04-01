package com.ble_mesh.meshtalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** MeshTalk always uses the dark mesh color scheme for a premium feel. */
private val MeshDarkColorScheme = darkColorScheme(
    primary = MeshPrimary,
    onPrimary = MeshOnBackground,
    primaryContainer = Color(0xFF4C1D95),
    secondary = MeshSecondary,
    tertiary = MeshTertiary,
    background = MeshBackground,
    surface = MeshSurface,
    onBackground = MeshOnBackground,
    onSurface = MeshOnSurface,
    error = MeshError
)

@Composable
fun MeshTalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always enforce dark mesh theme for a consistent premium look
    MaterialTheme(
        colorScheme = MeshDarkColorScheme,
        typography = Typography,
        content = content
    )
}