package com.example.myapplication2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.random.Random

class StarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.FILL
    }

    private data class Star(
        var x: Float,
        var y: Float,
        var alpha: Int,
        var radius: Float,
        var fadingIn: Boolean = true,
        var isShooting: Boolean = false,
        var vx: Float = 0f,
        var vy: Float = 0f,
        var life: Int = 0,
        var maxLife: Int = 0,
        var fadeInSpeed: Int = 10,
        var fadeOutSpeed: Int = 5
    )

    private val horizontalPadding = 10f
    private val verticalPadding = 10f

    private val stars = mutableListOf<Star>()
    private val maxStars = 7
    private val random = Random(System.currentTimeMillis())
    private val handler = Handler(Looper.getMainLooper())

    private val animationRunnable = object : Runnable {
        override fun run() {
            updateStars()
            invalidate()
            handler.postDelayed(this, 50)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (width > 0 && height > 0 && stars.isEmpty()) {
            initializeStars()
        }
        handler.post(animationRunnable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initializeStars()
    }

    private fun initializeStars() {
        stars.clear()
        for (i in 0 until maxStars) {
            stars.add(createRandomStar(width, height))
        }
    }

    private fun createRandomStar(viewWidth: Int, viewHeight: Int): Star {
        val lifespan = random.nextInt(600, 1200)
        return Star(
            x = random.nextFloat() * (viewWidth - 2 * horizontalPadding) + horizontalPadding,
            y = random.nextFloat() * (viewHeight - 2 * verticalPadding) + verticalPadding,
            alpha = 0,
            radius = random.nextFloat() * 7f + 5.5f,
            fadeInSpeed = random.nextInt(5, 15),
            fadeOutSpeed = random.nextInt(2, 8),
            life = lifespan,
            maxLife = lifespan
        )
    }

    private fun resetStar(star: Star) {
        star.x = random.nextFloat() * (width - 2 * horizontalPadding) + horizontalPadding
        star.y = random.nextFloat() * (height - 2 * verticalPadding) + verticalPadding
        star.radius = random.nextFloat() * 7f + 5.5f
        star.alpha = 0
        star.fadingIn = true
        star.fadeInSpeed = random.nextInt(5, 15)
        star.fadeOutSpeed = random.nextInt(2, 8)
        val lifespan = random.nextInt(300, 1200)
        star.life = lifespan
        star.maxLife = lifespan
    }

    private fun drawFourPointedStar(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
        val long = size
        val short = size / 3f

        path.moveTo(cx, cy - long)
        path.lineTo(cx + short, cy - short)
        path.lineTo(cx + long, cy)
        path.lineTo(cx + short, cy + short)
        path.lineTo(cx, cy + long)
        path.lineTo(cx - short, cy + short)
        path.lineTo(cx - long, cy)
        path.lineTo(cx - short, cy - short)
        path.close()

        canvas.drawPath(path, paint)
    }

    private fun updateStars() {
        for (star in stars) {
            star.life--

            if (star.isShooting) {
                star.x += star.vx
                star.y += star.vy
                star.alpha = (star.alpha * 0.92).toInt().coerceAtLeast(0)

                if (star.life <= 0 || star.x > width || star.y > height) {
                    star.isShooting = false
                    resetStar(star)
                }
                continue
            }

            if (star.fadingIn) {
                star.alpha += star.fadeInSpeed
                if (star.alpha >= 255) {
                    star.alpha = 255
                    star.fadingIn = false
                }
            } else {
                star.alpha -= star.fadeOutSpeed
                if (star.alpha < 0) star.alpha = 0
            }

            if (star.alpha <= 0 || star.life <= 0) {
                resetStar(star)
            }
        }

        if (stars.isNotEmpty() && random.nextInt(100) < 3 && stars.none { it.isShooting }) {
            val shootingStar = stars[random.nextInt(stars.size)]
            shootingStar.isShooting = true
            shootingStar.fadingIn = false
            shootingStar.x = random.nextFloat() * width / 2
            shootingStar.y = random.nextFloat() * height / 2
            shootingStar.vx = random.nextFloat() * 5f + 3f
            shootingStar.vy = random.nextFloat() * 3f + 2f
            shootingStar.life = 300
            shootingStar.alpha = 255
        }

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (star in stars) {
            starPaint.alpha = star.alpha
            if (star.isShooting) {
                canvas.drawLine(
                    star.x - star.vx * 7,
                    star.y - star.vy * 6,
                    star.x,
                    star.y,
                    starPaint
                )
            } else {
                drawFourPointedStar(canvas, star.x, star.y, star.radius, starPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(animationRunnable)
    }
}
