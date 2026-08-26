package com.hereliesaz.jamaisvu

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place as GooglePlace
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.kotlin.awaitFetchPlace
import com.google.android.libraries.places.api.net.kotlin.awaitFetchResolvedPhotoUri
import com.google.android.libraries.places.api.net.kotlin.awaitSearchByText

class PlacePhotoRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("jamaisvu_google_places", Context.MODE_PRIVATE)
    private val apiKey = BuildConfig.GOOGLE_PLACES_API_KEY.trim()
    private val client: PlacesClient?

    val configured: Boolean
        get() = apiKey.isNotBlank()

    init {
        client = if (configured) {
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(appContext, apiKey)
            }
            Places.createClient(appContext)
        } else {
            null
        }
    }

    suspend fun loadPhotos(place: Place, alreadyLoaded: Int, targetCount: Int): List<PlacePhoto> {
        val placesClient = client ?: return emptyList()
        if (targetCount <= alreadyLoaded) return emptyList()

        val placeId = resolvePlaceId(placesClient, place) ?: return emptyList()
        val remote = fetchPhotoMetadata(placesClient, placeId) ?: run {
            prefs.edit().remove(placeIdKey(place.id)).apply()
            val refreshedId = resolvePlaceId(placesClient, place) ?: return emptyList()
            fetchPhotoMetadata(placesClient, refreshedId) ?: return emptyList()
        }

        val metadata = remote.photoMetadatas.orEmpty()
        if (alreadyLoaded >= metadata.size) return emptyList()

        return metadata
            .drop(alreadyLoaded)
            .take((targetCount - alreadyLoaded).coerceAtLeast(0))
            .mapNotNull { photoMetadata ->
                runCatching {
                    val response = placesClient.awaitFetchResolvedPhotoUri(photoMetadata) {
                        maxWidth = 1280
                        maxHeight = 960
                    }
                    val authors = photoMetadata.authorAttributions
                        ?.asList()
                        .orEmpty()
                        .map { PhotoAuthor(name = it.name, uri = it.uri) }
                    PlacePhoto(
                        uri = response.uri.toString(),
                        attributionHtml = photoMetadata.attributions,
                        authors = authors,
                        googleMapsUri = photoMetadata.googleMapsUri?.toString()
                    )
                }.getOrNull()
            }
    }

    private suspend fun resolvePlaceId(placesClient: PlacesClient, place: Place): String? {
        val key = placeIdKey(place.id)
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }?.let { return it }

        val response = placesClient.awaitSearchByText(
            textQuery = place.venue,
            placeFields = listOf(GooglePlace.Field.ID)
        ) {
            maxResultCount = 1
            locationBias = CircularBounds.newInstance(
                LatLng(place.latitude, place.longitude),
                350.0
            )
        }

        val id = response.places.firstOrNull()?.id ?: return null
        prefs.edit().putString(key, id).apply()
        return id
    }

    private suspend fun fetchPhotoMetadata(placesClient: PlacesClient, placeId: String): GooglePlace? =
        runCatching {
            placesClient.awaitFetchPlace(
                placeId = placeId,
                placeFields = listOf(GooglePlace.Field.PHOTO_METADATAS)
            ).place
        }.getOrNull()

    private fun placeIdKey(localId: String) = "place_id_$localId"
}
