package com.hereliesaz.jamaisvu.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
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

// Expressive's wider corner-radius scale so grid tiles, sheets, and buttons read as one system.
private val JamaisVuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JamaisVuTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = JamaisVuColors,
        motionScheme = MotionScheme.expressive(),
        shapes = JamaisVuShapes,
        content = content
    )
}
