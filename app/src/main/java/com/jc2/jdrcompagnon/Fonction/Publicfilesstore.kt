package com.jc2.jdrcompagnon.ui

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/**
 * Accès générique au dossier public Téléchargements/JDRCompagnon/, utilisé à la fois
 * par GameState (fichiers .md des scénarios) et SrdRepository (bestiaire, sorts,
 * équipement, règles). Centralisé ici pour ne pas dupliquer la logique MediaStore.
 *
 * Depuis Android 10, on ne peut plus écrire directement dans un dossier public
 * arbitraire avec un simple chemin de fichier — MediaStore.Downloads est le seul
 * moyen d'accéder à un dossier public "facile d'accès" (visible dans n'importe quel
 * gestionnaire de fichiers, sous Téléchargements) sans permission de stockage.
 */
object PublicFilesStore {

    private const val ROOT = "JDRCompagnon"

    data class FileEntry(val name: String, val uri: Uri, val sizeBytes: Long)

    private fun relativePath(subfolder: String?): String =
        if (subfolder.isNullOrBlank()) "Download/$ROOT/" else "Download/$ROOT/${subfolder.trim('/')}/"

    /** Chemin (pour affichage à l'utilisateur) du sous-dossier donné. */
    fun directoryLabel(subfolder: String? = null): String =
        if (subfolder.isNullOrBlank()) "Téléchargements/$ROOT" else "Téléchargements/$ROOT/${subfolder.trim('/')}"

    /** Liste les fichiers d'une extension donnée dans le sous-dossier, triés par nom. */
    fun list(context: Context, subfolder: String? = null, extension: String = "md"): List<FileEntry> {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE)
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(relativePath(subfolder))
        val result = mutableListOf<FileEntry>()
        try {
            resolver.query(collection, projection, selection, args, "${MediaStore.Downloads.DISPLAY_NAME} ASC")?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol)
                    if (!name.endsWith(".$extension", ignoreCase = true)) continue
                    val id = cursor.getLong(idCol)
                    val size = cursor.getLong(sizeCol)
                    result += FileEntry(name, ContentUris.withAppendedId(collection, id), size)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PublicFilesStore", "Impossible de lister $subfolder", e)
        }
        return result
    }

    fun findUri(context: Context, displayName: String, subfolder: String? = null): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
        val args = arrayOf(displayName, relativePath(subfolder))
        try {
            resolver.query(collection, projection, selection, args, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                    return ContentUris.withAppendedId(collection, id)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PublicFilesStore", "Impossible de chercher $displayName", e)
        }
        return null
    }

    /** Lit le contenu texte d'un fichier du dossier public, ou null s'il n'existe pas encore. */
    fun readText(context: Context, displayName: String, subfolder: String? = null): String? {
        val uri = findUri(context, displayName, subfolder) ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            android.util.Log.e("PublicFilesStore", "Impossible de lire $displayName", e)
            null
        }
    }

    /** Écrit (ou réécrit intégralement) un fichier texte dans le dossier public. */
    fun writeText(context: Context, displayName: String, content: String, subfolder: String? = null, mimeType: String = "text/markdown") {
        try {
            val resolver = context.contentResolver
            val existingUri = findUri(context, displayName, subfolder)
            val uri = existingUri ?: run {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, relativePath(subfolder))
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            }
            if (uri != null) {
                resolver.openOutputStream(uri, "wt")?.use { out -> out.write(content.toByteArray(Charsets.UTF_8)) }
            } else {
                android.util.Log.e("PublicFilesStore", "Impossible de créer $displayName")
            }
        } catch (e: Exception) {
            android.util.Log.e("PublicFilesStore", "Impossible d'écrire $displayName", e)
        }
    }
}