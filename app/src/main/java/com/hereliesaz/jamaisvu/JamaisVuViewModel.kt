package com.hereliesaz.jamaisvu

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class JamaisVuViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("jamaisvu", Context.MODE_PRIVATE)
    private val photoRepository = PlacePhotoRepository(application)
    private val saved = mutableStateMapOf<String, Boolean>()
    private val visited = mutableStateMapOf<String, Boolean>()
    private val photoGalleries = mutableStateMapOf<String, PlacePhotoGallery>()

    val places: List<Place> = QuarterMuseSeed.load(application)
    val tags: List<String> = places.flatMap { it.tags }.distinct().sorted()
    val photosConfigured: Boolean get() = photoRepository.configured

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

    fun photoGallery(placeId: String): PlacePhotoGallery =
        photoGalleries[placeId] ?: PlacePhotoGallery()

    fun ensurePhotos(place: Place, requestedCount: Int) {
        if (!photosConfigured || requestedCount <= 0) return
        val current = photoGallery(place.id)
        if (current.isLoading || current.exhausted || current.photos.size >= requestedCount) return

        photoGalleries[place.id] = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val startCount = current.photos.size
            runCatching {
                photoRepository.loadPhotos(
                    place = place,
                    alreadyLoaded = startCount,
                    targetCount = requestedCount.coerceAtMost(5)
                )
            }.onSuccess { result ->
                val latest = photoGallery(place.id)
                val merged = (latest.photos + result.photos).distinctBy { it.uri }
                photoGalleries[place.id] = PlacePhotoGallery(
                    photos = merged,
                    isLoading = false,
                    exhausted = !result.hasMore,
                    error = if (merged.isEmpty()) "No Google Maps photo found" else null
                )
            }.onFailure { error ->
                photoGalleries[place.id] = current.copy(
                    isLoading = false,
                    error = error.message ?: "Could not load Google Maps photos"
                )
            }
        }
    }

    fun retryPhotos(place: Place, requestedCount: Int) {
        val current = photoGallery(place.id)
        photoGalleries[place.id] = current.copy(error = null, exhausted = false)
        ensurePhotos(place, requestedCount)
    }

    private fun persistTravelState() {
        prefs.edit()
            .putStringSet("saved", saved.keys.toSet())
            .putStringSet("visited", visited.keys.toSet())
            .apply()
    }
}
