package com.ustadmobile.meshrabiya.sensor.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.ustadmobile.meshrabiya.sensor.stream.LocalStreamIngestor
import com.ustadmobile.meshrabiya.sensor.capture.CameraCapture
import com.ustadmobile.meshrabiya.sensor.capture.AudioCapture
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.view.PreviewView
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
fun SensorApp() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // Discover available hardware sensors
    val hardwareSensors = remember { mutableStateMapOf<String, Sensor>() }
    LaunchedEffect(Unit) {
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { s ->
            val id = s.stringType ?: "sensor_${s.type}"
            hardwareSensors[id] = s
        }
    }

    // UI state
    val sensorCategories = remember { defaultSensorCategories() }
    var pollingFrequency by remember { mutableStateOf(10) }
    var selectedSensors by remember { mutableStateOf(setOf<String>()) }
    var ingestorRunning by remember { mutableStateOf(false) }
    val ingestLog = remember { mutableStateListOf<String>() }

    // Ingestor wiring
    val ingestor = remember { LocalStreamIngestor() }
    val listeners = remember { mutableMapOf<String, SensorEventListener>() }

    // Do not auto-start; expose controls to the user
    DisposableEffect(Unit) {
        onDispose {
            listeners.values.forEach { sensorManager.unregisterListener(it) }
            listeners.clear()
            if (ingestorRunning) ingestor.stop()
        }
    }

    // When selection changes: register/unregister hardware sensor listeners
    LaunchedEffect(selectedSensors, hardwareSensors.keys) {
        val hwIds = hardwareSensors.keys
        val toRegister = selectedSensors.filter { it in hwIds } - listeners.keys
        val toUnregister = listeners.keys - selectedSensors

        toUnregister.forEach { id ->
            listeners[id]?.let { sensorManager.unregisterListener(it) }
            listeners.remove(id)
        }

        toRegister.forEach { id ->
            val sensor = hardwareSensors[id] ?: return@forEach
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event == null) return
                    try {
                        val payload = floatsToByteArray(event.values)
                        val ts = System.currentTimeMillis()
                        ingestor.ingestSensorReading(id, ts, payload)
                    } catch (_: Exception) {}
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            listeners[id] = listener
        }
    }

    // Camera and audio capture controllers (for virtual sensors)
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { CameraCapture(context, lifecycleOwner, ingestor) }
    val audioController = remember { AudioCapture(ingestor) }

    LaunchedEffect(selectedSensors) {
        if (selectedSensors.contains("camera_stream")) {
            cameraController.start(periodicSeconds = 5)
        } else {
            cameraController.stop()
        }

        if (selectedSensors.contains("audio_stream")) {
            audioController.start()
        } else {
            audioController.stop()
        }
    }

    // UI layout
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        TopAppBar(title = { Text("Abhaya Sensor") }, actions = {})
        Spacer(modifier = Modifier.height(8.dp))

        // Controls: polling freq
        Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text("Polling Frequency: $pollingFrequency Hz")
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1,5,10,25,50,100).forEach { v ->
                        Button(onClick = { pollingFrequency = v }) { Text("${v}Hz") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Left: categories and sensors; Right: status/logs
        Row(modifier = Modifier.weight(1f)) {
            LazyColumn(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                items(sensorCategories) { cat ->
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(cat.name, style = MaterialTheme.typography.h6)
                            Spacer(modifier = Modifier.height(6.dp))
                            cat.sensors.forEach { s ->
                                val id = s.id
                                val available = when (id) {
                                    "camera_stream", "audio_stream", "periodic_photos" -> true
                                    else -> hardwareSensors.containsKey(id)
                                }
                                if (!available) return@forEach

                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val checked = selectedSensors.contains(id)
                                    Checkbox(checked = checked, onCheckedChange = { ch ->
                                        selectedSensors = if (ch) selectedSensors + id else selectedSensors - id
                                    })
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(s.name)
                                        Text(s.desc, color = Color.Gray)
                                    }
                                    // Per-virtual-sensor quick actions
                                    if (id == "camera_stream") {
                                        Button(onClick = { if (checked) cameraController.captureOnce() else {} }, enabled = checked) { Text("Capture") }
                                    }
                                    Text(if (checked) "On" else "Off")
                                }
                            }
                        }
                    }
                }
            }

                // Right column: status, camera preview and log
            Column(modifier = Modifier.weight(1f)) {
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Ingestor", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if (ingestorRunning) "Running" else "Stopped", color = if (ingestorRunning) Color(0xFF2E7D32) else Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = {
                                if (!ingestorRunning) {
                                    ingestor.start(); ingestorRunning = true
                                    // subscribe to events
                                    scope.launch {
                                        ingestor.events.collectLatest { ev ->
                                            ingestLog.add(0, "${ev.streamId} @ ${ev.timestampMs} (${ev.payloadLength} bytes)")
                                            if (ingestLog.size > 200) ingestLog.removeLast()
                                        }
                                    }
                                }
                            }, enabled = !ingestorRunning) { Text("Start Ingest") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { if (ingestorRunning) { ingestor.stop(); ingestorRunning = false } }, enabled = ingestorRunning) { Text("Stop Ingest") }
                        }
                    }
                }

                // Camera preview (CameraX PreviewView hosted in Compose)
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp).height(200.dp)) {
                    AndroidView(factory = { ctx ->
                        val pv = androidx.camera.view.PreviewView(ctx)
                        pv
                    }, update = { pv ->
                        // If camera_stream is selected and ingestor running, ensure the camera is started with the preview
                        if (selectedSensors.contains("camera_stream")) {
                            cameraController.start(pv)
                        } else {
                            cameraController.stop()
                        }
                    })
                }

                // Live ingest log
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp).weight(1f)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Event Log", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            ingestLog.forEach { l ->
                                Text(l, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Footer controls
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Selected: ${selectedSensors.size}")
            Row {
                Button(onClick = { selectedSensors = emptySet() }) { Text("Clear") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { if (!ingestorRunning) { ingestor.start(); ingestorRunning = true } }) { Text("Ensure Ingest Running") }
            }
        }
    }
}

