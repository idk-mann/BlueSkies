package com.example.myapplication2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.example.myapplication2.databinding.ActivitySettingsBinding
import com.example.myapplication2.notifications.NotificationPrefs
import com.example.myapplication2.notifications.NotificationReceiver
import com.example.myapplication2.notifications.NotificationSettingsActivity
import com.example.myapplication2.notifications.RecurringAlarmsActivity
import com.google.android.material.materialswitch.MaterialSwitch
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val radioGroup = binding.detailSelector

        // Detail level
        when (prefs.getString("detail_level", "All")) {
            "All" -> radioGroup.check(R.id.showAll)
            "Basic" -> radioGroup.check(R.id.showBasic)
            "Advanced" -> radioGroup.check(R.id.showAdvanced)
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.showAll -> "All"
                R.id.showBasic -> "Basic"
                R.id.showAdvanced -> "Advanced"
                else -> "All"
            }
            prefs.edit().putString("detail_level", selected).apply()
        }

        // View range
        val rangeGroup = binding.rangeGroup
        val savedRange = prefs.getInt("hour_range", 25)
        when (savedRange) {
            25 -> rangeGroup.check(R.id.range_1day)
            73 -> rangeGroup.check(R.id.range_3day)
            169 -> rangeGroup.check(R.id.range_7day)
            else -> rangeGroup.check(R.id.range_1day)
        }
        rangeGroup.setOnCheckedChangeListener { _, checkedId ->
            val hours = when (checkedId) {
                R.id.range_1day -> 25
                R.id.range_3day -> 73
                R.id.range_7day -> 169
                else -> 25
            }
            prefs.edit().putInt("hour_range", hours).apply()
        }

        // Time display interval
        val intervalButtons = listOf(
            findViewById<RadioButton>(R.id.interval_1hr),
            findViewById<RadioButton>(R.id.interval_2hr),
            findViewById<RadioButton>(R.id.interval_3hr),
            findViewById<RadioButton>(R.id.interval_6hr)
        )
        val savedInterval = prefs.getInt("time_interval", 2)
        intervalButtons.forEach { it.isChecked = false }
        when (savedInterval) {
            1 -> intervalButtons[0].isChecked = true
            2 -> intervalButtons[1].isChecked = true
            3 -> intervalButtons[2].isChecked = true
            6 -> intervalButtons[3].isChecked = true
        }
        intervalButtons.forEach { button ->
            button.setOnClickListener {
                intervalButtons.forEach { it.isChecked = false }
                button.isChecked = true
                val selectedHours = when (button.id) {
                    R.id.interval_1hr -> 1
                    R.id.interval_2hr -> 2
                    R.id.interval_3hr -> 3
                    R.id.interval_6hr -> 6
                    else -> 2
                }
                prefs.edit().putInt("time_interval", selectedHours).apply()
            }
        }

        // Date format
        findViewById<RadioGroup>(R.id.dateFormatGroup).apply {
            when (prefs.getString("date_format", "dd/MM")) {
                "dd/MM" -> check(R.id.dateFormat_ddmmyy)
                "MM/dd" -> check(R.id.dateFormat_mmddyy)
            }
            setOnCheckedChangeListener { _, checkedId ->
                val value = when (checkedId) {
                    R.id.dateFormat_ddmmyy -> "dd/MM"
                    R.id.dateFormat_mmddyy -> "MM/dd"
                    else -> "dd/MM"
                }
                prefs.edit().putString("date_format", value).apply()
            }
        }

        // Time format
        findViewById<RadioGroup>(R.id.timeFormatGroup).apply {
            when (prefs.getString("time_format", "24")) {
                "24" -> check(R.id.timeFormat_24)
                "12" -> check(R.id.timeFormat_12)
            }
            setOnCheckedChangeListener { _, checkedId ->
                val value = when (checkedId) {
                    R.id.timeFormat_24 -> "24"
                    R.id.timeFormat_12 -> "12"
                    else -> "24"
                }
                prefs.edit().putString("time_format", value).apply()
            }
        }

        // Notification toggles
        setupNotificationToggle(
            findViewById(R.id.layoutDailyNotification),
            findViewById(R.id.switchDailyNotification),
            "DailyNotification"
        )

        setupNotificationToggle(
            findViewById(R.id.layoutTemperatureWarning),
            findViewById(R.id.switchTemperatureWarningNotification),
            "TemperatureWarning"
        )

        setupNotificationToggle(
            findViewById(R.id.layoutRainWarning),
            findViewById(R.id.switchRainWarningNotification),
            "RainWarning"
        )

        setupNotificationToggle(
            findViewById(R.id.layoutRecurringNotification),
            findViewById(R.id.switchRecurringNotification),
            "RecurringNotification"
        )

        // Initial reschedule on first open
        rescheduleEnabledNotifications()
    }

    // Optional: if you return from the time picker, re-schedule for enabled types with the newest time.
    override fun onResume() {
        super.onResume()
        rescheduleEnabledNotifications()
    }

    private fun rescheduleEnabledNotifications() {
        val prefs = NotificationPrefs.getPrefs(this)

        // Check the correct flag (with _enabled)
        if (prefs.getBoolean("RecurringNotification_enabled", false)) {
            val times = prefs.getStringSet("RecurringNotification_times", emptySet()) ?: emptySet()
            for (time in times) {
                val (hour, minute) = time.split(":").map { it.toInt() }
                scheduleRecurringNotification(time, hour, minute)
            }
        }

        // Handle the rest normally
        val types = listOf("DailyNotification", "TemperatureWarning", "RainWarning")
        for (type in types) {
            val enabled = prefs.getBoolean("${type}_enabled", false)
            if (enabled) {
                val hour = prefs.getInt("${type}_hour", 8)
                val minute = prefs.getInt("${type}_minute", 0)
                scheduleNotification(type, hour, minute)
            }
        }
    }





    private fun setupNotificationToggle(
        rowView: View,
        switchView: MaterialSwitch,
        type: String
    ) {
        val nPrefs = NotificationPrefs.getPrefs(this)
        val currentEnabled = nPrefs.getBoolean("${type}_enabled", false)
        switchView.isChecked = currentEnabled

        // When row is clicked → open correct settings screen
        rowView.setOnClickListener {
            val intent = if (type == "RecurringNotification") {
                // Open the screen where the 5 alarms can be managed
                Intent(this, RecurringAlarmsActivity::class.java).apply {
                    putExtra("notificationType", type)
                }
            } else {
                Intent(this, NotificationSettingsActivity::class.java).apply {
                    putExtra("notificationType", type)
                }
            }
            startActivity(intent)
        }

        // Handle main toggle (on/off for all alarms)
        switchView.setOnCheckedChangeListener { _, isChecked ->
            nPrefs.edit().putBoolean("${type}_enabled", isChecked).apply()

            if (isChecked) {
                if (type == "RecurringNotification") {
                    // Enable all alarms saved in preferences
                    val times = nPrefs.getStringSet("RecurringNotification_times", emptySet()) ?: emptySet()
                    for (time in times) {
                        val (hour, minute) = time.split(":").map { it.toInt() }
                        scheduleRecurringNotification(time, hour, minute)
                    }
                } else {
                    // For single one-time notification types
                    val hour = nPrefs.getInt("${type}_hour", 8)
                    val minute = nPrefs.getInt("${type}_minute", 0)
                    scheduleNotification(type, hour, minute)
                }
            } else {
                if (type == "RecurringNotification") {
                    // Cancel ALL recurring alarms if master switch is off
                    cancelAllRecurringNotifications()
                } else {
                    cancelNotification(type)
                }
            }
        }
    }



    private fun scheduleNotification(type: String, hour: Int, minute: Int) {
        Log.d("ScheduleNotification", "Scheduling $type at $hour:$minute")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("notificationType", type)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, type.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelAllRecurringNotifications() {
        val nPrefs = NotificationPrefs.getPrefs(this)
        val times = nPrefs.getStringSet("RecurringNotification_times", emptySet()) ?: emptySet()
        for (time in times) {
            cancelNotification("RecurringNotification_$time")
        }
    }


    private fun scheduleRecurringNotification(time: String, hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("notificationType", "RecurringNotification")
            putExtra("time", time)
        }

        // Unique requestCode for each time
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            time.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d("ScheduleNotification", "Scheduled RecurringNotification at $hour:$minute ($time)")
    }

    private fun cancelNotification(type: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)

        if (type == "RecurringNotification") {
            val prefs = NotificationPrefs.getPrefs(this)
            val times = prefs.getStringSet("RecurringNotification_times", emptySet()) ?: emptySet()
            for (time in times) {
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    time.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        } else {
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                type.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
