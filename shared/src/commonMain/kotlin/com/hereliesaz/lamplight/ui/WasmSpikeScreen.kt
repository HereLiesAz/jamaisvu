package com.hereliesaz.lamplight.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// PR2 spike: proves SharedTransitionLayout/sharedBounds -- the mechanism behind this app's
// real mosaic-to-detail hero animation -- actually compiles and runs on wasmJs, before the
// rest of the migration plan is built on the assumption that it does. Superseded once PR9
// moves the real Explore/PlaceDetail screens into commonMain.
private val spikeItems = listOf("Lantern", "Quarter", "Lamplight", "Muse", "Anchor", "North Star")

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WasmSpikeScreen() {
    var selected by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        SharedTransitionLayout {
            AnimatedContent(targetState = selected, label = "wasm-spike-transition") { targetSelected ->
                if (targetSelected == null) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        items(spikeItems) { item ->
                            Surface(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .aspectRatio(1f)
                                    .sharedBounds(
                                        rememberSharedContentState(key = item),
                                        animatedVisibilityScope = this@AnimatedContent,
                                    )
                                    .clickable { selected = item },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(item, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().clickable { selected = null }) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .sharedBounds(
                                    rememberSharedContentState(key = targetSelected),
                                    animatedVisibilityScope = this@AnimatedContent,
                                ),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(targetSelected, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                        Text(
                            "Compose Multiplatform on wasmJs -- shared-element transition spike. Tap to go back.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}
