package com.hereliesaz.lamplight

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads the place-details manifest produced once per build by scripts/fetch_place_photos.py.
 * The app never calls the Places API itself, and never bundles raw review text -- only the
 * phone/website/address/hours/tags that manifest already distilled from it.
 */
object BundledPlaceDetails {
    private const val MANIFEST_FILE = "place_details_manifest.json"

    fun load(context: Context): Map<String, PlaceDetailsInfo> {
        val text = runCatching {
            context.assets.open(MANIFEST_FILE).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return emptyMap()

        val root = runCatching { JSONObject(text) }.getOrNull() ?: return emptyMap()
        val result = mutableMapOf<String, PlaceDetailsInfo>()

        root.keys().forEach { placeId ->
            val entry = root.optJSONObject(placeId) ?: return@forEach
            result[placeId] = PlaceDetailsInfo(
                phone = entry.optString("phone").takeIf { it.isNotBlank() },
                website = entry.optString("website").takeIf { it.isNotBlank() },
                address = entry.optString("address").takeIf { it.isNotBlank() },
                weekdayDescriptions = entry.optJSONArray("weekdayDescriptions").toStringList(),
                periods = entry.optJSONArray("periods").let { array ->
                    if (array == null) emptyList() else (0 until array.length()).mapNotNull { index ->
                        array.optJSONObject(index)?.let(::parsePeriod)
                    }
                },
                tags = entry.optJSONArray("tags").toStringList()
            )
        }

        return result
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
    }

    private fun parsePeriod(entry: JSONObject): OpeningPeriod? {
        if (!entry.has("openDay") || entry.isNull("openDay")) return null
        val openTime = entry.optString("openTime").takeIf { it.isNotBlank() } ?: return null
        val closeDay = if (entry.isNull("closeDay")) null else entry.optInt("closeDay")
        val closeTime = entry.optString("closeTime").takeIf { it.isNotBlank() }
        return OpeningPeriod(
            openDay = entry.optInt("openDay"),
            openTime = openTime,
            closeDay = closeDay,
            closeTime = closeTime
        )
    }
}
