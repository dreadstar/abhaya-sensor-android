package com.ustadmobile.meshrabiya.sensor.ui

// IMPORTS BLOCK FROM SensorApp.kt
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ustadmobile.meshrabiya.sensor.ui.SensorDiscovery
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.google.accompanist.flowlayout.FlowRow
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import com.ustadmobile.meshrabiya.sensor.capture.CameraCapture
import com.ustadmobile.meshrabiya.sensor.capture.AudioCapture
import com.ustadmobile.meshrabiya.sensor.capture.CameraControllerImpl
import com.ustadmobile.meshrabiya.sensor.stream.HttpStreamIngestor

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import com.ustadmobile.meshrabiya.sensor.util.floatsToByteArray
import com.ustadmobile.meshrabiya.sensor.util.UIIngestor
import com.ustadmobile.meshrabiya.sensor.ui.SensorCategory
import com.ustadmobile.meshrabiya.sensor.ui.SensorInfo
import com.ustadmobile.meshrabiya.sensor.ui.defaultSensorCategories

// UI-facing ingestor type: exposes the same events flow as the app's in-process ingestor
private interface UIIngestor : com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor {
    data class UIEvent(val streamId: String, val timestampMs: Long, val payloadLength: Int)
    val events: SharedFlow<UIEvent>
}

