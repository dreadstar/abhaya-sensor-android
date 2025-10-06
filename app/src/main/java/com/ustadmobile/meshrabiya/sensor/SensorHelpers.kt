package com.ustadmobile.meshrabiya.sensor

import java.security.MessageDigest

/** Small sensor helpers used by unit tests and the Meshrabiya client later. */
object SensorHelpers {
    fun chunkSha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    data class Envelope(val streamId: String, val schemaId: String, val length: Int, val chunkSha256: String)

    fun makeEnvelope(streamId: String, schemaId: String, data: ByteArray): Envelope {
        val sha = chunkSha256(data)
        return Envelope(streamId, schemaId, data.size, sha)
    }

    interface ServiceBinder {
        // Abstracts AIDL service binder calls so tests can inject fakes
        fun getOnionPubKey(): String?
        fun getUserId(): String?
    }
}
