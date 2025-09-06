package com.example.myapplication2

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication2.databinding.FragmentFirstBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min
import android.widget.ToggleButton
import org.json.JSONArray
import android.widget.LinearLayout
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.preference.PreferenceManager
import android.app.Activity
import android.widget.Toast
import java.time.LocalTime
import java.time.*
import androidx.recyclerview.widget.LinearLayoutManager
import java.util.Calendar
import android.content.Context
import androidx.core.content.ContextCompat
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import androidx.recyclerview.widget.RecyclerView
import android.widget.AutoCompleteTextView
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.temporal.ChronoUnit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.location.LocationServices
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged

import kotlinx.coroutines.delay

import com.example.myapplication2.utils.WeatherCodeUtils
import com.example.myapplication2.utils.TimezoneMapper
import com.example.myapplication2.viewmodel.WeatherViewModel
import com.example.myapplication2.utils.defaultLocations
import com.example.myapplication2.utils.SunTimesManager
import com.example.myapplication2.data.repository.CityRepository
import com.example.myapplication2.model.City
import com.example.myapplication2.data.local.entity.CityEntity

import com.example.myapplication2.data.model.HourlyData


//utility
fun resolveTimeFormat(formatPref: String?): String {
    return when (formatPref) {
        "12" -> "hh:mma"
        "24" -> "HH:mm"
        else -> "HH:mm" // default fallback
    }
}



class FirstFragment : Fragment() {

    fun View.fadeIn(duration: Long = 500) {
        this.visibility = View.VISIBLE
        this.alpha = 0f
        this.animate().alpha(1f).setDuration(duration).start()
    }


