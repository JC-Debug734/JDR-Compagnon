package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jc2.jdrcompagnon.ui.screens.mj.CustomBooksStore
import com.jc2.jdrcompagnon.ui.screens.mj.customBooksDir
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.PnjImport
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.readCustomBookContent
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.sceneProfiles
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Profils personnalisés embarqués dans un dossier de campagne ou de scénario, en plus des
 * scénarios eux-mêmes :
 * - `Personnages/` (fichiers .json) : fiches PNJ / PJ / créatures (format d'export d'un personnage,
 *   [CharacterExport], ou Character brut) ;
 * - `Bibliotheque/` (fichiers .md) : contenu de bibliothèque personnalisé (monstres, objets, sorts, dons,
 *   règles… au format de [CustomContentParser], balises `<!-- type: monstre -->`), ajouté comme
 *   livre personnalisé de l'univers courant.
 *
 * Utilisé par l'export/import de campagne ([CampagneFileStore]) et par l'import d'un dossier de
 * scénarios ([importerScenariosDepuisDossier]).
 */
object ProfilsPackStore {

    private val PERSONNAGES_DIRS = listOf("Personnages", "PNJ", "PJ", "Profils")
    private val BIBLIOTHEQUE_DIRS = listOf("Bibliotheque", "Bibliothèque", "Monstres", "Objets")
    private const val PERSONNAGES_EXPORT_DIR = "Personnages"
    private const val BIBLIOTHEQUE_EXPORT_DIR = "Bibliotheque"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** Ce qui a été intégré lors d'un import, pour le message de confirmation. */
    data class ResumeImport(
        val scenarios: Int = 0,
        val personnages: Int = 0,
        val personnagesDejaPresents: Int = 0,
        val livres: Int = 0,
        val monstres: Int = 0,
        val objets: Int = 0,
        val autresEntrees: Int = 0,
    ) {
        operator fun plus(autre: ResumeImport) = ResumeImport(
            scenarios + autre.scenarios,
            personnages + autre.personnages,
            personnagesDejaPresents + autre.personnagesDejaPresents,
            livres + autre.livres,
            monstres + autre.monstres,
            objets + autre.objets,
            autresEntrees + autre.autresEntrees,
        )

        fun texte(): String = buildList {
            if (scenarios > 0) add("$scenarios scénario(s)")
            if (personnages > 0) add("$personnages personnage(s)")
            if (monstres > 0) add("$monstres monstre(s)")
            if (objets > 0) add("$objets objet(s)")
            if (autresEntrees > 0) add("$autresEntrees autre(s) entrée(s) de bibliothèque")
            if (personnagesDejaPresents > 0) add("$personnagesDejaPresents personnage(s) déjà présent(s) ignoré(s)")
        }.joinToString(", ").ifEmpty { "aucun profil" }
    }

    private fun DocumentFile.readTextOrNull(context: Context): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (e: Exception) {
        android.util.Log.e("ProfilsPackStore", "Impossible de lire ${this.name}", e)
        null
    }

    private fun DocumentFile.sousDossier(noms: List<String>): List<DocumentFile> =
        listFiles().filter { it.isDirectory && noms.any { nom -> nom.equals(it.name, ignoreCase = true) } }

    private fun DocumentFile.fichiers(extension: String): List<DocumentFile> =
        listFiles().filter { it.isFile && it.name?.endsWith(extension, ignoreCase = true) == true }

    /** Importe les personnages et le contenu de bibliothèque présents dans [racine]. */
    suspend fun importerProfils(context: Context, racine: DocumentFile, worldId: String?): ResumeImport {
        var resume = ResumeImport()

        // Personnages : nouvel id et univers courant ; une fiche de même nom et même type déjà
        // présente dans l'univers n'est pas dupliquée (réimport du même paquet).
        racine.sousDossier(PERSONNAGES_DIRS).flatMap { it.fichiers(".json") }.forEach { fichier ->
            val texte = fichier.readTextOrNull(context) ?: return@forEach
            val export = GameState.characterFromJson(texte) ?: return@forEach
            val cible = worldId ?: "donjon_et_dragon"
            val existe = GameState.characters.value.any {
                it.worldId == cible && it.type == export.character.type &&
                    it.name.equals(export.character.name, ignoreCase = true)
            }
            if (existe) {
                resume = resume.copy(personnagesDejaPresents = resume.personnagesDejaPresents + 1)
                return@forEach
            }
            GameState.addCharacter(
                export.character.copy(id = java.util.UUID.randomUUID().toString(), worldId = cible, createdBy = "MJ"),
                historiqueImporte = export.historique
            )
            resume = resume.copy(personnages = resume.personnages + 1)
        }

        // Bibliothèque : chaque .md devient un livre personnalisé de l'univers courant.
        racine.sousDossier(BIBLIOTHEQUE_DIRS).flatMap { it.fichiers(".md") }.forEach { fichier ->
            val texte = fichier.readTextOrNull(context) ?: return@forEach
            val id = java.util.UUID.randomUUID().toString()
            val fileName = "$id.md"
            try {
                File(customBooksDir(context), fileName).writeText(texte)
            } catch (e: Exception) {
                android.util.Log.e("ProfilsPackStore", "Impossible de copier ${fichier.name}", e)
                return@forEach
            }
            val nom = fichier.name?.substringBeforeLast('.')?.ifBlank { null } ?: "Contenu importé"
            CustomBooksStore.add(context, worldId, id, nom, fileName)
            val analyse = CustomContentParser.parse(texte)
            val contenu = analyse.summary
            val pnjCrees = PnjImport.creerFiches(context, analyse.pnjs, worldId ?: "donjon_et_dragon", nom)
            resume = resume.copy(
                personnages = resume.personnages + pnjCrees,
                livres = resume.livres + 1,
                monstres = resume.monstres + contenu.monsters,
                objets = resume.objets + contenu.equipment,
                autresEntrees = resume.autresEntrees + (contenu.total - contenu.monsters - contenu.pnjs - contenu.equipment),
            )
        }
        if (resume.livres > 0) SrdRepository.invalidateWorld(worldId)

        return resume
    }

