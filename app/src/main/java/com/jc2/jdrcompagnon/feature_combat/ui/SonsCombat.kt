package com.jc2.jdrcompagnon.feature_combat.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombatStore
import com.jc2.jdrcompagnon.feature_combat.presentation.TypeRecit
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/** Famille d'un bruitage : chacune a son volume dans les réglages du combat. */
enum class CategorieSon { IMPACT, MAGIE, ANNONCE }

/**
 * Bruitages du combat (coup porté, critique, raté, chute, soin, nouveau round, victoire…), joués
 * sur l'appareil du MJ et sur celui des joueurs quand une ligne du récit apparaît.
 *
 * Fichiers : assets/son/combat/<clé>.ogg ou plusieurs variantes <clé>_1.ogg, <clé>_2.ogg… (ogg,
 * mp3 ou wav), tirées au hasard. Clés : voir [Son]. Une clé sans fichier est synthétisée par
 * l'application (repli : aujourd'hui « tour »). Lecture par SoundPool : sons préchargés, joués
 * sans délai et superposables.
 *
 * Les fichiers fournis sont montés (superpositions, salle, niveaux) à partir des packs Kenney
 * « RPG Audio », « Impact Sounds » et « Interface Sounds » (kenney.nl, licence CC0) ; la fanfare
 * de victoire est « Victory Fanfare Short » de cynicmusic (OpenGameArt, CC0).
 */
object SonsCombat {

    /** Un bruitage : [cle] = nom de fichier, [fort] = la musique baisse pendant qu'il sonne ([dureeMs]). */
    enum class Son(val cle: String, val libelle: String, val categorie: CategorieSon, val fort: Boolean = false, val dureeMs: Long = 600) {
        TOUCHE("touche", "Coup porté", CategorieSon.IMPACT),
        CRITIQUE("critique", "Coup critique", CategorieSon.IMPACT, fort = true, dureeMs = 1300),
        RATE("rate", "Attaque ratée", CategorieSon.IMPACT),
        KO("ko", "Personnage à terre", CategorieSon.IMPACT, fort = true, dureeMs = 1200),
        MORT("mort", "Ennemi abattu", CategorieSon.IMPACT, fort = true, dureeMs = 2000),
        SOIN("soin", "Soin", CategorieSon.MAGIE),
        ETAT("etat", "État infligé", CategorieSon.MAGIE),
        ROUND("round", "Nouveau round", CategorieSon.ANNONCE, fort = true, dureeMs = 1500),
        TOUR("tour", "Tour suivant", CategorieSon.ANNONCE),
        MON_TOUR("mon_tour", "« À toi de jouer »", CategorieSon.ANNONCE),
        BOSS("boss", "Boss", CategorieSon.ANNONCE, fort = true, dureeMs = 2200),
        VICTOIRE("victoire", "Victoire", CategorieSon.ANNONCE, fort = true, dureeMs = 9000),
    }

    private const val DOSSIER = "son/combat"
    private const val TAUX = 44100
    // Version de la synthèse de repli : la changer régénère les fichiers en cache.
    private const val VERSION_SYNTHESE = 2

