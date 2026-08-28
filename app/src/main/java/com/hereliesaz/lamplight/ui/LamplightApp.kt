@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class
)

package com.hereliesaz.lamplight.ui

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.hereliesaz.lamplight.GitHubUpdate
import com.hereliesaz.lamplight.InstallSource
import com.hereliesaz.lamplight.LamplightViewModel
import com.hereliesaz.lamplight.Place
import com.hereliesaz.lamplight.PlacePhoto

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    EXPLORE("Explore", Icons.Default.Explore),
    SAVED("Saved", Icons.Default.Bookmark),
    VISITED("Been", Icons.Default.CheckCircle)
}

// Cycled by grid position so the staggered grid reads as a mosaic instead of a uniform checkerboard.
private val MosaicAspectRatios = listOf(0.78f, 1.15f, 1.4f, 0.95f)

@Composable
fun LamplightApp(vm: LamplightViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.EXPLORE) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedId != null) { selectedId = null }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = selectedId,
            label = "place-hero-focus",
            transitionSpec = {
                val effects = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                fadeIn(effects) togetherWith fadeOut(effects)
            }
        ) { targetId ->
            val selected = vm.places.firstOrNull { it.id == targetId }
            if (selected != null) {
                PlaceDetail(selected, vm, this@SharedTransitionLayout, this@AnimatedContent) { selectedId = null }
            } else {
                LamplightHome(
                    vm = vm,
                    tab = tab,
                    onTabChange = { tab = it },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    open = { selectedId = it.id }
                )
            }
        }
    }
}

@Composable
private fun LamplightHome(
    vm: LamplightViewModel,
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    open: (Place) -> Unit
) {
    // Each install source gets only its own update path: Play Core is never touched for a
    // sideloaded install, and the GitHub check (vm.githubUpdate) never runs for a Play install.
    val playUpdateStatus = if (vm.installSource == InstallSource.GOOGLE_PLAY) {
        rememberPlayUpdateStatus()
    } else {
        PlayUpdateStatus.None
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel, modifier = Modifier.navigationBarsPadding()) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { onTabChange(item) },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            UpdateBanner(playUpdateStatus, vm.githubUpdate)
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.EXPLORE -> ExploreScreen(vm, sharedTransitionScope, animatedVisibilityScope, open)
                    Tab.SAVED -> CollectionScreen(
                        "saved", vm, vm.places.filter { vm.isSaved(it.id) },
                        sharedTransitionScope, animatedVisibilityScope, open
                    )
                    Tab.VISITED -> CollectionScreen(
                        "been there", vm, vm.places.filter { vm.isVisited(it.id) },
                        sharedTransitionScope, animatedVisibilityScope, open
                    )
                }
            }
        }
    }
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
private fun UpdateBanner(playUpdateStatus: PlayUpdateStatus, githubUpdate: GitHubUpdate?) {
    val context = LocalContext.current
    val readyToInstall = playUpdateStatus as? PlayUpdateStatus.ReadyToInstall

    val message: String
    val actionLabel: String
    val onAction: () -> Unit
    when {
        readyToInstall != null -> {
            message = "Update downloaded"
            actionLabel = "Restart"
            onAction = { readyToInstall.manager.completeUpdate() }
        }
        githubUpdate != null -> {
            message = "Lamplight ${githubUpdate.versionName} is available"
            actionLabel = "Download"
            onAction = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUpdate.downloadUrl)))
                }
            }
        }
        else -> return
    }

    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction) { Text(actionLabel, color = Acid) }
    }
}

@Composable
private fun ExploreScreen(
    vm: LamplightViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    open: (Place) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = vm.places.filter { place ->
        (tag == null || tag in place.tags) &&
            (query.isBlank() || place.venue.contains(query, true) || place.tags.any { it.contains(query, true) })
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("NEW ORLEANS", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("lamplight", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Text("${vm.places.size} places from the QuarterMuse catalog", color = Fog, fontSize = 13.sp)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search places or tags") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                AssistChip(onClick = { tag = null }, label = { Text(if (tag == null) "✓ All" else "All") })
            }
            items(vm.tags) { candidate ->
                AssistChip(
                    onClick = { tag = if (tag == candidate) null else candidate },
                    label = { Text(if (tag == candidate) "✓ $candidate" else candidate) }
                )
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} places", color = Fog, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (!vm.photosConfigured) Text("No bundled photos in this build", color = Acid, fontSize = 11.sp)
        }

        MosaicGrid(filtered, vm, sharedTransitionScope, animatedVisibilityScope, open, Modifier.weight(1f))
    }
}

@Composable
private fun CollectionScreen(
    title: String,
    vm: LamplightViewModel,
    places: List<Place>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    open: (Place) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(18.dp)) {
            Text("YOUR PLACES", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text("${places.size} places", color = Fog, fontSize = 12.sp)
        }
        if (places.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing here yet.", color = Fog)
            }
        } else {
            MosaicGrid(places, vm, sharedTransitionScope, animatedVisibilityScope, open, Modifier.weight(1f))
        }
    }
}

