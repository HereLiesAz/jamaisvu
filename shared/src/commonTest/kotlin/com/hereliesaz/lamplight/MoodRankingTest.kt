package com.hereliesaz.lamplight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoodRankingTest {

    @Test
    fun `a wine bar matches Romantic`() {
        assertTrue(vibeMatches(Vibe.ROMANTIC, listOf("Wine Bar")))
    }

    @Test
    fun `Food First, Cocktails First, Music Tonight, and Rain Plan reuse Discover's own categories`() {
        assertTrue(vibeMatches(Vibe.FOOD_FIRST, listOf("Restaurant")))
        assertTrue(vibeMatches(Vibe.COCKTAILS_FIRST, listOf("Bar")))
        assertTrue(vibeMatches(Vibe.MUSIC_TONIGHT, listOf("Live Music")))
        assertTrue(vibeMatches(Vibe.RAIN_PLAN, listOf("Museum")))
    }

    @Test
    fun `History, Not Hokum excludes the occult tags Discover's own History category includes`() {
        assertTrue(vibeMatches(Vibe.HISTORY_NOT_HOKUM, listOf("Historic Site")))
        assertFalse(vibeMatches(Vibe.HISTORY_NOT_HOKUM, listOf("Occult", "Ghost Tour", "Haunted")))
    }

    @Test
    fun `Family-Friendly and First Time Here match their literal catalog tags`() {
        assertTrue(vibeMatches(Vibe.FAMILY_FRIENDLY, listOf("Family-Friendly")))
        assertTrue(vibeMatches(Vibe.FIRST_TIME_HERE, listOf("First Timer Essential")))
        assertTrue(vibeMatches(Vibe.FIRST_TIME_HERE, listOf("Tourist Essential")))
    }

    @Test
    fun `Low Walking never matches any tags -- it's a distance preference, not a tag match`() {
        for (tags in listOf(listOf("Wine Bar"), listOf("Family-Friendly"), emptyList())) {
            assertFalse(vibeMatches(Vibe.LOW_WALKING, tags))
        }
    }

    @Test
    fun `a place with none of a vibe's tags doesn't match`() {
        assertFalse(vibeMatches(Vibe.ROMANTIC, listOf("Dive Bar", "Cheap Eats")))
    }

    @Test
    fun `every vibe except Low Walking is reachable by at least one real tag`() {
        for (vibe in Vibe.entries) {
            if (vibe == Vibe.LOW_WALKING) continue
            val matchingTag = when (vibe) {
                Vibe.ROMANTIC -> "Champagne Bar"
                Vibe.CURIOUS -> "Hidden Gem"
                Vibe.EARLY_TO_BED -> "Brunch"
                Vibe.NIGHT_OWL -> "24 Hours"
                Vibe.EASYGOING -> "Casual"
                Vibe.FOOD_FIRST -> "Seafood"
                Vibe.COCKTAILS_FIRST -> "Craft Cocktails"
                Vibe.MUSIC_TONIGHT -> "Jazz"
                Vibe.HISTORY_NOT_HOKUM -> "Local Institution"
                Vibe.RAIN_PLAN -> "Casino"
                Vibe.TREAT_US_WELL -> "Steakhouse"
                Vibe.ON_A_BUDGET -> "Free Admission"
                Vibe.BUSINESS_SAFE -> "Wine Bar"
                Vibe.FAMILY_FRIENDLY -> "Family-Friendly"
                Vibe.LOW_WALKING -> error("excluded above")
                Vibe.FIRST_TIME_HERE -> "Tourist Essential"
            }
            assertTrue(vibeMatches(vibe, listOf(matchingTag)), "Expected '$matchingTag' to match $vibe")
        }
    }

    @Test
    fun `moodRelevanceScore is zero with no vibe, no group size, and no matching tags`() {
        assertEquals(0, moodRelevanceScore(vibe = null, groupSize = null, allTags = listOf("Dive Bar")))
    }

    @Test
    fun `moodRelevanceScore adds one for a matching vibe`() {
        assertEquals(1, moodRelevanceScore(vibe = Vibe.ROMANTIC, groupSize = null, allTags = listOf("Wine Bar")))
    }

    @Test
    fun `moodRelevanceScore adds one for Solo plus the Solo Traveler Friendly tag`() {
        assertEquals(1, moodRelevanceScore(vibe = null, groupSize = GroupSize.SOLO, allTags = listOf("Solo Traveler Friendly")))
    }

    @Test
    fun `Small Group and Large Group never contribute, even with the Solo tag present`() {
        assertEquals(0, moodRelevanceScore(vibe = null, groupSize = GroupSize.SMALL_GROUP, allTags = listOf("Solo Traveler Friendly")))
        assertEquals(0, moodRelevanceScore(vibe = null, groupSize = GroupSize.LARGE_GROUP, allTags = listOf("Solo Traveler Friendly")))
    }

    @Test
    fun `a matching vibe and Solo together add up, never negative, never more than both signals`() {
        val score = moodRelevanceScore(vibe = Vibe.ROMANTIC, groupSize = GroupSize.SOLO, allTags = listOf("Wine Bar", "Solo Traveler Friendly"))
        assertEquals(2, score)
    }
}
