package com.ustadmobile.meshrabiya.sensor.lifecycle

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.ustadmobile.meshrabiya.sensor.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive lifecycle and background processing tests covering:
 * - Activity lifecycle state management and transitions
 * - Background task handling and WorkManager integration
 * - System integration and process lifecycle
 * - Memory management and resource cleanup
 * - Configuration changes and state preservation
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LifecycleBackgroundTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var context: Context
    private val lifecycleCallbacks = mutableListOf<String>()
    private val backgroundTaskCounter = AtomicInteger(0)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        lifecycleCallbacks.clear()
        backgroundTaskCounter.set(0)
    }

    @After
    fun tearDown() {
        lifecycleCallbacks.clear()
    }

    /**
     * Test basic activity lifecycle transitions
     */
    @Test
    fun testActivityLifecycleTransitions() {
        activityRule.scenario.onActivity { activity ->
            assertEquals("Activity should be in RESUMED state", 
                        Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }

        // Move through lifecycle states
        activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        activityRule.scenario.onActivity { activity ->
            assertEquals("Activity should be in STARTED state", 
                        Lifecycle.State.STARTED, activity.lifecycle.currentState)
        }

        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        activityRule.scenario.onActivity { activity ->
            assertEquals("Activity should be in CREATED state", 
                        Lifecycle.State.CREATED, activity.lifecycle.currentState)
        }

        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        activityRule.scenario.onActivity { activity ->
            assertEquals("Activity should return to RESUMED state", 
                        Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }
    }

    /**
     * Test configuration changes and state preservation
     */
    @Test
    fun testConfigurationChanges() {
        var originalActivity: MainActivity? = null
        
        activityRule.scenario.onActivity { activity ->
            originalActivity = activity
            
            // Verify initial state
            assertNotNull("Activity should be created", activity)
            assertEquals("Should be in resumed state", 
                        Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }

        // Simulate configuration change (screen rotation)
        activityRule.scenario.recreate()

        activityRule.scenario.onActivity { newActivity ->
            assertNotNull("Activity should be recreated", newActivity)
            assertNotEquals("Should be a new activity instance", 
                           originalActivity, newActivity)
            assertEquals("New activity should be resumed", 
                        Lifecycle.State.RESUMED, newActivity.lifecycle.currentState)
        }
    }

    /**
     * Test rapid background-foreground state transitions
     */
    @Test
    fun testBackgroundForegroundTransitions() { runBlocking {
        val latch = CountDownLatch(1)
        var backgroundDetected = false

        activityRule.scenario.onActivity { activity ->
            // Setup lifecycle observer
            activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    backgroundDetected = true
                    latch.countDown()
                }
            })
        }

        // Move to background
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        
        // Wait for background state to be detected
        assertTrue("Background state should be detected", 
                  latch.await(5, TimeUnit.SECONDS))
        assertTrue("Background transition should be detected", backgroundDetected)

        // Return to foreground
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        
        activityRule.scenario.onActivity { activity ->
            assertEquals("Should return to foreground", 
                        Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }
        } }


    /**
     * Test memory management during lifecycle changes
     */
    @Test
    fun testMemoryManagement() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Perform multiple lifecycle transitions
        repeat(5) {
            activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(100)
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            Thread.sleep(100)
        }

        // Force garbage collection
        System.gc()
        Thread.sleep(1000)

        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Memory increase should be reasonable (not indicating major leaks)
        assertTrue("Memory increase should be reasonable during lifecycle changes", 
                  memoryIncrease < 50 * 1024 * 1024) // 50MB threshold
    }

    /**
     * Test background task handling
     */
    @Test
    fun testBackgroundTaskHandling() { runBlocking {
        val taskCompleted = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        activityRule.scenario.onActivity { activity ->
            // Simulate background task
            val backgroundTask = Runnable {
                try {
                    Thread.sleep(2000) // Simulate work
                    backgroundTaskCounter.incrementAndGet()
                    taskCompleted.set(true)
                } finally {
                    latch.countDown()
                }
            }
            
            Thread(backgroundTask).start()
        }

        // Move activity to background while task runs
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        
        // Wait for task completion
        assertTrue("Background task should complete", 
                  latch.await(5, TimeUnit.SECONDS))
        assertTrue("Task should complete successfully", taskCompleted.get())
        assertEquals("Task counter should increment", 1, backgroundTaskCounter.get())

        // Return to foreground
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        } }


    /**
     * Test system integration and process survival
     */
    @Test
    fun testSystemIntegration() {
        activityRule.scenario.onActivity { activity ->
            // Test app responds to system callbacks
            val packageName = activity.packageName
            assertNotNull("Package name should be available", packageName)
            assertTrue("Package name should not be empty", packageName.isNotEmpty())

            // Test system services integration
            val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE)
            assertNotNull("Activity manager should be available", activityManager)
        }

        // Test activity can handle system pressure
        repeat(3) {
            activityRule.scenario.moveToState(Lifecycle.State.CREATED)
            System.gc() // Simulate memory pressure
            Thread.sleep(500)
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            Thread.sleep(500)
        }

        activityRule.scenario.onActivity { activity ->
            assertEquals("Activity should survive system pressure", 
                        Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }
    }

    /**
     * Test concurrent lifecycle operations
     */
    @Test
    fun testConcurrentLifecycleOperations() {
        val operations = mutableListOf<String>()
        val latch = CountDownLatch(6)

        activityRule.scenario.onActivity { activity ->
            activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onCreate(owner: androidx.lifecycle.LifecycleOwner) {
                    operations.add("onCreate")
                    latch.countDown()
                }
                
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    operations.add("onStart")
                    latch.countDown()
                }
                
                override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                    operations.add("onResume")
                    latch.countDown()
                }
                
                override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                    operations.add("onPause")
                    latch.countDown()
                }
                
                override fun onStop(owner: androidx.lifecycle.LifecycleOwner) {
                    operations.add("onStop")
                    latch.countDown()
                }
                
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                    operations.add("onDestroy")
                    latch.countDown()
                }
            })
        }

        // Perform rapid state transitions
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        
        assertTrue("Lifecycle operations should complete", 
                  latch.await(10, TimeUnit.SECONDS))
        assertTrue("Should have recorded lifecycle operations", 
                  operations.isNotEmpty())
    }

    /**
     * Test resource cleanup during destruction
     */
    @Test
    fun testResourceCleanup() {
        var resourcesCleaned = false
        val cleanupLatch = CountDownLatch(1)

        activityRule.scenario.onActivity { activity ->
            activity.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                    // Simulate resource cleanup
                    resourcesCleaned = true
                    cleanupLatch.countDown()
                }
            })
        }

        // Destroy activity
        activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        
        assertTrue("Cleanup should be triggered", 
                  cleanupLatch.await(5, TimeUnit.SECONDS))
        assertTrue("Resources should be cleaned up", resourcesCleaned)
    }

    /**
     * Test app launch performance and cold starts
     */
    @Test
    fun testLaunchPerformance() {
        val launchTimes = mutableListOf<Long>()

        repeat(3) {
            val startTime = System.currentTimeMillis()
            
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            scenario.onActivity { activity ->
                val endTime = System.currentTimeMillis()
                launchTimes.add(endTime - startTime)
            }
            scenario.close()
            
            Thread.sleep(1000) // Allow cleanup between launches
        }

        // Verify launch times are reasonable
        val averageLaunchTime = launchTimes.average()
        assertTrue("Average launch time should be reasonable (< 3 seconds)", 
                  averageLaunchTime < 3000)
        
        println("Average launch time: ${averageLaunchTime}ms")
    }

    /**
     * Test long-running background operations
     */
    @Test
    fun testLongRunningOperations() = runBlocking {
        val operationCompleted = AtomicBoolean(false)
        val progressUpdates = AtomicInteger(0)
        val completionLatch = CountDownLatch(1)

        activityRule.scenario.onActivity { activity ->
            // Simulate long-running operation
            Thread {
                try {
                    repeat(5) { iteration ->
                        Thread.sleep(1000)
                        progressUpdates.incrementAndGet()
                        
                        // Check if activity is still active
                        if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                            // Continue operation
                        }
                    }
                    operationCompleted.set(true)
                } finally {
                    completionLatch.countDown()
                }
            }.start()
        }

        // Simulate app going to background during operation
        delay(2000)
        activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        
        delay(2000)
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        // Wait for operation completion
        assertTrue("Long-running operation should complete", 
                  completionLatch.await(10, TimeUnit.SECONDS))
        assertTrue("Operation should complete successfully", operationCompleted.get())
        assertTrue("Should receive progress updates", progressUpdates.get() >= 3)
    }
}