package com.jbuilds.bingemode.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jbuilds.bingemode.data.repository.BingeRepository
import com.jbuilds.bingemode.utils.AutoCheckHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoCheckAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val showId = intent.getIntExtra("SHOW_ID", -1)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = BingeRepository(context)
                val tmdbApiKey = repository.getSetting("tmdb_key") ?: ""

                if (showId != -1) {
                    val show = repository.getShowById(showId)
                    if (show != null && show.autoCheckEnabled) {
                        val (addEps, nextLastRun) = AutoCheckHelper.evaluateAutoCheck(show, System.currentTimeMillis())
                        if (addEps > 0) {
                            AutoCheckHelper.processAutoCheck(
                                context = context,
                                repository = repository,
                                showId = show.id,
                                addEps = addEps,
                                nextLastRun = nextLastRun,
                                tmdbApiKey = tmdbApiKey
                            )
                        } else if (nextLastRun != show.autoCheckLastRun) {
                            repository.saveShow(show.copy(autoCheckLastRun = nextLastRun))
                        }
                        // Re-schedule alarm for this show's next trigger
                        val refreshed = repository.getShowById(show.id)
                        if (refreshed != null && refreshed.autoCheckEnabled) {
                            AutoCheckHelper.scheduleNextAlarm(context, refreshed)
                        }
                    }
                } else {
                    // Check all shows
                    AutoCheckHelper.checkAllShows(context, repository, tmdbApiKey)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
