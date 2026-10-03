package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Image d'une campagne (MjCampaign.imageFileName), même principe que ScenarioImageStore /
 * CarteImageStore : le contenu choisi est copié en stockage interne plutôt que de garder l'URI
 * "content://" d'origine. Le nom de fichier change à chaque choix ("<id>_<horodatage>.img") pour
 * que les aperçus mis en cache se rafraîchissent, et pour pouvoir annuler l'édition sans perdre
 * l'image enregistrée : les anciennes versions ne sont nettoyées qu'à l'enregistrement.
 */
object CampagneImageStore {

    private fun dossier(context: Context): File =
        File(context.filesDir, "campagne_images").apply { mkdirs() }

    fun fichier(context: Context, fileName: String): File = File(dossier(context), fileName)

    /** Retourne le nom du fichier copié, ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri, campagneId: String): String? {
        val fileName = "${campagneId}_${System.currentTimeMillis()}.img"
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                fichier(context, fileName).outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            fileName
        } catch (e: Exception) {
            null
        }
    }

    /** Écrit une image déjà lue en mémoire (import de campagne) ; retourne son nom de fichier. */
    fun ecrire(context: Context, campagneId: String, bytes: ByteArray): String {
        val fileName = "${campagneId}_${System.currentTimeMillis()}.img"
        fichier(context, fileName).writeBytes(bytes)
        return fileName
    }

    /** Supprime les images de la campagne, sauf [garder] (celle qui vient d'être enregistrée). */
    fun nettoyer(context: Context, campagneId: String, garder: String? = null) {
        dossier(context).listFiles { f -> f.name.startsWith("${campagneId}_") && f.name != garder }
            ?.forEach { it.delete() }
    }
}

/**
 * Décode une image de fichier hors du thread UI, sous-échantillonnée pour rester légère (fonds de
 * carte en haute résolution notamment). Null tant que le décodage n'est pas terminé ou s'il échoue.
 */
@Composable
fun rememberImageFichier(file: File?, maxDimension: Int = 1600): ImageBitmap? {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file?.absolutePath, file?.lastModified()) {
        value = if (file == null) null else withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) return@runCatching null
                val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bornes)
                var sample = 1
                while (maxOf(bornes.outWidth, bornes.outHeight) / (sample * 2) >= maxDimension) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

/** Raccourci : image de la campagne, ou null si elle n'en a pas. */
@Composable
fun rememberImageCampagne(campagne: GameState.MjCampaign?): ImageBitmap? {
    val context = LocalContext.current
    return rememberImageFichier(campagne?.imageFileName?.let { CampagneImageStore.fichier(context, it) })
}
