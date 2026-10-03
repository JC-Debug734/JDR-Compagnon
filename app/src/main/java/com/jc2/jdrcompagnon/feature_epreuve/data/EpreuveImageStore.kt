package com.jc2.jdrcompagnon.feature_epreuve.data

import android.content.Context
import android.net.Uri
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironmentImageStore
import java.io.File

/**
 * Image d'une épreuve, copiée dans le stockage interne (même principe que EnvironmentImageStore :
 * l'URI "content://" d'origine ne survit pas forcément au redémarrage). Le nom du fichier change à
 * chaque copie pour invalider les caches `remember(fileName)` et celui du navigateur de la table.
 *
 * Une copie ne supprime jamais l'image précédente : l'éditeur peut être annulé, c'est lui qui
 * fait le ménage (voir EpreuveEditorDialog).
 */
object EpreuveImageStore {

    private fun dossier(context: Context): File =
        File(context.filesDir, "epreuve_images").apply { mkdirs() }

    fun fichier(context: Context, fileName: String): File = File(dossier(context), fileName)

    /** Retourne le nom du fichier copié (à stocker dans Epreuve.imageFileName), ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri, epreuveId: String): String? =
        ecrire(context, epreuveId) { context.contentResolver.openInputStream(uri) }

    /** Image de la galerie d'environnements fournie avec l'app (assets/dnd/environnements). */
    fun copierDepuisAsset(context: Context, assetName: String, epreuveId: String): String? =
        ecrire(context, epreuveId) { context.assets.open("${EnvironmentImageStore.DOSSIER_ASSETS}/$assetName") }

    fun supprimer(context: Context, fileName: String?) {
        if (fileName != null) fichier(context, fileName).delete()
    }

    private fun ecrire(context: Context, epreuveId: String, ouvrir: () -> java.io.InputStream?): String? {
        val fileName = "${epreuveId}_${System.currentTimeMillis()}.img"
        val target = fichier(context, fileName)
        return try {
            ouvrir()?.use { input -> target.outputStream().use { output -> input.copyTo(output) } } ?: return null
            fileName
        } catch (e: Exception) {
            target.delete()
            null
        }
    }
}
