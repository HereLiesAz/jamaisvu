package com.hereliesaz.lamplight

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Where [LamplightViewModel]'s tracked download stands: never started, in flight, or sitting on disk ready for the guest to confirm the install. */
sealed interface GitHubUpdateDownloadState {
    data object NotStarted : GitHubUpdateDownloadState
    data object Downloading : GitHubUpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : GitHubUpdateDownloadState
}

/** Where a fetched GitHub-release update is downloaded to, always the same name so a fresh download replaces any prior one rather than accumulating. */
fun apkDownloadFile(context: Context): File = File(context.getExternalFilesDir(null), "lamplight-update.apk")

/** Enqueues the update APK via [DownloadManager], to a known file this app fully controls -- unlike handing the URL to `ACTION_VIEW`, which delegates the download to whatever app claims it and leaves this app with no idea where the file ends up (and so no way to ever clean it up). Returns the download ID, for matching against `DownloadManager.ACTION_DOWNLOAD_COMPLETE`. */
fun enqueueApkDownload(context: Context, downloadUrl: String): Long {
    val destination = apkDownloadFile(context)
    destination.delete()
    val request = DownloadManager.Request(Uri.parse(downloadUrl))
        .setTitle("Lamplight update")
        .setDestinationUri(Uri.fromFile(destination))
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    return manager.enqueue(request)
}

/** The system package installer needs a content:// URI (a raw file:// one is blocked since Android 7) -- see the matching `<provider>` in AndroidManifest.xml and res/xml/file_paths.xml. */
fun installApkIntent(context: Context, apkFile: File): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    return Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/**
 * Deletes the downloaded update APK once it has actually done its job -- fired by the OS when
 * this app itself finishes being replaced by an install the guest confirmed. Registered in the
 * manifest (not dynamically) so it still fires even though the process that started the install
 * is the one being torn down. If the guest downloads but never completes the install, the file
 * is left alone (a fresh download overwrites it next time regardless).
 */
class UpdateInstalledReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        apkDownloadFile(context).delete()
    }
}
