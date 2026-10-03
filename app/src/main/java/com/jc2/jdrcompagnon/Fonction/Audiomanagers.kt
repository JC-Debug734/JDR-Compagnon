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
    // Source jouée ("res:<id>" ou "file:<chemin>"), pour reprendre plutôt que relancer.
    private var currentKey: String? = null

    private val _currentTrack = MutableStateFlow<String?>(null)
    val currentTrack: StateFlow<String?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /**
     * Vrai quand le MJ a coupé la musique lui-même : la lecture automatique (musique de scène)
     * ne doit plus la relancer, même après un changement de scène ou un retour sur l'écran de
     * lecture, tant qu'il ne relance pas une piste manuellement ([play] / [resume]).
     */
    private val _mutedByUser = MutableStateFlow(false)
    val mutedByUser: StateFlow<Boolean> = _mutedByUser.asStateFlow()

    /** Coupure volontaire par le MJ (bouton son) : bloque la relecture automatique. */
    fun stopByUser() {
        _mutedByUser.value = true
        stop()
    }

    /** Pause volontaire par le MJ (écran Musique) : bloque aussi la relecture automatique. */
    fun pauseByUser() {
        _mutedByUser.value = true
        pause()
    }

    /**
     * Joue une piste locale en boucle.
     * Si la même piste est déjà en pause, elle reprend.
     * Si c'est une nouvelle piste, l'ancienne est arrêtée.
     */
    fun play(context: Context, resId: Int, trackName: String) =
        playSource(context, "res:$resId", trackName) { MediaPlayer.create(context.applicationContext, resId) }

    /** Joue une piste du catalogue, intégrée (res/raw) ou importée (fichier interne). */
    fun play(context: Context, track: LoopTrack) {
        val path = track.filePath
        if (path == null) {
            play(context, track.resId, track.displayName)
        } else {
            playSource(context, "file:$path", track.displayName) {
                MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                }
            }
        }
    }

    private fun playSource(context: Context, key: String, trackName: String, create: () -> MediaPlayer?) {
        // Toute lecture passe par un choix explicite (bouton) ou une lecture auto qui a déjà
        // vérifié mutedByUser : on lève donc la coupure.
        _mutedByUser.value = false
        if (currentKey == key && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
            _isPlaying.value = true
            _currentTrack.value = trackName
            return
        }

        stop()
        try {
            val player = create()
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
            currentKey = key
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
        _mutedByUser.value = false
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
        currentKey = null
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
 * Paramètres des effets sonores météo, persistés (voir GameState.weatherSoundSettings).
 */
@Serializable
data class WeatherSoundSettings(
    val enabled: Boolean = true,
    val volume: Float = 0.5f,
    // Scène en intérieur : la pluie est jouée étouffée (pluie_interieur.mp3). Réglage propre à
    // chaque appareil, comme le volume.
    val indoor: Boolean = false,
)

/**
 * Ambiance sonore météo (pluie, orage...), rejouée en boucle et synchronisée sur
 * ScenarioClockState.state.weather (voir ScenarioClockState.syncAmbientSound, appelé à chaque
 * changement de météo ou d'activation/désactivation de l'horloge, côté MJ comme côté joueurs).
 *
 * Les pistes sont lues depuis assets/son/effet (voir [trackFor]) ; une météo sans piste reste
 * silencieuse.
 */
object WeatherSoundManager {

    private const val EFFECTS_DIR = "son/effet"

    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: String? = null
    private var currentVolume: Float = 0.5f

    /** Fichier de assets/son/effet joué pour une météo (null = pas d'ambiance). */
    fun trackFor(weather: ScenarioWeather, indoor: Boolean): String? = when (weather) {
        ScenarioWeather.PLUIE -> if (indoor) "pluie_interieur.mp3" else "pluie.mp3"
        ScenarioWeather.ORAGE -> "orage_violent.mp3"
        else -> null
    }

    /** Joue (en boucle) l'ambiance associée à [weather] ; coupe le son si la météo n'en a pas. */
    fun play(context: Context, weather: ScenarioWeather, indoor: Boolean = false) {
        val track = trackFor(weather, indoor)
        if (track == null) {
            stop()
            return
        }
        if (currentTrack == track && mediaPlayer != null) return
        stop()
        val player = MediaPlayer()
        try {
            context.applicationContext.assets.openFd("$EFFECTS_DIR/$track").use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = true
            player.setVolume(currentVolume, currentVolume)
            player.prepare()
            player.start()
            mediaPlayer = player
            currentTrack = track
        } catch (e: Exception) {
            android.util.Log.e("WeatherSoundManager", "Impossible de jouer l'ambiance météo", e)
            runCatching { player.release() }
            stop()
        }
    }

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(currentVolume, currentVolume)
        } catch (_: Exception) { }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) { }
        mediaPlayer = null
        currentTrack = null
    }
}

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
    // Piste intégrée (res/raw) : resId renseigné. Piste importée par le MJ : filePath (copie
    // dans le stockage interne, voir ImportedMusicStore), resId à 0.
    val resId: Int = 0,
    val filePath: String? = null,
) {
    val isImported: Boolean get() = filePath != null
}

