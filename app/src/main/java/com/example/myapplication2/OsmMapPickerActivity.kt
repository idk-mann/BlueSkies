package com.example.myapplication2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.View


class OsmMapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var selectedPoint: GeoPoint? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load osmdroid configuration
        Configuration.getInstance().load(applicationContext, PreferenceManager.getDefaultSharedPreferences(applicationContext))

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION


        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT


        setContentView(R.layout.activity_osm_map_picker)

        val confirmButton = findViewById<Button>(R.id.confirm_button)



        mapView = findViewById(R.id.mapView)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(5.0)
        mapView.controller.setCenter(GeoPoint(20.0, 0.0)) // Default center

        // Handle map taps
        mapView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val projection = mapView.projection
                val tappedGeoPoint = projection.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                placeMarker(tappedGeoPoint)
            }
            false
        }

        findViewById<Button>(R.id.confirm_button).setOnClickListener {
            if (selectedPoint != null) {
                val intent = Intent().apply {
                    putExtra("latitude", selectedPoint!!.latitude)
                    putExtra("longitude", selectedPoint!!.longitude)
                }
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                // No location picked
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }

    private fun placeMarker(point: GeoPoint) {
        selectedPoint = point
        if (marker == null) {
            marker = Marker(mapView).apply {
                position = point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Selected Location"
            }
            mapView.overlays.add(marker)
        } else {
            marker!!.position = point
        }
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
