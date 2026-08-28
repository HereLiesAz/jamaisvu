package com.hereliesaz.lamplight

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.hereliesaz.lamplight.ui.LamplightApp
import com.hereliesaz.lamplight.ui.LamplightTheme

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("lamplightWeb") {
        // AsyncImage's default singleton ImageLoader has no fetcher for real HTTP(S) URIs
        // on its own -- Android never needed one (every photo URI is a local
        // file:///android_asset/... path), but web's photoBaseUri()-relative paths are
        // genuine fetches. Registering this here, not in :shared, since it's the one thing
        // in this whole migration Android's side has no equivalent of at all.
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory()) }
                .build()
        }

        LamplightTheme {
            // A page load has no Android-style "recreated but state retained" event for a
            // ViewModel factory to guard against, so a plain remember (tied to this composition,
            // which lives as long as the page does) is all the retention this needs -- no
            // platformBanner, since the GitHub-releases update surface it would carry is
            // Android-only, never a web concept.
            val vm = remember { LamplightViewModel(BrowserSettingsStore()) }
            LamplightApp(vm)
        }
    }
}
