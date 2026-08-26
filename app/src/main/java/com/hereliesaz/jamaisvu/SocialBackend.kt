package com.hereliesaz.jamaisvu

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.roundToInt

data class AuthAttempt(
    val session: AuthSession?,
    val confirmationRequired: Boolean = false
)

class SocialBackend(private val context: Context) {
    private val baseUrl = BuildConfig.CLOUDFLARE_API_URL.trim().trimEnd('/')
    private val client = OkHttpClient()

    val configured: Boolean
        get() = baseUrl.startsWith("https://") || baseUrl.startsWith("http://")

    suspend fun signUp(email: String, password: String, handle: String, city: String): AuthAttempt = withContext(Dispatchers.IO) {
        requireConfigured()
        val json = executeJson(
            path = "/v1/auth/signup",
            method = "POST",
            body = JSONObject().apply {
                put("email", email.trim())
                require(password.length >= 8) { "Password must be at least 8 characters" }
                put("passwordProof", passwordProof(email, password))
                put("handle", handle.trim().removePrefix("@"))
                put("city", city.trim())
            }
        )
        AuthAttempt(session = parseSession(json))
    }

    suspend fun signIn(email: String, password: String): AuthSession = withContext(Dispatchers.IO) {
        requireConfigured()
        parseSession(
            executeJson(
                path = "/v1/auth/signin",
                method = "POST",
                body = JSONObject().apply {
                    put("email", email.trim())
                    put("passwordProof", passwordProof(email, password))
                }
            )
        )
    }

    suspend fun refresh(refreshToken: String, email: String): AuthSession = withContext(Dispatchers.IO) {
        requireConfigured()
        val session = parseSession(
            executeJson(
                path = "/v1/auth/refresh",
                method = "POST",
                body = JSONObject().put("refresh_token", refreshToken)
            )
        )
        if (session.email.isBlank() && email.isNotBlank()) session.copy(email = email) else session
    }

    suspend fun loadPublicSnapshot(): SocialSnapshot = withContext(Dispatchers.IO) {
        requireConfigured()
        parseSnapshot(executeJson("/v1/snapshot", "GET"), currentUserId = null)
    }

    suspend fun loadSnapshot(session: AuthSession): SocialSnapshot = withContext(Dispatchers.IO) {
        requireConfigured()
        parseSnapshot(
            executeJson("/v1/snapshot", "GET", token = session.accessToken),
            currentUserId = session.userId
        )
    }

    suspend fun setSaved(session: AuthSession, gemId: String, value: Boolean) = withContext(Dispatchers.IO) {
        executeJson("/v1/saved/${Uri.encode(gemId)}", if (value) "PUT" else "DELETE", token = session.accessToken)
    }

    suspend fun setVisited(session: AuthSession, gemId: String, value: Boolean) = withContext(Dispatchers.IO) {
        executeJson("/v1/visited/${Uri.encode(gemId)}", if (value) "PUT" else "DELETE", token = session.accessToken)
    }

