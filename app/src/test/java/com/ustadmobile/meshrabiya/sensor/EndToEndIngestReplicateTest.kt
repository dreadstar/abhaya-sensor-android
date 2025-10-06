package com.ustadmobile.meshrabiya.sensor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EndToEndIngestReplicateTest {
    @Test
    fun testIngestToStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()

    // Test HttpStreamIngestor: provide a custom URL factory to capture POSTs deterministically
        val uploadedLatch = java.util.concurrent.CountDownLatch(1)
        val prevFactory = org.abhaya.sensor.meshrabiya.HttpStreamIngestor.urlFactory
        org.abhaya.sensor.meshrabiya.HttpStreamIngestor.urlFactory = { endpoint ->
            val handler = object : URLStreamHandler() {
                override fun openConnection(u: URL?): URLConnection {
                    // Return a minimal HttpURLConnection so HttpStreamIngestor's cast succeeds
                    return object : java.net.HttpURLConnection(u) {
                        private val out = java.io.ByteArrayOutputStream()
                        private var connected = false

                        override fun connect() { connected = true }

                        override fun usingProxy(): Boolean = false

                        override fun getOutputStream(): java.io.OutputStream = out

                        override fun getInputStream(): java.io.InputStream {
                            // when read, mark uploaded and return sample response
                            uploadedLatch.countDown()
                            return "OK".byteInputStream()
                        }

                        override fun getResponseCode(): Int = 200

                        override fun disconnect() {}
                    }
                }
            }
            URL(null, endpoint, handler)
        }

        val httpIngestor = org.abhaya.sensor.meshrabiya.HttpStreamIngestor("http://example.local/store", null)
        httpIngestor.start()
        httpIngestor.ingestSensorReading("s1", System.currentTimeMillis(), "payload".toByteArray())

    // wait for upload to be observed by the fake connection
        try {
            val ok = uploadedLatch.await(2, java.util.concurrent.TimeUnit.SECONDS)
            assertTrue("HttpStreamIngestor should have attempted upload", ok)
        } finally {
            // restore factory
            org.abhaya.sensor.meshrabiya.HttpStreamIngestor.urlFactory = prevFactory
        }
    }
}
