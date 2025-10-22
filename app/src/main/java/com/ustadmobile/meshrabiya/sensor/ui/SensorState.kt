package com.ustadmobile.meshrabiya.sensor.ui
import android.hardware.Sensor
data class SensorCategory(
    val name: String,
    val sensors: List<SensorInfo>
)

/**
 * Data classes and state containers for sensor categories and selection.
 */


data class SensorInfo(
    val id: String,
    val name: String,
    val desc: String = "",
    val type: String = "",
    val videoUri: String? = null,
    val photoUris: List<String> = emptyList()
)


fun defaultSensorCategories(): List<SensorCategory> {
    return listOf(
        SensorCategory("Motion Sensors", listOf(
            SensorInfo("sensor_1", "Accelerometer", "Device acceleration (m/s²)"),
            SensorInfo("sensor_4", "Gyroscope", "Rotation rate (rad/s)"),
            SensorInfo("sensor_2", "Magnetometer", "Magnetic field (μT)"),
            SensorInfo("sensor_10", "Linear Acceleration", "Acceleration without gravity"),
        )),
        SensorCategory("Environmental", listOf(
            SensorInfo("sensor_13", "Ambient Temperature", "Room temperature (°C)"),
            SensorInfo("sensor_6", "Pressure", "Atmospheric pressure (hPa)"),
            SensorInfo("sensor_12", "Humidity", "Relative humidity (%)"),
            SensorInfo("sensor_5", "Light", "Ambient light (lx)"),
        )),
        SensorCategory("Camera & Audio", listOf(
            SensorInfo("camera_stream", "Video", "Live video streaming with controls"),
            SensorInfo("audio_stream", "Audio Stream", "Audio capture with level monitoring"),
            SensorInfo("periodic_photos", "Periodic Photos", "Timed photo capture"),
        )),
        SensorCategory("System Metrics", listOf(
            SensorInfo("sensor_31", "Heart Beat", "Heartbeat detection"),
            SensorInfo("sensor_19", "Step Counter", "Steps since reboot"),
            SensorInfo("sensor_8", "Proximity", "Distance to object (cm)"),
        ))
    )
}

