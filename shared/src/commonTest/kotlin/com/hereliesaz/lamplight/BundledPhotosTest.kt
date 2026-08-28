package com.hereliesaz.lamplight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Fixture shape matches exactly what scripts/fetch_place_photos.py writes to
// photos_manifest.json (see its photos_manifest.append(...) block), not a guessed schema.
//
// This is commonTest, run against every target's own photoBaseUri() actual (Android's
// file:///android_asset/photos/, wasmJs's relative photos/) -- so uri assertions build the
// expected value from photoBaseUri() itself rather than hardcoding one target's answer.
class BundledPhotosTest {

    @Test
    fun `parses a populated manifest into place photos with authors`() {
        val json = """
            {
              "the-carousel-bar": [
                {
                  "file": "0.jpg",
                  "authors": [
                    {"name": "A Google User", "uri": "https://maps.google.com/contrib/1"}
                  ],
                  "googleMapsUri": "https://maps.google.com/maps/place/?q=place_id:abc"
                },
                {
                  "file": "1.jpg",
                  "authors": [],
                  "googleMapsUri": ""
                }
              ]
            }
        """.trimIndent()

        val result = BundledPhotos.parseManifest(json)

        val photos = result.getValue("the-carousel-bar")
        assertEquals(2, photos.size)
        assertEquals("${photoBaseUri()}the-carousel-bar/0.jpg", photos[0].uri)
        assertEquals(listOf(PhotoAuthor("A Google User", "https://maps.google.com/contrib/1")), photos[0].authors)
        assertEquals("https://maps.google.com/maps/place/?q=place_id:abc", photos[0].googleMapsUri)
        assertEquals("${photoBaseUri()}the-carousel-bar/1.jpg", photos[1].uri)
        assertTrue(photos[1].authors.isEmpty())
    }

    @Test
    fun `an entry missing the required file field is skipped, not the whole venue`() {
        val json = """
            {
              "some-venue": [
                {"authors": []},
                {"file": "1.jpg", "authors": []}
              ]
            }
        """.trimIndent()

        val photos = BundledPhotos.parseManifest(json).getValue("some-venue")

        assertEquals(1, photos.size)
        assertEquals("${photoBaseUri()}some-venue/1.jpg", photos[0].uri)
    }

    @Test
    fun `a venue whose every entry is unusable is dropped from the result entirely`() {
        val json = """{ "some-venue": [ {"authors": []} ] }"""

        assertTrue(BundledPhotos.parseManifest(json).isEmpty())
    }

    @Test
    fun `an empty manifest object yields an empty map`() {
        assertTrue(BundledPhotos.parseManifest("{}").isEmpty())
    }

    @Test
    fun `malformed JSON yields an empty map instead of crashing`() {
        assertTrue(BundledPhotos.parseManifest("not json at all").isEmpty())
    }
}
