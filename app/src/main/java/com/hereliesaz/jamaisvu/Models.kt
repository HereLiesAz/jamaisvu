package com.hereliesaz.jamaisvu

data class Place(
    val id: String,
    val venue: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>
)
