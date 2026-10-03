package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

/**
 * Images des monstres. Priorité à l'image importée par le MJ depuis son téléphone (bouton
 * "Choisir une image" de la fiche du monstre, stockée hors APK), sinon celle des assets,
 * rangée dans assets/dnd/monstres/. Deux façons d'associer une image d'assets :
 *
 * 1. Explicite, dans monster_srd521.md, par une ligne de champ du bloc du monstre :
 *      Image: gobelin.png
 *    (chemin relatif à dnd/monstres/ ; un chemin contenant déjà un "/" est pris depuis la racine
 *    des assets ; la syntaxe markdown `![Gobelin](gobelin.png)` est aussi acceptée).
 * 2. Automatique, sans toucher au .md : un fichier nommé d'après le monstre, en minuscules,
 *    sans accents, espaces et apostrophes remplacés par "_" — ex. "Dragon rouge adulte" →
 *    dragon_rouge_adulte.png (png, jpg, jpeg ou webp).
 *
 * Les monstres d'un livre .zip ou d'un univers importé peuvent aussi apporter leurs propres
 * images, résolues en chemin absolu par [CustomBookImages] et décodées ici en priorité.
 */
object MonsterImages {
    const val ASSET_DIR = "dnd/monstres"
    private const val IMPORT_DIR = "monster_images"
    private const val MAX_SIDE_PX = 1280

    private val extensions = listOf("png", "jpg", "jpeg", "webp")
    private val markdownImage = Regex("""!\[[^\]]*]\(([^)]+)\)""")
    private var files: Set<String>? = null
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    // Incrémenté à chaque import/retrait : relance le chargement des MonsterImage affichées.
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    // ── Images importées par le MJ depuis son téléphone ──
    // Prioritaires sur celles des assets, rangées dans le stockage interne de l'app (hors APK),
    // et rattachées au nom du monstre : valables aussi pour les monstres des livres
    // personnalisés, qui passent par le même SrdEntry.

    private fun importFile(context: Context, monster: SrdEntry): File =
        File(File(context.filesDir, IMPORT_DIR).apply { mkdirs() }, "${slug(monster.name)}.img")

    fun hasImport(context: Context, monster: SrdEntry): Boolean = importFile(context, monster).exists()

    /** Image choisie par le MJ pour ce monstre (stockage interne), null s'il n'en a pas choisi. */
    fun fichierImporte(context: Context, monster: SrdEntry): File? = importFile(context, monster).takeIf { it.exists() }

    /** Copie l'image choisie (uri content://) pour ce monstre ; false en cas d'échec. */
    fun importer(context: Context, uri: Uri, monster: SrdEntry): Boolean {
        val target = importFile(context, monster)
        val ok = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (ok) invalidate(target.absolutePath)
        return ok
    }

    /** Retire l'image importée : le monstre retombe sur celle des assets, s'il en a une. */
    fun supprimerImport(context: Context, monster: SrdEntry) {
        val target = importFile(context, monster)
        if (target.delete()) invalidate(target.absolutePath)
    }

    private fun invalidate(path: String) {
        cache.remove(path)
        missing.remove(path)
        _version.value++
    }

    /** Décode en réduisant les grosses photos (appareil photo) pour éviter un OutOfMemory. */
    private fun decodeSampled(file: File): ImageBitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE_PX) sample *= 2
        return BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            ?.asImageBitmap()
    }

    /** "Dragon rouge adulte" → "dragon_rouge_adulte". */
    fun slug(name: String): String =
        Normalizer.normalize(name.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')

    /** Chemin d'asset de l'image du monstre, ou null s'il n'en a pas. */
    fun assetPathFor(context: Context, monster: SrdEntry): String? {
        monster.image?.let { declared ->
            val path = (markdownImage.find(declared)?.groupValues?.get(1) ?: declared).trim()
            return if ('/' in path) path.removePrefix("/") else "$ASSET_DIR/$path"
        }
        val available = files ?: runCatching { context.assets.list(ASSET_DIR)?.toSet() }.getOrNull().orEmpty()
            .also { files = it }
        val base = slug(monster.name)
        return extensions.map { "$base.$it" }.firstOrNull { it in available }?.let { "$ASSET_DIR/$it" }
    }

    /** Charge l'image du monstre (mise en cache), null si aucune. À appeler hors thread principal. */
    fun load(context: Context, monster: SrdEntry): ImageBitmap? {
        val imported = importFile(context, monster)
        if (imported.exists()) {
            val key = imported.absolutePath
            cache[key]?.let { return it }
            decodeSampled(imported)?.let { cache[key] = it; return it }
        }
        // Image livrée avec un livre .zip ou un univers importé (voir CustomBookImages).
        monster.image?.takeIf { it.startsWith("/") }?.let(::File)?.takeIf { it.isFile }?.let { file ->
            val key = file.absolutePath
            cache[key]?.let { return it }
            decodeSampled(file)?.let { cache[key] = it; return it }
        }
        val path = assetPathFor(context, monster) ?: return null
        cache[path]?.let { return it }
        if (path in missing) return null
        val bitmap = runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
        if (bitmap != null) cache[path] = bitmap else missing += path
        return bitmap
    }
}

/** Image du monstre en en-tête de sa fiche ; n'affiche rien s'il n'en a pas. */
@Composable
fun MonsterImage(monster: SrdEntry, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version by MonsterImages.version.collectAsState()
    val bitmap by produceState<ImageBitmap?>(initialValue = null, monster.name, monster.image, version) {
        value = withContext(Dispatchers.IO) { MonsterImages.load(context, monster) }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = monster.name,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
}
