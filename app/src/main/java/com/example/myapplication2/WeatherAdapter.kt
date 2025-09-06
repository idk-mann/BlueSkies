package com.example.myapplication2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication2.utils.WeatherCodeUtils
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import com.example.myapplication2.data.model.HourlyData
import com.example.myapplication2.utils.TimeBasedColors

class  WeatherAdapter(
    data: List<HourlyData>,
    private val layoutInflater: LayoutInflater,
    private var dateFormatPref: String,
    private var timeFormatPref: String,
    private val dawnTime: LocalTime,
    private val sunriseTime: LocalTime,
    private val sunsetTime: LocalTime,
    private val duskTime: LocalTime,
    private val mode: String,
    private val zoneId: ZoneId,
    private val hourLimit: Int,
    private val timeInterval: Int,
    private val currentLocalTime: LocalTime    // NEW PARAMETER here
) : RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder>() {


    private var recyclerView: RecyclerView? = null

    // filter data based on user settings
    private val filteredData: List<HourlyData> = data
        .take(hourLimit) // apply hour range limit
        .filterIndexed { index, _ -> index % timeInterval == 0 } // apply interval

    private val showBasic: Boolean = mode == "Basic" || mode == "All"
    private val showAdvanced: Boolean = mode == "Advanced" || mode == "All"

    inner class WeatherViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(hourData: HourlyData) {
            val values = hourData.values

            val temp = values.temperature ?: Double.NaN
            val precip = values.precipitationProbability ?: Double.NaN
            val wind = values.windSpeed ?: Double.NaN
            val humidity = values.humidity ?: Double.NaN
            val uv = values.uvIndex ?: Double.NaN
            val cloudCover = values.cloudCover ?: Double.NaN
            val rainIntensity = values.rainIntensity ?: Double.NaN
            val cloudBase = values.cloudBase ?: Double.NaN
            val code = values.weatherCode ?: 0
            val description = WeatherCodeUtils.getDescriptionForWeatherCode(code)

            val isLikelyRainSoon = precip > 40 && rainIntensity > 0.2 && cloudCover > 60 &&
                    (cloudBase < 2.0 || cloudBase.isNaN())
            val prediction = when {
                isLikelyRainSoon -> "\uD83C\uDF27️ Likely Rain Soon"
                precip > 20 -> "\uD83C\uDF25️ Slight Chance of Rain"
                else -> "\u2600️ No Rain Expected"
            }

            val utcTime = hourData.time
            val deviceTime = ZonedDateTime.parse(utcTime, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(zoneId)

            val datePattern = if (dateFormatPref == "MM/dd") "MM/dd" else "dd/MM"
            val timePattern = if (timeFormatPref == "12") "hh:mma" else "HH:mm"
            val timeStr = deviceTime.format(DateTimeFormatter.ofPattern("$datePattern $timePattern"))

            view.findViewById<TextView>(R.id.timeText).text = timeStr
            view.findViewById<TextView>(R.id.tempText).text = "Temp: %.1f°C".format(temp)
            view.findViewById<TextView>(R.id.rainText).text = "Rain: %.0f%%".format(precip)
            view.findViewById<TextView>(R.id.rainIntensityText).text =
                if (!rainIntensity.isNaN()) "Rain Intensity: %.1f mm/h".format(rainIntensity) else "Rain Intensity: --"
            view.findViewById<TextView>(R.id.windText).text = "Wind: %.1f m/s".format(wind)
            view.findViewById<TextView>(R.id.humidityText).text = "Humidity: %.0f%%".format(humidity)
            view.findViewById<TextView>(R.id.uvText).text = "UV Index: $uv"
            view.findViewById<TextView>(R.id.cloudBaseText).text =
                if (!cloudBase.isNaN()) "Cloud Base: %.1f km".format(cloudBase) else "Cloud Base: --"
            view.findViewById<TextView>(R.id.cloudCoverText).text = "Cloud Cover: %.0f%%".format(cloudCover)
            view.findViewById<TextView>(R.id.conditionText).text = "$description ($prediction)"

            val forecastTime = deviceTime.toLocalTime()
            val isNight = forecastTime.isBefore(sunriseTime) || forecastTime.isAfter(sunsetTime)
            val adjustedCode = if (isNight && code in setOf(1000, 1100, 1101)) code + 10 else code
            val iconResId = WeatherCodeUtils.getIconForWeatherCode(adjustedCode)
            view.findViewById<ImageView>(R.id.weatherIcon).setImageResource(iconResId)

            fun View.showIf(condition: Boolean) {
                visibility = if (condition) View.VISIBLE else View.GONE
            }

            val bgColorRes = TimeBasedColors.getCurrentBackgroundColorRes(
                now = currentLocalTime,
                dawnT = dawnTime,
                sunriseT = sunriseTime,
                sunsetT = sunsetTime,
                duskT = duskTime
            )

            val textColorRes = TimeBasedColors.getCurrentTextColorRes(
                now = currentLocalTime,
                dawnT = dawnTime,
                sunriseT = sunriseTime,
                sunsetT = sunsetTime,
                duskT = duskTime
            )

            val cardView = view.findViewById<CardView>(R.id.weatherCardBackground)
            val bgColor = ContextCompat.getColor(view.context, bgColorRes)
            cardView.setCardBackgroundColor(bgColor)

            val textColor = ContextCompat.getColor(view.context, textColorRes)

// Apply text color to relevant TextViews
            listOf(
                R.id.timeText,
                R.id.tempText,
                R.id.rainText,
                R.id.windText,
                R.id.rainIntensityText,
                R.id.humidityText,
                R.id.uvText,
                R.id.cloudBaseText,
                R.id.cloudCoverText,
                R.id.conditionText
            ).forEach { id ->
                view.findViewById<TextView?>(id)?.setTextColor(textColor)
            }


            // Show/hide based on detail level
            view.findViewById<View>(R.id.timeText)?.showIf(showBasic || showAdvanced)
            view.findViewById<View>(R.id.tempText)?.showIf(showBasic || showAdvanced)
            view.findViewById<View>(R.id.rainText)?.showIf(showBasic)
            view.findViewById<View>(R.id.windText)?.showIf(showBasic)
            view.findViewById<View>(R.id.rainIntensityText)?.showIf(showAdvanced)
            view.findViewById<View>(R.id.humidityText)?.showIf(showAdvanced)
            view.findViewById<View>(R.id.uvText)?.showIf(showAdvanced)
            view.findViewById<View>(R.id.cloudBaseText)?.showIf(showAdvanced)
            view.findViewById<View>(R.id.cloudCoverText)?.showIf(showAdvanced)
            view.findViewById<View>(R.id.conditionText)?.showIf(showAdvanced)
        }
    }

    fun updateTimeFormat(newDateFormat: String, newTimeFormat: String) {
        dateFormatPref = newDateFormat
        timeFormatPref = newTimeFormat

        val datePattern = if (newDateFormat == "MM/dd") "MM/dd" else "dd/MM"
        val timePattern = if (newTimeFormat == "12") "hh:mma" else "HH:mm"
        val formatter = DateTimeFormatter.ofPattern("$datePattern $timePattern")

        for (i in 0 until itemCount) {
            val holder = recyclerView?.findViewHolderForAdapterPosition(i) as? WeatherViewHolder ?: continue

            val utcTime = filteredData[i].time
            val deviceTime = ZonedDateTime.parse(utcTime, DateTimeFormatter.ISO_DATE_TIME)
                .withZoneSameInstant(ZoneId.systemDefault())
            val formatted = deviceTime.format(formatter)

            holder.view.findViewById<TextView>(R.id.timeText)?.text = formatted
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val view = layoutInflater.inflate(R.layout.weather_card, parent, false)
        return WeatherViewHolder(view)
    }

    override fun getItemCount(): Int = filteredData.size

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        holder.bind(filteredData[position])
    }

    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        super.onAttachedToRecyclerView(rv)
        recyclerView = rv
    }
}
