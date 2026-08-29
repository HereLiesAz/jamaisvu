package com.hereliesaz.lamplight

/**
 * Turns the group-size/vibe answers guests already give (`MoodPrompt`, persisted on
 * [LamplightViewModel]) into a ranking signal, mirroring how Featured surfaces first in
 * Explore's sort (see `ExploreScreen`) rather than filtering anything out: an unmatched place
 * still shows, just without the boost, so no combination of answers can ever produce an empty
 * result set the way a hard filter on a niche vibe easily could.
 *
 * Grounded in the catalog's real tag vocabulary wherever a direct or near-direct match exists
 * (Family-Friendly, First Timer Essential/Tourist Essential, Solo Traveler Friendly are literal
 * tags; Food First/Cocktails First/Music Tonight/Rain Plan reuse Discover's own category
 * membership -- see [discoverCategoriesFor]). The rest are a curated subset of real tags, a
 * genuine judgment call: there's no "romantic" or "business-safe" column in the data, only
 * tags that plausibly correlate with the vibe's own tagline (see [Vibe]).
 *
 * Two answers intentionally contribute nothing, rather than a guessed something:
 * [Vibe.LOW_WALKING] isn't a tag match at all -- proximity is already a sort tiebreaker in
 * ExploreScreen regardless of vibe, so selecting it doesn't need its own mechanism.
 * [GroupSize.SMALL_GROUP] and [GroupSize.LARGE_GROUP] have no corresponding tag anywhere in
 * this catalog (only solo travel is tagged), so they're left as a genuine no-op.
 */
private val VIBE_TAGS: Map<Vibe, Set<String>> = mapOf(
    Vibe.ROMANTIC to setOf(
        "Champagne Bar", "Wine Bar", "Rooftop Bar", "Fine Dining", "Piano Bar", "Courtyard",
        "Grand Dame", "Architecture"
    ),
    Vibe.CURIOUS to setOf(
        "Hidden Gem", "Weird", "Novelty", "Dark Tourism", "Occult", "Witchcraft", "Museum",
        "Escape Room"
    ),
    Vibe.EARLY_TO_BED to setOf("Happy Hour", "Brunch", "Breakfast", "Cafe", "Jazz Brunch"),
    Vibe.NIGHT_OWL to setOf(
        "Late Night", "Late Night Food", "Open After 2AM", "24 Hours", "Dive Bar",
        "Karaoke Bar", "Dance Club"
    ),
    Vibe.EASYGOING to setOf("Casual", "Dive Bar", "Neighborhood Bar", "Cheap Eats", "Casual Dining"),
    // Deliberately narrower than Discover's own HISTORY category, which folds in Occult/
    // Witchcraft/Dark Tourism/Ghost Tour/Haunted -- exactly the "hokum" this vibe's tagline
    // (see Models.kt) says to leave out.
    Vibe.HISTORY_NOT_HOKUM to setOf(
        "Architecture", "Historic Site", "Historic Bar", "Historic Restaurant", "Grand Dame",
        "Old New Orleans", "Local Institution", "Iconic Street", "Tourist Landmark"
    ),
    Vibe.TREAT_US_WELL to setOf(
        "Fine Dining", "Craft Cocktails", "Historic Bar", "Grand Dame", "Hotel Bar", "Steakhouse"
    ),
    Vibe.ON_A_BUDGET to setOf("Cheap Eats", "Happy Hour", "Dive Bar", "Free Admission"),
    Vibe.BUSINESS_SAFE to setOf(
        "Wine Bar", "Craft Cocktails", "Fine Dining", "Historic Bar", "Local Institution",
        "Steakhouse", "Hotel Bar"
    ),
    Vibe.FAMILY_FRIENDLY to setOf("Family-Friendly"),
    Vibe.FIRST_TIME_HERE to setOf("First Timer Essential", "Tourist Essential")
)

private val VIBE_DISCOVER_CATEGORY: Map<Vibe, DiscoverCategory> = mapOf(
    Vibe.FOOD_FIRST to DiscoverCategory.FOOD,
    Vibe.COCKTAILS_FIRST to DiscoverCategory.DRINKS,
    Vibe.MUSIC_TONIGHT to DiscoverCategory.MUSIC,
    Vibe.RAIN_PLAN to DiscoverCategory.INDOOR
)

/** Whether [allTags] plausibly match [vibe] -- see the module doc above for what "match" means per vibe. */
fun vibeMatches(vibe: Vibe, allTags: List<String>): Boolean {
    VIBE_DISCOVER_CATEGORY[vibe]?.let { category -> return category in discoverCategoriesFor(allTags) }
    val tagSet = allTags.toSet()
    return VIBE_TAGS[vibe]?.any { it in tagSet } == true
}

/**
 * How much a place's ranking should be boosted for the guest's chosen vibe/group size -- a
 * small, non-negative integer added on top of whatever else a sort already considers
 * (Featured, proximity). Never negative: this only ever promotes a match, it doesn't punish a
 * non-match, so the worst case for any place is contributing zero, same as answering nothing.
 */
fun moodRelevanceScore(vibe: Vibe?, groupSize: GroupSize?, allTags: List<String>): Int {
    var score = 0
    if (vibe != null && vibeMatches(vibe, allTags)) score += 1
    if (groupSize == GroupSize.SOLO && "Solo Traveler Friendly" in allTags) score += 1
    return score
}
