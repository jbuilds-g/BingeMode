package com.jbuilds.bingemode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jbuilds.bingemode.ui.components.MediaImage
import com.jbuilds.bingemode.ui.viewmodel.BingeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtworkGalleryScreen(
    tmdbId: Int,
    isMovie: Boolean,
    title: String,
    viewModel: BingeViewModel,
    onNavigateBack: () -> Unit
) {
    var artwork by remember(tmdbId, isMovie) {
        mutableStateOf<com.jbuilds.bingemode.data.api.ArtworkGallery?>(null)
    }
    var isLoading by remember(tmdbId, isMovie) { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tmdbId, isMovie) {
        isLoading = true
        artwork = null
        viewModel.fetchArtwork(tmdbId, isMovie) { result ->
            artwork = result
            isLoading = false
        }
    }

    val categories = artwork?.let {
        listOf(
            "Posters" to it.posters,
            "Backdrops" to it.backdrops,
            "Logos" to it.logos,
            "Photos & Media" to it.stills
        ).filter { entry -> entry.second.isNotEmpty() }
    }.orEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedCategory ?: "Photos & Artwork",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) selectedCategory = null else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                categories.isEmpty() -> Text(
                    text = "No additional TMDB artwork available.",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                selectedCategory != null -> {
                    val images = categories.firstOrNull { it.first == selectedCategory }?.second.orEmpty()
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 112.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(images.distinct(), key = { _, imageUrl -> imageUrl }) { index, imageUrl ->
                            val collection = images.distinct()
                            MediaImage(
                                model = imageUrl,
                                contentDescription = "$selectedCategory for $title",
                                contentScale = ContentScale.Crop,
                                enableViewer = true,
                                viewerImages = collection,
                                viewerIndex = index,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        if (selectedCategory == "Backdrops" || selectedCategory == "Photos & Media") 1.6f else 0.72f
                                    )
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
                else -> {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        items(categories) { (category, images) ->
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(category, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    TextButton(onClick = { selectedCategory = category }) {
                                        Text("View All")
                                    }
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(images.take(8), key = { _, imageUrl -> imageUrl }) { _, imageUrl ->
                                        val collection = images.distinct()
                                        MediaImage(
                                            model = imageUrl,
                                            contentDescription = "$category for $title",
                                            contentScale = ContentScale.Crop,
                                            enableViewer = true,
                                            viewerImages = collection,
                                            viewerIndex = collection.indexOf(imageUrl).coerceAtLeast(0),
                                            modifier = Modifier
                                                .width(124.dp)
                                                .aspectRatio(
                                                    if (category == "Backdrops" || category == "Photos & Media") 1.6f else 0.72f
                                                )
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
