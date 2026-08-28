package com.hereliesaz.lamplight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberUrlOpener(): (String) -> Unit {
    return remember { { url: String -> window.open(url, "_blank") } }
}

@Composable
actual fun rememberWalkingDirectionsOpener(): (HotelAnchor) -> Unit {
    return remember {
        { anchor: HotelAnchor -> window.open(walkingDirectionsUrl(anchor.latitude, anchor.longitude), "_blank") }
    }
}
