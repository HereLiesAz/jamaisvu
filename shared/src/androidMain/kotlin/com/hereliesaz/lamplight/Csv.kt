package com.hereliesaz.lamplight

/**
 * Minimal CSV parser shared by every bundled catalog (venues, hotels): quoted fields, embedded
 * commas, and escaped quotes ("") are supported. Throws on an unterminated quoted field --
 * callers decide how to degrade (skip the row, or treat the whole file as unparseable).
 */
internal fun parseCsv(source: String): List<List<String>> {
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

    require(!quoted) { "Unterminated quoted field in CSV" }
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString().removeSuffix("\r"))
        rows.add(row)
    }
    return rows
}
