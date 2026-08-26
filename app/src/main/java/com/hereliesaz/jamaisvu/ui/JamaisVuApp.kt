@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hereliesaz.jamaisvu.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hereliesaz.jamaisvu.DemoData
import com.hereliesaz.jamaisvu.Gem
import com.hereliesaz.jamaisvu.JamaisVuViewModel

private enum class Tab(val label: String, val icon: ImageVector) {
    FEED("Home", Icons.Default.Home),
    DISCOVER("Discover", Icons.Default.Explore),
    ADD("Add", Icons.Default.Add),
    SAVED("Saved", Icons.Default.Bookmark),
    PROFILE("You", Icons.Default.Person)
}

@Composable
fun JamaisVuApp(vm: JamaisVuViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.FEED) }
    var selectedGemId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = vm.gems.firstOrNull { it.id == selectedGemId }

    if (selected != null) {
        GemDetail(selected, vm, onBack = { selectedGemId = null })
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
                Tab.FEED -> FeedScreen(vm) { selectedGemId = it.id }
                Tab.DISCOVER -> DiscoverScreen(vm) { selectedGemId = it.id }
                Tab.ADD -> AddGemScreen(vm) { tab = Tab.PROFILE }
                Tab.SAVED -> SavedScreen(vm) { selectedGemId = it.id }
                Tab.PROFILE -> ProfileScreen(vm) { selectedGemId = it.id }
            }
        }
    }
}

@Composable
private fun AppHeader(kicker: String? = null, title: String = "jamais vu") {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
        if (kicker != null) Text(kicker.uppercase(), color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
    }
}

@Composable
private fun FeedScreen(vm: JamaisVuViewModel, open: (Gem) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { AppHeader("hidden gems", "jamais vu") }
        item {
            Text(
                "Places worth leaving the house for.",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                color = Fog,
                fontSize = 15.sp
            )
        }
        items(vm.gems, key = { it.id }) { gem ->
            GemCard(gem, vm, open)
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun GemCard(gem: Gem, vm: JamaisVuViewModel, open: (Gem) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().padding(bottom = 22.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clickable { open(gem) }) {
            AsyncImage(
                model = gem.image,
                contentDescription = gem.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = .68f)))
                )
            )
            Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(gem.category.uppercase(), color = Acid, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(gem.title, color = Color.White, fontWeight = FontWeight.Black, fontSize = 26.sp)
                Text("${gem.neighborhood} · ${gem.city}", color = Color.White.copy(alpha = .82f), fontSize = 13.sp)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(gem.username)
            Spacer(Modifier.width(8.dp))
            Text(gem.username, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.toggleVisited(gem.id) }) {
                Icon(Icons.Default.CheckCircle, "Been there", tint = if (vm.isVisited(gem.id)) Moss else Fog)
            }
            IconButton(onClick = { vm.toggleSaved(gem.id) }) {
                Icon(if (vm.isSaved(gem.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Save", tint = if (vm.isSaved(gem.id)) Acid else Fog)
            }
            FilledTonalButton(onClick = { openMap(context, gem) }) {
                Text("GO")
            }
        }
        Text(gem.tip, color = Color.White, modifier = Modifier.padding(horizontal = 18.dp), fontSize = 15.sp, lineHeight = 21.sp)
    }
}

