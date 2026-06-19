package com.example.biometricattendanceapp.feature_attendance.presentation.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    fun schedulePunchOutReminder(closingTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, PunchOutReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100, // Request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Parse the time (e.g., "18:00")
            val parts = closingTime.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            // Calculate the exact trigger time
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, -10) // Subtract 10 minutes
            }

            // If the time has already passed today, don't trigger immediately.
            // Schedule it for tomorrow instead.
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Set the exact alarm. RTC_WAKEUP wakes up the device screen if it's asleep.
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Optional: Call this when they successfully punch out so they don't get the reminder anyway
    fun cancelReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, PunchOutReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}