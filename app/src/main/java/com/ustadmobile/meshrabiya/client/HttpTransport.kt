package com.ustadmobile.meshrabiya.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Internal-only HTTP-backed Transport used by MeshrabiyaClient.bindHttp as a convenience.
 * This is intentionally small: publishRequest POSTs the descriptor JSON to POST /descriptor.
 * subscribeOffers/unsubscribeOffers are no-ops — real transports should wire mesh gossip.
 *
 * IMPORTANT: This class is internal to discourage use for negotiation. ResourceRequest/
 * ResourceOffer negotiation MUST occur over the gossip/MMCP transport. Use this only for
 * descriptor POSTs or uploads.
 */
internal class HttpTransport(private val baseUrl: String, private val authToken: String?) : MeshrabiyaClient.Transport {

    override fun publishRequest(request: MeshrabiyaClient.ResourceRequest) {
        // Fire-and-forget POST to /descriptor
        GlobalScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = URL(baseUrl.trimEnd('/') + "/descriptor")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 5_000
                        readTimeout = 10_000
                        addRequestProperty("Content-Type", "application/json")
                        if (!authToken.isNullOrEmpty()) addRequestProperty("X-Meshrabiya-Auth", authToken)
                    }
                    conn.outputStream.use { out -> out.write(request.descriptorJson.toByteArray(Charsets.UTF_8)) }
                    conn.inputStream.close()
                    conn.disconnect()
                }
            } catch (_: Throwable) {
                // best-effort; ignore errors here — caller should handle retries via higher-level logic
            }
        }
    }

    override fun subscribeOffers(handler: (MeshrabiyaClient.ResourceOffer) -> Unit) {
        // No-op; HTTP-backed transport won't receive mesh offers via POST /descriptor. Real
        // implementations should hook into Meshrabiya gossip and call handler for incoming offers.
    }

    override fun unsubscribeOffers(handler: (MeshrabiyaClient.ResourceOffer) -> Unit) {
        // No-op
    }
}
