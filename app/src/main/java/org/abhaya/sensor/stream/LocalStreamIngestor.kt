package com.ustadmobile.meshrabiya.sensor.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * LocalStreamIngestor is a small in-memory ingestor used by the example app.
 * It exposes a read-only event flow that the UI can collect for simple logging.
 */
class LocalStreamIngestor : StreamIngestor {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val ch = Channel<Triple<String, Long, ByteArray>>(capacity = Channel.UNLIMITED)
    private val _events = MutableSharedFlow<IngestEvent>(extraBufferCapacity = 100)
    val events = _events.asSharedFlow()

    @Volatile
    private var running = false

    override fun start() {
        if (running) return
        running = true
        scope.launch {
            for (item in ch) {
                val (streamId, ts, payload) = item
                // In a real implementation this would persist to DistributedStorageAgent.
                _events.emit(IngestEvent(streamId, ts, payload.size))
            }
        }
    }

    override fun stop() {
        running = false
        try { ch.close() } catch (_: Exception) {}
    }

    override fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray) {
        if (!running) return
        scope.launch { ch.send(Triple(streamId, timestampMs, payload)) }
    }

    data class IngestEvent(val streamId: String, val timestampMs: Long, val payloadLength: Int)
}
