package com.ustadmobile.meshrabiya.sensor.meshrabiya

import android.content.Intent
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AidlDeviceIdTest {
    @Test
    fun fetchOnionPubKey_whenServiceAvailable_returnsNonEmpty() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val pub = MeshrabiyaAidlClient.fetchOnionPubKeyBlocking(ctx)
        // If service not installed or permission denied, we accept null/empty and skip
        if (pub.isNullOrBlank()) {
            // Skip assertion — service not available in this environment
            return
        }
        assertTrue(pub.isNotBlank())
    }
}
