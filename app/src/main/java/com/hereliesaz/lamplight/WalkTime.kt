package com.hereliesaz.lamplight

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

// An average, unhurried walking pace including street crossings -- about 4.6 km/h. The product
// direction is explicit that this should stay a rule-based estimate, not a routed ETA from a
// live directions API: "Approximate walking distance," never a promised arrival time.
private const val METERS_PER_MINUTE = 77.0

/** Great-circle distance between two points, in meters. */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val sinDLat = sin(dLat / 2)
    val sinDLon = sin(dLon / 2)
    val a = sinDLat * sinDLat +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinDLon * sinDLon
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
}

/** Approximate walking minutes from the hotel anchor to a place, rounded up to at least one minute. */
fun walkMinutesFromAnchor(anchor: HotelAnchor, place: Place): Int {
    val meters = haversineMeters(anchor.latitude, anchor.longitude, place.latitude, place.longitude)
    return maxOf(1, (meters / METERS_PER_MINUTE).roundToInt())
}
