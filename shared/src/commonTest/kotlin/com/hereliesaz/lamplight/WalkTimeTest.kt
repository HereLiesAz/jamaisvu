package com.hereliesaz.lamplight

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WalkTimeTest {

    @Test
    fun `haversine distance between the same point is zero`() {
        assertApproxEquals(0.0, haversineMeters(29.9547, -90.0672, 29.9547, -90.0672), 0.0001)
    }

    @Test
    fun `haversine distance matches the spherical-earth meters-per-degree-latitude approximation`() {
        // Spherical approximation: radius_meters * radians(1 degree) ~= 111,195m per degree of
        // latitude, independent of longitude -- this pins the formula, not real-world WGS84.
        val meters = haversineMeters(30.0, -90.0, 30.001, -90.0)
        assertApproxEquals(111.19, meters, 0.5)
    }

    @Test
    fun `walk time rounds up to at least one minute for a place right next door`() {
        val anchor = HotelAnchor("Test Hotel", 29.9547, -90.0672)
        val place = place(29.95471, -90.06721)

        assertEquals(1, walkMinutesFromAnchor(anchor, place))
    }

    @Test
    fun `walk time grows with distance`() {
        val anchor = HotelAnchor("Test Hotel", 29.9547, -90.0672)
        val near = place(29.9550, -90.0672)
        val far = place(29.9700, -90.0672)

        assertTrue(walkMinutesFromAnchor(anchor, far) > walkMinutesFromAnchor(anchor, near))
    }

    @Test
    fun `nearestHotelWithin matches a hotel well inside the proximity threshold`() {
        val hotel = hotel("close-hotel", 29.9542, -90.0677)
        // ~44m north -- comfortably inside the 120m threshold.
        val match = nearestHotelWithin(29.95460, -90.0677, listOf(hotel))

        assertEquals(hotel, match)
    }

    @Test
    fun `nearestHotelWithin returns null when nothing is close enough`() {
        val hotel = hotel("far-hotel", 29.9542, -90.0677)
        // ~222m north -- outside the 120m threshold.
        val match = nearestHotelWithin(29.9562, -90.0677, listOf(hotel))

        assertEquals(null, match)
    }

    @Test
    fun `nearestHotelWithin picks the closer of two hotels both within threshold`() {
        val near = hotel("near-hotel", 29.9542, -90.0677)
        val far = hotel("less-near-hotel", 29.9542, -90.0685)
        // ~33m from "near", further (but still within range) from "far".
        val match = nearestHotelWithin(29.95450, -90.0677, listOf(far, near))

        assertEquals(near, match)
    }

    // kotlin.test has no delta-tolerance assertEquals overload for Double the way JUnit does.
    private fun assertApproxEquals(expected: Double, actual: Double, tolerance: Double) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected but was $actual (tolerance $tolerance)"
        )
    }

    private fun place(lat: Double, lng: Double) = Place(
        id = "test-place",
        venue = "Test Place",
        latitude = lat,
        longitude = lng,
        tags = listOf("Tag")
    )

    private fun hotel(id: String, lat: Double, lng: Double) = Hotel(
        id = id,
        name = id,
        latitude = lat,
        longitude = lng
    )
}