    //reference
    val prefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }


    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private lateinit var sunriseSunsetManager: SunriseSunsetManager
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private val weatherViewModel: WeatherViewModel by viewModels()



    //#rain probability chart
    private var selectedHours: Int = 13

    var selectedCoordinates: String? = null



    private var locations: MutableMap<String, String> = mutableMapOf()
    private var pendingCoordInput: EditText? = null

    private fun loadLocations() {
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

        // Always add defaults back in (cannot be deleted)
        locations = defaultLocations().toMutableMap().apply {
            putAll(saved)
        }
    }

    private fun saveLocations() {
        val userOnly = locations.filterKeys { it !in defaultLocations().keys }
        val json = Gson().toJson(userOnly)
        prefs.edit().putString("user_locations_json", json).apply()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sunriseSunsetManager = SunriseSunsetManager(requireContext())
        autoCompleteTextView = view.findViewById(R.id.location_dropdown)
        val apiKey = BuildConfig.TOMORROW_API_KEY

        val mainActivity = activity as? MainActivity // or your actual activity class name
        mainActivity?.triggerUpdate?.observe(viewLifecycleOwner) { isReady ->
            if (isReady == true) {
                Log.d("Weatherloading", "✅ triggerUpdate is TRUE: location and observer are both ready")
                // Do something now that both are ready...
            }
        }


        setupWeatherObservers()

        weatherViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        // === Dropdown text color + drawable ===
        val autoCompleteTextView = view.findViewById<AutoCompleteTextView>(R.id.location_dropdown)
        val colorResId = SunTimesManager.getCurrentTextColorRes()
        val textColor = ContextCompat.getColor(requireContext(), colorResId)
        Log.d("ColorDebug2", "Resolved text color: ${String.format("#%08X", textColor)}")
        autoCompleteTextView.setTextColor(textColor)

        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.rotate_drawable)?.apply {
            val sizeInDp = 24
            val scale = resources.displayMetrics.density
            val sizeInPx = (sizeInDp * scale + 0.5f).toInt()
            setBounds(0, 0, sizeInPx, sizeInPx)
            setTint(autoCompleteTextView.currentTextColor)
        }
        autoCompleteTextView.setCompoundDrawables(null, null, drawable, null)

        // === Locations dropdown setup ===
        val dropdown = binding.locationDropdown
        loadLocations()

        val locationNames = locations.keys.toMutableList().apply { add("+ Add Location") }
        val locationAdapter = LocationAdapter(locationNames.toMutableList()) { /* delete mode toggle */ }

        val popupView = layoutInflater.inflate(R.layout.popup_location_list, null)
        val recyclerView = popupView.findViewById<RecyclerView>(R.id.location_recycler)
        val deleteBtn = popupView.findViewById<Button>(R.id.delete_button)
        val deleteControls = popupView.findViewById<LinearLayout>(R.id.delete_controls)
        val cancelBtn = popupView.findViewById<Button>(R.id.cancel_button)
        val confirmDeleteBtn = popupView.findViewById<Button>(R.id.confirm_delete_button)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = locationAdapter

        val popupWindow = PopupWindow(
            popupView,
            dropdown.width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 10f
            isOutsideTouchable = true
        }

        deleteControls.visibility = View.GONE

        val savedLocation = prefs.getString("selected_location_name", "gps")
        if (!savedLocation.isNullOrEmpty()) {
            dropdown.setText(savedLocation, false)
            selectedCoordinates = locations[savedLocation]

            when (selectedCoordinates) {
                "gps" -> {
                    fetchCurrentLocation { lat, lon ->
                        loadSunEvents(lat, lon)
                        // REMOVE: weatherViewModel.fetchWeather("$lat,$lon", apiKey)
                        // The observer will handle this automatically
                    }
                }
                "none" -> {
                    Toast.makeText(requireContext(), "No location selected", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    try {
                        selectedCoordinates?.split(",")?.let {
                            val (lat, lon) = it.map(String::toDouble)
                            loadSunEvents(lat, lon)
                            // REMOVE: weatherViewModel.fetchWeather(selectedCoordinates!!, apiKey)
                            // The observer will handle this automatically
                        }
                    } catch (e: Exception) {
                        Log.e("FirstFragment", "Error parsing coordinates: $selectedCoordinates", e)
                        Toast.makeText(requireContext(), "Invalid coordinates", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }




        // === Dropdown click shows popup ===
        dropdown.setOnClickListener {
            popupWindow.width = dropdown.width
            popupWindow.showAsDropDown(dropdown)
            animateArrow(true)

            locationAdapter.deleteMode = false
            deleteControls.visibility = View.GONE
            deleteBtn.visibility = View.VISIBLE
        }

        popupWindow.setOnDismissListener { animateArrow(false) }

        (requireActivity() as MainActivity).setObserversReady()

        // === Item clicks ===
        locationAdapter.onItemClick = { name ->
            popupWindow.dismiss()
            when {
                name == "+ Add Location" -> {
                    showAddLocationDialog(locationAdapter, locationNames)
                }
                else -> {
                    locations[name]?.let { coordsStr ->
                        dropdown.setText(name, false)
                        prefs.edit().putString("selected_location_name", name).apply()

                        if (coordsStr == "gps") {
                            fetchCurrentLocation { lat, lon ->
                                selectedCoordinates = "$lat,$lon"
                                loadSunEvents(lat, lon)
                                // Notify that location is ready

                                requireActivity().supportFragmentManager.setFragmentResult("location_changed", Bundle())
                            }
                        } else {
                            selectedCoordinates = coordsStr
                            try {
                                val coords = coordsStr.split(",").map { it.trim().toDouble() }
                                loadSunEvents(coords[0], coords[1])
                                // Notify that location is ready

                                requireActivity().supportFragmentManager.setFragmentResult("location_changed", Bundle())
                            } catch (e: Exception) {
                                Log.e("FirstFragment", "Error parsing coordinates: $coordsStr", e)
                                Toast.makeText(requireContext(), "Invalid coordinates", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        // === Long press -> delete mode ===
        locationAdapter.onItemLongClick = { name ->
            if (name != "+ Add Location") {
                locationAdapter.deleteMode = true
                deleteBtn.visibility = View.GONE
                deleteControls.visibility = View.VISIBLE
                true
            } else false
        }

        deleteBtn.setOnClickListener {
            locationAdapter.deleteMode = true
            deleteBtn.visibility = View.GONE
            deleteControls.visibility = View.VISIBLE
            popupWindow.update(dropdown.width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        cancelBtn.setOnClickListener {
            locationAdapter.deleteMode = false
            deleteControls.visibility = View.GONE
            deleteBtn.visibility = View.VISIBLE
            popupWindow.update(dropdown.width, WindowManager.LayoutParams.WRAP_CONTENT)
        }

        confirmDeleteBtn.setOnClickListener {
            val namesToDelete = locationAdapter.getSelectedItems()
            if (namesToDelete.isNotEmpty()) {
                namesToDelete.forEach { name ->
                    locations.remove(name)
                    locationNames.remove(name)
                }
                saveLocations()
                locationAdapter.updateList(locationNames)
                locationAdapter.deleteMode = false
                deleteControls.visibility = View.GONE
                deleteBtn.visibility = View.VISIBLE

                if (namesToDelete.contains(dropdown.text.toString())) {
                    val fallback = locationNames.firstOrNull { it != "+ Add Location" } ?: run {
                        dropdown.setText("", false)
                        selectedCoordinates = null
                        popupWindow.dismiss()
                        return@setOnClickListener
                    }
                    dropdown.setText(fallback, false)
                    selectedCoordinates = locations[fallback]
                    selectedCoordinates?.let { coordsStr ->
                        if (coordsStr != "gps" && coordsStr != "none") {
                            loadSunEventsFromCoordinates(coordsStr)
                            // Notify that location is ready

                        }
                    }
                    prefs.edit().putString("selected_location_name", fallback).apply()
                }
            }
            popupWindow.dismiss()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation(onLocationReady: (Double, Double) -> Unit) {
        val context = context ?: return // Safe context access
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // ask for permission if not granted
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (!isAdded) { // ← CRITICAL: Check if fragment is still attached
                Log.d("Location", "Fragment not attached, ignoring location result")
                return@addOnSuccessListener
            }

            val safeContext = context ?: return@addOnSuccessListener
            if (location != null) {
                onLocationReady(location.latitude, location.longitude)
            } else {
                Toast.makeText(safeContext, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun animateArrow(expand: Boolean) {
        val drawable = autoCompleteTextView.compoundDrawables[2] ?: return // right drawable
        val startLevel = if (expand) 0 else 5000
        val endLevel = if (expand) 5000 else 0

        val animator = ValueAnimator.ofInt(startLevel, endLevel)
        animator.duration = 300
        animator.addUpdateListener {
            val level = it.animatedValue as Int
            drawable.level = level
            autoCompleteTextView.invalidate()
        }
        animator.start()
    }

    private fun showAddLocationDialog(
        adapter: LocationAdapter,
        locationNames: MutableList<String>
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_location, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.location_name_input)
        val coordInput = dialogView.findViewById<EditText>(R.id.location_coord_input)
        val cityAuto = dialogView.findViewById<AutoCompleteTextView>(R.id.location_city_autocomplete)
        val pickBtn = dialogView.findViewById<View>(R.id.pick_on_map_button)
        val addBtn = dialogView.findViewById<Button>(R.id.add_button)
        val cancelBtn = dialogView.findViewById<Button>(R.id.cancel_button)

        pendingCoordInput = coordInput

        // Repository
        val cityRepository = CityRepository.getInstance(requireContext())

        // Adapter of cities (we’ll let AutoCompleteTextView render City.toString())
        val cityAdapter = ArrayAdapter<City>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        cityAuto.setAdapter(cityAdapter)

        // Cache current city results to map selection
        var currentCityResults: List<City> = emptyList()

        // When user types, fetch matching cities from DB
        cityAuto.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()?.trim() ?: ""
            if (query.length >= 2) {
                lifecycleScope.launch {
                    currentCityResults = cityRepository.searchCities(query)
                    cityAdapter.clear()
                    cityAdapter.addAll(currentCityResults) // uses City.toString()
                    cityAdapter.notifyDataSetChanged()
                    cityAuto.showDropDown()
                }
            }
        }

        // When user selects a city
        cityAuto.setOnItemClickListener { parent, _, position, _ ->
            val selectedCity = parent.getItemAtPosition(position) as City
            nameInput.setText(selectedCity.name)
            coordInput.setText("${selectedCity.coord.lat}, ${selectedCity.coord.lon}")
        }

        pickBtn.setOnClickListener {
            val intent = Intent(requireContext(), OsmMapPickerActivity::class.java)
            startActivityForResult(intent, 1010)
        }

        val dialog = AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        addBtn.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val coords = coordInput.text.toString().trim()

            if (name.isNotEmpty() && coords.matches(Regex("""-?\d+(\.\d+)?,\s*-?\d+(\.\d+)?"""))) {
                // Save location
                locations[name] = coords
                saveLocations()

                // Update adapter list
                locationNames.add(locationNames.size - 1, name)
                adapter.updateList(locationNames)

                // Save selection
                prefs.edit().putString("selected_location_name", name).apply()

                // Update dropdown text color
                val colorResId = SunTimesManager.getCurrentTextColorRes()
                val textColor = ContextCompat.getColor(requireContext(), colorResId)
                binding.locationDropdown.setTextColor(textColor)
                binding.locationDropdown.setText(name, false)

                // Trigger sun event loading
                selectedCoordinates = coords
                loadSunEventsFromCoordinates(coords)

                // Notify parent
                requireActivity().supportFragmentManager.setFragmentResult("location_changed", Bundle())

                pendingCoordInput = null
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Invalid input", Toast.LENGTH_SHORT).show()
            }
        }

        cancelBtn.setOnClickListener {
            pendingCoordInput = null
            dialog.dismiss()
        }

        dialog.show()

    }





    private fun loadSunEventsFromCoordinates(coordsStr: String) {
        try {
            val coords = coordsStr.split(",").map { it.trim().toDouble() }
            val (lat, lon) = coords
            loadSunEvents(lat, lon)
            selectedCoordinates = coordsStr
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Invalid coordinates", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWeatherObservers() {
        val mainActivity = requireActivity() as MainActivity

        // Observe combined readiness from MainActivity
        mainActivity.triggerUpdate.observe(viewLifecycleOwner) { ready ->
            if (ready == true) {
                val selectedLocation = prefs.getString("selected_location_name", null)
                val coordsStr = locations[selectedLocation]

                if (coordsStr != null && coordsStr != "none") {
                    if (coordsStr == "gps") {
                        Log.d("Weatherloading", "Triggered by GPS location")

                        fetchCurrentLocation { lat, lon ->
                            loadSunEvents(lat, lon)
                            selectedCoordinates = "$lat,$lon"
                            weatherViewModel.fetchWeather("$lat,$lon", BuildConfig.TOMORROW_API_KEY)
                        }
                    } else {
                        Log.d("Weatherloading", "Triggered by saved coordinates: $coordsStr")

                        loadSunEventsFromCoordinates(coordsStr)
                        weatherViewModel.fetchWeather(coordsStr, BuildConfig.TOMORROW_API_KEY)
                    }
                }
            }
        }

        // Observe weather state from ViewModel for UI updates only
        weatherViewModel.weatherState.observe(viewLifecycleOwner) { state ->
            when (state) {
                WeatherViewModel.WeatherState.Loading -> {
                    Log.d("Weatherloading", "Loading weather data...")
                }
                WeatherViewModel.WeatherState.Success -> {
                    Log.d("Weatherloading", "Success weather data...")
                    checkAndRenderWeather()
                }
                WeatherViewModel.WeatherState.Error -> {
                    // Error is handled by the separate error observer
                }
                WeatherViewModel.WeatherState.Idle -> {
                    // Initial state, do nothing
                }
            }
        }

        // Error observer
        weatherViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun checkAndRenderWeather() {
        val mainActivity = requireActivity() as MainActivity
        val ready = mainActivity.triggerUpdate.value == true
        val weatherState = weatherViewModel.weatherState.value
        val hasWeatherData = !weatherViewModel.weatherData.value.isNullOrEmpty()

        if (ready && weatherState == WeatherViewModel.WeatherState.Success && hasWeatherData && isAdded) {
            lifecycleScope.launch {
                renderWeatherUI()
            }
        }
    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1010 && resultCode == Activity.RESULT_OK) {
            val lat = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val lon = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            pendingCoordInput?.setText("$lat, $lon")
            pendingCoordInput = null
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("FirstFragment", "Resuming – fragment resumed")
    }

    fun Activity.showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun getTimeZoneFromLocation(lat: Double, lon: Double): ZoneId {
        return try {
            val timeZoneId = TimezoneMapper.latLngToTimezoneString(lat, lon)
            ZoneId.of(timeZoneId)
        } catch (e: Exception) {
            ZoneId.of("UTC")
        }
    }

    private var zoneId: ZoneId? = null
    private var sunriseT: LocalTime? = null
    private var sunsetT: LocalTime? = null
    private var dawnT: LocalTime? = null
    private var duskT: LocalTime? = null
    private var nowLocationLocalT: LocalTime? = null

    private fun loadSunEvents(lat: Double, lon: Double) {
        zoneId = getTimeZoneFromLocation(lat, lon)

        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        val todayUtc = LocalDate.now(ZoneOffset.UTC)
        val yesterdayUtc = todayUtc.minusDays(1)
        val tomorrowUtc = todayUtc.plusDays(1)

        fun parseToLocal(localDate: LocalDate, timeStr: String): LocalTime {
            return try {
                val utcTime = LocalTime.parse(timeStr, formatter)
                val utcDateTime = ZonedDateTime.of(localDate, utcTime, ZoneOffset.UTC)
                val localDateTime = utcDateTime.withZoneSameInstant(zoneId)
                localDateTime.toLocalTime()
            } catch (e: Exception) {
                LocalTime.MIDNIGHT
            }
        }

        val (sunriseYStr, sunsetYStr) = sunriseSunsetManager.getSunriseSunset(lat, lon, yesterdayUtc)
        val (dawnYStr, duskYStr) = sunriseSunsetManager.getDawnDusk(lat, lon, yesterdayUtc)

        val (sunriseTStr, sunsetTStr) = sunriseSunsetManager.getSunriseSunset(lat, lon, todayUtc)
        val (dawnTStr, duskTStr) = sunriseSunsetManager.getDawnDusk(lat, lon, todayUtc)

        val (sunriseTmrStr, sunsetTmrStr) = sunriseSunsetManager.getSunriseSunset(lat, lon, tomorrowUtc)
        val (dawnTmrStr, duskTmrStr) = sunriseSunsetManager.getDawnDusk(lat, lon, tomorrowUtc)

        val sunriseY = parseToLocal(yesterdayUtc, sunriseYStr)
        val sunsetY = parseToLocal(yesterdayUtc, sunsetYStr)
        val dawnY = parseToLocal(yesterdayUtc, dawnYStr)
        val duskY = parseToLocal(yesterdayUtc, duskYStr)

        sunriseT = parseToLocal(todayUtc, sunriseTStr)
        sunsetT = parseToLocal(todayUtc, sunsetTStr)
        dawnT = parseToLocal(todayUtc, dawnTStr)
        duskT = parseToLocal(todayUtc, duskTStr)

        val sunriseTmr = parseToLocal(tomorrowUtc, sunriseTmrStr)
        val sunsetTmr = parseToLocal(tomorrowUtc, sunsetTmrStr)
        val dawnTmr = parseToLocal(tomorrowUtc, dawnTmrStr)
        val duskTmr = parseToLocal(tomorrowUtc, duskTmrStr)

        val newLocalTime = ZonedDateTime.now(zoneId).toLocalTime()
        if (nowLocationLocalT == null || !nowLocationLocalT!!.truncatedTo(ChronoUnit.MINUTES)
                .equals(newLocalTime.truncatedTo(ChronoUnit.MINUTES))
        ) {
            nowLocationLocalT = newLocalTime
            applyTimeBasedTextColor()
        } else {
            nowLocationLocalT = newLocalTime
        }

        SunTimesManager.dawn = dawnT
        SunTimesManager.sunrise = sunriseT
        SunTimesManager.sunset = sunsetT
        SunTimesManager.dusk = duskT
        SunTimesManager.now = nowLocationLocalT

        val sunPathScroll = binding.sunPathScroll
        val sunPathView = binding.sunPathView

        // Make invisible before setting data
        sunPathScroll.visibility = View.INVISIBLE
        sunPathScroll.alpha = 0f

        // Set data AFTER hiding it
        sunPathView.sunriseTimes = listOf(sunriseY, sunriseT!!, sunriseTmr)
        sunPathView.sunsetTimes = listOf(sunsetY, sunsetT!!, sunsetTmr)
        sunPathView.dawnTimes = listOf(dawnY, dawnT!!, dawnTmr)
        sunPathView.duskTimes = listOf(duskY, duskT!!, duskTmr)
        sunPathView.currentLocalTime = nowLocationLocalT!!

        val timeFormatPref = prefs.getString("time_format", "24") ?: "24"
        val resolvedFormat = resolveTimeFormat(timeFormatPref)
        sunPathView.timeFormatter = DateTimeFormatter.ofPattern(resolvedFormat)

        // Post animation block
        sunPathView.post {
            val pathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1500L
                addUpdateListener { animation ->
                    sunPathView.animationFraction = animation.animatedValue as Float
                }
            }

            val scrollAnimator = ValueAnimator.ofInt(
                sunPathView.width - sunPathScroll.width, 0
            ).apply {
                duration = 1500L
                addUpdateListener { animation ->
                    sunPathScroll.scrollTo(animation.animatedValue as Int, 0)
                }
            }

            AnimatorSet().apply {
                playTogether(pathAnimator, scrollAnimator)
                startDelay = 100L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        sunPathScroll.visibility = View.VISIBLE
                        sunPathScroll.animate()
                            .alpha(1f)
                            .setDuration(300L)
                            .start()
                    }
                })
                start()
            }
        }
    }


    private fun resolveThemeColor(context: Context, attr: Int): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, Color.GRAY)
        typedArray.recycle()
        return color
    }

    private fun styleRainChart(chart: LineChart, timeLabels: List<String>, hoursToShow: Int) {

        val labelColor = resolveThemeColor(requireContext(), R.attr.colorOnSurfaceSecondary)

        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            setExtraOffsets(0f, 0f, 20f, 8f)
            setBackgroundColor(Color.TRANSPARENT)
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.apply {
                valueFormatter = if (hoursToShow >= 13) {
                    object : IndexAxisValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index % 2 == 0 && index in timeLabels.indices) {
                                timeLabels[index]
                            } else {
                                ""
                            }
                        }
                    }
                } else {
                    IndexAxisValueFormatter(timeLabels)
                }
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                setLabelCount(timeLabels.size, true)
                labelRotationAngle = -45f
                setDrawLabels(true)
                setDrawGridLines(false)
                setDrawAxisLine(true)
                textColor = labelColor
                textSize = 12f
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                setDrawAxisLine(true)
                setDrawLabels(true)
                textColor = labelColor
                textSize = 12f
                axisMinimum = 0f
                axisMaximum = 100f
                labelCount = 5
            }

            axisRight.isEnabled = false
        }
    }


    //#rain probability chart
    private fun updateRainChart(
        hoursToShow: Int,
        data: List<HourlyData>,
        chartCard: View,
        onlyTimeChanged: Boolean
    ) {
        val chart = chartCard.findViewById<LineChart>(R.id.rainChart)
        val rainChartBackground = chartCard.findViewById<LinearLayout>(R.id.rainChartBackground)

        val currentTime = nowLocationLocalT ?: LocalTime.now()


        val bgColorRes = SunTimesManager.getCurrentBackgroundColorRes()
        val textColorRes = SunTimesManager.getCurrentTextColorRes()

        val backgroundColor = ContextCompat.getColor(chartCard.context, bgColorRes)
        val textColor = ContextCompat.getColor(chartCard.context, textColorRes)

        rainChartBackground.setBackgroundColor(backgroundColor)

        val timeFormatPref = prefs.getString("time_format", "24") ?: "24"
        val timePattern = resolveTimeFormat(timeFormatPref)
        val formatter = DateTimeFormatter.ofPattern(timePattern)

        val displayCount = min(hoursToShow, data.size)

        if (onlyTimeChanged) {
            val timeLabels = ArrayList<String>()
            for (i in 0 until displayCount) {
                val hourData = data[i]
                val timeStr = ZonedDateTime.parse(
                    hourData.time,
                    DateTimeFormatter.ISO_DATE_TIME
                ).withZoneSameInstant(zoneId).format(formatter)
                timeLabels.add(timeStr)
            }

            chart.xAxis.valueFormatter = if (hoursToShow >= 13) {
                object : IndexAxisValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val index = value.toInt()
                        return if (index % 2 == 0 && index in timeLabels.indices) {
                            timeLabels[index]
                        } else {
                            ""
                        }
                    }
                }
            } else {
                IndexAxisValueFormatter(timeLabels)
            }

            // ✅ Update text colors for axis
            chart.xAxis.textColor = textColor
            chart.axisLeft.textColor = textColor

            chart.invalidate()
            return
        }

        val timeLabels = ArrayList<String>()
        val rainEntries = ArrayList<Entry>()

        for (i in 0 until displayCount) {
            val hourData = data[i]
            val rain = hourData.values.precipitationProbability ?: 0.0

            val timeStr = ZonedDateTime.parse(
                hourData.time,
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId).format(formatter)

            rainEntries.add(Entry(i.toFloat(), rain.toFloat()))
            timeLabels.add(timeStr)
        }

        val dataSet = LineDataSet(rainEntries, "Rain Probability (%)").apply {
            color = Color.parseColor("#2196F3")
            setCircleColor(Color.parseColor("#1976D2"))
            lineWidth = 2f


            valueTextColor = textColor
            valueTextSize = 10f

            highLightColor = Color.TRANSPARENT
            setDrawValues(false)
        }

        chart.xAxis.textColor = textColor
        chart.axisLeft.textColor = textColor

        chart.data = LineData(dataSet)
        chart.invalidate()
    }


    private fun setupChartCardViews(chartCard: View, weatherData: List<HourlyData>) {
        val toggle6h = chartCard.findViewById<ToggleButton>(R.id.toggle6h)
        val toggle13h = chartCard.findViewById<ToggleButton>(R.id.toggle13h)
        val title = chartCard.findViewById<TextView>(R.id.chartTitle)
        val chart = chartCard.findViewById<LineChart>(R.id.rainChart)
        val toggleContainer = chartCard.findViewById<LinearLayout>(R.id.toggleContainer)

        toggle13h.isChecked = true
        val toggleButtons = listOf(toggle6h, toggle13h)

        val currentTime = nowLocationLocalT ?: LocalTime.now()

        // 🎨 Dynamic text & background color
        val bgColorRes = SunTimesManager.getCurrentBackgroundColorRes()
        val textColorRes = SunTimesManager.getCurrentTextColorRes()

        val textColor = ContextCompat.getColor(chartCard.context, textColorRes)
        val backgroundColor = ContextCompat.getColor(chartCard.context, bgColorRes)

        // ✅ Apply dynamic text color
        title.setTextColor(textColor)

        // ✅ Apply dynamic background to the pill container
        applyPillBackground(toggleContainer, backgroundColor, textColor)


        // ✅ Preserve shape background while changing color
        val backgroundDrawable = chartCard.background?.mutate()
        if (backgroundDrawable is android.graphics.drawable.GradientDrawable) {
            backgroundDrawable.setColor(backgroundColor)
        }

        // 🕒 Time formatting
        val timeFormatPref = prefs.getString("time_format", "24") ?: "24"
        val timePattern = resolveTimeFormat(timeFormatPref)
        val formatter = DateTimeFormatter.ofPattern(timePattern)

        // ⏱ Initial time labels
        val timeLabels = ArrayList<String>()
        for (i in 0 until min(13, weatherData.size)) {
            val hourData = weatherData[i]
            val timeStr = ZonedDateTime.parse(
                hourData.time,
                DateTimeFormatter.ISO_DATE_TIME
            ).withZoneSameInstant(zoneId).format(formatter)
            timeLabels.add(timeStr)
        }

        // 🎯 Apply styles and update chart
        styleRainChart(chart, timeLabels, 13)
        updateRainChart(13, weatherData, chartCard, false)

        toggleButtons.forEach { toggle ->
            toggle.setOnClickListener {
                if (toggle.isChecked) {
                    toggleButtons.forEach { it.isChecked = (it == toggle) }
                    animateToggle(toggle)

                    selectedHours = if (toggle.id == R.id.toggle6h) 6 else 13

                    // ⏱ Updated time labels
                    val newTimeLabels = ArrayList<String>()
                    for (i in 0 until min(selectedHours, weatherData.size)) {
                        val hourData = weatherData[i]
                        val timeStr = ZonedDateTime.parse(
                            hourData.time,
                            DateTimeFormatter.ISO_DATE_TIME
                        ).withZoneSameInstant(zoneId).format(formatter)
                        newTimeLabels.add(timeStr)
                    }

                    styleRainChart(chart, newTimeLabels, selectedHours)
                    updateRainChart(selectedHours, weatherData, chartCard, false)
                } else {
                    toggle.isChecked = true
                }
            }
        }

        // ✅ Final line — safe to keep
        title.text = "Rain Probability"
    }

    // Helper function to apply dynamic colors to pill background
    private fun applyPillBackground(view: View, backgroundColor: Int, textColor: Int) {
        val pillDrawable = ContextCompat.getDrawable(view.context, R.drawable.pill_background)?.mutate()
        if (pillDrawable is GradientDrawable) {
            pillDrawable.setColor(backgroundColor)
            view.background = pillDrawable
        }
    }


    private fun animateToggle(view: View) {
        val scaleUpX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.1f)
        val scaleDownX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.1f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.1f, 1f)

        AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY)
            play(scaleDownX).after(scaleUpX)
            play(scaleDownY).after(scaleUpY)
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun applyTimeBasedTextColor() {
        val textColor = ContextCompat.getColor(requireContext(), SunTimesManager.getCurrentTextColorRes())
        binding.mainTempText.setTextColor(textColor)
        binding.mainConditionText.setTextColor(textColor)
        binding.locationDropdown.setTextColor(textColor)

        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.rotate_drawable)?.apply {
            val sizeInDp = 24
            val scale = resources.displayMetrics.density
            val sizeInPx = (sizeInDp * scale + 0.5f).toInt()
            setBounds(0, 0, sizeInPx, sizeInPx)
            setTint(textColor)
        }
        binding.locationDropdown.setCompoundDrawables(null, null, drawable, null)
    }


    private var lastAppliedTimeFormat: String? = null

    private fun renderWeatherUI() {
        // Comprehensive null checking at the start
        if (!isAdded) return
        val context = context ?: return
        if (_binding == null) return

        // Create local copies to avoid concurrent mutation issues
        val localSunriseT = sunriseT
        val localSunsetT = sunsetT
        val localDawnT = dawnT
        val localDuskT = duskT
        val localZoneId = zoneId
        val localNowLocationLocalT = nowLocationLocalT

        // Check if essential data is available using local copies
        if (localSunriseT == null || localSunsetT == null || localDawnT == null ||
            localDuskT == null || localZoneId == null) {
            Log.w("WeatherUI", "Essential sun time data not available yet")
            return
        }

        val hourLimit = prefs.getInt("hour_range", 25)
        val timeInterval = prefs.getInt("time_interval", 2)
        val mode = prefs.getString("detail_level", "All") ?: "All"
        val dateFormatPref = prefs.getString("date_format", "dd/MM") ?: "dd/MM"
        val timeFormatPref = prefs.getString("time_format", "24") ?: "24"


        val resolvedFormat = resolveTimeFormat(timeFormatPref)

// Only update if changed
        if (resolvedFormat != lastAppliedTimeFormat) {
            binding.sunPathView.timeFormatter = DateTimeFormatter.ofPattern(resolvedFormat)
            binding.sunPathView.invalidate()
            lastAppliedTimeFormat = resolvedFormat
        }

        // 🌙 StarView - with safe null checking using local copies
        val starView = view?.findViewById<StarView>(R.id.starView)
        starView?.visibility = if (localNowLocationLocalT?.isAfter(localDuskT) == true || localNowLocationLocalT?.isBefore(localDawnT) == true) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val weatherData = weatherViewModel.weatherData.value ?: return


        // ----- Stage 1: Quick render (main elements) -----
        if (weatherData.isNotEmpty()) {
            Log.d("WeatherUpdate", "Weather update UI logic triggered")

            val hourData = weatherData[0]
            val temp = hourData.values.temperature
            val code = hourData.values.weatherCode ?: 0
            val description = WeatherCodeUtils.getDescriptionForWeatherCode(code)

            // Quick updates first
            temp?.let { binding.mainTempText.text = String.format("%.1f°C", it) }
            binding.mainConditionText.text = description

            val forecastTime = LocalTime.now(localZoneId)
            val isNight = forecastTime.isBefore(localSunriseT) || forecastTime.isAfter(localSunsetT)
            val nightCapableCodes = setOf(1000, 1100, 1101)
            val adjustedCode = if (isNight && code in nightCapableCodes) code + 10 else code
            val iconResId = WeatherCodeUtils.getIconForWeatherCode(adjustedCode)

            binding.mainWeatherIcon.setImageResource(iconResId)

            binding.mainTempText.alpha = 0f
            binding.mainTempText.visibility = View.VISIBLE
            binding.mainConditionText.alpha = 0f
            binding.mainConditionText.visibility = View.VISIBLE
            binding.mainWeatherIcon.alpha = 0f
            binding.mainWeatherIcon.visibility = View.VISIBLE
            // Fade in main elements
            animateMainElements()
        }

        // ----- Stage 2: Offload heavy work to background -----

        lifecycleScope.launch {
            // Process data on background threads
            val dailySummariesData = processDailySummaryData(weatherData) // Heavy processing
            val adapter = prepareWeatherAdapter(weatherData, dateFormatPref, timeFormatPref, mode,
                hourLimit, timeInterval, localDawnT, localSunriseT, localSunsetT, localDuskT, localZoneId,
                nowLocationLocalT!! )



            // Switch to main thread for UI updates
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                // Inflate chart card on main thread
                val chartCard = layoutInflater.inflate(R.layout.weather_card_chart, null)
                setupChartCardViews(chartCard, weatherData)

                // Add chart card with animation
                binding.weatherContainer.removeAllViews()
                binding.weatherContainer.addView(chartCard)
                chartCard.alpha = 0f
                chartCard.animate().alpha(1f).setDuration(300).start()

                delay(100)

                // Setup recycler view
                binding.weatherRecycler.adapter = adapter
                binding.weatherRecycler.layoutManager = LinearLayoutManager(context)
                binding.weatherRecycler.alpha = 0f
                binding.weatherRecycler.animate().alpha(1f).setDuration(300).start()

                delay(100)

                // Render daily summaries with your preferred styling
                renderDailySummariesFromData(dailySummariesData, layoutInflater)

                // Add animations to daily summaries
                val dailyContainer = binding.dailySummaryContainer
                for (i in 0 until dailyContainer.childCount) {
                    val dayView = dailyContainer.getChildAt(i)
                    dayView.alpha = 0f
                    dayView.translationY = 20f

                    dayView.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(300)
                        .setStartDelay(i * 50L)
                        .start()
                }
            }
        }
    }



    private suspend fun processDailySummaryData(
        weatherData: List<HourlyData>
    ): List<DailySummaryResult> = withContext(Dispatchers.Default) {
        val summaries = mutableListOf<DailySummaryResult>()
        val localZoneId = zoneId ?: return@withContext summaries

        val dailySummary = mutableMapOf<String, MutableList<Triple<Double, Int, Double>>>()

        // Process weather data to create daily summaries (heavy computation)
        weatherData.forEach { hourData ->
            val time = ZonedDateTime.parse(hourData.time, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(localZoneId)

            val hour = time.hour
            if (hour in 6..18) { // Only consider daytime hours
                val day = time.dayOfWeek.name.substring(0, 3)
                val values = hourData.values
                val rain = values.precipitationProbability ?: 0.0
                val code = values.weatherCode ?: 0
                val temp = values.temperature ?: Double.NaN
                dailySummary.getOrPut(day) { mutableListOf() }.add(Triple(rain, code, temp))
            }
        }

        val today = ZonedDateTime.now(localZoneId).dayOfWeek
        val orderedDays = (0..6).map { today.plus(it.toLong()).name.substring(0, 3) }
        val displayableDays = orderedDays.filter { day -> !dailySummary[day].isNullOrEmpty() }

        // Create data objects only (no view inflation)
        for (day in displayableDays) {
            val readings = dailySummary[day] ?: continue

            val avgRain = readings.map { it.first }.average().toInt()
            val iconCode = readings.groupingBy { it.second }.eachCount()
                .maxByOrNull { it.value }?.key ?: 0
            val temps = readings.map { it.third }.filter { !it.isNaN() }
            val avgTemp = if (temps.isNotEmpty()) temps.average() else Double.NaN

            summaries.add(DailySummaryResult(day, avgRain, iconCode, avgTemp))
        }

        summaries
    }

    private fun renderDailySummariesFromData(
        dailySummaries: List<DailySummaryResult>,
        layoutInflater: LayoutInflater
    ) {
        val dailyContainer = binding.dailySummaryContainer
        dailyContainer.removeAllViews()

        val currentTime = nowLocationLocalT ?: LocalTime.now()

        // Determine background and text colors based on time
        val textColorRes = SunTimesManager.getCurrentTextColorRes()
        val bgColorRes = SunTimesManager.getCurrentBackgroundColorRes()

        val textColor = ContextCompat.getColor(requireContext(), textColorRes)
        val backgroundColor = ContextCompat.getColor(requireContext(), bgColorRes)


        for ((index, data) in dailySummaries.withIndex()) {
            val dayView = layoutInflater.inflate(R.layout.item_daily_summary, dailyContainer, false)

            // ✅ Change background color without losing corner radius
            val backgroundDrawable = dayView.background?.mutate()
            if (backgroundDrawable is android.graphics.drawable.GradientDrawable) {
                backgroundDrawable.setColor(backgroundColor)
            }

            val layoutParams = dayView.layoutParams as? LinearLayout.LayoutParams
                ?: LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

            val margin6dp = 6.dp
            val isFirst = index == 0
            val isLast = index == dailySummaries.lastIndex

            layoutParams.setMargins(
                if (isFirst) 0 else margin6dp,
                margin6dp,
                if (isLast) 0 else margin6dp,
                margin6dp
            )

            dayView.layoutParams = layoutParams

            val dayLabel = dayView.findViewById<TextView>(R.id.dayLabel)
            val rainText = dayView.findViewById<TextView>(R.id.probabilityText)
            val tempText = dayView.findViewById<TextView>(R.id.tempText)

            dayLabel.text = data.day.lowercase().replaceFirstChar { it.uppercase() }
            rainText.text = "🌧 ${data.avgRain}%"
            tempText.text = if (!data.avgTemp.isNaN()) "${data.avgTemp.toInt()}°C" else "--°C"

            // 🎨 Apply dynamic text color
            dayLabel.setTextColor(textColor)
            rainText.setTextColor(textColor)
            tempText.setTextColor(textColor)

            val iconRes = WeatherCodeUtils.getIconForWeatherCode(data.iconCode)
            dayView.findViewById<ImageView>(R.id.dayIcon)
                .setImageResource(if (iconRes != 0) iconRes else R.drawable.unknown)

            dailyContainer.addView(dayView)
        }
    }


    // Data class for daily summaries
    data class DailySummaryResult(
        val day: String,
        val avgRain: Int,
        val iconCode: Int,
        val avgTemp: Double
    )

    private suspend fun prepareWeatherAdapter(
        weatherData: List<HourlyData>,
        dateFormatPref: String,
        timeFormatPref: String,
        mode: String,
        hourLimit: Int,
        timeInterval: Int,
        dawnTime: LocalTime?,
        sunriseTime: LocalTime?,
        sunsetTime: LocalTime?,
        duskTime: LocalTime?,
        zoneId: ZoneId?,
        currentLocalTime: LocalTime
    ): WeatherAdapter = withContext(Dispatchers.Default) {
        val safeDawnTime = dawnTime ?: LocalTime.of(5, 30)     // Default dawn 5:30 AM
        val safeSunriseTime = sunriseTime ?: LocalTime.of(6, 0)
        val safeSunsetTime = sunsetTime ?: LocalTime.of(18, 0)
        val safeDuskTime = duskTime ?: LocalTime.of(19, 30)    // Default dusk 7:30 PM
        val safeZoneId = zoneId ?: ZoneId.systemDefault()

        WeatherAdapter(
            data = weatherData,
            layoutInflater = LayoutInflater.from(context),
            dateFormatPref = dateFormatPref,
            timeFormatPref = timeFormatPref,
            dawnTime = safeDawnTime,
            sunriseTime = safeSunriseTime,
            sunsetTime = safeSunsetTime,
            duskTime = safeDuskTime,
            mode = mode,
            zoneId = safeZoneId,
            hourLimit = hourLimit,
            timeInterval = timeInterval,
            currentLocalTime = currentLocalTime    // pass the current local time here
        )
    }

    private fun animateMainElements() {
        val animatorSet = AnimatorSet()

        val fadeInMain = ObjectAnimator.ofFloat(binding.mainTempText, "alpha", 0f, 1f)
        val fadeInCondition = ObjectAnimator.ofFloat(binding.mainConditionText, "alpha", 0f, 1f)
        val fadeInIcon = ObjectAnimator.ofFloat(binding.mainWeatherIcon, "alpha", 0f, 1f)



        animatorSet.playTogether(fadeInMain, fadeInCondition, fadeInIcon)
        animatorSet.duration = 300
        animatorSet.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()


//
//    private fun createDailySummaryViews(
//        dailySummariesData: List<DailySummaryData>,
//        layoutInflater: LayoutInflater
//    ): List<View> {
//        val views = mutableListOf<View>()
//
//        for ((index, data) in dailySummariesData.withIndex()) {
//            val dayView = layoutInflater.inflate(R.layout.item_daily_summary, null)
//
//            dayView.findViewById<TextView>(R.id.dayLabel).text =
//                data.day.lowercase().replaceFirstChar { it.uppercase() }
//            dayView.findViewById<TextView>(R.id.probabilityText).text = "🌧 ${data.avgRain}%"
//            dayView.findViewById<TextView>(R.id.tempText).text =
//                if (!data.avgTemp.isNaN()) "${data.avgTemp.toInt()}°C" else "--°C"
//
//            val iconRes = WeatherCodeUtils.getIconForWeatherCode(data.iconCode)
//            dayView.findViewById<ImageView>(R.id.dayIcon)
//                .setImageResource(if (iconRes != 0) iconRes else R.drawable.unknown)
//
//            // Set layout params
//            val layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//
//            val margin6dp = 6.dp
//            val isFirst = index == 0
//            val isLast = index == dailySummariesData.lastIndex
//
//            layoutParams.setMargins(
//                if (isFirst) 0 else margin6dp,
//                margin6dp,
//                if (isLast) 0 else margin6dp,
//                margin6dp
//            )
//
//            dayView.layoutParams = layoutParams
//            views.add(dayView)
//        }
//
//        return views
//    }
//
//
//
//    private suspend fun prepareDailySummariesData(
//        weatherData: List<HourlyData>
//    ): List<DailySummaryData> = withContext(Dispatchers.Default) {
//        val summaries = mutableListOf<DailySummaryData>()
//
//        // Create local copies to avoid concurrency issues
//        val localZoneId = zoneId ?: return@withContext summaries
//        val localSunriseT = sunriseT
//        val localSunsetT = sunsetT
//
//        if (localSunriseT == null || localSunsetT == null) return@withContext summaries
//
//        val dailySummary = mutableMapOf<String, MutableList<Triple<Double, Int, Double>>>()
//
//        // Process weather data to create daily summaries (data only, no views)
//        weatherData.forEach { hourData ->
//            val time = ZonedDateTime.parse(hourData.time, DateTimeFormatter.ISO_DATE_TIME)
//                .withZoneSameInstant(localZoneId)
//
//            val hour = time.hour
//            if (hour in 6..18) { // Only consider daytime hours
//                val day = time.dayOfWeek.name.substring(0, 3)
//                val values = hourData.values
//                val rain = values.precipitationProbability ?: 0.0
//                val code = values.weatherCode ?: 0
//                val temp = values.temperature ?: Double.NaN
//                dailySummary.getOrPut(day) { mutableListOf() }.add(Triple(rain, code, temp))
//            }
//        }
//
//        val today = ZonedDateTime.now(localZoneId).dayOfWeek
//        val orderedDays = (0..6).map { today.plus(it.toLong()).name.substring(0, 3) }
//        val displayableDays = orderedDays.filter { day -> !dailySummary[day].isNullOrEmpty() }
//
//        // Create data objects only (no view inflation)
//        for (day in displayableDays) {
//            val readings = dailySummary[day] ?: continue
//
//            val avgRain = readings.map { it.first }.average().toInt()
//            val iconCode = readings.groupingBy { it.second }.eachCount()
//                .maxByOrNull { it.value }?.key ?: 0
//            val temps = readings.map { it.third }.filter { !it.isNaN() }
//            val avgTemp = if (temps.isNotEmpty()) temps.average() else Double.NaN
//
//            summaries.add(DailySummaryData(day, avgRain, iconCode, avgTemp))
//        }
//
//        summaries
//    }

//fun updateTimeFormatInCards(container: ViewGroup, dateFormat: String, timeFormat: String) {
//    val formatterInput = DateTimeFormatter.ISO_DATE_TIME
//    val datePattern = if (dateFormat == "MM/dd") "MM/dd" else "dd/MM"
//    val timePattern = if (timeFormat == "12") "hh:mma" else "HH:mm"
//    val formatterOutput = DateTimeFormatter.ofPattern("$datePattern $timePattern")
//
//    for (i in 0 until container.childCount) {
//        val view = container.getChildAt(i) ?: continue
//
//        // Only update cards with the weather time tag
//        val utcTime = view.getTag(R.id.tag_weather_time) as? String ?: continue
//
//        val deviceTime = ZonedDateTime.parse(utcTime, formatterInput)
//            .withZoneSameInstant(ZoneId.systemDefault())
//        val formatted = deviceTime.format(formatterOutput)
//
//        view.findViewById<TextView>(R.id.timeText)?.text = formatted
//    }
//}

//private fun saveWeatherJsonIfNotRecent(responseBody: String) {
//    try {
//        val filename = "weather_${System.currentTimeMillis()}.json"
//        val file = requireContext().filesDir.resolve(filename)
//        file.writeText(responseBody)
//        Log.d("WeatherAPI", "JSON saved to ${file.absolutePath}")
//    } catch (e: Exception) {
//        Log.e("WeatherAPI", "Error saving weather JSON", e)
//    }
//}

//    private fun updateSpinnerTextColor() {
//        val locationNames = locations.keys.toList()
//        val colorResId = getCurrentTextColorRes()
//        val textColor = ContextCompat.getColor(requireContext(), colorResId)
//
//        val adapter = object : ArrayAdapter<String>(
//            requireContext(),
//            android.R.layout.simple_spinner_item,
//            locationNames
//        ) {
//            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//                val view = super.getView(position, convertView, parent)
//                (view as? TextView)?.setTextColor(textColor)
//                return view
//            }
//
//            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
//                val view = super.getDropDownView(position, convertView, parent)
//                (view as? TextView)?.setTextColor(textColor)
//                return view
//            }
//        }
//
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
//        binding.locationSpinner.adapter = adapter
//
//        // Restore selection
//        val savedLocation = prefs.getString("selected_location_name", "Farm")
//        val defaultIndex = locationNames.indexOf(savedLocation)
//        if (defaultIndex != -1) {
//            binding.locationSpinner.setSelection(defaultIndex)
//        }
//    }


//    private fun loadWeatherForLocation(location: String) {
//        val apiKey = BuildConfig.TOMORROW_API_KEY
//        Log.d("WeatherDebug", "API Key: $apiKey")
//
//        lifecycleScope.launch {
//            try {
//                // Retrofit call (suspend function)
//                val response = RetrofitClient.weatherApi.getForecast(
//                    location = location,
//                    apiKey = apiKey
//                )
//
//                // Save data (assuming your API returns something like ForecastResponse)
//                fullWeatherData = response.timelines.hourly
//
//                // Update limited array
//                updateLimitedArrayFromPrefs()
//
//                // Update UI (already on main thread with lifecycleScope, but ok to be explicit)
//                withContext(Dispatchers.Main) {
//                    renderWeatherUI()
//                }
//            } catch (e: Exception) {
//                Log.e("WeatherAPI", "Error loading weather", e)
//
//                withContext(Dispatchers.Main) {
//                    activity?.showError("Failed to load weather: ${e.message}")
//                }
//            }
//        }
//    }


//private fun renderDailySummaries(
//    weatherData: List<HourlyData>,
//    layoutInflater: LayoutInflater
//) {
//    val dailySummary = mutableMapOf<String, MutableList<Triple<Double, Int, Double>>>()
//    weatherData.forEach { hourData ->
//        val time = ZonedDateTime.parse(hourData.time, DateTimeFormatter.ISO_DATE_TIME)
//            .withZoneSameInstant(zoneId)
//
//        val hour = time.hour
//        if (hour in 6..18) {
//            val day = time.dayOfWeek.name.substring(0, 3)
//            val values = hourData.values
//            val rain = values.precipitationProbability ?: 0.0
//            val code = values.weatherCode ?: 0
//            val temp = values.temperature ?: Double.NaN
//            dailySummary.getOrPut(day) { mutableListOf() }.add(Triple(rain, code, temp))
//        }
//    }
//
//    val dailyContainer = binding.dailySummaryContainer
//    dailyContainer.removeAllViews()
//
//    val today = ZonedDateTime.now(zoneId).dayOfWeek
//    val orderedDays = (0..6).map { today.plus(it.toLong()).name.substring(0, 3) }
//    val displayableDays = orderedDays.filter { day -> !dailySummary[day].isNullOrEmpty() }
//
//    for ((index, day) in displayableDays.withIndex()) {
//        val readings = dailySummary[day] ?: continue
//
//        val avgRain = readings.map { it.first }.average().toInt()
//        val iconCode = readings.groupingBy { it.second }.eachCount()
//            .maxByOrNull { it.value }?.key ?: 0
//        val temps = readings.map { it.third }.filter { !it.isNaN() }
//        val avgTemp = if (temps.isNotEmpty()) temps.average() else Double.NaN
//
//        val dayView = layoutInflater.inflate(
//            R.layout.item_daily_summary,
//            dailyContainer,
//            false
//        )
//
//        val layoutParams = dayView.layoutParams as? LinearLayout.LayoutParams
//            ?: LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.WRAP_CONTENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//
//        val margin6dp = 6.dp
//        val isFirst = index == 0
//        val isLast = index == displayableDays.lastIndex
//
//        layoutParams.setMargins(
//            if (isFirst) 0 else margin6dp,
//            margin6dp,
//            if (isLast) 0 else margin6dp,
//            margin6dp
//        )
//
//        dayView.layoutParams = layoutParams
//        dayView.findViewById<TextView>(R.id.dayLabel).text =
//            day.lowercase().replaceFirstChar { it.uppercase() }
//        dayView.findViewById<TextView>(R.id.probabilityText).text = "🌧 $avgRain%"
//        dayView.findViewById<TextView>(R.id.tempText).text =
//            if (!avgTemp.isNaN()) "${avgTemp.toInt()}°C" else "--°C"
//
//        val iconRes = WeatherCodeUtils.getIconForWeatherCode(iconCode)
//        dayView.findViewById<ImageView>(R.id.dayIcon)
//            .setImageResource(if (iconRes != 0) iconRes else R.drawable.unknown)
//
//        dailyContainer.addView(dayView)
//    }
//}

//private var lastHourLimit: Int? = null
//private var lastDateFormat: String? = null
//private var lastTimeFormat: String? = null
//private var lastDetailMode: String? = null
//private var lastTimeInterval: Int? = null // NEW

//fun logSpecificPrefs(prefs: SharedPreferences) {
//    val selectedLocation = prefs.getString("selected_location_name", "Not set")
//    val userLocationsJson = prefs.getString("user_locations_json", "Not set")
//
//    Log.d("PrefsCheck", "selected_location_name: $selectedLocation")
//    Log.d("PrefsCheck", "user_locations_json: $userLocationsJson")
//}

//    logSpecificPrefs(prefs)

//        weatherViewModel.weatherData.observe(viewLifecycleOwner) { weatherList ->
//            if (!weatherList.isNullOrEmpty()) {
//                lifecycleScope.launch {
//                    renderWeatherUI()  // offloads heavy work
//                }
//            }
//        }

//        weatherViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
//            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
