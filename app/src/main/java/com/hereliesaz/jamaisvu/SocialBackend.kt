package com.hereliesaz.jamaisvu

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

data class AuthAttempt(
    val session: AuthSession?,
    val confirmationRequired: Boolean = false
)

class SocialBackend(private val context: Context) {
    private val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
    private val client = OkHttpClient()

    val configured: Boolean
        get() = baseUrl.startsWith("https://") && anonKey.isNotBlank()

    suspend fun signUp(email: String, password: String, handle: String, city: String): AuthAttempt = withContext(Dispatchers.IO) {
        requireConfigured()
        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("data", JSONObject().apply {
                put("handle", handle.trim().removePrefix("@"))
                put("city", city.trim())
            })
        }
        val json = executeJson("/auth/v1/signup", "POST", body = body)
        val session = parseSession(json, email.trim())
        AuthAttempt(session = session, confirmationRequired = session == null && json.optJSONObject("user") != null)
    }

    suspend fun signIn(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        requireConfigured()
        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
        val json = executeJson("/auth/v1/token?grant_type=password", "POST", body = body)
        parseSession(json, email.trim()) ?: throw IOException("Sign-in succeeded without a session")
    }

    suspend fun refresh(refreshToken: String, email: String): AuthSession = withContext(Dispatchers.IO) {
        requireConfigured()
        val body = JSONObject().put("refresh_token", refreshToken)
        val json = executeJson("/auth/v1/token?grant_type=refresh_token", "POST", body = body)
        parseSession(json, email) ?: throw IOException("Session refresh failed")
    }

    suspend fun loadPublicSnapshot(): SocialSnapshot = withContext(Dispatchers.IO) {
        requireConfigured()
        loadSnapshotInternal(token = null, userId = null)
    }

    suspend fun loadSnapshot(session: AuthSession): SocialSnapshot = withContext(Dispatchers.IO) {
        requireConfigured()
        loadSnapshotInternal(token = session.accessToken, userId = session.userId)
    }

    suspend fun setSaved(session: AuthSession, gemId: String, value: Boolean) = withContext(Dispatchers.IO) {
        setJoinRow("saved_gems", "user_id", session.userId, "gem_id", gemId, value, session.accessToken)
    }

    suspend fun setVisited(session: AuthSession, gemId: String, value: Boolean) = withContext(Dispatchers.IO) {
        setJoinRow("visited_gems", "user_id", session.userId, "gem_id", gemId, value, session.accessToken)
    }

    suspend fun setFollowing(session: AuthSession, profileId: String, value: Boolean) = withContext(Dispatchers.IO) {
        setJoinRow("follows", "follower_id", session.userId, "following_id", profileId, value, session.accessToken)
    }

    suspend fun addGem(
        session: AuthSession,
        title: String,
        city: String,
        neighborhood: String,
        category: String,
        tip: String,
        image: String?
    ): Gem = withContext(Dispatchers.IO) {
        requireConfigured()
        val imageUrl = when {
            image.isNullOrBlank() -> "https://picsum.photos/seed/${UUID.randomUUID()}/1000/1000"
            image.startsWith("content:") -> uploadGemImage(session, image)
            else -> image
        }
        val body = JSONObject().apply {
            put("author_id", session.userId)
            put("title", title.trim())
            put("city", city.trim())
            put("neighborhood", neighborhood.trim())
            put("category", category)
            put("tip", tip.trim())
            put("image_url", imageUrl)
        }
        val response = executeText(
            path = "/rest/v1/gems",
            method = "POST",
            token = session.accessToken,
            body = body.toString(),
            headers = mapOf("Prefer" to "return=representation")
        )
        val row = JSONArray(response).getJSONObject(0)
        val profileHandle = loadProfileHandle(session.userId, session.accessToken)
        row.toGem(profileHandle, isUserAdded = true)
    }

    private fun loadSnapshotInternal(token: String?, userId: String?): SocialSnapshot {
        val profilesJson = JSONArray(executeText("/rest/v1/profiles?select=id,handle,city,bio,avatar_url", "GET", token))
        val gemsJson = JSONArray(executeText("/rest/v1/gems?select=id,title,city,neighborhood,category,tip,image_url,author_id,created_at&order=created_at.desc&limit=200", "GET", token))
        val followsJson = JSONArray(executeText("/rest/v1/follows?select=follower_id,following_id", "GET", token))

        val gemCounts = mutableMapOf<String, Int>()
        for (i in 0 until gemsJson.length()) {
            val author = gemsJson.getJSONObject(i).optString("author_id")
            if (author.isNotBlank()) gemCounts[author] = (gemCounts[author] ?: 0) + 1
        }

        val followerCounts = mutableMapOf<String, Int>()
        val followingCounts = mutableMapOf<String, Int>()
        val currentFollowing = mutableSetOf<String>()
        for (i in 0 until followsJson.length()) {
            val row = followsJson.getJSONObject(i)
            val follower = row.getString("follower_id")
            val following = row.getString("following_id")
            followerCounts[following] = (followerCounts[following] ?: 0) + 1
            followingCounts[follower] = (followingCounts[follower] ?: 0) + 1
            if (follower == userId) currentFollowing += following
        }

        val profiles = buildList {
            for (i in 0 until profilesJson.length()) {
                val row = profilesJson.getJSONObject(i)
                val id = row.getString("id")
                add(
                    UserProfile(
                        id = id,
                        handle = row.optString("handle").ifBlank { "user_${id.take(8)}" },
                        city = row.optString("city"),
                        bio = row.optString("bio"),
                        avatarUrl = row.optNullableString("avatar_url"),
                        gemCount = gemCounts[id] ?: 0,
                        followerCount = followerCounts[id] ?: 0,
                        followingCount = followingCounts[id] ?: 0
                    )
                )
            }
        }
        val handles = profiles.associate { it.id to it.handle }
        val gems = buildList {
            for (i in 0 until gemsJson.length()) {
                val row = gemsJson.getJSONObject(i)
                val authorId = row.optString("author_id")
                add(row.toGem(handles[authorId] ?: "local", isUserAdded = authorId == userId))
            }
        }

        val saved = if (userId != null && token != null) loadIds("saved_gems", "user_id", userId, "gem_id", token) else emptySet()
        val visited = if (userId != null && token != null) loadIds("visited_gems", "user_id", userId, "gem_id", token) else emptySet()

        return SocialSnapshot(
            gems = gems,
            profiles = profiles,
            savedIds = saved,
            visitedIds = visited,
            followingIds = currentFollowing
        )
    }

    private fun loadIds(table: String, ownerColumn: String, ownerId: String, valueColumn: String, token: String): Set<String> {
        val rows = JSONArray(executeText("/rest/v1/$table?select=$valueColumn&$ownerColumn=eq.$ownerId", "GET", token))
        return buildSet {
            for (i in 0 until rows.length()) add(rows.getJSONObject(i).getString(valueColumn))
        }
    }

    private fun loadProfileHandle(userId: String, token: String): String {
        val rows = JSONArray(executeText("/rest/v1/profiles?select=handle&id=eq.$userId&limit=1", "GET", token))
        return if (rows.length() > 0) rows.getJSONObject(0).optString("handle", "you") else "you"
    }

    private fun setJoinRow(
        table: String,
        leftColumn: String,
        leftId: String,
        rightColumn: String,
        rightId: String,
        value: Boolean,
        token: String
    ) {
        requireConfigured()
        if (value) {
            val body = JSONObject().apply {
                put(leftColumn, leftId)
                put(rightColumn, rightId)
            }
            executeText(
                path = "/rest/v1/$table",
                method = "POST",
                token = token,
                body = body.toString(),
                headers = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal")
            )
        } else {
            executeText(
                path = "/rest/v1/$table?$leftColumn=eq.$leftId&$rightColumn=eq.$rightId",
                method = "DELETE",
                token = token
            )
        }
    }

    private fun uploadGemImage(session: AuthSession, uriString: String): String {
        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Could not read selected image")
        val contentType = resolver.getType(uri) ?: "image/jpeg"
        val path = "${session.userId}/${UUID.randomUUID()}"
        val request = Request.Builder()
            .url("$baseUrl/storage/v1/object/gems/$path")
            .header("apikey", anonKey)
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("x-upsert", "false")
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw apiError(response.code, response.body?.string().orEmpty())
        }
        return "$baseUrl/storage/v1/object/public/gems/$path"
    }

    private fun executeJson(path: String, method: String, token: String? = null, body: JSONObject? = null): JSONObject {
        val raw = executeText(path, method, token, body?.toString())
        return if (raw.isBlank()) JSONObject() else JSONObject(raw)
    }

    private fun executeText(
        path: String,
        method: String,
        token: String? = null,
        body: String? = null,
        headers: Map<String, String> = emptyMap()
    ): String {
        requireConfigured()
        val builder = Request.Builder()
            .url("$baseUrl$path")
            .header("apikey", anonKey)
            .header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        else builder.header("Authorization", "Bearer $anonKey")
        headers.forEach { (key, value) -> builder.header(key, value) }

        val jsonBody = body?.toRequestBody("application/json; charset=utf-8".toMediaType())
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(jsonBody ?: "{}".toRequestBody("application/json".toMediaType()))
            "PATCH" -> builder.patch(jsonBody ?: "{}".toRequestBody("application/json".toMediaType()))
            "DELETE" -> builder.delete(jsonBody)
            else -> error("Unsupported method: $method")
        }

        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(response.code, text)
            return text
        }
    }

    private fun parseSession(json: JSONObject, fallbackEmail: String): AuthSession? {
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token")
        if (accessToken.isBlank() || refreshToken.isBlank()) return null
        val user = json.optJSONObject("user") ?: return null
        return AuthSession(
            userId = user.getString("id"),
            email = user.optString("email", fallbackEmail),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun JSONObject.toGem(handle: String, isUserAdded: Boolean): Gem = Gem(
        id = getString("id"),
        title = getString("title"),
        city = getString("city"),
        neighborhood = optString("neighborhood"),
        category = getString("category"),
        tip = getString("tip"),
        username = "@${handle.removePrefix("@")}",
        image = optString("image_url").ifBlank { "https://picsum.photos/seed/${getString("id")}/1000/1000" },
        isUserAdded = isUserAdded,
        authorId = optNullableString("author_id"),
        createdAt = optNullableString("created_at")
    )

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun apiError(code: Int, raw: String): IOException {
        val message = runCatching {
            val json = JSONObject(raw)
            json.optString("msg").ifBlank {
                json.optString("message").ifBlank {
                    json.optString("error_description").ifBlank { json.optString("error") }
                }
            }
        }.getOrNull().orEmpty().ifBlank { "Backend request failed ($code)" }
        return IOException(message)
    }

    private fun requireConfigured() {
        if (!configured) throw IOException("Cloud sync is not configured for this build")
    }
}
