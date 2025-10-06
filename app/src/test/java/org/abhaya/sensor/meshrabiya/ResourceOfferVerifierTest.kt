package org.abhaya.sensor.meshrabiya

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class ResourceOfferVerifierTest {

    private fun signPayload(payload: String, kpBytes: java.security.KeyPair): String {
        val signer = Signature.getInstance("Ed25519")
        signer.initSign(kpBytes.private)
        signer.update(payload.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    private fun toJsonString(map: Map<String, Any?>): String {
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((k, v) in map) {
            if (!first) sb.append(',')
            first = false
            sb.append('"').append(k.replace("\\", "\\\\").replace("\"", "\\\"")).append('"').append(':')
            when (v) {
                null -> sb.append("null")
                is String -> sb.append('"').append(v.replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
                is Boolean, is Number -> sb.append(v.toString())
                else -> sb.append('"').append(v.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append('"')
            }
        }
        sb.append('}')
        return sb.toString()
    }

    private fun canonicalize(map: Map<String, Any?>): String {
        val filtered = map.filterKeys { it != "signature" && it != "signerPublicKey" }
        val sorted = filtered.toSortedMap()
        return toJsonString(sorted)
    }

    @Test
    fun valid_signed_offer_is_accepted() = runBlocking {
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp = kpg.generateKeyPair()

        val m = linkedMapOf<String, Any?>()
        m["type"] = "ResourceOffer"
        m["requestId"] = "r1"
        m["responderNode"] = "onion:abc"
        m["availableStorage"] = 1000
        m["timestamp"] = java.time.Instant.now().toString()

    val unsigned = toJsonString(m)
    val canon = ResourceOfferVerifier.canonicalizeForSigningString(unsigned)
    val s = Signature.getInstance("Ed25519")
    s.initSign(kp.private)
    s.update(canon.toByteArray(Charsets.UTF_8))
    val sigB64 = Base64.getEncoder().encodeToString(s.sign())

        val final = LinkedHashMap(m)
        final["signature"] = sigB64
        final["signerPublicKey"] = Base64.getEncoder().encodeToString(kp.public.encoded)

        val ts = TrustStore(null)

        val res = ResourceOfferVerifier.parseAndVerify(toJsonString(final), expectedRequestId = "r1", pubKeyResolver = { null }, trustStore = ts)
        if (res is OfferVerificationResult.Invalid) {
            fail("Verification failed: ${res.reason}")
        }
        assertTrue(res is OfferVerificationResult.Valid)
    }

    @Test
    fun expired_offer_is_rejected() = runBlocking {
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp = kpg.generateKeyPair()

        val m = linkedMapOf<String, Any?>()
        m["type"] = "ResourceOffer"
        m["requestId"] = "r2"
        m["responderNode"] = "onion:abc"
        m["timestamp"] = java.time.Instant.now().toString()
        m["expires_at"] = "2000-01-01T00:00:00Z"

    val unsigned = toJsonString(m)
    val canon = ResourceOfferVerifier.canonicalizeForSigningString(unsigned)
    val s = Signature.getInstance("Ed25519")
    s.initSign(kp.private)
    s.update(canon.toByteArray(Charsets.UTF_8))
    val sig = Base64.getEncoder().encodeToString(s.sign())

        val final = LinkedHashMap(m)
        final["signature"] = sig
        final["signerPublicKey"] = Base64.getEncoder().encodeToString(kp.public.encoded)

    val res = ResourceOfferVerifier.parseAndVerify(toJsonString(final), expectedRequestId = "r2", pubKeyResolver = { null }, trustStore = TrustStore(null))
    assertTrue(res is OfferVerificationResult.Invalid)
    }

    @Test
    fun replayed_token_is_rejected() = runBlocking {
        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp = kpg.generateKeyPair()

        val m = linkedMapOf<String, Any?>()
        m["type"] = "ResourceOffer"
        m["requestId"] = "r3"
        m["responderNode"] = "onion:abc"
        m["timestamp"] = java.time.Instant.now().toString()
        m["tokenId"] = "tok-1"

    val unsigned = toJsonString(m)
    val canon = ResourceOfferVerifier.canonicalizeForSigningString(unsigned)
    val s = Signature.getInstance("Ed25519")
    s.initSign(kp.private)
    s.update(canon.toByteArray(Charsets.UTF_8))
    val sig = Base64.getEncoder().encodeToString(s.sign())

        val final = LinkedHashMap(m)
        final["signature"] = sig
        final["signerPublicKey"] = Base64.getEncoder().encodeToString(kp.public.encoded)

        val ts = TrustStore(null)
        // first time should be accepted
    val r1 = ResourceOfferVerifier.parseAndVerify(toJsonString(final), expectedRequestId = "r3", pubKeyResolver = { null }, trustStore = ts)
    if (r1 is OfferVerificationResult.Invalid) fail("first verification failed: ${r1.reason}")
    assertTrue(r1 is OfferVerificationResult.Valid)

        // second time should be rejected as replay
    val r2 = ResourceOfferVerifier.parseAndVerify(toJsonString(final), expectedRequestId = "r3", pubKeyResolver = { null }, trustStore = ts)
    if (r2 is OfferVerificationResult.Valid) fail("replayed token was unexpectedly accepted")
    assertTrue(r2 is OfferVerificationResult.Invalid)
    }
}
