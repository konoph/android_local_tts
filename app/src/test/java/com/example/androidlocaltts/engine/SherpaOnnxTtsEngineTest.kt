package com.example.androidlocaltts.engine

import android.content.Context
import android.speech.tts.SynthesisCallback
import com.k2fsa.sherpa.onnx.GeneratedAudio
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.kotlin.atLeastOnce
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class SherpaOnnxTtsEngineTest {

    private lateinit var context: Context
    private lateinit var mockTts: OfflineTtsLike
    private lateinit var mockCallback: SynthesisCallback
    private lateinit var engine: SherpaOnnxTtsEngine

    @Before
    fun setup() {
        context = mock(Context::class.java)
        mockTts = mock(OfflineTtsLike::class.java)
        mockCallback = mock(SynthesisCallback::class.java)
        
        // Mock ttsProvider to return our mockTts
        engine = SherpaOnnxTtsEngine(context) { _, _ -> mockTts }
        
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
        val job = launch(kotlinx.coroutines.Dispatchers.Default) {
            engine.synthesize("test", 1.0f, mockCallback)
        }

        // Small delay to ensure synthesize started and locked the mutex
        delay(20)

        // Call release in another coroutine
        val releaseJob = launch(kotlinx.coroutines.Dispatchers.Default) {
            engine.release()
        }

        job.join()
        releaseJob.join()

        // Verify that tts.release() was called exactly once
        verify(mockTts).release()
    }

    @Test
    fun testSynthesize_CancellationStopsFurtherChunksAndCallsError() = runTest {
        val field = engine.javaClass.getDeclaredField("tts")
        field.isAccessible = true
        field.set(engine, mockTts)

        val generateCount = AtomicInteger(0)
        val jobRef = AtomicReference<kotlinx.coroutines.Job>()
        val audio = GeneratedAudio(floatArrayOf(0f), 16000)
        `when`(mockTts.generate(anyString(), anyInt(), anyFloat())).thenAnswer {
            if (generateCount.incrementAndGet() == 1) {
                jobRef.get()?.cancel()
            }
            audio
        }

        val job = launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            engine.synthesize("a。b。c。", 1.0f, mockCallback)
        }
        jobRef.set(job)
        job.start()
        job.join()

        verify(mockCallback, atLeastOnce()).start(anyInt(), anyInt(), anyInt())
        verify(mockCallback).error()
        verify(mockCallback, never()).done()
        verify(mockTts, times(1)).generate(anyString(), anyInt(), anyFloat())
    }

    @Test
    fun testSynthesize_GenerateThrows_CallsStartThenError() = runTest {
        val field = engine.javaClass.getDeclaredField("tts")
        field.isAccessible = true
        field.set(engine, mockTts)

        `when`(mockTts.generate(anyString(), anyInt(), anyFloat())).thenThrow(RuntimeException("boom"))

        engine.synthesize("test。", 1.0f, mockCallback)

        verify(mockCallback).start(anyInt(), anyInt(), anyInt())
        verify(mockCallback).error()
        verify(mockCallback, never()).done()
    }
}