/** Pistes intégrées + pistes importées par le MJ (écran Musique, bouton « Importer »). */
val availableLoopTracks: List<LoopTrack>
    get() = builtInLoopTracks + ImportedMusicStore.tracks.value

/**
 * Musiques importées par le MJ depuis le sélecteur de fichiers : le fichier est copié dans le
 * stockage interne de l'app (une URI "content://" choisie ponctuellement ne survit pas toujours
 * au redémarrage), la liste (id, nom, fichier) est persistée en SharedPreferences. Une piste
 * importée s'utilise partout comme une piste intégrée (scènes, environnements) via son id.
 */
object ImportedMusicStore {
    private const val PREFS_NAME = "imported_music"
    private const val KEY_TRACKS = "tracks_json"
    private const val DIR_NAME = "musique_importee"

    @Serializable
    private data class StoredTrack(val id: String, val name: String, val fileName: String)

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private var stored: List<StoredTrack> = emptyList()
    private var dir: java.io.File? = null

    private val _tracks = MutableStateFlow<List<LoopTrack>>(emptyList())
    val tracks: StateFlow<List<LoopTrack>> = _tracks.asStateFlow()

    fun init(context: Context) {
        dir = java.io.File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val raw = prefs(context).getString(KEY_TRACKS, null)
        stored = raw?.let { runCatching { json.decodeFromString<List<StoredTrack>>(it) }.getOrNull() }.orEmpty()
        publish()
    }

    /**
     * Copie le fichier audio [uri] et l'ajoute sous le nom [name]. Retourne la piste créée, ou
     * null si le fichier n'a pas pu être lu.
     */
    fun import(context: Context, uri: android.net.Uri, name: String, originalFileName: String?): LoopTrack? {
        val targetDir = dir ?: java.io.File(context.filesDir, DIR_NAME).also { it.mkdirs(); dir = it }
        val id = "import_" + System.currentTimeMillis()
        val extension = originalFileName?.substringAfterLast('.', "")?.takeIf { it.length in 1..5 } ?: "mp3"
        val fileName = "$id.$extension"
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                java.io.File(targetDir, fileName).outputStream().use { input.copyTo(it) }
            } != null
        }.getOrDefault(false)
        if (!ok) return null
        stored = stored + StoredTrack(id, name.ifBlank { originalFileName ?: "Musique importée" }, fileName)
        save(context)
        publish()
        return _tracks.value.firstOrNull { it.id == id }
    }

    fun rename(context: Context, id: String, name: String) {
        if (name.isBlank()) return
        stored = stored.map { if (it.id == id) it.copy(name = name.trim()) else it }
        save(context)
        publish()
    }

    fun remove(context: Context, id: String) {
        val track = stored.firstOrNull { it.id == id } ?: return
        dir?.let { java.io.File(it, track.fileName).delete() }
        stored = stored - track
        save(context)
        publish()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun save(context: Context) {
        prefs(context).edit().putString(KEY_TRACKS, json.encodeToString(kotlinx.serialization.builtins.ListSerializer(StoredTrack.serializer()), stored)).apply()
    }

    private fun publish() {
        val base = dir
        _tracks.value = stored.mapNotNull { t ->
            val file = base?.let { java.io.File(it, t.fileName) } ?: return@mapNotNull null
            if (!file.exists()) return@mapNotNull null
            LoopTrack(id = t.id, displayName = t.name, filePath = file.absolutePath)
        }
    }
}

private val builtInLoopTracks: List<LoopTrack> = listOf(
    LoopTrack(id = "verres_et_dagues", displayName = "Taverne animée", resId = R.raw.verres_et_dagues),
    LoopTrack(id = "campfireattheinn", displayName = "Feu de camp à l'auberge", resId = R.raw.campfireattheinn),
    LoopTrack(id = "shadowcharge", displayName = "Charge de l'ombre", resId = R.raw.shadowcharge),
    LoopTrack(id = "shadowchargeepic", displayName = "Charge de l'ombre (épique)", resId = R.raw.shadowchargeepic),
    LoopTrack(id = "stoneechoes", displayName = "Échos de pierre", resId = R.raw.stoneechoes),
    LoopTrack(id = "stoneroomechoes", displayName = "Échos de la salle de pierre", resId = R.raw.stoneroomechoes),
)