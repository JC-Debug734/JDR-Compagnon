package com.jc2.jdrcompagnon.feature_environnement.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copie l'image choisie par le MJ pour un environnement dans le stockage interne de l'app, même
 * principe que ScenarioImageStore/CarteImageStore : on copie le contenu plutôt que de garder
 * l'URI "content://" d'origine, dont l'autorisation d'accès n'est pas garantie de survivre au
 * redémarrage de l'app.
 */
object EnvironmentImageStore {

    private fun dossier(context: Context): File =
        File(context.filesDir, "environment_images").apply { mkdirs() }

    /** Retourne le nom du fichier copié (à stocker dans Environnement.imageFileName), ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri, environnementId: String): String? {
        val fileName = "$environnementId.img"
        val target = File(dossier(context), fileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            fileName
        } catch (e: Exception) {
            null
        }
    }

    fun fichier(context: Context, fileName: String): File = File(dossier(context), fileName)

    /** Images d'environnement fournies avec l'app (assets/dnd/environnements), proposées en galerie. */
    const val DOSSIER_ASSETS = "dnd/environnements"

    fun imagesPredefinies(context: Context): List<String> =
        runCatching { context.assets.list(DOSSIER_ASSETS)?.sorted().orEmpty() }.getOrDefault(emptyList())

    /**
     * Copie une image prédéfinie dans le stockage interne, comme [copier]. Le nom du fichier
     * inclut l'image source pour qu'un changement d'image invalide les caches `remember(fileName)`.
     */
    fun copierDepuisAsset(context: Context, assetName: String, environnementId: String, ancienFichier: String?): String? {
        val fileName = "${environnementId}_${assetName.substringBeforeLast('.')}.img"
        val target = File(dossier(context), fileName)
        return try {
            context.assets.open("$DOSSIER_ASSETS/$assetName").use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            if (ancienFichier != null && ancienFichier != fileName) File(dossier(context), ancienFichier).delete()
            fileName
        } catch (e: Exception) {
            null
        }
    }
}
