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
    private const val TAG = "KokoroTtsService"
    private lateinit var engine: SherpaOnnxTtsEngine
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isEngineReady = false

    override fun onCreate() {
        super.onCreate()
        engine = SherpaOnnxTtsEngine(this)
        
        serviceScope.launch(Dispatchers.IO) {
            // 1. Prepare assets if not already done
            if (!ModelManager.isModelPrepared(this@KokoroTtsService)) {
                Log.i(TAG, "Models not prepared. Copying from assets...")
                ModelManager.copyAssets(this@KokoroTtsService)
            }
            
            // 2. Initialize engine
            isEngineReady = engine.initialize()
            if (isEngineReady) {
                Log.i(TAG, "Engine ready for requests")
            } else {
                Log.e(TAG, "Failed to initialize engine")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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

        if (!isEngineReady) {
            Log.w(TAG, "Engine not ready yet. Synthesizing dummy silence or error.")
            callback.error(TextToSpeech.ERROR_SERVICE)
            return
        }

        // Use a dedicated scope for synthesis to avoid blocking
        serviceScope.launch(Dispatchers.IO) {
            engine.synthesize(text, speechRate, callback)
        }
    }

    override fun onStop() {
        Log.d(TAG, "Synthesis stopped by system")
        // Mutex in engine ensures we don't crash, but we could add a cancellation flag if needed
    }
}
