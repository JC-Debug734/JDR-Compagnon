package com.jc2.jdrcompagnon.feature_import

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.jc2.jdrcompagnon.ui.CampagneFileStore
import com.jc2.jdrcompagnon.ui.CampagneImageStore
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ImportedMusicStore
import com.jc2.jdrcompagnon.ui.ProfilsPackStore
import com.jc2.jdrcompagnon.ui.components.FilePortraits
import com.jc2.jdrcompagnon.ui.screens.mj.CustomBooksStore
import com.jc2.jdrcompagnon.ui.screens.mj.LivreAjoute
import com.jc2.jdrcompagnon.ui.screens.mj.ajouterLivrePersonnalise
import com.jc2.jdrcompagnon.ui.screens.mj.readCustomBookContent
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImport
import com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipInputStream

/**
 * Point d'entrée unique des imports de l'app (outil IMPORT, voir [ImportScreen]) : le genre de
 * contenu est reconnu d'après le fichier lui-même, puis confié au mécanisme existant qui le
 * gère — plus besoin de savoir dans quelle page importer quoi.
 *
 * Fichiers :
 * - `.zip` contenant un `reference.md` (à la racine ou dans un unique dossier) → univers
 *   ([CustomWorldsRepository.importWorldFromZip]) ;
 * - autre `.zip` → livre avec ses images (bestiaire illustré, objets, PNJ...) ;
 * - `.md` avec des entrées `<!-- type: ... -->` → livre de bibliothèque ; sinon → scénario
 *   ([ScenarioImport]) ;
 * - `.json` → fiche de personnage (export JDRCompagnon) ;
 * - fichier audio → musique ([ImportedMusicStore]) ;
 * - tout autre fichier (PDF...) → livre à consulter dans la bibliothèque.
 *
 * Dossiers : avec un `campagne.json` → campagne ([CampagneFileStore]) ; sinon → dossier de
 * scénarios et de profils ([ProfilsPackStore.importerScenariosDepuisDossier]).
 *
 * Chaque import, réussi ou non, est inscrit dans l'historique ([ImportHistorique]) avec la
 * liste de ce qu'il a créé, ce qui permet de le supprimer ensuite en entier ([supprimerImport]).
 */
object ImportCentral {

    private val audioExtensions = setOf("mp3", "ogg", "wav", "m4a", "aac", "flac", "opus")
    private const val REFERENCE_UNIVERS = "reference.md"
    private const val META_CAMPAGNE = "campagne.json"

    suspend fun importerFichier(context: Context, uri: Uri, worldId: String?): ImportHistorique.Entree =
        withContext(Dispatchers.IO) {
            val nomFichier = nomAffiche(context, uri) ?: "Fichier"
            val extension = nomFichier.substringAfterLast('.', "").lowercase()
            val nom = nomFichier.substringBeforeLast('.').ifBlank { nomFichier }
            val mime = context.contentResolver.getType(uri).orEmpty()
            val avant = photographier(context, worldId)

            val (genre, resultat) = try {
                when {
                    extension == "zip" && estUnivers(context, uri) -> ImportGenre.UNIVERS to importerUnivers(context, uri, nom)
                    extension == "zip" -> ImportGenre.LIVRE to importerLivre(context, uri, worldId, nom)
                    extension == "md" || extension == "markdown" -> importerMarkdown(context, uri, worldId, nom)
                    extension == "json" -> ImportGenre.PERSONNAGE to importerPersonnage(context, uri, worldId)
                    extension in audioExtensions || mime.startsWith("audio/") ->
                        ImportGenre.MUSIQUE to importerMusique(context, uri, nom, nomFichier)
                    else -> ImportGenre.LIVRE to importerLivre(context, uri, worldId, nom)
                }
            } catch (e: Exception) {
                ImportGenre.INCONNU to Result.failure(e)
            }
            ImportHistorique.ajouter(context, nomFichier, genre, resultat, nouveautes(context, worldId, avant))
        }

    suspend fun importerDossier(context: Context, uri: Uri, worldId: String?): ImportHistorique.Entree =
        withContext(Dispatchers.IO) {
            val racine = DocumentFile.fromTreeUri(context, uri)
            val nomDossier = racine?.name ?: "Dossier"
            val avant = photographier(context, worldId)
            val (genre, resultat) = try {
                if (racine?.findFile(META_CAMPAGNE) != null) {
                    ImportGenre.CAMPAGNE to CampagneFileStore.importerCampagneDepuisDossier(context, uri).map { (campagne, resume) ->
                        "Campagne « ${campagne.title} » : ${resume.texte()}."
                    }
                } else {
                    ImportGenre.DOSSIER to ProfilsPackStore.importerScenariosDepuisDossier(context, uri, worldId).map { resume ->
                        "${resume.texte().replaceFirstChar { it.uppercase() }}."
                    }
                }
            } catch (e: Exception) {
                ImportGenre.INCONNU to Result.failure(e)
            }
            ImportHistorique.ajouter(context, nomDossier, genre, resultat, nouveautes(context, worldId, avant))
        }

