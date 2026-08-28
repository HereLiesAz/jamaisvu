package com.hereliesaz.lamplight

import androidx.compose.runtime.Composable

/** Opens [url] on whichever platform this runs on -- an Android `Intent`, or a new browser tab on web. */
@Composable
expect fun rememberUrlOpener(): (String) -> Unit

/**
 * Google's documented cross-platform Maps URL format: a browser tab on web, or the Maps app
 * via an app/universal link on Android -- replacing the Android-only `geo:` URI scheme, which
 * has no web equivalent.
 */
fun mapsSearchUrl(latitude: Double, longitude: Double): String =
    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
