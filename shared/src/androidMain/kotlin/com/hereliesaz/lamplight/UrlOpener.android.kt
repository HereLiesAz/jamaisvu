package com.hereliesaz.lamplight

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url: String -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
    }
}

@Composable
actual fun rememberWalkingDirectionsOpener(): (HotelAnchor) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { anchor: HotelAnchor ->
            val navUri = Uri.parse("google.navigation:q=${anchor.latitude},${anchor.longitude}&mode=w")
            val mapsAppIntent = Intent(Intent.ACTION_VIEW, navUri).setPackage("com.google.android.apps.maps")
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(walkingDirectionsUrl(anchor.latitude, anchor.longitude)))
            runCatching { context.startActivity(mapsAppIntent) }
                .onFailure { runCatching { context.startActivity(webIntent) } }
        }
    }
}
