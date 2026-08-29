package com.hereliesaz.lamplight

import androidx.compose.runtime.Composable

@Composable
actual fun rememberLocationRequester(): suspend () -> GeoPosition? =
    rememberLocationRequesterFor { BrowserLocationProvider() }
