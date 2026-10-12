package com.rave.app.tracking

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Text-to-speech for split announcements ("1 kilometre, pace 6 minutes 10"). */
@Singleton
class VoiceCoach @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = mutableListOf<String>()

    fun warmUp() {
        if (tts == null) tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts?.language = Locale.getDefault()
            pending.forEach(::speak)
        }
        pending.clear()
    }

    fun speak(text: String) {
        warmUp()
        if (ready) tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "split_${text.hashCode()}")
        else pending += text
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
