package com.hereliesaz.lamplight

data class Place(
    val id: String,
    val venue: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>
)

data class PhotoAuthor(
    val name: String,
    val uri: String? = null
)

data class PlacePhoto(
    val uri: String,
    val authors: List<PhotoAuthor> = emptyList(),
    val googleMapsUri: String? = null
)

/** The guest's saved "Home Lantern" — a fixed point the whole stay is planned around. No account, device-local only. */
data class HotelAnchor(
    val label: String,
    val latitude: Double,
    val longitude: Double
)

/** A known hotel from the bundled hotel catalog, for the picker list and proximity detection. */
data class Hotel(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)
