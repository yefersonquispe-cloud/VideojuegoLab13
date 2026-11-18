package com.yeferson.videojuegolab13.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GameColorScheme = darkColorScheme(
    primary = Mostaza,
    onPrimary = Noche,
    secondary = Turquesa,
    onSecondary = Noche,
    tertiary = Bermellon,
    background = Noche,
    onBackground = Hueso,
    surface = Noche,
    onSurface = Hueso
)

@Composable
fun VideojuegoLab13Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GameColorScheme,
        typography = Typography,
        content = content
    )
}
