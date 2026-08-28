package com.hereliesaz.lamplight

/**
 * A small multiplatform key-value store standing in for `SharedPreferences` (Android) and
 * `localStorage` (web). Deliberately generic rather than domain-shaped (e.g. no
 * `saveHotelAnchor(...)`) -- it exists only to let `LamplightViewModel`'s existing read/write
 * calls swap their backing store per platform, not to redesign what gets persisted.
 */
interface SettingsStore {
    fun getStringSet(key: String): Set<String>
    fun putStringSet(key: String, values: Set<String>)
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}
