package com.jbuilds.bingemode

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jbuilds.bingemode.data.repository.BingeRepository
import com.jbuilds.bingemode.utils.AutoCheckHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = BingeRepository(applicationContext)
            val tmdbApiKey = repository.getSetting("tmdb_key") ?: ""
            AutoCheckHelper.checkAllShows(applicationContext, repository, tmdbApiKey)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
