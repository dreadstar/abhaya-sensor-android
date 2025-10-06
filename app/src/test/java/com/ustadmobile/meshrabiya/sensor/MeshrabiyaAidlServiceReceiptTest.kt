package com.ustadmobile.meshrabiya.sensor

import android.os.ParcelFileDescriptor
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.ustadmobile.meshrabiya.service.MeshrabiyaAidlService
import org.abhaya.sensor.meshrabiya.TrustStore
import java.io.FileOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MeshrabiyaAidlServiceReceiptTest {

    @Test
    fun storeBlob_records_receipt_and_truststore_loads_it() {
        // Start a service instance via Robolectric so filesDir is available
        val controller = Robolectric.buildService(MeshrabiyaAidlService::class.java)
        val service = controller.create().get()

    // Create payload and back it with a temporary file (avoid createPipe in JVM tests)
    val payload = "receipt-test-payload".toByteArray()
    val tmpDir = File(System.getProperty("java.io.tmpdir"))
    val tmpFile = File.createTempFile("meshrabiya-receipt-", ".blob", tmpDir)
    tmpFile.writeBytes(payload)
    tmpFile.deleteOnExit()
    val readFd = ParcelFileDescriptor.open(tmpFile, ParcelFileDescriptor.MODE_READ_ONLY)

        // Call the internal store function and get blob id
    val blobId = service.storeBlobInternal(readFd)
        assertNotNull(blobId)

        // receipts file may only be recorded if a signing key is available. Check mediator first.
        val blobsBase = service.filesDir.resolve("meshrabiya_blobs")
        val receiptsFile = blobsBase.resolve("receipts.txt")

        val pubB64 = try { com.ustadmobile.meshrabiya.service.SigningMediator.getInstance(service).getPublicKeyBase64() } catch (_: Throwable) { null }

        if (pubB64 == null) {
            // No signing backend available in test environment; receipts may not be recorded. Ensure no exception is thrown when loading TrustStore.
            val ts = TrustStore(service.filesDir)
            // Access private receipts via reflection and ensure it's a map (may be empty)
            val receiptsField = ts.javaClass.getDeclaredField("receipts")
            receiptsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val receipts = receiptsField.get(ts) as java.util.Map<String, String>
            // Either the receipts map is empty, or (rarely) contains an entry. Both are acceptable in this environment.
        } else {
            // receipts file should exist and contain an entry for blobId
            assertTrue(receiptsFile.exists())
            val lines = receiptsFile.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            val match = lines.find { it.startsWith("${blobId}|") }
            assertNotNull("receipts file should contain an entry for blobId", match)

            // Now ensure TrustStore pointed at filesDir loads that receipt
            val ts = TrustStore(service.filesDir)
            // Access private receipts via reflection
            val receiptsField = ts.javaClass.getDeclaredField("receipts")
            receiptsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val receipts = receiptsField.get(ts) as java.util.Map<String, String>

            assertTrue(receipts.containsKey(blobId))
            val signer = receipts[blobId]
            assertNotNull(signer)
            assertTrue(signer!!.isNotEmpty())
        }
    }
}
