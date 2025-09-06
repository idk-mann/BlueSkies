package com.example.myapplication2

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.preference.PreferenceManager
import com.example.myapplication2.databinding.ActivityMainBinding
import com.example.myapplication2.utils.TimeUtils
import java.time.*
import java.time.format.DateTimeFormatter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.color.DynamicColors
import android.view.Window
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.core.content.ContentProviderCompat.requireContext
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

import com.example.myapplication2.utils.defaultLocations
import com.example.myapplication2.data.repository.CityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {


    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sunriseSunsetManager: SunriseSunsetManager



    private val _locationReady = MutableLiveData(false)
    private val _observerReady = MutableLiveData(false)

    val triggerUpdate = MediatorLiveData<Boolean>().apply {
        fun checkStates() {
            val location = _locationReady.value ?: false
            val observer = _observerReady.value ?: false

            val combined = location && observer
            if (value != combined) {  // 👈 prevent duplicate emissions
                Log.d("TriggerUpdateDebug", "LocationReady=$location, ObserverReady=$observer → Trigger=$combined")
                value = combined
            }
        }
        addSource(_locationReady) { checkStates() }
        addSource(_observerReady) { checkStates() }

    }


    private val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    private var locations: Map<String, String> = emptyMap()

    private var sunriseT: LocalTime? = null
    private var sunsetT: LocalTime? = null
    private var duskT: LocalTime? = null
    private var dawnT: LocalTime? = null
    private var zoneId: ZoneId? = null
    private var isLightTheme: Boolean = true



    private fun loadLocationsFromPrefs(): MutableMap<String, String> {
        val json = prefs.getString("user_locations_json", null)
        val saved: MutableMap<String, String> = if (json != null) {
            try {
                val type = object : TypeToken<MutableMap<String, String>>() {}.type
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }

        // Always add defaults ("Current Location" = gps, "None" = none)
        return defaultLocations().toMutableMap().apply {
            putAll(saved)
        }
    }



    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(onLocationReady: (Double, Double) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // ask for permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
                _locationReady.value = true  // ✅ mark location ready
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Main function: get coords, handle GPS case
    @SuppressLint("MissingPermission")
    fun getSelectedLocationCoords(onLocationReady: (Double, Double) -> Unit) {
        _locationReady.value = false

        val locations = loadLocationsFromPrefs()
        val selectedLocation = prefs.getString("selected_location_name", "none")
        val coordsStr = locations[selectedLocation]

        when {
            coordsStr == "gps" -> {
                fetchCurrentLocation { lat, lon ->
                    onLocationReady(lat, lon)
                }
            }

            !coordsStr.isNullOrBlank() && coordsStr != "none" -> {
                val coords = coordsStr.split(",").map { it.trim().toDouble() }
                onLocationReady(coords[0], coords[1])
                _locationReady.value = true  // ✅ mark location ready
            }

            else -> {
                Toast.makeText(this, "Add a location", Toast.LENGTH_SHORT).show()
            }
        }
    }




    //
//    private var lastGradient: Drawable? = null
    private var lastGradient: Drawable? = null

    private fun saveLastGradientToPrefs(gradient: GradientDrawable) {
        val colors = gradient.colors ?: return
        val colorString = colors.joinToString(",") { String.format("#%06X", 0xFFFFFF and it) }
        prefs.edit().putString("last_gradient_colors", colorString).apply()
    }

    private fun loadLastGradientFromPrefs(): GradientDrawable? {
        val colorString = prefs.getString("last_gradient_colors", null) ?: return null
        val colors = colorString.split(",").map { colorHex: String -> Color.parseColor(colorHex) }.toIntArray()
        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            shape = GradientDrawable.RECTANGLE
        }
    }


    var areObserversReady = false
        private set


    fun setObserversReady() {
        _observerReady.value = true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("TransitionDebug", "🔼 onCreate STARTED")

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sunriseSunsetManager = SunriseSunsetManager(this)




        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
            Log.d("ThemeDebug", "First launch - theme applied and flag set to false")
        }

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 🌅 Initial location-based theme setup
        updateThemeBasedOnLocation()

        // 📦 Listen for location change from fragments
        supportFragmentManager.setFragmentResultListener("location_changed", this) { _, _ ->
            updateThemeBasedOnLocation()
        }

        lifecycleScope.launch {
            showLoading(true)

            withContext(Dispatchers.IO) {
                CityRepository.getInstance(this@MainActivity).ensurePreloaded()
            }

            showLoading(false)
        }

//        binding.toolbar.context.setTheme(R.style.ThemeOverlay_MyApp_Toolbar_Dark)

    }

    private fun showLoading(show: Boolean) {
        val overlay = binding.loadingOverlay // if using ViewBinding
        overlay.visibility = if (show) View.VISIBLE else View.GONE
    }


    private fun updateThemeBasedOnLocation() {
        getSelectedLocationCoords { lat, lon ->
            loadSunTimes(lat, lon) {
                val period = getTimePeriod()

                val lightTheme = isThemeLight(period)
                isLightTheme = lightTheme

                // Update background gradient
                lastGradient = loadLastGradientFromPrefs()
                val newGradient = getGradientForPeriod(period)

                animateBackgroundTransition(
                    binding.root,
                    newGradient,
                    lastGradient ?: ColorDrawable(Color.TRANSPARENT)
                )

                lastGradient = newGradient
                saveLastGradientToPrefs(newGradient)

//                // Control status bar icon colors (dark icons on light theme, etc.)
//                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    windowInsetsController.isAppearanceLightStatusBars = lightTheme
//                }
//
//                @Suppress("DEPRECATION")
//                window.statusBarColor = ContextCompat.getColor(
//                    this,
//                    if (lightTheme) R.color.status_bar_dark else R.color.status_bar_light
//                )
//
//                WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = lightTheme
//
//
//                // Toolbar colors (your own logic)
//                val toolbarColor = if (lightTheme) {
//                    ContextCompat.getColor(this, R.color.toolbar_light)
//                } else {
//                    ContextCompat.getColor(this, R.color.toolbar_dark)
//                }
//                binding.toolbar.setBackgroundColor(toolbarColor)
//
//                val toolbarTitleColor = if (lightTheme) Color.BLACK else Color.WHITE
//                binding.toolbar.setTitleTextColor(toolbarTitleColor)
//                binding.toolbar.navigationIcon?.setTint(toolbarTitleColor)

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                Log.d("MenuAction", "Settings selected")
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun loadSunTimes(
        lat: Double,
        lon: Double,
        onComplete: () -> Unit
    ) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val todayUtc = LocalDate.now(ZoneOffset.UTC)

        zoneId = TimeUtils.getTimeZoneFromLocation(lat, lon)
        if (zoneId == null) {
            Log.e("SunTimes", "ZoneId could not be resolved, using UTC fallback")
            zoneId = ZoneId.of("UTC")
        }

        Log.d("SunTimes", "Time: $todayUtc, Lat: $lat, Lon: $lon, ZoneId: $zoneId")

        val (sunriseStr, sunsetStr) = sunriseSunsetManager.getSunriseSunset(lat, lon, todayUtc)
        val (dawnStr, duskStr) = sunriseSunsetManager.getDawnDusk(lat, lon, todayUtc)

        sunriseT = TimeUtils.parseToLocal(todayUtc, sunriseStr, zoneId!!, formatter)
        sunsetT = TimeUtils.parseToLocal(todayUtc, sunsetStr, zoneId!!, formatter)
        dawnT = TimeUtils.parseToLocal(todayUtc, dawnStr, zoneId!!, formatter)
        duskT = TimeUtils.parseToLocal(todayUtc, duskStr, zoneId!!, formatter)

        Log.d("SunTimes", "Parsed Times -> Dawn: $dawnT, Sunrise: $sunriseT, Sunset: $sunsetT, Dusk: $duskT")
        onComplete()
    }


    enum class TimePeriod {
        DAWN, DAY, DUSK, NIGHT
    }

    private fun getTimePeriod(): TimePeriod {
        if (zoneId == null || sunriseT == null || sunsetT == null || dawnT == null || duskT == null) {
            Log.w("TimePeriod", "Missing sun data, fallback to DAY")
            return TimePeriod.DAY
        }

        val now = ZonedDateTime.now(zoneId).toLocalTime()
        Log.d("TimePeriod", "Now: $now, Dawn: $dawnT, Sunrise: $sunriseT, Sunset: $sunsetT, Dusk: $duskT")

        return when {
            now.isAfter(dawnT) && now.isBefore(sunriseT) -> {
                Log.d("TimePeriod", "Detected period: DAWN")
                TimePeriod.DAWN
            }
            now.isAfter(sunriseT) && now.isBefore(sunsetT) -> {
                Log.d("TimePeriod", "Detected period: DAY")
                TimePeriod.DAY
            }
            now.isAfter(sunsetT) && now.isBefore(duskT) -> {
                Log.d("TimePeriod", "Detected period: DUSK")
                TimePeriod.DUSK
            }
            else -> {
                Log.d("TimePeriod", "Detected period: NIGHT")
                TimePeriod.NIGHT
            }
        }
    }


    private fun isThemeLight(period: TimePeriod): Boolean {
        return period == TimePeriod.DAWN || period == TimePeriod.DAY
    }

    private fun getGradientForPeriod(period: TimePeriod): GradientDrawable {
        val colors = when (period) {
            TimePeriod.DAWN -> intArrayOf(
                Color.parseColor("#FFF176"),
                Color.parseColor("#FFA726")
            )
            TimePeriod.DAY -> intArrayOf(
                Color.parseColor("#AFD2E6"),
                Color.parseColor("#91B9D7")
            )
            TimePeriod.DUSK -> intArrayOf(
                Color.parseColor("#FC992C"),
                Color.parseColor("#8A77D9")
            )
            TimePeriod.NIGHT -> intArrayOf(
                Color.parseColor("#2562D6"),
                Color.parseColor("#123A85"),
                Color.parseColor("#030A17")
            )
        }


        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            shape = GradientDrawable.RECTANGLE
        }
    }



    fun getDefaultGradient(): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                Color.parseColor("#2562D6"),
                Color.parseColor("#123A85"),
                Color.parseColor("#030A17")
            )
        ).apply {
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }
    }


    fun animateBackgroundTransition(
        view: View,
        newGradient: GradientDrawable,
        lastGradient: Drawable?, // ✅ now nullable and general
        duration: Int = 1000
    ) {
        Log.d("BackgroundTransition", "animateBackgroundTransition called with duration: $duration")

        val fromDrawable: Drawable = lastGradient ?: run {
            Log.d("BackgroundTransition", "lastGradient is null (using default gradient).")
            getDefaultGradient()
        }

        Log.d("BackgroundTransition", "From drawable: ${fromDrawable.javaClass.simpleName}")
        Log.d("BackgroundTransition", "To drawable: ${newGradient.javaClass.simpleName}")

        val transition = TransitionDrawable(arrayOf(fromDrawable, newGradient)).apply {
            isCrossFadeEnabled = true
        }

        view.background = transition
        transition.startTransition(duration)
    }

}

