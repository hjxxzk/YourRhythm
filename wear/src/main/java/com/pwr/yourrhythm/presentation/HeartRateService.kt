package com.pwr.yourrhythm.presentation

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.PermissionChecker
import androidx.core.app.ServiceCompat
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable


class HeartRateService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null

    override fun onCreate() {
        super.onCreate()

        // Sprawdzenie uprawnień BODY_SENSORS
        val sensorPermission = PermissionChecker.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
        if (sensorPermission != PermissionChecker.PERMISSION_GRANTED) {
            Log.e("Wear", "Brak uprawnień BODY_SENSORS")
            stopSelf()
            return
        }

        // Kanał powiadomień dla foreground service
        val channel = NotificationChannel(
            "heart_rate_channel",
            "Heart Rate Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, "heart_rate_channel")
            .setContentTitle("Your Rhythm")
            .setContentText("Measuring heart rate…")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()

        // Uruchomienie serwisu w foreground
        ServiceCompat.startForeground(
            this,
            1,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )

        // Rejestracja czujnika tętna
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val heartRate = event.values[0]
        Log.d("Wear", "Heart rate: $heartRate")
        sendHeartRateToPhone(heartRate)
    }

    private fun sendHeartRateToPhone(heartRate: Float) {
        val message = heartRate.toString().toByteArray()
        Thread {
            try {
                val nodes = Tasks.await(Wearable.getNodeClient(this).connectedNodes)
                for (node in nodes) {
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, "/heartrate", message)
                        .addOnSuccessListener {
                            Log.d("Wear", "Sent heart rate: $heartRate to ${node.displayName}")
                        }
                        .addOnFailureListener {
                            Log.e("Wear", "Failed to send heart rate", it)
                        }
                }
            } catch (e: Exception) {
                Log.e("Wear", "Error sending heart rate", e)
            }
        }.start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}
