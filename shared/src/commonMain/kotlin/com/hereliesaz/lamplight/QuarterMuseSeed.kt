package com.hereliesaz.lamplight

import lamplight.shared.generated.resources.Res

object QuarterMuseSeed {
    private const val FILE_PATH = "files/quartermuse_master_v11.csv"
    private val EXPECTED_HEADER = listOf("Id", "Venue", "Latitude", "Longitude", "Category Tags", "Featured")

    suspend fun load(): List<Place> {
        val source = runCatching { Res.readBytes(FILE_PATH).decodeToString() }
            .getOrElse { error ->
                println("QuarterMuseSeed: could not read $FILE_PATH: ${error.message}")
                return emptyList()
            }
        return parseCatalog(source)
    }

    /**
     * Pure CSV -> catalog parsing with no platform dependencies, so it can be exercised
     * directly by tests. A malformed row is skipped rather than crashing the whole catalog; a
     * malformed file (bad header, unterminated quote) yields an empty catalog rather than a crash.
     */
    fun parseCatalog(source: String): List<Place> {
        val rows = runCatching { parseCsv(source.removePrefix("﻿")).toMutableList() }
            .getOrElse { error ->
                println("QuarterMuseSeed: could not parse QuarterMuse CSV: ${error.message}")
                return emptyList()
            }

        val header = rows.removeFirstOrNull()
        if (header != EXPECTED_HEADER) {
            println("QuarterMuseSeed: unexpected QuarterMuse CSV header: $header")
            return emptyList()
        }

        return rows
            .filter { row -> row.any { it.isNotBlank() } }
            .mapIndexedNotNull { index, row -> parseRow(row, index + 2) }
    }

    private fun parseRow(row: List<String>, lineNumber: Int): Place? = runCatching {
        require(row.size == 6) { "row $lineNumber has ${row.size} columns" }
        val id = row[0].trim()
        val venue = row[1].trim()
        val latitude = row[2].trim().toDouble()
        val longitude = row[3].trim().toDouble()
        val tags = row[4].trim().split(';').map(String::trim).filter(String::isNotBlank)
        val featured = row[5].trim().equals("true", ignoreCase = true)
        require(id.isNotBlank() && venue.isNotBlank() && tags.isNotEmpty()) {
            "row $lineNumber is missing an id, venue, or tag"
        }

        Place(id = id, venue = venue, latitude = latitude, longitude = longitude, tags = tags, featured = featured)
    }.onFailure { error ->
        println("QuarterMuseSeed: skipping malformed QuarterMuse row: ${error.message}")
    }.getOrNull()
}
