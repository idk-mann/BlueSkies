package com.example.myapplication2.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object NotificationPrefs {

    private const val KEY_NOTIFICATION_TYPE = "notification_type"
    private const val KEY_NOTIFICATION_HOUR = "notification_hour"
    private const val KEY_NOTIFICATION_MINUTE = "notification_minute"
    private const val KEY_NOTIFICATION_MESSAGE = "notification_message"

    fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    // Save notification settings
    fun saveNotificationSettings(
        context: Context,
        type: String,
        hour: Int,
        minute: Int,
        message: String
    ) {
        getPrefs(context).edit().apply {
            putString(KEY_NOTIFICATION_TYPE, type)
            putInt(KEY_NOTIFICATION_HOUR, hour)
            putInt(KEY_NOTIFICATION_MINUTE, minute)
            putString(KEY_NOTIFICATION_MESSAGE, message)
            apply()
        }
    }

    // Get notification type
    fun getNotificationType(context: Context): String {
        return getPrefs(context).getString(KEY_NOTIFICATION_TYPE, "daily") ?: "daily"
    }

    // Get notification time
    fun getNotificationHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOTIFICATION_HOUR, 8) // default 8 AM
    }

    fun getNotificationMinute(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOTIFICATION_MINUTE, 0)
    }

    // Get notification message
    fun getNotificationMessage(context: Context): String {
        return getPrefs(context).getString(KEY_NOTIFICATION_MESSAGE, "Don't forget to check the weather!") ?: "Don't forget to check the weather!"
    }
}
