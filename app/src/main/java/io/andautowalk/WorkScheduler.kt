package io.andautowalk

import android.content.Context
import android.util.Log
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
    const val WORK_NAME = "AutoStepSync"

    fun scheduleNextWork(context: Context, forceNextDay: Boolean = false) {
        val settings = SettingsManager(context)
        val now = LocalDateTime.now()
        val delayMinutes = LogicUtils.calculateNextSyncDelay(now, settings.syncTime, forceNextDay)
        val actualNextSync = now.plusMinutes(delayMinutes)
        Log.d("HealthAutoSteps", "Scheduling next work at $actualNextSync (in $delayMinutes minutes)")

        val workRequest = OneTimeWorkRequestBuilder<StepWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true) // User wants to keep this constraint
                    .build()
            )
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace to ensure only one pending task at a time
            workRequest
        )
    }
}
