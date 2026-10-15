package com.jc2.jdrcompagnon.network

import android.content.Context
import com.jc2.jdrcompagnon.ui.PorteeCampagne
import com.jc2.jdrcompagnon.ui.screens.mj.CustomBook
import com.jc2.jdrcompagnon.ui.screens.mj.CustomBooksStore
import com.jc2.jdrcompagnon.ui.screens.mj.customBooksDir
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Imports (livres personnalisés de la bibliothèque) que le MJ partage avec les joueurs de sa
 * partie. Le MJ les choisit dans l'écran serveur ; à sa connexion, un joueur reçoit leur liste
 * (NetworkMessage.TYPE_SHARED_IMPORTS) et se voit proposer de télécharger ceux qu'il n'a pas,
 * en entier (texte et images), depuis la page d'affichage table du MJ (MjWebServer, `/import`).
 *
 * Un import n'est partagé qu'en entier : pour proposer le contenu de jeu d'une campagne (classes,
 * sous-classes, sorts, objets...) sans ses scénarios, le MJ en fait deux imports distincts.
 */
internal object ImportsPartages {

    private const val PREFS = "imports_partages"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun cleSelection(worldId: String) = "$worldId:ids"

    // ── Côté MJ ──

    /** Livres de la bibliothèque du monde que le MJ peut partager (contenu markdown). */
    fun livresPartageables(context: Context, worldId: String): List<CustomBook> =
        CustomBooksStore.list(context, worldId).filter { it.fileName.endsWith(".md", ignoreCase = true) }

    /** Identifiants des livres partagés (choix conservé d'une partie à l'autre). */
    fun selection(context: Context, worldId: String): Set<String> =
        prefs(context).getStringSet(cleSelection(worldId), emptySet()).orEmpty().toSet()

    fun definirPartage(context: Context, worldId: String, livreId: String, partage: Boolean) {
        val ids = selection(context, worldId).let { if (partage) it + livreId else it - livreId }
        prefs(context).edit().putStringSet(cleSelection(worldId), ids).apply()
    }

    /** Descriptifs des livres partagés du monde, envoyés aux joueurs. */
    fun descriptifs(context: Context, worldId: String): List<ImportPartageData> {
        val choisis = selection(context, worldId)
        return livresPartageables(context, worldId).filter { it.id in choisis }.mapNotNull { livre ->
            val fichier = File(customBooksDir(context), livre.fileName)
            val texte = runCatching { fichier.readText() }.getOrNull() ?: return@mapNotNull null
            ImportPartageData(
                id = livre.id,
                nom = livre.name,
                worldId = worldId,
                empreinte = empreinte(texte),
                taille = dossierDuLivre(context, livre)?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: fichier.length(),
            )
        }
    }

    /** Dossier d'un livre issu d'une archive .zip (images comprises), null pour un .md seul. */
    private fun dossierDuLivre(context: Context, livre: CustomBook): File? =
        livre.fileName.substringBefore('/', "").ifBlank { null }?.let { File(customBooksDir(context), it) }?.takeIf { it.isDirectory }