    private var appContext: Context? = null
    private var pool: SoundPool? = null
    private val charges = mutableMapOf<Son, MutableList<Int>>()
    private val derniereLecture = mutableMapOf<Son, Long>()
    private val chargeur = Executors.newSingleThreadExecutor()
    private val principal = Handler(Looper.getMainLooper())
    private val retablirMusique = Runnable { MusicManager.setVolume(GameState.musicSettings.value.volume) }

    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        ReglagesCombatStore.init(ctx)
        val p = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool = p
        val enAttente = mutableMapOf<Int, Son>()
        p.setOnLoadCompleteListener { _, id, statut ->
            val son = synchronized(enAttente) { enAttente.remove(id) } ?: return@setOnLoadCompleteListener
            if (statut == 0) synchronized(charges) { charges.getOrPut(son) { mutableListOf() }.add(id) }
        }
        chargeur.execute {
            val fichiers = runCatching { ctx.assets.list(DOSSIER)?.toList() }.getOrNull().orEmpty()
            Son.entries.forEach { son ->
                val variantes = fichiers.filter { f ->
                    val base = f.substringBeforeLast('.')
                    f.substringAfterLast('.').lowercase() in setOf("ogg", "mp3", "wav") &&
                        (base == son.cle || base.matches(Regex("${son.cle}_\\d+")))
                }
                runCatching {
                    if (variantes.isNotEmpty()) {
                        variantes.forEach { f ->
                            ctx.assets.openFd("$DOSSIER/$f").use { fd -> synchronized(enAttente) { enAttente[p.load(fd, 1)] = son } }
                        }
                    } else {
                        val wav = fichierSynthese(ctx, son)
                        synchronized(enAttente) { enAttente[p.load(wav.absolutePath, 1)] = son }
                    }
                }
            }
        }
    }

    fun basculer() = ReglagesCombatStore.modifier { it.copy(sons = !it.sons) }

    /** Son d'une ligne du récit ; [monTour] : « c'est à toi » (un appel plus marqué). */
    fun jouer(type: TypeRecit, monTour: Boolean = false) {
        if (!ReglagesCombatStore.courant.sons) return
        val son = if (monTour) Son.MON_TOUR else sonDe(type) ?: return
        lire(son)
    }

    /** Écoute d'un bruitage depuis les réglages, même sons coupés. */
    fun essayer(son: Son) = lire(son, forcer = true)

    private fun lire(son: Son, forcer: Boolean = false) {
        val p = pool ?: return
        val maintenant = SystemClock.elapsedRealtime()
        // Plusieurs lignes identiques d'un coup (attaques multiples) : un seul son.
        if (!forcer && maintenant - (derniereLecture[son] ?: 0L) < 120) return
        derniereLecture[son] = maintenant
        val id = synchronized(charges) { charges[son]?.randomOrNull() } ?: return
        val r = ReglagesCombatStore.courant
        val volume = (r.volume * when (son.categorie) {
            CategorieSon.IMPACT -> r.volumeImpacts
            CategorieSon.MAGIE -> r.volumeMagie
            CategorieSon.ANNONCE -> r.volumeAnnonces
        }).coerceIn(0f, 1f)
        if (volume <= 0f) return
        // Légère variation de hauteur sur les impacts : deux coups d'épée ne sonnent jamais pareil.
        val vitesse = if (son.categorie == CategorieSon.IMPACT) 0.93f + Random.nextFloat() * 0.14f else 1f
        p.play(id, volume, volume, if (son.fort) 2 else 1, 0, vitesse)
        if (son.fort && r.attenuerMusique) {
            principal.removeCallbacks(retablirMusique)
            MusicManager.setVolume(GameState.musicSettings.value.volume * 0.35f)
            principal.postDelayed(retablirMusique, son.dureeMs)
        }
    }

    private fun sonDe(type: TypeRecit): Son? = when (type) {
        TypeRecit.TOUCHE, TypeRecit.DEGATS -> Son.TOUCHE
        TypeRecit.CRITIQUE -> Son.CRITIQUE
        TypeRecit.RATE -> Son.RATE
        TypeRecit.KO -> Son.KO
        TypeRecit.MORT -> Son.MORT
        TypeRecit.SOIN -> Son.SOIN
        TypeRecit.ETAT -> Son.ETAT
        TypeRecit.ROUND -> Son.ROUND
        TypeRecit.TOUR -> Son.TOUR
        TypeRecit.BOSS -> Son.BOSS
        TypeRecit.VICTOIRE -> Son.VICTOIRE
        else -> null
    }

    /** Lignes du récit qui font du bruit (les autres restent silencieuses). */
    fun sonore(type: TypeRecit): Boolean = type != TypeRecit.TOUR && sonDe(type) != null

    // ── Synthèse de repli (clé sans fichier) ──

    private fun fichierSynthese(ctx: Context, son: Son): File {
        val dossier = File(ctx.cacheDir, "sons_combat").apply { mkdirs() }
        val f = File(dossier, "${son.cle}_v$VERSION_SYNTHESE.wav")
        if (!f.exists() || f.length() == 0L) ecrireWav(f, synthese(son))
        return f
    }

    private fun ecrireWav(f: File, pcm: ShortArray) {
        RandomAccessFile(f, "rw").use { out ->
            out.setLength(0)
            val octets = pcm.size * 2
            fun i32(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))
            fun i16(v: Int) = out.write(byteArrayOf(v.toByte(), (v shr 8).toByte()))
            out.writeBytes("RIFF"); i32(36 + octets); out.writeBytes("WAVEfmt ")
            i32(16); i16(1); i16(1); i32(TAUX); i32(TAUX * 2); i16(2); i16(16)
            out.writeBytes("data"); i32(octets)
            val tampon = ByteArray(octets)
            pcm.forEachIndexed { i, s -> tampon[2 * i] = s.toInt().toByte(); tampon[2 * i + 1] = (s.toInt() shr 8).toByte() }
            out.write(tampon)
        }
    }

    private fun echantillons(ms: Int) = ms * TAUX / 1000

    /** Note de [freq] Hz : attaque courte, extinction exponentielle, partiels ([rapports] × freq, [amplitudes]). */
    private fun note(
        freq: Double, ms: Int, extinction: Double = 6.0,
        rapports: List<Double> = listOf(1.0, 2.0, 3.0), amplitudes: List<Double> = listOf(1.0, 0.35, 0.12),
        glissando: Double = 0.0, attaqueMs: Int = 4,
    ): FloatArray {
        val n = echantillons(ms)
        val attaque = echantillons(attaqueMs).coerceAtLeast(1)
        val phases = DoubleArray(rapports.size)
        val somme = amplitudes.sum()
        return FloatArray(n) { i ->
            val t = i.toDouble() / n
            val f = freq * (1 + glissando * t)
            var v = 0.0
            rapports.forEachIndexed { k, r ->
                phases[k] += 2 * PI * f * r / TAUX
                // Les partiels aigus s'éteignent plus vite : timbre plus naturel.
                v += amplitudes[k] * sin(phases[k]) * exp(-extinction * k * 0.6 * t)
            }
            val env = (if (i < attaque) i.toDouble() / attaque else 1.0) * exp(-extinction * t)
            (v / somme * env).toFloat()
        }
    }

    /** Coup sourd : sinus dont la hauteur chute vite (corps d'un impact). */
    private fun coupSourd(ms: Int, depart: Double, arrivee: Double, extinction: Double = 9.0): FloatArray {
        val n = echantillons(ms)
        var phase = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / n
            phase += 2 * PI * (arrivee + (depart - arrivee) * exp(-t * 18)) / TAUX
            (sin(phase) * exp(-extinction * t)).toFloat()
        }
    }

    /** Bruit filtré passe-bande ([centre] Hz) : souffle, frottement, éclat. */
    private fun bruit(ms: Int, centre: Double, extinction: Double = 8.0, montee: Boolean = false, graine: Int = ms): FloatArray {
        val n = echantillons(ms)
        val alea = Random(graine)
        // Filtre d'état variable : passe-bande résonant.
        val f = 2 * sin(PI * centre / TAUX)
        val q = 0.7
        var bas = 0.0
        var bande = 0.0
        return FloatArray(n) { i ->
            val t = i.toDouble() / n
            val entree = alea.nextDouble() * 2 - 1
            bas += f * bande
            val haut = entree - bas - q * bande
            bande += f * haut
            val env = if (montee) sin(PI * t).let { it * it } else exp(-extinction * t)
            (bande * env).toFloat()
        }
    }

    private fun suite(vararg parties: FloatArray): FloatArray = parties.reduce { a, b -> a + b }

    private fun melange(vararg parties: Pair<FloatArray, Int>, gains: List<Float>? = null): FloatArray {
        val n = parties.maxOf { (p, decalage) -> p.size + echantillons(decalage) }
        val sortie = FloatArray(n)
        parties.forEachIndexed { k, (p, decalage) ->
            val g = gains?.getOrNull(k) ?: 1f
            val d = echantillons(decalage)
            p.forEachIndexed { i, v -> sortie[i + d] += v * g }
        }
        return sortie
    }

    /** Petite réverbération (peignes en parallèle + passe-tout) : place le son dans une salle. */
    private fun reverb(x: FloatArray, mix: Float, queueMs: Int = 600): FloatArray {
        val n = x.size + echantillons(queueMs)
        val entree = x.copyOf(n)
        val sortie = FloatArray(n)
        listOf(1557 to 0.80f, 1617 to 0.79f, 1491 to 0.78f, 1422 to 0.77f).forEach { (retard, gain) ->
            val tampon = FloatArray(n)
            for (i in 0 until n) {
                tampon[i] = entree[i] + if (i >= retard) tampon[i - retard] * gain else 0f
                sortie[i] += tampon[i] * 0.25f
            }
        }
        val diffus = FloatArray(n)
        val retard = 225
        for (i in 0 until n) {
            val retarde = if (i >= retard) diffus[i - retard] else 0f
            diffus[i] = sortie[i] + retarde * 0.5f
            sortie[i] = retarde - diffus[i] * 0.5f
        }
        return FloatArray(n) { i -> entree[i] * (1 - mix) + sortie[i] * mix }
    }

    private fun synthese(son: Son): ShortArray {
        val metal = listOf(1.0, 2.76, 5.40, 8.93)
        val ametal = listOf(1.0, 0.6, 0.35, 0.2)
        val (signal, crete, mix) = when (son) {
            Son.TOUCHE -> Triple(
                melange(coupSourd(260, 180.0, 55.0) to 0, bruit(90, 2500.0, 25.0) to 0, note(620.0, 380, 9.0, metal, ametal) to 5, gains = listOf(1f, 0.8f, 0.25f)),
                0.85f, 0.12f,
            )
            Son.CRITIQUE -> Triple(
                melange(
                    coupSourd(420, 220.0, 45.0, 6.0) to 0, bruit(140, 3000.0, 18.0) to 0,
                    note(880.0, 900, 4.5, metal, ametal) to 10, note(1320.0, 1100, 4.0, metal, ametal) to 60,
                    gains = listOf(1f, 0.9f, 0.35f, 0.25f),
                ),
                0.95f, 0.25f,
            )
            Son.RATE -> Triple(bruit(340, 900.0, montee = true), 0.4f, 0.08f)
            Son.KO -> Triple(
                melange(coupSourd(500, 140.0, 40.0, 5.0) to 0, suite(note(330.0, 200, 3.0), note(262.0, 200, 3.0), note(196.0, 700, 3.5, glissando = -0.25)) to 80, gains = listOf(1f, 0.5f)),
                0.75f, 0.25f,
            )
            Son.MORT -> Triple(
                melange(coupSourd(900, 120.0, 32.0, 3.5) to 0, note(73.0, 1900, 2.2, listOf(1.0, 2.0, 3.0, 4.0), listOf(1.0, 0.5, 0.3, 0.15)) to 0, note(110.0, 1600, 2.8) to 350, gains = listOf(1f, 0.6f, 0.35f)),
                0.85f, 0.3f,
            )
            Son.SOIN -> Triple(
                melange(*listOf(523.0, 659.0, 784.0, 1046.0).mapIndexed { i, f -> note(f, 700, 3.5, listOf(1.0, 2.0), listOf(1.0, 0.15), attaqueMs = 25) to i * 80 }.toTypedArray()),
                0.5f, 0.35f,
            )
            Son.ETAT -> Triple(melange(note(220.0, 600, 3.0, listOf(1.0, 1.5), listOf(1.0, 0.4), glissando = 0.5, attaqueMs = 40) to 0, bruit(500, 1800.0, montee = true) to 0, gains = listOf(1f, 0.4f)), 0.45f, 0.3f)
            Son.ROUND -> Triple(
                melange(coupSourd(1200, 90.0, 48.0, 3.0) to 0, note(98.0, 1400, 3.0, listOf(1.0, 2.0, 3.0, 4.0), listOf(1.0, 0.6, 0.45, 0.3)) to 0, bruit(300, 400.0, 12.0) to 0, gains = listOf(1f, 0.6f, 0.4f)),
                0.8f, 0.3f,
            )
            Son.TOUR -> Triple(note(740.0, 180, 7.0, listOf(1.0, 2.0), listOf(1.0, 0.2)), 0.35f, 0.15f)
            Son.MON_TOUR -> Triple(
                melange(note(659.0, 260, 4.0) to 0, note(880.0, 280, 4.0) to 120, note(1318.0, 500, 4.5) to 240),
                0.6f, 0.25f,
            )
            Son.BOSS -> Triple(
                melange(coupSourd(1600, 70.0, 36.0, 2.0) to 0, note(55.0, 1800, 2.2, listOf(1.0, 2.0, 3.0, 4.0), listOf(1.0, 0.7, 0.5, 0.3)) to 0, bruit(1400, 250.0, montee = true) to 0, gains = listOf(1f, 0.7f, 0.5f)),
                0.9f, 0.3f,
            )
            Son.VICTOIRE -> Triple(
                suite(
                    note(523.0, 160, 3.0), note(659.0, 160, 3.0), note(784.0, 160, 3.0),
                    melange(note(1046.0, 1100, 2.5) to 0, note(784.0, 1100, 2.5) to 0, note(659.0, 1100, 2.5) to 0),
                ),
                0.65f, 0.3f,
            )
        }
        val avecSalle = reverb(signal, mix)
        // Fondu final : pas de clic quand le son s'arrête.
        val fondu = echantillons(20)
        for (i in 0 until fondu.coerceAtMost(avecSalle.size)) avecSalle[avecSalle.size - 1 - i] *= i.toFloat() / fondu
        val max = avecSalle.maxOfOrNull { abs(it) }?.takeIf { it > 0f } ?: return ShortArray(0)
        return ShortArray(avecSalle.size) { i -> (avecSalle[i] / max * crete * Short.MAX_VALUE).toInt().toShort() }
    }
}
