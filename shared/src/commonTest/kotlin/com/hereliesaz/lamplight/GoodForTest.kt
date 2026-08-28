package com.hereliesaz.lamplight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GoodForTest {

    @Test
    fun `a family-friendly museum surfaces its good-for tag`() {
        assertEquals(listOf("Family-Friendly"), goodForTagsIn(listOf("Museum", "Family-Friendly", "Tourist Essential")))
    }

    @Test
    fun `matches come back in fixed priority order, not input order`() {
        val tags = listOf("Cheap Eats", "Family-Friendly", "Vegan-friendly")

        assertEquals(listOf("Family-Friendly", "Vegan-friendly", "Cheap Eats"), goodForTagsIn(tags))
    }

    @Test
    fun `a place with no good-for tags yields an empty list, not a crash`() {
        assertTrue(goodForTagsIn(listOf("Dive Bar", "Local Institution")).isEmpty())
    }

    @Test
    fun `no tags at all yields an empty list`() {
        assertTrue(goodForTagsIn(emptyList()).isEmpty())
    }

    @Test
    fun `a place can carry multiple good-for tags at once`() {
        val tags = listOf("Restaurant", "Vegan-friendly", "Vegetarian-friendly", "Cheap Eats")

        assertEquals(listOf("Vegan-friendly", "Vegetarian-friendly", "Cheap Eats"), goodForTagsIn(tags))
    }
}
