package com.example.trackerapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client = OkHttpClient()
    private lateinit var deviceId: String
    private val serverUrl = "http://18.116.37.113/tracker/receive_location.php"

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        deviceId = getOrCreateDeviceId()
        startForeground(1, createNotification())
        startLocationUpdates()
    }

    private fun getOrCreateDeviceId(): String {
        val prefs = getSharedPreferences("tracker_prefs", MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    private fun createNotification(): Notification {
        val channelId = "tracker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Tracker Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rastreo activo")
            .setContentText("La ubicación se está enviando al servidor")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun startLocationUpdates() {
        val builder = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000 // cada 1 segundo
        )
            .setMinUpdateIntervalMillis(500)
            .setMinUpdateDistanceMeters(0f) // reporta hasta sin movimiento
            .setWaitForAccurateLocation(true)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdates(Int.MAX_VALUE) // sin límite de actualizaciones

        val request = builder.build()

        fusedLocationClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    sendLocation(location.latitude, location.longitude)
                }
            },
            Looper.getMainLooper()
        )
    }



    private fun sendLocation(lat: Double, lon: Double) {
        val json = JSONObject().apply {
            put("device_id", deviceId)
            put("lat", lat)
            put("lon", lon)
        }

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
