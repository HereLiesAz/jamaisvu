package com.hereliesaz.lamplight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuarterMuseSeedTest {

    private val header = "Id,Venue,Latitude,Longitude,Category Tags"

    @Test
    fun `parses well-formed rows`() {
        val csv = """
            $header
            21st-amendment-bar,21st Amendment Bar,29.9547,-90.0672,Craft Cocktails; Live Music
            acme-oyster-house,Acme Oyster House,29.9543,-90.0689,Seafood; Historic Restaurant
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals(2, places.size)
        val first = places[0]
        assertEquals("21st-amendment-bar", first.id)
        assertEquals("21st Amendment Bar", first.venue)
        assertEquals(29.9547, first.latitude, 0.0)
        assertEquals(-90.0672, first.longitude, 0.0)
        assertEquals(listOf("Craft Cocktails", "Live Music"), first.tags)
    }

    @Test
    fun `id comes directly from the CSV, not derived from venue or coordinates`() {
        val csv = """
            $header
            stable-id,Original Name,29.9,-90.1,Tag
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals("stable-id", places.single().id)
    }

    @Test
    fun `a row with the wrong column count is skipped, not fatal`() {
        val csv = """
            $header
            good-one,Good Venue,29.9,-90.1,Tag
            too-few,Too Few Columns,29.9
            good-two,Another Good Venue,29.8,-90.2,Tag
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals(listOf("good-one", "good-two"), places.map { it.id })
    }

    @Test
    fun `a row with a blank venue or no tags is skipped`() {
        val csv = """
            $header
            blank-venue,,29.9,-90.1,Tag
            no-tags,Has No Tags,29.9,-90.1,
            fine,Fine Venue,29.9,-90.1,Tag
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals(listOf("fine"), places.map { it.id })
    }

    @Test
    fun `a non-numeric coordinate is skipped, not fatal`() {
        val csv = """
            $header
            bad-coords,Bad Coords,not-a-number,-90.1,Tag
            fine,Fine Venue,29.9,-90.1,Tag
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals(listOf("fine"), places.map { it.id })
    }

    @Test
    fun `an unexpected header yields an empty catalog instead of crashing`() {
        val csv = """
            Name,Lat,Lon,Tags
            x,Venue,29.9,-90.1,Tag
        """.trimIndent()

        assertTrue(QuarterMuseSeed.parseCatalog(csv).isEmpty())
    }

    @Test
    fun `an unterminated quote yields an empty catalog instead of crashing`() {
        val csv = """
            $header
            broken,"Unterminated Quote,29.9,-90.1,Tag
        """.trimIndent()

        assertTrue(QuarterMuseSeed.parseCatalog(csv).isEmpty())
    }

    @Test
    fun `a quoted field with an embedded comma parses as one field`() {
        val csv = """
            $header
            quoted,"Smith, Jones Bar",29.9,-90.1,Tag
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals("Smith, Jones Bar", places.single().venue)
    }

    @Test
    fun `blank interior lines do not break subsequent rows`() {
        val csv = """
            $header
            first,First Venue,29.9,-90.1,Tag

            second,Second Venue,29.8,-90.2,Tag
        """.trimIndent()

        val places = QuarterMuseSeed.parseCatalog(csv)

        assertEquals(listOf("first", "second"), places.map { it.id })
    }
}
