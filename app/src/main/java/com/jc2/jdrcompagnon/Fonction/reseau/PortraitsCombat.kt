package com.jc2.jdrcompagnon.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.FilePortraits
import com.jc2.jdrcompagnon.ui.components.PnjPortraits
import com.jc2.jdrcompagnon.ui.screens.joueur.characterPortraitOptions
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterImages
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Portraits des combattants pour la page table (MjWebServer, panneau de combat) :
 * - personnage : son portrait (image intégrée, portrait PNJ des assets, ou photo importée) ;
 * - monstre : l'image choisie par le MJ, celle de son livre importé, ou celle du bestiaire.
 * Réduits à [COTE_PX] px en JPEG et mis en cache (même portrait, même octets) ; null s'il n'y en a pas.
 */
object PortraitsCombat {

    private const val COTE_PX = 256

    // Clé (personnage + portrait, ou nom de monstre) → image ; tableau vide = aucune image.
    private val cache = ConcurrentHashMap<String, ByteArray>()

    private fun cle(c: Combattant): String? {
        val perso = c.characterId?.let { id -> GameState.characters.value.firstOrNull { it.id == id } }
        val imageBoss = c.bossId?.let { com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue.parId(it) }
            ?.let { com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue.imageDe(it) }
        return when {
            perso != null -> perso.portrait.takeIf { it.isNotBlank() }?.let { "perso:$it" }
            // Boss illustré (Cryovain…) : son illustration plutôt que l'image du monstre de base.
            imageBoss != null -> "asset:$imageBoss"
            // Image du monstre importée ou retirée par le MJ : nouvelle clé, nouvelle image.
            else -> "monstre:${MonsterImages.version.value}:${c.monstreNom ?: c.nom}"
        }
    }

    /** Version du portrait de [c] (change avec l'image), ou null s'il n'en a pas. À appeler hors du thread UI. */
    fun version(context: Context, c: Combattant): Int? {
        val cle = cle(c) ?: return null
        val octets = cache.getOrPut(cle) { charger(context, cle) ?: ByteArray(0) }
        return octets.takeIf { it.isNotEmpty() }?.let { cle.hashCode() xor it.size }
    }

    /** Image JPEG du portrait de [c], ou null. */
    fun jpeg(context: Context, c: Combattant): ByteArray? {
        version(context, c) ?: return null
        return cle(c)?.let { cache[it] }?.takeIf { it.isNotEmpty() }
    }

    private fun charger(context: Context, cle: String): ByteArray? = runCatching {
        val bitmap = when {
            cle.startsWith("asset:") -> cle.removePrefix("asset:").let { chemin ->
                // Image du boss choisie par le MJ (chemin absolu) ou livrée avec l'appli (asset).
                if (chemin.startsWith("/")) decoder(File(chemin))
                else context.assets.open(chemin).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = 2 }) }
            }
            cle.startsWith("perso:") -> {
                val portrait = cle.removePrefix("perso:")
                when {
                    FilePortraits.isFilePortrait(portrait) -> decoder(FilePortraits.file(portrait))
                    PnjPortraits.isPnjPortrait(portrait) ->
                        context.assets.open(PnjPortraits.assetPath(portrait)).use { BitmapFactory.decodeStream(it) }
                    else -> characterPortraitOptions.firstOrNull { it.id == portrait }
                        ?.let { BitmapFactory.decodeResource(context.resources, it.resId) }
                }
            }
            else -> {
                val nom = cle.removePrefix("monstre:").substringAfter(':')
                val fiche = runBlocking { SrdRepository.getMonsterByName(context, nom, GameState.currentWorldId()) } ?: return null
                MonsterImages.fichierImporte(context, fiche)?.let(::decoder)
                    ?: fiche.image?.takeIf { it.startsWith("/") }?.let { File(it) }?.takeIf { it.isFile }?.let(::decoder)
                    ?: MonsterImages.assetPathFor(context, fiche)?.let { chemin ->
                        runCatching { context.assets.open(chemin).use { BitmapFactory.decodeStream(it) } }.getOrNull()
                    }
            }
        } ?: return null
        val echelle = COTE_PX.toFloat() / maxOf(bitmap.width, bitmap.height)
        val reduit = if (echelle < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * echelle).toInt().coerceAtLeast(1), (bitmap.height * echelle).toInt().coerceAtLeast(1), true)
        } else bitmap
        ByteArrayOutputStream().use { sortie ->
            reduit.compress(Bitmap.CompressFormat.JPEG, 85, sortie)
            sortie.toByteArray()
        }
    }.getOrNull()

    /** Décode un fichier en réduisant d'abord les très grandes photos. */
    private fun decoder(fichier: File): Bitmap? {
        if (!fichier.isFile) return null
        val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(fichier.absolutePath, bornes)
        var echantillon = 1
        while (maxOf(bornes.outWidth, bornes.outHeight) / echantillon > COTE_PX * 4) echantillon *= 2
        return BitmapFactory.decodeFile(fichier.absolutePath, BitmapFactory.Options().apply { inSampleSize = echantillon })
    }
}
