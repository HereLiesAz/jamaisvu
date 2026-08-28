package com.hereliesaz.lamplight

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.hereliesaz.lamplight.ui.WasmSpikeScreen

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("lamplightWeb") {
        WasmSpikeScreen()
    }
}
