package org.abhaya.sensor.meshrabiya

import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Data model for a minimal ResourceOffer used by the sensor when selecting a peer to accept storage.
 * Fields mirror the canonical example in the KNOWLEDGE doc.
 */
data class ResourceOffer(
    val requestId: String,
    val responderNode: String,
    val availableStorage: Long?,
    val latencyMs: Int?,
    val endpoint: String?,
    val expiresAtIso: String?,
    val timestampIso: String?,
    val rawJson: String,
    val signatureBase64: String?,
    val signerPubKeyBase64: String?
)

/**
 * Result of an offer verification attempt.
 */
sealed class OfferVerificationResult {
    data class Valid(val offer: ResourceOffer) : OfferVerificationResult()
    data class Invalid(val reason: String) : OfferVerificationResult()
}

/**
 * Small utility to parse and verify ResourceOffer JSON messages.
 *
 * Verification steps:
 *  - required fields present (requestId, responderNode)
 *  - expiry (expires_at) not in the past
 *  - signature verification (if signature present): supports embedded base64 signerPublicKey or
 *    a pluggable resolver that returns the public key bytes for a given responder node.
 *
 * Note: this helper canonicalizes the JSON payload by removing the `signature` and `signerPublicKey`
 * fields and producing a lexicographically-sorted-key JSON string before verifying the signature.
 * The signer MUST sign the identical canonical bytes. This approach keeps the verifier deterministic
 * and avoids variations from key ordering in raw JSON strings.
 */
object ResourceOfferVerifier {

    /**
     * Parse JSON and run validation + optional signature verification.
     *
     * @param json the incoming JSON string of the offer
     * @param expectedRequestId optional expected requestId to verify the offer is for our request
     * @param pubKeyResolver optional suspendable resolver that returns raw X.509 encoded public key bytes for a responderNode
     */
    suspend fun parseAndVerify(
        json: String,
        expectedRequestId: String? = null,
        pubKeyResolver: suspend (responderNode: String) -> ByteArray? = { null },
        trustStore: TrustStore? = null
    ): OfferVerificationResult {
        try {
            val jo = JSONObject(json)

            // Safely extract strings from JSONObject in a Kotlin-friendly way
            val requestId = if (jo.has("requestId") && !jo.isNull("requestId")) jo.getString("requestId") else null
                ?: return OfferVerificationResult.Invalid("missing requestId")
            val responderNode = if (jo.has("responderNode") && !jo.isNull("responderNode")) jo.getString("responderNode") else null
                ?: return OfferVerificationResult.Invalid("missing responderNode")

            if (expectedRequestId != null && expectedRequestId != requestId) {
                return OfferVerificationResult.Invalid("requestId mismatch: expected $expectedRequestId got $requestId")
            }

            val availableStorage = if (jo.has("availableStorage")) jo.optLong("availableStorage") else null
            val latencyMs = if (jo.has("latencyMs")) jo.optInt("latencyMs") else null
            val endpoint = if (jo.has("endpoint") && !jo.isNull("endpoint")) jo.getString("endpoint") else null
            val expiresAt = if (jo.has("expires_at") && !jo.isNull("expires_at")) jo.getString("expires_at") else null
            val timestamp = if (jo.has("timestamp") && !jo.isNull("timestamp")) jo.getString("timestamp") else null

            val signatureBase64 = if (jo.has("signature") && !jo.isNull("signature")) jo.getString("signature") else null
            val signerPubKeyBase64 = if (jo.has("signerPublicKey") && !jo.isNull("signerPublicKey")) jo.getString("signerPublicKey") else null

            // expiry check (if present) - quick ISO comparison: rely on lexical compare for ISO-8601
            if (expiresAt != null) {
                try {
                    val nowIso = java.time.Instant.now().toString()
                    if (expiresAt <= nowIso) {
                        return OfferVerificationResult.Invalid("offer expired at $expiresAt")
                    }
                } catch (t: Throwable) {
                    // If parsing fails, treat as invalid
                    return OfferVerificationResult.Invalid("invalid expires_at format")
                }
            }

            val offer = ResourceOffer(
                requestId = requestId,
                responderNode = responderNode,
                availableStorage = availableStorage,
                latencyMs = latencyMs,
                endpoint = endpoint,
                expiresAtIso = expiresAt,
                timestampIso = timestamp,
                rawJson = json,
                signatureBase64 = signatureBase64,
                signerPubKeyBase64 = signerPubKeyBase64
            )

            // If no signature present, treat as valid but warn (depending on policy you may reject unsigned offers)
            if (signatureBase64 == null) return OfferVerificationResult.Valid(offer)

            // Determine public key bytes
            var pubKeyBytes: ByteArray? = null
            if (!signerPubKeyBase64.isNullOrEmpty()) {
                try {
                    pubKeyBytes = Base64.getDecoder().decode(signerPubKeyBase64)
                } catch (e: IllegalArgumentException) {
                    return OfferVerificationResult.Invalid("invalid signerPublicKey base64")
                }
            }

            if (pubKeyBytes == null) {
                // try resolver
                pubKeyBytes = pubKeyResolver(responderNode)
                if (pubKeyBytes == null) return OfferVerificationResult.Invalid("no public key available for responderNode $responderNode")
            }

            // For delegation-capability messages, there may be a nested `capability` or `delegation` field
            // which itself is a signed token (JSON). We verify the outer message signature first,
            // then optionally validate the embedded token chain using the trustStore.

            // canonicalize JSON by removing signature and signerPublicKey and producing sorted keys
            val canonical = canonicalizeForSigning(jo)
            val sigBytes: ByteArray
            try {
                sigBytes = Base64.getDecoder().decode(signatureBase64)
            } catch (e: IllegalArgumentException) {
                return OfferVerificationResult.Invalid("invalid signature base64")
            }

            return try {
                val keySpec = X509EncodedKeySpec(pubKeyBytes)
                val kf = KeyFactory.getInstance("Ed25519")
                val pub = kf.generatePublic(keySpec)

                val verifier = Signature.getInstance("Ed25519")
                verifier.initVerify(pub)
                verifier.update(canonical.toByteArray(Charsets.UTF_8))
                val ok = verifier.verify(sigBytes)
                if (!ok) return OfferVerificationResult.Invalid("signature verification failed")

                // At this point outer signature is valid. If there's a nested capability/delegation token,
                // attempt to verify the chain using the trustStore.
                if (jo.has("capability") && !jo.isNull("capability") && trustStore != null) {
                    val capJson = jo.getJSONObject("capability").toString()
                    val chainValid = try {
                        verifyDelegationChain(capJson, trustStore)
                    } catch (t: Throwable) {
                        false
                    }
                    if (!chainValid) return OfferVerificationResult.Invalid("delegation chain invalid or untrusted")
                }

                // replay protection: check tokenId/nonce in trustStore if present
                if (jo.has("tokenId") && trustStore != null) {
                    val tokenId = jo.optString("tokenId", "")
                    if (tokenId.isNotEmpty()) {
                        if (trustStore.isSeenToken(tokenId)) return OfferVerificationResult.Invalid("replayed token")
                        trustStore.markTokenSeen(tokenId)
                    }
                }

                return OfferVerificationResult.Valid(offer)
            } catch (t: Exception) {
                OfferVerificationResult.Invalid("signature verification error: ${t.message}")
            }

        } catch (ex: Exception) {
            return OfferVerificationResult.Invalid("invalid JSON offer: ${ex.message}")
        }
    }

