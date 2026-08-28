package com.hereliesaz.lamplight.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.hereliesaz.lamplight.GitHubUpdateController
import com.hereliesaz.lamplight.GitHubUpdateDownloadState
import com.hereliesaz.lamplight.InstallSource
import com.hereliesaz.lamplight.installApkIntent

/**
 * The actual `platformBanner` content `:androidApp` feeds into `LamplightHome` -- Play Core
 * in-app updates for a Play install, the GitHub-releases sideload flow for anything else. Each
 * install source gets only its own update path: Play Core is never touched for a sideloaded
 * install, and the GitHub check never runs for a Play install (see [GitHubUpdateController]).
 * No web counterpart, ever: a website has no "installer package," the concept doesn't exist.
 */
@Composable
fun AndroidUpdateBanner(updateController: GitHubUpdateController) {
    val playUpdateStatus = if (updateController.installSource == InstallSource.GOOGLE_PLAY) {
        rememberPlayUpdateStatus()
    } else {
        PlayUpdateStatus.None
    }
    UpdateBanner(updateController, playUpdateStatus)
}

private sealed interface PlayUpdateStatus {
    data object None : PlayUpdateStatus
    data class ReadyToInstall(val manager: AppUpdateManager) : PlayUpdateStatus
}

/** Starts a flexible Play in-app update in the background and reports when it's ready to install. */
@Composable
private fun rememberPlayUpdateStatus(): PlayUpdateStatus {
    val activity = LocalActivity.current
    var status by remember { mutableStateOf<PlayUpdateStatus>(PlayUpdateStatus.None) }

    if (activity != null) {
        val appUpdateManager = remember(activity) { AppUpdateManagerFactory.create(activity) }
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

        DisposableEffect(appUpdateManager) {
            val listener = InstallStateUpdatedListener { state ->
                if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    status = PlayUpdateStatus.ReadyToInstall(appUpdateManager)
                }
            }
            appUpdateManager.registerListener(listener)
            onDispose { appUpdateManager.unregisterListener(listener) }
        }

        LaunchedEffect(appUpdateManager) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                        )
                    }
                }
            }
        }
    }

    return status
}

@Composable
private fun UpdateBanner(updateController: GitHubUpdateController, playUpdateStatus: PlayUpdateStatus) {
    val context = LocalContext.current
    val readyToInstall = playUpdateStatus as? PlayUpdateStatus.ReadyToInstall
    val githubUpdate = updateController.githubUpdate

    val message: String
    val actionLabel: String
    var actionEnabled = true
    val onAction: () -> Unit
    when {
        readyToInstall != null -> {
            message = "Update downloaded"
            actionLabel = "Restart"
            onAction = { readyToInstall.manager.completeUpdate() }
        }
        githubUpdate != null -> when (val download = updateController.githubUpdateDownload) {
            is GitHubUpdateDownloadState.ReadyToInstall -> {
                message = "Lamplight ${githubUpdate.versionName} is ready to install"
                actionLabel = "Install"
                onAction = {
                    if (context.packageManager.canRequestPackageInstalls()) {
                        runCatching { context.startActivity(installApkIntent(context, download.apkFile)) }
                    } else {
                        // "Install unknown apps" is a per-app Settings toggle, not a runtime
                        // permission dialog -- send the guest there, then they tap Install again.
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }
                    }
                }
            }
            GitHubUpdateDownloadState.Downloading -> {
                message = "Downloading Lamplight ${githubUpdate.versionName}…"
                actionLabel = "Downloading…"
                actionEnabled = false
                onAction = {}
            }
            GitHubUpdateDownloadState.NotStarted -> {
                message = "Lamplight ${githubUpdate.versionName} is available"
                actionLabel = "Download"
                onAction = { updateController.startGitHubUpdateDownload(githubUpdate) }
            }
        }
        else -> return
    }

    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Cream, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction, enabled = actionEnabled) { Text(actionLabel, color = Amber) }
    }
}
