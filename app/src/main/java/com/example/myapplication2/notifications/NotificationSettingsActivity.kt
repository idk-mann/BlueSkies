package com.example.myapplication2.notifications

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication2.R
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

import com.example.myapplication2.utils.TimeUtils

class NotificationSettingsActivity : AppCompatActivity() {

    private var selectedHour = 8
    private var selectedMinute = 0
    private lateinit var notificationType: String
    private lateinit var tvSelectedTime: TextView
    private lateinit var picker: MaterialTimePicker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        notificationType = intent.getStringExtra("notificationType") ?: "Notification"

        val formattedTitle = notificationType.replace(Regex("(?<=.)([A-Z])"), " $1")
        supportActionBar?.title = "$formattedTitle Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Request notification permission if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Load shared prefs
        val prefs = NotificationPrefs.getPrefs(this)

        // Load saved time
        selectedHour = prefs.getInt("${notificationType}_hour", 8)
        selectedMinute = prefs.getInt("${notificationType}_minute", 0)

        // UI setup
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        val btnSelectTime: Button = findViewById(R.id.btnSelectTime)
        val btnSave: Button = findViewById(R.id.btnSaveTime)

        updateTimeDisplay(prefs)

        btnSelectTime.setOnClickListener { showMaterialTimePicker(prefs) }

        btnSave.setOnClickListener {
            // Save new time
            prefs.edit()
                .putInt("${notificationType}_hour", selectedHour)
                .putInt("${notificationType}_minute", selectedMinute)
                .apply()

            val isEnabled = prefs.getBoolean("${notificationType}_enabled", false)

            Log.d(
                "SaveNotification",
                "Saved time for $notificationType → $selectedHour:$selectedMinute | Enabled: $isEnabled"
            )

            Toast.makeText(this, "Time saved for $formattedTitle", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("NotificationSettings", "Notification permission granted")
            } else {
                Toast.makeText(this, "Notifications won't work without permission.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateTimeDisplay(prefs: android.content.SharedPreferences) {
        val timeFormatPref = prefs.getString("time_format", "24")
        val is24HourFormat = timeFormatPref == "24"

        val formattedTime = TimeUtils.formatTime(selectedHour, selectedMinute, is24HourFormat)

        tvSelectedTime.text = "Selected Time: $formattedTime"
    }

    private fun showMaterialTimePicker(prefs: android.content.SharedPreferences) {
        val timeFormatPref = prefs.getString("time_format", "24")
        val is24HourFormat = timeFormatPref == "24"

        picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (is24HourFormat) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Select Time")
            .build()

        picker.show(supportFragmentManager, "materialTimePicker")

        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeDisplay(prefs)
        }
    }
}
