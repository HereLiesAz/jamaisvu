package com.hereliesaz.lamplight

import android.content.Context
import android.util.Log

object QuarterMuseSeed {
    private const val FILE_NAME = "quartermuse_master_v11.csv"
    private const val TAG = "QuarterMuseSeed"
    private val EXPECTED_HEADER = listOf("Id", "Venue", "Latitude", "Longitude", "Category Tags")

    fun load(context: Context): List<Place> {
        val source = runCatching {
            context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            Log.e(TAG, "Could not read $FILE_NAME", error)
            return emptyList()
        }
        return parseCatalog(source)
    }

    /**
     * Pure CSV -> catalog parsing with no Android dependencies, so it can be exercised directly
     * by JVM unit tests. A malformed row is skipped rather than crashing the whole catalog; a
     * malformed file (bad header, unterminated quote) yields an empty catalog rather than a crash.
     */
    fun parseCatalog(source: String): List<Place> {
        val rows = runCatching { parseCsv(source.removePrefix("﻿")).toMutableList() }
            .getOrElse { error ->
                Log.e(TAG, "Could not parse QuarterMuse CSV", error)
                return emptyList()
            }

        val header = rows.removeFirstOrNull()
        if (header != EXPECTED_HEADER) {
            Log.e(TAG, "Unexpected QuarterMuse CSV header: $header")
            return emptyList()
        }

        return rows
            .filter { row -> row.any { it.isNotBlank() } }
            .mapIndexedNotNull { index, row -> parseRow(row, index + 2) }
    }

    private fun parseRow(row: List<String>, lineNumber: Int): Place? = runCatching {
        require(row.size == 5) { "row $lineNumber has ${row.size} columns" }
        val id = row[0].trim()
        val venue = row[1].trim()
        val latitude = row[2].trim().toDouble()
        val longitude = row[3].trim().toDouble()
        val tags = row[4].trim().split(';').map(String::trim).filter(String::isNotBlank)
        require(id.isNotBlank() && venue.isNotBlank() && tags.isNotEmpty()) {
            "row $lineNumber is missing an id, venue, or tag"
        }

        Place(id = id, venue = venue, latitude = latitude, longitude = longitude, tags = tags)
    }.onFailure { error ->
        Log.w(TAG, "Skipping malformed QuarterMuse row: ${error.message}")
    }.getOrNull()
}
