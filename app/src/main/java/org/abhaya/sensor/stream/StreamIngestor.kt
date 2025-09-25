package com.ustadmobile.meshrabiya.sensor.stream

interface StreamIngestor {
    fun start()
    fun stop()
    fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray)
}
