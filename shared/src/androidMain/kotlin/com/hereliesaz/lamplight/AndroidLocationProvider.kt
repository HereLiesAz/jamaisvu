package com.hereliesaz.lamplight

import android.content.Context

/** Wraps [requestOneTimeLocation] behind [LocationProvider]. Caller must already hold ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION. */
class AndroidLocationProvider(private val context: Context) : LocationProvider {
    override suspend fun currentLocation(): GeoPosition? =
        requestOneTimeLocation(context)?.let { GeoPosition(it.latitude, it.longitude) }
}
