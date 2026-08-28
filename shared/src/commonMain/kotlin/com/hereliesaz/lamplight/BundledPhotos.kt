package com.hereliesaz.lamplight

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import lamplight.shared.generated.resources.Res

/**
 * Reads the venue photo manifest produced once per build by scripts/fetch_place_photos.py.
 * The app never calls the Places API itself -- photos are fetched and bundled at build time,
 * the same way the venue catalog CSV is, and simply loaded as local assets here.
 */
object BundledPhotos {
    private const val MANIFEST_PATH = "files/photos_manifest.json"

    suspend fun load(): Map<String, List<PlacePhoto>> {
        val text = runCatching { Res.readBytes(MANIFEST_PATH).decodeToString() }.getOrNull() ?: return emptyMap()
        return parseManifest(text)
    }

    /**
     * The actual parsing, split out from [load] so it's testable with a fixture string --
     * `Res.readBytes` needs a real Compose Resources setup no commonTest fixture can supply.
     */
    internal fun parseManifest(text: String): Map<String, List<PlacePhoto>> {
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return emptyMap()
        val result = mutableMapOf<String, List<PlacePhoto>>()
        val baseUri = photoBaseUri()

        root.keys.forEach { placeId ->
            val entries = (root[placeId] as? JsonArray) ?: return@forEach
            val photos = entries.mapNotNull { entry ->
                val entryObject = entry as? JsonObject ?: return@mapNotNull null
                val file = entryObject.stringOrNull("file") ?: return@mapNotNull null
                val authors = (entryObject["authors"] as? JsonArray)?.mapNotNull { author ->
                    val authorObject = author as? JsonObject ?: return@mapNotNull null
                    val name = authorObject.stringOrNull("name") ?: return@mapNotNull null
                    PhotoAuthor(name = name, uri = authorObject.stringOrNull("uri"))
                }.orEmpty()
                PlacePhoto(
                    uri = "$baseUri$placeId/$file",
                    authors = authors,
                    googleMapsUri = entryObject.stringOrNull("googleMapsUri")
                )
            }
            if (photos.isNotEmpty()) result[placeId] = photos
        }

        return result
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
