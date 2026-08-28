package com.hereliesaz.lamplight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiscoverCategoriesTest {

    @Test
    fun `a jazz bar lands in both Drinks and Music`() {
        val categories = discoverCategoriesFor(listOf("Jazz Bar", "Live Music"))

        assertEquals(setOf(DiscoverCategory.DRINKS, DiscoverCategory.MUSIC), categories)
    }

    @Test
    fun `happy hour is its own narrow category, not folded into Drinks or Food`() {
        val categories = discoverCategoriesFor(listOf("Happy Hour"))

        assertEquals(setOf(DiscoverCategory.HAPPY_HOUR), categories)
    }

    @Test
    fun `a tag with no Discover-category home yields an empty set, not a crash`() {
        assertTrue(discoverCategoriesFor(listOf("Solo Traveler Friendly", "Instagrammable")).isEmpty())
    }

    @Test
    fun `no tags at all yields an empty set`() {
        assertTrue(discoverCategoriesFor(emptyList()).isEmpty())
    }

    @Test
    fun `a ghost tour lands in History only`() {
        val categories = discoverCategoriesFor(listOf("Ghost Tour", "Guided Tour", "Haunted"))

        assertEquals(setOf(DiscoverCategory.HISTORY), categories)
    }

    @Test
    fun `a 24-hour diner lands in Food and Late`() {
        val categories = discoverCategoriesFor(listOf("Restaurant", "24 Hours"))

        assertEquals(setOf(DiscoverCategory.FOOD, DiscoverCategory.LATE), categories)
    }

    @Test
    fun `every one of the eight categories is reachable by at least one real tag`() {
        for (category in DiscoverCategory.entries) {
            val matchingTag = when (category) {
                DiscoverCategory.DRINKS -> "Bar"
                DiscoverCategory.FOOD -> "Restaurant"
                DiscoverCategory.HAPPY_HOUR -> "Happy Hour"
                DiscoverCategory.MUSIC -> "Live Music"
                DiscoverCategory.SHOPS -> "Shopping"
                DiscoverCategory.INDOOR -> "Museum"
                DiscoverCategory.LATE -> "Late Night"
                DiscoverCategory.HISTORY -> "Historic Site"
            }
            assertTrue(
                category in discoverCategoriesFor(listOf(matchingTag)),
                "Expected '$matchingTag' to map to $category"
            )
        }
    }
}
