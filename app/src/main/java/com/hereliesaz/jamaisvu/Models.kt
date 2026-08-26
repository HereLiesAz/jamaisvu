package com.hereliesaz.jamaisvu

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
    val attributionHtml: String = "",
    val authors: List<PhotoAuthor> = emptyList(),
    val googleMapsUri: String? = null
)

data class PlacePhotoGallery(
    val photos: List<PlacePhoto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
