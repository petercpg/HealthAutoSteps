package com.example.healthautosteps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("HealthAutoSteps", "Boot completed, rescheduling service...")

            val settings = SettingsManager(context)
            rescheduleWork(context, settings)
            showBootNotification(context)
        }
    }

    private fun rescheduleWork(context: Context, settings: SettingsManager) {
        val now = LocalDateTime.now()
        var nextSync = now.withHour(settings.syncTime.hour).withMinute(settings.syncTime.minute).withSecond(0).withNano(0)
        if (now.isAfter(nextSync) || now.isEqual(nextSync)) {
            nextSync = nextSync.plusDays(1)
        }
        val delayMinutes = Duration.between(now, nextSync).toMinutes()

        val workRequest = PeriodicWorkRequestBuilder<StepWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "AutoStepSync",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun showBootNotification(context: Context) {
        val channelId = "health_auto_steps_service"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "服務狀態", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Health Auto Steps")
            .setContentText("服務已於開機後重新啟動")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