    /**
     * Exporte dans [dossier] les fiches des PNJ/créatures cités par [scenarios], les membres du
     * groupe sélectionné (PJ) et les livres personnalisés de l'univers.
     */
    fun exporterProfils(context: Context, dossier: String, scenarios: List<GameState.MjScenario>, worldId: String?) {
        val personnagesDuMonde = GameState.characters.value.filter { worldId == null || it.worldId == worldId }
        val nomsCites = scenarios.flatMap { scenario ->
            val contenus = scenario.scenes.map { it.markdownContent } + scenario.markdownContent
            contenus.flatMap { contenu -> sceneProfiles(contenu, personnagesDuMonde).map { it.name.lowercase() } }
        }.toSet()
        val groupe = GameState.currentGroupId.value?.let { id -> GameState.mjGroups.value.firstOrNull { it.id == id } }
        val aExporter = personnagesDuMonde.filter {
            (it.type != "PJ" && it.name.lowercase() in nomsCites) || (groupe != null && it.id in groupe.memberIds)
        }
        aExporter.forEach { personnage ->
            PublicFilesStore.writeText(
                context = context,
                displayName = GameState.characterFileName(personnage),
                content = json.encodeToString(CharacterExport(personnage)),
                subfolder = "$dossier/$PERSONNAGES_EXPORT_DIR",
                mimeType = "application/json"
            )
        }

        CustomBooksStore.list(context, worldId)
            .filter { it.fileName.endsWith(".md", ignoreCase = true) }
            .forEach { livre ->
                val contenu = readCustomBookContent(context, livre.fileName) ?: return@forEach
                PublicFilesStore.writeText(
                    context = context,
                    displayName = "${GameState.slugify(livre.name)}.md",
                    content = contenu,
                    subfolder = "$dossier/$BIBLIOTHEQUE_EXPORT_DIR"
                )
            }
    }

    /**
     * Importe un dossier de scénario(s) avec ses profils : les .md à la racine ou dans
     * `Scenarios/`, plus `Personnages/` et `Bibliotheque/` (voir la doc de la classe).
     */
    suspend fun importerScenariosDepuisDossier(context: Context, dossierUri: Uri, worldId: String?): Result<ResumeImport> {
        val racine = DocumentFile.fromTreeUri(context, dossierUri)
            ?: return Result.failure(IllegalArgumentException("Dossier introuvable."))

        // Profils d'abord : les liens des scénarios (#pnj, #combat…) les retrouvent ainsi dès
        // la première lecture.
        var resume = importerProfils(context, racine, worldId)

        val fichiersScenarios = racine.fichiers(".md") +
            racine.sousDossier(listOf("Scenarios", "Scénarios")).flatMap { it.fichiers(".md") }
        fichiersScenarios.forEach { fichier ->
            val markdown = fichier.readTextOrNull(context)?.takeIf { it.isNotBlank() } ?: return@forEach
            val (titre, scenes) = GameState.parseScenarioMarkdown(markdown)
            GameState.addMjScenario(
                GameState.MjScenario(
                    title = titre,
                    description = GameState.preambuleScenario(markdown),
                    worldId = worldId ?: "",
                    scenes = scenes,
                    createdBy = "MJ"
                ),
                context
            )
            resume = resume.copy(scenarios = resume.scenarios + 1)
        }

        if (resume == ResumeImport()) {
            return Result.failure(IllegalArgumentException("Aucun scénario ni profil trouvé dans ce dossier."))
        }
        return Result.success(resume)
    }
}
