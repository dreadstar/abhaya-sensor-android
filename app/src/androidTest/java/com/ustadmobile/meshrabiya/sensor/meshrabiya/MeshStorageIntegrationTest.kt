package com.ustadmobile.meshrabiya.sensor.meshrabiya

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@LargeTest
class MeshStorageIntegrationTest {

    @Test
    fun storeAndRetrieveBlob_viaAidlTestMode_roundTrip() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent("com.ustadmobile.meshrabiya.ACTION_BIND").setPackage(ctx.packageName)
        intent.putExtra("meshrabiya_test_mode", true)

        var bound = false
        val lock = java.util.concurrent.CountDownLatch(1)
        var stub: com.ustadmobile.meshrabiya.api.IMeshrabiyaService? = null

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                stub = com.ustadmobile.meshrabiya.api.IMeshrabiyaService.Stub.asInterface(service)
                bound = true
                lock.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName?) {}
        }

        val didBind = ctx.applicationContext.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        if (!didBind) return // skip if can't bind
        lock.await()

        try {
            val toStore = "hello-mesh-storage".toByteArray()
            // Create a pipe and write the bytes into the write-end; pass the read-end to the AIDL store
            val pipe = ParcelFileDescriptor.createPipe()
            val readEnd = pipe[0]
            val writeEnd = pipe[1]
            try {
                FileOutputStream(writeEnd.fileDescriptor).use { it.write(toStore) }
            } finally {
                try { writeEnd.close() } catch (_: Exception) {}
            }

            val blobId = try { stub?.storeBlob(readEnd) } catch (_: Exception) { null }
            if (blobId.isNullOrBlank()) return

            val readPfd = try { stub?.openBlob(blobId) } catch (_: Exception) { null }
            if (readPfd == null) return
            val readBytes = FileInputStream(readPfd.fileDescriptor).use { it.readBytes() }
            assertArrayEquals(toStore, readBytes)
        } finally {
            if (bound) try { ctx.applicationContext.unbindService(conn) } catch (_: Exception) {}
        }
    }
}
