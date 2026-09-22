package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.BingeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutomationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "com.example.ACTION_UNDO_AUTOMATION") {
            val showId = intent.getIntExtra("SHOW_ID", -1)
            val previousEpisodeCount = intent.getIntExtra("PREVIOUS_EPISODE_COUNT", -1)
            val previousWatchedEpisodes = intent.getStringExtra("PREVIOUS_WATCHED_EPISODES")
            val previousSeason = intent.getIntExtra("PREVIOUS_SEASON", -1)

            // Cancel notification (ID 1002)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1002)

            if (showId != -1 && previousEpisodeCount != -1 && previousWatchedEpisodes != null) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = BingeRepository(context)
                        val show = repository.getShowById(showId)
                        if (show != null) {
                            val restoredShow = show.copy(
                                season = if (previousSeason != -1) previousSeason else show.season,
                                episode = previousEpisodeCount,
                                watchedEpisodes = previousWatchedEpisodes,
                                updated = System.currentTimeMillis()
                            )
                            repository.saveShow(restoredShow)

                            // Send broadcast so ViewModel updates the active checklist and removes banner
                            val updateIntent = Intent("com.example.ACTION_AUTO_CHECK_COMPLETED").apply {
                                putExtra("SHOW_ID", showId)
                                putExtra("IS_UNDO", true)
                            }
                            context.sendBroadcast(updateIntent)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
