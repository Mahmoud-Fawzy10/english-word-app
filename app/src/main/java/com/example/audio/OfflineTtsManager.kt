package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

class OfflineTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var isLanguageAvailable = false

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
                    engine.setSpeechRate(0.92f)
                    isInitialized = true
                }
            }
        }
    }

    fun isAvailable(): Boolean = isInitialized && isLanguageAvailable

    /**
     * Speaks the given text strictly offline using local device TTS synthesis.
     * Returns true if playback started, false if offline TTS is unavailable on this device.
     */
    fun speakOffline(text: String): Boolean {
        val clean = text.trim()
        val engine = tts ?: return false
        if (clean.isEmpty() || !isInitialized || !isLanguageAvailable) return false

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "false")
        }
        val result = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, params, "qamusi_tts_${System.currentTimeMillis()}")
        return result == TextToSpeech.SUCCESS
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            isLanguageAvailable = false
        }
    }
}
