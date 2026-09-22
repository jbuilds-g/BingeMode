package com.example.data.repository

import android.content.Context
import com.example.data.api.TmdbService
import com.example.data.database.BingeModeDatabase
import com.example.data.model.Show
import com.example.data.model.Setting
import com.example.data.model.SeasonInfo
import com.example.data.model.EpisodeInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class BingeRepository(context: Context) {
    val context: Context = context.applicationContext
    private val db = BingeModeDatabase.getDatabase(this.context)
    private val showDao = db.showDao()
    private val settingDao = db.settingDao()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val tmdbService = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TmdbService::class.java)

    // Flow of tracked shows
    val allShows: Flow<List<Show>> = showDao.getAllShowsFlow()

    suspend fun getShowById(id: Int): Show? = showDao.getShowById(id)

    fun getShowFlowById(id: Int): Flow<Show?> = showDao.getShowFlowById(id)

    suspend fun getAllShowsList(): List<Show> = showDao.getAllShows()

    suspend fun saveShow(show: Show): Long {
        return showDao.insertShow(show.copy(updated = System.currentTimeMillis()))
    }

    suspend fun deleteShow(show: Show) {
        showDao.deleteShow(show)
    }

    suspend fun deleteShowById(id: Int) {
        showDao.deleteShowById(id)
    }

    // Settings
    suspend fun getSetting(key: String): String? {
        return settingDao.getSettingValue(key)?.value
    }

    fun getSettingFlow(key: String): Flow<Setting?> {
        return settingDao.getSettingValueFlow(key)
    }

    suspend fun saveSetting(key: String, value: String?) {
        settingDao.insertSetting(Setting(key, value))
    }

    // TMDB Search API call
    suspend fun searchShows(query: String): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.searchMulti(apiKey, query)
            response.results
                .filter { it.mediaType == "tv" || it.mediaType == "movie" }
                .map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                val title = if (item.mediaType == "movie") item.title ?: item.name ?: "Unknown Movie" else item.name ?: item.title ?: "Unknown Show"
                val statusType = if (item.mediaType == "movie") "Movie" else ""
                Show(
                    title = title,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = statusType,
                    rating = 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TMDB Details API call
    suspend fun getTvShowDetails(tmdbId: Int): Show? {
        val apiKey = getSetting("tmdb_key") ?: return null
        if (apiKey.isBlank()) return null
        return try {
            val details = tmdbService.getTvDetails(tmdbId, apiKey)
            val posterUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            val seasonsMap = details.seasons
                ?.filter { it.seasonNumber > 0 }
                ?.map { SeasonInfo(number = it.seasonNumber, episodes = it.episodeCount) }
                ?: emptyList()

            Show(
                title = details.name,
                tmdbId = details.id,
                poster = posterUrl,
                status = details.status ?: "",
                rating = details.voteAverage ?: 0.0,
                seasonData = seasonsMap,
                updated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // TMDB Season Episodes API call
    suspend fun getSeasonEpisodes(tmdbId: Int, seasonNum: Int): List<EpisodeInfo>? {
        val apiKey = getSetting("tmdb_key") ?: return null
        if (apiKey.isBlank()) return null
        return try {
            val response = tmdbService.getSeasonDetails(tmdbId, seasonNum, apiKey)
            response.episodes.map { ep ->
                EpisodeInfo(
                    number = ep.episodeNumber,
                    name = ep.name,
                    overview = ep.overview
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // TMDB Trending TV Show Discovery call
    suspend fun getTrendingTv(page: Int = 1): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.getTrendingTv(apiKey, page)
            response.results.map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                Show(
                    title = item.name,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = "Returning Series", // default status
                    rating = item.voteAverage ?: 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TMDB Trending Movie Discovery call
    suspend fun getTrendingMovies(page: Int = 1): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.getTrendingMovies(apiKey, page)
            response.results.map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                Show(
                    title = item.title,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = "Movie",
                    rating = item.voteAverage ?: 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TMDB Movie Details call
    suspend fun getMovieDetails(tmdbId: Int): Show? {
        val apiKey = getSetting("tmdb_key") ?: return null
        if (apiKey.isBlank()) return null
        return try {
            val details = tmdbService.getMovieDetails(tmdbId, apiKey)
            val posterUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
            Show(
                title = details.title,
                tmdbId = details.id,
                poster = posterUrl,
                status = "Movie",
                rating = details.voteAverage ?: 0.0,
                seasonData = emptyList(),
                updated = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // TMDB Upcoming Movies
    suspend fun getUpcomingMovies(page: Int = 1): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.getUpcomingMovies(apiKey, page, "US")
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            response.results.map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                Show(
                    title = item.title,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = "Movie",
                    rating = item.voteAverage ?: 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis(),
                    releaseDate = item.releaseDate
                )
            }.filter { show ->
                // Only include movies that are unreleased or releasing today/future
                show.releaseDate == null || show.releaseDate.isBlank() || show.releaseDate >= todayStr
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TMDB Top Rated Movies
    suspend fun getTopRatedMovies(page: Int = 1): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.getTopRatedMovies(apiKey, page)
            response.results.map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                Show(
                    title = item.title,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = "Movie",
                    rating = item.voteAverage ?: 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TMDB Top Rated TV Series
    suspend fun getTopRatedTv(page: Int = 1): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.getTopRatedTv(apiKey, page)
            response.results.map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                Show(
                    title = item.name,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = "Returning Series",
                    rating = item.voteAverage ?: 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TMDB Popular TV Series
    suspend fun getPopularTv(page: Int = 1): List<Show> {
        val apiKey = getSetting("tmdb_key") ?: return emptyList()
        if (apiKey.isBlank()) return emptyList()
        return try {
            val response = tmdbService.getPopularTv(apiKey, page)
            response.results.map { item ->
                val posterUrl = item.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                Show(
                    title = item.name,
                    tmdbId = item.id,
                    poster = posterUrl,
                    status = "Returning Series",
                    rating = item.voteAverage ?: 0.0,
                    seasonData = emptyList(),
                    updated = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Live and fallback full info details resolver for Discovery card modal selection
    suspend fun getDiscoveryDetail(tmdbId: Int, isMovie: Boolean): DiscoveryDetail {
        val apiKey = getSetting("tmdb_key")
        if (apiKey.isNullOrBlank()) {
            return getMockDiscoveryDetail(tmdbId, isMovie)
        }
        return try {
            if (isMovie) {
                val details = tmdbService.getMovieDetails(tmdbId, apiKey)
                val posterUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                DiscoveryDetail(
                    title = details.title,
                    overview = details.overview ?: "No synopsis available.",
                    rating = details.voteAverage ?: 0.0,
                    dateOrSeason = details.releaseDate ?: "N/A",
                    genres = details.genres?.map { it.name } ?: listOf("Movie"),
                    runtimeOrEpisodes = details.runtime?.let { "$it min" } ?: "N/A",
                    poster = posterUrl,
                    isMovie = true
                )
            } else {
                val details = tmdbService.getTvDetails(tmdbId, apiKey)
                val posterUrl = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                val runtimeStr = details.seasons?.let { seasonsList ->
                    "${seasonsList.size} Seasons (${seasonsList.sumOf { s -> s.episodeCount }} Episodes)"
                } ?: "N/A"
                DiscoveryDetail(
                    title = details.name,
                    overview = details.overview ?: "No synopsis available.",
                    rating = details.voteAverage ?: 0.0,
                    dateOrSeason = details.firstAirDate ?: "N/A",
                    genres = details.genres?.map { it.name } ?: listOf("TV Series"),
                    runtimeOrEpisodes = runtimeStr,
                    poster = posterUrl,
                    isMovie = false
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getMockDiscoveryDetail(tmdbId, isMovie)
        }
    }

    private fun getMockDiscoveryDetail(tmdbId: Int, isMovie: Boolean): DiscoveryDetail {
        return if (isMovie) {
            when (tmdbId) {
                693134 -> DiscoveryDetail(
                    title = "Dune: Part Two",
                    overview = "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while on a path of revenge against the conspirators who destroyed his family.",
                    rating = 8.3,
                    dateOrSeason = "2024-03-01",
                    genres = listOf("Science Fiction", "Adventure"),
                    runtimeOrEpisodes = "166 min",
                    poster = "https://image.tmdb.org/t/p/w500/cz062RhR639g8Y67UV56La6Ucrg.jpg",
                    isMovie = true
                )
                157336 -> DiscoveryDetail(
                    title = "Interstellar",
                    overview = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
                    rating = 8.4,
                    dateOrSeason = "2014-11-05",
                    genres = listOf("Science Fiction", "Drama", "Adventure"),
                    runtimeOrEpisodes = "169 min",
                    poster = "https://image.tmdb.org/t/p/w500/gEU2Yth6uX6vSIGb6Zg7K7stb2R.jpg",
                    isMovie = true
                )
                872585 -> DiscoveryDetail(
                    title = "Oppenheimer",
                    overview = "The story of J. Robert Oppenheimer's role in the development of the atomic bomb during World War II.",
                    rating = 8.1,
                    dateOrSeason = "2023-07-20",
                    genres = listOf("Drama", "History"),
                    runtimeOrEpisodes = "180 min",
                    poster = "https://image.tmdb.org/t/p/w500/8Gxv2gSjdh46g7asv56z0TdXfLq.jpg",
                    isMovie = true
                )
                569094 -> DiscoveryDetail(
                    title = "Spider-Man: Across the Spider-Verse",
                    overview = "Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People charged with protecting its very existence.",
                    rating = 8.4,
                    dateOrSeason = "2023-06-01",
                    genres = listOf("Animation", "Action", "Adventure"),
                    runtimeOrEpisodes = "140 min",
                    poster = "https://image.tmdb.org/t/p/w500/8vtB7p8v6P02vCl68vPRQQDMLvY.jpg",
                    isMovie = true
                )
                else -> DiscoveryDetail(
                    title = "Upcoming Blockbuster",
                    overview = "A highly anticipated feature film. Enable your TMDb API key in Settings to fetch full live synopsis, production details, and release schedules!",
                    rating = 8.0,
                    dateOrSeason = "Coming Soon",
                    genres = listOf("Trending", "Movie"),
                    runtimeOrEpisodes = "TBD min",
                    poster = null,
                    isMovie = true
                )
            }
        } else {
            when (tmdbId) {
                66732 -> DiscoveryDetail(
                    title = "Stranger Things",
                    overview = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
                    rating = 8.6,
                    dateOrSeason = "2016-07-15",
                    genres = listOf("Drama", "Sci-Fi & Fantasy", "Mystery"),
                    runtimeOrEpisodes = "4 Seasons (34 Episodes)",
                    poster = "https://image.tmdb.org/t/p/w500/hx0QD076M9CE7Ge7j4o68g6Ofv9.jpg",
                    isMovie = false
                )
                1399 -> DiscoveryDetail(
                    title = "Breaking Bad",
                    overview = "Walter White, a chemistry teacher, discovers he has cancer and decides to get into the meth-making business to repay his medical debts and secure his family's financial future.",
                    rating = 8.9,
                    dateOrSeason = "2008-01-20",
                    genres = listOf("Drama", "Crime"),
                    runtimeOrEpisodes = "5 Seasons (62 Episodes)",
                    poster = "https://image.tmdb.org/t/p/w500/ztkUQv63U7vXY492z6vTT76S6Gz.jpg",
                    isMovie = false
                )
                100088 -> DiscoveryDetail(
                    title = "The Last of Us",
                    overview = "Twenty years after modern civilization has been destroyed, Joel, a hardened survivor, is hired to steal Ellie, a 14-year-old girl, out of an oppressive quarantine zone.",
                    rating = 8.7,
                    dateOrSeason = "2023-01-15",
                    genres = listOf("Drama", "Sci-Fi & Fantasy", "Action & Adventure"),
                    runtimeOrEpisodes = "1 Season (9 Episodes)",
                    poster = "https://image.tmdb.org/t/p/w500/uKVKS7g9GCM6gCOvC9X7UAsg73q.jpg",
                    isMovie = false
                )
                94605 -> DiscoveryDetail(
                    title = "Arcane",
                    overview = "Set in the steep trust of Piltover and the oppressed underground of Zaun, the story follows the origins of two iconic League of Legends champions-and the power that will tear them apart.",
                    rating = 8.8,
                    dateOrSeason = "2021-11-06",
                    genres = listOf("Animation", "Sci-Fi & Fantasy", "Action & Adventure"),
                    runtimeOrEpisodes = "2 Seasons (18 Episodes)",
                    poster = "https://image.tmdb.org/t/p/w500/fqldcfctv06mfe686f377uq077Z.jpg",
                    isMovie = false
                )
                else -> DiscoveryDetail(
                    title = "Popular Series",
                    overview = "An acclaimed television production. Enable your TMDb API key in Settings to load live multi-season guides, episode layouts, ratings and trailers!",
                    rating = 8.2,
                    dateOrSeason = "On Air",
                    genres = listOf("Trending", "TV Series"),
                    runtimeOrEpisodes = "Multi-Season",
                    poster = null,
                    isMovie = false
                )
            }
        }
    }
}

data class DiscoveryDetail(
    val title: String,
    val overview: String,
    val rating: Double,
    val dateOrSeason: String,
    val genres: List<String>,
    val runtimeOrEpisodes: String,
    val poster: String?,
    val isMovie: Boolean
)

