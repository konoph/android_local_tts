package com.example.androidlocaltts.service

import android.content.Context
import android.speech.tts.SynthesisCallback
import com.example.androidlocaltts.engine.OfflineTtsLike
import com.example.androidlocaltts.engine.SherpaOnnxTtsEngine
import com.k2fsa.sherpa.onnx.GeneratedAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyFloat
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class KokoroTtsServiceTest {

    @Test
    fun testInitializationInProgress_ReportsNotInstalledYet() {
        val service = KokoroTtsService()
        service.serviceScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        service.initState = KokoroTtsService.InitState.INITIALIZING
        service.isEngineReady = false

        val callback = mock(SynthesisCallback::class.java)
        service.handleSynthesis("test", 1.0f, callback)

        verify(callback).error()
    }

    @Test
    fun testConsecutiveRequests_CancelsPreviousJob() {
        val context = mock(Context::class.java)
        val mockTts = mock(OfflineTtsLike::class.java)
        val service = KokoroTtsService()
        service.serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        service.engine = SherpaOnnxTtsEngine(context) { _, _ -> mockTts }
        service.initState = KokoroTtsService.InitState.READY
        service.isEngineReady = true

        val ttsField = service.engine.javaClass.getDeclaredField("tts")
        ttsField.isAccessible = true
        ttsField.set(service.engine, mockTts)

        `when`(mockTts.generate(anyString(), anyInt(), anyFloat())).thenAnswer {
            Thread.sleep(100)
            GeneratedAudio(floatArrayOf(0f), 16000)
        }

        val callback1 = mock(SynthesisCallback::class.java)
        service.handleSynthesis("a。b。", 1.0f, callback1)

        val firstJob = getSynthesisJob(service)

        val callback2 = mock(SynthesisCallback::class.java)
        service.handleSynthesis("c。", 1.0f, callback2)

        assertTrue("Previous synthesis job should be cancelled", firstJob.isCancelled)
    }

    private fun getSynthesisJob(service: KokoroTtsService): kotlinx.coroutines.Job {
        val field: Field = service.javaClass.getDeclaredField("synthesisJob")
        field.isAccessible = true
        return field.get(service) as kotlinx.coroutines.Job
    }
}
