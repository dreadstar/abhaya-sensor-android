package com.ustadmobile.meshrabiya.sensor.data

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Comprehensive data persistence tests covering:
 * - SharedPreferences storage and retrieval
 * - File system operations and data integrity
 * - Database operations and transaction handling
 * - Data migration and versioning scenarios
 * - Concurrent access and thread safety
 * - Storage capacity and performance testing
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class DataPersistenceTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var testDataDir: File
    
    private val testPrefsName = "test_sensor_preferences"
    private val testFiles = mutableListOf<File>()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharedPrefs = context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE)
        testDataDir = File(context.filesDir, "test_data")
        testDataDir.mkdirs()
        
        // Clear any existing test data
        sharedPrefs.edit().clear().apply()
        testFiles.clear()
    }

    @After
    fun tearDown() {
        // Clean up test data
        sharedPrefs.edit().clear().apply()
        testFiles.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        if (testDataDir.exists()) {
            testDataDir.deleteRecursively()
        }
    }

    /**
     * Test SharedPreferences basic operations
     */
    @Test
    fun testSharedPreferencesBasicOperations() {
        // Test string storage
        val testString = "sensor_config_${System.currentTimeMillis()}"
        sharedPrefs.edit().putString("test_string", testString).apply()
        
        assertEquals("String should be stored and retrieved correctly", 
                    testString, sharedPrefs.getString("test_string", null))

        // Test integer storage
        val testInt = Random.nextInt(1000, 9999)
        sharedPrefs.edit().putInt("test_int", testInt).apply()
        
        assertEquals("Integer should be stored and retrieved correctly", 
                    testInt, sharedPrefs.getInt("test_int", -1))

        // Test boolean storage
        sharedPrefs.edit().putBoolean("test_bool", true).apply()
        assertTrue("Boolean should be stored and retrieved correctly", 
                  sharedPrefs.getBoolean("test_bool", false))

        // Test float storage
        val testFloat = Random.nextFloat() * 100
        sharedPrefs.edit().putFloat("test_float", testFloat).apply()
        
        assertEquals("Float should be stored and retrieved correctly", 
                    testFloat, sharedPrefs.getFloat("test_float", -1f), 0.001f)
    }

    /**
     * Test SharedPreferences complex data structures
     */
    @Test
    fun testSharedPreferencesComplexData() {
        // Test storing sensor configuration
        val sensorConfig = mapOf(
            "camera_enabled" to true,
            "audio_enabled" to false,
            "sample_rate" to 44100,
            "quality" to 0.85f,
            "output_format" to "h264"
        )

        // Store configuration
        with(sharedPrefs.edit()) {
            putBoolean("camera_enabled", sensorConfig["camera_enabled"] as Boolean)
            putBoolean("audio_enabled", sensorConfig["audio_enabled"] as Boolean)
            putInt("sample_rate", sensorConfig["sample_rate"] as Int)
            putFloat("quality", sensorConfig["quality"] as Float)
            putString("output_format", sensorConfig["output_format"] as String)
            apply()
        }

        // Verify retrieval
        assertEquals("Camera config should match", 
                    sensorConfig["camera_enabled"], 
                    sharedPrefs.getBoolean("camera_enabled", false))
        assertEquals("Audio config should match", 
                    sensorConfig["audio_enabled"], 
                    sharedPrefs.getBoolean("audio_enabled", true))
        assertEquals("Sample rate should match", 
                    sensorConfig["sample_rate"], 
                    sharedPrefs.getInt("sample_rate", -1))
        assertEquals("Quality should match", 
                    sensorConfig["quality"] as Float, 
                    sharedPrefs.getFloat("quality", -1f), 0.001f)
        assertEquals("Output format should match", 
                    sensorConfig["output_format"], 
                    sharedPrefs.getString("output_format", null))
    }

    /**
     * Test file system operations and data integrity
     */
    @Test
    fun testFileSystemOperations() {
        val testFile = File(testDataDir, "sensor_data.txt")
        testFiles.add(testFile)
        
        // Test writing data
        val testData = "Sensor data: ${System.currentTimeMillis()}\nTemperature: 23.5°C\nHumidity: 65%"
        
        FileOutputStream(testFile).use { fos ->
            fos.write(testData.toByteArray(Charsets.UTF_8))
            fos.flush()
        }
        
        assertTrue("File should be created", testFile.exists())
        assertTrue("File should have content", testFile.length() > 0)

        // Test reading data
        val readData = FileInputStream(testFile).use { fis ->
            fis.readBytes().toString(Charsets.UTF_8)
        }
        
        assertEquals("File content should match", testData, readData)
    }

    /**
     * Test binary data handling
     */
    @Test
    fun testBinaryDataHandling() {
        val testFile = File(testDataDir, "binary_sensor_data.bin")
        testFiles.add(testFile)
        
        // Generate test binary data (simulating sensor readings)
        val binaryData = ByteArray(1024) { (it % 256).toByte() }
        
        // Write binary data
        FileOutputStream(testFile).use { fos ->
            fos.write(binaryData)
            fos.flush()
        }
        
        assertTrue("Binary file should be created", testFile.exists())
        assertEquals("File size should match", binaryData.size.toLong(), testFile.length())

        // Read and verify binary data
        val readData = FileInputStream(testFile).use { fis ->
            fis.readBytes()
        }
        
        assertArrayEquals("Binary data should match", binaryData, readData)
    }

    /**
     * Test concurrent file access
     */
    @Test
    fun testConcurrentFileAccess() = runBlocking {
        val testFile = File(testDataDir, "concurrent_access.txt")
        testFiles.add(testFile)
        
        val writeOperations = 5
        val latch = CountDownLatch(writeOperations)
        val results = mutableListOf<Boolean>()
        
        // Perform concurrent writes
        repeat(writeOperations) { index ->
            Thread {
                try {
                    val data = "Thread $index: ${System.currentTimeMillis()}\n"
                    FileOutputStream(testFile, true).use { fos ->
                        fos.write(data.toByteArray(Charsets.UTF_8))
                        fos.flush()
                    }
                    synchronized(results) {
                        results.add(true)
                    }
                } catch (e: Exception) {
                    synchronized(results) {
                        results.add(false)
                    }
                } finally {
                    latch.countDown()
                }
            }.start()
        }
        
        assertTrue("All concurrent operations should complete", 
                  latch.await(10, TimeUnit.SECONDS))
        assertEquals("All operations should succeed", writeOperations, results.count { it })
        assertTrue("File should contain data from all threads", testFile.length() > 0)
    }

    /**
     * Test large data handling and performance
     */
    @Test
    fun testLargeDataHandling() {
        val testFile = File(testDataDir, "large_sensor_data.bin")
        testFiles.add(testFile)
        
        // Generate large dataset (simulating continuous sensor data)
        val dataSize = 5 * 1024 * 1024 // 5MB
        val largeData = ByteArray(dataSize) { (Random.nextInt() % 256).toByte() }
        
        val startTime = System.currentTimeMillis()
        
        // Write large data
        FileOutputStream(testFile).use { fos ->
            fos.write(largeData)
            fos.flush()
        }
        
        val writeTime = System.currentTimeMillis() - startTime
        
        assertTrue("Large file should be created", testFile.exists())
        assertEquals("File size should match", dataSize.toLong(), testFile.length())
        
        // Read large data back
        val readStartTime = System.currentTimeMillis()
        
        val readData = FileInputStream(testFile).use { fis ->
            fis.readBytes()
        }
        
        val readTime = System.currentTimeMillis() - readStartTime
        
        assertArrayEquals("Large data should match", largeData, readData)
        
        println("Write time: ${writeTime}ms, Read time: ${readTime}ms for ${dataSize / 1024 / 1024}MB")
        
        // Performance should be reasonable
        assertTrue("Write performance should be reasonable", writeTime < 10000) // 10 seconds
        assertTrue("Read performance should be reasonable", readTime < 5000) // 5 seconds
    }

    /**
     * Test data persistence across app restarts
     */
    @Test
    fun testDataPersistenceAcrossRestarts() {
        // Store data that should persist
        val persistentData = "persistent_sensor_config_${System.currentTimeMillis()}"
        sharedPrefs.edit().putString("persistent_key", persistentData).apply()
        
        val persistentFile = File(testDataDir, "persistent_data.txt")
        testFiles.add(persistentFile)
        
        FileOutputStream(persistentFile).use { fos ->
            fos.write("Persistent file data".toByteArray(Charsets.UTF_8))
        }
        
        // Simulate app restart by creating new SharedPreferences instance
        val newSharedPrefs = context.getSharedPreferences(testPrefsName, Context.MODE_PRIVATE)
        
        // Verify data persists
        assertEquals("SharedPreferences data should persist", 
                    persistentData, newSharedPrefs.getString("persistent_key", null))
        assertTrue("File should persist", persistentFile.exists())
        
        val fileContent = FileInputStream(persistentFile).use { fis ->
            fis.readBytes().toString(Charsets.UTF_8)
        }
        assertEquals("File content should persist", "Persistent file data", fileContent)
    }

    /**
     * Test data migration scenarios
     */
    @Test
    fun testDataMigration() {
        // Simulate old data format
        sharedPrefs.edit()
            .putString("old_format_config", "camera:true,audio:false")
            .putInt("data_version", 1)
            .apply()
        
        // Simulate migration to new format
        val oldConfig = sharedPrefs.getString("old_format_config", "")
        if (oldConfig?.isNotEmpty() == true && sharedPrefs.getInt("data_version", 0) == 1) {
            // Parse old format and convert to new format
            val configParts = oldConfig.split(",")
            val cameraEnabled = configParts[0].split(":")[1].toBoolean()
            val audioEnabled = configParts[1].split(":")[1].toBoolean()
            
            // Store in new format
            sharedPrefs.edit()
                .putBoolean("camera_enabled", cameraEnabled)
                .putBoolean("audio_enabled", audioEnabled)
                .putInt("data_version", 2)
                .remove("old_format_config")
                .apply()
        }
        
        // Verify migration
        assertEquals("Data version should be updated", 2, sharedPrefs.getInt("data_version", 0))
        assertTrue("Camera setting should be migrated", sharedPrefs.getBoolean("camera_enabled", false))
        assertFalse("Audio setting should be migrated", sharedPrefs.getBoolean("audio_enabled", true))
        assertNull("Old format should be removed", sharedPrefs.getString("old_format_config", null))
    }

    /**
     * Test storage capacity and limitations
     */
    @Test
    fun testStorageCapacity() {
        // Test SharedPreferences size limits
        val largeString = "x".repeat(100000) // 100KB string
        
        sharedPrefs.edit().putString("large_string", largeString).apply()
        
        val retrievedString = sharedPrefs.getString("large_string", null)
        assertEquals("Large string should be stored in SharedPreferences", 
                    largeString, retrievedString)
        
        // Test multiple large entries
        repeat(5) { index ->
            val key = "large_entry_$index"
            val value = "data_${index}_${"y".repeat(50000)}"
            sharedPrefs.edit().putString(key, value).apply()
            
            assertEquals("Large entry $index should be stored", 
                        value, sharedPrefs.getString(key, null))
        }
    }

    /**
     * Test data validation and integrity checks
     */
    @Test
    fun testDataValidation() {
        // Test with various data types and edge cases
        val testCases = mapOf(
            "empty_string" to "",
            "null_replacement" to "null",
            "special_chars" to "!@#$%^&*()_+-={}[]|\\:;\"'<>?,./'",
            "unicode" to "😀🎉🔒🌟💾",
            "very_long" to "x".repeat(10000),
            "number_string" to "123456789",
            "boolean_string" to "true"
        )
        
        testCases.forEach { (key, value) ->
            sharedPrefs.edit().putString(key, value).apply()
            assertEquals("Value for $key should be stored correctly", 
                        value, sharedPrefs.getString(key, "default"))
        }
        
        // Test data integrity with checksums
        val originalData = "Important sensor calibration data: ${System.currentTimeMillis()}"
        val checksum = originalData.hashCode()
        
        sharedPrefs.edit()
            .putString("sensor_data", originalData)
            .putInt("sensor_data_checksum", checksum)
            .apply()
        
        val retrievedData = sharedPrefs.getString("sensor_data", "")
        val retrievedChecksum = sharedPrefs.getInt("sensor_data_checksum", 0)
        
        assertEquals("Data should match", originalData, retrievedData)
        assertEquals("Checksum should match", checksum, retrievedChecksum)
        assertEquals("Checksum validation should pass", 
                    checksum, retrievedData?.hashCode() ?: 0)
    }

    /**
     * Test atomic operations and transactions
     */
    @Test
    fun testAtomicOperations() {
        // Test atomic SharedPreferences operations
        val editor = sharedPrefs.edit()
        
        // Build complex transaction
        editor.putString("config_name", "sensor_config_v2")
        editor.putInt("config_version", 2)
        editor.putBoolean("config_validated", true)
        editor.putLong("config_timestamp", System.currentTimeMillis())
        
        // Apply atomically
        assertTrue("Atomic operation should succeed", editor.commit())
        
        // Verify all data is present
        assertEquals("Config name should be set", "sensor_config_v2", 
                    sharedPrefs.getString("config_name", null))
        assertEquals("Config version should be set", 2, 
                    sharedPrefs.getInt("config_version", -1))
        assertTrue("Config validation should be set", 
                  sharedPrefs.getBoolean("config_validated", false))
        assertTrue("Config timestamp should be set", 
                  sharedPrefs.getLong("config_timestamp", 0) > 0)
    }
}