//    fun animateBackgroundTransition(view: View, newGradient: GradientDrawable, duration: Int = 1000) {
//        Log.d("BackgroundTransition", "animateBackgroundTransition called with duration: $duration")
//
//        val fromDrawable = lastGradient ?: getDefaultGradient()
//
//
//
//        Log.d("BackgroundTransition", "From drawable: ${fromDrawable.javaClass.simpleName}")
//        Log.d("BackgroundTransition", "To drawable: ${arrayOf(fromDrawable, newGradient).joinToString { it.javaClass.simpleName }}")
//
//
//
//        val transition = TransitionDrawable(arrayOf(fromDrawable, newGradient)).apply {
//            isCrossFadeEnabled = true
//        }
//
//        view.background = transition
//        transition.startTransition(duration)
//
//        // Save the new gradient as the "last" one for the next call
//        lastGradient = newGradient
//
//        Log.d("BackgroundTransition", "Transition started. Updated lastGradient.")
//    }

//    private fun getTimeBasedGradient(): GradientDrawable {
//        if (zoneId == null || sunriseT == null || sunsetT == null || dawnT == null || duskT == null) {
//            Log.w("Gradient", "Fallback to default gradient due to missing data")
//            return defaultGradient()
//        }
//
//        val now = ZonedDateTime.now(zoneId).toLocalTime()
//        Log.d("TimeDebug", "Current local time: $now")
//
//        val (colors, themeIsLight) = when {
//            now.isAfter(dawnT) && now.isBefore(sunriseT) -> Pair(
//                intArrayOf(
//                    Color.parseColor("#FFF176"),
//                    Color.parseColor("#FFA726")
//                ),
//                true // light theme
//            )
//
//            now.isAfter(sunriseT) && now.isBefore(duskT) -> Pair(
//                intArrayOf(
//                    Color.parseColor("#AFD2E6"),
//                    Color.parseColor("#91B9D7")
//                ),
//                true // light theme
//            )
//
//            now.isAfter(duskT) && now.isBefore(sunsetT) -> Pair(
//                intArrayOf(
//                    Color.parseColor("#FC992C"),
//                    Color.parseColor("#8A77D9")
//                ),
//                false // dark theme
//            )
//
//            else -> Pair(
//                intArrayOf(
//                    Color.parseColor("#2562D6"),
//                    Color.parseColor("#123A85"),
//                    Color.parseColor("#030A17")
//                ),
//                false // dark theme
//            )
//        }
//
//        isLightTheme = themeIsLight
//
////        // Automatically switch app-wide theme based on time
////        AppCompatDelegate.setDefaultNightMode(
////            if (isLightTheme) AppCompatDelegate.MODE_NIGHT_NO
////            else AppCompatDelegate.MODE_NIGHT_YES
////        )
//
//
//
//
//        return GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
//            shape = GradientDrawable.RECTANGLE
//        }
//    }


//    private fun defaultGradient(): GradientDrawable {
//        return GradientDrawable(
//            GradientDrawable.Orientation.TOP_BOTTOM,
//            intArrayOf(Color.DKGRAY, Color.BLACK)
//        ).apply {
//            shape = GradientDrawable.RECTANGLE
//        }
//    }


