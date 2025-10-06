package org.abhaya.sensor.meshrabiya

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small helper demonstrating how a sensor should publish a descriptor and upload to the
 * returned upload endpoint/token using the HTTP-backed Meshrabiya client.
 *
 * Note: This is an example utility for the sensor app. Real code should handle chunking,
 * retries, backoff, and cleanup of temporary files as described in the KNOWLEDGE doc.
 */
object SampleMeshrabiyaUsage {

    suspend fun publishDescriptorAndUpload(descriptorJson: String,
                                           localTempPfd: ParcelFileDescriptor?,
                                           baseUrl: String,
                                           authToken: String?): String? {
        val client = MeshrabiyaClient.bindHttp(baseUrl, authToken)
        try {
            val acceptance = client.requestDelegatedStorage(descriptorJson)
            return when (acceptance) {
                is Acceptance.UploadToken -> {
                    // perform upload to the returned endpoint
                    uploadToEndpoint(acceptance.uploadEndpoint, localTempPfd, acceptance.token)
                    // wait for authoritative blobId
                    client.waitForBlobId(acceptance.descriptorId)
                }
                is Acceptance.DirectAccept -> {
                    // direct peer streaming (not implemented here)
                    streamToPeer(acceptance.peerSocketInfo, localTempPfd)
                    client.waitForBlobId(acceptance.descriptorId)
                }
                else -> null
            }
        } finally {
            client.close()
        }
    }

    private suspend fun uploadToEndpoint(uploadEndpoint: String, pfd: ParcelFileDescriptor?, token: String) =
        withContext(Dispatchers.IO) {
            if (pfd == null) throw IllegalArgumentException("pfd required for upload")

            val url = URL(uploadEndpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 60_000
                addRequestProperty("X-Meshrabiya-Auth", token)
                addRequestProperty("Content-Type", "application/octet-stream")
            }

            // Stream the ParcelFileDescriptor contents to the upload endpoint
            conn.outputStream.use { out ->
                val input = ParcelFileDescriptor.AutoCloseInputStream(pfd)
                BufferedInputStream(input).use { bin ->
                    val buf = ByteArray(8 * 1024)
                    var read = bin.read(buf)
                    while (read >= 0) {
                        out.write(buf, 0, read)
                        read = bin.read(buf)
                    }
                    out.flush()
                }
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val resp = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw java.io.IOException("Upload failed: HTTP $code - $resp")
            }
        }

    private suspend fun streamToPeer(peerSocketInfo: String, pfd: ParcelFileDescriptor?): Unit = withContext(Dispatchers.IO) {
        // Placeholder for peer-streaming (WebRTC/datachannel or onion stream) implementation.
        // Implementations will vary depending on the chosen mesh transport.
        throw UnsupportedOperationException("Direct peer streaming not implemented in sample")
    }
}
