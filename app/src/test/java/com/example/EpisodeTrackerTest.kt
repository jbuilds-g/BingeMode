package com.example

import com.example.data.model.Show
import com.example.utils.EpisodeTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeTrackerTest {

    @Test
    fun testLegacyFormatCompatibility() {
        val legacyWatched = "1,2,3"
        val season1Watched = EpisodeTracker.getWatchedEpisodesForSeason(legacyWatched, 1)
        val season2Watched = EpisodeTracker.getWatchedEpisodesForSeason(legacyWatched, 2)

        assertEquals(setOf(1, 2, 3), season1Watched)
        assertTrue(season2Watched.isEmpty())
    }

    @Test
    fun testMultiSeasonTracking() {
        // Toggle episode 1 and 2 in season 1
        var (watchedStr, setS1) = EpisodeTracker.toggleEpisode("", 1, 1)
        var res = EpisodeTracker.toggleEpisode(watchedStr, 1, 2)
        watchedStr = res.first

        assertEquals(setOf(1, 2), EpisodeTracker.getWatchedEpisodesForSeason(watchedStr, 1))
        assertTrue(EpisodeTracker.getWatchedEpisodesForSeason(watchedStr, 2).isEmpty())

        // Now toggle episode 1 in season 2
        res = EpisodeTracker.toggleEpisode(watchedStr, 2, 1)
        watchedStr = res.first

        // Both seasons should retain their progress!
        assertEquals(setOf(1, 2), EpisodeTracker.getWatchedEpisodesForSeason(watchedStr, 1))
        assertEquals(setOf(1), EpisodeTracker.getWatchedEpisodesForSeason(watchedStr, 2))
    }

    @Test
    fun testSequentialAutoFill() {
        // Mark up to episode 4 in season 1
        val (watchedStr, set) = EpisodeTracker.setSequentialProgress("", 1, 4)
        assertEquals(setOf(1, 2, 3, 4), set)

        // Then mark up to episode 3 in season 2
        val (multiStr, setS2) = EpisodeTracker.setSequentialProgress(watchedStr, 2, 3)
        assertEquals(setOf(1, 2, 3), setS2)
        assertEquals(setOf(1, 2, 3, 4), EpisodeTracker.getWatchedEpisodesForSeason(multiStr, 1))
    }

    @Test
    fun testUncheckingEpisode() {
        var (watchedStr, _) = EpisodeTracker.setSequentialProgress("", 1, 3)
        assertTrue(EpisodeTracker.getWatchedEpisodesForSeason(watchedStr, 1).contains(2))

        val (uncheckStr, uncheckSet) = EpisodeTracker.toggleEpisode(watchedStr, 1, 2)
        assertFalse(uncheckSet.contains(2))
        assertTrue(uncheckSet.contains(1))
        assertTrue(uncheckSet.contains(3))
        assertEquals(setOf(1, 3), EpisodeTracker.getWatchedEpisodesForSeason(uncheckStr, 1))
    }
    @Test
    fun testLegacyFormatMapsToSeasonOne() {
        val raw = "1,3,5"
        assertEquals(setOf(1, 3, 5), EpisodeTracker.getAllSeasonsWatchedMap(raw, 3)[1])
        assertTrue(EpisodeTracker.getWatchedEpisodesForSeason(raw, 3).isEmpty())
    }

    @Test
    fun testSequentialProgressPreservesNonContiguousEpisodes() {
        val raw = EpisodeTracker.serializeMap(mapOf(1 to setOf(1, 3, 5)))
        val (updated, watched) = EpisodeTracker.setSequentialProgress(raw, 1, 3)

        assertEquals(setOf(1, 2, 3, 5), watched)
        assertEquals(setOf(1, 2, 3, 5), EpisodeTracker.getWatchedEpisodesForSeason(updated, 1))
    }

    @Test
    fun testCompletionRequiresEveryEpisode() {
        val incomplete = Show(
            title = "Test",
            seasonData = listOf(com.example.data.model.SeasonInfo(1, 3)),
            watchedEpisodes = EpisodeTracker.serializeMap(mapOf(1 to setOf(1, 3)))
        )
        val complete = incomplete.copy(
            watchedEpisodes = EpisodeTracker.serializeMap(mapOf(1 to setOf(1, 2, 3)))
        )

        assertFalse(EpisodeTracker.isEntireShowCompleted(incomplete))
        assertTrue(EpisodeTracker.isEntireShowCompleted(complete))
    }

}

