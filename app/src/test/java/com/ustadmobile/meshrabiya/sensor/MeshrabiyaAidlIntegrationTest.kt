package com.ustadmobile.meshrabiya.sensor

import android.os.ParcelFileDescriptor
import com.ustadmobile.meshrabiya.api.IMeshrabiyaService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.FileOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.File

/**
 * Integration-style test that uses a fake IMeshrabiyaService.Stub to exercise AIDL stubs.
 * This runs as a JVM unit test by directly instantiating the generated Stub class.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeshrabiyaAidlIntegrationTest {

    @Test
    fun fakeService_store_and_read_blob_via_pfd() {
        // Create a tiny fake service that implements storeBlob/openBlob/readBlobRange
        val stub = object : IMeshrabiyaService.Stub() {
            private var lastBlob: ByteArray? = null

            override fun getOnionPubKey(): String? = "FAKE_KEY"
            override fun getKeyAlgorithm(): String? = "Ed25519"
            override fun getApiVersion(): Int = 1

            override fun signData(data: ByteArray?): ByteArray? = null
            override fun ensureMeshActive(): com.ustadmobile.meshrabiya.api.MeshStatus? = null
            override fun publishToMesh(data: ParcelFileDescriptor?, topic: String?): Int = -1

            override fun storeBlob(blob: ParcelFileDescriptor?): String? {
                if (blob == null) return ""
                // Read entire PFD into memory (small test only)
                val fis = ParcelFileDescriptor.AutoCloseInputStream(blob)
                lastBlob = fis.readAllBytes()
                fis.close()
                return "fake-blob-id"
            }

            override fun openBlob(blobId: String?): ParcelFileDescriptor? {
                val data = lastBlob ?: return null
                // For unit tests, back the data with a temporary file and return a read ParcelFileDescriptor
                val tmpDir = File(System.getProperty("java.io.tmpdir"))
                val tmpFile = File.createTempFile("meshrabiya-stub-", ".blob", tmpDir)
                try {
                    FileOutputStream(tmpFile).use { out -> out.write(data) }
                    return ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } catch (t: Throwable) {
                    try { tmpFile.delete() } catch (_: Throwable) {}
                    return null
                }
            }

            override fun readBlobRange(blobId: String?, offset: Long, length: Int): ByteArray? {
                val data = lastBlob ?: return null
                if (offset < 0 || offset >= data.size) return ByteArray(0)
                val to = kotlin.math.min(data.size.toLong(), offset + length)
                return data.sliceArray(offset.toInt() until to.toInt())
            }

            override fun requestCompute(taskSpec: ByteArray?, cb: com.ustadmobile.meshrabiya.api.IOperationCallback?): Int = -1
        }

        // Compose a small payload and write it into a ParcelFileDescriptor via a pipe
        val payload = "integration-test-payload".toByteArray()
            // Use a temp file and open a read ParcelFileDescriptor for unit tests instead
            val tmpDir = File(System.getProperty("java.io.tmpdir"))
            val tmpFile = File.createTempFile("meshrabiya-int-", ".blob", tmpDir)
            tmpFile.writeBytes(payload)
            val readFd = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY)

    // Write payload to writeFd
            // No longer needed since we are using a temp file

        // Call storeBlob and verify returned id
        val blobId = stub.storeBlob(readFd)
        assertEquals("fake-blob-id", blobId)

        // Now openBlob and read back contents
        val readBackFd = stub.openBlob(blobId)
        assertNotNull(readBackFd)
    val readBytes = ParcelFileDescriptor.AutoCloseInputStream(readBackFd).use { it.readAllBytes() }

        assertArrayEquals(payload, readBytes)

        // Test readBlobRange
        val slice = stub.readBlobRange(blobId, 0, 5)
        assertArrayEquals(payload.sliceArray(0..4), slice)
    }
}
