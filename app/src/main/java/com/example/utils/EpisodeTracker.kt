package com.example.utils

import com.example.data.model.Show

/**
 * Robust utility for managing watched episode state with per-season preservation.
 * Backwards compatible with legacy single-string comma-separated format ("1,2,3"),
 * while migrating seamlessly to multi-season format ("s1:1,2,3|s2:1,2").
 */
object EpisodeTracker {

    /**
     * Parses the watched episode numbers for a specific season.
     */
    fun getWatchedEpisodesForSeason(raw: String, seasonNum: Int): Set<Int> {
        if (raw.isBlank()) return emptySet()

        // Check if multi-season format is used (contains "s" and ":")
        if (raw.contains("s") && raw.contains(":")) {
            val entries = raw.split("|", ";").map { it.trim() }.filter { it.isNotEmpty() }
            for (entry in entries) {
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val entrySeason = parts[0].removePrefix("s").removePrefix("S").toIntOrNull()
                    if (entrySeason == seasonNum) {
                        return parts[1].split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .toSet()
                    }
                }
            }
            return emptySet()
        }

        // Legacy format: comma-separated list applies strictly to season 1
        if (seasonNum == 1) {
            return raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        }
        return emptySet()
    }

    /**
     * Parses all watched episodes across all seasons into a map of Season -> Set of Episode Numbers.
     */
    fun getAllSeasonsWatchedMap(raw: String, currentSeason: Int): Map<Int, Set<Int>> {
        val resultMap = mutableMapOf<Int, MutableSet<Int>>()
        if (raw.isBlank()) return resultMap

        if (raw.contains("s") && raw.contains(":")) {
            val entries = raw.split("|", ";").map { it.trim() }.filter { it.isNotEmpty() }
            for (entry in entries) {
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val sNum = parts[0].removePrefix("s").removePrefix("S").toIntOrNull() ?: continue
                    val eps = parts[1].split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .toMutableSet()
                    resultMap[sNum] = eps
                }
            }
        } else {
            // Legacy format: map to the current active season
            val legacyEps = raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .toMutableSet()
            if (legacyEps.isNotEmpty()) {
                resultMap[currentSeason] = legacyEps
            }
        }

        return resultMap
    }

    /**
     * Updates the watched episode set for a specific season and encodes into multi-season string.
     */
    fun setWatchedEpisodesForSeason(raw: String, seasonNum: Int, watched: Set<Int>): String {
        val map = getAllSeasonsWatchedMap(raw, seasonNum).toMutableMap()
        if (watched.isEmpty()) {
            map.remove(seasonNum)
        } else {
            map[seasonNum] = watched.toMutableSet()
        }

        return serializeMap(map)
    }

    /**
     * Toggles an individual episode's watched status for a season.
     * Returns Pair(updatedRawString, updatedSetForSeason).
     */
    fun toggleEpisode(raw: String, seasonNum: Int, episodeNum: Int): Pair<String, Set<Int>> {
        val currentSet = getWatchedEpisodesForSeason(raw, seasonNum).toMutableSet()
        if (currentSet.contains(episodeNum)) {
            currentSet.remove(episodeNum)
        } else {
            currentSet.add(episodeNum)
        }

        val updatedRaw = setWatchedEpisodesForSeason(raw, seasonNum, currentSet)
        return Pair(updatedRaw, currentSet)
    }

    /**
     * Sets sequential watched episodes up to [targetEpisodeNum] for a season.
     * If already at target, unmarks the last episode.
     */
    fun setSequentialProgress(raw: String, seasonNum: Int, targetEpisodeNum: Int): Pair<String, Set<Int>> {
        val currentSet = getWatchedEpisodesForSeason(raw, seasonNum)
        val currentMax = currentSet.maxOrNull() ?: 0

        val newSet: Set<Int> = if (currentMax == targetEpisodeNum) {
            // Clicking current top unmarks it
            if (targetEpisodeNum > 1) (1 until targetEpisodeNum).toSet() else emptySet()
        } else {
            (1..targetEpisodeNum).toSet()
        }

        val updatedRaw = setWatchedEpisodesForSeason(raw, seasonNum, newSet)
        return Pair(updatedRaw, newSet)
    }

    /**
     * Marks all episodes for a season as watched.
     */
    fun markSeasonAll(raw: String, seasonNum: Int, totalEpisodes: Int): Pair<String, Set<Int>> {
        val allEps = if (totalEpisodes > 0) (1..totalEpisodes).toSet() else emptySet()
        val updatedRaw = setWatchedEpisodesForSeason(raw, seasonNum, allEps)
        return Pair(updatedRaw, allEps)
    }

    /**
     * Clears all watched episodes for a specific season.
     */
    fun clearSeason(raw: String, seasonNum: Int): String {
        return setWatchedEpisodesForSeason(raw, seasonNum, emptySet())
    }

    /**
     * Serializes season map into "s1:1,2,3|s2:1,2" format.
     */
    fun serializeMap(map: Map<Int, Set<Int>>): String {
        if (map.isEmpty()) return ""
        return map.entries
            .sortedBy { it.key }
            .filter { it.value.isNotEmpty() }
            .joinToString("|") { (season, eps) ->
                "s$season:${eps.sorted().joinToString(",")}"
            }
    }

    /**
     * Total episodes watched across all seasons in this show.
     */
    fun getTotalWatchedCount(show: Show): Int {
        if (show.status == "Movie") {
            return if (show.episode >= 1) 1 else 0
        }
        val map = getAllSeasonsWatchedMap(show.watchedEpisodes, show.season)
        return map.values.sumOf { it.size }
    }

    /**
     * Returns true if the entire show (all seasons or movie) is completely watched.
     */
    fun isEntireShowCompleted(show: Show): Boolean {
        if (show.status == "Movie") {
            return show.episode >= 1 || show.watchedEpisodes.isNotBlank()
        }
        if (show.seasonData.isEmpty()) {
            return false
        }
        val map = getAllSeasonsWatchedMap(show.watchedEpisodes, show.season)
        return show.seasonData.all { seasonInfo ->
            val watched = map[seasonInfo.number] ?: emptySet()
            seasonInfo.episodes > 0 && watched.size >= seasonInfo.episodes
        }
    }
}
