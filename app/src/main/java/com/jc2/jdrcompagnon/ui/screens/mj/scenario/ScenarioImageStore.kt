package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Copie l'image de lieu choisie par le MJ pour un scénario dans le stockage interne de l'app,
 * même principe que CarteImageStore (fonds de carte) : on copie le contenu plutôt que de garder
 * l'URI "content://" d'origine, dont l'autorisation d'accès n'est pas garantie de survivre au
 * redémarrage de l'app.
 */
object ScenarioImageStore {

    private fun dossier(context: Context): File =
        File(context.filesDir, "scenario_images").apply { mkdirs() }

    /** Retourne le nom du fichier copié (à stocker dans MjScenario.lieuImageFileName), ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri, scenarioId: String): String? {
        val fileName = "$scenarioId.img"
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

    /** Supprime les images importées avec un scénario ({mimage:}, préfixées par son id). */
    fun supprimerImagesDe(context: Context, scenarioId: String) {
        dossier(context).listFiles { f -> f.name.startsWith("${scenarioId}_") }?.forEach { it.delete() }
    }

    /** Écrit une image embarquée dans un scénario importé (balise {mimage:}) ; retourne son nom de fichier. */
    fun ecrire(context: Context, fileName: String, bytes: ByteArray): String {
        File(dossier(context), fileName).writeBytes(bytes)
        return fileName
    }
}
