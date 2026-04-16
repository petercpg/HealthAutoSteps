package io.andautowalk

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalTime

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("health_auto_steps_prefs", Context.MODE_PRIVATE)

    var startTime: LocalTime
        get() = LocalTime.parse(prefs.getString("start_time", "08:00")!!)
        set(value) = prefs.edit().putString("start_time", value.toString()).apply()

    var endTime: LocalTime
        get() = LocalTime.parse(prefs.getString("end_time", "22:00")!!)
        set(value) = prefs.edit().putString("end_time", value.toString()).apply()

    var minSteps: Int
        get() = prefs.getInt("min_steps", 15000)
        set(value) = prefs.edit().putInt("min_steps", value).apply()

    var maxSteps: Int
        get() = prefs.getInt("max_steps", 20000)
        set(value) = prefs.edit().putInt("max_steps", value).apply()

    var syncTime: LocalTime
        get() = LocalTime.parse(prefs.getString("sync_time", "23:00")!!)
        set(value) = prefs.edit().putString("sync_time", value.toString()).apply()
}
