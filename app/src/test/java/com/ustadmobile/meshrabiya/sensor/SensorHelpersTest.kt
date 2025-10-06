package com.ustadmobile.meshrabiya.sensor

import org.junit.Assert.*
import org.junit.Test

class SensorHelpersTest {

    @Test
    fun chunkSha256_knownVector() {
        val data = "hello world".toByteArray()
        val sha = SensorHelpers.chunkSha256(data)
    // SHA-256 for "hello world"
    assertEquals("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9", sha)
    }

    @Test
    fun makeEnvelope_containsCorrectFields() {
        val data = byteArrayOf(1,2,3,4,5)
        val envelope = SensorHelpers.makeEnvelope("stream1", "schema:1", data)
        assertEquals("stream1", envelope.streamId)
        assertEquals("schema:1", envelope.schemaId)
        assertEquals(5, envelope.length)
        assertEquals(SensorHelpers.chunkSha256(data), envelope.chunkSha256)
    }

    @Test
    fun serviceBinder_getUserId_and_onion() {
        val binder = object : SensorHelpers.ServiceBinder {
            override fun getOnionPubKey(): String? = "TEST_KEY"
            override fun getUserId(): String? = "user-123"
        }

        assertEquals("TEST_KEY", binder.getOnionPubKey())
        assertEquals("user-123", binder.getUserId())
    }
}
