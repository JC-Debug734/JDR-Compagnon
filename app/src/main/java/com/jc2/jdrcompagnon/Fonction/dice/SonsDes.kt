package com.jc2.jdrcompagnon.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.jc2.jdrcompagnon.ui.GameState
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sons de l'outil Dé, synthétisés par l'application (aucun fichier audio) : un dé qui roule et
 * rebondit sur la table (chocs de plus en plus rapprochés et faibles, chacun un peu différent),
 * puis un tintement au résultat — plus éclatant sur un 20 naturel, plus sourd sur un 1.
 * Joués seulement si « Sons des dés » est activé.
 */
object SonsDes {

    private const val TAUX = 44100
    private val lecteur = Executors.newSingleThreadExecutor()

    private fun actifs() = GameState.diceState.value.soundEnabled

    /** Le dé roule (à chaque lancer un tirage différent des rebonds). */
    fun lancer() {
        if (!actifs()) return
        val graine = Random.nextInt()
        lecteur.execute { lire(roulement(Random(graine))) }
    }

    /** Résultat affiché : [critique] (20 naturel) ou [echec] (1 naturel) changent le tintement. */
    fun resultat(critique: Boolean = false, echec: Boolean = false) {
        if (!actifs()) return
        lecteur.execute { lire(tintement(critique, echec)) }
    }

    private fun lire(signal: FloatArray) {
        val crete = signal.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0f } ?: return
        val pcm = ShortArray(signal.size) { i -> (signal[i] / crete * 0.8f * Short.MAX_VALUE).toInt().toShort() }
        runCatching {
            val piste = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(TAUX)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            piste.write(pcm, 0, pcm.size)
            piste.play()
            Thread.sleep(pcm.size * 1000L / TAUX + 60)
            piste.release()
        }
    }

    private fun echantillons(ms: Double) = (ms * TAUX / 1000).toInt()

    /**
     * Un choc du dé sur la table : quelques résonances amorties (le « clac » du dé en résine), un
     * bref claquement de bruit à l'attaque et un léger « toc » grave du plateau.
     */
    private fun choc(sortie: FloatArray, debut: Int, force: Float, alea: Random) {
        val modes = listOf(
            2100.0 * (0.9 + alea.nextDouble() * 0.2) to 1.0,
            3450.0 * (0.9 + alea.nextDouble() * 0.2) to 0.6,
            5200.0 * (0.9 + alea.nextDouble() * 0.2) to 0.35,
        )
        val duree = echantillons(45.0)
        for (i in 0 until duree) {
            val idx = debut + i
            if (idx >= sortie.size) break
            val t = i.toDouble() / TAUX
            var v = 0.0
            modes.forEach { (f, a) -> v += a * sin(2 * PI * f * t) * exp(-t * (90 + f / 40)) }
            // Claquement initial (2 ms de bruit) et « toc » grave du plateau.
            if (i < echantillons(2.0)) v += (alea.nextDouble() * 2 - 1) * 0.8 * (1 - i / echantillons(2.0).toDouble())
            v += 0.5 * sin(2 * PI * 170 * t) * exp(-t * 70)
            sortie[idx] += (v * force).toFloat()
        }
    }

    /** Le dé lancé : 9 à 12 rebonds, de plus en plus rapprochés et faibles, puis il s'immobilise. */
    private fun roulement(alea: Random): FloatArray {
        val sortie = FloatArray(echantillons(900.0))
        var instant = echantillons(10.0)
        var ecart = 150.0
        var force = 1f
        val rebonds = 9 + alea.nextInt(4)
        repeat(rebonds) {
            choc(sortie, instant, force, alea)
            // Parfois deux arêtes touchent presque en même temps.
            if (alea.nextInt(4) == 0) choc(sortie, instant + echantillons(8.0 + alea.nextDouble() * 6), force * 0.5f, alea)
            instant += echantillons(ecart * (0.85 + alea.nextDouble() * 0.3))
            ecart *= 0.8
            force *= 0.8f
        }
        return sortie
    }

    /** Note de cloche douce (fondamentale + partiel inharmonique qui s'éteint vite). */
    private fun cloche(sortie: FloatArray, debut: Int, freq: Double, duree: Double, volume: Double) {
        val n = echantillons(duree)
        for (i in 0 until n) {
            val idx = debut + i
            if (idx >= sortie.size) break
            val t = i.toDouble() / TAUX
            val attaque = (i / echantillons(4.0).toDouble()).coerceAtMost(1.0)
            val v = sin(2 * PI * freq * t) * exp(-t * 5) + 0.35 * sin(2 * PI * freq * 2.76 * t) * exp(-t * 14)
            sortie[idx] += (v * attaque * volume).toFloat()
        }
    }

    private fun tintement(critique: Boolean, echec: Boolean): FloatArray = when {
        critique -> FloatArray(echantillons(1300.0)).also { s ->
            listOf(784.0, 988.0, 1175.0, 1568.0).forEachIndexed { i, f -> cloche(s, echantillons(i * 90.0), f, 1000.0, 0.6) }
        }
        echec -> FloatArray(echantillons(900.0)).also { s ->
            cloche(s, 0, 330.0, 700.0, 0.7)
            cloche(s, echantillons(160.0), 247.0, 700.0, 0.7)
        }
        else -> FloatArray(echantillons(700.0)).also { s -> cloche(s, 0, 1046.0, 700.0, 0.6) }
    }
}
