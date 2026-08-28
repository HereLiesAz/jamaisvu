package com.hereliesaz.lamplight

import android.content.Context

/** Wraps the exact `SharedPreferences` calls `LamplightViewModel` always used, behind [SettingsStore]. */
class AndroidSettingsStore(context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences("lamplight", Context.MODE_PRIVATE)

    override fun getStringSet(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty()

    override fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean = prefs.getBoolean(key, false)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
