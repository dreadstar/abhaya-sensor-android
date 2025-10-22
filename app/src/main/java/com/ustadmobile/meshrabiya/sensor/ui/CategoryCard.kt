
package com.ustadmobile.meshrabiya.sensor.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ustadmobile.meshrabiya.sensor.ui.SensorCategory
import com.ustadmobile.meshrabiya.sensor.ui.SensorInfo
import com.ustadmobile.meshrabiya.sensor.ui.SensorRow
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.ustadmobile.meshrabiya.sensor.camera.CameraController

@Composable
fun CategoryCard(
    cat: SensorCategory,
    expandedCategories: Set<String>,
    onExpandToggle: (String) -> Unit,
    hardwareSensors: Map<String, android.hardware.Sensor>,
    selectedSensors: Set<String>,
    onSensorCheckedChange: (String, Boolean) -> Unit,
    cameraController: CameraController
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onExpandToggle(cat.name) },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color.White.copy(alpha = 0.05f),
        elevation = 0.dp,
        content = {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cat.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    val isExpanded = expandedCategories.contains(cat.name)
                    Text(
                        text = if (isExpanded) "▼" else "▶",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Show sensors only if category is expanded
                if (expandedCategories.contains(cat.name)) {
                    Spacer(modifier = Modifier.height(8.dp))
                    cat.sensors.forEach { s ->
                        val id = s.id
                        val available = when (id) {
                            "camera_stream", "audio_stream", "periodic_photos" -> true
                            else -> hardwareSensors.containsKey(id)
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SensorRow(
                                id = id,
                                name = s.name,
                                desc = s.desc,
                                available = available,
                                checked = selectedSensors.contains(id),
                                onCheckedChange = { ch -> onSensorCheckedChange(id, ch) }
                            )
                            // Preview blocks for Camera, Audio, and Periodic Photos sensors
                            if (id == "camera_stream" && selectedSensors.contains(id)) {
                                var sensorState by remember { mutableStateOf(s) }
                                VideoPreviewBlock(sensor = sensorState, onSensorChange = { updated: SensorInfo ->
                                    sensorState = updated
                                }, 
                                cameraController = cameraController)
                            }
                            if (id == "audio_stream" && selectedSensors.contains(id)) {
                                var sensorState by remember { mutableStateOf(s) }
                                AudioStreamPreviewBlock(sensor = sensorState, onSensorChange = { updated: SensorInfo ->
                                    sensorState = updated
                                })
                            }
                            if (id == "periodic_photos" && selectedSensors.contains(id)) {
                                var sensorState by remember { mutableStateOf(s) }
                                PeriodicPhotosPreviewBlock(sensor = sensorState, onSensorChange = { updated: SensorInfo ->
                                    sensorState = updated
                                }, 
                                cameraController = cameraController)
                            }
                        }
                    }
                }
            }
        }
    )
}
