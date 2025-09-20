package com.example.trackerapp

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var deviceId: String
    private var isTracking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs: SharedPreferences = getSharedPreferences("tracker_prefs", MODE_PRIVATE)
        deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }

        val statusText = findViewById<TextView>(R.id.statusText)
        val startButton = findViewById<Button>(R.id.startButton)
        val stopButton = findViewById<Button>(R.id.stopButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        startButton.setOnClickListener {
            if (checkLocationPermission()) {
                startTrackingService()
                isTracking = true
                statusText.text = "Rastreo en segundo plano\nID: $deviceId"
                startButton.visibility = Button.GONE
                stopButton.visibility = Button.VISIBLE
            } else {
                requestLocationPermission()
            }
        }

        stopButton.setOnClickListener {
            stopService(Intent(this, LocationService::class.java))
            isTracking = false
            statusText.text = "Rastreo detenido"
            startButton.visibility = Button.VISIBLE
            stopButton.visibility = Button.GONE
        }
    }

    private fun startTrackingService() {
        val intent = Intent(this, LocationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
    }
}
