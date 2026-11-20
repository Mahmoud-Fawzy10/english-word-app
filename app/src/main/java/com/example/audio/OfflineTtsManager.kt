package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class OfflineTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var isLanguageAvailable = false
    @Volatile
    private var speechRate: Float = 0.92f
    @Volatile
    private var onUtteranceCompleted: ((String) -> Unit)? = null

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val resUs = engine.setLanguage(Locale.US)
                    val resUk = if (resUs == TextToSpeech.LANG_MISSING_DATA || resUs == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.setLanguage(Locale.UK)
                    } else {
                        resUs
                    }
                    isLanguageAvailable = resUk != TextToSpeech.LANG_MISSING_DATA &&
                        resUk != TextToSpeech.LANG_NOT_SUPPORTED
                    engine.setSpeechRate(speechRate)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit
                        override fun onDone(utteranceId: String?) {
                            if (utteranceId != null) {
                                onUtteranceCompleted?.invoke(utteranceId)
                            }
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (utteranceId != null) {
                                onUtteranceCompleted?.invoke(utteranceId)
                            }
                        }
                    })
                    isInitialized = true
                }
            }
        }
    }

    fun isAvailable(): Boolean = isInitialized && isLanguageAvailable

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.6f, 1.4f)
        runCatching { tts?.setSpeechRate(speechRate) }
    }

    fun setUtteranceListener(listener: ((String) -> Unit)?) {
        onUtteranceCompleted = listener
    }

    /**
     * Speaks the given text strictly offline using local device TTS synthesis.
     * Returns true if playback started, false if offline TTS is unavailable on this device.
     */
    fun speakOffline(text: String, utteranceId: String = "qamusi_tts_${System.currentTimeMillis()}"): Boolean {
        val clean = text.trim()
        val engine = tts ?: return false
        if (clean.isEmpty() || !isInitialized || !isLanguageAvailable) return false

        engine.setSpeechRate(speechRate)
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "false")
        }
        val result = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            onUtteranceCompleted = null
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isLanguageAvailable = false
        }
    }
}
