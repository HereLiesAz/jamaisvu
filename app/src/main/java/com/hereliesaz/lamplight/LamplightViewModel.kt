package com.hereliesaz.lamplight

import android.app.Application
import android.content.Context
import android.location.Location
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LamplightViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("lamplight", Context.MODE_PRIVATE)
    private val saved = mutableStateMapOf<String, Boolean>()
    private val visited = mutableStateMapOf<String, Boolean>()
    private val seen = mutableStateMapOf<String, Boolean>()
    private val photosByPlace: Map<String, List<PlacePhoto>> = BundledPhotos.load(application)
    private val placeDetailsByPlace: Map<String, PlaceDetailsInfo> = BundledPlaceDetails.load(application)
    private val githubUpdateState = mutableStateOf<GitHubUpdate?>(null)
    private val hotelAnchorState = mutableStateOf(loadHotelAnchor())
    private val hotelPromptAnsweredState = mutableStateOf(
        hotelAnchorState.value != null || prefs.getBoolean(KEY_HOTEL_SKIPPED, false)
    )
    private val currentLocationState = mutableStateOf<Location?>(null)
    private val detectedHotelState = mutableStateOf<Hotel?>(null)
    private val groupSizeState = mutableStateOf(loadGroupSize())
    private val vibeState = mutableStateOf(loadVibe())
    private val moodPromptAnsweredState = mutableStateOf(
        groupSizeState.value != null || vibeState.value != null || prefs.getBoolean(KEY_MOOD_SKIPPED, false)
    )

    val places: List<Place> = QuarterMuseSeed.load(application)
    val tags: List<String> = places.flatMap { it.tags }.distinct().sorted()
    val hotels: List<Hotel> = HotelCatalog.load(application)
    val photosConfigured: Boolean = photosByPlace.isNotEmpty()
    val installSource: InstallSource = detectInstallSource(application)
    val githubUpdate: GitHubUpdate? get() = githubUpdateState.value

    /** The guest's Home Lantern, or null if they haven't set one (or chose "not staying at a hotel"). */
    val hotelAnchor: HotelAnchor? get() = hotelAnchorState.value

    /** False only until the guest has answered the first-open hotel prompt one way or another. */
    val hasAnsweredHotelPrompt: Boolean get() = hotelPromptAnsweredState.value

    /** The device's last fetched location, for immediate relevance sorting before a hotel is confirmed. Session-only, never persisted. */
    val currentLocation: Location? get() = currentLocationState.value

    /** A known hotel whose coordinates are suspiciously close to [currentLocation], awaiting a yes/no from the guest. */
    val detectedHotel: Hotel? get() = detectedHotelState.value

    val groupSize: GroupSize? get() = groupSizeState.value
    val vibe: Vibe? get() = vibeState.value

    /** False only until the guest has answered the group-size/vibe prompt one way or another. */
    val hasAnsweredMoodPrompt: Boolean get() = moodPromptAnsweredState.value

    init {
        prefs.getStringSet("saved", emptySet()).orEmpty().forEach { saved[it] = true }
        prefs.getStringSet("visited", emptySet()).orEmpty().forEach { visited[it] = true }
        prefs.getStringSet("seen", emptySet()).orEmpty().forEach { seen[it] = true }

        // Only a sideloaded install should ever be told about a GitHub release; a Play
        // install's update path is handled entirely separately, via Play Core, in the UI layer.
        if (installSource == InstallSource.OTHER) {
            viewModelScope.launch {
                githubUpdateState.value = fetchGitHubUpdate(application)
            }
        }
    }

    fun isSaved(id: String): Boolean = saved[id] == true

    fun isVisited(id: String): Boolean = visited[id] == true

    /** "Seen" is a one-way, auto-tracked record of having opened a place's detail screen -- distinct from "Been", which the guest marks deliberately for an actual real-world visit. */
    fun isSeen(id: String): Boolean = seen[id] == true

    fun toggleSaved(id: String) {
        if (isSaved(id)) saved.remove(id) else saved[id] = true
        persistTravelState()
    }

    fun toggleVisited(id: String) {
        if (isVisited(id)) visited.remove(id) else visited[id] = true
        persistTravelState()
    }

    /** Marks a place seen the first time its detail screen opens. A no-op after that -- seen has no "un-see". */
    fun markSeen(id: String) {
        if (isSeen(id)) return
        seen[id] = true
        persistTravelState()
    }

    fun photos(placeId: String): List<PlacePhoto> = photosByPlace[placeId].orEmpty()

    /** Business details bundled at build time (phone, hours, website, address, extra search tags). */
    fun placeDetails(placeId: String): PlaceDetailsInfo = placeDetailsByPlace[placeId] ?: PlaceDetailsInfo()

    /** Saves the Home Lantern. Label is display-only; only lat/lng drive walk-time and "Take me back". */
    fun setHotelAnchor(label: String, latitude: Double, longitude: Double) {
        val anchor = HotelAnchor(label.ifBlank { "Your hotel" }, latitude, longitude)
        hotelAnchorState.value = anchor
        hotelPromptAnsweredState.value = true
        prefs.edit()
            .putString(KEY_HOTEL_LABEL, anchor.label)
            .putString(KEY_HOTEL_LAT, anchor.latitude.toString())
            .putString(KEY_HOTEL_LNG, anchor.longitude.toString())
            .putBoolean(KEY_HOTEL_SKIPPED, false)
            .apply()
    }

    /** "I'm not staying at a hotel" -- answers the prompt without setting an anchor. */
    fun skipHotelAnchor() {
        hotelAnchorState.value = null
        hotelPromptAnsweredState.value = true
        prefs.edit()
            .remove(KEY_HOTEL_LABEL)
            .remove(KEY_HOTEL_LAT)
            .remove(KEY_HOTEL_LNG)
            .putBoolean(KEY_HOTEL_SKIPPED, true)
            .apply()
    }

    /**
     * Feeds in a fresh location fix -- used immediately for proximity sorting, and (only while
     * the guest hasn't answered the hotel prompt yet) checked against the hotel catalog for a
     * close match worth confirming.
     */
    fun setCurrentLocation(location: Location) {
        currentLocationState.value = location
        if (!hasAnsweredHotelPrompt) {
            detectedHotelState.value = nearestHotelWithin(location.latitude, location.longitude, hotels)
        }
    }

    /** "Yes, that's my hotel" -- adopts the detected hotel's own coordinates, not the raw GPS fix. */
    fun confirmDetectedHotel() {
        val hotel = detectedHotelState.value ?: return
        setHotelAnchor(hotel.name, hotel.latitude, hotel.longitude)
        detectedHotelState.value = null
    }

    /** "No, let me choose" -- dismisses the suggestion without answering the prompt itself. */
    fun dismissDetectedHotel() {
        detectedHotelState.value = null
    }

    /** Answers both questions together -- "Who's out tonight?" and "What are we in the mood for?" */
    fun setMood(groupSize: GroupSize, vibe: Vibe) {
        groupSizeState.value = groupSize
        vibeState.value = vibe
        moodPromptAnsweredState.value = true
        prefs.edit()
            .putString(KEY_GROUP_SIZE, groupSize.name)
            .putString(KEY_VIBE, vibe.name)
            .putBoolean(KEY_MOOD_SKIPPED, false)
            .apply()
    }

    /** Answers the prompt without picking anything -- the guest can still reopen it later. */
    fun skipMoodPrompt() {
        moodPromptAnsweredState.value = true
        prefs.edit().putBoolean(KEY_MOOD_SKIPPED, true).apply()
    }

    private fun loadGroupSize(): GroupSize? =
        prefs.getString(KEY_GROUP_SIZE, null)?.let { name -> GroupSize.entries.find { it.name == name } }

    private fun loadVibe(): Vibe? =
        prefs.getString(KEY_VIBE, null)?.let { name -> Vibe.entries.find { it.name == name } }

    private fun loadHotelAnchor(): HotelAnchor? {
        val label = prefs.getString(KEY_HOTEL_LABEL, null) ?: return null
        val lat = prefs.getString(KEY_HOTEL_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_HOTEL_LNG, null)?.toDoubleOrNull() ?: return null
        return HotelAnchor(label, lat, lng)
    }

    private fun persistTravelState() {
        prefs.edit()
            .putStringSet("saved", saved.keys.toSet())
            .putStringSet("visited", visited.keys.toSet())
            .putStringSet("seen", seen.keys.toSet())
            .apply()
    }

    private companion object {
        const val KEY_HOTEL_LABEL = "hotel_label"
        const val KEY_HOTEL_LAT = "hotel_lat"
        const val KEY_HOTEL_LNG = "hotel_lng"
        const val KEY_HOTEL_SKIPPED = "hotel_skipped"
        const val KEY_GROUP_SIZE = "group_size"
        const val KEY_VIBE = "vibe"
        const val KEY_MOOD_SKIPPED = "mood_skipped"
    }
}
