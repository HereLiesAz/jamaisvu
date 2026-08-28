package com.hereliesaz.lamplight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationRequester(): suspend () -> GeoPosition? {
    val locationProvider = remember { BrowserLocationProvider() }
    return remember(locationProvider) { { locationProvider.currentLocation() } }
}
