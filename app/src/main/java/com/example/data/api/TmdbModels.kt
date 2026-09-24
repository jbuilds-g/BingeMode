package com.jbuilds.bingemode.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TmdbSearchResponse(
    val results: List<TmdbSearchShow>
)

@JsonClass(generateAdapter = true)
data class TmdbSearchMultiResponse(
    val results: List<TmdbSearchMultiResult>
)

@JsonClass(generateAdapter = true)
data class TmdbSearchMultiResult(
    val id: Int,
    @Json(name = "media_type") val mediaType: String?,
    val name: String? = null,
    val title: String? = null,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbSearchShow(
    val id: Int,
    val name: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "first_air_date") val firstAirDate: String?
)

@JsonClass(generateAdapter = true)
data class TmdbGenre(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class TmdbTvDetails(
    val id: Int,
    val name: String,
    @Json(name = "poster_path") val posterPath: String?,
    val status: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    val seasons: List<TmdbSeason>?,
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    val overview: String? = null,
    val genres: List<TmdbGenre>? = null
)

@JsonClass(generateAdapter = true)
data class TmdbSeason(
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "episode_count") val episodeCount: Int
)

@JsonClass(generateAdapter = true)
data class TmdbSeasonDetailsResponse(
    val episodes: List<TmdbEpisode>
)

@JsonClass(generateAdapter = true)
data class TmdbEpisode(
    @Json(name = "episode_number") val episodeNumber: Int,
    val name: String,
    val overview: String?
)

@JsonClass(generateAdapter = true)
data class TmdbTrendingTvResponse(
    val results: List<TmdbTrendingTvShow>
)

@JsonClass(generateAdapter = true)
data class TmdbTrendingTvShow(
    val id: Int,
    val name: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    val overview: String?
)

@JsonClass(generateAdapter = true)
data class TmdbTrendingMovieResponse(
    val results: List<TmdbTrendingMovie>
)

@JsonClass(generateAdapter = true)
data class TmdbTrendingMovie(
    val id: Int,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    @Json(name = "vote_average") val voteAverage: Double?,
    val overview: String?,
    @Json(name = "release_date") val releaseDate: String? = null
)

@JsonClass(generateAdapter = true)
data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    @Json(name = "poster_path") val posterPath: String?,
    val overview: String?,
    val runtime: Int?,
    @Json(name = "vote_average") val voteAverage: Double?,
    @Json(name = "release_date") val releaseDate: String? = null,
    val genres: List<TmdbGenre>? = null
)
