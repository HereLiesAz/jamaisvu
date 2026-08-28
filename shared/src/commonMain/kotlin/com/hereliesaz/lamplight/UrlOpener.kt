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

/** Same documented Maps URL format, in walking-directions-to form rather than a plain search. */
fun walkingDirectionsUrl(latitude: Double, longitude: Double): String =
    "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking"

/**
 * Walking directions to the Home Lantern. Android prefers the Maps app's turn-by-turn
 * navigation mode (a distinct experience from just viewing directions), falling back to
 * [walkingDirectionsUrl] only if the Maps app isn't installed; every other target has no such
 * app to prefer, so it's just that same URL.
 */
@Composable
expect fun rememberWalkingDirectionsOpener(): (HotelAnchor) -> Unit
