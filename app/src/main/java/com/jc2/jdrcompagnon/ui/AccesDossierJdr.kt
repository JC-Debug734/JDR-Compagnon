package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Accès complet au dossier Téléchargements/JDRCompagnon, accordé une fois par l'utilisateur
 * (sélecteur de dossier Android) et conservé. Sans lui, l'app ne voit que les fichiers qu'elle a
 * écrits elle-même (voir [PublicFilesStore]) : un fichier collé à la main avec un gestionnaire
 * de fichiers lui reste invisible. Avec lui, elle lit, réécrit et supprime n'importe quel fichier
 * du dossier (ex. l'histoire `.md` d'un personnage et ses images, voir [PersonnageFichiers]).
 */
object AccesDossierJdr {

    private const val PREFS = "acces_dossier_jdr"
    private const val KEY_URI = "tree_uri"
    private const val NOM_RACINE = "JDRCompagnon"

    private val _accorde = MutableStateFlow<Boolean?>(null)
    /** null tant que [estAccorde] n'a pas été appelé. */
    val accorde: StateFlow<Boolean?> = _accorde.asStateFlow()

    /** Dossier proposé à l'ouverture du sélecteur (Téléchargements/JDRCompagnon). */
    fun uriInitiale(): Uri =
        DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Download/$NOM_RACINE")

    /**
     * Enregistre le dossier choisi dans le sélecteur. Le dossier JDRCompagnon lui-même, ou un
     * dossier qui le contient, est accepté. Retourne false si ce n'est pas le bon dossier.
     */
    fun enregistrer(context: Context, uri: Uri): Boolean {
        val choisi = DocumentFile.fromTreeUri(context, uri) ?: return false
        val valide = choisi.name.equals(NOM_RACINE, ignoreCase = true) ||
            choisi.findFile(NOM_RACINE)?.isDirectory == true
        if (!valide) return false
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.getOrElse { return false }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URI, uri.toString()).apply()
        _accorde.value = true
        return true
    }

    fun estAccorde(context: Context): Boolean = (racine(context) != null).also { _accorde.value = it }

    private fun racine(context: Context): DocumentFile? {
        val texte = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URI, null) ?: return null
        val uri = Uri.parse(texte)
        val permise = context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
        if (!permise) return null
        val choisi = DocumentFile.fromTreeUri(context, uri)?.takeIf { it.exists() } ?: return null
        return if (choisi.name.equals(NOM_RACINE, ignoreCase = true)) choisi else choisi.findFile(NOM_RACINE)
    }

    /** Sous-dossier [chemin] (ex. "Personnages/PJ") de JDRCompagnon, créé au besoin si [creer]. */
    private fun dossier(context: Context, chemin: String, creer: Boolean): DocumentFile? {
        var courant = racine(context) ?: return null
        chemin.split('/').filter { it.isNotBlank() }.forEach { nom ->
            courant = courant.findFile(nom)?.takeIf { it.isDirectory }
                ?: (if (creer) courant.createDirectory(nom) else null)
                ?: return null
        }
        return courant
    }

    private fun fichier(context: Context, chemin: String, nom: String): DocumentFile? =
        dossier(context, chemin, creer = false)?.findFile(nom)?.takeIf { it.isFile }

    fun lireOctets(context: Context, chemin: String, nom: String): ByteArray? = runCatching {
        val f = fichier(context, chemin, nom) ?: return null
        context.contentResolver.openInputStream(f.uri)?.use { it.readBytes() }
    }.getOrNull()

    fun lireTexte(context: Context, chemin: String, nom: String): String? =
        lireOctets(context, chemin, nom)?.toString(Charsets.UTF_8)

    /** Écrit (ou remplace) le fichier ; false si l'accès n'est pas accordé ou en cas d'échec. */
    fun ecrire(context: Context, chemin: String, nom: String, contenu: ByteArray): Boolean = runCatching {
        val d = dossier(context, chemin, creer = true) ?: return false
        // Type générique : le fournisseur garde alors le nom (et l'extension) tel quel.
        val f = d.findFile(nom)?.takeIf { it.isFile } ?: d.createFile("application/octet-stream", nom) ?: return false
        context.contentResolver.openOutputStream(f.uri, "wt")?.use { it.write(contenu) } ?: return false
        true
    }.getOrDefault(false)

    fun supprimer(context: Context, chemin: String, nom: String): Boolean =
        runCatching { fichier(context, chemin, nom)?.delete() == true }.getOrDefault(false)
}
