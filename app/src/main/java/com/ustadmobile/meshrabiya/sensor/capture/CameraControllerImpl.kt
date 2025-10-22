package com.ustadmobile.meshrabiya.sensor.capture

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.core.content.ContextCompat
import com.ustadmobile.meshrabiya.sensor.util.UIIngestor

/**
 * Wrapper for official CameraX LifecycleCameraController, centralizing camera logic and enhancements.
 */
import com.ustadmobile.meshrabiya.sensor.camera.CameraController

class CameraControllerImpl(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val ingestor: UIIngestor
) : CameraController {
    private val cameraController = LifecycleCameraController(context)
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var torchEnabled: Boolean = false
    private val cameraCapture = CameraCapture(context, lifecycleOwner, ingestor)


    // override fun start(previewView: PreviewView, periodicSeconds: Int) {
    //     cameraController.bindToLifecycle(lifecycleOwner)
    //     previewView.controller = cameraController
    //     // TODO: Use periodicSeconds if needed
    // }
    override fun start(previewView: PreviewView, periodicSeconds: Int) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                previewView.context as androidx.lifecycle.LifecycleOwner,
                cameraSelector,
                preview
            )
        }, ContextCompat.getMainExecutor(context))
    }

    // override fun stop() {
    //     // CameraX does not have a direct stop, but you can unbind or clear controller if needed
    //     cameraController.unbind()
    // }
    override fun stop() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
        }, ContextCompat.getMainExecutor(context))
    }

    override fun switchCamera() {
        val newSelector = if (cameraController.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        cameraController.cameraSelector = newSelector
        cameraSelector = newSelector // keep local property in sync
    }
    

    override fun isFrontCamera(): Boolean {
        return cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
    }

    fun cycleImageFlashMode() {
        cameraCapture.cycleImageFlashMode()
    }

    override fun toggleTorch() {
        torchEnabled = !isTorchEnabled()
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    fun getImageCaptureFlashMode(): Int = cameraCapture.getImageCaptureFlashMode()

    override fun isTorchEnabled(): Boolean {
        return camera?.cameraInfo?.torchState?.value == androidx.camera.core.TorchState.ON
            ?: false
    }

    override fun cycleFlash() {
        cameraCapture.cycleImageFlashMode()
    }

    override fun getFlashMode(): Int {
        return cameraCapture.getImageCaptureFlashMode()
    }


    fun setZoom(ratio: Float) {
        cameraController.setZoomRatio(ratio)
    }

    fun getController(): LifecycleCameraController = cameraController
}
