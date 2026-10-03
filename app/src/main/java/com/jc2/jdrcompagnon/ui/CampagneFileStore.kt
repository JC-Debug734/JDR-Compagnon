package com.jc2.jdrcompagnon.ui

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.jc2.jdrcompagnon.di.BoutiqueDependencies
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Export/import "à plat" des données d'une campagne (scénarios attachés, carte + points
 * d'intérêt, boutiques des villes de la campagne) sous forme de fichiers, dans un dossier
 * nommé d'après la campagne. Vient EN PLUS de Room/SharedPreferences (qui restent la source de
 * vérité de l'app) : sert à partager/sauvegarder une campagne en dehors de l'app, ou à la
 * recharger sur un autre appareil via [importerCampagneDepuisDossier].
 *
 * L'export réutilise PublicFilesStore (déjà utilisé pour les .md de scénarios), dans
 * Téléchargements/JDRCompagnon/Campagnes/<slug-titre>-<id8>/.
 */
object CampagneFileStore {

    private const val CAMPAGNES_SUBFOLDER = "Campagnes"
    private const val CAMPAGNE_META_FILE = "campagne.json"
    private const val CARTE_FILE = "carte.json"
    private const val CARTE_IMAGE_FILE = "carte_image"
    private const val CAMPAGNE_IMAGE_FILE = "campagne_image"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    @Serializable
    private data class CarteExportDto(
        val carte: CarteCampagne,
        val points: List<PointInteret>,
        val imageFileName: String? = null,
        // Cartes en plus de la carte principale ; image de la i-ème : "carte_image_i".
        val autresCartes: List<CarteCampagne> = emptyList()
    )

    private fun campagneFolderName(campagne: GameState.MjCampaign): String =
        "${GameState.slugify(campagne.title)}-${campagne.id.take(8)}"

    private fun campagneSubfolder(campagne: GameState.MjCampaign): String =
        "$CAMPAGNES_SUBFOLDER/${campagneFolderName(campagne)}"

    /** Exporte l'intégralité des données de la campagne dans son dossier dédié. */
    suspend fun exporterCampagne(context: Context, campagne: GameState.MjCampaign) {
        val dossier = campagneSubfolder(campagne)

        PublicFilesStore.writeText(
            context = context,
            displayName = CAMPAGNE_META_FILE,
            content = json.encodeToString(campagne),
            subfolder = dossier,
            mimeType = "application/json"
        )

        campagne.imageFileName?.let { CampagneImageStore.fichier(context, it) }?.takeIf { it.exists() }?.let { fichier ->
            PublicFilesStore.writeBytes(
                context = context,
                displayName = CAMPAGNE_IMAGE_FILE,
                content = fichier.readBytes(),
                subfolder = dossier,
                mimeType = "application/octet-stream"
            )
        }

        val scenarios = GameState.mjScenarios.value.filter { it.id in campagne.scenarioIds }
        scenarios.forEach { scenario ->
            PublicFilesStore.writeText(
                context = context,
                displayName = GameState.scenarioFileName(scenario),
                content = GameState.scenarioToMarkdown(scenario),
                subfolder = "$dossier/Scenarios"
            )
        }

        // Profils personnalisés : PNJ/créatures cités, PJ du groupe, contenu de bibliothèque.
        ProfilsPackStore.exporterProfils(context, dossier, scenarios, campagne.worldId.ifBlank { null })

        val toutesLesCartes = CarteDependencies.repository.observerCartes(campagne.id).first()
        val carte = toutesLesCartes.firstOrNull() ?: CarteCampagne(campagneId = campagne.id)
        val autresCartes = toutesLesCartes.drop(1)
        val points = CarteDependencies.repository.observerPoints(campagne.id).first()
        PublicFilesStore.writeText(
            context = context,
            displayName = CARTE_FILE,
            content = json.encodeToString(CarteExportDto(carte, points, carte.imageFileName, autresCartes)),
            subfolder = "$dossier/Carte",
            mimeType = "application/json"
        )
        suspend fun exporterImage(fileName: String?, displayName: String) {
            val fichier = fileName?.let { CarteImageStore.fichier(context, it) } ?: return
            if (fichier.exists()) {
                PublicFilesStore.writeBytes(
                    context = context,
                    displayName = displayName,
                    content = fichier.readBytes(),
                    subfolder = "$dossier/Carte",
                    mimeType = "application/octet-stream"
                )
            }
        }
        exporterImage(carte.imageFileName, CARTE_IMAGE_FILE)
        autresCartes.forEachIndexed { index, autre -> exporterImage(autre.imageFileName, "${CARTE_IMAGE_FILE}_$index") }

        val villeIds = points.filter { it.type == TypePointInteret.VILLE }.map { it.id }.toSet()
        val boutiques = BoutiqueDependencies.repository.observerToutesLesBoutiques().first()
            .filter { it.villeId != null && it.villeId in villeIds }
        boutiques.forEach { boutique ->
            PublicFilesStore.writeText(
                context = context,
                displayName = "boutique-${boutique.id}.json",
                content = json.encodeToString(boutique),
                subfolder = "$dossier/Boutiques",
                mimeType = "application/json"
            )
        }
    }

