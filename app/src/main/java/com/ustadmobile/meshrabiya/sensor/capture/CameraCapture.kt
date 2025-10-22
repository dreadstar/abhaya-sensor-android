package com.ustadmobile.meshrabiya.sensor.capture

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor
import java.util.concurrent.Executors
import androidx.camera.core.ImageCapture
import com.ustadmobile.meshrabiya.sensor.util.UIIngestor

/**
 * Camera capture that grabs ImageProxy frames and forwards byte buffers
 * to the StreamIngestor, with live preview support.
 */
class CameraCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val ingestor: UIIngestor
) {
    companion object {
        private const val TAG = "CameraCapture"
    }
    
    private val cameraThread = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    private var currentPreviewView: PreviewView? = null
    private var currentLensFacing = CameraSelector.LENS_FACING_BACK
    
    // Flash mode enum with three states
    private var imageCaptureFlashMode: Int = ImageCapture.FLASH_MODE_OFF
    private var torchEnabled: Boolean = false
    
    // When true the analyzer will forward the next available frame to the ingestor
    @Volatile
    private var captureNextFrame = false

    /**
     * Start camera capturing. If `previewView` is provided the Preview use case will render to it.
     * Must be called from main thread or will post to main thread.
     */
    fun start(previewView: PreviewView? = null, periodicSeconds: Int = 5) {
        Log.d(TAG, "start() called with previewView=$previewView, periodicSeconds=$periodicSeconds")
        currentPreviewView = previewView
        
        // Ensure camera initialization happens on main thread for PreviewView
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initializeCamera()
        } else {
            mainHandler.post { initializeCamera() }
        }
    }
    
    private fun initializeCamera() {
        Log.d(TAG, "initializeCamera() starting on thread: ${Thread.currentThread().name}")
        try {
            val provider = ProcessCameraProvider.getInstance(context).get()
            cameraProvider = provider
            Log.d(TAG, "ProcessCameraProvider obtained")

            // Build preview use case
            val preview = Preview.Builder().build()
            
            // Connect preview to PreviewView on main thread
            currentPreviewView?.let { pv ->
                Log.d(TAG, "Setting surface provider for PreviewView")
                preview.setSurfaceProvider(pv.surfaceProvider)
            }

            // Build image capture use case with current flash mode
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1280, 720))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(imageCaptureFlashMode)
                .build()
            Log.d(TAG, "ImageCapture configured with flash mode: $imageCaptureFlashMode")

            // Build image analysis use case
            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis?.setAnalyzer(cameraThread) { imageProxy ->
                try {
                    val buffer = imageProxy.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val ts = System.currentTimeMillis()
                    // Send frames when requested
                    if (captureNextFrame) {
                        captureNextFrame = false
                        ingestor.ingestSensorReading("camera_stream", ts, bytes)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in image analyzer", e)
                } finally {
                    imageProxy.close()
                }
            }

            // Select camera lens
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(currentLensFacing)
                .build()
            
            Log.d(TAG, "Camera selector configured for lens facing: $currentLensFacing")

            // Unbind all previous use cases and bind new ones
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
            
            Log.d(TAG, "Camera bound to lifecycle successfully")
            
           
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera", e)
        }
    }
    

    fun stop() {
        Log.d(TAG, "stop() called")
        try {
            camera?.cameraControl?.enableTorch(false)
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
        }
        currentPreviewView = null
    }

    /** Request the next analyzer frame to be forwarded to the ingestor. */
    fun captureOnce() {
        captureNextFrame = true
    }
    
    /** Switch between front and back camera */
    fun switchCamera() {
        Log.d(TAG, "switchCamera() called, current lens: $currentLensFacing")
        currentLensFacing = if (currentLensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        Log.d(TAG, "Switching to lens: $currentLensFacing")
        
        // Restart with current preview view
        currentPreviewView?.let { pv ->
            stop()
            start(pv)
        }
    }
    
    /** Cycle through flash modes: OFF -> ON -> AUTO -> OFF */
    fun cycleImageFlashMode() {
        imageCaptureFlashMode = when (imageCaptureFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = imageCaptureFlashMode
        Log.d(TAG, "ImageCapture flash mode changed to: $imageCaptureFlashMode")
    }

    fun toggleTorch() {
        torchEnabled = !torchEnabled
        camera?.cameraControl?.enableTorch(torchEnabled)
        Log.d(TAG, "Torch enabled: $torchEnabled")
    }
    
    fun getImageCaptureFlashMode(): Int = imageCaptureFlashMode
    fun isTorchEnabled(): Boolean = torchEnabled


    
    /** Check if currently using front camera */
    fun isFrontCamera(): Boolean = currentLensFacing == CameraSelector.LENS_FACING_FRONT
}
