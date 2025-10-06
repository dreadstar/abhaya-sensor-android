package org.abhaya.sensor.meshrabiya

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class FakeApi : MeshrabiyaServiceApi {
    private val blobs = ConcurrentHashMap<String, String>()

    override suspend fun requestDelegatedStorage(descriptorJson: String): Acceptance {
        val descriptorId = "fake-desc-" + descriptorJson.hashCode().toString(16)
        return if (descriptorJson.contains("upload_token")) {
            blobs[descriptorId] = "blob-" + descriptorId
            Acceptance.UploadToken(descriptorId, "onion://fake/upload/$descriptorId", "signed-token-$descriptorId")
        } else {
            blobs[descriptorId] = "blob-" + descriptorId
            Acceptance.DirectAccept(descriptorId, "peer://socket/$descriptorId")
        }
    }

    override suspend fun waitForBlobId(descriptorId: String, timeoutMillis: Long): String? {
        return blobs[descriptorId]
    }
}

class MeshrabiyaClientTest {

    @Test
    fun testUploadTokenFlow() = runBlocking {
        val api = FakeApi()
        MeshrabiyaClient.bind(api).use { client ->
            val descriptor = "{\"schema\":\"sensor.v1\",\"upload_token\":true}"
            val acceptance = client.requestDelegatedStorage(descriptor)
            when (acceptance) {
                is Acceptance.UploadToken -> {
                    assertTrue(acceptance.uploadEndpoint.contains("onion://fake/upload"))
                    val blobId = client.waitForBlobId(acceptance.descriptorId)
                    assertNotNull(blobId)
                    assertTrue(blobId!!.startsWith("blob-"))
                }
                else -> fail("Expected UploadToken acceptance")
            }
        }
    }

    @Test
    fun testDirectAcceptFlow() = runBlocking {
        val api = FakeApi()
        MeshrabiyaClient.bind(api).use { client ->
            val descriptor = "{\"schema\":\"sensor.v1\"}"
            val acceptance = client.requestDelegatedStorage(descriptor)
            when (acceptance) {
                is Acceptance.DirectAccept -> {
                    assertTrue(acceptance.peerSocketInfo.contains("peer://socket"))
                    val blobId = client.waitForBlobId(acceptance.descriptorId)
                    assertNotNull(blobId)
                    assertTrue(blobId!!.startsWith("blob-"))
                }
                else -> fail("Expected DirectAccept acceptance")
            }
        }
    }
}
