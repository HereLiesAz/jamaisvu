package com.hereliesaz.lamplight

import android.app.Application
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The sideload/GitHub-releases self-update surface -- checking, downloading, and tracking
 * readiness to install a newer APK from GitHub Releases. Entirely Android-only, with no web
 * counterpart, ever: a website has no "installer package," the concept doesn't exist, not just
 * lacks an implementation. Extracted out of [LamplightViewModel] so the rest of that class can
 * move to `commonMain` without this coming along for the ride.
 */
class GitHubUpdateController(private val application: Application, scope: CoroutineScope) {
    private val githubUpdateState = mutableStateOf<GitHubUpdate?>(null)
    private val githubUpdateDownloadState = mutableStateOf<GitHubUpdateDownloadState>(GitHubUpdateDownloadState.NotStarted)
    private var downloadCompleteReceiver: BroadcastReceiver? = null
    private var pendingDownloadId: Long? = null

    val installSource: InstallSource = detectInstallSource(application)
    val githubUpdate: GitHubUpdate? get() = githubUpdateState.value

    /** Where the guest's tap on "Download" currently stands -- survives navigating into a place detail and back, unlike plain Composable-local state, since this download outlives any one screen. */
    val githubUpdateDownload: GitHubUpdateDownloadState get() = githubUpdateDownloadState.value

    init {
        // Only a sideloaded install should ever be told about a GitHub release; a Play
        // install's update path is handled entirely separately, via Play Core, in the UI layer.
        if (installSource == InstallSource.OTHER) {
            scope.launch {
                githubUpdateState.value = fetchGitHubUpdate(application)
            }
        }
    }

    /** Starts (or, if already in flight, no-ops on) downloading a GitHub-release update, then watches for its completion. */
    fun startGitHubUpdateDownload(update: GitHubUpdate) {
        if (githubUpdateDownloadState.value is GitHubUpdateDownloadState.Downloading) return
        val downloadId = enqueueApkDownload(application, update.downloadUrl)
        pendingDownloadId = downloadId
        githubUpdateDownloadState.value = GitHubUpdateDownloadState.Downloading

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId != pendingDownloadId) return
                pendingDownloadId = null
                downloadCompleteReceiver?.let { runCatching { application.unregisterReceiver(it) } }
                downloadCompleteReceiver = null
                githubUpdateDownloadState.value = GitHubUpdateDownloadState.ReadyToInstall(apkDownloadFile(application))
            }
        }
        downloadCompleteReceiver = receiver
        ContextCompat.registerReceiver(
            application,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    /** Unregisters the download-complete receiver, if one is still registered. Call from the owning ViewModel's onCleared(). */
    fun dispose() {
        downloadCompleteReceiver?.let { runCatching { application.unregisterReceiver(it) } }
        downloadCompleteReceiver = null
    }
}
