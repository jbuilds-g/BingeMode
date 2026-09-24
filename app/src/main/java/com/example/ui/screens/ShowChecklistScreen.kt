package com.jbuilds.bingemode.ui.screens

import com.jbuilds.bingemode.ui.theme.elementBorder
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jbuilds.bingemode.data.model.Show
import com.jbuilds.bingemode.ui.viewmodel.BingeViewModel
import com.jbuilds.bingemode.utils.AutoCheckHelper
import com.jbuilds.bingemode.utils.EpisodeTracker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowChecklistScreen(
    showId: Int,
    viewModel: BingeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEditShow: (id: Int) -> Unit
) {
    val showState by viewModel.activeChecklistShow.collectAsState()
    val isLoading by viewModel.isLoadingChecklist.collectAsState()
    val use24HourClock by viewModel.use24HourClock.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var seasonToDelete by remember { mutableStateOf<Int?>(null) }
    var showAddSeasonDialog by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(showId) {
        viewModel.openShowChecklist(showId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            )
    ) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = showState?.title ?: "Managing Program",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading || showState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val show = showState!!
            val currentSeasonDetails = show.seasonData.find { s -> s.number == show.season }
            val isMovie = show.status == "Movie"
            val episodeNamesList = currentSeasonDetails?.episodeList ?: emptyList()
            
            val sequentialAutoFill by viewModel.sequentialAutoFill.collectAsState()
            val watchedSet = remember(show.watchedEpisodes, show.season) {
                EpisodeTracker.getWatchedEpisodesForSeason(show.watchedEpisodes, show.season)
            }
            val watchedCount = watchedSet.size
            val totalEps = if (isMovie) 1 else (currentSeasonDetails?.episodes ?: maxOf(show.episode, watchedCount, 10))
            val allSeasonNums = (show.seasonData.map { it.number } + listOf(show.season, 1)).distinct().sorted()

            val autoMarkBanner by viewModel.autoMarkBanner.collectAsState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (autoMarkBanner != null && autoMarkBanner?.showId == show.id) {
                    val banner = autoMarkBanner!!
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .elementBorder(RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Rounded.AutoAwesome,
                                    contentDescription = "Automation Executed",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp).padding(top = 2.dp)
                                )
                                Column {
                                    Text(
                                        text = "Auto-Mark Executed",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Season ${banner.seasonNum} : Episode ${banner.episodeNum} was automatically checked today at ${banner.triggerTime}.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { viewModel.dismissAutoMarkBanner() }) {
                                    Text("Dismiss", fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.revertAutoMark() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Rounded.Undo,
                                        contentDescription = "Undo",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Revert", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

            if (isMovie) {
                // RENDER GORGEOUS THEATRE/CINEMATIC TRACKING CANVAS FOR MOVIES
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Movie Hero / Cinematic Status Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .elementBorder(RoundedCornerShape(24.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (show.poster != null) {
                                        coil.compose.AsyncImage(
                                            model = show.poster,
                                            contentDescription = show.title,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Movie, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = show.title,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Theatrical Feature Film Tracker",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (watchedCount >= 1) "Status: Completed" else "Status: Unwatched",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            imageVector = if (watchedCount >= 1) Icons.Rounded.CheckCircle else Icons.Rounded.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (watchedCount >= 1) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Watch Tracker Switch Row
                    item {
                        val isWatched = if (sequentialAutoFill) {
                            show.episode >= 1
                        } else {
                            if (show.watchedEpisodes.isNotBlank()) true else show.episode >= 1
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isWatched) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .elementBorder(RoundedCornerShape(20.dp))
                                .clickable {
                                    viewModel.setWatchedEpisode(show.id, if (isWatched) 0 else 1)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isWatched) "Completed Watching!" else "Mark Movie as Watched",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isWatched) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isWatched) "You enjoyed this film! Toggle to clear history." else "Have you sat through this film? Mark off to complete.",
                                        fontSize = 12.sp,
                                        color = if (isWatched) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isWatched,
                                    onCheckedChange = {
                                        viewModel.setWatchedEpisode(show.id, if (isWatched) 0 else 1)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            }
                        }
                    }

                    // Interactive Rating Slider / Stars Selector
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .elementBorder(RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "PERSONAL CRITIQUE SCORE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Your Rating: ",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (show.rating > 0.0) {
                                            Icon(
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = if (show.rating > 0.0) "${show.rating.toString().take(3)} / 10" else "Not Rated Yet",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    (2..10 step 2).forEach { scoreIndex ->
                                        val score = scoreIndex.toDouble()
                                        val isSelected = show.rating >= score - 1.0
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .clickable {
                                                    viewModel.updateShowRating(show.id, score)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${score.toInt()}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Popcorn & Cinematic Experience Milestones Checklist
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .elementBorder(RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "CINEMATIC EXTRA CHECKLIST",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                var imaxChecked by remember { mutableStateOf(false) }
                                var snackChecked by remember { mutableStateOf(false) }
                                var friendsChecked by remember { mutableStateOf(false) }
                                var creditsChecked by remember { mutableStateOf(false) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { imaxChecked = !imaxChecked }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(checked = imaxChecked, onCheckedChange = { imaxChecked = it })
                                    Column {
                                        Text("IMAX Premium Format", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Experienced on high-definition big screen", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { snackChecked = !snackChecked }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(checked = snackChecked, onCheckedChange = { snackChecked = it })
                                    Column {
                                        Text("Popcorn & Soda Combo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("True theatrical snack setup", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { friendsChecked = !friendsChecked }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(checked = friendsChecked, onCheckedChange = { friendsChecked = it })
                                    Column {
                                        Text("Cinema Watch Party", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Shared with friends or movie community", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { creditsChecked = !creditsChecked }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Checkbox(checked = creditsChecked, onCheckedChange = { creditsChecked = it })
                                    Column {
                                        Text("Stayed through Post-Credits", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("Listened to soundtrack and saw teaser scenes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Actions Row (Delete & Edit)
                    item {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                shape = RoundedCornerShape(100.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete Movie", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            if (show.tmdbId == null) {
                                Button(
                                    onClick = { onNavigateToEditShow(show.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    shape = RoundedCornerShape(100.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Info", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header details
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .elementBorder(RoundedCornerShape(18.dp))
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (show.poster != null) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = show.poster,
                                            contentDescription = show.title,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Season ${show.season} Tracking",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (show.rating > 0) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "${show.rating}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val progress = if (totalEps > 0) watchedCount.toFloat() / totalEps else 0f
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(100.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Binge Progress: $watchedCount of $totalEps watched",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AutoCheckConfigCard(
                            show = show,
                            use24HourClock = use24HourClock,
                            onSaveConfig = { enabled, days, time, type, count ->
                                viewModel.updateShowAutoCheck(show.id, enabled, days, time, type, count, showToast = false)
                            },
                            onTriggerManualCheck = {
                                viewModel.triggerManualAutoCheck(show.id)
                            }
                        )
                    }

                    // Material 3 Expressive Season Selector Pill Bar
                    item {
                        Column(
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SEASONS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Season ${show.season} Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (allSeasonNums.size > 1 && (show.tmdbId == null || show.season > 1)) {
                                        IconButton(
                                            onClick = { seasonToDelete = show.season },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Season ${show.season}",
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(allSeasonNums) { sNum ->
                                    val isSelected = sNum == show.season
                                    val sWatched = EpisodeTracker.getWatchedEpisodesForSeason(show.watchedEpisodes, sNum)
                                    val sData = show.seasonData.find { it.number == sNum }
                                    val sTotal = sData?.episodes ?: 0
                                    val isComplete = sTotal > 0 && sWatched.size >= sTotal

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (!isSelected) {
                                                viewModel.selectSeason(show.id, sNum)
                                            }
                                        },
                                        label = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Season $sNum",
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 12.sp
                                                )
                                                if (isComplete) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.CheckCircle,
                                                        contentDescription = "Completed",
                                                        modifier = Modifier.size(13.dp),
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                    )
                                                } else if (sWatched.isNotEmpty()) {
                                                    Text(
                                                        text = "(${sWatched.size}${if (sTotal > 0) "/$sTotal" else ""})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                // "+ Season" is only for manually added shows since API shows have official season manifests
                                if (show.tmdbId == null) {
                                    item {
                                        val nextSeasonCandidate = (allSeasonNums.maxOrNull() ?: 1) + 1
                                        val prevSeason = show.seasonData.find { it.number == nextSeasonCandidate - 1 }
                                            ?: show.seasonData.maxByOrNull { it.number }
                                        val defaultEpisodes = prevSeason?.episodes?.takeIf { it > 0 } ?: (if (show.episode > 0) show.episode else 10)

                                        AssistChip(
                                            onClick = {
                                                showAddSeasonDialog = nextSeasonCandidate to defaultEpisodes
                                            },
                                            label = {
                                                Text(
                                                    text = "+ Season $nextSeasonCandidate",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "EPISODES",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items((1..totalEps).toList()) { epIndex ->
                        val isWatched = watchedSet.contains(epIndex)
                        val epObj = episodeNamesList.find { e -> e.number == epIndex }
                        val epTitle = epObj?.name ?: "Episode $epIndex"
                        val epOverview = epObj?.overview

                        EpisodeProgressRow(
                            seasonNum = show.season,
                            episodeNum = epIndex,
                            title = epTitle,
                            overview = epOverview,
                            isWatched = isWatched,
                            onClick = {
                                viewModel.setWatchedEpisode(show.id, epIndex)
                            }
                        )
                    }

                    // Finished season triggers
                    if (watchedCount >= totalEps && totalEps > 0) {
                        val nextSeasonObj = show.seasonData.find { s -> s.number == show.season + 1 }
                        if (nextSeasonObj != null) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .elementBorder(RoundedCornerShape(16.dp))
                                        .padding(vertical = 12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Season Completed!",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.EmojiEvents,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Text(
                                            text = "You finished Season ${show.season}. Start Season ${show.season + 1} with ${nextSeasonObj.episodes} episodes?",
                                            fontSize = 13.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.startNextSeason(show.id, show.season + 1)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary
                                            )
                                        ) {
                                            Text("Start Season ${show.season + 1}")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Edit/Delete section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showDeleteConfirm = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(100.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete Show", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                if (show.tmdbId == null) {
                                    Button(
                                        onClick = {
                                            onNavigateToEditShow(show.id)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        shape = RoundedCornerShape(100.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit Show", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (allSeasonNums.size > 1 && (show.tmdbId == null || show.season > 1)) {
                                OutlinedButton(
                                    onClick = { seasonToDelete = show.season },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(100.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Delete Season ${show.season}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Tracked Series?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete '${showState?.title}'? All tracked session history will be forgotten."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showState?.id?.let {
                            viewModel.deleteShow(it)
                        }
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete Tracked Show")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (seasonToDelete != null) {
        val targetSeason = seasonToDelete!!
        AlertDialog(
            onDismissRequest = { seasonToDelete = null },
            title = { Text("Delete Season $targetSeason?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete Season $targetSeason from '${showState?.title}'? All logged episodes and tracking progress for this season will be removed.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showState?.let { s ->
                            viewModel.deleteSeason(s.id, targetSeason)
                        }
                        seasonToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete Season")
                }
            },
            dismissButton = {
                TextButton(onClick = { seasonToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddSeasonDialog != null) {
        val (seasonCandidate, defaultEpisodes) = showAddSeasonDialog!!
        var episodeCountText by remember(seasonCandidate) { mutableStateOf(defaultEpisodes.toString()) }
        var episodeCount by remember(seasonCandidate) { mutableIntStateOf(defaultEpisodes) }

        AlertDialog(
            onDismissRequest = { showAddSeasonDialog = null },
            title = {
                Text(
                    text = "Add Season $seasonCandidate",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Specify how many episodes Season $seasonCandidate will have. Defaults to $defaultEpisodes episodes based on the previous season.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (episodeCount > 1) {
                                    episodeCount--
                                    episodeCountText = episodeCount.toString()
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                        }
                        OutlinedTextField(
                            value = episodeCountText,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }.take(4)
                                episodeCountText = clean
                                val parsed = clean.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    episodeCount = parsed
                                }
                            },
                            modifier = Modifier
                                .width(90.dp)
                                .padding(horizontal = 8.dp),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        FilledTonalIconButton(
                            onClick = {
                                episodeCount++
                                episodeCountText = episodeCount.toString()
                            }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Increase")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = episodeCountText.toIntOrNull() ?: episodeCount
                        showState?.let { s ->
                            viewModel.startNextSeason(
                                showId = s.id,
                                nextSeasonNum = seasonCandidate,
                                customEpisodeCount = count
                            )
                        }
                        showAddSeasonDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Add Season")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSeasonDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
}

@Composable
fun EpisodeProgressRow(
    seasonNum: Int,
    episodeNum: Int,
    title: String,
    overview: String?,
    isWatched: Boolean,
    onClick: () -> Unit
) {
    var expandedOverview by remember { mutableStateOf(false) }

    val containerColor by animateColorAsState(
        targetValue = if (isWatched) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "epContainerColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isWatched) 0.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .elementBorder(RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Checkbox status Box with minimum 48dp touch target
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isWatched) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                            .border(
                                width = if (isWatched) 0.dp else 1.5.dp,
                                color = if (isWatched) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isWatched) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Watched",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Ep detailed Title text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "S$seasonNum E$episodeNum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isWatched) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (isWatched) TextDecoration.LineThrough else TextDecoration.None
                    )
                }

                if (!overview.isNullOrBlank()) {
                    IconButton(
                        onClick = { expandedOverview = !expandedOverview },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (expandedOverview) Icons.Rounded.ExpandLess else Icons.Rounded.Info,
                            contentDescription = if (expandedOverview) "Less info" else "Show overview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expandedOverview && !overview.isNullOrBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 48.dp, end = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = overview ?: "",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AutoCheckConfigCard(
    show: Show,
    use24HourClock: Boolean = true,
    onSaveConfig: (enabled: Boolean, days: String, time: String, type: String, count: Int) -> Unit,
    onTriggerManualCheck: () -> Unit = {}
) {
    var isExpanded by remember(show.id) { mutableStateOf(false) }
    var enabled by remember(show.id) { mutableStateOf(show.autoCheckEnabled) }
    val selectedType = "custom_days"
    var selectedDaysStr by remember(show.id) {
        val initialDays = if (show.autoCheckType == "daily") "1,2,3,4,5,6,7" else show.autoCheckDays
        mutableStateOf(if (initialDays.isBlank()) "1,2,3,4,5,6,7" else initialDays)
    }
    var timeStr by remember(show.id) { mutableStateOf(show.autoCheckTime) }
    var episodesCount by remember(show.id) { mutableIntStateOf(show.autoCheckCount) }

    val timeParts = timeStr.split(":")
    var selectedHour by remember(show.id) { mutableStateOf(timeParts.getOrNull(0) ?: "20") }
    var selectedMinute by remember(show.id) { mutableStateOf(timeParts.getOrNull(1) ?: "00") }

    val daysSet = remember(selectedDaysStr) {
        if (selectedDaysStr.isBlank()) emptySet()
        else selectedDaysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val timePickerDialog = remember(show.id, selectedHour, selectedMinute, use24HourClock) {
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                val hStr = String.format("%02d", hour)
                val mStr = String.format("%02d", minute)
                selectedHour = hStr
                selectedMinute = mStr
                timeStr = "$hStr:$mStr"
                onSaveConfig(enabled, selectedDaysStr, "$hStr:$mStr", selectedType, episodesCount)
            },
            selectedHour.toIntOrNull() ?: 20,
            selectedMinute.toIntOrNull() ?: 0,
            use24HourClock
        )
    }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevronRotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .elementBorder(RoundedCornerShape(22.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Collapsible Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Scheduled Auto-Check",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Expressive status pill
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = if (enabled) "ACTIVE" else "OFF",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Text(
                            text = if (enabled) {
                                AutoCheckHelper.formatNextScheduledTime(show, use24Hour = use24HourClock)
                            } else {
                                "Off • Tap to configure schedule"
                            },
                            fontSize = 11.sp,
                            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { isChecked ->
                            enabled = isChecked
                            onSaveConfig(isChecked, selectedDaysStr, "$selectedHour:$selectedMinute", selectedType, episodesCount)
                        }
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.rotate(chevronRotation)
                        )
                    }
                }
            }

            // Collapsible Details Section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))

                    // Next Run Expressive Callout Banner
                    if (enabled) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTimeFilled,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "UPCOMING TRIGGER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = AutoCheckHelper.formatNextScheduledTime(show, use24Hour = use24HourClock),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Schedule Quick Presets
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PRESETS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isDaily = daysSet.size == 7
                            val isWeekdays = daysSet == setOf(1, 2, 3, 4, 5)
                            val isWeekends = daysSet == setOf(6, 7)

                            listOf(
                                Triple("Daily", "1,2,3,4,5,6,7", isDaily),
                                Triple("Weekdays", "1,2,3,4,5", isWeekdays),
                                Triple("Weekends", "6,7", isWeekends)
                            ).forEach { (label, daysValue, isSelected) ->
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedDaysStr = daysValue
                                        onSaveConfig(enabled, daysValue, "$selectedHour:$selectedMinute", selectedType, episodesCount)
                                    },
                                    label = {
                                        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Trigger Days Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CUSTOM DAYS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "M" to 1,
                                "T" to 2,
                                "W" to 3,
                                "T" to 4,
                                "F" to 5,
                                "Sa" to 6,
                                "Su" to 7
                            ).forEach { (dayLabel, dayInt) ->
                                val isSelected = daysSet.contains(dayInt)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                                        )
                                        .clickable {
                                            val newSet = if (isSelected) daysSet - dayInt else daysSet + dayInt
                                            val newStr = newSet.sorted().joinToString(",")
                                            selectedDaysStr = newStr
                                            onSaveConfig(enabled, newStr, "$selectedHour:$selectedMinute", selectedType, episodesCount)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // Bottom Row: Time Picker and Episode Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Trigger Time Box
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "TRIGGER TIME",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Card(
                                onClick = { timePickerDialog.show() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatDisplayTime(selectedHour, selectedMinute, use24HourClock),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Stepper for Episode Count
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "EPISODES TO MARK",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable {
                                            if (episodesCount > 1) {
                                                episodesCount--
                                                onSaveConfig(enabled, selectedDaysStr, "$selectedHour:$selectedMinute", selectedType, episodesCount)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Remove,
                                        contentDescription = "Decrease",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = episodesCount.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable {
                                            episodesCount++
                                            onSaveConfig(enabled, selectedDaysStr, "$selectedHour:$selectedMinute", selectedType, episodesCount)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Increase",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Test Action: Run Check Now
                    FilledTonalButton(
                        onClick = onTriggerManualCheck,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Test Auto-Check Now (Mark $episodesCount Ep)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatDisplayTime(hourStr: String, minuteStr: String, is24Hour: Boolean): String {
    val h = hourStr.toIntOrNull() ?: 20
    val m = minuteStr.toIntOrNull() ?: 0
    return if (is24Hour) {
        String.format("%02d:%02d", h, m)
    } else {
        val amPm = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        String.format("%d:%02d %s", displayHour, m, amPm)
    }
}
