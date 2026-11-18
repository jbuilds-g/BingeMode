package com.jbuilds.bingemode.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Image tile with a shared full-screen zoom viewer and user-selected download destination.
 * The Storage Access Framework avoids broad storage permissions on every supported Android version.
 */
@Composable
fun MediaImage(
    model: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewerOpen by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val fileName = remember(model) {
        Uri.parse(model ?: "").lastPathSegment?.substringBefore("?")?.takeIf { it.contains('.') }
            ?: "bingemode-image.jpg"
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        if (uri != null && model != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val bytes = URL(model).openStream().use { it.readBytes() }
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                            ?: error("Could not open the selected destination.")
                    }
                    android.widget.Toast.makeText(context, "Image saved", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Couldn't save image", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.then(if (model != null) Modifier.clickableNoRipple { viewerOpen = true } else Modifier)
    )

    if (viewerOpen && model != null) {
        var scale by remember(model) { mutableFloatStateOf(1f) }
        var offsetX by remember(model) { mutableFloatStateOf(0f) }
        var offsetY by remember(model) { mutableFloatStateOf(0f) }
        val transformState = rememberTransformableState { zoom, pan, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            offsetX += pan.x
            offsetY += pan.y
        }
        Dialog(
            onDismissRequest = { viewerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = model,
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(transformState)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { saveLauncher.launch(fileName) }) {
                            Icon(Icons.Rounded.Download, "Save image", tint = androidx.compose.ui.graphics.Color.White)
                        }
                        IconButton(onClick = { viewerOpen = false }) {
                            Icon(Icons.Rounded.Close, "Close viewer", tint = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))
