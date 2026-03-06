package com.example.healthautosteps

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.random.Random

class StepWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("HealthAutoSteps", "StepWorker starting...")
        val healthConnectManager = HealthConnectManager(applicationContext)

        if (!healthConnectManager.hasAllPermissions()) {
            Log.w("HealthAutoSteps", "StepWorker missing permissions, retrying...")
            return Result.retry()
        }

        val settings = SettingsManager(applicationContext)

        val minSteps = settings.minSteps
        val maxSteps = kotlin.math.max(minSteps, settings.maxSteps)
        val totalSteps = Random.nextLong(minSteps.toLong(), maxSteps.toLong() + 1)

        val now = LocalDateTime.now()
        val startTime = now.withHour(settings.startTime.hour).withMinute(settings.startTime.minute).withSecond(0).withNano(0)
        var endTime = now.withHour(settings.endTime.hour).withMinute(settings.endTime.minute).withSecond(0).withNano(0)

        if (endTime.isBefore(startTime)) {
            endTime = endTime.plusDays(1)
        }

        val startInstant = startTime.atZone(ZoneId.systemDefault()).toInstant()
        val endInstant = endTime.atZone(ZoneId.systemDefault()).toInstant()

        return try {
            healthConnectManager.writeDistributedSteps(totalSteps, startInstant, endInstant)
            Log.d("HealthAutoSteps", "StepWorker success: wrote $totalSteps steps between $startTime and $endTime")
            Result.success()
        } catch (e: Exception) {
            Log.e("HealthAutoSteps", "StepWorker failed", e)
            Result.failure()
        }
    }
}
