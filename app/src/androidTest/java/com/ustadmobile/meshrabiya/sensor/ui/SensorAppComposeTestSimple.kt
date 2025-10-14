package com.ustadmobile.meshrabiya.sensor.ui

import androidx.activity.ComponentActivity
import android.Manifest
import androidx.test.rule.GrantPermissionRule
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Before
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import androidx.lifecycle.Lifecycle

/**
 * Simplified Compose UI tests
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class SensorAppComposeTestSimple {


    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @get:Rule
    val composeTestRule = createAndroidComposeRule<com.ustadmobile.meshrabiya.sensor.MainActivity>()

    @Before
    fun setUp() {
        composeTestRule.activityRule.scenario.onActivity { /* ensure activity launched */ }
        composeTestRule.waitForIdle()

        // Enable test-mode fake sensor injector by broadcasting
        try {
            val ctx = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            val intent = android.content.Intent("com.ustadmobile.meshrabiya.sensor.TEST_INJECTOR")
            intent.putExtra("fake_sensors", true)
            ctx.sendBroadcast(intent)
        } catch (_: Throwable) {}

        // Wait for stable status using compose rule waitUntil
        composeTestRule.waitUntil(30_000) {
            try {
                composeTestRule.onNodeWithTag("app_status").assertExists()
                true
            } catch (_: Throwable) { false }
        }
    }

    /**
     * Test basic Compose setup
     */
    @Test
    fun testComposeRuleSetup() {
        // Allow activity/compose to settle and check for a header or status text
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_status").assertIsDisplayed()
    }

    /**
     * Test sensor list UI concepts
     */
    @Test
    fun testSensorListConcepts() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("select_sensors_card").assertIsDisplayed()
    // (setup handles activity launch and waits)
    }

    /**
     * Test monitoring controls UI
     */
    @Test
    fun testMonitoringControls() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("control_start_stop").assertIsDisplayed()
    }

    /**
     * Test data display components
     */
    @Test
    fun testDataDisplayComponents() {
        val sensorData = "Temperature: 23.5°C"
        // Sensor readings are dynamic; check for the word "Temperature" instead
        composeTestRule.waitForIdle()
        // Prefer test tag for temperature component if present, otherwise substring check
        // Retry presence of temperature component for a short period before failing
        var found = false
        val deadline = System.currentTimeMillis() + 15_000
        while (System.currentTimeMillis() < deadline) {
            try {
                composeTestRule.onNodeWithTag("sensor_temperature").assertIsDisplayed()
                found = true
                break
            } catch (_: Throwable) {
                // try substring fallback
                try {
                    composeTestRule.onNodeWithText("Temperature", substring = true).assertIsDisplayed()
                    found = true
                    break
                } catch (_: Throwable) {
                    // not found yet
                }
            }
            Thread.sleep(250)
        }
        if (!found) {
            throw AssertionError("Temperature component not visible after retries")
        }
    }

    /**
     * Test app theme and styling
     */
    @Test
    fun testAppTheme() {
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("app_status").assertIsDisplayed()
    }
}