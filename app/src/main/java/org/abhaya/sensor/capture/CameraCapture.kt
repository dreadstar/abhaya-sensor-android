package com.ustadmobile.meshrabiya.sensor.capture

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * Minimal Camera capture that grabs ImageProxy frames and forwards NV21 byte buffers
 * as a simple segment to the StreamIngestor. This is a lightweight example and not
 * production-ready (no encoding, no chunking, no format negotiation).
 */
class CameraCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val ingestor: StreamIngestor
) {
    private val cameraThread = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    // When true the analyzer will forward the next available frame to the ingestor
    @Volatile
    private var captureNextFrame = false

    /**
     * Start camera capturing. If `previewView` is provided the Preview use case will render to it.
     * This method returns quickly and performs camera init on a background thread.
     */
    fun start(previewView: PreviewView? = null, periodicSeconds: Int = 5) {
        // initialize on camera thread to avoid blocking UI
        cameraThread.execute {
            try {
                val provider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider = provider

                val preview = Preview.Builder().build()
                previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }

        imageCapture = ImageCapture.Builder()
            .setTargetResolution(Size(1280, 720))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

                imageAnalysis?.setAnalyzer(cameraThread) { imageProxy ->
            try {
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val ts = System.currentTimeMillis()
                // Always send analysis frames when periodic operation is active
                if (captureNextFrame) {
                    captureNextFrame = false
                    ingestor.ingestSensorReading("camera_stream", ts, bytes)
                } else {
                    // Optionally we could sample; for now do not send continuous frames
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                imageProxy.close()
            }
        }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis)
            } catch (e: Exception) {
                // ignore startup errors for the sample app
            }
        }
    }

    fun stop() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
    }

    /** Request the next analyzer frame to be forwarded to the ingestor. */
    fun captureOnce() {
        captureNextFrame = true
    }
}
