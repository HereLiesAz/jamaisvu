package com.hereliesaz.lamplight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkTimeTest {

    @Test
    fun `haversine distance between the same point is zero`() {
        assertEquals(0.0, haversineMeters(29.9547, -90.0672, 29.9547, -90.0672), 0.0001)
    }

    @Test
    fun `haversine distance matches the spherical-earth meters-per-degree-latitude approximation`() {
        // Spherical approximation: radius_meters * radians(1 degree) ~= 111,195m per degree of
        // latitude, independent of longitude -- this pins the formula, not real-world WGS84.
        val meters = haversineMeters(30.0, -90.0, 30.001, -90.0)
        assertEquals(111.19, meters, 0.5)
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

    private fun place(lat: Double, lng: Double) = Place(
        id = "test-place",
        venue = "Test Place",
        latitude = lat,
        longitude = lng,
        tags = listOf("Tag")
    )
}
