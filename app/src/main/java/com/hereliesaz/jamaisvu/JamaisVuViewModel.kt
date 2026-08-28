package com.hereliesaz.jamaisvu

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel

class JamaisVuViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("jamaisvu", Context.MODE_PRIVATE)
    private val saved = mutableStateMapOf<String, Boolean>()
    private val visited = mutableStateMapOf<String, Boolean>()
    private val photosByPlace: Map<String, List<PlacePhoto>> = BundledPhotos.load(application)

    val places: List<Place> = QuarterMuseSeed.load(application)
    val tags: List<String> = places.flatMap { it.tags }.distinct().sorted()
    val photosConfigured: Boolean = photosByPlace.isNotEmpty()

    init {
        prefs.getStringSet("saved", emptySet()).orEmpty().forEach { saved[it] = true }
        prefs.getStringSet("visited", emptySet()).orEmpty().forEach { visited[it] = true }
    }

    fun isSaved(id: String): Boolean = saved[id] == true

    fun isVisited(id: String): Boolean = visited[id] == true

    fun toggleSaved(id: String) {
        if (isSaved(id)) saved.remove(id) else saved[id] = true
        persistTravelState()
    }

    fun toggleVisited(id: String) {
        if (isVisited(id)) visited.remove(id) else visited[id] = true
        persistTravelState()
    }

    fun photos(placeId: String): List<PlacePhoto> = photosByPlace[placeId].orEmpty()

    private fun persistTravelState() {
        prefs.edit()
            .putStringSet("saved", saved.keys.toSet())
            .putStringSet("visited", visited.keys.toSet())
            .apply()
    }
}
