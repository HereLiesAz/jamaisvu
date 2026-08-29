package com.hereliesaz.lamplight

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Shared body for both browser targets' `rememberLocationRequester()` actual -- each leaf
 * only supplies its own [BrowserLocationProvider] construction, since that class itself stays
 * per-target on purpose (the JS-interop mechanism underneath genuinely differs; see
 * `BrowserLocationProvider.kt` in `jsMain` and `wasmJsMain`).
 */
@Composable
internal fun rememberLocationRequesterFor(newProvider: () -> LocationProvider): suspend () -> GeoPosition? {
    val locationProvider = remember { newProvider() }
    return remember(locationProvider) { { locationProvider.currentLocation() } }
}
