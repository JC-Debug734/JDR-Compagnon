package com.jc2.jdrcompagnon.feature_import

import android.content.Context
import com.jc2.jdrcompagnon.di.BoutiqueDependencies
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Marchand
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Service
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.ui.CampagneImageStore
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * Campagne livrée dans une archive .zip (fichier `campagne.json`, en général dans un dossier
 * `Campagne/`, avec l'image de sa carte) : elle relie les scénarios, les fiches PNJ et le livre
 * importés par la même archive, et crée sa carte, ses lieux (villes, donjons...), les lieux
 * notables des villes et leurs boutiques.
 *
 * Format :
 * ```json
 * { "titre": "…", "description": "…", "image": "presentation.webp", "calendrier": "harptos",
 *   "objectifs": ["…"], "scenarios": ["Titre de scénario", …],
 *   "carte": { "nom": "…", "image": "carte.jpg", "largeurCases": 35, "hauteurCases": 48, "echelleKmParCase": 5 },
 *   "lieux": [ { "nom": "…", "type": "VILLE", "x": 20, "y": 37, "description": "…",
 *                "scenarios": ["Titre"], "lieuxNotables": [{ "nom": "…", "description": "…" }],
 *                "boutiques": [{ "nom": "…", "type": "MARCHAND", "standing": "MODESTE",
 *                                "marchand": { "nom": "…", "description": "…", "trait": "…" },
 *                                "argent": 200, "articles": [{ "nom": "…", "prix": 1, "type": "Équipement", "quantite": 5 }],
 *                                "services": [{ "nom": "…", "description": "…", "prix": 1 }] }] } ] }
 * ```
 * Les scénarios sont désignés par leur titre ; une campagne non sélectionnée masque son contenu
 * (voir PorteeCampagne).
 */
object CampagneArchive {

    const val NOM_FICHIER = "campagne.json"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    data class Dto(
        val titre: String,
        val description: String = "",
        // Image de la page de présentation (fichier voisin de campagne.json).
        val image: String? = null,
        val calendrier: String? = null,
        val objectifs: List<String> = emptyList(),
        val scenarios: List<String> = emptyList(),
        val carte: CarteDto? = null,
        val lieux: List<LieuDto> = emptyList(),
    )

    @Serializable
    data class CarteDto(
        val nom: String = CarteCampagne.NOM_CARTE_PRINCIPALE,
        val image: String? = null,
        val largeurCases: Int = 20,
        val hauteurCases: Int = 15,
        val echelleKmParCase: Int = 10,
    )

    @Serializable
    data class LieuDto(
        val nom: String,
        val type: String = "AUTRE",
        val x: Int = 0,
        val y: Int = 0,
        val description: String = "",
        val scenarios: List<String> = emptyList(),
        val lieuxNotables: List<LieuNotableDto> = emptyList(),
        val boutiques: List<BoutiqueDto> = emptyList(),
    )

    @Serializable
    data class LieuNotableDto(val nom: String, val description: String = "")

    @Serializable
    data class BoutiqueDto(
        val nom: String,
        val type: String = "MARCHAND",
        val standing: String = "CORRECT",
        val marchand: MarchandDto = MarchandDto("Marchand"),
        val argent: Int = 0,
        val articles: List<ArticleDto> = emptyList(),
        val services: List<ServiceDto> = emptyList(),
    )

    @Serializable
    data class MarchandDto(val nom: String, val description: String = "", val trait: String = "")

    @Serializable
    data class ArticleDto(val nom: String, val prix: Int, val type: String = "Équipement", val quantite: Int = 1)

    @Serializable
    data class ServiceDto(val nom: String, val description: String = "", val prix: Int = 0)

    /** Contenu lu dans l'archive extraite : le json et les images de carte et de présentation (lues avant le nettoyage du dossier). */
    class Contenu(val dto: Dto, val imageCarte: ByteArray?, val imageCampagne: ByteArray? = null)

    /** Cherche `campagne.json` dans le dossier extrait ; null s'il n'y en a pas ou s'il est invalide. */
    fun lire(dossier: File): Contenu? {
        val fichier = dossier.walkTopDown().firstOrNull { it.isFile && it.name.equals(NOM_FICHIER, ignoreCase = true) } ?: return null
        val dto = runCatching { json.decodeFromString(Dto.serializer(), fichier.readText()) }
            .getOrNull() ?: return null // json invalide : l'archive est importée sans campagne
        fun image(nom: String?) = nom?.let { File(fichier.parentFile, it).takeIf { f -> f.isFile } ?: File(dossier, it).takeIf { f -> f.isFile } }
        return Contenu(dto, image(dto.carte?.image)?.readBytes(), image(dto.image)?.readBytes())
    }

    /**
     * Crée la campagne [contenu] : [scenarioIdsParTitre] (scénarios de la même archive, et à
     * défaut ceux déjà présents dans le monde), [pnjIds], [livreId] et les [monstres] du livre
     * (son « Bestiaire attaché ») lui sont rattachés. La campagne est sélectionnée. Retourne le
     * compte rendu.
     */
    suspend fun importer(
        context: Context,
        contenu: Contenu,
        worldId: String,
        scenarioIdsParTitre: Map<String, String>,
        pnjIds: List<String>,
        livreId: String?,
        monstres: List<String> = emptyList(),
    ): String {
        val dto = contenu.dto
        val campagneId = UUID.randomUUID().toString()
        val existants = GameState.mjScenarios.value.filter { it.worldId == worldId }
        fun scenarioId(titre: String): String? =
            scenarioIdsParTitre.entries.firstOrNull { it.key.equals(titre, ignoreCase = true) }?.value
                ?: existants.lastOrNull { it.title.equals(titre, ignoreCase = true) }?.id
        // Scénarios dans l'ordre de campagne, puis ceux de l'archive qu'elle ne cite pas.
        val scenarioIds = (dto.scenarios.mapNotNull { scenarioId(it) } + scenarioIdsParTitre.values).distinct()

        val calendrierId = dto.calendrier?.takeIf { it.contains("harptos", ignoreCase = true) || it.contains("harpiste", ignoreCase = true) }?.let {
            ScenarioClockState.calendars.value.firstOrNull { c -> c.nom.contains("Harpistes", ignoreCase = true) }?.id
                ?: ScenarioClockState.addHarptosCalendar().id
        }

        // Carte principale (id = campagneId) et lieux.
        var lieux = 0
        var boutiques = 0
        dto.carte?.let { carte ->
            val image = contenu.imageCarte?.let { CarteImageStore.copierBytes(context, campagneId, it) }
            CarteDependencies.repository.sauvegarderCarte(
                CarteCampagne(
                    campagneId = campagneId,
                    largeurCases = carte.largeurCases,
                    hauteurCases = carte.hauteurCases,
                    echelleKmParCase = carte.echelleKmParCase,
                    imageFileName = image,
                    nom = carte.nom,
                )
            )
        }
        dto.lieux.forEach { lieu ->
            val pointId = UUID.randomUUID().toString()
            val boutiqueIds = lieu.boutiques.map { b ->
                val boutique = Boutique(
                    id = UUID.randomUUID().toString(),
                    nom = b.nom,
                    standing = enumOu(b.standing, StandingBoutique.CORRECT),
                    marchand = Marchand(b.marchand.nom, b.marchand.description, b.marchand.trait),
                    employes = emptyList(),
                    inventaire = b.articles.map { a ->
                        ArticleEnVente(EquipementReference(a.nom, a.prix, a.type), a.prix, a.quantite, toujoursDisponible = true)
                    },
                    type = enumOu(b.type, TypeBoutique.MARCHAND),
                    services = b.services.map { s -> Service(s.nom, s.description, s.prix, personnalise = true) },
                    argentDisponibleEnPo = b.argent,
                    villeId = pointId,
                )
                BoutiqueDependencies.repository.sauvegarderBoutique(boutique)
                boutiques++
                boutique.id
            }
            CarteDependencies.repository.sauvegarderPoint(
                PointInteret(
                    id = pointId,
                    campagneId = campagneId,
                    nom = lieu.nom,
                    type = enumOu(lieu.type, TypePointInteret.AUTRE),
                    x = lieu.x,
                    y = lieu.y,
                    description = lieu.description,
                    boutiqueIds = boutiqueIds,
                    scenarioIds = lieu.scenarios.mapNotNull { scenarioId(it) },
                    carteId = if (dto.carte != null) campagneId else null,
                )
            )
            lieu.lieuxNotables.forEach { notable ->
                CarteDependencies.repository.sauvegarderLieuNotable(
                    LieuNotable(UUID.randomUUID().toString(), pointId, notable.nom, notable.description)
                )
            }
            lieux++
        }

        GameState.addMjCampaign(
            GameState.MjCampaign(
                id = campagneId,
                title = dto.titre,
                description = dto.description,
                worldId = worldId,
                scenarioIds = scenarioIds,
                // Objectifs du dossier de campagne : quêtes en cours, cachées aux joueurs.
                quests = dto.objectifs.filter { it.isNotBlank() }.map { com.jc2.jdrcompagnon.feature_quete.domain.model.Quest(title = it.trim()) },
                calendarId = calendrierId,
                pnjIds = pnjIds.distinct(),
                livreIds = listOfNotNull(livreId),
                monsterIds = monstres.distinctBy { it.lowercase() },
                imageFileName = contenu.imageCampagne?.let { CampagneImageStore.ecrire(context, campagneId, it) },
            )
        )
        GameState.setCurrentCampaignId(campagneId)

        return "Campagne « ${dto.titre} » (sélectionnée) : ${scenarioIds.size} scénario(s), ${pnjIds.size} fiche(s), " +
            "${monstres.distinctBy { it.lowercase() }.size} monstre(s) au bestiaire, " +
            "$lieux lieu(x) sur la carte, $boutiques boutique(s)."
    }

    /** Supprime la carte, les lieux (et leurs lieux notables) et les boutiques d'une campagne. */
    suspend fun supprimerDonnees(campagneId: String) {
        val repo = CarteDependencies.repository
        val points = repo.observerPoints(campagneId).first()
        points.forEach { point ->
            repo.observerLieuxNotables(point.id).first().forEach { repo.supprimerLieuNotable(it.id) }
            point.boutiqueIds.forEach { BoutiqueDependencies.repository.supprimerBoutique(it) }
            repo.supprimerPoint(point.id)
        }
        BoutiqueDependencies.repository.observerToutesLesBoutiques().first()
            .filter { b -> points.any { it.id == b.villeId } }
            .forEach { BoutiqueDependencies.repository.supprimerBoutique(it.id) }
        repo.observerCartes(campagneId).first().forEach { repo.supprimerCarte(it) }
    }

    private inline fun <reified E : Enum<E>> enumOu(nom: String, defaut: E): E =
        enumValues<E>().firstOrNull { it.name.equals(nom.trim(), ignoreCase = true) } ?: defaut
}
