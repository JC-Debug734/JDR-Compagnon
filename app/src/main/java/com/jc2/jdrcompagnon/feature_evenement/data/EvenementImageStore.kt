package com.jc2.jdrcompagnon.feature_evenement.data

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Illustrations des événements (Evenement.imageFileName), copiées dans le stockage interne de
 * l'app comme ScenarioImageStore : une par événement, nommée d'après son id (choisie dans l'outil
 * ÉVÉNEMENTS, apportée par une campagne importée ou par un exemple de l'app).
 */
object EvenementImageStore {

    private fun dossier(context: Context): File =
        File(context.filesDir, "evenement_images").apply { mkdirs() }

    private fun nomPour(evenementId: String) = "$evenementId.img"

    fun fichier(context: Context, fileName: String): File = File(dossier(context), fileName)

    /** Copie une image choisie par le MJ ; retourne le nom du fichier, ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri, evenementId: String): String? = runCatching {
        val fileName = nomPour(evenementId)
        context.contentResolver.openInputStream(uri)?.use { input ->
            fichier(context, fileName).outputStream().use { input.copyTo(it) }
        } ?: return null
        fileName
    }.getOrNull()

    /** Écrit une image lue dans une archive importée (campagne.json) ; retourne le nom du fichier. */
    fun ecrire(context: Context, evenementId: String, bytes: ByteArray): String? = runCatching {
        nomPour(evenementId).also { fichier(context, it).writeBytes(bytes) }
    }.getOrNull()

    /** Copie une image des assets de l'app (illustration d'un événement d'exemple) ; retourne le nom du fichier. */
    fun copierAsset(context: Context, assetPath: String, evenementId: String): String? = runCatching {
        nomPour(evenementId).also { nom ->
            context.assets.open(assetPath).use { input -> fichier(context, nom).outputStream().use { input.copyTo(it) } }
        }
    }.getOrNull()

    fun supprimer(context: Context, fileName: String) {
        fichier(context, fileName).delete()
    }
}
