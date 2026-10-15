package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * Fichiers d'une fiche de personnage dans Téléchargements/JDRCompagnon/Personnages/, rangés par
 * type : `PJ/`, `PNJ/` et `Monstres/` (monstres, boss, créatures, familiers). Pour chaque fiche :
 * - `<nom>-<id8>.json` : la fiche et son historique (voir GameState.writeCharacterFile) ;
 * - `<nom>-<id8>.md` : son histoire personnelle ([Character.histoirePersonnelle]), écrite à côté
 *   du .json sous le même nom pour que les deux se copient/collent ensemble ;
 * - les images de l'histoire, référencées dans le .md par leur seul nom (`![](nom.jpg)`), dans le
 *   même dossier. Une copie interne (filesDir/histoires) garantit leur affichage dans l'app même
 *   si Android ne laisse plus relire le dossier public.
 */
object PersonnageFichiers {

    const val RACINE = "Personnages"
    private val SOUS_DOSSIERS = listOf("PJ", "PNJ", "Monstres")
    private const val DOSSIER_INTERNE = "histoires"

    private val imageMarkdown = Regex("""!\[([^\]]*)]\(\s*<?([^)>\s]+)>?(?:\s+"[^"]*")?\s*\)""")
    // « nom-1a2b3c4d.json », ou sa copie « nom-1a2b3c4d (1).json » créée par Android.
    private val fichierFiche = Regex("""-([0-9a-f]{8})(?: \(\d+\))?\.(json|md)$""", RegexOption.IGNORE_CASE)
    private val extensionsImage = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    fun dossier(character: Character): String = "$RACINE/" + when (character.type) {
        "PJ" -> "PJ"
        "PNJ" -> "PNJ"
        else -> "Monstres"
    }

    fun nomBase(character: Character): String = "${GameState.slugify(character.name)}-${character.id.take(8)}"
    fun nomJson(character: Character): String = "${nomBase(character)}.json"
    fun nomHistoire(character: Character): String = "${nomBase(character)}.md"

    /** Chemin affiché à l'utilisateur du fichier .md de l'histoire. */
    fun cheminHistoire(character: Character): String =
        PublicFilesStore.directoryLabel(dossier(character)) + "/" + nomHistoire(character)

    /** Noms des images référencées par un texte markdown (le chemin éventuel est ignoré). */
    fun imagesReferencees(texte: String): List<String> =
        imageMarkdown.findAll(texte).map { nomImage(it.groupValues[2]) }.filter { it.isNotBlank() }.distinct().toList()

    /** Découpe un texte markdown en morceaux de texte et images (nom de fichier, texte alternatif). */
    sealed interface Bloc {
        data class Texte(val contenu: String) : Bloc
        data class Image(val nom: String, val alt: String) : Bloc
    }

    fun decouper(texte: String): List<Bloc> {
        val blocs = mutableListOf<Bloc>()
        var debut = 0
        imageMarkdown.findAll(texte).forEach { m ->
            val avant = texte.substring(debut, m.range.first)
            if (avant.isNotBlank()) blocs += Bloc.Texte(avant)
            val cible = m.groupValues[2]
            // Image distante : non chargée, son texte alternatif reste affiché.
            blocs += if (cible.startsWith("http", ignoreCase = true)) Bloc.Texte(m.groupValues[1])
            else Bloc.Image(nomImage(cible), m.groupValues[1])
            debut = m.range.last + 1
        }
        texte.substring(debut).takeIf { it.isNotBlank() }?.let { blocs += Bloc.Texte(it) }
        return blocs
    }

    private fun nomImage(cible: String): String =
        Uri.decode(cible).replace('\\', '/').substringAfterLast('/').trim()

    private fun dossierInterne(context: Context): File =
        File(context.filesDir, DOSSIER_INTERNE).apply { mkdirs() }

    // ── Accès aux fichiers du dossier public ──
    // Avec l'accès au dossier JDRCompagnon (AccesDossierJdr), tout fichier est visible, y compris
    // ceux collés à la main ; sinon seuls ceux écrits par l'app (PublicFilesStore / MediaStore).

    fun lireFichier(context: Context, chemin: String, nom: String): ByteArray? =
        AccesDossierJdr.lireOctets(context, chemin, nom) ?: PublicFilesStore.readBytes(context, nom, chemin)

