package org.abhaya.sensor.meshrabiya

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple HTTP-backed MeshrabiyaServiceApi that calls network-style endpoints
 * (POST /descriptor, POST /store, GET /identity) on either localhost or
 * remote onion addresses. This enforces identical semantics for local loopback
 * and remote peers per the KNOWLEDGE doc.
 */
class HttpMeshrabiyaServiceApi(private val baseUrl: String, private val authToken: String?) : MeshrabiyaServiceApi {

    private fun buildUrl(path: String): String {
        return if (baseUrl.endsWith("/")) baseUrl.dropLast(1) + path else baseUrl + path
    }

    override suspend fun requestDelegatedStorage(descriptorJson: String): Acceptance = withContext(Dispatchers.IO) {
        val url = URL(buildUrl("/descriptor"))
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 30_000
            addRequestProperty("Content-Type", "application/json")
            if (!authToken.isNullOrEmpty()) addRequestProperty("X-Meshrabiya-Auth", authToken)
        }

        conn.outputStream.use { out -> out.write(descriptorJson.toByteArray(Charsets.UTF_8)) }

        val code = conn.responseCode
        val respText = conn.inputStream.bufferedReader().use { it.readText() }
        if (code < 200 || code >= 300) {
            throw java.io.IOException("Descriptor publish failed: HTTP $code - $respText")
        }

        val json = JSONObject(respText)
        // For now map the stub acceptance to UploadToken with empty endpoint/token
        val descriptorId = json.optString("descriptorId", java.util.UUID.randomUUID().toString())
        return@withContext Acceptance.UploadToken(descriptorId = descriptorId, uploadEndpoint = json.optString("upload_endpoint", ""), token = json.optString("token", ""))
    }

    override suspend fun waitForBlobId(descriptorId: String, timeoutMillis: Long): String? = withContext(Dispatchers.IO) {
        // Poll GET /identity or poll a small status endpoint; since none exists,
        // implement a simple backoff loop calling GET /identity to verify connectivity.
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            try {
                val u = URL(buildUrl("/identity"))
                val conn = (u.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5_000
                    readTimeout = 10_000
                    if (!authToken.isNullOrEmpty()) addRequestProperty("X-Meshrabiya-Auth", authToken)
                }
                if (conn.responseCode in 200..299) return@withContext descriptorId
            } catch (_: Exception) {
                // ignore and retry
            }
            Thread.sleep(500)
        }
        return@withContext null
    }
}
