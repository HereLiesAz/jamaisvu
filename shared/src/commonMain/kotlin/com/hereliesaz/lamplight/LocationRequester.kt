package com.hereliesaz.lamplight

import androidx.compose.runtime.Composable

/**
 * Requests a single location fix, handling whatever permission step the platform needs first --
 * Android's explicit runtime-permission prompt, or nothing at all on web, where the browser
 * prompts on its own the moment the underlying [LocationProvider] is called. Returns null for
 * every kind of failure (denied, timed out, unsupported) alike, same as [LocationProvider]
 * itself -- callers that need to tell "denied" apart from "no fix" don't exist today.
 */
@Composable
expect fun rememberLocationRequester(): suspend () -> GeoPosition?
