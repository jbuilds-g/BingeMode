package com.jbuilds.bingemode

import com.jbuilds.bingemode.data.model.Show
import com.jbuilds.bingemode.utils.AutoCheckHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class AutoCheckHelperTest {

    @Test
    fun testAutoCheckCapsCatchUpAtFiveEpisodes() {
        val base = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 1, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val show = Show(
            title = "Test",
            autoCheckEnabled = true,
            autoCheckDays = "1,2,3,4,5,6,7",
            autoCheckType = "daily",
            autoCheckCount = 2,
            autoCheckLastRun = base.timeInMillis
        )

        val now = Calendar.getInstance().apply {
            timeInMillis = base.timeInMillis
            add(Calendar.DAY_OF_YEAR, 10)
        }.timeInMillis

        val (episodes, nextRun) = AutoCheckHelper.evaluateAutoCheck(show, now)

        assertEquals(5, episodes)
        assertEquals(now, nextRun)
    }

    @Test
    fun testAutoCheckDoesNotRunForCompletedShow() {
        val show = Show(
            title = "Test",
            seasonData = listOf(com.jbuilds.bingemode.data.model.SeasonInfo(1, 2)),
            watchedEpisodes = "s1:1,2",
            autoCheckEnabled = true,
            autoCheckLastRun = 1L
        )

        val (episodes, nextRun) = AutoCheckHelper.evaluateAutoCheck(show, 100_000L)

        assertEquals(0, episodes)
        assertEquals(1L, nextRun)
    }
}
