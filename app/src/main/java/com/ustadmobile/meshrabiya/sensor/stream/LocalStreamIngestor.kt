package com.ustadmobile.meshrabiya.sensor.stream

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// The StreamIngestor interface is defined in StreamIngestor.kt in production sources.
// This file provides a local test-friendly implementation that emits IngestEvent via a SharedFlow.

data class IngestEvent(val streamId: String, val timestampMs: Long, val payloadLength: Int)

class LocalStreamIngestor : StreamIngestor {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _events = MutableSharedFlow<IngestEvent>(replay = 0)
    val events = _events.asSharedFlow()

    private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray) {
        if (!running) return
        val ev = IngestEvent(streamId, timestampMs, payload.size)
        scope.launch {
            _events.emit(ev)
        }
    }
}
