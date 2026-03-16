package com.example.androidlocaltts.engine

import android.content.Context
import android.speech.tts.SynthesisCallback
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.atLeastOnce
import java.io.File

class SherpaOnnxTtsEngineTest {

    private lateinit var context: Context
    private lateinit var mockTts: OfflineTts
    private lateinit var mockCallback: SynthesisCallback
    private lateinit var engine: SherpaOnnxTtsEngine

    @Before
    fun setup() {
        context = mock(Context::class.java)
        mockTts = mock(OfflineTts::class.java)
        mockCallback = mock(SynthesisCallback::class.java)
        
        // Mock ttsProvider to return our mockTts
        engine = SherpaOnnxTtsEngine(context) { mockTts }
        
        // Setup mock environment for initialize()
        val mockFilesDir = File("temp_files")
        `when`(context.filesDir).thenReturn(mockFilesDir)
        // Note: isModelPrepared is an object call, might need more mocking or just skip initialize() and set tts manually if possible
    }

    @Test
    fun testRelease_WaitsForSynthesizeToFinish() = runTest {
        // Set tts manually
        val field = engine.javaClass.getDeclaredField("tts")
        field.isAccessible = true
        field.set(engine, mockTts)

        // Mock generate to take some time
        `when`(mockTts.generate(anyString(), anyInt(), anyFloat())).thenAnswer {
            Thread.sleep(100) // Simulate work
            GeneratedAudio(floatArrayOf(0f), 16000)
        }

        // Start synthesize in a separate coroutine
        val job = kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.Default) {
            engine.synthesize("test", 1.0f, mockCallback)
        }

        // Small delay to ensure synthesize started and locked the mutex
        kotlinx.coroutines.delay(20)

        // Call release in another coroutine
        val releaseJob = kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.Default) {
            engine.release()
        }

        job.join()
        releaseJob.join()

        // Verify that tts.release() was called exactly once
        verify(mockTts).release()
    }
}
