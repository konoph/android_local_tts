package com.example.androidlocaltts.engine

import android.content.Context
import android.content.res.AssetManager
import android.speech.tts.SynthesisCallback
import android.util.Log
import com.example.androidlocaltts.utils.ModelManager
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

interface OfflineTtsLike {
    fun generate(text: String, sid: Int, speed: Float): com.k2fsa.sherpa.onnx.GeneratedAudio
    fun release()
}

private class OfflineTtsAdapter(private val delegate: OfflineTts) : OfflineTtsLike {
    override fun generate(text: String, sid: Int, speed: Float): com.k2fsa.sherpa.onnx.GeneratedAudio {
        return delegate.generate(text, sid, speed)
    }

    override fun release() {
        delegate.release()
    }
}


class SherpaOnnxTtsEngine(
    private val context: Context,
    private val ttsProvider: (AssetManager, OfflineTtsConfig) -> OfflineTtsLike = { assets, config -> OfflineTtsAdapter(OfflineTts(assets, config)) }
) {
    companion object {
        private const val TAG = "SherpaOnnxTtsEngine"
    }
    private var tts: OfflineTtsLike? = null
    private val mutex = Mutex()

    fun isReady(): Boolean = tts != null

    fun initialize(): Boolean {
        if (tts != null) return true

        val modelDir = ModelManager.getModelDir(context.filesDir)
        if (!ModelManager.isModelPrepared(context.filesDir)) {
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
            tts = ttsProvider(context.assets, config)
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
            var isStarted = false
            try {
                if (chunks.isEmpty()) {
                    // Even if empty, we must signal completion
                    callback.start(16000, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
                    isStarted = true
                    callback.done()
                    return
                }

                for (chunk in chunks) {
                    coroutineContext.ensureActive()
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
                        if (i % 1024 == 0) {
                            coroutineContext.ensureActive()
                        }
                        pcmData[i] = (samples[i] * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    val byteBuffer = ByteBuffer.allocate(pcmData.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                    byteBuffer.asShortBuffer().put(pcmData)
                    
                    callback.audioAvailable(byteBuffer.array(), 0, byteBuffer.capacity())
                }
                callback.done()
            } catch (e: CancellationException) {
                Log.i(TAG, "Synthesis cancelled", e)
                if (!isStarted) {
                    callback.start(16000, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
                }
                callback.error()
            } catch (e: Exception) {
                Log.e(TAG, "Error during synthesis", e)
                if (!isStarted) {
                    callback.start(16000, android.media.AudioFormat.ENCODING_PCM_16BIT, 1)
                }
                callback.error()
            }
        }
    }

    fun release() {
        // Use blocking lock if called from onDestroy
        kotlinx.coroutines.runBlocking {
            mutex.withLock {
                tts?.release()
                tts = null
            }
        }
    }
}
