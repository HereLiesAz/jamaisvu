package com.hereliesaz.lamplight

import kotlinx.browser.window

/** Wraps `localStorage` behind [SettingsStore]. String sets are newline-joined -- place/hotel
 * ids are plain CSV slugs, never containing a newline, so this needs no escaping. */
class BrowserSettingsStore : SettingsStore {
    private val storage get() = window.localStorage

    override fun getStringSet(key: String): Set<String> {
        val raw = getString(key) ?: return emptySet()
        return raw.split("\n").filter { it.isNotEmpty() }.toSet()
    }

    override fun putStringSet(key: String, values: Set<String>) {
        putString(key, values.joinToString("\n"))
    }

    override fun getString(key: String): String? = storage.getItem(key)

    override fun putString(key: String, value: String) {
        storage.setItem(key, value)
    }

    override fun getBoolean(key: String): Boolean = storage.getItem(key) == "true"

    override fun putBoolean(key: String, value: Boolean) {
        storage.setItem(key, value.toString())
    }

    override fun remove(key: String) {
        storage.removeItem(key)
    }
}
