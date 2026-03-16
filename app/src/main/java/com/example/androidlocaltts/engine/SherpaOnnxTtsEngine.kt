package com.example.androidlocaltts.engine

import android.content.Context
import android.speech.tts.SynthesisCallback
import android.util.Log
import com.example.androidlocaltts.utils.ModelManager
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SherpaOnnxTtsEngine(private val context: Context) {
    private const val TAG = "SherpaOnnxTtsEngine"
    private var tts: OfflineTts? = null
    private val mutex = Mutex()

    fun isReady(): Boolean = tts != null

    fun initialize(): Boolean {
        if (tts != null) return true

        val modelDir = ModelManager.getModelDir(context)
        if (!ModelManager.isModelPrepared(context)) {
            Log.e(TAG, "Model files not prepared in ${modelDir.absolutePath}")
            return false
        }

        return try {
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    kokoro = OfflineTtsKokoroModelConfig(
                        model = File(modelDir, "model.onnx").absolutePath,
                        voices = File(modelDir, "voices.bin").absolutePath,
                        tokens = File(modelDir, "tokens.txt").absolutePath,
                        dataDir = File(modelDir, "espeak-ng-data").absolutePath
                    ),
                    numThreads = 2,
                    debug = true
                )
            )
            tts = OfflineTts(config)
            Log.i(TAG, "Sherpa-ONNX OfflineTts initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Sherpa-ONNX", e)
            false
        }
    }

    suspend fun synthesize(text: String, speed: Float, callback: SynthesisCallback) {
        val engine = tts ?: run {
            Log.e(TAG, "TTS Engine not initialized")
            callback.error()
            return
        }

        // Split text into chunks by punctuation to reduce latency
        val chunks = text.split(Regex("(?<=[。！？、.,!?])|(?=\\n)")).filter { it.isNotBlank() }
        
        mutex.withLock {
            try {
                // Initial setup for the whole synthesis request
                // We'll use the sample rate from the first generated chunk
                var isStarted = false

                for (chunk in chunks) {
                    if (callback.hasStarted() && !isStarted) {
                        // Already started by system? This shouldn't happen usually here
                    }

                    val audio = engine.generate(text = chunk, speed = speed, sid = 0)
                    val samples = audio.samples
                    val sampleRate = audio.sampleRate

                    if (!isStarted) {
                        callback.start(sampleRate, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
                        isStarted = true
                    }

                    // Convert float samples to PCM 16-bit
                    val pcmData = ShortArray(samples.size)
                    for (i in samples.indices) {
                        pcmData[i] = (samples[i] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                    byteBuffer.asShortBuffer().put(pcmData)
                    
                    callback.audioAvailable(byteBuffer.array(), 0, byteBuffer.capacity())
                    
                    Log.d(TAG, "Synthesized chunk: ${chunk.take(10)}... Size: ${samples.size}")
                }
                callback.done()
            } catch (e: Exception) {
                Log.e(TAG, "Error during synthesis", e)
                callback.error()
            }
        }
    }

    fun release() {
        tts?.release()
        tts = null
    }
}
