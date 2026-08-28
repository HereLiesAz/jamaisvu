package com.hereliesaz.lamplight.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.hereliesaz.lamplight.DiscoverCategory
import com.hereliesaz.lamplight.LamplightViewModel
import com.hereliesaz.lamplight.Place
import com.hereliesaz.lamplight.PlacePhoto
import com.hereliesaz.lamplight.discoverCategoriesFor

/**
 * The client brief's "Discover" -- a small, fixed set of categories rather than a free-form
 * tag browser (see [DiscoverCategory]), each opening onto the same mosaic grid the Explore
 * screen already uses, filtered to places whose tags land in that category.
 */
@Composable
fun DiscoverScreen(vm: LamplightViewModel, onBack: () -> Unit, open: (Place) -> Unit) {
    // Every place's category membership is recomputed from its current tags each time the
    // catalog changes, not stored -- the catalog is a few hundred places, and this is a
    // handful of set lookups per place, cheap enough not to need caching.
    val placesByCategory = remember(vm.places) {
        val result = mutableMapOf<DiscoverCategory, MutableList<Place>>()
        vm.places.forEach { place ->
            val tags = place.tags + vm.placeDetails(place.id).tags
            discoverCategoriesFor(tags).forEach { category ->
                result.getOrPut(category) { mutableListOf() }.add(place)
            }
        }
        result
    }
    val representativePhotoByCategory = remember(placesByCategory) {
        DiscoverCategory.entries.associateWith { category ->
            placesByCategory[category].orEmpty().firstNotNullOfOrNull { vm.photos(it.id).firstOrNull() }
        }
    }

    var selectedCategory by remember { mutableStateOf<DiscoverCategory?>(null) }
    val category = selectedCategory

    if (category == null) {
        DiscoverCategoryList(
            representativePhotoByCategory = representativePhotoByCategory,
            onBack = onBack,
            onSelectCategory = { selectedCategory = it }
        )
    } else {
        DiscoverCategoryResults(
            category = category,
            places = placesByCategory[category].orEmpty(),
            vm = vm,
            onBack = { selectedCategory = null },
            open = open
        )
    }
}

@Composable
private fun DiscoverCategoryList(
    representativePhotoByCategory: Map<DiscoverCategory, PlacePhoto?>,
    onBack: () -> Unit,
    onSelectCategory: (DiscoverCategory) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Cream) }
            Text("DISCOVER", color = Fog, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = LocalMartianMonoFontFamily.current)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
            items(DiscoverCategory.entries) { category ->
                DiscoverCategoryRow(
                    category = category,
                    representativePhoto = representativePhotoByCategory[category],
                    onClick = { onSelectCategory(category) }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun DiscoverCategoryRow(category: DiscoverCategory, representativePhoto: PlacePhoto?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(0.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (representativePhoto != null) {
            AsyncImage(
                model = representativePhoto.uri,
                contentDescription = null,
                modifier = Modifier.width(64.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(category.label, color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(2.dp))
            Text(category.tagline, color = Fog, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DiscoverCategoryResults(
    category: DiscoverCategory,
    places: List<Place>,
    vm: LamplightViewModel,
    onBack: () -> Unit,
    open: (Place) -> Unit
) {
    Column(Modifier.fillMaxSize().background(Ink).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Cream) }
            Column {
                Text(category.label.uppercase(), color = Fog, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = LocalMartianMonoFontFamily.current)
                Text(category.tagline, color = Cream, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (places.isEmpty()) {
            Text(
                "Nothing tagged for this category yet.",
                color = Fog,
                fontSize = 13.sp,
                modifier = Modifier.padding(18.dp)
            )
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                itemsIndexed(places, key = { _, place -> place.id }) { _, place ->
                    DiscoverPlaceCard(place, vm, open)
                }
            }
        }
    }
}

@Composable
private fun DiscoverPlaceCard(place: Place, vm: LamplightViewModel, open: (Place) -> Unit) {
    val photo = vm.photos(place.id).firstOrNull()
    Card(
        onClick = { open(place) },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            if (photo != null) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = "Photo of ${place.venue}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize().background(Color.Black))
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(place.venue, color = Cream, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 2)
        }
    }
}
