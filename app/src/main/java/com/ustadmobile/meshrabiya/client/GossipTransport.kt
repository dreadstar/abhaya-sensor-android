package com.ustadmobile.meshrabiya.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * GossipTransport that wires directly into the MMCP/VirtualNode stack.
 *
 * Behavior:
 * - publishRequest: wraps the provided descriptor JSON into a canonical delegation
 *   wrapper (__delegation_type + payload) and sends an MmcpDelegationMessage using
 *   MmcpMessageFactory -> VirtualNode.sendMessage.
 * - subscribeOffers: listens for incoming MmcpDelegationMessage instances on the
 *   VirtualNode and delivers the raw signed JSON payload string (the jsonPayload
 *   field of MmcpDelegationMessage) to the MeshrabiyaClient via the ResourceOffer.rawJson
 *   field. Minimal extraction of `requestId` is performed so higher-level collectors
 *   can filter offers; full cryptographic verification and canonical parsing is
 *   delegated to `ResourceOfferVerifier.parseAndVerify(...)` inside `MeshrabiyaClient`.
 */

/**
 * GossipTransport that accepts a flow of raw signed delegation JSON strings.
 *
 * This avoids a hard compile-time dependency on `VirtualNode` in the sensor app
 * module. The same semantic contract applies: incoming delegation wrappers (the
 * same JSON bytes that were signed) must be delivered as `rawJson` to the client
 * so the verifier can canonicalize and verify the signature.
 */
class GossipTransport(
    /** Flow emitting raw signed delegation JSON strings (MmcpDelegationMessage.jsonPayload). */
    private val incomingSignedJsonFlow: Flow<String>? = null,
    /** Optional lambda to send a signed delegation JSON string into the mesh. */
    private val publishSignedJson: (suspend (String) -> Unit)? = null,
    /** Optional signer KeyPair for outgoing wrapper signing (used by callers that do signing). */
    private val signerKeyPair: java.security.KeyPair? = null,
    /** CoroutineScope used to collect the incoming flow. Allows tests to inject a deterministic scope. */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job()),
) : MeshrabiyaClient.Transport {

    init {
        incomingSignedJsonFlow?.let { flow ->
            scope.launch {
                try {
                    flow.collect { payload ->
                        try {
                            val wrapperJo = JSONObject(payload)
                            val inner = if (wrapperJo.has("payload") && !wrapperJo.isNull("payload")) wrapperJo.get("payload") else null
                            val requestId = try {
                                when (inner) {
                                    is JSONObject -> inner.optString("requestId", "")
                                    is String -> JSONObject(inner).optString("requestId", "")
                                    else -> wrapperJo.optString("requestId", "")
                                }
                            } catch (_: Throwable) { "" }

                            val offer = MeshrabiyaClient.ResourceOffer(
                                requestId = requestId,
                                responderNode = "",
                                uploadEndpoint = "",
                                uploadToken = null,
                                rawJson = payload
                            )
                            deliverOffer(offer)
                        } catch (_: Throwable) {
                            // ignore malformed JSON wrapper
                        }
                    }
                } catch (_: Throwable) {
                }
            }
        }
    }

    // Thin abstraction to allow tests or alternate wiring; implemented by MeshrabiyaClient
    private var offerCallback: ((MeshrabiyaClient.ResourceOffer) -> Unit)? = null

    private fun deliverOffer(offer: MeshrabiyaClient.ResourceOffer) {
        try {
            // Debug: print when the transport attempts to deliver an offer (tests will capture stdout)
            try { println("GossipTransport.deliverOffer -> requestId=${offer.requestId} rawJsonPresent=${offer.rawJson != null}") } catch (_: Throwable) {}
            offerCallback?.invoke(offer)
        } catch (_: Throwable) {}
    }

    override fun publishRequest(request: MeshrabiyaClient.ResourceRequest) {
        try {
            val wrapper = JSONObject()
            val payloadJo = try { JSONObject(request.descriptorJson) } catch (_: Throwable) { JSONObject().put("raw", request.descriptorJson) }
            wrapper.put("__delegation_type", "ResourceRequest")
            wrapper.put("payload", payloadJo)

            // If caller provided a publish lambda, hand them the signed JSON string.
            // The publishSignedJson lambda may perform signing or wrap into MMCP.
            val signedJson = wrapper.toString()
            publishSignedJson?.let { suspendFn ->
                kotlinx.coroutines.GlobalScope.launch { try { kotlinx.coroutines.runBlocking { suspendFn(signedJson) } } catch (_: Throwable) {} }
            }
        } catch (_: Throwable) {
        }
    }

    override fun subscribeOffers(handler: (MeshrabiyaClient.ResourceOffer) -> Unit) {
        this.offerCallback = handler
    }

    override fun unsubscribeOffers(handler: (MeshrabiyaClient.ResourceOffer) -> Unit) {
        // best-effort unsubscribe - if the same handler instance is provided, clear it
        if (this.offerCallback == handler) this.offerCallback = null
    }
}
