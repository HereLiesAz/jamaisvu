package com.hereliesaz.lamplight

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlinx.coroutines.await

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
    () => new Promise((resolve) => {
        if (!navigator.geolocation) { resolve(''); return; }
        navigator.geolocation.getCurrentPosition(
            (position) => resolve(position.coords.latitude + ',' + position.coords.longitude),
            () => resolve('')
        );
    })
    """
)
private external fun currentPositionAsync(): Promise<JsString>

/**
 * Wraps `navigator.geolocation.getCurrentPosition` behind [LocationProvider]. The browser
 * itself prompts for permission on first call -- there's no separate permission-request step
 * the way Android needs. The fix crosses the JS boundary as a plain "lat,lng" string (empty
 * means denied/unsupported/failed), avoiding the more involved external-interface marshaling
 * a structured JS position object would need.
 */
class BrowserLocationProvider : LocationProvider {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun currentLocation(): GeoPosition? {
        val raw = currentPositionAsync().await().toString()
        if (raw.isEmpty()) return null
        val parts = raw.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lng = parts[1].toDoubleOrNull() ?: return null
        return GeoPosition(lat, lng)
    }
}
