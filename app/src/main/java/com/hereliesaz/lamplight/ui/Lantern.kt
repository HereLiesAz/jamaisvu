@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hereliesaz.lamplight.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hereliesaz.lamplight.HotelAnchor
import com.hereliesaz.lamplight.LamplightViewModel
import com.hereliesaz.lamplight.requestOneTimeLocation
import kotlinx.coroutines.launch

/** The Four Panes mark: a lantern reduced to a 2x2 pane grid. [litCount] panes (0-4) read as lit. */
@Composable
fun FourPanesMark(litCount: Int, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val paneSize = (size - 3.dp) / 2
    Column(modifier.size(size), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LanternPane(lit = litCount >= 1, paneSize)
            LanternPane(lit = litCount >= 2, paneSize)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            LanternPane(lit = litCount >= 3, paneSize)
            LanternPane(lit = litCount >= 4, paneSize)
        }
    }
}

@Composable
private fun LanternPane(lit: Boolean, size: Dp) {
    Column(
        Modifier
            .size(size)
            .background(if (lit) Amber else Color.Transparent)
            .then(if (!lit) Modifier.border(1.dp, Fog) else Modifier)
    ) {}
}

/** Persistent, single-tap access to the Home Lantern from any screen. */
@Composable
fun HomeLanternButton(vm: LamplightViewModel) {
    var showSheet by remember { mutableStateOf(false) }
    var showPrompt by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = { showSheet = true },
        containerColor = Panel,
        contentColor = Amber,
        modifier = Modifier.semantics { contentDescription = "Home Lantern" }
    ) {
        FourPanesMark(litCount = if (vm.hotelAnchor != null) 4 else 1, size = 22.dp)
    }

    if (showSheet) {
        HomeLanternSheet(
            vm = vm,
            onChangeHotel = {
                showSheet = false
                showPrompt = true
            },
            onDismiss = { showSheet = false }
        )
    }
    if (showPrompt) {
        HotelAnchorPrompt(vm, mandatory = false, onDone = { showPrompt = false })
    }
}

@Composable
private fun HomeLanternSheet(vm: LamplightViewModel, onChangeHotel: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val anchor = vm.hotelAnchor

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Panel) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FourPanesMark(litCount = if (anchor != null) 4 else 1, size = 28.dp)
            Spacer(Modifier.height(16.dp))
            if (anchor != null) {
                Text("HOME LANTERN", color = Fog, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(anchor.label, color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { openWalkingDirections(context, anchor) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Take me back", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onChangeHotel, modifier = Modifier.fillMaxWidth()) {
                    Text("Change hotel", color = Fog)
                }
            } else {
                Text("You haven't set a hotel yet.", color = Cream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Save it once and Lamplight can always point you home.",
                    color = Fog,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onChangeHotel, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Set my hotel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * "Where are you staying?" -- [mandatory] on first open (no dismiss but "not staying at a
 * hotel" is always one of the choices), reopenable later via "Change hotel".
 */
@Composable
fun HotelAnchorPrompt(vm: LamplightViewModel, mandatory: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var label by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            error = "Location permission is needed to save this spot."
            return@rememberLauncherForActivityResult
        }
        locating = true
        error = null
        scope.launch {
            val location = requestOneTimeLocation(context)
            locating = false
            if (location != null) {
                vm.setHotelAnchor(label, location.latitude, location.longitude)
                onDone()
            } else {
                error = "Couldn't get a location fix. Try again outdoors or near a window."
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!mandatory) onDone() },
        properties = DialogProperties(dismissOnBackPress = !mandatory, dismissOnClickOutside = !mandatory)
    ) {
        Column(Modifier.fillMaxWidth().background(Panel).padding(24.dp)) {
            Text("Where are you staying?", color = Cream, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "This becomes your Home Lantern -- a fixed point for the whole stay. No account needed.",
                color = Fog,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                placeholder = { Text("Hotel name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                enabled = !locating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (locating) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (locating) "Finding you…" else "Use my location")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    vm.skipHotelAnchor()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("I'm not staying at a hotel", color = Fog)
            }
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Amber, fontSize = 12.sp)
            }
        }
    }
}

/** Walking directions handoff -- prefers the Google Maps app, falls back to a browser deep link. */
fun openWalkingDirections(context: Context, anchor: HotelAnchor) {
    val navUri = Uri.parse("google.navigation:q=${anchor.latitude},${anchor.longitude}&mode=w")
    val mapsAppIntent = Intent(Intent.ACTION_VIEW, navUri).setPackage("com.google.android.apps.maps")
    val webUri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=${anchor.latitude},${anchor.longitude}&travelmode=walking"
    )
    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
    runCatching { context.startActivity(mapsAppIntent) }
        .onFailure { runCatching { context.startActivity(webIntent) } }
}
