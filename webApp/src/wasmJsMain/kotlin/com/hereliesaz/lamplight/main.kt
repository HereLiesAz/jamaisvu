package com.hereliesaz.lamplight

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.hereliesaz.lamplight.ui.LamplightApp
import com.hereliesaz.lamplight.ui.LamplightTheme

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("lamplightWeb") {
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