    /**
     * Archive .zip d'un livre partagé (son .md et, pour un livre issu d'une archive, tout son
     * dossier : images...), le fichier du livre étant toujours `livre.md` à la racine. Null si le
     * livre n'existe plus ou n'est pas partagé — la page table ne sert que les imports choisis.
     */
    fun archive(context: Context, worldId: String, livreId: String): File? {
        if (livreId !in selection(context, worldId)) return null
        val livre = livresPartageables(context, worldId).firstOrNull { it.id == livreId } ?: return null
        val source = File(customBooksDir(context), livre.fileName)
        if (!source.isFile) return null
        // Une archive par téléchargement (deux joueurs peuvent télécharger en même temps) ; les
        // anciennes, forcément envoyées depuis, sont nettoyées au passage.
        val dossierCache = File(context.cacheDir, "imports_partages").apply { mkdirs() }
        dossierCache.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 3_600_000L }?.forEach { it.delete() }
        val zip = File.createTempFile("import_", ".zip", dossierCache)
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            fun ajouter(nom: String, fichier: File) {
                out.putNextEntry(ZipEntry(nom))
                fichier.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
            ajouter(FICHIER_LIVRE, source)
            dossierDuLivre(context, livre)?.let { dossier ->
                dossier.walkTopDown()
                    .filter { it.isFile && it != source && !it.extension.equals("md", ignoreCase = true) }
                    .forEach { ajouter(it.relativeTo(dossier).invariantSeparatorsPath, it) }
            }
        }
        return zip
    }

    private const val FICHIER_LIVRE = "livre.md"

    // ── Côté joueur ──

    /** Import du MJ absent chez le joueur ; [remplace] : son livre du même nom, dans une autre version. */
    data class ImportManquant(val import: ImportPartageData, val remplace: CustomBook?)

    /**
     * Imports du MJ que le joueur n'a pas : un livre au contenu identique (même texte, quel que
     * soit son nom) compte comme possédé ; un livre du même nom au contenu différent est
     * signalé comme une autre version, remplacée si le joueur télécharge celle du MJ.
     */
    fun manquants(context: Context, imports: List<ImportPartageData>): List<ImportManquant> =
        imports.mapNotNull { import ->
            val livres = CustomBooksStore.list(context, import.worldId)
            val possede = livres.any { livre ->
                runCatching { File(customBooksDir(context), livre.fileName).readText() }.getOrNull()
                    ?.let { empreinte(it) == import.empreinte } == true
            }
            if (possede) null
            else ImportManquant(import, livres.firstOrNull { it.name.trim().equals(import.nom.trim(), ignoreCase = true) })
        }

    /**
     * Télécharge un import depuis la page table du MJ ([urlTable]) et l'ajoute à la bibliothèque
     * du joueur (images comprises), à la place de son ancienne version le cas échéant.
     */
    fun telecharger(context: Context, urlTable: String, manquant: ImportManquant) {
        val import = manquant.import
        val url = "${urlTable.trimEnd('/')}/import?monde=${URLEncoder.encode(import.worldId, "UTF-8")}&id=${URLEncoder.encode(import.id, "UTF-8")}"
        val id = "${System.currentTimeMillis()}"
        val dossier = File(customBooksDir(context), id).apply { mkdirs() }
        try {
            val connexion = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 60_000
            }
            try {
                if (connexion.responseCode != HttpURLConnection.HTTP_OK) error("Le MJ ne partage plus « ${import.nom} » (${connexion.responseCode}).")
                extraire(ZipInputStream(connexion.inputStream.buffered()), dossier)
            } finally {
                connexion.disconnect()
            }
            if (!File(dossier, FICHIER_LIVRE).isFile) error("Archive de « ${import.nom} » incomplète.")
        } catch (e: Exception) {
            dossier.deleteRecursively()
            throw e
        }
        manquant.remplace?.let { CustomBooksStore.remove(context, import.worldId, it.id) }
        CustomBooksStore.add(context, import.worldId, id, import.nom, "$id/$FICHIER_LIVRE")
        SrdRepository.invalidateWorld(import.worldId)
        PorteeCampagne.livresChanges()
    }

    /** Extrait l'archive dans [dossier], en ignorant les entrées qui en sortiraient ("zip slip"). */
    private fun extraire(zip: ZipInputStream, dossier: File) {
        val racine = dossier.canonicalPath + File.separator
        zip.use {
            var entree = it.nextEntry
            while (entree != null) {
                val cible = File(dossier, entree.name.replace('\\', '/'))
                if (!entree.isDirectory && cible.canonicalPath.startsWith(racine)) {
                    cible.parentFile?.mkdirs()
                    cible.outputStream().use { sortie -> it.copyTo(sortie) }
                }
                it.closeEntry()
                entree = it.nextEntry
            }
        }
    }

    /** Empreinte du texte d'un livre : fins de ligne et espaces de fin de ligne ignorés. */
    private fun empreinte(texte: String): String {
        val normalise = texte.replace("\r\n", "\n").replace('\r', '\n').lines().joinToString("\n") { it.trimEnd() }.trim()
        return MessageDigest.getInstance("SHA-256").digest(normalise.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
