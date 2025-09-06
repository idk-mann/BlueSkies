package com.example.myapplication2.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class DailyNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmTag = intent.getStringExtra("alarm_tag") ?: "Daily Alarm"

        val notification = NotificationCompat.Builder(context, "daily_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Reminder")
            .setContentText("It's time! ($alarmTag)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(alarmTag.hashCode(), notification)
        }
    }
}