    fun ecrireFichier(context: Context, chemin: String, nom: String, contenu: ByteArray, mime: String) {
        if (!AccesDossierJdr.ecrire(context, chemin, nom, contenu)) {
            PublicFilesStore.writeBytes(context, nom, contenu, chemin, mime)
        }
    }

    fun supprimerFichier(context: Context, chemin: String, nom: String) {
        AccesDossierJdr.supprimer(context, chemin, nom)
        PublicFilesStore.delete(context, nom, chemin)
    }

    private val _imagesRecues = kotlinx.coroutines.flow.MutableStateFlow(0)
    /** Incrémenté quand des images d'histoire arrivent (réseau) : l'affichage se rafraîchit. */
    val imagesRecues: kotlinx.coroutines.flow.StateFlow<Int> = _imagesRecues

    /**
     * Fichier de l'image [nom], lu d'abord dans le dossier du personnage (une image collée ou
     * remplacée à la main y est reprise automatiquement) et recopié en interne ; à défaut, la
     * copie interne. Null si introuvable.
     */
    fun fichierImage(context: Context, character: Character, nom: String): File? {
        val interne = File(dossierInterne(context), nom)
        val octets = lireFichier(context, dossier(character), nom)
        if (octets != null && (!interne.exists() || interne.length() != octets.size.toLong() || !interne.readBytes().contentEquals(octets))) {
            runCatching { interne.writeBytes(octets) }
        }
        return interne.takeIf { it.exists() }
    }

    /** Images référencées par l'histoire et introuvables sur cet appareil. */
    fun imagesManquantes(context: Context, character: Character): List<String> =
        imagesReferencees(character.histoirePersonnelle).filter { nom ->
            !File(dossierInterne(context), nom).exists() && fichierImage(context, character, nom) == null
        }

    /** Image reçue (réseau) : enregistrée en interne et dans le dossier du personnage. */
    fun enregistrerImage(context: Context, character: Character, nom: String, octets: ByteArray) {
        val propre = nomImage(nom).takeIf { it.isNotBlank() && !it.startsWith(".") } ?: return
        runCatching { File(dossierInterne(context), propre).writeBytes(octets) }
        ecrireFichier(context, dossier(character), propre, octets, mimeImage(propre))
        _imagesRecues.value++
    }

    /**
     * Copie l'image choisie ([uri]) dans le dossier du personnage et en interne, sous le nom
     * [nomImpose] (image manquante d'un .md importé) ou un nouveau nom `<nom>-<id8>-img<horodatage>.ext`.
     * Retourne le nom du fichier, à référencer dans le markdown.
     */
    fun ajouterImage(context: Context, character: Character, uri: Uri, nomImpose: String? = null): String? {
        val octets = runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull() ?: return null
        val nom = nomImpose ?: run {
            val extension = nomAffiche(context, uri)?.substringAfterLast('.', "")?.lowercase()
                ?.takeIf { it in extensionsImage }
                ?: context.contentResolver.getType(uri)?.substringAfter("image/")?.takeIf { it in extensionsImage }
                ?: "jpg"
            "${nomBase(character)}-img${System.currentTimeMillis()}.$extension"
        }
        runCatching { File(dossierInterne(context), nom).writeBytes(octets) }.getOrElse { return null }
        ecrireFichier(context, dossier(character), nom, octets, mimeImage(nom))
        return nom
    }

    /** Écrit le .md de l'histoire (supprimé si l'histoire est vide). */
    fun ecrireHistoire(context: Context, character: Character) {
        if (character.histoirePersonnelle.isBlank()) {
            supprimerFichier(context, dossier(character), nomHistoire(character))
        } else {
            ecrireFichier(context, dossier(character), nomHistoire(character), character.histoirePersonnelle.toByteArray(Charsets.UTF_8), "text/markdown")
        }
    }

    /**
     * Contenu actuel du .md (modifié hors de l'app, ou collé à la main sous le nom exact de la
     * fiche), ou null s'il n'existe pas / n'est pas lisible.
     */
    fun lireHistoire(context: Context, character: Character): String? =
        lireFichier(context, dossier(character), nomHistoire(character))?.toString(Charsets.UTF_8)