@Composable
private fun DiscoverScreen(vm: JamaisVuViewModel, open: (Gem) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("New Orleans") }
    var category by rememberSaveable { mutableStateOf("All") }
    var showCities by rememberSaveable { mutableStateOf(false) }

    val filtered = vm.gems.filter {
        it.city == city && (category == "All" || it.category == category) &&
            (query.isBlank() || listOf(it.title, it.category, it.neighborhood, it.username).any { field -> field.contains(query, true) })
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DISCOVER", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(city, color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black)
            }
            TextButton(onClick = { showCities = true }) { Text("Change city") }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search gems, neighborhoods, people") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AssistChip(onClick = { category = "All" }, label = { Text("All") }, leadingIcon = { Icon(Icons.Default.Star, null, Modifier.size(16.dp)) }) }
            items(DemoData.categories) { c -> AssistChip(onClick = { category = c }, label = { Text(c) }) }
        }
        Text("ALL-TIME FAVORITES", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = Fog, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.gems.filter { it.city == city }.take(5)) { gem ->
                MiniGem(gem) { open(gem) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("ENDLESS DISCOVER", color = Fog, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("${filtered.size} gems", color = Fog, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f)) {
            gridItems(filtered, key = { it.id }) { gem ->
                Box(Modifier.aspectRatio(1f).clickable { open(gem) }) {
                    AsyncImage(gem.image, gem.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .72f)))))
                    Text(gem.title, Modifier.align(Alignment.BottomStart).padding(7.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (showCities) {
        AlertDialog(
            onDismissRequest = { showCities = false },
            title = { Text("Pick a city") },
            text = {
                Column { DemoData.cities.forEach { c -> TextButton(onClick = { city = c; showCities = false }) { Text(c) } } }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun MiniGem(gem: Gem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(150.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(12.dp)
    ) {
        AsyncImage(gem.image, gem.title, Modifier.fillMaxWidth().aspectRatio(1.35f), contentScale = ContentScale.Crop)
        Text(gem.title, Modifier.padding(10.dp), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AddGemScreen(vm: JamaisVuViewModel, done: () -> Unit) {
    val context = LocalContext.current
    var image by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("New Orleans") }
    var neighborhood by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Eat") }
    var tip by rememberSaveable { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            image = uri.toString()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(18.dp)) {
        Text("ADD A GEM", color = Moss, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text("Something worth sharing", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(1.35f).clip(RoundedCornerShape(18.dp)).background(Panel).clickable { picker.launch(arrayOf("image/*")) },
            contentAlignment = Alignment.Center
        ) {
            if (image != null) AsyncImage(image, "Selected photo", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, null, tint = Moss); Text("Choose a photo", color = Fog) }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(title, { title = it }, label = { Text("Place name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(neighborhood, { neighborhood = it }, label = { Text("Neighborhood") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("TYPE", color = Fog, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DemoData.categories) { c -> AssistChip(onClick = { category = c }, label = { Text(if (category == c) "✓ $c" else c) }) }
        }
        OutlinedTextField(tip, { tip = it }, label = { Text("Your tip — why is it worth going?") }, modifier = Modifier.fillMaxWidth().height(130.dp))
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                vm.addGem(title, city, neighborhood, category, tip, image)
                done()
            },
            enabled = title.isNotBlank() && city.isNotBlank() && tip.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("PUBLISH GEM") }
        Text("No stars. No takedowns. If it isn't worth recommending, don't post it.", color = Fog, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
    }
}

@Composable
private fun SavedScreen(vm: JamaisVuViewModel, open: (Gem) -> Unit) {
    var visitedOnly by rememberSaveable { mutableStateOf(false) }
    val shown = vm.gems.filter { if (visitedOnly) vm.isVisited(it.id) else vm.isSaved(it.id) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        AppHeader("your travel diary", if (visitedOnly) "been there" else "want to go")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = { visitedOnly = false }) { Text("Want to go") }
            FilledTonalButton(onClick = { visitedOnly = true }) { Text("Been there") }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nothing here yet. A suspiciously clean slate.", color = Fog) }
        } else {
            LazyColumn { items(shown, key = { it.id }) { gem -> CompactGem(gem, open) } }
        }
    }
}

@Composable
private fun CompactGem(gem: Gem, open: (Gem) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { open(gem) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(gem.image, gem.title, Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(gem.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${gem.neighborhood} · ${gem.city}", color = Fog, fontSize = 12.sp)
            Text(gem.category, color = Moss, fontSize = 12.sp)
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = .06f))
}

@Composable
private fun ProfileScreen(vm: JamaisVuViewModel, open: (Gem) -> Unit) {
    val yours = vm.gems.filter { it.isUserAdded }
    val cities = vm.gems.groupBy { it.city }.entries.sortedByDescending { it.value.size }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar("@you", 78.dp)
                Spacer(Modifier.height(8.dp))
                Text("@you", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("Local collector of reasons to go outside.", color = Fog)
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    Stat(yours.size, "gems")
                    Stat(vm.gems.count { vm.isSaved(it.id) }, "saved")
                    Stat(vm.gems.count { vm.isVisited(it.id) }, "visited")
                }
            }
        }
        item { Text("LOCATION BOARDS", Modifier.padding(16.dp), color = Fog, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
        items(cities.take(6)) { (city, gems) ->
            Card(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable { open(gems.first()) },
                colors = CardDefaults.cardColors(containerColor = Panel)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(gems.first().image, city, Modifier.size(92.dp), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(14.dp)) {
                        Text(city, color = Color.White, fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("${gems.size} gems", color = Fog)
                    }
                }
            }
        }
        item {
            Text("CREATORS TO FOLLOW", Modifier.padding(16.dp), color = Fog, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            DemoData.creators.forEach { creator ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(creator.handle)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(creator.handle, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(creator.city, color = Fog, fontSize = 12.sp)
                    }
                    Text("${creator.gemCount} gems", color = Moss, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

@Composable
private fun Stat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(label, color = Fog, fontSize = 11.sp)
    }
}

@Composable
private fun Avatar(handle: String, size: androidx.compose.ui.unit.Dp = 38.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Moss), contentAlignment = Alignment.Center) {
        Text(handle.removePrefix("@").take(1).uppercase(), color = Color.Black, fontWeight = FontWeight.Black, fontSize = (size.value * .42f).sp)
    }
}

@Composable
private fun GemDetail(gem: Gem, vm: JamaisVuViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().aspectRatio(.88f)) {
            AsyncImage(gem.image, gem.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .4f), Color.Transparent, Color.Black.copy(alpha = .9f)))))
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp).align(Alignment.TopStart).background(Color.Black.copy(alpha = .4f), CircleShape)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                Text(gem.category.uppercase(), color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(gem.title, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("${gem.neighborhood} · ${gem.city}", color = Fog)
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(gem.username)
            Spacer(Modifier.width(8.dp))
            Text(gem.username, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.toggleSaved(gem.id) }) { Icon(if (vm.isSaved(gem.id)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Save", tint = Acid) }
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${gem.title} — ${gem.city}\n${gem.tip}") }
                context.startActivity(Intent.createChooser(intent, "Share gem"))
            }) { Icon(Icons.Default.Share, "Share", tint = Fog) }
        }
        Text(gem.tip, Modifier.padding(horizontal = 18.dp, vertical = 10.dp), color = Color.White, fontSize = 18.sp, lineHeight = 26.sp)
        Button(onClick = { openMap(context, gem) }, modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Icon(Icons.Default.Map, null)
            Spacer(Modifier.width(8.dp))
            Text("GO TO THIS GEM")
        }
        TextButton(onClick = { vm.toggleVisited(gem.id) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.Default.CheckCircle, null)
            Spacer(Modifier.width(6.dp))
            Text(if (vm.isVisited(gem.id)) "Marked as been there" else "I've been here")
        }
        Spacer(Modifier.height(28.dp))
    }
}

private fun openMap(context: android.content.Context, gem: Gem) {
    val query = Uri.encode("${gem.title}, ${gem.neighborhood}, ${gem.city}")
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
    runCatching { context.startActivity(intent) }
}
