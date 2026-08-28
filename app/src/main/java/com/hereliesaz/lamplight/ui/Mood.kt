package com.hereliesaz.lamplight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hereliesaz.lamplight.GroupSize
import com.hereliesaz.lamplight.LamplightViewModel
import com.hereliesaz.lamplight.Vibe

/**
 * "Who's out tonight?" and "What are we in the mood for?" shown together on one screen --
 * the client brief's two free selector questions, distinct from the hotel question (which has
 * its own proactive-detection flow). No recommendation engine reads this yet; it's the
 * selector only, kept from the shelved pricing brief without reviving its paywall.
 */
@Composable
fun MoodPrompt(vm: LamplightViewModel, onDone: () -> Unit) {
    var groupSize by remember { mutableStateOf(vm.groupSize) }
    var vibe by remember { mutableStateOf(vm.vibe) }

    Dialog(onDismissRequest = onDone) {
        Column(
            Modifier.fillMaxWidth().background(Panel).padding(24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Who's out tonight?", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupSize.entries.forEach { option ->
                    FilterChip(
                        selected = groupSize == option,
                        onClick = { groupSize = option },
                        label = { Text(option.label) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("What are we in the mood for?", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("Choose a vibe, not a schedule.", color = Fog, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Vibe.entries.forEach { option ->
                    FilterChip(
                        selected = vibe == option,
                        onClick = { vibe = option },
                        label = { Text(option.label) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val size = groupSize
                    val mood = vibe
                    if (size != null && mood != null) {
                        vm.setMood(size, mood)
                        onDone()
                    }
                },
                enabled = groupSize != null && vibe != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Set the mood", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    vm.skipMoodPrompt()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip for now", color = Fog)
            }
        }
    }
}
