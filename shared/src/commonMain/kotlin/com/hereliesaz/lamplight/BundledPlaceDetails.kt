package com.hereliesaz.lamplight

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import lamplight.shared.generated.resources.Res

/**
 * Reads the place-details manifest produced once per build by scripts/fetch_place_photos.py.
 * The app never calls the Places API itself, and never bundles raw review text -- only the
 * phone/website/address/hours/tags that manifest already distilled from it.
 */
object BundledPlaceDetails {
    private const val MANIFEST_PATH = "files/place_details_manifest.json"

    suspend fun load(): Map<String, PlaceDetailsInfo> {
        val text = runCatching { Res.readBytes(MANIFEST_PATH).decodeToString() }.getOrNull() ?: return emptyMap()
        return parseManifest(text)
    }

    /**
     * The actual parsing, split out from [load] so it's testable with a fixture string --
     * `Res.readBytes` needs a real Compose Resources setup no commonTest fixture can supply.
     */
    internal fun parseManifest(text: String): Map<String, PlaceDetailsInfo> {
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return emptyMap()
        val result = mutableMapOf<String, PlaceDetailsInfo>()

        root.keys.forEach { placeId ->
            val entry = (root[placeId] as? JsonObject) ?: return@forEach
            result[placeId] = PlaceDetailsInfo(
                phone = entry.stringOrNull("phone"),
                website = entry.stringOrNull("website"),
                address = entry.stringOrNull("address"),
                weekdayDescriptions = entry.stringListOrEmpty("weekdayDescriptions"),
                periods = (entry["periods"] as? JsonArray)?.mapNotNull { period ->
                    (period as? JsonObject)?.let(::parsePeriod)
                }.orEmpty(),
                tags = entry.stringListOrEmpty("tags")
            )
        }

        return result
    }

    private fun parsePeriod(entry: JsonObject): OpeningPeriod? {
        val openDay = entry.intOrNull("openDay") ?: return null
        val openTime = entry.stringOrNull("openTime") ?: return null
        return OpeningPeriod(
            openDay = openDay,
            openTime = openTime,
            closeDay = entry.intOrNull("closeDay"),
            closeTime = entry.stringOrNull("closeTime")
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.intOrNull(key: String): Int? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()

    private fun JsonObject.stringListOrEmpty(key: String): List<String> =
        (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }.orEmpty()
}
