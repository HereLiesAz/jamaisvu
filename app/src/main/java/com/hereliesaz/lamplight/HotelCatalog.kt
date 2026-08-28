package com.hereliesaz.lamplight

import android.content.Context
import android.util.Log

object HotelCatalog {
    private const val FILE_NAME = "hotels.csv"
    private const val TAG = "HotelCatalog"
    private val EXPECTED_HEADER = listOf("Id", "Name", "Latitude", "Longitude")

    fun load(context: Context): List<Hotel> {
        val source = runCatching {
            context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            Log.e(TAG, "Could not read $FILE_NAME", error)
            return emptyList()
        }
        return parseCatalog(source)
    }

    /**
     * Pure CSV -> hotel-list parsing with no Android dependencies, so it can be exercised
     * directly by JVM unit tests. Mirrors QuarterMuseSeed's degrade-gracefully behavior: a
     * malformed row is skipped, a malformed file yields an empty list.
     */
    fun parseCatalog(source: String): List<Hotel> {
        val rows = runCatching { parseCsv(source.removePrefix("﻿")).toMutableList() }
            .getOrElse { error ->
                Log.e(TAG, "Could not parse hotel CSV", error)
                return emptyList()
            }

        val header = rows.removeFirstOrNull()
        if (header != EXPECTED_HEADER) {
            Log.e(TAG, "Unexpected hotel CSV header: $header")
            return emptyList()
        }

        return rows
            .filter { row -> row.any { it.isNotBlank() } }
            .mapIndexedNotNull { index, row -> parseRow(row, index + 2) }
    }

    private fun parseRow(row: List<String>, lineNumber: Int): Hotel? = runCatching {
        require(row.size == 4) { "row $lineNumber has ${row.size} columns" }
        val id = row[0].trim()
        val name = row[1].trim()
        val latitude = row[2].trim().toDouble()
        val longitude = row[3].trim().toDouble()
        require(id.isNotBlank() && name.isNotBlank()) { "row $lineNumber is missing an id or name" }

        Hotel(id = id, name = name, latitude = latitude, longitude = longitude)
    }.onFailure { error ->
        Log.w(TAG, "Skipping malformed hotel row: ${error.message}")
    }.getOrNull()
}
