package com.ustadmobile.meshrabiya.sensor.capture

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.ustadmobile.meshrabiya.sensor.stream.StreamIngestor
import kotlin.math.abs
import kotlin.math.log10

class AudioCapture(private val ingestor: StreamIngestor) {
    private var recorder: AudioRecord? = null
    private var running = false
    
    private val _audioLevel = MutableStateFlow(0f) // 0-100 scale
    val audioLevel: StateFlow<Float> = _audioLevel

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        try {
            recorder?.startRecording()
            running = true
            CoroutineScope(Dispatchers.IO).launch {
                val buf = ShortArray(bufferSize / 2)
                while (running && recorder != null) {
                    val read = recorder!!.read(buf, 0, buf.size)
                    if (read > 0) {
                        // Calculate audio amplitude for visualization
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += abs(buf[i].toDouble())
                        }
                        val amplitude = sum / read
                        // Convert to dB scale (0-100)
                        val db = if (amplitude > 1) {
                            20 * log10(amplitude / 32768.0) + 90 // Normalize to 0-90 range
                        } else {
                            0.0
                        }
                        _audioLevel.value = db.toFloat().coerceIn(0f, 100f)
                        
                        // convert to byte array (PCM 16 LE)
                        val bytes = ShortArrayToByteArray(buf, read)
                        val ts = System.currentTimeMillis()
                        ingestor.ingestSensorReading("audio_stream", ts, bytes)
                    }
                }
            }
        } catch (_: Exception) {
            stop()
        }
    }

    fun stop() {
        running = false
        _audioLevel.value = 0f
        try {
            recorder?.stop()
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
    }

    private fun ShortArrayToByteArray(shorts: ShortArray, len: Int): ByteArray {
        val bytes = ByteArray(len * 2)
        var i = 0
        for (s in 0 until len) {
            val v = shorts[s]
            bytes[i++] = (v.toInt() and 0x00FF).toByte()
            bytes[i++] = ((v.toInt() shr 8) and 0x00FF).toByte()
        }
        return bytes
    }
}
