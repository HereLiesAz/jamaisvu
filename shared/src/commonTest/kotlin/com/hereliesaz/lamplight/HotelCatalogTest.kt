package com.hereliesaz.lamplight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HotelCatalogTest {

    private val header = "Id,Name,Latitude,Longitude"

    @Test
    fun `parses well-formed rows`() {
        val csv = """
            $header
            hotel-monteleone,Hotel Monteleone,29.9542,-90.0677
            the-roosevelt-new-orleans,The Roosevelt New Orleans,29.9539,-90.0716
        """.trimIndent()

        val hotels = HotelCatalog.parseCatalog(csv)

        assertEquals(2, hotels.size)
        val first = hotels[0]
        assertEquals("hotel-monteleone", first.id)
        assertEquals("Hotel Monteleone", first.name)
        assertEquals(29.9542, first.latitude)
        assertEquals(-90.0677, first.longitude)
    }

    @Test
    fun `a row with the wrong column count is skipped, not fatal`() {
        val csv = """
            $header
            good-one,Good Hotel,29.95,-90.06
            too-few,Too Few Columns
            good-two,Another Good Hotel,29.96,-90.07
        """.trimIndent()

        val hotels = HotelCatalog.parseCatalog(csv)

        assertEquals(listOf("good-one", "good-two"), hotels.map { it.id })
    }

    @Test
    fun `a row with a blank name is skipped`() {
        val csv = """
            $header
            blank-name,,29.95,-90.06
            fine,Fine Hotel,29.95,-90.06
        """.trimIndent()

        val hotels = HotelCatalog.parseCatalog(csv)

        assertEquals(listOf("fine"), hotels.map { it.id })
    }

    @Test
    fun `a non-numeric coordinate is skipped, not fatal`() {
        val csv = """
            $header
            bad-coords,Bad Coords,not-a-number,-90.06
            fine,Fine Hotel,29.95,-90.06
        """.trimIndent()

        val hotels = HotelCatalog.parseCatalog(csv)

        assertEquals(listOf("fine"), hotels.map { it.id })
    }

    @Test
    fun `an unexpected header yields an empty catalog instead of crashing`() {
        val csv = """
            Name,Lat,Lon
            x,Hotel,29.95,-90.06
        """.trimIndent()

        assertTrue(HotelCatalog.parseCatalog(csv).isEmpty())
    }
}