    // ── Suppression d'un import ──

    /**
     * Ce qui existe dans l'app avant un import (identifiants par type d'élément) : comparé à
     * l'état d'après, il donne exactement ce que l'import a créé, quel que soit le mécanisme
     * d'import sollicité (livre, univers, campagne, dossier de scénarios...).
     */
    private class Photo(val parType: Map<String, Map<String, String>>)

    private fun photographier(context: Context, worldId: String?): Photo = Photo(
        mapOf(
            "scenario" to GameState.mjScenarios.value.associate { it.id to it.title },
            "campagne" to GameState.mjCampaigns.value.associate { it.id to it.title },
            "personnage" to GameState.characters.value.associate { it.id to "${it.name} (${it.type})" },
            "livre" to CustomBooksStore.list(context, worldId).associate { it.id to it.name },
            "univers" to CustomWorldsRepository.listCustomWorlds(context).associate { it.id to it.name },
            "musique" to ImportedMusicStore.tracks.value.associate { it.id to it.displayName },
        )
    )

    private fun nouveautes(context: Context, worldId: String?, avant: Photo): List<ImportHistorique.ElementImporte> {
        val apres = photographier(context, worldId)
        return ImportHistorique.TYPES.flatMap { type ->
            val anciens = avant.parType[type].orEmpty()
            apres.parType[type].orEmpty()
                .filterKeys { it !in anciens }
                .map { (id, libelle) -> ImportHistorique.ElementImporte(type, id, libelle, worldId.takeIf { type == "livre" }) }
        }
    }

    /**
     * Retire tout ce qu'a créé l'import [entree] — livres et leurs fichiers (images comprises),
     * fiches (et portraits importés), scénarios (fichiers et images), campagnes, univers,
     * musiques — puis le marque comme supprimé dans l'historique. Les éléments déjà supprimés
     * à la main entre-temps sont simplement ignorés. Retourne le compte rendu.
     */
    suspend fun supprimerImport(context: Context, entree: ImportHistorique.Entree): String = withContext(Dispatchers.IO) {
        val retires = mutableListOf<String>()
        val mondesModifies = mutableSetOf<String?>()
        entree.elements.forEach { e ->
            val ok = when (e.type) {
                "scenario" -> GameState.mjScenarios.value.any { it.id == e.id }.also { if (it) GameState.removeMjScenario(e.id, context) }
                "campagne" -> GameState.mjCampaigns.value.any { it.id == e.id }.also {
                    if (it) {
                        GameState.removeMjCampaign(e.id)
                        CampagneImageStore.nettoyer(context, e.id)
                        // Carte, lieux et boutiques créés avec la campagne (voir CampagneArchive).
                        runCatching { CampagneArchive.supprimerDonnees(e.id) }
                    }
                }
                "personnage" -> GameState.characters.value.firstOrNull { it.id == e.id }?.let { perso ->
                    // Portrait copié par l'import d'un livre (pnj_portraits/) : supprimé avec la fiche.
                    if (FilePortraits.isFilePortrait(perso.portrait)) FilePortraits.file(perso.portrait).delete()
                    GameState.removeCharacter(perso.id)
                    true
                } ?: false
                "livre" -> CustomBooksStore.list(context, e.worldId).any { it.id == e.id }.also {
                    if (it) {
                        CustomBooksStore.remove(context, e.worldId, e.id)
                        mondesModifies += e.worldId
                    }
                }
                "univers" -> CustomWorldsRepository.deleteCustomWorld(context, e.id)
                "musique" -> ImportedMusicStore.tracks.value.any { it.id == e.id }.also { if (it) ImportedMusicStore.remove(context, e.id) }
                else -> false
            }
            if (ok) retires += e.libelle
        }
        mondesModifies.forEach { SrdRepository.invalidateWorld(it) }
        val compteRendu = if (retires.isEmpty()) "Import supprimé (plus rien à retirer)."
        else "Import supprimé : " + retires.joinToString(", ") + "."
        ImportHistorique.marquerSupprime(context, entree, compteRendu)
        compteRendu
    }

    private suspend fun importerLivre(context: Context, uri: Uri, worldId: String?, nom: String): Result<String> {
        val livre = ajouterLivrePersonnalise(context, uri, worldId, nom)
            ?: return Result.failure(IllegalArgumentException("Fichier illisible, ou archive sans fichier .md."))
        // Campagne livrée par l'archive : relie scénarios, fiches et livre qui viennent d'être importés.
        // Ses monstres forment le « Bestiaire attaché » de la campagne.
        val campagne = livre.campagne?.let {
            val monstres = livre.fileName?.let { f -> readCustomBookContent(context, f) }
                ?.let { texte -> CustomContentParser.parse(texte).monsters.map { m -> m.name } }
                .orEmpty()
            " " + CampagneArchive.importer(context, it, worldId ?: "donjon_et_dragon", livre.scenarioIds, livre.pnjIds, livre.id, monstres)
        }.orEmpty()
        return resumeLivre(livre, nom).map { it + campagne }
    }

