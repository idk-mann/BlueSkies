package com.example.myapplication2.notifications

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.airbnb.lottie.LottieAnimationView
import com.example.myapplication2.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.example.myapplication2.utils.TimeUtils
import android.animation.LayoutTransition

import android.graphics.Color

class RecurringAlarmsActivity : AppCompatActivity() {

    private lateinit var alarmContainer: LinearLayout
    private lateinit var prefs: SharedPreferences
    private lateinit var notificationType: String
    private val maxAlarms = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_notifications)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        notificationType = intent.getStringExtra("notificationType") ?: "Notification"

        val formattedTitle = notificationType.replace(Regex("(?<=.)([A-Z])"), " $1")
        supportActionBar?.title = "$formattedTitle Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        prefs = NotificationPrefs.getPrefs(this)
        alarmContainer = findViewById(R.id.alarmContainer)

        val btnAddAlarm: Button = findViewById(R.id.btnAddAlarm)
        val btnSaveAlarms: Button = findViewById(R.id.btnSaveAlarms)


        // Load saved alarms
        val times = prefs.getStringSet("RecurringNotification_times", emptySet()) ?: emptySet()
        for (time in times) {
            addAlarmRow(time)
        }

        btnAddAlarm.setOnClickListener {
            if (alarmContainer.childCount >= maxAlarms) {
                Toast.makeText(this, "Max $maxAlarms alarms allowed", Toast.LENGTH_SHORT).show()
            } else {
                addAlarmRow(null) // empty row
            }
        }

        btnSaveAlarms.setOnClickListener {
            saveAlarms()
        }

        alarmContainer.layoutTransition = LayoutTransition().apply {
            enableTransitionType(LayoutTransition.CHANGING)
        }
    }

    private fun addAlarmRow(time: String?) {
        val row = layoutInflater.inflate(R.layout.item_alarm, alarmContainer, false)

        val tvLabel: TextView = row.findViewById(R.id.tvAlarmLabel)
        val btnPick: Button = row.findViewById(R.id.btnPickTime)
        val btnDelete: LottieAnimationView = row.findViewById(R.id.btnDeleteAlarm)
        btnDelete.setColorFilter(Color.RED)


        var selectedHour = 12
        var selectedMinute = 0

        if (!time.isNullOrEmpty()) {
            val parts = time.split(":")
            if (parts.size == 2) {
                selectedHour = parts[0].toIntOrNull() ?: 12
                selectedMinute = parts[1].toIntOrNull() ?: 0
            }
        }

        val timeFormatPref = prefs.getString("time_format", "24")
        val is24HourFormat = timeFormatPref == "24"

        val displayTime = if (!time.isNullOrEmpty()) {
            TimeUtils.formatTime(selectedHour, selectedMinute, is24HourFormat)
        } else {
            "No time"
        }

        tvLabel.text = "Alarm: $displayTime"

        btnPick.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(
                    if (is24HourFormat) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
                )
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText("Select Time")
                .build()

            picker.show(supportFragmentManager, "materialTimePicker")

            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute

                val internalTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                row.tag = internalTime

                val displayTime = TimeUtils.formatTime(selectedHour, selectedMinute, is24HourFormat)

                tvLabel.text = "Alarm: $displayTime"
            }
        }


        btnDelete.setOnClickListener {

            btnDelete.playAnimation()

            // Animate the row to fade and slide out
            row.animate()
                .alpha(0f)
                .translationX(-row.width.toFloat())
                .setDuration(300)
                .withEndAction {
                    // Temporarily disable layout transition
                    val layoutTransition = alarmContainer.layoutTransition
                    alarmContainer.layoutTransition = null

                    alarmContainer.removeView(row)

                    // Post to re-enable transition after layout settles
                    alarmContainer.post {
                        alarmContainer.layoutTransition = layoutTransition
                    }
                }
                .start()
        }

        row.tag = time // Optional: initial tag before time is picked
        alarmContainer.addView(row)
    }



    private fun saveAlarms() {
        val times = mutableSetOf<String>()
        for (i in 0 until alarmContainer.childCount) {
            val row = alarmContainer.getChildAt(i)
            val time = row.tag as? String
            if (time != null) times.add(time)
        }

        prefs.edit().putStringSet("RecurringNotification_times", times).apply()

        // Schedule alarms
        for (time in times) {
            val (hour, minute) = time.split(":").map { it.toInt() }
            scheduleRecurringNotification(time, hour, minute)
        }

        Toast.makeText(this, "Saved ${times.size} alarms", Toast.LENGTH_SHORT).show()
        finish()
    }



    private fun scheduleRecurringNotification(tag: String, hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent for your BroadcastReceiver that shows the notification
        val intent = Intent(this, DailyNotificationReceiver::class.java).apply {
            putExtra("alarm_tag", tag) // optional: identify which alarm
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            tag.hashCode(), // unique request code per alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set calendar for the alarm time
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // if the time is before "now", move to next day
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Schedule repeating daily alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}
