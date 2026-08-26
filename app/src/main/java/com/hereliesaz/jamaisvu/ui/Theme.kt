package com.hereliesaz.jamaisvu.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF080A09)
val Panel = Color(0xFF111512)
val Moss = Color(0xFF91E7B7)
val Acid = Color(0xFFE8FF74)
val Fog = Color(0xFFB8C2BB)

private val JamaisVuColors = darkColorScheme(
    primary = Moss,
    secondary = Acid,
    background = Ink,
    surface = Panel,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun JamaisVuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JamaisVuColors, content = content)
}
