package com.hereliesaz.lamplight

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun rememberLocationRequester(): suspend () -> GeoPosition? {
    val context = LocalContext.current
    val locationProvider = remember(context) { AndroidLocationProvider(context) }
    val pendingGrant = remember { mutableStateOf<CancellableContinuation<Boolean>?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingGrant.value?.resume(granted)
        pendingGrant.value = null
    }
    return remember(locationProvider, launcher) {
        suspend {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val granted = alreadyGranted || suspendCancellableCoroutine { continuation ->
                pendingGrant.value = continuation
                launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (granted) locationProvider.currentLocation() else null
        }
    }
}
