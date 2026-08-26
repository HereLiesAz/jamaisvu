package com.hereliesaz.jamaisvu

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JamaisVuViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("jamaisvu", Context.MODE_PRIVATE)
    private val _gems = mutableStateListOf<Gem>()
    private val saved = mutableStateMapOf<String, Boolean>()
    private val visited = mutableStateMapOf<String, Boolean>()

    val gems: List<Gem> get() = _gems

    init {
        _gems += DemoData.gems
        loadCustomGems()
        prefs.getStringSet("saved", emptySet()).orEmpty().forEach { saved[it] = true }
        prefs.getStringSet("visited", emptySet()).orEmpty().forEach { visited[it] = true }
    }

    fun isSaved(id: String) = saved[id] == true
    fun isVisited(id: String) = visited[id] == true

    fun toggleSaved(id: String) {
        if (isSaved(id)) saved.remove(id) else saved[id] = true
        prefs.edit().putStringSet("saved", saved.keys.toSet()).apply()
    }

    fun toggleVisited(id: String) {
        if (isVisited(id)) visited.remove(id) else visited[id] = true
        prefs.edit().putStringSet("visited", visited.keys.toSet()).apply()
    }

    fun addGem(title: String, city: String, neighborhood: String, category: String, tip: String, image: String?) {
        val gem = Gem(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            city = city.trim(),
            neighborhood = neighborhood.trim(),
            category = category,
            tip = tip.trim(),
            username = "@you",
            image = image ?: "https://picsum.photos/seed/${UUID.randomUUID()}/1000/1000",
            isUserAdded = true
        )
        _gems.add(0, gem)
        visited[gem.id] = true
        persistCustomGems()
        prefs.edit().putStringSet("visited", visited.keys.toSet()).apply()
    }

    private fun persistCustomGems() {
        val array = JSONArray()
        _gems.filter { it.isUserAdded }.forEach { gem ->
            array.put(JSONObject().apply {
                put("id", gem.id)
                put("title", gem.title)
                put("city", gem.city)
                put("neighborhood", gem.neighborhood)
                put("category", gem.category)
                put("tip", gem.tip)
                put("username", gem.username)
                put("image", gem.image)
            })
        }
        prefs.edit().putString("custom_gems", array.toString()).apply()
    }

    private fun loadCustomGems() {
        val raw = prefs.getString("custom_gems", null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                _gems.add(
                    0,
                    Gem(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        city = o.getString("city"),
                        neighborhood = o.optString("neighborhood"),
                        category = o.getString("category"),
                        tip = o.optString("tip"),
                        username = o.optString("username", "@you"),
                        image = o.getString("image"),
                        isUserAdded = true
                    )
                )
            }
        }
    }
}
