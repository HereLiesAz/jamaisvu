package com.hereliesaz.lamplight

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * How this install reached the device. Update checks must stay confined to the matching
 * source -- a Play install should only ever hear about Play updates, a sideloaded install
 * should only ever hear about GitHub releases.
 */
enum class InstallSource { GOOGLE_PLAY, OTHER }

fun detectInstallSource(context: Context): InstallSource {
    val installer = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull()
    return if (installer == "com.android.vending") InstallSource.GOOGLE_PLAY else InstallSource.OTHER
}

data class GitHubUpdate(val versionName: String, val downloadUrl: String)

private const val GITHUB_RELEASES_URL = "https://api.github.com/repos/HereLiesAz/lamplight/releases"
private val VERSION_CODE_LINE = Regex("""version_code:\s*(\d+)""")
private val VERSION_NAME_LINE = Regex("""version_name:\s*(\S+)""")

/**
 * Checks GitHub Releases for a build newer than the one installed. Only meaningful for a
 * sideloaded install -- callers must gate this on [detectInstallSource] returning
 * [InstallSource.OTHER], never call it for a Play install.
 *
 * CI (.github/workflows/build-and-release.yml) embeds "version_code: N" / "version_name: X"
 * lines in each release's notes specifically so this has something structured to parse; a
 * GitHub release's tag alone only carries major.minor, not the full build-numbered version.
 */
suspend fun fetchGitHubUpdate(context: Context): GitHubUpdate? = withContext(Dispatchers.IO) {
    runCatching {
        val installedVersionCode = installedVersionCode(context) ?: return@runCatching null

        val connection = URL(GITHUB_RELEASES_URL).openConnection() as HttpURLConnection
        val body = try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            // GitHub's API rejects unauthenticated requests with no User-Agent header.
            connection.setRequestProperty("User-Agent", "Lamplight-Android-App")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }

        val releases = JSONArray(body)
        if (releases.length() == 0) return@runCatching null
        val latest = releases.getJSONObject(0)
        val notes = latest.optString("body")

        val remoteVersionCode = VERSION_CODE_LINE.find(notes)?.groupValues?.get(1)?.toLongOrNull()
            ?: return@runCatching null
        if (remoteVersionCode <= installedVersionCode) return@runCatching null

        val assets = latest.optJSONArray("assets") ?: return@runCatching null
        // By created_at, not array order or first-match: CI's asset filename is meant to stay
        // stable so --clobber replaces it every build, but if a release ever does carry more
        // than one .apk (a transitional period, a manual upload), this must not silently pick
        // a stale one -- ISO-8601 timestamps sort correctly as plain strings.
        val apkUrl = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk") }
            .maxByOrNull { it.optString("created_at") }
            ?.optString("browser_download_url")
            ?: return@runCatching null

        val versionName = VERSION_NAME_LINE.find(notes)?.groupValues?.get(1) ?: latest.optString("tag_name")
        GitHubUpdate(versionName = versionName, downloadUrl = apkUrl)
    }.getOrNull()
}

private fun installedVersionCode(context: Context): Long? = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
}.getOrNull()
