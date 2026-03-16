package com.example.androidlocaltts.service

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import com.example.androidlocaltts.engine.SherpaOnnxTtsEngine
import com.example.androidlocaltts.utils.ModelManager
import kotlinx.coroutines.*
import java.util.*

class KokoroTtsService : TextToSpeechService() {
    companion object {
        private const val TAG = "KokoroTtsService"
    }
    internal lateinit var engine: SherpaOnnxTtsEngine
    internal var serviceScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    internal enum class InitState {
        INITIALIZING,
        READY,
        FAILED
    }
    
    @Volatile
    internal var isEngineReady = false

    @Volatile
    internal var initState: InitState = InitState.INITIALIZING
    
    // Ongoing synthesis job to allow cancellation
    private var synthesisJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        engine = SherpaOnnxTtsEngine(this)
        initState = InitState.INITIALIZING
        
        serviceScope.launch(Dispatchers.IO) {
            // 1. Prepare assets if not already done
            if (!ModelManager.isModelPrepared(this@KokoroTtsService.filesDir)) {
                Log.i(TAG, "Models not prepared. Copying from assets...")
                ModelManager.copyAssets(this@KokoroTtsService)
            }
            
            // 2. Initialize engine
            isEngineReady = engine.initialize()
            initState = if (isEngineReady) InitState.READY else InitState.FAILED
            if (isEngineReady) {
                Log.i(TAG, "Engine ready for requests")
            } else {
                Log.e(TAG, "Failed to initialize engine")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        synthesisJob?.cancel()
        engine.release()
        serviceScope.cancel()
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        // We only support Japanese for now
        return if (lang == "jpn" || lang == "ja") {
            TextToSpeech.LANG_AVAILABLE
        } else {
            TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onGetLanguage(): Array<String> {
        return arrayOf("ja", "jpn", "")
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        return onIsLanguageAvailable(lang, country, variant)
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return

        val text = request.charSequenceText.toString()
        val speechRate = request.speechRate / 100.0f // Normal is 100
        
        Log.d(TAG, "Synthesis request for text: ${text.take(20)}... Speed: $speechRate")

        handleSynthesis(text, speechRate, callback)
    }

    internal fun handleSynthesis(text: String, speechRate: Float, callback: SynthesisCallback) {
        if (initState == InitState.INITIALIZING) {
            Log.w(TAG, "Engine initializing. Rejecting synthesis request.")
            reportError(callback, TextToSpeech.ERROR_NOT_INSTALLED_YET)
            return
        }
        if (!isEngineReady || initState == InitState.FAILED) {
            Log.w(TAG, "Engine not ready. Rejecting synthesis request.")
            reportError(callback, TextToSpeech.ERROR_SERVICE)
            return
        }

        // Cancel previous job if it's still running
        synthesisJob?.cancel()

        // Use a dedicated scope for synthesis to avoid blocking
        synthesisJob = serviceScope.launch(Dispatchers.IO) {
            try {
                engine.synthesize(text, speechRate, callback)
            } finally {
                if (synthesisJob?.isActive == false) {
                    Log.d(TAG, "Synthesis job cancelled")
                }
            }
        }
    }

    override fun onStop() {
        Log.d(TAG, "Synthesis stopped by system")
        synthesisJob?.cancel()
    }

    private fun reportError(callback: SynthesisCallback, code: Int) {
        try {
            callback.error(code)
        } catch (e: NoSuchMethodError) {
            callback.error()
        }
    }
}