/** A custom two-column masonry grid: tiles cycle through [MosaicAspectRatios] instead of a uniform checkerboard. */
@Composable
private fun MosaicGrid(
    places: List<Place>,
    vm: LamplightViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    open: (Place) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        itemsIndexed(places, key = { _, place -> place.id }) { index, place ->
            MosaicPlaceCard(place, vm, index, sharedTransitionScope, animatedVisibilityScope, open)
        }
    }
}

@Composable
private fun MosaicPlaceCard(
    place: Place,
    vm: LamplightViewModel,
    index: Int,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    open: (Place) -> Unit
) {
    val photos = vm.photos(place.id)
    val aspectRatio = MosaicAspectRatios[index % MosaicAspectRatios.size]

    Card(
        onClick = { open(place) },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            PhotoFrame(
                place = place,
                photo = photos.firstOrNull(),
                message = "No photo",
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                sharedKey = "photo-${place.id}",
                fullAttribution = false,
                modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio)
            )
            IconButton(
                onClick = { vm.toggleSaved(place.id) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(
                    if (vm.isSaved(place.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    if (vm.isSaved(place.id)) "Remove ${place.venue} from saved" else "Save ${place.venue}",
                    tint = if (vm.isSaved(place.id)) Acid else Color.White
                )
            }
            if (vm.isVisited(place.id)) {
                Icon(
                    Icons.Default.CheckCircle,
                    "Been to ${place.venue}",
                    tint = Moss,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(18.dp)
                )
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(place.venue, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(place.tags.firstOrNull().orEmpty(), color = Fog, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PlaceDetail(
    place: Place,
    vm: LamplightViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedContentScope,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val photos = vm.photos(place.id)

    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("LAMPLIGHT", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Hero focus: this frame shares bounds with the mosaic tile that was tapped.
        PhotoFrame(
            place = place,
            photo = photos.firstOrNull(),
            message = if (!vm.photosConfigured) "No photos bundled in this build" else "No photo for this venue",
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            sharedKey = "photo-${place.id}",
            fullAttribution = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(280.dp)
        )

        Text(
            place.venue,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 38.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
        )
        Text(
            "${place.latitude}, ${place.longitude}",
            color = Fog,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )

        // Full listing info below the hero: remaining photos, tags, actions, map.
        if (photos.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(photos.drop(1), key = { it.uri }) { photo ->
                    PhotoFrame(
                        place = place,
                        photo = photo,
                        message = null,
                        modifier = Modifier.width(220.dp).height(160.dp)
                    )
                }
            }
        }

        Text("TAGS", color = Fog, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(place.tags) { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
        }

        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = { vm.toggleSaved(place.id) }, modifier = Modifier.weight(1f)) {
                Icon(if (vm.isSaved(place.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null)
                Spacer(Modifier.width(6.dp))
                Text(if (vm.isSaved(place.id)) "Saved" else "Save")
            }
            FilledTonalButton(onClick = { vm.toggleVisited(place.id) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(6.dp))
                Text(if (vm.isVisited(place.id)) "Been" else "Been there")
            }
        }

        Button(onClick = { openMaps(context, place) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Icon(Icons.Default.Map, null)
            Spacer(Modifier.width(8.dp))
            Text("OPEN IN MAPS")
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun PhotoFrame(
    place: Place,
    photo: PlacePhoto?,
    message: String?,
    modifier: Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedContentScope? = null,
    sharedKey: String? = null,
    fullAttribution: Boolean = true
) {
    val context = LocalContext.current
    val frameModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null && sharedKey != null) {
        with(sharedTransitionScope) {
            modifier.sharedBounds(
                rememberSharedContentState(key = sharedKey),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                }
            )
        }
    } else {
        modifier
    }

    Column(frameModifier.clip(MaterialTheme.shapes.large).background(Color.Black)) {
        Box(Modifier.fillMaxWidth().weight(1f, fill = true), contentAlignment = Alignment.Center) {
            if (photo != null) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = "Photo of ${place.venue}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(18.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Fog, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(message ?: "No photo found", color = Fog, fontSize = 12.sp)
                }
            }
        }
        if (photo != null) {
            Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text("Google Maps", color = Fog, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                if (fullAttribution) PhotoAttribution(photo)
            }
        }
    }
}

@Composable
private fun PhotoAttribution(photo: PlacePhoto) {
    val html = buildAttributionHtml(photo)
    if (html.isBlank()) return

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            TextView(context).apply {
                textSize = 12f
                setTextColor(android.graphics.Color.LTGRAY)
                setLinkTextColor(android.graphics.Color.WHITE)
                movementMethod = LinkMovementMethod.getInstance()
                linksClickable = true
            }
        },
        update = { view ->
            view.text = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        }
    )
}

private fun buildAttributionHtml(photo: PlacePhoto): String {
    val parts = mutableListOf<String>()
    photo.authors.forEach { author ->
        val name = TextUtils.htmlEncode(author.name)
        val uri = author.uri
        parts += if (uri.isNullOrBlank()) name else "<a href=\"${TextUtils.htmlEncode(uri)}\">$name</a>"
    }
    return parts.distinct().joinToString(" · ")
}

private fun openMaps(context: android.content.Context, place: Place) {
    val label = Uri.encode(place.venue)
    val uri = Uri.parse("geo:${place.latitude},${place.longitude}?q=${place.latitude},${place.longitude}($label)")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}