    private fun resumeLivre(livre: LivreAjoute, nom: String): Result<String> {
        val scenarios = livre.scenarios.takeIf { it.isNotEmpty() }?.let { titres ->
            " Scénario(s) : " + titres.joinToString(", ") { "« $it »" } +
                (if (livre.pnjScenarios > 0) " (${livre.pnjScenarios} fiche(s) PNJ créée(s))" else "") + "."
        }.orEmpty()
        if (livre.id == null) return Result.success(scenarios.trim())
        val resume = livre.summary ?: return Result.success("Livre « $nom » ajouté à la bibliothèque.$scenarios")
        val details = buildList {
            if (resume.monsters > 0) add("${resume.monsters} monstre(s)")
            if (livre.pnjCrees > 0) add("${livre.pnjCrees} fiche(s) PNJ créée(s)")
            val pnjExistants = resume.pnjs - livre.pnjCrees
            if (pnjExistants > 0) add("$pnjExistants PNJ déjà présent(s)")
            if (resume.equipment > 0) add("${resume.equipment} objet(s)")
            if (resume.spells > 0) add("${resume.spells} sort(s)")
            if (resume.sousClasses > 0) add("${resume.sousClasses} sous-classe(s)")
            val autres = resume.classes + resume.especes + resume.historiques + resume.dons + resume.rules
            if (autres > 0) add("$autres autre(s) entrée(s)")
        }
        return Result.success(
            "Livre « $nom » : " + (details.joinToString(", ").ifEmpty { "aucune entrée reconnue" }) + "." + scenarios
        )
    }

    private suspend fun importerMarkdown(context: Context, uri: Uri, worldId: String?, nom: String): Pair<ImportGenre, Result<String>> {
        val texte = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return ImportGenre.INCONNU to Result.failure(IllegalArgumentException("Fichier illisible."))
        if (CustomContentParser.parse(texte).summary.total > 0) {
            return ImportGenre.LIVRE to importerLivre(context, uri, worldId, nom)
        }
        val (scenario, pnjCrees) = ScenarioImport.importer(context, texte, worldId ?: "")
        return ImportGenre.SCENARIO to Result.success(
            "Scénario « ${scenario.title} »" + if (pnjCrees > 0) " ($pnjCrees fiche(s) PNJ créée(s))." else "."
        )
    }

    private fun importerPersonnage(context: Context, uri: Uri, worldId: String?): Result<String> {
        val texte = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        val export = texte?.let { GameState.characterFromJson(it) }
            ?: return Result.failure(IllegalArgumentException("Ce n'est pas une fiche de personnage JDRCompagnon."))
        GameState.addCharacter(
            export.character.copy(
                id = java.util.UUID.randomUUID().toString(),
                worldId = worldId ?: "donjon_et_dragon",
                createdBy = "MJ",
            ),
            historiqueImporte = export.historique,
        )
        return Result.success("Fiche « ${export.character.name} » (${export.character.type}) ajoutée.")
    }

    private fun importerMusique(context: Context, uri: Uri, nom: String, nomFichier: String): Result<String> {
        val piste = ImportedMusicStore.import(context, uri, nom, nomFichier)
            ?: return Result.failure(IllegalArgumentException("Impossible de lire ce fichier audio."))
        return Result.success("Musique « ${piste.displayName} » ajoutée.")
    }

    private fun importerUnivers(context: Context, uri: Uri, nom: String): Result<String> =
        CustomWorldsRepository.importWorldFromZip(context, uri, nom).map { univers ->
            "Univers « ${univers.name} » ajouté : à choisir dans la sélection des univers."
        }

    /**
     * Un .zip d'univers se reconnaît à son `reference.md`, à la racine ou dans l'unique dossier
     * de premier niveau (voir CustomWorldsRepository.resolveContentRoot).
     */
    private fun estUnivers(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                generateSequence { zip.nextEntry }
                    .map { it.name.replace('\\', '/').trimStart('/') }
                    .any { nom ->
                        val parties = nom.split('/')
                        parties.last().equals(REFERENCE_UNIVERS, ignoreCase = true) && parties.size <= 2
                    }
            }
        } ?: false
    }.getOrDefault(false)

    private fun nomAffiche(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
}

/**
 * Demande d'ouverture de l'outil IMPORT depuis un écran qui n'a pas accès au NavController
 * (anciens boutons d'import de la Bibliothèque, des Scénarios, des Campagnes, de la Musique) :
 * JdrNavGraph observe [demande] et navigue, comme pour GameState.requestRoleChange().
 */
object ImportNavigation {
    private val _demande = kotlinx.coroutines.flow.MutableStateFlow(false)
    val demande: kotlinx.coroutines.flow.StateFlow<Boolean> = _demande

    fun ouvrir() {
        _demande.value = true
    }

    fun consommer() {
        _demande.value = false
    }
}
