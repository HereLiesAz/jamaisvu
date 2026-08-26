package com.hereliesaz.jamaisvu

import android.content.Context
import java.security.MessageDigest

object QuarterMuseSeed {
    private const val FILE_NAME = "quartermuse_master_v11.csv"
    private const val SEED_AUTHOR_ID = "seed-quartermuse"

    fun load(context: Context): List<Gem> = runCatching {
        val source = context.assets.open(FILE_NAME).bufferedReader().use { it.readText() }.removePrefix("\uFEFF")
        val rows = parseCsv(source).toMutableList()
        val header = rows.removeFirstOrNull()
        require(header == listOf("Venue", "Latitude", "Longitude", "Category Tags")) {
            "Unexpected QuarterMuse CSV header"
        }

        rows.filter { row -> row.any { it.isNotBlank() } }.mapIndexed { index, row ->
            require(row.size == 4) { "QuarterMuse row ${index + 2} has ${row.size} columns" }
            val venue = row[0].trim()
            val latitudeText = row[1].trim()
            val longitudeText = row[2].trim()
            val rawTags = row[3].trim()
            val latitude = latitudeText.toDouble()
            val longitude = longitudeText.toDouble()
            val tags = rawTags.split(';').map(String::trim).filter(String::isNotBlank)
            require(venue.isNotBlank() && tags.isNotEmpty()) { "Invalid QuarterMuse row ${index + 2}" }

            Gem(
                id = "qm-${(index + 1).toString().padStart(3, '0')}-${sha1("$venue|$latitudeText|$longitudeText").take(10)}",
                title = venue,
                city = "New Orleans",
                neighborhood = "",
                category = deriveCategory(tags),
                tip = rawTags,
                username = "@quartermuse",
                image = "",
                isUserAdded = false,
                authorId = SEED_AUTHOR_ID,
                createdAt = null,
                tags = tags,
                latitude = latitude,
                longitude = longitude
            )
        }
    }.getOrElse { emptyList() }

    private fun deriveCategory(tags: List<String>): String {
        val tagSet = tags.toSet()
        return when {
            tagSet.hasAny("Craft Cocktails", "Dive Bar", "Historic Bar", "Dive Bar Adjacent", "Cocktail History") -> "Drink"
            tagSet.hasAny("Seafood", "Historic Restaurant", "Fine Dining", "Casual Dining", "Cheap Eats", "Late Night Food", "Craft Coffee") -> "Eat"
            "Shopping" in tagSet -> "Shop"
            tagSet.hasAny("Occult", "Haunted", "Goth", "Gothic", "Dark Tourism", "Witchcraft", "Weird", "Voodoo Culture") -> "Weird"
            tagSet.hasAny("Live Music", "Jazz Essential", "Punk", "Late Night", "Open After 2AM", "Alternative", "Underground", "LGBTQ+") -> "Play"
            else -> "Art"
        }
    }

    private fun Set<String>.hasAny(vararg values: String): Boolean = values.any(::contains)

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun parseCsv(source: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0

        while (index < source.length) {
            val char = source[index]
            if (quoted) {
                when {
                    char == '"' && index + 1 < source.length && source[index + 1] == '"' -> {
                        field.append('"')
                        index += 1
                    }
                    char == '"' -> quoted = false
                    else -> field.append(char)
                }
            } else {
                when (char) {
                    '"' -> quoted = true
                    ',' -> {
                        row.add(field.toString())
                        field.clear()
                    }
                    '\n' -> {
                        row.add(field.toString().removeSuffix("\r"))
                        field.clear()
                        rows.add(row)
                        row = mutableListOf()
                    }
                    else -> field.append(char)
                }
            }
            index += 1
        }

        require(!quoted) { "Unterminated quoted field in QuarterMuse CSV" }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString().removeSuffix("\r"))
            rows.add(row)
        }
        return rows
    }
}
