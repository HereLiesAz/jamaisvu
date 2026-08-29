package com.hereliesaz.lamplight

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/** The venue/hotel catalogs and their bundled photos/business details, loaded together once at startup. */
private data class Catalog(
    val places: List<Place>,
    val hotels: List<Hotel>,
    val photosByPlace: Map<String, List<PlacePhoto>>,
    val placeDetailsByPlace: Map<String, PlaceDetailsInfo>
)

/**
 * The GitHub-releases self-update surface (installSource/githubUpdate/etc.) deliberately does
 * NOT live here -- it's Android-only, no web counterpart ever (see [GitHubUpdateController]),
 * so it's owned and constructed by `:androidApp` directly and fed into `LamplightHome`'s
 * `platformBanner` slot instead of threading through the shared ViewModel.
 */
class LamplightViewModel(private val settingsStore: SettingsStore) : ViewModel() {
    private val saved = mutableStateMapOf<String, Boolean>()
    private val visited = mutableStateMapOf<String, Boolean>()
    private val seen = mutableStateMapOf<String, Boolean>()
    // Reading the bundled CSV/JSON now goes through Compose resources, which is suspend on
    // every target (web needs fetch()) -- null until that first load completes. Every place/
    // hotel/tag/photo-related property below reads through this and degrades to empty/false
    // rather than exposing the null directly, so existing call sites (and the proximity check
    // in setCurrentLocation) don't need their own loading-state handling; the one real
    // consequence is a location fix that arrives before this finishes won't have hotels to
    // match against yet, same as it having no fix at all.
    private val catalogState = mutableStateOf<Catalog?>(null)
    private val hotelAnchorState = mutableStateOf(loadHotelAnchor())
    private val hotelPromptAnsweredState = mutableStateOf(
        hotelAnchorState.value != null || settingsStore.getBoolean(KEY_HOTEL_SKIPPED)
    )
    private val currentLocationState = mutableStateOf<GeoPosition?>(null)
    private val detectedHotelState = mutableStateOf<Hotel?>(null)
    private val groupSizeState = mutableStateOf(loadGroupSize())
    private val vibeState = mutableStateOf(loadVibe())
    private val moodPromptAnsweredState = mutableStateOf(
        groupSizeState.value != null || vibeState.value != null || settingsStore.getBoolean(KEY_MOOD_SKIPPED)
    )

    val places: List<Place> get() = catalogState.value?.places.orEmpty()
    val hotels: List<Hotel> get() = catalogState.value?.hotels.orEmpty()
    val photosConfigured: Boolean get() = catalogState.value?.photosByPlace?.isNotEmpty() == true

    /** The guest's Home Lantern, or null if they haven't set one (or chose "not staying at a hotel"). */
    val hotelAnchor: HotelAnchor? get() = hotelAnchorState.value

    /** False only until the guest has answered the first-open hotel prompt one way or another. */
    val hasAnsweredHotelPrompt: Boolean get() = hotelPromptAnsweredState.value

    /** The device's last fetched location, for immediate relevance sorting before a hotel is confirmed. Session-only, never persisted. */
    val currentLocation: GeoPosition? get() = currentLocationState.value

    /** A known hotel whose coordinates are suspiciously close to [currentLocation], awaiting a yes/no from the guest. */
    val detectedHotel: Hotel? get() = detectedHotelState.value

    val groupSize: GroupSize? get() = groupSizeState.value
    val vibe: Vibe? get() = vibeState.value

    /** False only until the guest has answered the group-size/vibe prompt one way or another. */
    val hasAnsweredMoodPrompt: Boolean get() = moodPromptAnsweredState.value

    init {
        settingsStore.getStringSet("saved").forEach { saved[it] = true }
        settingsStore.getStringSet("visited").forEach { visited[it] = true }
        settingsStore.getStringSet("seen").forEach { seen[it] = true }

        viewModelScope.launch {
            catalogState.value = Catalog(
                places = QuarterMuseSeed.load(),
                hotels = HotelCatalog.load(),
                photosByPlace = BundledPhotos.load(),
                placeDetailsByPlace = BundledPlaceDetails.load()
            )
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

    fun photos(placeId: String): List<PlacePhoto> = catalogState.value?.photosByPlace?.get(placeId).orEmpty()

    /** Business details bundled at build time (phone, hours, website, address, extra search tags). */
    fun placeDetails(placeId: String): PlaceDetailsInfo = catalogState.value?.placeDetailsByPlace?.get(placeId) ?: PlaceDetailsInfo()

    /** Saves the Home Lantern. Label is display-only; only lat/lng drive walk-time and "Take me back". */
    fun setHotelAnchor(label: String, latitude: Double, longitude: Double) {
        val anchor = HotelAnchor(label.ifBlank { "Your hotel" }, latitude, longitude)
        hotelAnchorState.value = anchor
        hotelPromptAnsweredState.value = true
        settingsStore.putString(KEY_HOTEL_LABEL, anchor.label)
        settingsStore.putString(KEY_HOTEL_LAT, anchor.latitude.toString())
        settingsStore.putString(KEY_HOTEL_LNG, anchor.longitude.toString())
        settingsStore.putBoolean(KEY_HOTEL_SKIPPED, false)
    }

    /** "I'm not staying at a hotel" -- answers the prompt without setting an anchor. */
    fun skipHotelAnchor() {
        hotelAnchorState.value = null
        hotelPromptAnsweredState.value = true
        settingsStore.remove(KEY_HOTEL_LABEL)
        settingsStore.remove(KEY_HOTEL_LAT)
        settingsStore.remove(KEY_HOTEL_LNG)
        settingsStore.putBoolean(KEY_HOTEL_SKIPPED, true)
    }

    /**
     * Feeds in a fresh location fix -- used immediately for proximity sorting, and (only while
     * the guest hasn't answered the hotel prompt yet) checked against the hotel catalog for a
     * close match worth confirming.
     */
    fun setCurrentLocation(location: GeoPosition) {
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
        settingsStore.putString(KEY_GROUP_SIZE, groupSize.name)
        settingsStore.putString(KEY_VIBE, vibe.name)
        settingsStore.putBoolean(KEY_MOOD_SKIPPED, false)
    }

    /** Answers the prompt without picking anything -- the guest can still reopen it later. */
    fun skipMoodPrompt() {
        moodPromptAnsweredState.value = true
        settingsStore.putBoolean(KEY_MOOD_SKIPPED, true)
    }

    private fun loadGroupSize(): GroupSize? =
        settingsStore.getString(KEY_GROUP_SIZE)?.let { name -> GroupSize.entries.find { it.name == name } }

    private fun loadVibe(): Vibe? =
        settingsStore.getString(KEY_VIBE)?.let { name -> Vibe.entries.find { it.name == name } }

    private fun loadHotelAnchor(): HotelAnchor? {
        val label = settingsStore.getString(KEY_HOTEL_LABEL) ?: return null
        val lat = settingsStore.getString(KEY_HOTEL_LAT)?.toDoubleOrNull() ?: return null
        val lng = settingsStore.getString(KEY_HOTEL_LNG)?.toDoubleOrNull() ?: return null
        return HotelAnchor(label, lat, lng)
    }

    private fun persistTravelState() {
        settingsStore.putStringSet("saved", saved.keys.toSet())
        settingsStore.putStringSet("visited", visited.keys.toSet())
        settingsStore.putStringSet("seen", seen.keys.toSet())
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
