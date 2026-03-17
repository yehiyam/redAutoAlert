package com.redautoalert.processor

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.redautoalert.model.AlertEvent
import com.redautoalert.model.AlertProcessor
import com.redautoalert.util.PrefsManager
import java.util.Locale
import java.util.UUID

/**
 * Announces alerts using Text-to-Speech, ensuring the driver hears them.
 * Handles audio focus to pause music during announcement, then resume.
 */
class TtsAlertAnnouncer(private val context: Context) : AlertProcessor {

    companion object {
        private const val TAG = "TtsAnnouncer"
    }

    private val prefs = PrefsManager(context)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var audioFocusRequest: AudioFocusRequest? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                updateLanguage()
                Log.i(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    fun updateLanguage() {
        val locale = when (prefs.ttsLanguage) {
            "he" -> Locale("he", "IL")
            "en" -> Locale.US
            "ru" -> Locale("ru", "RU")
            "ar" -> Locale("ar")
            else -> Locale("he", "IL")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language $locale not supported, falling back to default")
            tts?.setLanguage(Locale.US)
        }
    }

    override fun onAlert(event: AlertEvent) {
        if (!ttsReady) {
            Log.w(TAG, "TTS not ready, skipping announcement")
            return
        }

        val announcement = buildAnnouncement(event)
        speak(announcement)
    }

    override fun isEnabled(): Boolean = prefs.isTtsEnabled

    private fun buildAnnouncement(event: AlertEvent): String {
        val isHebrew = prefs.ttsLanguage == "he"

        val typeText = if (isHebrew) {
            when (event.alertType) {
                AlertEvent.AlertType.ROCKET -> "צבע אדום"
                AlertEvent.AlertType.DRONE -> "חדירת כלי טיס עוין"
                AlertEvent.AlertType.EARTHQUAKE -> "רעידת אדמה"
                AlertEvent.AlertType.TSUNAMI -> "צונמי"
                AlertEvent.AlertType.HAZARDOUS_MATERIALS -> "חומרים מסוכנים"
                AlertEvent.AlertType.TERRORIST_INFILTRATION -> "חדירת מחבלים"
                AlertEvent.AlertType.UNKNOWN -> "התרעה"
            }
        } else {
            when (event.alertType) {
                AlertEvent.AlertType.ROCKET -> "Red Alert"
                AlertEvent.AlertType.DRONE -> "Hostile aircraft infiltration"
                AlertEvent.AlertType.EARTHQUAKE -> "Earthquake"
                AlertEvent.AlertType.TSUNAMI -> "Tsunami"
                AlertEvent.AlertType.HAZARDOUS_MATERIALS -> "Hazardous materials"
                AlertEvent.AlertType.TERRORIST_INFILTRATION -> "Terrorist infiltration"
                AlertEvent.AlertType.UNKNOWN -> "Alert"
            }
        }

        val citiesText = event.cities.joinToString(", ")

        return if (isHebrew) {
            "$typeText: $citiesText"
        } else {
            "$typeText in: $citiesText"
        }
    }

    private fun speak(text: String) {
        requestAudioFocus()

        val utteranceId = UUID.randomUUID().toString()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                abandonAudioFocus()
            }
            override fun onError(id: String?) {
                abandonAudioFocus()
            }
        })

        // Speak with elevated volume using STREAM_ALARM to bypass media volume settings
        val params = android.os.Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        Log.i(TAG, "Speaking: $text")
    }

    private fun requestAudioFocus() {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()

        audioFocusRequest = focusRequest
        audioManager.requestAudioFocus(focusRequest)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
        }
        audioFocusRequest = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        abandonAudioFocus()
    }
}
