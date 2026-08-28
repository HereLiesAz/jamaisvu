package com.hereliesaz.lamplight

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

// kotlin.math has no toRadians -- java.lang.Math's isn't available outside JVM/Android targets.
private fun degreesToRadians(degrees: Double): Double = degrees * PI / 180.0

// An average, unhurried walking pace including street crossings -- about 4.6 km/h. The product
// direction is explicit that this should stay a rule-based estimate, not a routed ETA from a
// live directions API: "Approximate walking distance," never a promised arrival time.
private const val METERS_PER_MINUTE = 77.0

/** Great-circle distance between two points, in meters. */
fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = degreesToRadians(lat2 - lat1)
    val dLon = degreesToRadians(lon2 - lon1)
    val sinDLat = sin(dLat / 2)
    val sinDLon = sin(dLon / 2)
    val a = sinDLat * sinDLat +
        cos(degreesToRadians(lat1)) * cos(degreesToRadians(lat2)) * sinDLon * sinDLon
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
}

/** Approximate walking minutes between two points, rounded up to at least one minute. */
fun walkMinutesFrom(latitude: Double, longitude: Double, place: Place): Int {
    val meters = haversineMeters(latitude, longitude, place.latitude, place.longitude)
    return maxOf(1, (meters / METERS_PER_MINUTE).roundToInt())
}

/** Approximate walking minutes from the hotel anchor to a place, rounded up to at least one minute. */
fun walkMinutesFromAnchor(anchor: HotelAnchor, place: Place): Int =
    walkMinutesFrom(anchor.latitude, anchor.longitude, place)

// A guest standing in a hotel's lobby or room can easily read 50-100m off from the hotel
// entrance on GPS; anything tighter risks missing a real match, anything looser risks matching
// the wrong hotel a couple of doors down.
private const val HOTEL_PROXIMITY_METERS = 120.0

/** The closest hotel to a point, if any hotel is within [HOTEL_PROXIMITY_METERS]. */
fun nearestHotelWithin(latitude: Double, longitude: Double, hotels: List<Hotel>): Hotel? =
    hotels
        .map { it to haversineMeters(latitude, longitude, it.latitude, it.longitude) }
        .filter { (_, meters) -> meters <= HOTEL_PROXIMITY_METERS }
        .minByOrNull { (_, meters) -> meters }
        ?.first
