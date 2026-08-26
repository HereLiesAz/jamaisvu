@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hereliesaz.jamaisvu.ui

import android.content.Intent
import android.net.Uri
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.hereliesaz.jamaisvu.JamaisVuViewModel
import com.hereliesaz.jamaisvu.Place
import com.hereliesaz.jamaisvu.PlacePhoto

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    EXPLORE("Explore", Icons.Default.Explore),
    SAVED("Saved", Icons.Default.Bookmark),
    VISITED("Been", Icons.Default.CheckCircle)
}

@Composable
fun JamaisVuApp(vm: JamaisVuViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.EXPLORE) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = vm.places.firstOrNull { it.id == selectedId }

    if (selected != null) {
        PlaceDetail(selected, vm) { selectedId = null }
        return
    }

    Scaffold(
        containerColor = Ink,
        bottomBar = {
            NavigationBar(containerColor = Panel, modifier = Modifier.navigationBarsPadding()) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(item.icon, item.label) },
                        label = { Text(item.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.EXPLORE -> ExploreScreen(vm) { selectedId = it.id }
                Tab.SAVED -> CollectionScreen("saved", vm, vm.places.filter { vm.isSaved(it.id) }) { selectedId = it.id }
                Tab.VISITED -> CollectionScreen("been there", vm, vm.places.filter { vm.isVisited(it.id) }) { selectedId = it.id }
            }
        }
    }
}

@Composable
private fun ExploreScreen(vm: JamaisVuViewModel, open: (Place) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = vm.places.filter { place ->
        (tag == null || tag in place.tags) &&
            (query.isBlank() || place.venue.contains(query, true) || place.tags.any { it.contains(query, true) })
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text("NEW ORLEANS", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("jamais vu", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
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
            if (!vm.photosConfigured) Text("Photos need a Places API key", color = Acid, fontSize = 11.sp)
        }

        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(filtered, key = { it.id }) { place ->
                PlaceCard(place, vm, open)
            }
        }
    }
}

@Composable
private fun PlaceCard(place: Place, vm: JamaisVuViewModel, open: (Place) -> Unit) {
    val context = LocalContext.current
    val gallery = vm.photoGallery(place.id)
    LaunchedEffect(place.id, gallery.photos.size, gallery.isLoading, gallery.error) {
        if (gallery.photos.isEmpty() && gallery.error == null) vm.ensurePhotos(place, 1)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.clickable { open(place) }) {
            PhotoFrame(
                place = place,
                photo = gallery.photos.firstOrNull(),
                loading = gallery.isLoading,
                message = when {
                    !vm.photosConfigured -> "Photos are not configured in this build"
                    gallery.error != null -> gallery.error
                    else -> null
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)
            )
            Column(Modifier.padding(16.dp)) {
                Text(place.venue, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(8.dp))
                Text(place.tags.joinToString(" · "), color = Fog, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.toggleVisited(place.id) }) {
                Icon(Icons.Default.CheckCircle, null, tint = if (vm.isVisited(place.id)) Moss else Fog)
                Spacer(Modifier.width(5.dp))
                Text(if (vm.isVisited(place.id)) "Been" else "Been there")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { vm.toggleSaved(place.id) }) {
                Icon(
                    if (vm.isSaved(place.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    "Save",
                    tint = if (vm.isSaved(place.id)) Acid else Fog
                )
            }
            FilledTonalButton(onClick = { openMaps(context, place) }) { Text("GO") }
        }
    }
}

@Composable
private fun CollectionScreen(title: String, vm: JamaisVuViewModel, places: List<Place>, open: (Place) -> Unit) {
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
            LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
                items(places, key = { it.id }) { place -> PlaceCard(place, vm, open) }
            }
        }
    }
}

@Composable
private fun PlaceDetail(place: Place, vm: JamaisVuViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val gallery = vm.photoGallery(place.id)

    LaunchedEffect(place.id, gallery.photos.size, gallery.isLoading, gallery.error) {
        if (gallery.error == null && gallery.photos.size < 5) vm.ensurePhotos(place, 5)
    }

    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("JAMAIS VU", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Text(place.venue, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, lineHeight = 38.sp, modifier = Modifier.padding(horizontal = 18.dp))
        Text(
            "${place.latitude}, ${place.longitude}",
            color = Fog,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (gallery.photos.isEmpty()) {
                item {
                    PhotoFrame(
                        place = place,
                        photo = null,
                        loading = gallery.isLoading,
                        message = when {
                            !vm.photosConfigured -> "Photos are not configured in this build"
                            gallery.error != null -> gallery.error
                            else -> null
                        },
                        modifier = Modifier.width(320.dp).height(230.dp)
                    )
                }
            } else {
                items(gallery.photos, key = { it.uri }) { photo ->
                    PhotoFrame(place, photo, false, null, Modifier.width(320.dp).height(260.dp))
                }
                if (gallery.isLoading) {
                    item {
                        Box(Modifier.width(100.dp).height(230.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }
        }

        if (gallery.error != null && vm.photosConfigured) {
            TextButton(onClick = { vm.retryPhotos(place, 5) }, modifier = Modifier.padding(horizontal = 10.dp)) {
                Text("Retry photos")
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
    loading: Boolean,
    message: String?,
    modifier: Modifier
) {
    val context = LocalContext.current
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.Black)) {
        Box(Modifier.fillMaxWidth().weight(1f, fill = true), contentAlignment = Alignment.Center) {
            when {
                photo != null -> {
                    val request = ImageRequest.Builder(context)
                        .data(photo.uri)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build()
                    AsyncImage(
                        model = request,
                        contentDescription = "Google Maps photo of ${place.venue}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                loading -> CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(18.dp)) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Fog, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(message ?: "No Google Maps photo found", color = Fog, fontSize = 12.sp)
                }
            }
        }
        if (photo != null) {
            Column(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text("Google Maps", color = Fog, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                PhotoAttribution(photo)
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
    if (photo.attributionHtml.isNotBlank()) parts += photo.attributionHtml
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
