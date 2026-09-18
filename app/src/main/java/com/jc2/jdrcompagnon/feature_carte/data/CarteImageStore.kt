package com.jc2.jdrcompagnon.feature_carte.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copie l'image de fond choisie par le MJ dans le stockage interne de l'app, même principe que
 * copyPickedFileToInternalStorage dans LibraryScreen.kt (livres personnalisés) : on copie le
 * contenu plutôt que de garder l'URI "content://" d'origine, dont l'autorisation d'accès n'est
 * pas garantie de survivre au redémarrage de l'app.
 */
object CarteImageStore {

    private fun dossier(context: Context): File =
        File(context.filesDir, "carte_images").apply { mkdirs() }

    /** Retourne le nom du fichier copié (à passer à CarteCampagneViewModel.onDefinirImageFond), ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri, campagneId: String): String? {
        val fileName = "$campagneId.img"
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
}
