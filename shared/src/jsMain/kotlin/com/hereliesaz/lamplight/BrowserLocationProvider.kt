package com.hereliesaz.lamplight

import kotlin.js.Promise
import kotlinx.coroutines.await

private fun currentPositionAsync(): Promise<String> = js(
    """
    new Promise(function(resolve) {
        if (!navigator.geolocation) { resolve(''); return; }
        navigator.geolocation.getCurrentPosition(
            function(position) { resolve(position.coords.latitude + ',' + position.coords.longitude); },
            function() { resolve(''); }
        );
    })
    """
)

/**
 * Wraps `navigator.geolocation.getCurrentPosition` behind [LocationProvider] -- the classic
 * Kotlin/JS counterpart of wasmJsMain's own `BrowserLocationProvider`. Kept as a separate file
 * rather than folded into webMain: the JS-interop mechanism itself differs per target (plain
 * Kotlin/JS's `js("...")` intrinsic here vs. Kotlin/Wasm's `@JsFun` boundary marshaling there,
 * which also needs the extra `JsString` round-trip this target doesn't), even though the
 * browser-side behavior and the "lat,lng" string contract are identical.
 */
class BrowserLocationProvider : LocationProvider {
    override suspend fun currentLocation(): GeoPosition? {
        val raw = currentPositionAsync().await()
        if (raw.isEmpty()) return null
        val parts = raw.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return GeoPosition(lat, lng)
    }
}
