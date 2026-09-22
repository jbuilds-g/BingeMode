package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shows")
data class Show(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val tmdbId: Int? = null,
    val mediaType: String = "tv",
    val poster: String? = null,
    val status: String? = null,
    val rating: Double = 0.0,
    val seasonData: List<SeasonInfo> = emptyList(),
    val season: Int = 1,
    val episode: Int = 0,
    val updated: Long = System.currentTimeMillis(),
    val releaseDate: String? = null,
    val autoCheckEnabled: Boolean = false,
    val autoCheckDays: String = "",
    val autoCheckTime: String = "20:00",
    val autoCheckType: String = "daily",
    val autoCheckCount: Int = 1,
    val autoCheckLastRun: Long = 0L,
    val watchedEpisodes: String = ""
)

data class SeasonInfo(
    val number: Int,
    val episodes: Int,
    val episodeList: List<EpisodeInfo>? = null
)

data class EpisodeInfo(
    val number: Int,
    val name: String,
    val overview: String? = null
)

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val key: String,
    val value: String?
)
