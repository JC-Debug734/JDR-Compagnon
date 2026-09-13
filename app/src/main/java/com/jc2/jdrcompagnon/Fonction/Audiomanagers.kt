package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.jc2.jdrcompagnon.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

// ─────────────────────────────────────────────────────────────
// Gestion audio de l'application : musique de fond (MusicManager)
// et effets sonores (SoundManager), fusionnés dans un seul fichier
// (même package com.jc2.jdrcompagnon.ui). MusicScreen.kt reste à
// part car il vit dans un package différent (ui.screens.mj).
// ─────────────────────────────────────────────────────────────

/**
 * Gestionnaire global de musique de fond.
 * Singleton, calqué sur SoundManager. Le MediaPlayer survit à la navigation et
 * est libéré à la destruction de l'application.
 *
 * V6 : supporte aussi les playlists YouTube (mode externe) avec un état réactif.
 */
object MusicManager {

    private var mediaPlayer: MediaPlayer? = null
    private var currentResId: Int? = null

    private val _currentTrack = MutableStateFlow<String?>(null)
    val currentTrack: StateFlow<String?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /**
     * Joue une piste locale en boucle.
     * Si la même piste est déjà en pause, elle reprend.
     * Si c'est une nouvelle piste, l'ancienne est arrêtée.
     */
    fun play(context: Context, resId: Int, trackName: String) {
        if (currentResId == resId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
            _isPlaying.value = true
            _currentTrack.value = trackName
            return
        }

        stop()
        try {
            val player = MediaPlayer.create(context.applicationContext, resId)
            player?.apply {
                isLooping = true
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
                start()
            }
            mediaPlayer = player
            currentResId = resId
            _isPlaying.value = player != null
            _currentTrack.value = trackName
        } catch (e: Exception) {
            android.util.Log.e("MusicManager", "Impossible de jouer la piste", e)
            stop()
        }
    }

    /**
     * Marque une piste YouTube/externe comme active sans MediaPlayer local.
     * L'indicateur reste visible ; la lecture est gérée par l'app externe.
     */
    fun playYouTube(trackName: String) {
        // Arrête la piste locale éventuelle, mais conserve le trackName
        if (mediaPlayer != null) {
            stop()
        }
        _currentTrack.value = trackName
        _isPlaying.value = true
    }

    /**
     * Arrête YouTube uniquement si aucune piste locale n'est active.
     * Utilisé au changement de monde pour ne pas couper la musique locale.
     */
    fun stopIfYouTube() {
        if (mediaPlayer == null && _currentTrack.value != null) {
            stop()
        }
    }

    /**
     * Met la musique en pause sans libérer le player.
     */
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) { }
    }

    fun resume() {
        try {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
                _isPlaying.value = true
            }
        } catch (_: Exception) { }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {
        }
        _isPlaying.value = false
    }

    /**
     * Arrête et libère le MediaPlayer.
     */
    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        currentResId = null
        _isPlaying.value = false
        _currentTrack.value = null
    }

    /**
     * Libère les ressources audio. À appeler à la destruction de l'application.
     */
    fun release() {
        stop()
    }
}

/**
 * Paramètres de musique persistés.
 */
@Serializable
data class MusicSettings(
    val volume: Float = 0.7f,
    val lastTrackName: String? = null,
    val loop: Boolean = true
)

/**
 * Gestionnaire audio pour les sons de dés.
 * Singleton initialisé depuis MainActivity et libéré dans onDestroy.
 */
object SoundManager {

    private var soundPool: SoundPool? = null
    private var rollSoundId: Int = 0
    private var resultSoundId: Int = 0
    private var loaded = false

    fun init(context: Context) {
        if (loaded) return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(audioAttributes)
                .build()
                .apply {
                    rollSoundId = load(context, R.raw.dice_roll, 1)
                    resultSoundId = load(context, R.raw.dice_result, 1)
                }
            loaded = true
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Impossible d'initialiser les sons", e)
            release()
        }
    }

    fun playRoll() {
        if (!shouldPlay()) return
        try {
            soundPool?.play(rollSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Erreur lecture son de lancer", e)
        }
    }

    fun playResult() {
        if (!shouldPlay()) return
        try {
            soundPool?.play(resultSoundId, 1f, 1f, 1, 0, 1f)
        } catch (e: Exception) {
            android.util.Log.e("SoundManager", "Erreur lecture son de résultat", e)
        }
    }

    private fun shouldPlay(): Boolean {
        return loaded && GameState.diceState.value.soundEnabled
    }

    fun release() {
        try {
            soundPool?.release()
        } catch (_: Exception) {
            // ignore
        }
        soundPool = null
        loaded = false
        rollSoundId = 0
        resultSoundId = 0
    }
}

/**
 * Catalogue des pistes musicales locales, jouables en boucle.
 * `id` est la clé stable utilisée dans le markdown des scénarios
 * ({mscenemeta: music=<id>}) et dans les préférences persistées ;
 * `displayName` est ce qui s'affiche dans l'UI (MusicScreen, éditeur
 * de scénario...) ; `resId` pointe vers le fichier dans res/raw.
 * Source unique : ScenarioEditorScreen et MusicScreen doivent tous
 * les deux lire cette liste plutôt que d'en maintenir une copie.
 */
data class LoopTrack(
    val id: String,
    val displayName: String,
    val resId: Int
)

val availableLoopTracks: List<LoopTrack> = listOf(
    LoopTrack(id = "verres_et_dagues", displayName = "Taverne animée", resId = R.raw.verres_et_dagues),
    LoopTrack(id = "campfireattheinn", displayName = "Feu de camp à l'auberge", resId = R.raw.campfireattheinn),
    LoopTrack(id = "shadowcharge", displayName = "Charge de l'ombre", resId = R.raw.shadowcharge),
    LoopTrack(id = "shadowchargeepic", displayName = "Charge de l'ombre (épique)", resId = R.raw.shadowchargeepic),
    LoopTrack(id = "stoneechoes", displayName = "Échos de pierre", resId = R.raw.stoneechoes),
    LoopTrack(id = "stoneroomechoes", displayName = "Échos de la salle de pierre", resId = R.raw.stoneroomechoes),
)