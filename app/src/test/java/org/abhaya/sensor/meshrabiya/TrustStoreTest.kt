package org.abhaya.sensor.meshrabiya

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.KeyPairGenerator
import java.util.Base64

class TrustStoreTest {

    @Test
    fun record_and_revoke_and_score() {
        val tmp = File.createTempFile("truststore", "tmp")
        tmp.deleteOnExit()
        val ts = TrustStore(tmp)
        ts.clear()

        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp = kpg.generateKeyPair()
        val pub = kp.public.encoded

        assertEquals(0, ts.getTrustScore(pub))
        ts.recordObservedKey(pub)
        assertTrue(ts.getTrustScore(pub) >= 1)

        ts.revoke(pub)
        assertTrue(ts.isRevoked(pub))
        assertEquals(0, ts.getTrustScore(pub))
    }

    @Test
    fun seen_token_prevents_replay() {
        val ts = TrustStore(null)
        ts.clear()
        val token = "token-123"
        assertFalse(ts.isSeenToken(token))
        ts.markTokenSeen(token)
        assertTrue(ts.isSeenToken(token))
    }

    @Test
    fun mergeRevocations_and_persistence() {
        val tmpDir = kotlin.io.path.createTempDirectory().toFile()
        tmpDir.deleteOnExit()
        val ts = TrustStore(tmpDir)
        ts.clear()

        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp1 = kpg.generateKeyPair()
        val kp2 = kpg.generateKeyPair()
        val pub1 = Base64.getEncoder().encodeToString(kp1.public.encoded)
        val pub2 = Base64.getEncoder().encodeToString(kp2.public.encoded)

        val added = ts.mergeRevocations(listOf(pub1, pub2, pub1, "   "))
        assertEquals(2, added)

        // both keys should now be considered revoked
        assertTrue(ts.isRevoked(kp1.public.encoded))
        assertTrue(ts.isRevoked(kp2.public.encoded))

        // revoked file should exist and contain the keys
        val revokedFile = File(tmpDir, "revoked_keys.txt")
        assertTrue(revokedFile.exists())
        val lines = revokedFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(lines.contains(pub1))
        assertTrue(lines.contains(pub2))

        // merging again should not add more
        val added2 = ts.mergeRevocations(listOf(pub1, pub2))
        assertEquals(0, added2)
    }

    @Test
    fun recordReceipt_and_clear() {
        // Use a temp directory to verify receipts persist
        val tmpDir = kotlin.io.path.createTempDirectory().toFile()
        tmpDir.deleteOnExit()
        val ts = TrustStore(tmpDir)
        ts.clear()

        val kpg = KeyPairGenerator.getInstance("Ed25519")
        val kp = kpg.generateKeyPair()
        val pubB64 = Base64.getEncoder().encodeToString(kp.public.encoded)
        val tokenId = "tok-xyz"

        ts.recordReceipt(tokenId, pubB64)

        // receipts map is private; access via reflection for test verification
        val receiptsField = ts.javaClass.getDeclaredField("receipts")
        receiptsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val receipts = receiptsField.get(ts) as java.util.Map<String, String>

        assertEquals(pubB64, receipts[tokenId])

        // Creating a new TrustStore pointed at the same dir should load the persisted receipt
        val ts2 = TrustStore(tmpDir)
        val receiptsField2 = ts2.javaClass.getDeclaredField("receipts")
        receiptsField2.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val receipts2 = receiptsField2.get(ts2) as java.util.Map<String, String>
        assertEquals(pubB64, receipts2[tokenId])

        ts.clear()
        assertTrue(receipts.isEmpty())
    }
}
