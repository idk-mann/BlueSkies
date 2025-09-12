package com.example.myapplication2

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.math.*
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.format.DateTimeFormatter

import com.example.myapplication2.utils.SunTimesManager

class SunPathView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var sunriseTimes: List<LocalTime> = emptyList()
    var sunsetTimes: List<LocalTime> = emptyList()
    var dawnTimes: List<LocalTime> = emptyList()
    var duskTimes: List<LocalTime> = emptyList()
    var timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    var currentLocalTime: LocalTime = LocalTime.now()

    var animationFraction: Float = 1f
        set(value) {
            field = value
            invalidate()
        }

    private val totalHours = 36  // Show 36 hours (yesterday + today + tomorrow)
    private val hourWidth = 60f  // Width of each hour in pixels

    private val sunPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dayPaint = Paint().apply {
        color = Color.parseColor("#FFD54F")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val nightPaint = Paint().apply {
        color = Color.parseColor("#37474F")
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val axisPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 30f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    fun updatePaintColors(context: Context) {
        val colorResId = SunTimesManager.getCurrentTextColorRes()
        val resolvedColor = ContextCompat.getColor(context, colorResId)

        textPaint.color = resolvedColor
        linePaint.color = resolvedColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (totalHours * hourWidth).toInt()
        val desiredHeight = 300
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    //start of draw moon function
    fun drawCrescentMoon(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        val crescentPath = Path()

        // Draw full moon
        crescentPath.addCircle(cx, cy, radius, Path.Direction.CW)

        // Cut out inner part to form crescent shape
        val cutoutPath = Path()
        cutoutPath.addCircle(cx + radius / 2.5f, cy - radius / 6f, radius * 0.85f, Path.Direction.CCW)

        // Subtract the cutout from full moon
        crescentPath.op(cutoutPath, Path.Op.DIFFERENCE)

        canvas.drawPath(crescentPath, paint)
    }

    val moonPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
        isAntiAlias = true
        clearShadowLayer()
        maskFilter = null
    }
    //end of draw moon function

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (sunriseTimes.isEmpty() || sunsetTimes.isEmpty()) return



        val w = width.toFloat()
        val h = height.toFloat()
        val padding = 40f
        val centerY = h / 2f
        val amplitude = (h - 2 * padding) / 3f
        val points = 600
        val dx = w / points

        val nowMin = currentLocalTime.toSecondOfDay() / 60f
        val graphStartMin = nowMin - 720f
        val graphEndMin = graphStartMin + totalHours * 60f
        val totalGraphMinutes = graphEndMin - graphStartMin

        // Calculate the current visible start X based on animationFraction
        // animationFraction == 0 -> drawStartX == w (nothing visible)
        // animationFraction == 1 -> drawStartX == 0 (full path visible)
        val drawStartX = w * (1f - animationFraction)

        var lastX = drawStartX
        var lastY = centerY

        val visualSkipEnd = graphStartMin + 15f
        val intersections = mutableListOf<Pair<Float, Float>>()

            fun getCycleMinutes(index: Int): Quadruple<Float, Float, Float, Float> {
            val sunriseMin = sunriseTimes.getOrNull(index)?.toSecondOfDay()?.div(60f) ?: 360f
            val sunsetMin = sunsetTimes.getOrNull(index)?.toSecondOfDay()?.div(60f) ?: 1080f
            val dawnMin = dawnTimes.getOrNull(index)?.toSecondOfDay()?.div(60f) ?: 330f
            val duskMin = duskTimes.getOrNull(index)?.toSecondOfDay()?.div(60f) ?: 1110f
            return Quadruple(sunriseMin, sunsetMin, dawnMin, duskMin)
        }

        for (i in 1..points) {
            val x = i * dx
            if (x < drawStartX) {
                // Skip drawing points before the current visible start
                continue
            }

            val tMinutes = graphStartMin + (i / points.toFloat()) * totalGraphMinutes

            val dayIndex = ((tMinutes + 720) / 1440f).toInt().coerceIn(0, sunriseTimes.size - 1)
            val (sunriseMin, sunsetMin, _, _) = getCycleMinutes(dayIndex)

            val dayTime = (tMinutes % 1440f + 1440f) % 1440f
            val isDaytime = dayTime in sunriseMin..sunsetMin

            val y = if (isDaytime) {
                val dayDuration = (sunsetMin - sunriseMin).takeIf { it > 0f } ?: 720f
                val progress = (dayTime - sunriseMin) / dayDuration
                val angle = progress * Math.PI
                centerY - (amplitude * sin(angle)).toFloat()
            } else {
                val nextSunrise = if (dayTime > sunsetMin) {
                    sunriseTimes.getOrNull(dayIndex + 1)?.toSecondOfDay()?.div(60f)
                        ?: (sunriseMin + 1440f)
                } else {
                    sunriseTimes.getOrNull(dayIndex)?.toSecondOfDay()?.div(60f)
                        ?: sunriseMin
                }
                val nightStart = sunsetMin
                val nightEnd = nextSunrise
                val nightDuration = (nightEnd - nightStart + 1440f) % 1440f
                val adjustedDuration = if (nightDuration <= 0f) 1440f - sunsetMin + sunriseMin else nightDuration

                val progress = if (dayTime > sunsetMin) {
                    (dayTime - nightStart) / adjustedDuration
                } else {
                    (dayTime + (1440f - nightStart)) / adjustedDuration
                }
                val angle = progress * Math.PI
                centerY + (amplitude * sin(angle)).toFloat()
            }

            // Draw line segment only if lastX is initialized (first iteration skips)
            if (lastX != drawStartX) {
                val paint = if (isDaytime) dayPaint else nightPaint
                canvas.drawLine(lastX, lastY, x, y, paint)
            }

            lastX = x
            lastY = y
        }

        val skipEndX = ((visualSkipEnd - graphStartMin) / totalGraphMinutes) * w
        canvas.drawLine(skipEndX, centerY, w, centerY, axisPaint)



        fun drawEventMarker(label: String, timeMin: Float, isAbove: Boolean) {
            val x = ((timeMin - graphStartMin) / totalGraphMinutes) * w
            val dayIndex = ((timeMin + 720) / 1440f).toInt().coerceIn(0, sunriseTimes.size - 1)
            val (sunriseMin, sunsetMin, _, _) = getCycleMinutes(dayIndex)

            val timeOfDayMin = ((timeMin % 1440f + 1440f) % 1440f)

            // Decide whether the event falls during day or night
            val isDaytimeMarker = timeOfDayMin in sunriseMin..sunsetMin
            val y = if (isDaytimeMarker) {
                val dayDuration = (sunsetMin - sunriseMin).takeIf { it > 0f } ?: 720f
                val progress = ((timeOfDayMin - sunriseMin) / dayDuration).coerceIn(0f, 1f)
                val angle = progress * PI
                centerY - (amplitude * sin(angle)).toFloat()
            } else {
                // NIGHTTIME Y-CALCULATION LOGIC
                val nextSunrise = if (timeOfDayMin > sunsetMin) {
                    sunriseTimes.getOrNull(dayIndex + 1)?.toSecondOfDay()?.div(60f)
                        ?: (sunriseMin + 1440f)
                } else {
                    sunriseTimes.getOrNull(dayIndex)?.toSecondOfDay()?.div(60f)
                        ?: sunriseMin
                }

                val nightStart = sunsetMin
                val nightEnd = nextSunrise
                val nightDuration = (nightEnd - nightStart + 1440f) % 1440f
                val adjustedDuration = if (nightDuration <= 0f) 1440f - sunsetMin + sunriseMin else nightDuration

                val progress = if (timeOfDayMin > sunsetMin) {
                    (timeOfDayMin - nightStart) / adjustedDuration
                } else {
                    (timeOfDayMin + (1440f - nightStart)) / adjustedDuration
                }

                val angle = progress * PI
                centerY + (amplitude * sin(angle)).toFloat()
            }

            // Draw marker line
            val markerHeight = 80f
            canvas.drawLine(x, y, x, if (isAbove) y - markerHeight else y + markerHeight, linePaint)

            // Label
            val labelY = if (isAbove) y - (markerHeight + 10f) else y + (markerHeight + 30f)
            val displayTime = ((timeMin % 1440f + 1440f) % 1440f) * 60
            val displayLocalTime = LocalTime.ofSecondOfDay(displayTime.toLong()).truncatedTo(ChronoUnit.MINUTES)
            val timeLabel = displayLocalTime.format(timeFormatter)

            canvas.drawText("$label $timeLabel", x, labelY, textPaint)

        }

        // Draw current time
        val nowX = ((nowMin - graphStartMin) / totalGraphMinutes) * w
        val nowDayTime = (nowMin % 1440f + 1440f) % 1440f
        val nowDayIndex = ((nowMin + 720) / 1440f).toInt().coerceIn(0, sunriseTimes.size - 1)

        val (sunriseNow, sunsetNow, _, _) = getCycleMinutes(nowDayIndex)
        val isNowDaytime = nowDayTime in sunriseNow..sunsetNow


        val nowY = if (isNowDaytime) {
            val dayDuration = (sunsetNow - sunriseNow).takeIf { it > 0f } ?: 720f
            val progress = (nowDayTime - sunriseNow) / dayDuration
            val angle = progress * PI
            centerY - (amplitude * sin(angle)).toFloat()
        } else {

            val rawNextSunrise = sunriseTimes.getOrNull(nowDayIndex + 1)?.toSecondOfDay()?.div(60f)
            val nextSunrise = if (rawNextSunrise != null && rawNextSunrise < sunsetNow) {
                rawNextSunrise + 1440f
            } else {
                rawNextSunrise ?: (sunriseNow + 1440f)
            }
            Log.d("SunPathDebug", "nextSunrise (adjusted) = $nextSunrise")

            val nightDuration = if (nowDayTime > sunsetNow) {
                nextSunrise - sunsetNow
            } else {
                (1440f - sunsetNow) + sunriseNow
            }
            Log.d("SunPathDebug", "nightDuration = $nightDuration")

            val progress = if (nowDayTime > sunsetNow) {
                (nowDayTime - sunsetNow) / nightDuration
            } else {
                (nowDayTime + (1440f - sunsetNow)) / nightDuration
            }
            Log.d("SunPathDebug", "night progress = $progress")

            val angle = progress * PI
            Log.d("SunPathDebug", "night angle (rad) = $angle")
            centerY + (amplitude * sin(angle)).toFloat()
        }

        if (isNowDaytime) {
            canvas.drawCircle(nowX, nowY, 16f, sunPaint)
        } else {
            drawCrescentMoon(canvas, nowX, nowY, 16f, moonPaint)
        }
        canvas.drawText("Now", nowX, nowY - 30f, textPaint)

        sunriseTimes.forEachIndexed { i, t ->
            val timeMin = t.toSecondOfDay() / 60f + (i - 1) * 1440f

            drawEventMarker("Sunrise", timeMin, false)
        }



        sunsetTimes.forEachIndexed { i, t ->
            val timeMin = t.toSecondOfDay() / 60f + (i - 1) * 1440f

            drawEventMarker("Sunset", timeMin, false)
        }

        dawnTimes.forEachIndexed { i, t ->
            val timeMin = t.toSecondOfDay() / 60f + (i - 1) * 1440f

            drawEventMarker("Dawn", timeMin, true)
        }

        duskTimes.forEachIndexed { i, t ->
            val timeMin = t.toSecondOfDay() / 60f + (i - 1) * 1440f

            drawEventMarker("Dusk", timeMin, true)
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
