package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.EpisodeInfo
import com.example.data.model.SeasonInfo
import com.example.data.model.Show
import com.example.data.repository.BingeRepository
import com.example.utils.AutoCheckHelper
import com.example.utils.EpisodeTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BingeViewModel(private val repository: BingeRepository) : ViewModel() {

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    // Tracked shows sorted by updated in database
    val trackedShows: StateFlow<List<Show>> = repository.allShows
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _tmdbApiKey = MutableStateFlow("")
    val tmdbApiKey: StateFlow<String> = _tmdbApiKey.asStateFlow()

    private val _themeMode = MutableStateFlow("cyberpunk")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _displayMode = MutableStateFlow("system")
    val displayMode: StateFlow<String> = _displayMode.asStateFlow()

    private val _use24HourClock = MutableStateFlow(true)
    val use24HourClock: StateFlow<Boolean> = _use24HourClock.asStateFlow()

    private val _elementBorders = MutableStateFlow(false)
    val elementBorders: StateFlow<Boolean> = _elementBorders.asStateFlow()

    private val _hideNavLabels = MutableStateFlow(false)
    val hideNavLabels: StateFlow<Boolean> = _hideNavLabels.asStateFlow()

    private val _sequentialAutoFill = MutableStateFlow(false)
    val sequentialAutoFill: StateFlow<Boolean> = _sequentialAutoFill.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _trendingTvShows = MutableStateFlow<List<Show>>(emptyList())
    val trendingTvShows: StateFlow<List<Show>> = _trendingTvShows.asStateFlow()

    private val _trendingMovies = MutableStateFlow<List<Show>>(emptyList())
    val trendingMovies: StateFlow<List<Show>> = _trendingMovies.asStateFlow()

    private val _upcomingMovies = MutableStateFlow<List<Show>>(emptyList())
    val upcomingMovies: StateFlow<List<Show>> = _upcomingMovies.asStateFlow()

    private val _topRatedMovies = MutableStateFlow<List<Show>>(emptyList())
    val topRatedMovies: StateFlow<List<Show>> = _topRatedMovies.asStateFlow()

    private val _topRatedTvShows = MutableStateFlow<List<Show>>(emptyList())
    val topRatedTvShows: StateFlow<List<Show>> = _topRatedTvShows.asStateFlow()

    private val _popularTvShows = MutableStateFlow<List<Show>>(emptyList())
    val popularTvShows: StateFlow<List<Show>> = _popularTvShows.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Show>>(emptyList())
    val searchResults: StateFlow<List<Show>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _activeChecklistShow = MutableStateFlow<Show?>(null)
    val activeChecklistShow: StateFlow<Show?> = _activeChecklistShow.asStateFlow()

    private val _isLoadingChecklist = MutableStateFlow(false)
    val isLoadingChecklist: StateFlow<Boolean> = _isLoadingChecklist.asStateFlow()

    data class AutoMarkBannerState(
        val showId: Int,
        val showName: String,
        val seasonNum: Int,
        val episodeNum: Int,
        val previousWatchedEpisodes: String,
        val previousEpisodeCount: Int,
        val previousAutoCheckLastRun: Long = 0L,
        val triggerTime: String
    )
    
    private val _autoMarkBanner = MutableStateFlow<AutoMarkBannerState?>(null)
    val autoMarkBanner: StateFlow<AutoMarkBannerState?> = _autoMarkBanner.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadApiKey()
        loadThemeMode()
        loadAutoMarkBanner()
        startAutoCheckScanner()
        startTmdbAutoUpdater()
        checkDueAutoChecks()
    }

    private fun loadAutoMarkBanner() {
        viewModelScope.launch {
            val bannerStr = repository.getSetting("auto_mark_banner")
            if (bannerStr != null) {
                val parts = bannerStr.split("|")
                if (parts.size >= 7) {
                    _autoMarkBanner.value = AutoMarkBannerState(
                        showId = parts[0].toIntOrNull() ?: 0,
                        showName = parts[1],
                        seasonNum = parts[2].toIntOrNull() ?: 0,
                        episodeNum = parts[3].toIntOrNull() ?: 0,
                        previousWatchedEpisodes = parts[4],
                        previousEpisodeCount = parts[5].toIntOrNull() ?: 0,
                        previousAutoCheckLastRun = parts.getOrNull(7)?.toLongOrNull() ?: 0L,
                        triggerTime = parts[6]
                    )
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun saveAutoMarkBanner(banner: AutoMarkBannerState?) {
        _autoMarkBanner.value = banner
        viewModelScope.launch {
            if (banner == null) {
                repository.saveSetting("auto_mark_banner", null)
            } else {
                val str = "${banner.showId}|${banner.showName}|${banner.seasonNum}|${banner.episodeNum}|${banner.previousWatchedEpisodes}|${banner.previousEpisodeCount}|${banner.triggerTime}|${banner.previousAutoCheckLastRun}"
                repository.saveSetting("auto_mark_banner", str)
            }
        }
    }

    fun dismissAutoMarkBanner() {
        saveAutoMarkBanner(null)
    }

    fun revertAutoMark() {
        val banner = _autoMarkBanner.value ?: return
        viewModelScope.launch {
            val show = repository.getShowById(banner.showId) ?: return@launch
            val updatedShow = show.copy(
                episode = banner.previousEpisodeCount,
                watchedEpisodes = banner.previousWatchedEpisodes,
                autoCheckLastRun = if (banner.previousAutoCheckLastRun > 0L) banner.previousAutoCheckLastRun else show.autoCheckLastRun,
                updated = System.currentTimeMillis()
            )
            repository.saveShow(updatedShow)
            saveAutoMarkBanner(null)
            
            // Also update active checklist show if it matches
            if (_activeChecklistShow.value?.id == banner.showId) {
                _activeChecklistShow.value = updatedShow
            }
        }
    }

    fun setupSimulationMockShow() {
        if (!com.example.BuildConfig.DEBUG) return
        viewModelScope.launch {
            val existingMock = trackedShows.value.find { it.title.contains("Arcane", ignoreCase = true) }
            if (existingMock == null) {
                val mockShow = Show(
                    title = "Arcane",
                    poster = "",
                    status = "Returning Series",
                    season = 2,
                    episode = 4,
                    watchedEpisodes = "1,2,3,4",
                    updated = System.currentTimeMillis()
                )
                repository.saveShow(mockShow)
            }
        }
    }

    fun simulateAutoCheck(context: android.content.Context) {
        if (!com.example.BuildConfig.DEBUG) return
        viewModelScope.launch {
            val existingMock = trackedShows.value.find { it.title.contains("Arcane", ignoreCase = true) }
            val mockShow = if (existingMock == null) {
                val newShow = Show(
                    title = "Arcane",
                    poster = "",
                    status = "Returning Series",
                    season = 2,
                    episode = 4,
                    watchedEpisodes = "1,2,3,4",
                    updated = System.currentTimeMillis()
                )
                val id = repository.saveShow(newShow).toInt()
                newShow.copy(id = id)
            } else {
                existingMock
            }
            
            com.example.utils.AutoCheckHelper.processAutoCheck(
                context, 
                repository, 
                mockShow.id, 
                1, 
                mockShow.autoCheckLastRun, 
                _tmdbApiKey.value
            )
        }
    }
    
    private var _simulationJob: kotlinx.coroutines.Job? = null
    
    fun cancelSimulation() {
        _simulationJob?.cancel()
        _simulationJob = null
    }

    private fun loadApiKey() {
        viewModelScope.launch {
            _tmdbApiKey.value = repository.getSetting("tmdb_key") ?: ""
            loadDiscoveryFeed()
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            repository.saveSetting("tmdb_key", key)
            _tmdbApiKey.value = key
            _toastMessage.value = "Settings Saved!"
            loadDiscoveryFeed()
        }
    }

    fun loadDiscoveryFeed() {
        viewModelScope.launch {
            if (_tmdbApiKey.value.isBlank()) {
                if (com.example.BuildConfig.DEBUG) {
                    _trendingTvShows.value = getMockTvShows()
                    _trendingMovies.value = getMockMovies()
                    _upcomingMovies.value = getMockUpcomingMovies()
                    _topRatedMovies.value = getMockTopRatedMovies()
                    _topRatedTvShows.value = getMockTopRatedTvShows()
                    _popularTvShows.value = getMockPopularTvShows()
                } else {
                    _trendingTvShows.value = emptyList()
                    _trendingMovies.value = emptyList()
                    _upcomingMovies.value = emptyList()
                    _topRatedMovies.value = emptyList()
                    _topRatedTvShows.value = emptyList()
                    _popularTvShows.value = emptyList()
                }
                return@launch
            }
            _isDiscovering.value = true
            try {
                val tvList = repository.getTrendingTv()
                val movieList = repository.getTrendingMovies()
                val upcomingList = repository.getUpcomingMovies()
                val topRatedMovieList = repository.getTopRatedMovies()
                val topRatedTvList = repository.getTopRatedTv()
                val popularTvList = repository.getPopularTv()

                _trendingTvShows.value = if (tvList.isNotEmpty()) tvList else if (com.example.BuildConfig.DEBUG) getMockTvShows() else emptyList()
                _trendingMovies.value = if (movieList.isNotEmpty()) movieList else if (com.example.BuildConfig.DEBUG) getMockMovies() else emptyList()
                
                // Filter out any upcoming movie that already exists in trending to ensure zero duplicates
                val trendingIds = movieList.mapNotNull { it.tmdbId }.toSet()
                val trendingTitles = movieList.map { it.title.lowercase().trim() }.toSet()
                val filteredUpcoming = upcomingList.filter { 
                    it.tmdbId !in trendingIds && it.title.lowercase().trim() !in trendingTitles
                }
                _upcomingMovies.value = if (filteredUpcoming.isNotEmpty()) filteredUpcoming else if (com.example.BuildConfig.DEBUG) getMockUpcomingMovies() else emptyList()
                
                _topRatedMovies.value = if (topRatedMovieList.isNotEmpty()) topRatedMovieList else if (com.example.BuildConfig.DEBUG) getMockTopRatedMovies() else emptyList()
                _topRatedTvShows.value = if (topRatedTvList.isNotEmpty()) topRatedTvList else if (com.example.BuildConfig.DEBUG) getMockTopRatedTvShows() else emptyList()
                _popularTvShows.value = if (popularTvList.isNotEmpty()) popularTvList else if (com.example.BuildConfig.DEBUG) getMockPopularTvShows() else emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                _trendingTvShows.value = emptyList()
                _trendingMovies.value = emptyList()
                _upcomingMovies.value = emptyList()
                _topRatedMovies.value = emptyList()
                _topRatedTvShows.value = emptyList()
                _popularTvShows.value = emptyList()
            } finally {
                _isDiscovering.value = false
            }
        }
    }

    fun fetchDiscoveryDetail(tmdbId: Int, isMovie: Boolean, onComplete: (com.example.data.repository.DiscoveryDetail) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(repository.getDiscoveryDetail(tmdbId, isMovie))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "TMDB details request failed."
            }
        }
    }


    fun trackDiscoveredShow(discovered: Show, context: android.content.Context? = null) {
        viewModelScope.launch {
            val isUpcomingMovie = discovered.mediaType == "movie" && (
                discovered.rating <= 0.0 || 
                listOf(1000001, 1000002, 1000003).contains(discovered.tmdbId) || 
                discovered.title.contains("(2025)") || 
                discovered.title.contains("(2026)") || 
                discovered.title.contains("Fantastic Four") || 
                discovered.title.contains("Avatar")
            )

            // Check if key is empty
            if (_tmdbApiKey.value.isBlank()) {
                val exists = repository.getAllShowsList().any { it.title.equals(discovered.title, ignoreCase = true) }
                if (exists) {
                    _toastMessage.value = "'${discovered.title}' is already in your Watchlist!"
                    return@launch
                }
                val newShow = Show(
                    id = 0,
                    title = discovered.title,
                    tmdbId = discovered.tmdbId,
                    poster = discovered.poster,
                    status = discovered.status,
                    rating = discovered.rating,
                    seasonData = discovered.seasonData,
                    season = 1,
                    episode = 0,
                    updated = System.currentTimeMillis()
                )
                repository.saveShow(newShow)
                _toastMessage.value = "Added '${discovered.title}' to Watchlist!"
                
                if (isUpcomingMovie && context != null) {
                    NotificationHelper.sendScheduledNotification(context, discovered.title)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2500)
                        NotificationHelper.sendAvailableNotification(context, discovered.title)
                    }
                }
                return@launch
            }

            val exists = repository.getAllShowsList().any { it.tmdbId == discovered.tmdbId }
            if (exists) {
                _toastMessage.value = "'${discovered.title}' is already in your Watchlist!"
                return@launch
            }

            if (discovered.status != "Movie") {
                _isDiscovering.value = true
                discovered.tmdbId?.let { tmdbId ->
                    val fullDetails = repository.getTvShowDetails(tmdbId)
                    if (fullDetails != null) {
                        repository.saveShow(fullDetails.copy(season = 1, episode = 0))
                        _toastMessage.value = "Added '${fullDetails.title}' to Watchlist!"
                    } else {
                        repository.saveShow(discovered.copy(id = 0, season = 1, episode = 0))
                        _toastMessage.value = "Added '${discovered.title}' to Watchlist!"
                    }
                }
                _isDiscovering.value = false
            } else {
                _isDiscovering.value = true
                discovered.tmdbId?.let { tmdbId ->
                    val fullDetails = repository.getMovieDetails(tmdbId)
                    if (fullDetails != null) {
                        repository.saveShow(fullDetails)
                        _toastMessage.value = "Added Movie '${fullDetails.title}' to Watchlist!"
                    } else {
                        repository.saveShow(discovered.copy(id = 0))
                        _toastMessage.value = "Added Movie '${discovered.title}' to Watchlist!"
                    }
                }
                _isDiscovering.value = false
                
                if (isUpcomingMovie && context != null) {
                    NotificationHelper.sendScheduledNotification(context, discovered.title)
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(2500)
                        NotificationHelper.sendAvailableNotification(context, discovered.title)
                    }
                }
            }
        }
    }

    private fun getMockTvShows(): List<Show> {
        return listOf(
            Show(
                id = -1,
                title = "Stranger Things",
                tmdbId = 66732,
                poster = "https://image.tmdb.org/t/p/w500/hx0QD076M9CE7Ge7j4o68g6Ofv9.jpg",
                status = "Returning Series",
                rating = 8.6,
                seasonData = listOf(SeasonInfo(1, 8), SeasonInfo(2, 9), SeasonInfo(3, 8), SeasonInfo(4, 9)),
                season = 1,
                episode = 0
            ),
            Show(
                id = -2,
                title = "Breaking Bad",
                tmdbId = 1399,
                poster = "https://image.tmdb.org/t/p/w500/ztkUQv63U7vXY492z6vTT76S6Gz.jpg",
                status = "Ended",
                rating = 8.9,
                seasonData = listOf(SeasonInfo(1, 7), SeasonInfo(2, 13), SeasonInfo(3, 13), SeasonInfo(4, 13), SeasonInfo(5, 16)),
                season = 1,
                episode = 0
            ),
            Show(
                id = -3,
                title = "The Last of Us",
                tmdbId = 100088,
                poster = "https://image.tmdb.org/t/p/w500/uKVKS7g9GCM6gCOvC9X7UAsg73q.jpg",
                status = "Returning Series",
                rating = 8.7,
                seasonData = listOf(SeasonInfo(1, 9)),
                season = 1,
                episode = 0
            ),
            Show(
                id = -4,
                title = "Arcane",
                tmdbId = 94605,
                poster = "https://image.tmdb.org/t/p/w500/fqldcfctv06mfe686f377uq077Z.jpg",
                status = "Returning Series",
                rating = 8.8,
                seasonData = listOf(SeasonInfo(1, 9), SeasonInfo(2, 9)),
                season = 1,
                episode = 0
            )
        )
    }

    private fun getMockMovies(): List<Show> {
        return listOf(
            Show(
                id = -11,
                title = "Dune: Part Two",
                tmdbId = 693134,
                poster = "https://image.tmdb.org/t/p/w500/cz062RhR639g8Y67UV56La6Ucrg.jpg",
                mediaType = "movie",
                    status = "Movie",
                rating = 8.3,
                season = 1,
                episode = 0
            ),
            Show(
                id = -12,
                title = "Interstellar",
                tmdbId = 157336,
                poster = "https://image.tmdb.org/t/p/w500/gEU2Yth6uX6vSIGb6Zg7K7stb2R.jpg",
                status = "Movie",
                rating = 8.4,
                season = 1,
                episode = 0
            ),
            Show(
                id = -13,
                title = "Oppenheimer",
                tmdbId = 872585,
                poster = "https://image.tmdb.org/t/p/w500/8Gxv2gSjdh46g7asv56z0TdXfLq.jpg",
                status = "Movie",
                rating = 8.1,
                season = 1,
                episode = 0
            ),
            Show(
                id = -14,
                title = "Spider-Man: Across the Spider-Verse",
                tmdbId = 569094,
                poster = "https://image.tmdb.org/t/p/w500/8vtB7p8v6P02vCl68vPRQQDMLvY.jpg",
                status = "Movie",
                rating = 8.4,
                season = 1,
                episode = 0
            )
        )
    }

    private fun getMockUpcomingMovies(): List<Show> {
        return listOf(
            Show(
                id = -21,
                title = "Superman (2026)",
                tmdbId = 1000001,
                poster = "https://image.tmdb.org/t/p/w500/v96I7Pz4ZunInlW9jUee461Mekh.jpg",
                status = "Movie",
                rating = 0.0,
                season = 1,
                episode = 0,
                releaseDate = "2026-07-10"
            ),
            Show(
                id = -22,
                title = "The Fantastic Four (2026)",
                tmdbId = 1000002,
                poster = "https://image.tmdb.org/t/p/w500/6vD9T78h99rR3j3lY6J7W7OqP5f.jpg",
                status = "Movie",
                rating = 0.0,
                season = 1,
                episode = 0,
                releaseDate = "2026-08-28"
            ),
            Show(
                id = -23,
                title = "Avatar: Fire and Ash (2026)",
                tmdbId = 1000003,
                poster = "https://image.tmdb.org/t/p/w500/afvP76vQzXbN00E9D93YfD4Bv8C.jpg",
                status = "Movie",
                rating = 0.0,
                season = 1,
                episode = 0,
                releaseDate = "2026-12-18"
            )
        )
    }

    private fun getMockTopRatedMovies(): List<Show> {
        return listOf(
            Show(
                id = -31,
                title = "The Shawshank Redemption",
                tmdbId = 278,
                poster = "https://image.tmdb.org/t/p/w500/9cqN00Gmq677Z864mAd66iZ6e6b.jpg",
                status = "Movie",
                rating = 8.7,
                season = 1,
                episode = 0
            ),
            Show(
                id = -32,
                title = "The Godfather",
                tmdbId = 238,
                poster = "https://image.tmdb.org/t/p/w500/3bhkrj6UGV2pa6ST976yFHFBgXW.jpg",
                status = "Movie",
                rating = 8.7,
                season = 1,
                episode = 0
            ),
            Show(
                id = -33,
                title = "The Dark Knight",
                tmdbId = 155,
                poster = "https://image.tmdb.org/t/p/w500/qJ2tWGBi6EZYv76E9x7k3gR669t.jpg",
                status = "Movie",
                rating = 8.5,
                season = 1,
                episode = 0
            )
        )
    }

    private fun getMockTopRatedTvShows(): List<Show> {
        return listOf(
            Show(
                id = -41,
                title = "Chernobyl",
                tmdbId = 87108,
                poster = "https://image.tmdb.org/t/p/w500/hlPhHdaO0gCOq9hKkS1vWjAorXg.jpg",
                status = "Ended",
                rating = 8.9,
                seasonData = listOf(SeasonInfo(1, 5)),
                season = 1,
                episode = 0
            ),
            Show(
                id = -42,
                title = "Attack on Titan",
                tmdbId = 1429,
                poster = "https://image.tmdb.org/t/p/w500/qm99ovZid7Nq7shR82zQo6Y4VvN.jpg",
                status = "Ended",
                rating = 8.7,
                seasonData = listOf(SeasonInfo(1, 25), SeasonInfo(2, 12), SeasonInfo(3, 22), SeasonInfo(4, 28)),
                season = 1,
                episode = 0
            )
        )
    }

    private fun getMockPopularTvShows(): List<Show> {
        return listOf(
            Show(
                id = -51,
                title = "House of the Dragon",
                tmdbId = 94997,
                poster = "https://image.tmdb.org/t/p/w500/77u1u3x7e7D1u7W7X2D1u7W7X.jpg",
                status = "Returning Series",
                rating = 8.4,
                seasonData = listOf(SeasonInfo(1, 10), SeasonInfo(2, 8)),
                season = 1,
                episode = 0
            ),
            Show(
                id = -52,
                title = "Severance",
                tmdbId = 95396,
                poster = "https://image.tmdb.org/t/p/w500/7vS9S5eS97V58n7f3S9V58n7f3S.jpg",
                status = "Returning Series",
                rating = 8.4,
                seasonData = listOf(SeasonInfo(1, 9), SeasonInfo(2, 10)),
                season = 1,
                episode = 0
            )
        )
    }

    private fun loadThemeMode() {
        viewModelScope.launch {
            _themeMode.value = repository.getSetting("theme_mode") ?: "cyberpunk"
            _displayMode.value = repository.getSetting("display_mode") ?: "system"
            _use24HourClock.value = (repository.getSetting("use_24h_clock") ?: "true") == "true"
            _elementBorders.value = repository.getSetting("element_borders") == "true"
            _hideNavLabels.value = repository.getSetting("hide_nav_labels") == "true"
            _sequentialAutoFill.value = repository.getSetting("sequential_auto_fill") == "true"
        }
    }

    fun saveUse24HourClock(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("use_24h_clock", enabled.toString())
            _use24HourClock.value = enabled
            _toastMessage.value = if (enabled) "24-Hour clock format enabled!" else "12-Hour clock format enabled!"
        }
    }

    fun saveElementBorders(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("element_borders", enabled.toString())
            _elementBorders.value = enabled
            _toastMessage.value = if (enabled) "Element borders enabled!" else "Element borders disabled."
        }
    }

    fun saveHideNavLabels(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("hide_nav_labels", if (enabled) "true" else "false")
            _hideNavLabels.value = enabled
        }
    }

    fun saveSequentialAutoFill(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveSetting("sequential_auto_fill", enabled.toString())
            _sequentialAutoFill.value = enabled
            _toastMessage.value = if (enabled) "Sequential Auto-Fill enabled." else "Sequential Auto-Fill disabled."
        }
    }

    fun saveThemeMode(mode: String) {
        viewModelScope.launch {
            repository.saveSetting("theme_mode", mode)
            _themeMode.value = mode
            val themeName = when (mode) {
                "adaptive" -> "Adaptive Wallpaper"
                "cinema" -> "Cinema Accent"
                "cyberpunk" -> "Cyberpunk Neon"
                "sunset" -> "Sunset Amber"
                "forest" -> "Forest Sage"
                "lavender" -> "Sweet Lavender"
                else -> mode
            }
            _toastMessage.value = "$themeName theme enabled!"
        }
    }

    fun saveDisplayMode(mode: String) {
        viewModelScope.launch {
            repository.saveSetting("display_mode", mode)
            _displayMode.value = mode
            val modeName = when (mode) {
                "system" -> "System Theme"
                "dark" -> "Dark Mode"
                "light" -> "Light Mode"
                "amoled" -> "AMOLED Black"
                else -> mode
            }
            _toastMessage.value = "$modeName enabled!"
        }
    }

    suspend fun fetchSectionPage(sectionId: String, page: Int): List<Show> {
        val apiKey = _tmdbApiKey.value
        if (apiKey.isBlank()) return emptyList()
        return try {
            when (sectionId) {
                "coming_soon" -> {
                    val rawUpcoming = repository.getUpcomingMovies(page)
                    val trendingIds = _trendingMovies.value.mapNotNull { it.tmdbId }.toSet()
                    val trendingTitles = _trendingMovies.value.map { it.title.lowercase().trim() }.toSet()
                    rawUpcoming.filter { 
                        it.tmdbId !in trendingIds && it.title.lowercase().trim() !in trendingTitles
                    }
                }
                "trending_movies" -> repository.getTrendingMovies(page)
                "top_rated_movies" -> repository.getTopRatedMovies(page)
                "trending_tv" -> repository.getTrendingTv(page)
                "top_rated_tv" -> repository.getTopRatedTv(page)
                "popular_tv" -> repository.getPopularTv(page)
                else -> emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _toastMessage.value = "TMDB request failed. Check your API key or network connection."
            emptyList()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            try {
                _searchResults.value = repository.searchShows(query)
            } catch (e: Exception) {
                e.printStackTrace()
                _searchResults.value = emptyList()
                _toastMessage.value = "TMDB search failed. Check your API key or network connection."
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Load a TMDB show's full details (to get seasons, status, average rating)
    fun fetchShowDetailsForSelection(tmdbId: Int, isMovie: Boolean, onComplete: (Show?) -> Unit) {
        viewModelScope.launch {
            try {
                val showDetails = if (isMovie) {
                    repository.getMovieDetails(tmdbId)
                } else {
                    repository.getTvShowDetails(tmdbId)
                }
                onComplete(showDetails)
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "TMDB details request failed."
                onComplete(null)
            }
        }
    }

    // Save a custom show (Manual or TMDB)
    fun saveShow(
        id: Int = 0,
        title: String,
        tmdbId: Int? = null,
        poster: String? = null,
        status: String? = null,
        rating: Double = 0.0,
        seasonData: List<SeasonInfo> = emptyList(),
        season: Int = 1,
        episode: Int = 0
    ) {
        viewModelScope.launch {
            val currentShow = if (id > 0) repository.getShowById(id) else null
            val seasonChanged = currentShow != null && currentShow.season != season

            val finalEpisode: Int
            val finalWatchedEpisodes: String

            if (currentShow != null) {
                if (seasonChanged) {
                    val seasonWatched = EpisodeTracker.getWatchedEpisodesForSeason(currentShow.watchedEpisodes, season)
                    finalEpisode = seasonWatched.maxOrNull() ?: 0
                    finalWatchedEpisodes = currentShow.watchedEpisodes
                } else {
                    finalEpisode = episode
                    val currentSeasonWatched = EpisodeTracker.getWatchedEpisodesForSeason(currentShow.watchedEpisodes, season)
                    finalWatchedEpisodes = if (currentShow.watchedEpisodes.isNotBlank()) {
                        currentShow.watchedEpisodes
                    } else if (episode > 0) {
                        EpisodeTracker.setSequentialProgress("", season, episode).first
                    } else {
                        ""
                    }
                }
            } else {
                finalEpisode = episode
                finalWatchedEpisodes = if (episode > 0) {
                    EpisodeTracker.setSequentialProgress("", season, episode).first
                } else {
                    ""
                }
            }

            val showToSave = Show(
                id = id,
                title = title,
                tmdbId = tmdbId ?: currentShow?.tmdbId,
                poster = poster ?: currentShow?.poster,
                status = status ?: currentShow?.status,
                rating = if (rating > 0.0) rating else (currentShow?.rating ?: 0.0),
                seasonData = if (seasonData.isNotEmpty()) seasonData else (currentShow?.seasonData ?: emptyList()),
                season = season,
                episode = finalEpisode,
                watchedEpisodes = finalWatchedEpisodes,
                autoCheckEnabled = currentShow?.autoCheckEnabled ?: false,
                autoCheckDays = currentShow?.autoCheckDays ?: "1,2,3,4,5,6,7",
                autoCheckTime = currentShow?.autoCheckTime ?: "20:00",
                autoCheckType = currentShow?.autoCheckType ?: "custom_days",
                autoCheckCount = currentShow?.autoCheckCount ?: 1,
                autoCheckLastRun = currentShow?.autoCheckLastRun ?: 0L,
                updated = System.currentTimeMillis()
            )
            repository.saveShow(showToSave)
            if (_activeChecklistShow.value?.id == id) {
                _activeChecklistShow.value = showToSave
            }
            _toastMessage.value = "Show Saved!"
        }
    }

    fun deleteShow(showId: Int) {
        viewModelScope.launch {
            AutoCheckHelper.cancelAlarm(repository.context, showId)
            repository.deleteShowById(showId)
            if (_activeChecklistShow.value?.id == showId) {
                _activeChecklistShow.value = null
            }
            _toastMessage.value = "Show Deleted"
        }
    }

    private var checklistFlowJob: kotlinx.coroutines.Job? = null

    // Opens a show in the Checklist view, triggering details/episodes auto-heal
    fun openShowChecklist(showId: Int) {
        checklistFlowJob?.cancel()
        if (_activeChecklistShow.value?.id != showId) {
            _activeChecklistShow.value = null
            _isLoadingChecklist.value = true
        }
        checklistFlowJob = viewModelScope.launch {
            val initialShow = repository.getShowById(showId)
            if (initialShow == null) {
                _isLoadingChecklist.value = false
                _toastMessage.value = "Error: Show not found"
                return@launch
            }
            _activeChecklistShow.value = initialShow
            _isLoadingChecklist.value = false

            // Continuously collect from Room database as single source of truth
            launch {
                repository.getShowFlowById(showId).collect { freshShow ->
                    if (freshShow != null) {
                        _activeChecklistShow.value = freshShow
                    }
                    _isLoadingChecklist.value = false
                }
            }

            // Auto-heal in background:
            val tmdbId = initialShow.tmdbId
            if (tmdbId != null && _tmdbApiKey.value.isNotBlank()) {
                var needsSave = false
                var updatedSeasonData = initialShow.seasonData
                var updatedStatus = initialShow.status
                var updatedRating = initialShow.rating
                var updatedPoster = initialShow.poster

                // 1. Fetch missing seasonData (seasons structure)
                if (updatedSeasonData.isEmpty()) {
                    val details = repository.getTvShowDetails(tmdbId)
                    if (details != null && details.seasonData.isNotEmpty()) {
                        updatedSeasonData = details.seasonData
                        updatedStatus = details.status
                        updatedRating = details.rating
                        updatedPoster = details.poster ?: updatedPoster
                        needsSave = true
                    }
                }

                // 2. Fetch missing Episode Names for current active season
                val currentSeasonObj = updatedSeasonData.find { s -> s.number == initialShow.season }
                if (currentSeasonObj != null && currentSeasonObj.episodeList.isNullOrEmpty()) {
                    val epList = repository.getSeasonEpisodes(tmdbId, initialShow.season)
                    if (epList != null) {
                        updatedSeasonData = updatedSeasonData.map { s ->
                            if (s.number == initialShow.season) s.copy(episodeList = epList) else s
                        }
                        needsSave = true
                    }
                }

                if (needsSave) {
                    // Fetch latest show from DB to avoid overwriting user interaction during network call
                    val latestShow = repository.getShowById(showId)
                    if (latestShow != null) {
                        val merged = latestShow.copy(
                            seasonData = updatedSeasonData,
                            status = updatedStatus,
                            rating = if (latestShow.rating == 0.0) updatedRating else latestShow.rating,
                            poster = updatedPoster,
                            updated = System.currentTimeMillis()
                        )
                        repository.saveShow(merged)
                    }
                }
            }
        }
    }

    // Toggle watch status of an episode inside active show
    fun setWatchedEpisode(showId: Int, clickedEpNumber: Int) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            val isSequential = _sequentialAutoFill.value

            val (newWatchedEpisodes, newWatchedSet) = if (isSequential) {
                EpisodeTracker.setSequentialProgress(show.watchedEpisodes, show.season, clickedEpNumber)
            } else {
                EpisodeTracker.toggleEpisode(show.watchedEpisodes, show.season, clickedEpNumber)
            }

            val newProgress = newWatchedSet.maxOrNull() ?: 0

            val updatedShow = show.copy(
                episode = newProgress,
                watchedEpisodes = newWatchedEpisodes,
                updated = System.currentTimeMillis()
            )
            repository.saveShow(updatedShow)
            _activeChecklistShow.value = updatedShow
        }
    }

    // Update rating of a show or movie manually directly on the checklist/canvas details page
    fun updateShowRating(showId: Int, newRating: Double) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            val updatedShow = show.copy(
                rating = newRating,
                updated = System.currentTimeMillis()
            )
            repository.saveShow(updatedShow)
            _activeChecklistShow.value = updatedShow
        }
    }

    // Switch between seasons cleanly preserving each season's watched episodes
    fun selectSeason(showId: Int, targetSeasonNum: Int) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            val seasonWatched = EpisodeTracker.getWatchedEpisodesForSeason(show.watchedEpisodes, targetSeasonNum)
            val seasonProgress = seasonWatched.maxOrNull() ?: 0

            var updatedShow = show.copy(
                season = targetSeasonNum,
                episode = seasonProgress,
                updated = System.currentTimeMillis()
            )

            // Auto-heal the season's episode names if needed
            val tmdbId = updatedShow.tmdbId
            if (tmdbId != null && _tmdbApiKey.value.isNotBlank()) {
                val seasonObj = updatedShow.seasonData.find { s -> s.number == targetSeasonNum }
                if (seasonObj != null && seasonObj.episodeList.isNullOrEmpty()) {
                    val epList = repository.getSeasonEpisodes(tmdbId, targetSeasonNum)
                    if (epList != null) {
                        val updatedSeasons = updatedShow.seasonData.map { s ->
                            if (s.number == targetSeasonNum) {
                                s.copy(episodeList = epList)
                            } else s
                        }
                        updatedShow = updatedShow.copy(seasonData = updatedSeasons)
                    }
                }
            }

            repository.saveShow(updatedShow)
            _activeChecklistShow.value = updatedShow
        }
    }

    // Transition to next season while preserving previous seasons
    fun startNextSeason(showId: Int, nextSeasonNum: Int, customEpisodeCount: Int? = null) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            var updatedSeasons = show.seasonData
            if (updatedSeasons.none { it.number == 1 } && nextSeasonNum > 1) {
                val prevEpCount = show.episode.coerceAtLeast(1)
                updatedSeasons = listOf(SeasonInfo(number = 1, episodes = prevEpCount)) + updatedSeasons
            }
            if (updatedSeasons.none { it.number == nextSeasonNum }) {
                val defaultEpisodes = customEpisodeCount ?: run {
                    val prevSeason = updatedSeasons.find { it.number == nextSeasonNum - 1 }
                        ?: updatedSeasons.maxByOrNull { it.number }
                    prevSeason?.episodes?.takeIf { it > 0 } ?: 10
                }
                updatedSeasons = updatedSeasons + SeasonInfo(number = nextSeasonNum, episodes = defaultEpisodes)
            }

            val existingSeasonWatched = EpisodeTracker.getWatchedEpisodesForSeason(show.watchedEpisodes, nextSeasonNum)
            val initialProgress = existingSeasonWatched.maxOrNull() ?: 0

            var updatedShow = show.copy(
                season = nextSeasonNum,
                episode = initialProgress,
                seasonData = updatedSeasons,
                autoCheckLastRun = System.currentTimeMillis(),
                updated = System.currentTimeMillis()
            )

            // Auto-heal the next season's episode names as well
            val tmdbId = updatedShow.tmdbId
            if (tmdbId != null && _tmdbApiKey.value.isNotBlank()) {
                val nextSeasonObj = updatedShow.seasonData.find { s -> s.number == nextSeasonNum }
                if (nextSeasonObj != null && nextSeasonObj.episodeList.isNullOrEmpty()) {
                    val epList = repository.getSeasonEpisodes(tmdbId, nextSeasonNum)
                    if (epList != null) {
                        val fetchedSeasons = updatedShow.seasonData.map { s ->
                            if (s.number == nextSeasonNum) {
                                s.copy(episodeList = epList, episodes = maxOf(s.episodes, epList.size))
                            } else s
                        }
                        updatedShow = updatedShow.copy(seasonData = fetchedSeasons)
                    }
                }
            }

            repository.saveShow(updatedShow)
            _activeChecklistShow.value = updatedShow
            _toastMessage.value = "Started Season $nextSeasonNum!"
        }
    }

    // Delete a season and clean up its tracking state cleanly
    fun deleteSeason(showId: Int, seasonNum: Int) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            val allSeasonNums = (show.seasonData.map { it.number } + listOf(show.season, 1)).distinct().sorted()
            if (allSeasonNums.size <= 1) {
                _toastMessage.value = "Cannot delete the only season of a show."
                return@launch
            }

            val remainingSeasons = show.seasonData.filter { it.number != seasonNum }
            val remainingNums = allSeasonNums.filter { it != seasonNum }

            val updatedWatched = EpisodeTracker.clearSeason(show.watchedEpisodes, seasonNum)

            val newActiveSeason = if (show.season == seasonNum) {
                remainingNums.filter { it < seasonNum }.maxOrNull()
                    ?: remainingNums.maxOrNull()
                    ?: 1
            } else {
                show.season
            }

            val newSeasonWatched = EpisodeTracker.getWatchedEpisodesForSeason(updatedWatched, newActiveSeason)
            val newProgress = newSeasonWatched.maxOrNull() ?: 0

            val updatedShow = show.copy(
                season = newActiveSeason,
                episode = newProgress,
                seasonData = remainingSeasons,
                watchedEpisodes = updatedWatched,
                updated = System.currentTimeMillis()
            )

            repository.saveShow(updatedShow)
            _activeChecklistShow.value = updatedShow
            _toastMessage.value = "Season $seasonNum deleted."
        }
    }

    // Import Backup JSON string.
    // Legacy raw-array backups remain supported for backward compatibility.
    fun importBackup(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val payload = com.example.data.backup.BackupParser.parse(jsonString)
                val restoredCount = repository.restoreBackup(payload.shows, payload.settings)

                _activeChecklistShow.value = null
                loadApiKey()
                loadThemeMode()
                loadAutoMarkBanner()
                _toastMessage.value =
                    "Backup restored: " + restoredCount + " show(s) and " +
                        payload.settings.size + " setting(s)."
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Backup restore failed: " + (e.message ?: "unknown error")
                onComplete(false)
            }
        }
    }

    // Export a versioned backup containing portable local app state.
    // The TMDB API key is intentionally excluded by BackupParser.
    fun exportBackup(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val showsList = repository.getAllShowsList()
                val settingsList = repository.getAllSettings()
                    .filter { it.key != com.example.data.backup.BackupParser.TMDB_KEY_SETTING }

                onComplete(
                    com.example.data.backup.BackupParser.serialize(showsList, settingsList)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _toastMessage.value = "Backup export failed: " + (e.message ?: "unknown error")
                onComplete("{}")
            }
        }
    }

    private var autoCheckReceiver: android.content.BroadcastReceiver? = null

    private fun startAutoCheckScanner() {
        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.AutoCheckWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).build()
        androidx.work.WorkManager.getInstance(repository.context).enqueueUniquePeriodicWork(
            "AutoCheckScanner",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        // Listen for updates from AutoCheckWorker
        val filter = android.content.IntentFilter("com.example.ACTION_AUTO_CHECK_COMPLETED")
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                val showId = intent.getIntExtra("SHOW_ID", -1)
                val isUndo = intent.getBooleanExtra("IS_UNDO", false)
                if (isUndo) {
                    dismissAutoMarkBanner()
                    if (showId != -1 && _activeChecklistShow.value?.id == showId) {
                        viewModelScope.launch {
                            _activeChecklistShow.value = repository.getShowById(showId)
                        }
                    }
                    return
                }

                if (showId != -1 && _activeChecklistShow.value?.id == showId) {
                    viewModelScope.launch {
                        _activeChecklistShow.value = repository.getShowById(showId)
                    }
                }
                
                val seasonNum = intent.getIntExtra("SEASON", 1)
                val episodeNum = intent.getIntExtra("EPISODE", 1)
                val prevEps = intent.getStringExtra("PREVIOUS_EPISODES") ?: ""
                val prevCount = intent.getIntExtra("PREVIOUS_EPISODE_COUNT", 0)
                
                viewModelScope.launch {
                    val updatedShow = repository.getShowById(showId) ?: return@launch
                    val currentTime = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(java.util.Date())
                    val bannerState = AutoMarkBannerState(
                        showId = updatedShow.id,
                        showName = updatedShow.title,
                        seasonNum = seasonNum,
                        episodeNum = episodeNum,
                        previousWatchedEpisodes = prevEps,
                        previousEpisodeCount = prevCount,
                        previousAutoCheckLastRun = updatedShow.autoCheckLastRun,
                        triggerTime = currentTime
                    )
                    saveAutoMarkBanner(bannerState)
                }
            }
        }
        autoCheckReceiver = receiver
        repository.context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onCleared() {
        super.onCleared()
        autoCheckReceiver?.let { receiver ->
            try {
                repository.context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            autoCheckReceiver = null
        }
    }

    fun updateShowAutoCheck(
        showId: Int,
        enabled: Boolean,
        days: String,
        time: String,
        type: String,
        count: Int,
        showToast: Boolean = true
    ) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            val nextLastRun = if (show.autoCheckLastRun > 0L) show.autoCheckLastRun else System.currentTimeMillis()
            val updatedShow = show.copy(
                autoCheckEnabled = enabled,
                autoCheckDays = days,
                autoCheckTime = time,
                autoCheckType = type,
                autoCheckCount = count,
                autoCheckLastRun = nextLastRun
            )
            repository.saveShow(updatedShow)
            if (_activeChecklistShow.value?.id == showId) {
                _activeChecklistShow.value = updatedShow
            }
            if (enabled) {
                AutoCheckHelper.scheduleNextAlarm(repository.context, updatedShow)
            } else {
                AutoCheckHelper.cancelAlarm(repository.context, showId)
            }
            if (showToast) {
                _toastMessage.value = if (enabled) "Auto-check scheduled" else "Auto-check turned off"
            }
        }
    }

    fun triggerManualAutoCheck(showId: Int) {
        viewModelScope.launch {
            val show = repository.getShowById(showId) ?: return@launch
            val count = if (show.autoCheckCount > 0) show.autoCheckCount else 1
            val success = AutoCheckHelper.processAutoCheck(
                context = repository.context,
                repository = repository,
                showId = show.id,
                addEps = count,
                nextLastRun = System.currentTimeMillis(),
                tmdbApiKey = _tmdbApiKey.value
            )
            if (success) {
                val refreshed = repository.getShowById(showId)
                if (refreshed != null) {
                    _activeChecklistShow.value = refreshed
                }
                _toastMessage.value = "Checked $count episode(s) automatically"
            }
        }
    }

    fun checkDueAutoChecks() {
        viewModelScope.launch {
            try {
                AutoCheckHelper.checkAllShows(repository.context, repository, _tmdbApiKey.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startTmdbAutoUpdater() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(5000)
            while (true) {
                try {
                    val apiKey = _tmdbApiKey.value
                    if (apiKey.isNotBlank()) {
                        val showsList = repository.getAllShowsList()
                        for (show in showsList) {
                            val tmdbId = show.tmdbId ?: continue
                            val isMovie = show.mediaType == "movie"
                            if (isMovie) {
                                try {
                                    val freshMovie = repository.getMovieDetails(tmdbId)
                                    if (freshMovie != null) {
                                        val freshShow = repository.getShowById(show.id) ?: continue
                                        val updatedShow = freshShow.copy(
                                            title = freshMovie.title,
                                            poster = freshMovie.poster ?: freshShow.poster,
                                            status = freshMovie.status,
                                            rating = if (freshShow.rating == 0.0) freshMovie.rating else freshShow.rating,
                                            releaseDate = freshMovie.releaseDate,
                                            updated = System.currentTimeMillis()
                                        )
                                        if (updatedShow != freshShow) {
                                            repository.saveShow(updatedShow)
                                            if (_activeChecklistShow.value?.id == show.id) {
                                                _activeChecklistShow.value = updatedShow
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                try {
                                    val freshTv = repository.getTvShowDetails(tmdbId)
                                    if (freshTv != null) {
                                        val freshShow = repository.getShowById(show.id) ?: continue
                                        val freshSeasonData = freshTv.seasonData
                                        val mergedSeasonData = freshSeasonData.map { freshSeason ->
                                            val existingSeason = freshShow.seasonData.find { s -> s.number == freshSeason.number }
                                            if (existingSeason != null) {
                                                freshSeason.copy(episodeList = existingSeason.episodeList)
                                            } else {
                                                freshSeason
                                            }
                                        }
                                        val updatedShow = freshShow.copy(
                                            title = freshTv.title,
                                            poster = freshTv.poster ?: freshShow.poster,
                                            status = freshTv.status,
                                            seasonData = mergedSeasonData,
                                            releaseDate = freshTv.releaseDate,
                                            updated = System.currentTimeMillis()
                                        )
                                        if (updatedShow != freshShow) {
                                            repository.saveShow(updatedShow)
                                            if (_activeChecklistShow.value?.id == show.id) {
                                                _activeChecklistShow.value = updatedShow
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            kotlinx.coroutines.delay(1000)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(2 * 60 * 60 * 1000)
            }
        }
    }
}

class BingeViewModelFactory(private val repository: BingeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BingeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BingeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