    suspend fun setFollowing(session: AuthSession, profileId: String, value: Boolean) = withContext(Dispatchers.IO) {
        executeJson("/v1/follows/${Uri.encode(profileId)}", if (value) "PUT" else "DELETE", token = session.accessToken)
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
            image.isNullOrBlank() -> null
            image.startsWith("content:") -> uploadGemImage(session, image)
            else -> image
        }
        val body = JSONObject().apply {
            put("title", title.trim())
            put("city", city.trim())
            put("neighborhood", neighborhood.trim())
            put("category", category)
            put("tip", tip.trim())
            put("tags", JSONArray().put(category))
            if (imageUrl != null) put("imageUrl", imageUrl)
        }
        parseGem(
            executeJson("/v1/gems", "POST", token = session.accessToken, body = body),
            currentUserId = session.userId
        )
    }

    private fun uploadGemImage(session: AuthSession, uriString: String): String {
        val uri = Uri.parse(uriString)
        val bytes = compressForUpload(uri)
        val request = Request.Builder()
            .url("$baseUrl/v1/images")
            .header("Authorization", "Bearer ${session.accessToken}")
            .post(bytes.toRequestBody("image/jpeg".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(response.code, text)
            return JSONObject(text).getString("url")
        }
    }

    private fun compressForUpload(uri: Uri): ByteArray {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxDimension = maxOf(info.size.width, info.size.height)
            if (maxDimension > 1280) {
                val scale = 1280f / maxDimension.toFloat()
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1)
                )
            }
        }

        val qualities = intArrayOf(82, 74, 66, 58, 50, 42)
        for (quality in qualities) {
            val bytes = bitmap.toJpeg(quality)
            if (bytes.size <= 700_000) {
                bitmap.recycle()
                return bytes
            }
        }

        val smaller = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 0.75f).roundToInt().coerceAtLeast(1),
            (bitmap.height * 0.75f).roundToInt().coerceAtLeast(1),
            true
        )
        try {
            for (quality in intArrayOf(58, 50, 42, 35)) {
                val bytes = smaller.toJpeg(quality)
                if (bytes.size <= 700_000) return bytes
            }
        } finally {
            if (smaller !== bitmap) smaller.recycle()
            bitmap.recycle()
        }
        throw IOException("Could not compress image below the free-storage upload limit")
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        if (!compress(Bitmap.CompressFormat.JPEG, quality, output)) {
            throw IOException("Could not encode selected image")
        }
        return output.toByteArray()
    }

    private fun passwordProof(email: String, password: String): String {
        val normalizedEmail = email.trim().lowercase()
        val salt = "jamaisvu-v1|$normalizedEmail".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(password.toCharArray(), salt, 210_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
                .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        } finally {
            spec.clearPassword()
        }
    }

    private fun executeJson(
        path: String,
        method: String,
        token: String? = null,
        body: JSONObject? = null
    ): JSONObject {
        requireConfigured()
        val builder = Request.Builder()
            .url("$baseUrl$path")
            .header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")

        val jsonBody = body?.toString()?.toRequestBody("application/json; charset=utf-8".toMediaType())
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(jsonBody ?: "{}".toRequestBody("application/json".toMediaType()))
            "PUT" -> builder.put(jsonBody ?: "{}".toRequestBody("application/json".toMediaType()))
            "DELETE" -> builder.delete()
            else -> error("Unsupported method: $method")
        }

        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw apiError(response.code, text)
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        }
    }

    private fun parseSnapshot(json: JSONObject, currentUserId: String?): SocialSnapshot {
        val profilesJson = json.optJSONArray("profiles") ?: JSONArray()
        val gemsJson = json.optJSONArray("gems") ?: JSONArray()

        val profiles = buildList {
            for (i in 0 until profilesJson.length()) {
                val row = profilesJson.getJSONObject(i)
                add(
                    UserProfile(
                        id = row.getString("id"),
                        handle = row.optString("handle"),
                        city = row.optString("city"),
                        bio = row.optString("bio"),
                        avatarUrl = row.optNullableString("avatarUrl"),
                        gemCount = row.optInt("gemCount"),
                        followerCount = row.optInt("followerCount"),
                        followingCount = row.optInt("followingCount")
                    )
                )
            }
        }

        val gems = buildList {
            for (i in 0 until gemsJson.length()) {
                add(parseGem(gemsJson.getJSONObject(i), currentUserId))
            }
        }

        return SocialSnapshot(
            gems = gems,
            profiles = profiles,
            savedIds = json.optStringSet("savedIds"),
            visitedIds = json.optStringSet("visitedIds"),
            followingIds = json.optStringSet("followingIds")
        )
    }

    private fun parseSession(json: JSONObject): AuthSession {
        val userId = json.optString("userId")
        val accessToken = json.optString("accessToken")
        val refreshToken = json.optString("refreshToken")
        if (userId.isBlank() || accessToken.isBlank() || refreshToken.isBlank()) {
            throw IOException("Backend returned an invalid session")
        }
        return AuthSession(
            userId = userId,
            email = json.optString("email"),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun parseGem(json: JSONObject, currentUserId: String?): Gem {
        val authorId = json.optNullableString("authorId")
        val tagsArray = json.optJSONArray("tags") ?: JSONArray()
        val tags = buildList {
            for (i in 0 until tagsArray.length()) add(tagsArray.optString(i))
        }.filter { it.isNotBlank() }
        return Gem(
            id = json.getString("id"),
            title = json.getString("title"),
            city = json.getString("city"),
            neighborhood = json.optString("neighborhood"),
            category = json.optString("category"),
            tip = json.optString("tip"),
            username = json.optString("username", "@local"),
            image = json.optString("image"),
            isUserAdded = authorId != null && authorId == currentUserId,
            authorId = authorId,
            createdAt = json.optNullableString("createdAt"),
            tags = tags,
            latitude = json.optNullableDouble("latitude"),
            longitude = json.optNullableDouble("longitude")
        )
    }

    private fun JSONObject.optStringSet(key: String): Set<String> {
        val array = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (i in 0 until array.length()) {
                array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { !it.isNaN() && !it.isInfinite() }

    private fun apiError(code: Int, raw: String): IOException {
        val message = runCatching {
            JSONObject(raw).optString("message")
        }.getOrNull().orEmpty().ifBlank { "Backend request failed ($code)" }
        return IOException(message)
    }

    private fun requireConfigured() {
        if (!configured) throw IOException("Cloud sync is not configured for this build")
    }
}
