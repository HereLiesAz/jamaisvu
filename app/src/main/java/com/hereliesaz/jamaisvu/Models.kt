package com.hereliesaz.jamaisvu

data class Gem(
    val id: String,
    val title: String,
    val city: String,
    val neighborhood: String,
    val category: String,
    val tip: String,
    val username: String,
    val image: String,
    val isUserAdded: Boolean = false
)

data class Creator(val handle: String, val city: String, val gemCount: Int)

object DemoData {
    val cities = listOf("New Orleans", "New York City", "Los Angeles", "Seattle", "Boston", "Chicago")
    val categories = listOf("Eat", "Drink", "Play", "Stay", "Outside", "Art", "Weird")
    val creators = listOf(
        Creator("@nightbus", "New Orleans", 84),
        Creator("@tinydoors", "New York City", 71),
        Creator("@softserve", "Los Angeles", 63),
        Creator("@feralweekday", "Seattle", 58)
    )

    val gems = listOf(
        Gem("nola-1", "Crescent Vinyl Room", "New Orleans", "Marigny", "Drink", "Go late, sit near the back, and let the room decide what happens next.", "@nightbus", "https://picsum.photos/seed/jv-nola-1/1000/1000"),
        Gem("nola-2", "Backstreet Espresso", "New Orleans", "Bywater", "Eat", "Tiny counter, excellent coffee, no reason to bring a laptop unless it owes you money.", "@softserve", "https://picsum.photos/seed/jv-nola-2/1000/1000"),
        Gem("nola-3", "The Pocket Museum", "New Orleans", "Tremé", "Art", "Small enough to miss. Strange enough to remember.", "@tinydoors", "https://picsum.photos/seed/jv-nola-3/1000/1000"),
        Gem("nola-4", "Lantern Courtyard", "New Orleans", "French Quarter", "Play", "Best after dark. Bring somebody who doesn't need a schedule.", "@feralweekday", "https://picsum.photos/seed/jv-nola-4/1000/1000"),
        Gem("nola-5", "Riverwall Steps", "New Orleans", "Lower Garden", "Outside", "Sunset, headphones, no agenda.", "@nightbus", "https://picsum.photos/seed/jv-nola-5/1000/1000"),
        Gem("nola-6", "Odd Hours Books", "New Orleans", "Uptown", "Weird", "The shelf labels are suggestions. The owner is not.", "@tinydoors", "https://picsum.photos/seed/jv-nola-6/1000/1000"),
        Gem("nyc-1", "Basement Noodles", "New York City", "Chinatown", "Eat", "Order whatever they are making fastest.", "@tinydoors", "https://picsum.photos/seed/jv-nyc-1/1000/1000"),
        Gem("nyc-2", "Roofline Cinema", "New York City", "Brooklyn", "Play", "A movie, a skyline, and the agreeable suspicion that you found the wrong elevator.", "@nightbus", "https://picsum.photos/seed/jv-nyc-2/1000/1000"),
        Gem("la-1", "Sunset Plant Motel", "Los Angeles", "Echo Park", "Stay", "More plants than furniture. This is a compliment.", "@softserve", "https://picsum.photos/seed/jv-la-1/1000/1000"),
        Gem("la-2", "Concrete Picnic", "Los Angeles", "Arts District", "Eat", "Get the thing wrapped in paper. Sit outside.", "@softserve", "https://picsum.photos/seed/jv-la-2/1000/1000"),
        Gem("sea-1", "Rain Cabinet", "Seattle", "Capitol Hill", "Drink", "A bar built for weather that has given up apologizing.", "@feralweekday", "https://picsum.photos/seed/jv-sea-1/1000/1000"),
        Gem("bos-1", "Second Floor Bakery", "Boston", "Somerville", "Eat", "The staircase looks wrong. The pastry case is right.", "@nightbus", "https://picsum.photos/seed/jv-bos-1/1000/1000")
    )
}
