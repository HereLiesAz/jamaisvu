package com.hereliesaz.lamplight

/** A single lat/lng fix, replacing `android.location.Location` everywhere it's read outside androidMain. */
data class GeoPosition(val latitude: Double, val longitude: Double)

/** A single, on-demand location fix -- never background or continuous tracking. Returns null if a fix couldn't be obtained (denied, timed out, unsupported). */
interface LocationProvider {
    suspend fun currentLocation(): GeoPosition?
}
