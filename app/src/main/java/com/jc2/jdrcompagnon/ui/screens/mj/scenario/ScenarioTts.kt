package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.UUID

/**
 * Synthèse vocale des zones "à lire aux joueurs" ({lire}…{/lire}) du lecteur de scénario.
 * [speakingText] = texte en cours de lecture (null si rien), pour basculer l'icône lecture/stop.
 */
class ScenarioTts(context: Context) {
    var speakingText by mutableStateOf<String?>(null)
        private set

    @Volatile private var ready = false
    @Volatile private var currentUtteranceId: String? = null
    private val main = Handler(Looper.getMainLooper())
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.FRANCE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(Locale.getDefault())
                }
                ready = true
            }
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = finished(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finished(utteranceId)
        })
    }

    private fun finished(utteranceId: String?) {
        main.post { if (utteranceId == currentUtteranceId) speakingText = null }
    }

    /** Lit [text], ou arrête la lecture s'il est déjà en cours. Retourne false si la synthèse est indisponible. */
    fun toggle(text: String): Boolean {
        if (speakingText == text) {
            tts.stop()
            speakingText = null
            return true
        }
        if (!ready || text.isBlank()) return false
        val id = UUID.randomUUID().toString()
        currentUtteranceId = id
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        speakingText = text
        return true
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

@Composable
fun rememberScenarioTts(): ScenarioTts {
    val context = LocalContext.current
    val tts = remember { ScenarioTts(context) }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }
    return tts
}
