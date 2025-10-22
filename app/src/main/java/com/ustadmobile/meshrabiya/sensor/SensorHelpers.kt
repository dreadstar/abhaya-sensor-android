package com.ustadmobile.meshrabiya.sensor.util

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.flow.SharedFlow
import com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor


fun floatsToByteArray(values: FloatArray): ByteArray {
    val bb = ByteBuffer.allocate(4 * values.size).order(ByteOrder.nativeOrder())
    for (v in values) bb.putFloat(v)
    return bb.array()
}

interface UIIngestor : StreamIngestor {
    data class UIEvent(val streamId: String, val timestampMs: Long, val payloadLength: Int)
    val events: SharedFlow<UIEvent>
}