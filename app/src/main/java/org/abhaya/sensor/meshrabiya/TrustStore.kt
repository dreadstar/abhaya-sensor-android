package org.abhaya.sensor.meshrabiya

import java.io.File
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal TrustStore implementing TOFU, seen-token cache, and revoked keys cache.
 * Persisted storage is optional; this implementation keeps in-memory store and an optional file-backed
 * dump for observations (simple newline-delimited Base64 keys).
 */
class TrustStore(private val storageFile: File? = null) {

    // observed public keys (base64 encoded) -> observation count
    private val observed = ConcurrentHashMap<String, Int>()

    // revoked public keys (base64 encoded)
    private val revoked = ConcurrentHashMap.newKeySet<String>()

    // seen tokens to prevent replays
    private val seenTokens = ConcurrentHashMap.newKeySet<String>()

    // receipts: tokenId -> signerPubKeyBase64
    private val receipts = ConcurrentHashMap<String, String>()

    // Files for persistence (if storageFile is a directory)
    private val observedFile: File? = storageFile?.let { if (it.isDirectory) File(it, "observed_keys.txt") else File(it.parentFile, "observed_keys.txt") }
    private val revokedFile: File? = storageFile?.let { if (it.isDirectory) File(it, "revoked_keys.txt") else File(it.parentFile, "revoked_keys.txt") }
    private val receiptsFile: File? = storageFile?.let { if (it.isDirectory) File(it, "receipts.txt") else File(it.parentFile, "receipts.txt") }

    init {
        // Load observed keys
        try {
            observedFile?.let { f ->
                if (f.exists()) {
                    f.readLines().forEach { line ->
                        val key = line.trim()
                        if (key.isNotEmpty()) observed[key] = (observed[key] ?: 0) + 1
                    }
                }
            }
        } catch (_: Throwable) { }

        // Load revoked keys
        try {
            revokedFile?.let { f ->
                if (f.exists()) {
                    f.readLines().forEach { line ->
                        val key = line.trim()
                        if (key.isNotEmpty()) revoked.add(key)
                    }
                }
            }
        } catch (_: Throwable) { }

        // Load receipts
        try {
            receiptsFile?.let { f ->
                if (f.exists()) {
                    f.readLines().forEach { line ->
                        val parts = line.trim().split('|', limit = 2)
                        if (parts.size == 2) {
                            val token = parts[0].trim()
                            val signer = parts[1].trim()
                            if (token.isNotEmpty() && signer.isNotEmpty()) receipts[token] = signer
                        }
                    }
                }
            }
        } catch (_: Throwable) { }
    }

    fun recordObservedKey(pubKeyBytes: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(pubKeyBytes)
        observed.compute(b64) { _, v -> (v ?: 0) + 1 }
        try {
            observedFile?.let { it.parentFile?.mkdirs(); it.appendText("$b64\n") }
        } catch (_: Throwable) { }
    }

    fun isRevoked(pubKeyBytes: ByteArray): Boolean {
        val b64 = Base64.getEncoder().encodeToString(pubKeyBytes)
        return revoked.contains(b64)
    }

    fun revoke(pubKeyBytes: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(pubKeyBytes)
        if (revoked.add(b64)) {
            try { revokedFile?.let { it.parentFile?.mkdirs(); it.appendText("$b64\n") } } catch (_: Throwable) { }
        }
    }

    fun getTrustScore(pubKeyBytes: ByteArray): Int {
        val b64 = Base64.getEncoder().encodeToString(pubKeyBytes)
        if (revoked.contains(b64)) return 0
        return observed[b64] ?: 0
    }

    fun isSeenToken(tokenId: String): Boolean = seenTokens.contains(tokenId)

    fun markTokenSeen(tokenId: String) { seenTokens.add(tokenId) }

    /** Store a signed receipt for a token (tokenId -> signerPublicKeyBase64) */
    fun recordReceipt(tokenId: String, signerPubKeyBase64: String) {
        receipts[tokenId] = signerPubKeyBase64
        try {
            receiptsFile?.let { it.parentFile?.mkdirs(); it.appendText("${tokenId}|${signerPubKeyBase64}\n") }
        } catch (_: Throwable) { }
    }

    /** Merge a list of revoked public keys (base64 encoded) obtained via gossip. Returns number of newly added revocations. */
    fun mergeRevocations(revokedKeysBase64: Collection<String>): Int {
        var added = 0
        for (k in revokedKeysBase64) {
            val trimmed = k.trim()
            if (trimmed.isEmpty()) continue
            if (revoked.add(trimmed)) {
                try { revokedFile?.let { it.parentFile?.mkdirs(); it.appendText("$trimmed\n") } } catch (_: Throwable) { }
                added++
            }
        }
        return added
    }

    // For tests/debugging
    fun clear() {
        observed.clear()
        revoked.clear()
        seenTokens.clear()
        receipts.clear()
        try { observedFile?.writeText("") } catch (_: Throwable) {}
        try { revokedFile?.writeText("") } catch (_: Throwable) {}
        try { receiptsFile?.writeText("") } catch (_: Throwable) {}
    }
}
