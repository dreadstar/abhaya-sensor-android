package org.abhaya.sensor.meshrabiya

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

/**
 * Lightweight coroutine-friendly MeshrabiyaClient helper.
 * This file provides a small API surface used by the KNOWLEDGE doc examples.
 *
 * NOTE: This is a stubbed client intended for local integration and unit tests.
 * The real implementation should bind to the Meshrabiya AIDL service and
 * perform IPC. For now, the client calls into a pluggable [MeshrabiyaServiceApi]
 * which can be faked in tests.
 */

sealed class Acceptance {
    data class UploadToken(val descriptorId: String, val uploadEndpoint: String, val token: String) : Acceptance()
    data class DirectAccept(val descriptorId: String, val peerSocketInfo: String) : Acceptance()
}

interface MeshrabiyaServiceApi {
    suspend fun requestDelegatedStorage(descriptorJson: String): Acceptance
    suspend fun waitForBlobId(descriptorId: String, timeoutMillis: Long = 30_000L): String?
}

class MeshrabiyaClient private constructor(private val api: MeshrabiyaServiceApi) : Closeable {

    companion object {
        // In production this would perform a Context.bindService(...) to an AIDL-backed service.
        // For testability we expose a simple binder-free factory.
        fun bind(api: MeshrabiyaServiceApi): MeshrabiyaClient {
            return MeshrabiyaClient(api)
        }

        /**
         * Convenience factory that binds an HTTP-backed MeshrabiyaServiceApi.
         * baseUrl should include protocol and host, e.g. "http://127.0.0.1:8080" or
         * an onion URL. authToken is optional and will be sent as X-Meshrabiya-Auth.
         */
        fun bindHttp(baseUrl: String, authToken: String? = null): MeshrabiyaClient {
            return MeshrabiyaClient(HttpMeshrabiyaServiceApi(baseUrl, authToken))
        }
    }

    suspend fun requestDelegatedStorage(descriptorJson: String): Acceptance = withContext(Dispatchers.IO) {
        api.requestDelegatedStorage(descriptorJson)
    }

    suspend fun waitForBlobId(descriptorId: String, timeoutMillis: Long = 30_000L): String? = withContext(Dispatchers.IO) {
        api.waitForBlobId(descriptorId, timeoutMillis)
    }

    override fun close() {
        // unbind in production
    }
}
