package com.jc2.jdrcompagnon.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.jc2.jdrcompagnon.ui.screens.joueur.characterPortraitOptions
import java.util.concurrent.ConcurrentHashMap

/**
 * Portraits réservés aux PNJ : images du dossier assets/dnd/PNJ, lues dynamiquement (ajouter une
 * image au dossier suffit à la proposer). Leur id de portrait est "pnj:<nom du fichier>" pour
 * cohabiter avec les ids des portraits de PJ (characterPortraitOptions, drawables). Les deux
 * appareils (MJ et joueur) ayant le même APK, l'id suffit sur le réseau.
 */
object PnjPortraits {
    const val ASSET_DIR = "dnd/PNJ"
    const val ID_PREFIX = "pnj:"

    private val imageExtensions = setOf("png", "jpg", "jpeg", "webp")
    private var cachedIds: List<String>? = null
    private val bitmaps = ConcurrentHashMap<String, ImageBitmap>()

    fun isPnjPortrait(portraitId: String?): Boolean = portraitId?.startsWith(ID_PREFIX) == true

    fun assetPath(portraitId: String): String = "$ASSET_DIR/${portraitId.removePrefix(ID_PREFIX)}"

    /** Ids de tous les portraits PNJ disponibles, triés par nom de fichier. */
    fun ids(context: Context): List<String> = cachedIds ?: run {
        val files = runCatching { context.assets.list(ASSET_DIR)?.toList() }.getOrNull().orEmpty()
        files.filter { it.substringAfterLast('.').lowercase() in imageExtensions }
            .sortedBy { it.lowercase() }
            .map { ID_PREFIX + it }
            .also { cachedIds = it }
    }

    /**
     * Portraits d'un sexe donné en premier (suffixe _f / _m / _h du nom de fichier), puis les
     * autres. Si [especeKey] est fourni (ex. "tieffelin"), les portraits dont le nom de fichier
     * le contient passent en tête de chaque groupe.
     */
    fun idsFor(context: Context, feminin: Boolean, especeKey: String? = null): List<String> {
        val (femmes, autres) = ids(context).partition { nomSansCopie(it).endsWith("_f") }
        // [especeKey] peut lister plusieurs orthographes séparées par "|" ("tieffelin|tiefelin").
        val keys = especeKey?.lowercase()?.split("|")?.map { it.trim() }?.filter { it.isNotBlank() }.orEmpty()
        fun List<String>.especeEnTete() =
            if (keys.isEmpty()) this else sortedByDescending { id -> keys.any { it in id.lowercase() } }
        return if (feminin) femmes.especeEnTete() + autres.especeEnTete()
        else autres.especeEnTete() + femmes.especeEnTete()
    }

    /** Libellé lisible : "pnj_guerrier_f.png" → "Guerrier (F)". */
    /** Nom de fichier sans extension, en minuscules, sans suffixe de copie (" (1)"). */
    private fun nomSansCopie(portraitId: String): String =
        portraitId.removePrefix(ID_PREFIX).substringBeforeLast('.').lowercase()
            .replace(Regex("""\s*\(\d+\)$"""), "")

    fun label(portraitId: String): String {
        val base = nomSansCopie(portraitId).removePrefix("pnj_")
        val sexe = when {
            base.endsWith("_f") -> " (F)"
            base.endsWith("_m") || base.endsWith("_h") -> " (H)"
            else -> ""
        }
        val nom = base.removeSuffix("_f").removeSuffix("_m").removeSuffix("_h").replace('_', ' ')
        return nom.replaceFirstChar { it.uppercase() } + sexe
    }

    /** Portrait PNJ déterministe pour un nom (attribution automatique à la création). */
    fun defaultFor(context: Context, name: String): String? =
        ids(context).takeIf { it.isNotEmpty() }?.let { it[Math.floorMod(name.hashCode(), it.size)] }

    fun bitmap(context: Context, portraitId: String): ImageBitmap? = bitmaps[portraitId] ?: runCatching {
        context.assets.open(assetPath(portraitId)).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
    }.getOrNull()?.also { bitmaps[portraitId] = it }
}

/**
 * Portraits apportés par un import (fiche PNJ d'un livre, voir PnjImport) : image copiée dans
 * le stockage interne de l'app, id "fichier:<chemin absolu>". Propre à cet appareil — un joueur
 * connecté qui reçoit la fiche n'a pas le fichier et voit la silhouette de repli.
 */
object FilePortraits {
    const val ID_PREFIX = "fichier:"
    private const val MAX_SIDE_PX = 1024
    private val bitmaps = ConcurrentHashMap<String, ImageBitmap>()

    fun isFilePortrait(portraitId: String?): Boolean = portraitId?.startsWith(ID_PREFIX) == true

    fun file(portraitId: String): java.io.File = java.io.File(portraitId.removePrefix(ID_PREFIX))

    fun idFor(file: java.io.File): String = ID_PREFIX + file.absolutePath

    fun bitmap(portraitId: String): ImageBitmap? = bitmaps[portraitId] ?: runCatching {
        val path = file(portraitId).absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
    }.getOrNull()?.also { bitmaps[portraitId] = it }
}

/**
 * Painter du portrait d'un personnage, qu'il s'agisse d'un portrait de PJ (drawable) ou de PNJ
 * (assets/dnd/PNJ). Null si l'id est vide ou inconnu.
 */
@Composable
fun rememberCharacterPortraitPainter(portraitId: String?): Painter? {
    if (portraitId.isNullOrBlank()) return null
    if (PnjPortraits.isPnjPortrait(portraitId)) {
        val context = LocalContext.current
        val bitmap = remember(portraitId) { PnjPortraits.bitmap(context, portraitId) } ?: return null
        return remember(bitmap) { BitmapPainter(bitmap) }
    }
    if (FilePortraits.isFilePortrait(portraitId)) {
        val bitmap = remember(portraitId) { FilePortraits.bitmap(portraitId) } ?: return null
        return remember(bitmap) { BitmapPainter(bitmap) }
    }
    val resId = characterPortraitOptions.find { it.id == portraitId }?.resId ?: return null
    return painterResource(id = resId)
}
