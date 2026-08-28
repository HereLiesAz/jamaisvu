package com.hereliesaz.lamplight

// Google's own place-type taxonomy (surfaced into PlaceDetailsInfo.tags by
// fetch_place_photos.py's readable_type(), alongside the curated CSV Category Tags and
// vocabulary-matched review terms) uses the same Title Case, space-separated shape as the
// CSV tags -- both land in one case-sensitive lookup here without needing to normalize either.
private val DRINKS_TAGS = setOf(
    "Bar", "Champagne Bar", "Classic Cocktails", "Cocktail History", "Craft Cocktails",
    "Dive Bar", "Dive Bar Adjacent", "Gay Bar", "Hotel Bar", "Irish Pub", "Jazz Bar",
    "Karaoke Bar", "Martini Bar", "Neighborhood Bar", "Piano Bar", "Rooftop Bar",
    "Rotating Bar", "Tiki Bar", "Tourist Bar", "Wine Bar", "Whiskey", "Signature Drinks",
    "Historic Bar", "Brewery", "Craft Coffee", "Coffee"
)

private val FOOD_TAGS = setOf(
    "African Cuisine", "American", "Argentine", "BBQ", "Bakery", "Beignets", "Bistro",
    "Brasserie", "Breakfast", "Brunch", "Jazz Brunch", "Burgers", "Cafe", "Cajun",
    "Caribbean", "Casual Dining", "Cheap Eats", "Cheese", "Chinese", "Cookhouse", "Creole",
    "Cuban", "Dim Sum", "Dinner", "Filipino", "Fine Dining", "Fried Chicken", "French",
    "French Bistro", "Fusion", "Healthy", "Historic Restaurant", "Hot Dogs", "Indian",
    "Italian", "Japanese", "Late Night Food", "Latin American", "Louisiana Cuisine",
    "Mediterranean", "Mexican", "Middle Eastern", "Muffuletta", "New American", "Oysters",
    "Po-boys", "Pizza", "Restaurant", "Sandwiches", "Sausage", "Seafood", "Soul Food",
    "Southern", "Steakhouse", "Sushi", "Tacos", "Tapas", "Thai", "Vegan-friendly",
    "Vegetarian-friendly", "Wings"
)

private val HAPPY_HOUR_TAGS = setOf("Happy Hour")

private val MUSIC_TAGS = setOf(
    "DJ", "Dance Club", "Jazz", "Jazz Bar", "Jazz Brunch", "Jazz Essential", "Karaoke Bar",
    "Live Music", "Live Performance", "Piano Bar", "Cabaret", "Rockabilly", "Punk"
)

private val SHOPS_TAGS = setOf("Shopping", "Market")

private val INDOOR_TAGS = setOf("Museum", "Casino", "Escape Room", "Theater", "Visitor Center")

private val LATE_TAGS = setOf("Late Night", "Late Night Food", "Open After 2AM", "24 Hours")

private val HISTORY_TAGS = setOf(
    "Architecture", "Authentic Cultural Practice", "Cemetery", "Cemetery Tour",
    "Cocktail History", "Culture", "Dark Tourism", "Ghost Tour", "Gothic", "Grand Dame",
    "Guided Tour", "Haunted", "Historic Bar", "Historic Restaurant", "Historic Site",
    "Iconic Street", "Local Institution", "Occult", "Old New Orleans",
    "Resistance Landmark", "Riverboat", "Riverboat Cruise", "Tourist Landmark",
    "Voodoo Culture", "Walking Tour", "Witchcraft", "Weird", "Goth"
)

private val CATEGORY_TAGS: Map<DiscoverCategory, Set<String>> = mapOf(
    DiscoverCategory.DRINKS to DRINKS_TAGS,
    DiscoverCategory.FOOD to FOOD_TAGS,
    DiscoverCategory.HAPPY_HOUR to HAPPY_HOUR_TAGS,
    DiscoverCategory.MUSIC to MUSIC_TAGS,
    DiscoverCategory.SHOPS to SHOPS_TAGS,
    DiscoverCategory.INDOOR to INDOOR_TAGS,
    DiscoverCategory.LATE to LATE_TAGS,
    DiscoverCategory.HISTORY to HISTORY_TAGS
)

/**
 * Maps a place's existing tags (its own curated CSV categories, plus whatever Google
 * place-types and review keywords ended up in its [PlaceDetailsInfo.tags]) onto the client
 * brief's fixed eight Discover categories. A place can land in any number of these,
 * including zero: most of the catalog's tag vocabulary describes something more specific
 * than these eight deliberately broad buckets ("Solo Traveler Friendly" has no Discover
 * category, for instance), and that's expected -- Discover is a curated entry point, not a
 * guarantee every place is reachable from it.
 */
fun discoverCategoriesFor(allTags: List<String>): Set<DiscoverCategory> {
    val tagSet = allTags.toSet()
    return CATEGORY_TAGS.filterValues { categoryTags -> categoryTags.any { it in tagSet } }.keys
}
