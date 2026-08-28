package com.hereliesaz.lamplight

import lamplight.shared.generated.resources.Res

object HotelCatalog {
    private const val FILE_PATH = "files/hotels.csv"
    private val EXPECTED_HEADER = listOf("Id", "Name", "Latitude", "Longitude")

    suspend fun load(): List<Hotel> {
        val source = runCatching { Res.readBytes(FILE_PATH).decodeToString() }
            .getOrElse { error ->
                println("HotelCatalog: could not read $FILE_PATH: ${error.message}")
                return emptyList()
            }
        return parseCatalog(source)
    }

    /**
     * Pure CSV -> hotel-list parsing with no platform dependencies, so it can be exercised
     * directly by tests. Mirrors QuarterMuseSeed's degrade-gracefully behavior: a malformed
     * row is skipped, a malformed file yields an empty list.
     */
    fun parseCatalog(source: String): List<Hotel> {
        val rows = runCatching { parseCsv(source.removePrefix("﻿")).toMutableList() }
            .getOrElse { error ->
                println("HotelCatalog: could not parse hotel CSV: ${error.message}")
                return emptyList()
            }

        val header = rows.removeFirstOrNull()
        if (header != EXPECTED_HEADER) {
            println("HotelCatalog: unexpected hotel CSV header: $header")
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
        println("HotelCatalog: skipping malformed hotel row: ${error.message}")
    }.getOrNull()
}