private fun floatsToByteArray(values: FloatArray): ByteArray {
    val bb = ByteBuffer.allocate(4 * values.size).order(ByteOrder.nativeOrder())
    for (v in values) bb.putFloat(v)
    return bb.array()
}

private fun defaultSensorCategories(): List<SensorCategory> {
    return listOf(
        SensorCategory("Motion Sensors", listOf(
            SensorDescriptor("sensor_${Sensor.TYPE_ACCELEROMETER}", "Accelerometer", "Device acceleration (m/s²)"),
            SensorDescriptor("sensor_${Sensor.TYPE_GYROSCOPE}", "Gyroscope", "Rotation rate (rad/s)"),
            SensorDescriptor("sensor_${Sensor.TYPE_MAGNETIC_FIELD}", "Magnetometer", "Magnetic field (μT)"),
            SensorDescriptor("sensor_${Sensor.TYPE_LINEAR_ACCELERATION}", "Linear Acceleration", "Acceleration without gravity"),
        )),
        SensorCategory("Environmental", listOf(
            SensorDescriptor("sensor_${Sensor.TYPE_AMBIENT_TEMPERATURE}", "Ambient Temperature", "Room temperature (°C)"),
            SensorDescriptor("sensor_${Sensor.TYPE_PRESSURE}", "Pressure", "Atmospheric pressure (hPa)"),
            SensorDescriptor("sensor_${Sensor.TYPE_RELATIVE_HUMIDITY}", "Humidity", "Relative humidity (%)"),
            SensorDescriptor("sensor_${Sensor.TYPE_LIGHT}", "Light", "Ambient light (lx)"),
        )),
        SensorCategory("Camera & Audio", listOf(
            SensorDescriptor("camera_stream", "Camera Stream", "Video capture with controls"),
            SensorDescriptor("audio_stream", "Audio Stream", "Audio capture with threshold"),
            SensorDescriptor("periodic_photos", "Periodic Photos", "Timed photo capture"),
        )),
        SensorCategory("System Metrics", listOf(
            SensorDescriptor("sensor_${Sensor.TYPE_HEART_BEAT}", "Heart Beat", "Heartbeat detection"),
            SensorDescriptor("sensor_${Sensor.TYPE_STEP_COUNTER}", "Step Counter", "Steps since reboot"),
            SensorDescriptor("sensor_${Sensor.TYPE_PROXIMITY}", "Proximity", "Distance to object (cm)"),
        ))
    )
}

private data class SensorCategory(val name: String, val sensors: List<SensorDescriptor>)
private data class SensorDescriptor(val id: String, val name: String, val desc: String)
