package com.ustadmobile.meshrabiya.client

import com.ustadmobile.meshrabiya.api.IMeshrabiyaService
import java.io.FileOutputStream
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * A Transport implementation that publishes delegation ResourceRequest messages
 * by calling the Meshrabiya AIDL service's publishToMesh. This allows sensor
 * code to broadcast small JSON messages (descriptors / requests) onto the mesh
 * without pulling in VM-level MMCP stacks.
 *
 * Note: offer subscription is not available via IMeshrabiyaService today, so
 * subscribeOffers/unsubscribeOffers are currently no-ops. Receiving offers
 * must be implemented via a future callback API or by polling server-side
 * resources.
 */
class AidlTransport(private val aidl: IMeshrabiyaService) : MeshrabiyaClient.Transport {

    companion object {
        private const val TAG = "AidlTransport"
        private const val DEFAULT_TOPIC = "delegation"
    }

    override fun publishRequest(request: MeshrabiyaClient.ResourceRequest) {
        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readFd = pipe[0]
            val writeFd = pipe[1]

            // Write JSON payload to the write end of the pipe and close
            FileOutputStream(writeFd.fileDescriptor).use { fos ->
                fos.write(request.descriptorJson.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            // Publish on the mesh under a fixed topic. publishToMesh returns int error code.
            try {
                aidl.publishToMesh(readFd, DEFAULT_TOPIC)
            } finally {
                try { readFd.close() } catch (_: Throwable) {}
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to publish request via AIDL", t)
        }
    }

    override fun subscribeOffers(handler: (MeshrabiyaClient.ResourceOffer) -> Unit) {
        // Not supported via current AIDL surface. No-op for now.
        Log.w(TAG, "subscribeOffers called but not implemented in AidlTransport")
    }

    override fun unsubscribeOffers(handler: (MeshrabiyaClient.ResourceOffer) -> Unit) {
        // No-op
    }
}
