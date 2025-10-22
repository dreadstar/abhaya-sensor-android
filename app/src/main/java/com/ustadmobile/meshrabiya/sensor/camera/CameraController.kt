package com.ustadmobile.meshrabiya.sensor.camera

import androidx.camera.view.PreviewView

interface CameraController {
    fun start(previewView: PreviewView, periodicSeconds: Int = 5)
    fun stop()
    // Add other methods as needed (e.g., switchCamera, getFlashMode, etc.)
    fun switchCamera()
    fun isFrontCamera(): Boolean
    fun cycleFlash()
    fun getFlashMode(): Int
    fun isTorchEnabled(): Boolean
    fun toggleTorch()
}
