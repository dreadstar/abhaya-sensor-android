package com.ustadmobile.meshrabiya.sensor.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Provides sensor discovery and registration utilities for the app.
 */
object SensorDiscovery {
    /**
     * Returns a map of available hardware sensors by numeric and string type.
     */
    fun discoverSensors(context: Context): Map<String, Sensor> {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = mutableMapOf<String, Sensor>()
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { s ->
            val numericId = "sensor_${s.type}"
            sensors[numericId] = s
            s.stringType?.let { sensors[it] = s }
        }
        return sensors
    }
}
