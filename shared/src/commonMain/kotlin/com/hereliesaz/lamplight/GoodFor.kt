package com.hereliesaz.lamplight

/**
 * A fixed, ordered shortlist of the catalog's existing tags that answer "good for who or what,"
 * not "what kind of place" -- surfaced as their own row on Place Detail (see [goodForTagsIn])
 * so they don't get lost in a venue's full, often much longer, tag list. No new copy: every
 * entry is a tag the catalog already carries verbatim. Order is priority, not alphabetical --
 * broad audience fit, then dietary fit, then occasion fit.
 */
private val GOOD_FOR_TAGS = listOf(
    "Family-Friendly",
    "Solo Traveler Friendly",
    "LGBTQ+",
    "Vegan-friendly",
    "Vegetarian-friendly",
    "First Timer Essential",
    "Cheap Eats",
    "Free Admission",
    "Rainy Day Option"
)

/** Which of [GOOD_FOR_TAGS] a place carries, in [GOOD_FOR_TAGS]'s fixed priority order. */
fun goodForTagsIn(allTags: List<String>): List<String> {
    val tagSet = allTags.toSet()
    return GOOD_FOR_TAGS.filter { it in tagSet }
}
