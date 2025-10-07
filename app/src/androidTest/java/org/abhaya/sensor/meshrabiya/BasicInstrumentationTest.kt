package org.abhaya.sensor.meshrabiya

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BasicInstrumentationTest {

    @Test
    fun testInstrumentationContext() {
        // Simple test to verify instrumentation framework is working
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = appContext.packageName
        
        // Accept both fullperm and nightly flavor package names
        val validPackageNames = listOf(
            "com.ustadmobile.meshrabiya.sensor.debug",        // fullperm flavor
            "com.ustadmobile.meshrabiya.sensor.nightly.debug" // nightly flavor
        )
        
        assertTrue(
            "Package name '$packageName' should be one of: $validPackageNames",
            validPackageNames.contains(packageName)
        )
    }

    @Test
    fun testBasicAssertion() {
        // Basic test that should always pass
        assertTrue("This should always be true", true)
        assertEquals("Expected value", "Expected value", "Expected value")
    }
}