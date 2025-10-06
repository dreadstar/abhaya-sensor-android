package org.abhaya.sensor.meshrabiya

import com.ustadmobile.meshrabiya.api.IMeshrabiyaService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume
import java.io.FileOutputStream
import java.io.File
import android.os.ParcelFileDescriptor

class AidlMeshrabiyaServiceApiTest {

    @Test
    fun testRequestDelegatedStorage_and_waitForBlobId() = runBlocking {
        // This test uses android.os.ParcelFileDescriptor APIs not available in plain JVM unit tests.
        // Skip when running on the JVM (these are covered by instrumentation/device tests).
        Assume.assumeFalse("Skip AIDL test on JVM", System.getProperty("java.vendor")?.contains("Android") != true)
        // Fake AIDL stub that stores the last written bytes and responds to openBlob
        val stub = object : IMeshrabiyaService.Stub() {
            private var lastBlob: ByteArray? = null

            override fun getOnionPubKey(): String? = null
            override fun getKeyAlgorithm(): String? = null
            override fun getApiVersion(): Int = 1
            override fun signData(data: ByteArray?): ByteArray? = null
            override fun ensureMeshActive(): com.ustadmobile.meshrabiya.api.MeshStatus? = null
            override fun publishToMesh(data: ParcelFileDescriptor?, topic: String?): Int = -1

            override fun storeBlob(blob: ParcelFileDescriptor?): String? {
                if (blob == null) return null
                ParcelFileDescriptor.AutoCloseInputStream(blob).use { fis ->
                    lastBlob = fis.readAllBytes()
                }
                return "fake-desc-id"
            }

            override fun openBlob(blobId: String?): ParcelFileDescriptor? {
                val data = lastBlob ?: return null
                val tmpDir = File(System.getProperty("java.io.tmpdir"))
                val tmpFile = File.createTempFile("meshrabiya-api-", ".blob", tmpDir)
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

        val api = AidlMeshrabiyaServiceApi(stub)

        val descriptor = "{\"schema\":\"sensor.v1\",\"meta\":\"x\"}"
        val acceptance = api.requestDelegatedStorage(descriptor)
        assertTrue(acceptance is Acceptance.UploadToken)
        val descId = (acceptance as Acceptance.UploadToken).descriptorId
        assertEquals("fake-desc-id", descId)

        val blobId = api.waitForBlobId(descId, 5_000L)
        assertEquals(descId, blobId)
    }
}
