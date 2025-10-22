package com.ustadmobile.meshrabiya.sensor.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Simple HTTP-backed StreamIngestor that POSTs raw payload bytes to the configured endpoint.
 * This implementation is intentionally small and test-friendly: it performs network calls on a
 * background coroutine scope and tolerates transient failures (logs them).
 */
class HttpStreamIngestor(private val endpoint: String, private val token: String? = null) : StreamIngestor {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    @Volatile
    private var running = false

    fun updateEndpoint(newEndpoint: String) {
        endpoint = newEndpoint
    }

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray) {
        if (!running) return
        val url = endpoint
        scope.launch {
            try {
                val mediaType = "application/octet-stream".toMediaTypeOrNull()
                val body: RequestBody = payload.toRequestBody(mediaType)
                val rb = Request.Builder().url(url).post(body)
                    .addHeader("X-Stream-Id", streamId)
                    .addHeader("X-Timestamp", timestampMs.toString())
                token?.let { rb.addHeader("Authorization", "Bearer $it") }
                val req = rb.build()
                client.newCall(req).execute().use { resp ->
                    // Optionally inspect resp.code() / body
                }
            } catch (t: Throwable) {
                android.util.Log.w("HttpStreamIngestor", "Failed to POST ingest", t)
            }
        }
    }
}
