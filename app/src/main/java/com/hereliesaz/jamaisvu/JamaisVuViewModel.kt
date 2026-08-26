package com.hereliesaz.jamaisvu

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JamaisVuViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("jamaisvu", Context.MODE_PRIVATE)
    private val backend = SocialBackend(application)
    private val _gems = mutableStateListOf<Gem>()
    private val _profiles = mutableStateListOf<UserProfile>()
    private val saved = mutableStateMapOf<String, Boolean>()
    private val visited = mutableStateMapOf<String, Boolean>()
    private val following = mutableStateMapOf<String, Boolean>()

    val gems: List<Gem> get() = _gems
    val profiles: List<UserProfile> get() = _profiles
    val syncConfigured: Boolean get() = backend.configured
    val isSignedIn: Boolean get() = session != null
    val currentProfile: UserProfile?
        get() = session?.userId?.let { id -> _profiles.firstOrNull { it.id == id } }

    var session by mutableStateOf<AuthSession?>(null)
        private set
    var authBusy by mutableStateOf(false)
        private set
    var syncBusy by mutableStateOf(false)
        private set
    var publishBusy by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    init {
        _gems += DemoData.gems
        loadCustomGems()
        prefs.getStringSet("saved", emptySet()).orEmpty().forEach { saved[it] = true }
        prefs.getStringSet("visited", emptySet()).orEmpty().forEach { visited[it] = true }
        session = loadSession()
        if (backend.configured) refreshCloud()
    }

    fun isSaved(id: String) = saved[id] == true
    fun isVisited(id: String) = visited[id] == true
    fun isFollowing(id: String) = following[id] == true

    fun clearStatus() {
        statusMessage = null
    }

    fun toggleSaved(id: String) {
        val newValue = !isSaved(id)
        if (newValue) saved[id] = true else saved.remove(id)
        persistTravelState()
        val current = session ?: return
        if (_gems.firstOrNull { it.id == id }?.authorId == null) return
        viewModelScope.launch {
            runCatching { backend.setSaved(current, id, newValue) }
                .onFailure { statusMessage = it.message }
        }
    }

    fun toggleVisited(id: String) {
        val newValue = !isVisited(id)
        if (newValue) visited[id] = true else visited.remove(id)
        persistTravelState()
        val current = session ?: return
        if (_gems.firstOrNull { it.id == id }?.authorId == null) return
        viewModelScope.launch {
            runCatching { backend.setVisited(current, id, newValue) }
                .onFailure { statusMessage = it.message }
        }
    }

    fun toggleFollowing(profileId: String) {
        val current = session ?: run {
            statusMessage = "Sign in to follow people"
            return
        }
        if (profileId == current.userId) return
        val newValue = !isFollowing(profileId)
        if (newValue) following[profileId] = true else following.remove(profileId)
        viewModelScope.launch {
            runCatching {
                backend.setFollowing(current, profileId, newValue)
                refreshCloud()
            }.onFailure {
                if (newValue) following.remove(profileId) else following[profileId] = true
                statusMessage = it.message
            }
        }
    }

    fun signUp(email: String, password: String, handle: String, city: String) {
        if (!backend.configured) {
            statusMessage = "Cloud sync is not configured for this build"
            return
        }
        authBusy = true
        statusMessage = null
        viewModelScope.launch {
            runCatching { backend.signUp(email, password, handle, city) }
                .onSuccess { attempt ->
                    if (attempt.session != null) {
                        acceptSession(attempt.session)
                        refreshCloud()
                    } else if (attempt.confirmationRequired) {
                        statusMessage = "Check your email, then sign in."
                    }
                }
                .onFailure { statusMessage = it.message }
            authBusy = false
        }
    }

    fun signIn(email: String, password: String) {
        if (!backend.configured) {
            statusMessage = "Cloud sync is not configured for this build"
            return
        }
        authBusy = true
        statusMessage = null
        viewModelScope.launch {
            runCatching { backend.signIn(email, password) }
                .onSuccess {
                    acceptSession(it)
                    refreshCloud()
                }
                .onFailure { statusMessage = it.message }
            authBusy = false
        }
    }

    fun signOut() {
        session = null
        prefs.edit().remove("session").apply()
        following.clear()
        _profiles.clear()
        _gems.clear()
        _gems += DemoData.gems
        loadCustomGems()
        if (backend.configured) refreshCloud()
    }

    fun refreshCloud() {
        if (!backend.configured || syncBusy) return
        syncBusy = true
        viewModelScope.launch {
            val current = session
            val result = if (current == null) {
                runCatching { backend.loadPublicSnapshot() }
            } else {
                runCatching { backend.loadSnapshot(current) }.recoverCatching {
                    val fresh = backend.refresh(current.refreshToken, current.email)
                    acceptSession(fresh)
                    backend.loadSnapshot(fresh)
                }
            }
            result.onSuccess { applySnapshot(it) }
                .onFailure { statusMessage = it.message }
            syncBusy = false
        }
    }

    fun addGem(title: String, city: String, neighborhood: String, category: String, tip: String, image: String?) {
        val current = session
        if (backend.configured && current != null) {
            publishBusy = true
            statusMessage = null
            viewModelScope.launch {
                runCatching { backend.addGem(current, title, city, neighborhood, category, tip, image) }
                    .onSuccess { gem ->
                        _gems.removeAll { it.id == gem.id }
                        _gems.add(0, gem)
                        visited[gem.id] = true
                        persistTravelState()
                        runCatching { backend.setVisited(current, gem.id, true) }
                        refreshCloud()
                    }
                    .onFailure { statusMessage = it.message }
                publishBusy = false
            }
            return
        }

        if (backend.configured && current == null) {
            statusMessage = "Sign in before publishing a gem"
            return
        }

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
        persistTravelState()
    }

    private fun applySnapshot(snapshot: SocialSnapshot) {
        _profiles.clear()
        _profiles += snapshot.profiles
        following.clear()
        snapshot.followingIds.forEach { following[it] = true }

        if (snapshot.gems.isNotEmpty()) {
            _gems.clear()
            _gems += snapshot.gems
        }

        if (session != null) {
            saved.clear()
            visited.clear()
            snapshot.savedIds.forEach { saved[it] = true }
            snapshot.visitedIds.forEach { visited[it] = true }
            persistTravelState()
        }
    }

    private fun acceptSession(newSession: AuthSession) {
        session = newSession
        prefs.edit().putString(
            "session",
            JSONObject().apply {
                put("userId", newSession.userId)
                put("email", newSession.email)
                put("accessToken", newSession.accessToken)
                put("refreshToken", newSession.refreshToken)
            }.toString()
        ).apply()
    }

    private fun loadSession(): AuthSession? {
        val raw = prefs.getString("session", null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            AuthSession(
                userId = o.getString("userId"),
                email = o.getString("email"),
                accessToken = o.getString("accessToken"),
                refreshToken = o.getString("refreshToken")
            )
        }.getOrNull()
    }

    private fun persistTravelState() {
        prefs.edit()
            .putStringSet("saved", saved.keys.toSet())
            .putStringSet("visited", visited.keys.toSet())
            .apply()
    }

    private fun persistCustomGems() {
        val array = JSONArray()
        _gems.filter { it.isUserAdded && it.authorId == null }.forEach { gem ->
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
