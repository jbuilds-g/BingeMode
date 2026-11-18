package com.jbuilds.bingemode.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Image tile with a swipeable full-screen collection viewer and user-selected download destination.
 * The Storage Access Framework avoids broad storage permissions on every supported Android version.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    enableViewer: Boolean = false,
    viewerImages: List<String> = emptyList(),
    viewerIndex: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewerOpen by remember { mutableStateOf(false) }
    var saveTarget by remember { mutableStateOf<String?>(null) }
    val imageUrls = remember(model, viewerImages) {
        (viewerImages.ifEmpty { listOfNotNull(model) })
            .map { it.toOriginalTmdbImageUrl() }
            .distinct()
    }
    val initialPage = viewerIndex.coerceIn(0, (imageUrls.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { imageUrls.size }
    )

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/*")
    ) { uri ->
        val target = saveTarget
        if (uri != null && target != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val bytes = readImageBytes(context, target)
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("Could not open the selected destination.")
                    }
                    android.widget.Toast.makeText(context, "Image saved", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Couldn't save image", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        saveTarget = null
    }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.then(
            if (model != null && enableViewer) Modifier.clickableNoRipple { viewerOpen = true }
            else Modifier
        )
    )

    if (viewerOpen && imageUrls.isNotEmpty()) {
        Dialog(
            onDismissRequest = { viewerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = androidx.compose.ui.graphics.Color.Black
            ) {
                Box(Modifier.fillMaxSize()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { imageUrls[it] }
                    ) { page ->
                        val imageUrl = imageUrls[page]
                        var scale by remember(imageUrl) { mutableFloatStateOf(1f) }
                        var offsetX by remember(imageUrl) { mutableFloatStateOf(0f) }
                        var offsetY by remember(imageUrl) { mutableFloatStateOf(0f) }
                        val transformState = rememberTransformableState { zoom, pan, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offsetX += pan.x
                            offsetY += pan.y
                        }
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(transformState, canPan = { scale > 1f })
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offsetX
                                    translationY = offsetY
                                }
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = {
                            val currentImage = imageUrls[pagerState.currentPage]
                            saveTarget = currentImage
                            saveLauncher.launch(imageFileName(currentImage))
                        }) {
                            Icon(
                                Icons.Rounded.Download,
                                "Save image",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                        IconButton(onClick = { viewerOpen = false }) {
                            Icon(
                                Icons.Rounded.Close,
                                "Close viewer",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }

                    if (imageUrls.size > 1) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp),
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${imageUrls.size}",
                                color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun readImageBytes(context: Context, image: String): ByteArray =
    withContext(Dispatchers.IO) {
        if (image.startsWith("http://") || image.startsWith("https://")) {
            URL(image).openStream().use { it.readBytes() }
        } else {
            context.contentResolver.openInputStream(Uri.parse(image))?.use { it.readBytes() }
                ?: error("Could not read image.")
        }
    }

private fun imageFileName(image: String): String =
    Uri.parse(image).lastPathSegment?.substringBefore("?")?.takeIf { it.contains('.') }
        ?: "bingemode-image.jpg"

private fun String.toOriginalTmdbImageUrl(): String {
    if (Uri.parse(this).host != "image.tmdb.org") return this
    return replace(Regex("/t/p/(?:w\\d+|h\\d+|original)/"), "/t/p/original/")
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
