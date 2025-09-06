package com.example.myapplication2.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.myapplication2.WeatherWorker
import java.util.Calendar

class NotificationReceiver : BroadcastReceiver() {

    private val TAG = "NotificationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("notificationType") ?: "daily"
        val time = intent.getStringExtra("time") // only set for recurring alarms

        Log.d(TAG, "Received alarm for: $type (time=$time)")

        // Pass type + time into your Worker
        val workRequest = OneTimeWorkRequestBuilder<WeatherWorker>()
            .setInputData(
                workDataOf(
                    "notificationType" to type,
                    "time" to (time ?: "")
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)

        // 🔹 Only reschedule fixed alarms (daily/weekly),
        //    NOT the user-defined recurring list
        if (type == "daily" || type == "weekly") {
            rescheduleNextFixedAlarm(context, type)
        }
    }

    /**
     * Reschedules the next daily/weekly alarm using NotificationPrefs.
     * Recurring alarms are handled in the Activity with scheduleRecurringNotification().
     */
    private fun rescheduleNextFixedAlarm(context: Context, notificationType: String) {
        val hour = NotificationPrefs.getNotificationHour(context)
        val minute = NotificationPrefs.getNotificationMinute(context)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (notificationType == "weekly") {
                add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("notificationType", notificationType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationType.hashCode(),
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "Next $notificationType alarm scheduled for: ${calendar.time}")
    }
}
