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

/** One opening-hours span. Google's day convention: 0=Sunday..6=Saturday. A null close means no recorded close boundary. */
data class OpeningPeriod(
    val openDay: Int,
    val openTime: String,
    val closeDay: Int?,
    val closeTime: String?
)

/**
 * Business details bundled at build time from scripts/fetch_place_photos.py -- phone, website,
 * address, hours, and search tags (the curated Category Tags column plus Google's place types
 * and vocabulary-matched review terms; never review text itself). Every field defaults empty,
 * so a venue with no matched Google Place still gets a valid, "nothing to show" instance.
 */
data class PlaceDetailsInfo(
    val phone: String? = null,
    val website: String? = null,
    val address: String? = null,
    val weekdayDescriptions: List<String> = emptyList(),
    val periods: List<OpeningPeriod> = emptyList(),
    val tags: List<String> = emptyList()
)
