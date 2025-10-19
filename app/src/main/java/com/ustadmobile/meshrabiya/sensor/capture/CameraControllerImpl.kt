
package com.ustadmobile.meshrabiya.sensor.capture

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner

/**
 * Wrapper for official CameraX LifecycleCameraController, centralizing camera logic and enhancements.
 */
class CameraControllerImpl(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView
) {
    private val cameraController = LifecycleCameraController(context)

    init {
        cameraController.bindToLifecycle(lifecycleOwner)
        previewView.controller = cameraController
    }

    fun switchCamera() {
        val currentSelector = cameraController.cameraSelector
        cameraController.cameraSelector = if (currentSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }

    fun isFrontCamera(): Boolean =
        cameraController.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

    fun cycleFlash() {
        val current = cameraController.imageCaptureFlashMode
        val next = when (current) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_OFF
        }
        cameraController.imageCaptureFlashMode = next
    }

    fun getFlashMode(): Int = cameraController.imageCaptureFlashMode

    fun setZoom(ratio: Float) {
        cameraController.setZoomRatio(ratio)
    }

    fun getController(): LifecycleCameraController = cameraController
}