    private fun DocumentFile.readTextOrNull(context: Context): String? = try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
    } catch (e: Exception) {
        android.util.Log.e("CampagneFileStore", "Impossible de lire ${this.name}", e)
        null
    }

    private fun DocumentFile.readBytesOrNull(context: Context): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (e: Exception) {
        android.util.Log.e("CampagneFileStore", "Impossible de lire ${this.name}", e)
        null
    }

    /**
     * Importe une campagne depuis un dossier choisi par l'utilisateur (ACTION_OPEN_DOCUMENT_TREE),
     * en recréant tout avec de nouveaux identifiants pour éviter toute collision avec des données
     * existantes.
     */
    suspend fun importerCampagneDepuisDossier(
        context: Context,
        dossierUri: Uri,
    ): Result<Pair<GameState.MjCampaign, ProfilsPackStore.ResumeImport>> {
        val racine = DocumentFile.fromTreeUri(context, dossierUri)
            ?: return Result.failure(IllegalArgumentException("Dossier introuvable."))

        val metaFile = racine.findFile(CAMPAGNE_META_FILE)
            ?: return Result.failure(IllegalArgumentException("Aucun fichier $CAMPAGNE_META_FILE trouvé dans ce dossier."))
        val metaJson = metaFile.readTextOrNull(context)
            ?: return Result.failure(IllegalStateException("Impossible de lire $CAMPAGNE_META_FILE."))
        val campagneImportee = try {
            json.decodeFromString<GameState.MjCampaign>(metaJson)
        } catch (e: Exception) {
            return Result.failure(e)
        }

        val nouvelId = java.util.UUID.randomUUID().toString()

        // Profils personnalisés (Personnages/, Bibliotheque/) avant les scénarios, pour que
        // leurs liens (#pnj, #combat…) les retrouvent dès la première lecture.
        var resume = ProfilsPackStore.importerProfils(context, racine, GameState.currentWorldId())

        // Scénarios : chaque .md est réimporté comme un nouveau scénario, en conservant une
        // correspondance ancien id (commentaire caché) -> nouvel id pour remapper scenarioIds.
        val ancienVersNouveauScenarioId = mutableMapOf<String, String>()
        racine.findFile("Scenarios")?.listFiles()
            ?.filter { it.isFile && it.name?.endsWith(".md", ignoreCase = true) == true }
            ?.forEach { fichier ->
                val markdown = fichier.readTextOrNull(context) ?: return@forEach
                val ancienId = GameState.hiddenScenarioId(markdown)
                val (titre, scenes) = GameState.parseScenarioMarkdown(markdown)
                val nouveauScenario = GameState.MjScenario(
                    title = titre,
                    description = GameState.preambuleScenario(markdown),
                    worldId = GameState.currentWorldId() ?: "",
                    scenes = scenes,
                    createdBy = "MJ"
                )
                GameState.addMjScenario(nouveauScenario, context)
                resume = resume.copy(scenarios = resume.scenarios + 1)
                if (ancienId != null) ancienVersNouveauScenarioId[ancienId] = nouveauScenario.id
            }

        // Carte + points d'intérêt.
        val ancienVersNouveauPointId = mutableMapOf<String, String>()
        val dossierCarte = racine.findFile("Carte")
        val carteJson = dossierCarte?.findFile(CARTE_FILE)?.readTextOrNull(context)
        if (dossierCarte != null && carteJson != null) {
            try {
                val carteDto = json.decodeFromString<CarteExportDto>(carteJson)
                var nouvelleImage: String? = null
                dossierCarte.findFile(CARTE_IMAGE_FILE)?.readBytesOrNull(context)?.let { bytes ->
                    nouvelleImage = CarteImageStore.copierBytes(context, nouvelId, bytes)
                }
                CarteDependencies.repository.sauvegarderCarte(
                    carteDto.carte.copy(campagneId = nouvelId, id = nouvelId, imageFileName = nouvelleImage)
                )
                // Autres cartes : nouveaux ids, les points y sont remappés ci-dessous.
                val ancienVersNouvelleCarteId = mutableMapOf(carteDto.carte.id to nouvelId)
                // Export antérieur aux cartes multiples : aucun point n'a de carteId, ils étaient
                // tous sur l'unique carte de la campagne.
                val exportUneSeuleCarte = carteDto.autresCartes.isEmpty() && carteDto.points.all { it.carteId == null }
                carteDto.autresCartes.forEachIndexed { index, autre ->
                    val nouvelleCarteId = java.util.UUID.randomUUID().toString()
                    ancienVersNouvelleCarteId[autre.id] = nouvelleCarteId
                    val image = dossierCarte.findFile("${CARTE_IMAGE_FILE}_$index")?.readBytesOrNull(context)
                        ?.let { bytes -> CarteImageStore.copierBytes(context, nouvelleCarteId, bytes) }
                    CarteDependencies.repository.sauvegarderCarte(
                        autre.copy(campagneId = nouvelId, id = nouvelleCarteId, imageFileName = image)
                    )
                }
                carteDto.points.forEach { point ->
                    val nouveauPointId = java.util.UUID.randomUUID().toString()
                    ancienVersNouveauPointId[point.id] = nouveauPointId
                    CarteDependencies.repository.sauvegarderPoint(
                        point.copy(
                            id = nouveauPointId,
                            campagneId = nouvelId,
                            // Lieu non placé (ou carte inconnue) : null ; sinon sa nouvelle carte.
                            carteId = if (exportUneSeuleCarte) nouvelId
                                else point.carteId?.let { ancienVersNouvelleCarteId[it] },
                            boutiqueIds = emptyList(),
                            scenarioIds = point.scenarioIds.mapNotNull { ancienVersNouveauScenarioId[it] }
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("CampagneFileStore", "Impossible d'importer la carte", e)
            }
        }

        // Boutiques : nouvel id, villeId remappé vers le nouveau point, puis on ré-attache les
        // boutiques à leur point (2e passe, car un point est créé avec boutiqueIds vide ci-dessus).
        val boutiquesParNouveauPoint = mutableMapOf<String, MutableList<String>>()
        racine.findFile("Boutiques")?.listFiles()
            ?.filter { it.isFile && it.name?.endsWith(".json", ignoreCase = true) == true }
            ?.forEach { fichier ->
                val texte = fichier.readTextOrNull(context) ?: return@forEach
                try {
                    val boutique = json.decodeFromString<Boutique>(texte)
                    val nouveauVilleId = boutique.villeId?.let { ancienVersNouveauPointId[it] }
                    val nouvelleBoutique = boutique.copy(id = java.util.UUID.randomUUID().toString(), villeId = nouveauVilleId)
                    BoutiqueDependencies.repository.sauvegarderBoutique(nouvelleBoutique)
                    if (nouveauVilleId != null) {
                        boutiquesParNouveauPoint.getOrPut(nouveauVilleId) { mutableListOf() }.add(nouvelleBoutique.id)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CampagneFileStore", "Impossible d'importer ${fichier.name}", e)
                }
            }
        if (boutiquesParNouveauPoint.isNotEmpty()) {
            val points = CarteDependencies.repository.observerPoints(nouvelId).first()
            points.filter { it.id in boutiquesParNouveauPoint }.forEach { point ->
                CarteDependencies.repository.sauvegarderPoint(
                    point.copy(boutiqueIds = boutiquesParNouveauPoint[point.id].orEmpty())
                )
            }
        }

        val nouvelleCampagne = campagneImportee.copy(
            id = nouvelId,
            worldId = GameState.currentWorldId() ?: campagneImportee.worldId,
            scenarioIds = campagneImportee.scenarioIds.mapNotNull { ancienVersNouveauScenarioId[it] },
            imageFileName = racine.findFile(CAMPAGNE_IMAGE_FILE)?.readBytesOrNull(context)
                ?.let { bytes -> CampagneImageStore.ecrire(context, nouvelId, bytes) }
        )
        GameState.addMjCampaign(nouvelleCampagne)

        return Result.success(nouvelleCampagne to resume)
    }
}