    /**
     * Produce a deterministic JSON string for signing/verification by copying keys except
     * `signature` and `signerPublicKey` and sorting keys lexicographically.
     */
    private fun canonicalizeForSigning(jo: JSONObject): String {
        val entries = mutableListOf<Pair<String, Any?>>()
        val it = jo.keys()
        while (it.hasNext()) {
            val k = it.next()
            if (k == "signature" || k == "signerPublicKey") continue
            entries.add(k to jo.opt(k))
        }
        entries.sortBy { it.first }

        val map = linkedMapOf<String, Any?>()
        for ((k, v) in entries) map[k] = v

        // JSONObject will render nulls as JSONObject.NULL; handle that
        val cleanJo = JSONObject()
        for ((k, v) in map) {
            if (v == null) cleanJo.put(k, JSONObject.NULL) else cleanJo.put(k, v)
        }
        return cleanJo.toString()
    }

    /**
     * Public helper for tests and callers to canonicalize a JSON string for signing/verification.
     */
    fun canonicalizeForSigningString(json: String): String {
        val jo = JSONObject(json)
        return canonicalizeForSigning(jo)
    }

    /**
     * Verify a delegation chain encoded as JSON. The token format is expected to be:
     * {"delegations": [ {"issuerPublicKey":"BASE64_X509","signature":"BASE64","payload":{...}}, ... ] }
     * where each delegation is signed by issuerPublicKey over the canonicalized payload.
     * The last payload typically contains the capability (scope, expires_at, subjectPubKey, nonce, etc.).
     */
    private fun verifyDelegationChain(chainJson: String, trustStore: TrustStore): Boolean {
        try {
            val jo = JSONObject(chainJson)
            if (!jo.has("delegations")) return false
            val arr = jo.getJSONArray("delegations")

            // Walk chain from root (0) to leaf (n-1). For each delegation, verify signature with issuerPublicKey.
            for (i in 0 until arr.length()) {
                val del = arr.getJSONObject(i)
                val issuerB64 = del.optString("issuerPublicKey", null) ?: return false
                val sigB64 = del.optString("signature", null) ?: return false
                val payload = del.optJSONObject("payload") ?: return false

                val issuerBytes = try { Base64.getDecoder().decode(issuerB64) } catch (_: IllegalArgumentException) { return false }
                val sigBytes = try { Base64.getDecoder().decode(sigB64) } catch (_: IllegalArgumentException) { return false }

                val keySpec = X509EncodedKeySpec(issuerBytes)
                val kf = KeyFactory.getInstance("Ed25519")
                val pub = kf.generatePublic(keySpec)

                val verifier = Signature.getInstance("Ed25519")
                verifier.initVerify(pub)
                val canon = canonicalizeForSigning(payload)
                verifier.update(canon.toByteArray(Charsets.UTF_8))
                if (!verifier.verify(sigBytes)) return false

                // check expiry on payload if present
                val expiresAt = if (payload.has("expires_at") && !payload.isNull("expires_at")) payload.getString("expires_at") else null
                if (expiresAt != null) {
                    val nowIso = java.time.Instant.now().toString()
                    if (expiresAt <= nowIso) return false
                }

                // Optional TOFU check: if issuer is unknown, record it.
                trustStore.recordObservedKey(issuerBytes)
            }

            // If we get here, chain signatures validated; trustStore can decide on trustworthiness separately.
            return true
        } catch (t: Throwable) {
            return false
        }
    }

}
