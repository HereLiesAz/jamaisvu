package com.hereliesaz.jamaisvu.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val Ink = Color(0xFF080A09)
val Panel = Color(0xFF111512)
val Moss = Color(0xFF91E7B7)
val Acid = Color(0xFFE8FF74)
val Fog = Color(0xFFB8C2BB)

// Every role a component this app actually uses (NavigationBar, Button, FilledTonalButton,
// OutlinedTextField, AssistChip, CircularProgressIndicator) reads is set explicitly here —
// leaving any of these unset falls back to Material's stock purple baseline underneath it.
private val JamaisVuColors = darkColorScheme(
    primary = Moss,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1C3A2C),
    onPrimaryContainer = Moss,
    secondary = Acid,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF2A331A),
    onSecondaryContainer = Acid,
    tertiary = Moss,
    onTertiary = Color.Black,
    background = Ink,
    onBackground = Color.White,
    surface = Panel,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B211D),
    onSurfaceVariant = Fog,
    outline = Color(0xFF5C665F),
    outlineVariant = Color(0xFF33392F),
    error = Color(0xFFFF6E6E),
    onError = Color.Black,
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFFD9D9)
)

// Expressive's wider corner-radius scale so grid tiles, sheets, and buttons read as one system.
private val JamaisVuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun JamaisVuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JamaisVuColors,
        shapes = JamaisVuShapes,
        content = content
    )
}
