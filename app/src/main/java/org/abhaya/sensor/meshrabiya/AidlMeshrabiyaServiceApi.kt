package org.abhaya.sensor.meshrabiya

import android.content.Context
import android.os.ParcelFileDescriptor
import com.ustadmobile.meshrabiya.api.IMeshrabiyaService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * A simple AIDL-backed implementation of MeshrabiyaServiceApi.
 *
 * Notes on behavior: IMeshrabiyaService does not currently expose a dedicated
 * requestDelegatedStorage method. As a pragmatic bridge, this adapter stores
 * the JSON descriptor as a small blob using `storeBlob(ParcelFileDescriptor)`
 * and returns an UploadToken whose descriptorId is the returned blob id.
 *
 * waitForBlobId polls `openBlob(descriptorId)` until the authoritative blob
 * is available (or timeout). This is a best-effort bridge; server-side
 * additions to AIDL could provide better async acceptance callbacks.
 */
class AidlMeshrabiyaServiceApi(private val aidl: IMeshrabiyaService) : MeshrabiyaServiceApi {

    override suspend fun requestDelegatedStorage(descriptorJson: String): Acceptance = withContext(Dispatchers.IO) {
        // Do not prefer local HTTP; use the AIDL pipe-based descriptor storage.
        // The preferred upload target should be chosen by the selection algorithm
        // among offers received by the coordinator; even when local storage is
        // selected, the mesh address is acceptable. Keep the AIDL adapter simple
        // and store the descriptor as a small blob via storeBlob.

        // Write descriptorJson into a ParcelFileDescriptor pipe and call storeBlob
        val pipe = ParcelFileDescriptor.createPipe()
        val readFd = pipe[0]
        val writeFd = pipe[1]

        var wrote = false
        try {
            // Write descriptor payload to write end and close it
            FileOutputStream(writeFd.fileDescriptor).use { fos ->
                fos.write(descriptorJson.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            wrote = true
        } catch (t: Throwable) {
            try { writeFd.close() } catch (_: Throwable) {}
            try { readFd.close() } catch (_: Throwable) {}
            throw IOException("Failed to write descriptor pipe", t)
        }

        try {
            val blobId = aidl.storeBlob(readFd) ?: throw IOException("storeBlob returned null")
            // Return an UploadToken with placeholder endpoint/token. The descriptorId is the stored blob id.
            return@withContext Acceptance.UploadToken(descriptorId = blobId, uploadEndpoint = "", token = "")
        } finally {
            try { readFd.close() } catch (_: Throwable) {}
        }
    }

    override suspend fun waitForBlobId(descriptorId: String, timeoutMillis: Long): String? = withContext(Dispatchers.IO) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            try {
                val pfd = aidl.openBlob(descriptorId)
                if (pfd != null) {
                    try { pfd.close() } catch (_: Throwable) {}
                    return@withContext descriptorId
                }
            } catch (_: Exception) {
                // ignore and retry
            }

            // Sleep a small interval before retrying
            Thread.sleep(200)
        }

        return@withContext null
    }
}
