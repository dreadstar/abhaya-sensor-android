package org.abhaya.sensor.meshrabiya

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

/**
 * Small in-process end-to-end style test that demonstrates the flow:
 *  - sensor requests delegated storage (descriptor)
 *  - service returns an UploadToken
 *  - sensor 'uploads' (simulated) and waits for blob id
 *  - an in-process compute worker is triggered on the blob id and produces a result blob
 *  - orchestrator stores the result and notifies the user (simulated notifier)
 */
class EndToEndUploadComputeNotificationTest {

    class InMemoryFakeApi : MeshrabiyaServiceApi {
        private val blobs = ConcurrentHashMap<String, String>()

        override suspend fun requestDelegatedStorage(descriptorJson: String): Acceptance {
            val descriptorId = "fake-desc-" + descriptorJson.hashCode().toString(16)
            return if (descriptorJson.contains("upload_token")) {
                // In a realistic flow the client would upload to acceptance.uploadEndpoint.
                // For the fake, we'll create the blob map entry only after the caller simulates upload.
                Acceptance.UploadToken(descriptorId, "onion://fake/upload/$descriptorId", "signed-token-$descriptorId")
            } else {
                blobs[descriptorId] = "blob-" + descriptorId
                Acceptance.DirectAccept(descriptorId, "peer://socket/$descriptorId")
            }
        }

        override suspend fun waitForBlobId(descriptorId: String, timeoutMillis: Long): String? {
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < timeoutMillis) {
                val v = blobs[descriptorId]
                if (v != null) return v
                // small backoff
                delay(20)
            }
            return null
        }

        // Test helper to simulate that the upload completed and the service assigned a blob id.
        fun simulateUploadCompletes(descriptorId: String) {
            blobs[descriptorId] = "blob-" + descriptorId
        }

        // Test helper to record results (not used by MeshrabiyaClient but useful for verification)
        val resultStore = ConcurrentHashMap<String, String>()
        fun storeResultBlob(blobId: String, payloadRef: String) {
            resultStore[blobId] = payloadRef
        }
    }

    class FakeNotifier {
        val notified = ConcurrentHashMap<String, String>()
        fun notifyUserForResult(taskId: String, resultBlobId: String) {
            // in real app this would call NotificationManager.notify(...)
            notified[taskId] = resultBlobId
        }
    }

    @Test
    fun testEndToEndUploadComputeNotification() = runBlocking {
        val api = InMemoryFakeApi()
        MeshrabiyaClient.bind(api).use { client ->
            val descriptor = "{\"schema\":\"sensor.v1\",\"upload_token\":true}"

            // 1) request delegated storage -> should get an UploadToken
            val acceptance = client.requestDelegatedStorage(descriptor)
            assertTrue("Expected UploadToken", acceptance is Acceptance.UploadToken)
            val uploadToken = acceptance as Acceptance.UploadToken

            // 2) simulate upload completing (sensor would upload to uploadEndpoint)
            api.simulateUploadCompletes(uploadToken.descriptorId)

            // 3) wait for blob id (sensor-side)
            val blobId = client.waitForBlobId(uploadToken.descriptorId)
            assertNotNull("blobId should be available after simulated upload", blobId)

            // 4) trigger compute (in-process fake worker). We'll simulate asynchronous worker processing.
            val notifier = FakeNotifier()
            val workerTaskId = "task-${System.currentTimeMillis()}"

            // Launch a fake worker that reads the blobId and produces a result blob id after a short delay
            val workerJob = launch(Dispatchers.Default) {
                // simulate reading the blob (would normally call DistributedStorageAgent.retrieveFile)
                // produce result
                withContext(Dispatchers.Default) {
                    delay(50)
                    val resultBlobId = "result-$blobId"
                    // store result in the fake API under resultStore (simulating service storing it)
                    api.storeResultBlob(resultBlobId, "computed-output-ref-for-$blobId")
                    // orchestrator: notify user that result available
                    notifier.notifyUserForResult(workerTaskId, resultBlobId)
                }
            }

            // Wait for worker to finish
            workerJob.join()

            // 5) Assert that result was stored and notification sent
            val stored = api.resultStore.entries.firstOrNull()
            assertNotNull("resultStore should contain produced result blob", stored)
            val storedBlobId = stored!!.key
            assertEquals("result-$blobId", storedBlobId)
            assertEquals(storedBlobId, notifier.notified[workerTaskId])
        }
    }
}
