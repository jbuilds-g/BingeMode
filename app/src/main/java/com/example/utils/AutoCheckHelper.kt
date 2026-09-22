package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.Show
import com.example.data.repository.BingeRepository
import com.example.receiver.AutoCheckAlarmReceiver
import com.example.receiver.AutomationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AutoCheckHelper {

    private const val CHANNEL_ID = "automation_channel"
    private const val CHANNEL_NAME = "Automation & Tracking"

    /**
     * Parses the trigger days from show configuration into a set of ISO days (1=Mon, 7=Sun).
     */
    fun getTargetDays(show: Show): Set<Int> {
        return when (show.autoCheckType) {
            "daily" -> setOf(1, 2, 3, 4, 5, 6, 7)
            "weekdays" -> setOf(1, 2, 3, 4, 5)
            "weekends" -> setOf(6, 7)
            "custom_days", "schedule" -> {
                if (show.autoCheckDays.isNotBlank()) {
                    show.autoCheckDays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..7 }
                        .toSet()
                        .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
                } else {
                    setOf(1, 2, 3, 4, 5, 6, 7)
                }
            }
            "weekly" -> {
                if (show.autoCheckDays.isNotBlank()) {
                    show.autoCheckDays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..7 }
                        .take(1)
                        .toSet()
                        .ifEmpty { setOf(1) }
                } else {
                    setOf(1)
                }
            }
            else -> {
                if (show.autoCheckDays.isNotBlank()) {
                    show.autoCheckDays.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 1..7 }
                        .toSet()
                        .ifEmpty { setOf(1, 2, 3, 4, 5, 6, 7) }
                } else {
                    setOf(1, 2, 3, 4, 5, 6, 7)
                }
            }
        }
    }

    /**
     * Converts a Java Calendar day of week to ISO-8601 day of week (1=Monday, ..., 7=Sunday).
     */
    private fun calendarDayToIso(calDay: Int): Int {
        return when (calDay) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    /**
     * Finds the next scheduled trigger time in milliseconds for the given show.
     */
    fun getNextScheduledTime(show: Show, fromMs: Long = System.currentTimeMillis()): Long? {
        if (!show.autoCheckEnabled) return null
        if (EpisodeTracker.isEntireShowCompleted(show)) return null

        val targetDays = getTargetDays(show)
        if (targetDays.isEmpty()) return null

        val timeParts = show.autoCheckTime.split(":")
        val targetHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
        val targetMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            timeInMillis = fromMs
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Search through the next 14 days for the nearest matching slot
        for (i in 0..14) {
            val slotTime = cal.timeInMillis
            val isoDay = calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))

            if (slotTime > fromMs && targetDays.contains(isoDay)) {
                return slotTime
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return null
    }

    /**
     * Formats the next scheduled trigger time into a friendly string (e.g. "Today at 8:00 PM").
     */
    fun formatNextScheduledTime(show: Show, fromMs: Long = System.currentTimeMillis(), use24Hour: Boolean = false): String {
        if (EpisodeTracker.isEntireShowCompleted(show)) {
            return "All episodes watched"
        }
        if (!show.autoCheckEnabled) {
            return "Schedule inactive"
        }

        val nextTime = getNextScheduledTime(show, fromMs) ?: return "No upcoming schedule"

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextTime }
        val nowCal = Calendar.getInstance().apply { timeInMillis = fromMs }

        val h = nextCal.get(Calendar.HOUR_OF_DAY)
        val m = nextCal.get(Calendar.MINUTE)

        val timeFormatted = if (use24Hour) {
            String.format(Locale.US, "%02d:%02d", h, m)
        } else {
            val amPm = if (h >= 12) "PM" else "AM"
            val displayH = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format(Locale.US, "%d:%02d %s", displayH, m, amPm)
        }

        val isToday = nextCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                nextCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val tomorrowCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = nextCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                nextCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        return when {
            isToday -> "Today at $timeFormatted"
            isTomorrow -> "Tomorrow at $timeFormatted"
            else -> {
                val dayName = SimpleDateFormat("EEEE", Locale.US).format(Date(nextTime))
                "$dayName at $timeFormatted"
            }
        }
    }

    /**
     * Schedules an AlarmManager alarm for the next occurrence of this show's auto-check.
     */
    fun scheduleNextAlarm(context: Context, show: Show) {
        if (!show.autoCheckEnabled || EpisodeTracker.isEntireShowCompleted(show)) {
            cancelAlarm(context, show.id)
            return
        }

        val nextTime = getNextScheduledTime(show) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AutoCheckAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_AUTO_CHECK"
            putExtra("SHOW_ID", show.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            show.id + 10000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback non-exact
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, pendingIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * Cancels any active alarm for this show.
     */
    fun cancelAlarm(context: Context, showId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AutoCheckAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_TRIGGER_AUTO_CHECK"
            putExtra("SHOW_ID", showId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            showId + 10000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Evaluates if any scheduled triggers have occurred since lastRun.
     * Returns Pair(episodesToMark, newLastRunTimestamp).
     */
    fun evaluateAutoCheck(show: Show, nowMs: Long): Pair<Int, Long> {
        if (!show.autoCheckEnabled || EpisodeTracker.isEntireShowCompleted(show)) {
            return Pair(0, show.autoCheckLastRun)
        }

        // If never run, initialize baseline to now so past history is not marked retroactively
        if (show.autoCheckLastRun <= 0L) {
            return Pair(0, nowMs)
        }

        val lastRun = show.autoCheckLastRun
        if (lastRun >= nowMs) return Pair(0, lastRun)

        val targetDays = getTargetDays(show)
        if (targetDays.isEmpty()) return Pair(0, lastRun)

        val timeParts = show.autoCheckTime.split(":")
        val targetHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 20
        val targetMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            timeInMillis = lastRun
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If cal is before or equal to lastRun, step forward to the next day
        if (cal.timeInMillis <= lastRun) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        var slotsTriggered = 0
        var daysScanned = 0
        while (cal.timeInMillis <= nowMs && daysScanned < 30) {
            val isoDay = calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
            if (targetDays.contains(isoDay)) {
                slotsTriggered++
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            daysScanned++
        }

        if (slotsTriggered == 0) {
            return Pair(0, lastRun)
        }

        // Cap episodes to mark (max 5) to prevent unbounded batch progress
        val perSlot = if (show.autoCheckCount > 0) show.autoCheckCount else 1
        val episodesToMark = minOf(slotsTriggered * perSlot, 5)

        return Pair(episodesToMark, nowMs)
    }

    /**
     * Executes the episode check, preserves all seasons, updates DB, and sends notification.
     */
    suspend fun processAutoCheck(
        context: Context,
        repository: BingeRepository,
        showId: Int,
        addEps: Int,
        nextLastRun: Long,
        tmdbApiKey: String
    ): Boolean {
        val currentShow = repository.getShowById(showId) ?: return false

        // Guard: check if show is already completed
        if (EpisodeTracker.isEntireShowCompleted(currentShow)) {
            val completedShow = currentShow.copy(
                autoCheckEnabled = false,
                autoCheckLastRun = nextLastRun,
                updated = System.currentTimeMillis()
            )
            repository.saveShow(completedShow)
            cancelAlarm(context, showId)
            return false
        }

        val isMovie = currentShow.status == "Movie"
        val previousEpisode = currentShow.episode
        val previousWatchedEpisodes = currentShow.watchedEpisodes
        val previousSeason = currentShow.season

        val updatedShow: Show
        val targetEpisodeNum: Int
        val targetSeasonNum: Int

        if (isMovie) {
            targetEpisodeNum = 1
            targetSeasonNum = 1
            updatedShow = currentShow.copy(
                episode = 1,
                watchedEpisodes = "1",
                autoCheckEnabled = false, // Movie is finished!
                autoCheckLastRun = nextLastRun,
                updated = System.currentTimeMillis()
            )
        } else {
            var curSeason = currentShow.season
            var remaining = addEps
            var workingWatched = currentShow.watchedEpisodes
            var seasonsList = currentShow.seasonData
            var lastMarkedEp = 0

            while (remaining > 0) {
                val seasonObj = seasonsList.find { it.number == curSeason }
                val maxEpsInCurSeason = seasonObj?.episodes ?: maxOf(currentShow.episode, 10)
                val watchedInCurSeason = EpisodeTracker.getWatchedEpisodesForSeason(workingWatched, curSeason)

                // Find next unwatched episode in this season
                var nextUnwatched = (1..maxEpsInCurSeason).firstOrNull { !watchedInCurSeason.contains(it) }

                if (nextUnwatched != null) {
                    // Mark this episode
                    val newWatchedSet = watchedInCurSeason + nextUnwatched
                    workingWatched = EpisodeTracker.setWatchedEpisodesForSeason(workingWatched, curSeason, newWatchedSet)
                    lastMarkedEp = nextUnwatched
                    remaining--
                } else {
                    // Current season is completely watched! Check for next season
                    val nextSeasonNum = curSeason + 1
                    val nextSeasonObj = seasonsList.find { it.number == nextSeasonNum }

                    if (nextSeasonObj != null) {
                        // Advance to next season seamlessly without wiping the previous season!
                        curSeason = nextSeasonNum
                        // Mark episode 1 of next season
                        val nextSeasonWatched = setOf(1)
                        workingWatched = EpisodeTracker.setWatchedEpisodesForSeason(workingWatched, curSeason, nextSeasonWatched)
                        lastMarkedEp = 1
                        remaining--

                        // Auto-fetch episode names for the new season if TMDb key available
                        val tmdbId = currentShow.tmdbId
                        if (tmdbId != null && tmdbApiKey.isNotBlank() && nextSeasonObj.episodeList.isNullOrEmpty()) {
                            try {
                                val epList = repository.getSeasonEpisodes(tmdbId, nextSeasonNum)
                                if (epList != null) {
                                    seasonsList = seasonsList.map { s ->
                                        if (s.number == nextSeasonNum) s.copy(episodeList = epList) else s
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } else {
                        // No more seasons available - series is completely finished!
                        break
                    }
                }
            }

            targetSeasonNum = curSeason
            val currentSeasonWatched = EpisodeTracker.getWatchedEpisodesForSeason(workingWatched, targetSeasonNum)
            targetEpisodeNum = currentSeasonWatched.maxOrNull() ?: lastMarkedEp

            val isNowCompleted = seasonsList.isNotEmpty() && seasonsList.all { s ->
                val w = EpisodeTracker.getWatchedEpisodesForSeason(workingWatched, s.number)
                s.episodes > 0 && w.size >= s.episodes
            }

            updatedShow = currentShow.copy(
                season = targetSeasonNum,
                episode = targetEpisodeNum,
                watchedEpisodes = workingWatched,
                seasonData = seasonsList,
                autoCheckEnabled = if (isNowCompleted) false else currentShow.autoCheckEnabled,
                autoCheckLastRun = nextLastRun,
                updated = System.currentTimeMillis()
            )
        }

        repository.saveShow(updatedShow)

        // Schedule next alarm or cancel if finished
        if (updatedShow.autoCheckEnabled) {
            scheduleNextAlarm(context, updatedShow)
        } else {
            cancelAlarm(context, showId)
        }

        // Post Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val undoIntent = Intent(context, AutomationReceiver::class.java).apply {
            action = "com.example.ACTION_UNDO_AUTOMATION"
            putExtra("SHOW_ID", currentShow.id)
            putExtra("PREVIOUS_SEASON", previousSeason)
            putExtra("PREVIOUS_EPISODE_COUNT", previousEpisode)
            putExtra("PREVIOUS_WATCHED_EPISODES", previousWatchedEpisodes)
        }

        val undoPendingIntent = PendingIntent.getBroadcast(
            context,
            currentShow.id + 20000,
            undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationTitle = "Episode Marked as Watched"
        val bodyText = if (isMovie) {
            "${currentShow.title} marked as watched"
        } else {
            "${currentShow.title} [S${targetSeasonNum}:E${targetEpisodeNum}] marked as watched"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(bodyText)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_revert, "Undo", undoPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1002, notification)

        // Broadcast to update UI and trigger in-app banner
        val updateIntent = Intent("com.example.ACTION_AUTO_CHECK_COMPLETED").apply {
            putExtra("SHOW_ID", currentShow.id)
            putExtra("SEASON", targetSeasonNum)
            putExtra("EPISODE", targetEpisodeNum)
            putExtra("PREVIOUS_SEASON", previousSeason)
            putExtra("PREVIOUS_EPISODES", previousWatchedEpisodes)
            putExtra("PREVIOUS_EPISODE_COUNT", previousEpisode)
        }
        context.sendBroadcast(updateIntent)

        return true
    }

    /**
     * Checks all shows in repository and triggers any due auto-checks.
     */
    suspend fun checkAllShows(context: Context, repository: BingeRepository, tmdbApiKey: String): Int {
        val shows = repository.getAllShowsList()
        val nowMs = System.currentTimeMillis()
        var triggeredCount = 0

        for (show in shows) {
            if (show.autoCheckEnabled) {
                val (addEps, nextLastRun) = evaluateAutoCheck(show, nowMs)
                if (addEps > 0) {
                    val success = processAutoCheck(
                        context = context,
                        repository = repository,
                        showId = show.id,
                        addEps = addEps,
                        nextLastRun = nextLastRun,
                        tmdbApiKey = tmdbApiKey
                    )
                    if (success) triggeredCount++
                } else if (show.autoCheckLastRun <= 0L) {
                    val fresh = repository.getShowById(show.id)
                    if (fresh != null) {
                        repository.saveShow(fresh.copy(autoCheckLastRun = nextLastRun))
                    }
                }
                // Ensure alarm is scheduled
                val fresh = repository.getShowById(show.id)
                if (fresh != null && fresh.autoCheckEnabled) {
                    scheduleNextAlarm(context, fresh)
                }
            }
        }
        return triggeredCount
    }
}
