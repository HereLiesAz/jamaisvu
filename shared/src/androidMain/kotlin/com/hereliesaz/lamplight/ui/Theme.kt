package com.hereliesaz.lamplight.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hereliesaz.lamplight.shared.R

// Uncut Sans (the client brief's original pick) is a commercial foundry font with no
// confirmed license for bundling into this app. Archivo is its licensed stand-in: SIL OFL,
// on Google Fonts, and -- like Uncut Sans -- explicitly designed for confident display
// headlines with a true Black weight, matching the FontWeight.Black already used throughout.
val ArchivoFamily = FontFamily(
    Font(R.font.archivo_regular, FontWeight.Normal),
    Font(R.font.archivo_bold, FontWeight.Bold),
    Font(R.font.archivo_black, FontWeight.Black)
)

// Martian Mono (Evil Martians, SIL OFL) for functional/utility text: distance, category,
// time, status, coordinates, "OPEN," and directional detail, per the design direction.
val MartianMonoFamily = FontFamily(
    Font(R.font.martian_mono_regular, FontWeight.Normal),
    Font(R.font.martian_mono_bold, FontWeight.Bold)
)

// Near-black, never pure black; amber is the only bright accent in the whole app.
// See docs/design-system.md for the source design direction these tokens implement.
val Ink = Color(0xFF080A09)
val Panel = Color(0xFF111512)
val Amber = Color(0xFFFFC24B)
val Cream = Color(0xFFF2EFEA)
val Fog = Color(0xFFAFAFAA)

// Every role a component this app actually uses (Button, FilledTonalButton, FilterChip,
// OutlinedTextField, AssistChip, CircularProgressIndicator) reads is set explicitly here —
// leaving any of these unset falls back to Material's stock purple baseline underneath it.
// primary/secondary/tertiary all resolve to Amber on purpose: the design direction calls for
// a single accent, differentiated by placement and icon shape, never by a second hue.
private val LamplightColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A2E12),
    onPrimaryContainer = Amber,
    secondary = Amber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF3A2E12),
    onSecondaryContainer = Amber,
    tertiary = Amber,
    onTertiary = Color.Black,
    background = Ink,
    onBackground = Cream,
    surface = Panel,
    onSurface = Cream,
    surfaceVariant = Color(0xFF1B1B1A),
    onSurfaceVariant = Fog,
    outline = Color(0xFF5C5C58),
    outlineVariant = Color(0xFF33332F),
    error = Color(0xFFFF6E6E),
    onError = Color.Black,
    errorContainer = Color(0xFF4A1414),
    onErrorContainer = Color(0xFFFFD9D9)
)

// "A very good independent magazine" reads as square, not rounded: minimally softened
// corners everywhere, hairline dividers instead of elevated cards.
private val LamplightShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(4.dp)
)

// Every Text() in this app that omits its own style/fontFamily resolves to this bodyLarge --
// so setting Archivo here is what actually makes headline-ish text use it, app-wide, without
// touching every call site. Utility-label call sites opt into MartianMonoFamily explicitly.
private val LamplightTypography = Typography().let { defaults ->
    defaults.copy(bodyLarge = defaults.bodyLarge.copy(fontFamily = ArchivoFamily))
}

@Composable
fun LamplightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LamplightColors,
        shapes = LamplightShapes,
        typography = LamplightTypography,
        content = content
    )
}