    /**
     * La fiche a changé de nom ou de type : ses fichiers changent de nom ou de dossier. Les
     * anciens .json/.md sont supprimés (les nouveaux sont écrits par l'appelant) ; en cas de
     * changement de dossier, les images suivent.
     */
    fun deplacer(context: Context, ancien: Character, nouveau: Character) {
        if (dossier(ancien) == dossier(nouveau) && nomBase(ancien) == nomBase(nouveau)) return
        supprimerFichier(context, dossier(ancien), nomJson(ancien))
        supprimerFichier(context, dossier(ancien), nomHistoire(ancien))
        if (dossier(ancien) != dossier(nouveau)) {
            imagesReferencees(nouveau.histoirePersonnelle).forEach { nom ->
                val fichier = fichierImage(context, ancien, nom) ?: return@forEach
                ecrireFichier(context, dossier(nouveau), nom, fichier.readBytes(), mimeImage(nom))
                supprimerFichier(context, dossier(ancien), nom)
            }
        }
    }

    /**
     * Fiche supprimée : son .json, son .md et ses images (dossier public et copie interne), sauf
     * les images encore utilisées par une autre fiche ([autres]).
     */
    fun supprimer(context: Context, character: Character, autres: List<Character>) {
        supprimerFichier(context, dossier(character), nomJson(character))
        supprimerFichier(context, dossier(character), nomHistoire(character))
        val utilisees = autres.flatMapTo(HashSet()) { imagesReferencees(it.histoirePersonnelle) }
        imagesReferencees(character.histoirePersonnelle).filter { it !in utilisees }.forEach { nom ->
            supprimerFichier(context, dossier(character), nom)
            File(dossierInterne(context), nom).delete()
        }
    }

    /**
     * Range et nettoie le dossier Personnages (au lancement de l'app) :
     * - les fichiers de l'ancien emplacement (`Personnages/` sans sous-dossier) sont retirés, la
     *   fiche étant réécrite dans le bon sous-dossier ;
     * - les .json/.md d'une fiche qui n'existe plus dans l'app (supprimée, ou import supprimé),
     *   ou laissés sous un ancien nom/dossier, sont supprimés, ainsi que les images qui ne
     *   servent plus à aucune histoire ;
     * Retourne les fiches dont le .json manque, à (ré)écrire par l'appelant.
     * Seuls les fichiers visibles de l'app (ceux qu'elle a écrits) sont concernés.
     */
    fun ranger(context: Context, personnages: List<Character>): List<Character> {
        val parId8 = personnages.groupBy { it.id.take(8) }
        val presents = HashSet<Pair<String, String>>() // (dossier, nom) des .json/.md à garder
        val imagesUtilisees = HashMap<String, MutableSet<String>>()
        personnages.forEach { c ->
            presents += dossier(c) to nomJson(c)
            if (c.histoirePersonnelle.isNotBlank()) presents += dossier(c) to nomHistoire(c)
            imagesUtilisees.getOrPut(dossier(c)) { HashSet() } += imagesReferencees(c.histoirePersonnelle)
        }

        // Ancien emplacement : tout fichier de fiche reconnu est retiré (réécrit ailleurs).
        PublicFilesStore.listAll(context, RACINE).forEach { f ->
            if (fichierFiche.containsMatchIn(f.name)) supprimerUri(context, f.uri)
        }

        val jsonPresents = HashSet<Pair<String, String>>()
        SOUS_DOSSIERS.map { "$RACINE/$it" }.forEach { sousDossier ->
            PublicFilesStore.listAll(context, sousDossier).forEach { f ->
                val extension = f.name.substringAfterLast('.', "").lowercase()
                val fiche = fichierFiche.find(f.name)
                when {
                    fiche != null -> {
                        // Copie « (n) » ou fichier d'une fiche disparue / renommée / déplacée.
                        val connue = parId8.containsKey(fiche.groupValues[1].lowercase())
                        if (connue && (sousDossier to f.name) in presents) {
                            if (extension == "json") jsonPresents += sousDossier to f.name
                        } else {
                            supprimerUri(context, f.uri)
                        }
                    }
                    extension in extensionsImage -> {
                        if (f.name !in imagesUtilisees[sousDossier].orEmpty()) supprimerUri(context, f.uri)
                    }
                }
            }
        }

        // Copies internes d'images que plus aucune histoire n'utilise.
        val toutesImages = imagesUtilisees.values.flatten().toSet()
        dossierInterne(context).listFiles()?.forEach { if (it.name !in toutesImages) it.delete() }

        return personnages.filter { (dossier(it) to nomJson(it)) !in jsonPresents }
    }

    private fun supprimerUri(context: Context, uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }

    fun mimeImage(nom: String): String = when (nom.substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }

    private fun nomAffiche(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()
}
