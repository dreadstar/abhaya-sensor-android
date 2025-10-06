package org.abhaya.sensor.meshrabiya

import com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple HTTP StreamIngestor that uploads sensor segments to a provided uploadEndpoint
 * using the provided token. This mirrors remote peer semantics and uses POST /store.
 */
class HttpStreamIngestor(private val uploadEndpoint: String, private val token: String?) : StreamIngestor {

    companion object {
        /** Optional test hook to create a URL from the uploadEndpoint string. If non-null,
         *  this will be used instead of calling URL(uploadEndpoint) directly. */
        @JvmStatic
        var urlFactory: ((String) -> URL)? = null
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    @Volatile private var running = false

    override fun start() {
        running = true
    }

    override fun stop() {
        running = false
    }

    override fun ingestSensorReading(streamId: String, timestampMs: Long, payload: ByteArray) {
        if (!running) return
        scope.launch {
            try {
                val url = urlFactory?.invoke(uploadEndpoint) ?: URL(uploadEndpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 30_000
                    addRequestProperty("Content-Type", "application/octet-stream")
                    if (!token.isNullOrEmpty()) addRequestProperty("X-Meshrabiya-Auth", token)
                }
                conn.outputStream.use { out: OutputStream -> out.write(payload) }
                val code = conn.responseCode
                // ignore body; best-effort
                conn.inputStream.use { it.readBytes() }
            } catch (_: Exception) {
                // best-effort: swallow network errors
            }
        }
    }
}
