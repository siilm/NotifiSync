package com.deaworks.notifisync

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.deaworks.notifisync.NotifiListener.Companion.KEY_SERVICE_RUNNING
import com.deaworks.notifisync.NotifiListener.Companion.PREFS_NAME
import com.deaworks.notifisync.util.isConfigEmpty

class ServiceDaemon(private val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            if (prefs.getBoolean(KEY_SERVICE_RUNNING, false)) {
                if (!isConfigEmpty(context)) {
                    NotifiListener.startService(context)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("ServiceDaemon", "Restart failed", e)
            Result.retry()
        }
    }
}
