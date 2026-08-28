package com.hereliesaz.jamaisvu

import android.content.Context
import org.json.JSONObject

/**
 * Reads the venue photo manifest produced once per build by scripts/fetch_place_photos.py.
 * The app never calls the Places API itself -- photos are fetched and bundled at build time,
 * the same way the venue catalog CSV is, and simply loaded as local assets here.
 */
object BundledPhotos {
    private const val MANIFEST_FILE = "photos_manifest.json"

    fun load(context: Context): Map<String, List<PlacePhoto>> {
        val text = runCatching {
            context.assets.open(MANIFEST_FILE).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return emptyMap()

        val root = runCatching { JSONObject(text) }.getOrNull() ?: return emptyMap()
        val result = mutableMapOf<String, List<PlacePhoto>>()

        root.keys().forEach { placeId ->
            val entries = root.optJSONArray(placeId) ?: return@forEach
            val photos = (0 until entries.length()).mapNotNull { index ->
                val entry = entries.optJSONObject(index) ?: return@mapNotNull null
                val file = entry.optString("file").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val authors = entry.optJSONArray("authors")?.let { authorsArray ->
                    (0 until authorsArray.length()).mapNotNull { authorIndex ->
                        val author = authorsArray.optJSONObject(authorIndex) ?: return@mapNotNull null
                        val name = author.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        PhotoAuthor(name = name, uri = author.optString("uri").takeIf { it.isNotBlank() })
                    }
                }.orEmpty()
                PlacePhoto(
                    uri = "file:///android_asset/photos/$placeId/$file",
                    authors = authors,
                    googleMapsUri = entry.optString("googleMapsUri").takeIf { it.isNotBlank() }
                )
            }
            if (photos.isNotEmpty()) result[placeId] = photos
        }

        return result
    }
}
