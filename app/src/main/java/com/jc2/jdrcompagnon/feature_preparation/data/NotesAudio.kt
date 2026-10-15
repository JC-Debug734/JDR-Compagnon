package com.jc2.jdrcompagnon.feature_preparation.data

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Enregistrement et lecture des notes audio du MJ, en AAC (.m4a) dans le stockage interne de
 * l'app (dossier notes_audio). Un seul enregistrement et une seule lecture à la fois.
 */
object NotesAudio {

    private const val DOSSIER = "notes_audio"

    private var recorder: MediaRecorder? = null
    private var fichierEnCours: File? = null
    private var debut = 0L

    private var player: MediaPlayer? = null

    fun fichier(context: Context, nom: String): File = File(File(context.filesDir, DOSSIER), nom)

    /** Démarre un enregistrement ; renvoie false si le micro est indisponible. */
    fun demarrer(context: Context): Boolean {
        arreterEnregistrement()
        val dossier = File(context.filesDir, DOSSIER).apply { mkdirs() }
        val fichier = File(dossier, "note_${System.currentTimeMillis()}.m4a")
        return runCatching {
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(fichier.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            fichierEnCours = fichier
            debut = System.currentTimeMillis()
        }.onFailure {
            recorder = null
            fichier.delete()
        }.isSuccess
    }

    /** Arrête l'enregistrement ; renvoie (nom du fichier, durée en ms), ou null si rien d'exploitable. */
    fun arreterEnregistrement(): Pair<String, Long>? {
        val r = recorder ?: return null
        val fichier = fichierEnCours
        val duree = System.currentTimeMillis() - debut
        recorder = null
        fichierEnCours = null
        // stop() échoue si l'enregistrement est trop court : le fichier est alors inutilisable.
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        if (!ok || fichier == null) {
            fichier?.delete()
            return null
        }
        return fichier.name to duree
    }

    /** Lit le fichier [nom] ; [aLaFin] est appelé quand la lecture se termine ou échoue. */
    fun lire(context: Context, nom: String, aLaFin: () -> Unit) {
        arreterLecture()
        val p = MediaPlayer()
        runCatching {
            p.setDataSource(fichier(context, nom).absolutePath)
            p.setOnCompletionListener { arreterLecture(); aLaFin() }
            p.prepare()
            p.start()
            player = p
        }.onFailure {
            p.release()
            aLaFin()
        }
    }

    fun arreterLecture() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}
