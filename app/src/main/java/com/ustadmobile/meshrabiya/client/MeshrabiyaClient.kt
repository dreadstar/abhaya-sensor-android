package com.ustadmobile.meshrabiya.client

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import android.util.Log
import android.content.Context

/**
 * Minimal coroutine-friendly MeshrabiyaClient helper for sensors.
 * - broadcastResourceRequest publishes a ResourceRequest on the mesh and returns the requestId
 * - collectOffers waits for offers matching the requestId for up to `timeoutMs`
 * - uploadToEndpoint posts the blob file to a given endpoint, optionally including an auth token
 *
 * This is intentionally lightweight and uses an internal Transport interface so it can be
 * wired to the real Meshrabiya/MMCP implementation later. For now a stub transport can be
 * used in unit tests.
 */
import org.abhaya.sensor.meshrabiya.ResourceOfferVerifier
import org.abhaya.sensor.meshrabiya.OfferVerificationResult

class MeshrabiyaClient(
    private val transport: Transport,
    private val pubKeyResolver: suspend (responderNode: String) -> ByteArray? = { null },
    private val trustStore: org.abhaya.sensor.meshrabiya.TrustStore? = null,
    // Optional Android Context for production modules; when provided we should call
    // BetaTestLogger.getInstance(context) to obtain the logger instead of falling back
    // to reflective test instances.
    private val context: Context? = null
) {

    /**
     * Builder for MeshrabiyaClient to make providing a Context explicit in production code.
     * Example:
     *   val client = MeshrabiyaClient.Builder(transport).withContext(appContext).build()
     */
    class Builder() {
        private var transport: Transport? = null
        private var pubKeyResolver: suspend (String) -> ByteArray? = { null }
        private var context: Context? = null
        private var trustStore: org.abhaya.sensor.meshrabiya.TrustStore? = null

        fun withTransport(transport: Transport): Builder {
            this.transport = transport
            return this
        }

        fun pubKeyResolver(resolver: suspend (String) -> ByteArray?): Builder {
            this.pubKeyResolver = resolver
            return this
        }

        fun trustStore(ts: org.abhaya.sensor.meshrabiya.TrustStore?): Builder {
            this.trustStore = ts
            return this
        }

        fun withContext(ctx: Context): Builder {
            this.context = ctx.applicationContext
            return this
        }

        fun build(): MeshrabiyaClient = MeshrabiyaClient(
            transport ?: throw IllegalStateException("transport must be provided via withTransport()"),
            pubKeyResolver,
            trustStore,
            context
        )
    }

    companion object {
        /** Factory that requires Context for production usage. */
        fun withContext(context: Context, transport: Transport, pubKeyResolver: suspend (String) -> ByteArray? = { null }): MeshrabiyaClient {
            return MeshrabiyaClient(transport, pubKeyResolver, null, context.applicationContext)
        }

        /** Convenience: bind an HTTP-backed transport (sensor app helper). */
        fun bindHttp(baseUrl: String, authToken: String?, context: Context? = null): MeshrabiyaClient {
            // HTTP transport is only useful for posting descriptors and performing uploads.
            // It does NOT implement gossip messaging for ResourceRequest/ResourceOffer —
            // those MUST be done over a gossip/messaging transport so offers are exchanged
            // at the mesh level. Use bindGossip to connect a gossip-backed transport.
            val transport = HttpTransport(baseUrl, authToken)
            return MeshrabiyaClient(transport, { null }, context = context)
        }

        // NOTE: The preferred gossip binding is Flow-based. Avoid referencing service-level
        // VirtualNode types from this module; service modules should adapt their
        // VirtualNode/ MMCP flows into a Flow<String> and call bindGossipFromFlow.

        /**
         * Preferred: bind a gossip transport by providing a Flow of raw signed delegation JSON
         * payloads (the same jsonPayload that would be present inside an MmcpDelegationMessage),
         * and an optional suspend publish function used to emit outgoing signed JSON into the mesh.
         */
        fun bindGossipFromFlow(incomingSignedJsonFlow: kotlinx.coroutines.flow.Flow<String>?, publishSignedJson: (suspend (String) -> Unit)? = null, signerKeyPair: java.security.KeyPair? = null): Builder {
            val transport = com.ustadmobile.meshrabiya.client.GossipTransport(incomingSignedJsonFlow, publishSignedJson, signerKeyPair)
            return Builder().withTransport(transport)
        }
    }

    data class ResourceRequest(val requestId: String, val descriptorJson: String)
    data class ResourceOffer(
        val requestId: String,
        val responderNode: String,
        val uploadEndpoint: String,
        val uploadToken: String?,
        // optional raw JSON payload when the transport can provide the full delegation message
        val rawJson: String? = null
    )

    interface Transport {
        fun publishRequest(request: ResourceRequest)
        fun subscribeOffers(handler: (ResourceOffer) -> Unit)
        fun unsubscribeOffers(handler: (ResourceOffer) -> Unit)
    }

    private val offersChannel = Channel<ResourceOffer>(Channel.UNLIMITED)
    private val offerHandler: (ResourceOffer) -> Unit = { offer ->
        // If the transport provided the raw JSON for the offer, attempt cryptographic verification
        // before delivering to collectors. If verification fails, drop the offer.
        if (offer.rawJson != null) {
            // Perform verification asynchronously so we don't block the caller thread (important for
            // deterministic tests where transports may run on the test thread). parseAndVerify is suspend
            // so launch a coroutine to call it and deliver results to the offersChannel when ready.
            try {
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        val verified = ResourceOfferVerifier.parseAndVerify(offer.rawJson, expectedRequestId = offer.requestId, pubKeyResolver = pubKeyResolver, trustStore = trustStore)
                        when (verified) {
                            is OfferVerificationResult.Valid -> {
                                try { println("MeshrabiyaClient: verification valid for requestId=${offer.requestId}") } catch (_: Throwable) {}
                                val sendRes = offersChannel.trySend(offer)
                                try { println("MeshrabiyaClient: trySend result=${sendRes.isSuccess}") } catch (_: Throwable) {}
                            }
                            is OfferVerificationResult.Invalid -> {
                                try { println("MeshrabiyaClient: verification INVALID for requestId=${offer.requestId} reason=${verified.reason}") } catch (_: Throwable) {}
                                logWarn("Dropped invalid ResourceOffer: ${verified.reason}")
                            }
                        }
                    } catch (t: Throwable) {
                        logError("Error verifying ResourceOffer: ${t.message}", t)
                    }
                }
            } catch (t: Throwable) {
                // If launching fails for some reason, log and drop
                logError("Error launching verification coroutine: ${t.message}", t)
            }
        } else {
            // deliver all offers for collection
            offersChannel.trySend(offer)
        }
    }

    // Lightweight logging helpers: prefer BetaTestLogger when available; fallback to android.util.Log
    private fun logWarn(message: String) {
        // Prefer BetaTestLogger.getInstance(context) when a Context is available. Use reflection
        // so this module doesn't require a compile-time dependency on the BetaTestLogger class.
        try {
            val clazz = Class.forName("com.ustadmobile.meshrabiya.beta.BetaTestLogger")
            val logMethod = clazz.getMethod("log", Class.forName("com.ustadmobile.meshrabiya.beta.LogLevel"), String::class.java, String::class.java, Map::class.java, Throwable::class.java)
            val logLevelClass = Class.forName("com.ustadmobile.meshrabiya.beta.LogLevel")
            val warnLevel = logLevelClass.getField("WARN").get(null)

            if (context != null) {
                try {
                    val getInstance = clazz.getMethod("getInstance", android.content.Context::class.java)
                    val logger = getInstance.invoke(null, context)
                    logMethod.invoke(logger, warnLevel, "MeshrabiyaClient", message, emptyMap<String, String>(), null)
                    return
                } catch (_: Throwable) {
                    // fallback to createTestInstance below
                }
            }

            // No Context or getInstance failed — try creating a test instance
            val createTestInstance = clazz.getMethod("createTestInstance")
            val logger = createTestInstance.invoke(null)
            logMethod.invoke(logger, warnLevel, "MeshrabiyaClient", message, emptyMap<String, String>(), null)
            return
        } catch (_: Throwable) {
            // fall-through to android Log
        }
        try {
            Log.w("MeshrabiyaClient", message)
        } catch (_: Throwable) {
            // In unit tests android.util.Log methods may be unimplemented; swallow to avoid crashing
        }
    }

    private fun logError(message: String, t: Throwable?) {
        try {
            val clazz = Class.forName("com.ustadmobile.meshrabiya.beta.BetaTestLogger")
            val logMethod = clazz.getMethod("log", Class.forName("com.ustadmobile.meshrabiya.beta.LogLevel"), String::class.java, String::class.java, Map::class.java, Throwable::class.java)
            val logLevelClass = Class.forName("com.ustadmobile.meshrabiya.beta.LogLevel")
            val errorLevel = logLevelClass.getField("ERROR").get(null)

            if (context != null) {
                try {
                    val getInstance = clazz.getMethod("getInstance", android.content.Context::class.java)
                    val logger = getInstance.invoke(null, context)
                    logMethod.invoke(logger, errorLevel, "MeshrabiyaClient", message, emptyMap<String, String>(), t)
                    return
                } catch (_: Throwable) {
                    // fallback to createTestInstance below
                }
            }

            val createTestInstance = clazz.getMethod("createTestInstance")
            val logger = createTestInstance.invoke(null)
            logMethod.invoke(logger, errorLevel, "MeshrabiyaClient", message, emptyMap<String, String>(), t)
            return
        } catch (_: Throwable) {
            // fall-through
        }
        try {
            Log.e("MeshrabiyaClient", message, t)
        } catch (_: Throwable) {
            // In unit tests android.util.Log methods may be unimplemented; swallow to avoid crashing
        }
    }

    init {
        transport.subscribeOffers(offerHandler)
    }

    fun close() {
        try { transport.unsubscribeOffers(offerHandler) } catch (_: Exception) {}
        try { offersChannel.close() } catch (_: Exception) {}
    }

    suspend fun broadcastResourceRequest(descriptorJson: String, requestId: String): ResourceRequest {
        val req = ResourceRequest(requestId, descriptorJson)
        withContext(Dispatchers.IO) {
            transport.publishRequest(req)
        }
        return req
    }

    /**
     * Collect offers for a given requestId up to timeoutMs. Returns list of offers matched.
     */
    suspend fun collectOffers(requestId: String, timeoutMs: Long = 1000L): List<ResourceOffer> {
        val results = mutableListOf<ResourceOffer>()
        // Poll the offersChannel for up to timeoutMs. Use non-suspending tryReceive when
        // possible and only suspend briefly when the channel is empty. This avoids relying on
        // cancellation semantics that can interact poorly with test dispatchers.
        val deadline = System.currentTimeMillis() + timeoutMs
        try { println("MeshrabiyaClient: collectOffers start polling for requestId=$requestId timeoutMs=$timeoutMs") } catch (_: Throwable) {}
        while (System.currentTimeMillis() <= deadline) {
            val tryRes = offersChannel.tryReceive()
            if (tryRes.isSuccess) {
                val offer = tryRes.getOrNull() ?: continue
                try { println("MeshrabiyaClient: collectOffers got offer requestId=${offer.requestId}") } catch (_: Throwable) {}
                if (offer.requestId == requestId) results.add(offer)
                // continue draining any immediately available offers until channel is empty
                continue
            }

            // no offer currently available; suspend briefly to yield
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) break
            val sleep = if (remaining < 20) remaining else 10L
            kotlinx.coroutines.delay(sleep)
        }
        return results
    }

    /**
     * Simple HTTP POST of blobFile to endpoint. Sends X-Meshrabiya-Auth header if authToken non-null.
     * Throws IOException on non-2xx response.
     */
    suspend fun uploadToEndpoint(endpoint: String, blobFile: File, authToken: String?): Unit = withContext(Dispatchers.IO) {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 30_000
            addRequestProperty("Content-Type", "application/octet-stream")
            if (!authToken.isNullOrEmpty()) addRequestProperty("X-Meshrabiya-Auth", authToken)
        }

        blobFile.inputStream().use { input ->
            conn.outputStream.use { out ->
                input.copyTo(out)
            }
        }

        val code = conn.responseCode
        if (code < 200 || code >= 300) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
            throw java.io.IOException("Upload failed: HTTP $code - $err")
        }
    }
}