@Composable
fun SensorApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    // Discover available hardware sensors
    val hardwareSensors = remember { mutableStateMapOf<String, Sensor>() }
    LaunchedEffect(Unit) {
        sensorManager.getSensorList(Sensor.TYPE_ALL).forEach { s ->
            // Store by numeric type ID (e.g., "sensor_1") to match category sensor IDs
            val numericId = "sensor_${s.type}"
            hardwareSensors[numericId] = s
            // Also store by string type if available for reference
            s.stringType?.let { hardwareSensors[it] = s }
            // Debug logging
            android.util.Log.d("SensorApp", "Found sensor: $numericId (stringType=${s.stringType}, type=${s.type}, name=${s.name})")
        }
        android.util.Log.d("SensorApp", "Total sensors found: ${hardwareSensors.size}")
    }

    // State management
    val sensorCategories = remember { defaultSensorCategories() }
    var pollingFrequency by remember { mutableStateOf(10) }
    var selectedSensors by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ingestorRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready to start") }
    val ingestLog = remember { mutableStateListOf<String>() }
    val listeners = remember { mutableMapOf<String, SensorEventListener>() }
    
    // Camera & Audio control state
    var videoFrameRate by remember { mutableStateOf(30) }
    var videoFormat by remember { mutableStateOf("H.264") }
    var photoAspectRatio by remember { mutableStateOf("16:9") }
    var photoPixelDepth by remember { mutableStateOf("8-bit") }
    var photoFrequency by remember { mutableStateOf(5) } // seconds
    var initialStepCount by remember { mutableStateOf<Float?>(null) }

    

    // Initialize HttpStreamIngestor with UI event tracking
    val ingestor = remember {
        val http = HttpStreamIngestor("https://example.com/store", token = null)
        val _events = MutableSharedFlow<UIIngestor.UIEvent>(replay = 0, extraBufferCapacity = 100)
        object : UIIngestor {
            override val events: SharedFlow<UIIngestor.UIEvent> = _events
            override fun start() = http.start()
            override fun stop() = http.stop()
            override fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray) {
                http.ingestSensorReading(streamId, timestampMs, payload)
                try {
                    _events.tryEmit(UIIngestor.UIEvent(streamId, timestampMs, payload.size))
                } catch (_: Throwable) {}
            }
        }
    }

    // Add mesh node discovery logic (pseudo-code for clarity)
    fun discoverStorageNodes(): List<MeshNodeInfo> {
        // Use mesh APIs to broadcast a request and collect responses
        // Each response includes node URL, latency, system state, etc.
        // Return list of candidate nodes
        return listOf(
            MeshNodeInfo(url = "https://example.com/store", latency = 10, systemState = "OK"),
            MeshNodeInfo(url = "https://example.com/alt", latency = 20, systemState = "OK")
        )
    }

    fun selectBestNode(candidates: List<MeshNodeInfo>): MeshNodeInfo {
        // Evaluate candidates based on latency, system state, etc.
        // Return the best node
        return candidates.minByOrNull { it.latency } ?: MeshNodeInfo()
    }

    // Camera and audio capture controllers
    val cameraController = remember { CameraControllerImpl(context, lifecycleOwner, ingestor) }
    val audioController = remember { AudioCapture(ingestor) }
    val audioLevel by audioController.audioLevel.collectAsState()

    // Job for events collection so we can cancel it on stop
    var eventsJob by remember { mutableStateOf<Job?>(null) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            listeners.values.forEach { sensorManager.unregisterListener(it) }
            listeners.clear()
            eventsJob?.cancel()
            if (ingestorRunning) {
                ingestor.stop()
            }
        }
    }

    // When selection changes: register/unregister hardware sensor listeners
    LaunchedEffect(selectedSensors, hardwareSensors.keys.toList()) {
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
                        val ts = System.currentTimeMillis()
                        if (id == "sensor_19") { // Sensor.TYPE_STEP_COUNTER == 19
                            val stepValue = event.values[0]
                            if (initialStepCount == null) {
                                initialStepCount = stepValue
                            }
                            val stepsSinceStart = stepValue - (initialStepCount ?: stepValue)
                            val payload = floatsToByteArray(floatArrayOf(stepsSinceStart))
                            ingestor.ingestSensorReading(id, ts, payload)
                        } else {
                            val payload = floatsToByteArray(event.values)
                            ingestor.ingestSensorReading(id, ts, payload)
                        }
                    } catch (_: Exception) {}
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            listeners[id] = listener
        }
    }

    

    // Start/stop audio based on selection (camera is managed by AndroidView)
    LaunchedEffect(selectedSensors) {
        if (selectedSensors.contains("audio_stream")) {
            audioController.start()
        } else {
            audioController.stop()
        }
    }

    // Start/stop functions
    val startIngestor = {
        if (!ingestorRunning) {
            initialStepCount = null
            // 1. Discover storage nodes
            val candidates = discoverStorageNodes()
            // 2. Select best candidate
            val selectedNode = selectBestNode(candidates)
            // 3. Update HttpStreamIngestor endpoint
            (ingestor as? HttpStreamIngestor)?.updateEndpoint(selectedNode.url)
            // 4. Start streaming
            ingestor.start()
            ingestorRunning = true
            statusMessage = "Streaming active"
            eventsJob = scope.launch {
                try {
                    (ingestor as? UIIngestor)?.events?.collectLatest { ev ->
                        ingestLog.add(0, "${ev.streamId} @ ${ev.timestampMs} (${ev.payloadLength} bytes)")
                        if (ingestLog.size > 200) ingestLog.removeAt(ingestLog.lastIndex)
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    // Listen for stop signals from storage agent (pseudo-code)
    fun listenForStopSignal(selectedNode: MeshNodeInfo) {
        // Use mesh APIs to subscribe to stop signals from selectedNode
        // On stop signal:
        //   - discoverStorageNodes()
        //   - selectBestNode()
        //   - updateEndpoint()
        //   - continue streaming
    }

    val stopIngestor = {
        if (ingestorRunning) {
            eventsJob?.cancel()
            eventsJob = null
            ingestor.stop()
            ingestorRunning = false
            statusMessage = "Stopped"
        }
    }

    // Gradient background colors (slate-900 to purple-900 to slate-900)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E293B), // slate-900
            Color(0xFF581C87), // purple-900
            Color(0xFF1E293B)  // slate-900
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // App Header
            AppHeader(
                isRunning = ingestorRunning,
                statusMessage = statusMessage
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Polling Frequency Section
            PollingFrequencySection(
                currentFrequency = pollingFrequency,
                onFrequencyChange = { pollingFrequency = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sensors Selection with Categories
            GlassmorphicCard(title = "Select Sensors", titleTestTag = "select_sensors_card") {
                Column {
                    sensorCategories.forEach { cat ->
                        CategoryCard(
                            cat = cat,
                            expandedCategories = expandedCategories,
                            onExpandToggle = { catName ->
                                expandedCategories = if (expandedCategories.contains(catName)) {
                                    expandedCategories - catName
                                } else {
                                    expandedCategories + catName
                                }
                            },
                            hardwareSensors = hardwareSensors,
                            selectedSensors = selectedSensors,
                            onSensorCheckedChange = { id, ch ->
                                selectedSensors = if (ch) {
                                    selectedSensors + id
                                } else {
                                    selectedSensors - id
                                }
                            },
                            cameraController = cameraController
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Event Log
            EventLogSection(ingestLog = ingestLog, modifier = Modifier.padding(8.dp))

            // Control Buttons
            ControlButtons(
                isRunning = ingestorRunning,
                onStartClick = startIngestor,
                onStopClick = stopIngestor
            )
        }
    }
}



@Composable
private fun FrequencyButton(
    frequency: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSelect,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = if (isSelected)
                Color(0xFF8B5CF6)
            else
                Color.White.copy(alpha = 0.1f),
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Text(
            text = "${frequency}Hz",
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = if (isRunning) onStopClick else onStartClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .semantics { testTag = "control_start_stop" },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isRunning)
                    Color(0xFFEF4444) // red
                else
                    Color(0xFF10B981), // green
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (isRunning) "Stop Streaming" else "Start Streaming",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


