package io.andautowalk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
            Log.d("HealthAutoSteps", "StepWorker executing: Write $totalSteps steps from $startInstant to $endInstant (Settings: min=$minSteps, max=$maxSteps)")
            healthConnectManager.writeDistributedSteps(totalSteps, startInstant, endInstant)
            Log.d("HealthAutoSteps", "StepWorker success: wrote $totalSteps steps")
            showCompletionNotification(totalSteps, startTime, endTime)
            
            // Reschedule the next day's work to avoid looping if syncTime is current time
            WorkScheduler.scheduleNextWork(applicationContext, forceNextDay = true)
            
            Result.success()
        } catch (e: Exception) {
            Log.e("HealthAutoSteps", "StepWorker failed", e)
            Result.failure()
        }
    }

    private fun showCompletionNotification(totalSteps: Long, startTime: LocalDateTime, endTime: LocalDateTime) {
        val channelId = "health_auto_steps_completion"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "同步完成", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val content = "已自動寫入 $totalSteps 步 (區間: ${startTime.format(formatter)} - ${endTime.format(formatter)})"

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("步數同步完成")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1002, notification)
    }
}
