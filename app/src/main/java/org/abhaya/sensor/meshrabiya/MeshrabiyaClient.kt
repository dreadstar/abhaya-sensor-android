package org.abhaya.sensor.meshrabiya

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

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
        fun bind(api: MeshrabiyaServiceApi): MeshrabiyaClient {
            return MeshrabiyaClient(api)
        }
        /** Convenience factory for HTTP-backed service API (local loopback or remote onion). */
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
