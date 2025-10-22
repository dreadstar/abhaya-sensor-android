package com.ustadmobile.meshrabiya.sensor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.clickable
import androidx.compose.material.MaterialTheme
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.FontFamily

@Composable
fun AppHeader(isRunning: Boolean, statusMessage: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = if (isRunning) "Sensor Streaming" else "Sensor App",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = statusMessage,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PollingFrequencySection(currentFrequency: Int, onFrequencyChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Polling Frequency:",
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        listOf(1, 5, 10, 30, 60).forEach { freq ->
            Text(
                text = "$freq s",
                fontSize = 13.sp,
                color = if (currentFrequency == freq) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onFrequencyChange(freq) }
            )
        }
    }
}

@Composable
fun GlassmorphicCard(title: String, titleTestTag: String? = null, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}


@Composable
fun EventLogSection(
    ingestLog: List<String>,
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        title = "Event Log",
        titleTestTag = "event_log_section",
        modifier = modifier
    ) {
        if (ingestLog.isEmpty()) {
            Text(
                text = "No events yet",
                style = MaterialTheme.typography.body2,
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                items(ingestLog) { entry ->
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = 8.dp)
                            .background(Color(0xFF1E293B).copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}
