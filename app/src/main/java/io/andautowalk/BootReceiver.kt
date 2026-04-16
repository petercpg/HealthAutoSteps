package io.andautowalk

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
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            
            Log.d("HealthAutoSteps", "Recovery event triggered: $action. Rescheduling service...")

            WorkScheduler.scheduleNextWork(context)
            showBootNotification(context, action)
        }
    }

    private fun showBootNotification(context: Context, action: String?) {
        val channelId = "health_auto_steps_service"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Updated to IMPORTANCE_DEFAULT to be more noticeable
        val channel = NotificationChannel(channelId, "服務狀態", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)

        val message = if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            "套件更新後服務已恢復"
        } else {
            "開機後服務已恢復啟動"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Health Auto Steps")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
