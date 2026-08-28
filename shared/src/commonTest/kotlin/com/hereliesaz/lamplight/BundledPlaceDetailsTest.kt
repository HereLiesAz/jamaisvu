package com.hereliesaz.lamplight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// Fixture shape matches exactly what scripts/fetch_place_photos.py writes to
// place_details_manifest.json (see its details_entry construction), not a guessed schema.
class BundledPlaceDetailsTest {

    @Test
    fun `parses a fully populated entry`() {
        val json = """
            {
              "the-carousel-bar": {
                "phone": "+1 504-523-3341",
                "website": "https://www.hotelmonteleone.com/carousel-bar/",
                "address": "214 Royal St, New Orleans, LA 70130",
                "weekdayDescriptions": ["Monday: 11:00 AM – 12:00 AM", "Tuesday: 11:00 AM – 12:00 AM"],
                "periods": [
                  {"openDay": 1, "openTime": "11:00", "closeDay": 2, "closeTime": "00:00"}
                ],
                "tags": ["Historic Bar", "Craft Cocktails", "Rotating Bar"]
              }
            }
        """.trimIndent()

        val details = BundledPlaceDetails.parseManifest(json).getValue("the-carousel-bar")

        assertEquals("+1 504-523-3341", details.phone)
        assertEquals("https://www.hotelmonteleone.com/carousel-bar/", details.website)
        assertEquals("214 Royal St, New Orleans, LA 70130", details.address)
        assertEquals(2, details.weekdayDescriptions.size)
        assertEquals(listOf("Historic Bar", "Craft Cocktails", "Rotating Bar"), details.tags)
        assertEquals(OpeningPeriod(openDay = 1, openTime = "11:00", closeDay = 2, closeTime = "00:00"), details.periods.single())
    }

    @Test
    fun `a period with a null closeDay and closeTime parses -- an overnight span with no recorded close`() {
        val json = """
            { "v": { "periods": [ {"openDay": 3, "openTime": "18:00", "closeDay": null, "closeTime": null} ] } }
        """.trimIndent()

        val period = BundledPlaceDetails.parseManifest(json).getValue("v").periods.single()

        assertEquals(3, period.openDay)
        assertEquals("18:00", period.openTime)
        assertNull(period.closeDay)
        assertNull(period.closeTime)
    }

    @Test
    fun `a period missing its required openDay is dropped, not the whole entry`() {
        val json = """
            {
              "v": {
                "phone": "+1 504-000-0000",
                "periods": [
                  {"openTime": "11:00"},
                  {"openDay": 1, "openTime": "11:00", "closeDay": 1, "closeTime": "22:00"}
                ]
              }
            }
        """.trimIndent()

        val details = BundledPlaceDetails.parseManifest(json).getValue("v")

        assertEquals("+1 504-000-0000", details.phone)
        assertEquals(1, details.periods.size)
    }

    @Test
    fun `a venue with no matched Google Place still gets a valid, all-empty entry`() {
        val details = BundledPlaceDetails.parseManifest("""{ "v": {} }""").getValue("v")

        assertEquals(PlaceDetailsInfo(), details)
    }

    @Test
    fun `an empty manifest object yields an empty map`() {
        assertTrue(BundledPlaceDetails.parseManifest("{}").isEmpty())
    }

    @Test
    fun `malformed JSON yields an empty map instead of crashing`() {
        assertTrue(BundledPlaceDetails.parseManifest("not json at all").isEmpty())
    }
}
