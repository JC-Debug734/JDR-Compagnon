package com.jc2.jdrcompagnon.feature_import

import android.content.Context
import com.jc2.jdrcompagnon.di.BoutiqueDependencies
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.di.EnvironmentDependencies
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementImageStore
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.ProfilEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueImageStore
import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Marchand
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Service
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.data.VilleImageStore
import com.jc2.jdrcompagnon.feature_carte.data.VilleEmblemeStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.ui.CampagneImageStore
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ImportedMusicStore
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.availableLoopTracks
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
 *                                "argent": 200, "articles": [{ "nom": "…", "prix": 0.1, "type": "Équipement", "quantite": 5 }],
 *                                "services": [{ "nom": "…", "description": "…", "prix": 1 }], "enseigne": "forge" }] } ] }
 * ```
 * Montants des boutiques ("argent", "prix") en po, décimales permises (0.1 = 1 pa, 0.01 = 1 pc).
 * Les scénarios sont désignés par leur titre ; une campagne non sélectionnée masque son contenu
 * (voir PorteeCampagne). Un lieu peut aussi avoir une illustration (`"image": "camp.webp"`, fichier
 * voisin du json, voir VilleImageStore) et une icône de carte (`"icone": "campement"`, clé de
 * IconesPointInteret.PALETTE), un emblème de ville (`"embleme": "port"`, clé de VilleEmblemeStore),
 * être montré aux joueurs dès le départ (`"visible": true`) et porter
 * des événements (`"evenements": [ { "titre": "…", "type": "RUMEUR_INDICE", "description": "…",
 * "effets": [{ "categorie": "LIEU", "cible": "Nom du lieu" }], "issues": [ … ] } ]`, voir
 * [EvenementDto]). `"environnements": [{ "nom": "…", "evenements": [ … ] }]` crée des
 * environnements ; `"environnement": "nom"` désigne celui de la campagne, dont les événements sont
 * proposés à chaque étape de voyage. Un environnement peut porter sa musique d'ambiance
 * (`"musique": "musique/theme.mp3", "musiqueNom": "…"`, fichier audio de l'archive ajouté aux
 * musiques importées, ou id / nom d'une piste existante).
 * `"evenements": [ { "titre": "L'ogre congelé", "type": "DECOUVERTE", "image": "evenements/ogre.webp", … } ]`
 * crée les événements de la campagne dans l'outil ÉVÉNEMENTS (avec leur illustration, montrée
 * sur la table quand le MJ les joue) ; les scénarios les appellent par `#evenement:[Titre]`.
 *
 * Lieux seuls, sans campagne : fichier `lieux.json` (`{ "lieux": [ … ] }`, même format de lieu)
 * ajoutés à la campagne sélectionnée, non placés sur la carte ([importerLieux]).
 */
object CampagneArchive {

    const val NOM_FICHIER = "campagne.json"
    const val NOM_FICHIER_LIEUX = "lieux.json"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    data class Dto(
        val titre: String,
        val description: String = "",
        // Image de la page de présentation (fichier voisin de campagne.json).
        val image: String? = null,
        val calendrier: String? = null,
        // Ancien format : un titre de quête par ligne (ignoré si [quetes] est renseigné).
        val objectifs: List<String> = emptyList(),
        val quetes: List<QueteDto> = emptyList(),
        val scenarios: List<String> = emptyList(),
        val carte: CarteDto? = null,
        val lieux: List<LieuDto> = emptyList(),
        // Environnements créés avec la campagne ; [environnement] nomme celui de la campagne, dont
        // les événements sont proposés à chaque étape de voyage (à défaut : le premier).
        val environnements: List<EnvironnementDto> = emptyList(),
        val environnement: String? = null,
        // Événements de l'outil ÉVÉNEMENTS apportés par la campagne, appelés depuis les scénarios
        // par `#evenement:[Titre]` (les scénarios ne décrivent pas d'événement eux-mêmes).
        val evenements: List<EvenementDto> = emptyList(),
    )

    /**
     * Environnement de la campagne : un environnement du même nom déjà présent dans le monde est
     * réutilisé (ses événements de campagne remplacés) plutôt que dupliqué à chaque réimport.
     */
    @Serializable
    data class EnvironnementDto(
        val nom: String,
        val description: String = "",
        val terrains: List<String> = emptyList(),
        val evenements: List<EvenementDto> = emptyList(),
        // Musique d'ambiance : fichier audio de l'archive (voisin du json, ex. "musique/frozen-star.mp3"),
        // ajouté aux musiques importées, ou id / nom d'une piste déjà présente dans l'app.
        val musique: String? = null,
        // Nom de la piste importée (à défaut : le nom du fichier).
        val musiqueNom: String? = null,
    )

    /**
     * Événement de la bibliothèque créé avec la campagne (rattaché à un lieu ou à un
     * environnement, ou à la campagne elle-même) : [type] est un TypeEvenement (RENCONTRE par défaut), [monstres] et [pnj] des
     * noms avec quantité facultative (« Orc x4 »), [effets] et [issues] au format de l'app.
     */
    @Serializable
    data class EvenementDto(
        val titre: String,
        val type: String = "RENCONTRE",
        val description: String = "",
        val objectif: String = "",
        val monstres: List<String> = emptyList(),
        val pnj: List<String> = emptyList(),
        val effets: List<EffetDto> = emptyList(),
        val issues: List<IssueDto> = emptyList(),
        // Illustration (fichier voisin du json, à défaut à la racine de l'archive), montrée sur la
        // page d'affichage table quand le MJ joue l'événement.
        val image: String? = null,
    )

    /** Effet d'événement : [categorie] est une CategorieEffet (LIEU : révèle le lieu nommé [cible]). */
    @Serializable
    data class EffetDto(
        val categorie: String = "AUTRE",
        val gain: Boolean = true,
        val quantite: Int = 1,
        val cible: String = "",
        val description: String = "",
    )

    /** Issue d'événement : [nature] est une NatureIssue ; [combat] = l'issue mène au combat. */
    @Serializable
    data class IssueDto(
        val nature: String = "REUSSITE",
        val titre: String = "",
        val description: String = "",
        val effets: List<EffetDto> = emptyList(),
        val combat: Boolean = false,
    )

    /**
     * Quête de la campagne, au format des quêtes de l'app ([com.jc2.jdrcompagnon.feature_quete.domain.model.Quest]) :
     * [description] est le texte montré aux joueurs, [notesMj] reste réservé au MJ, [donneur] est le
     * nom d'une fiche PNJ (créée par un scénario de l'archive), [statut] un QuestStatus (EN_ATTENTE
     * par défaut). Récompenses : type QuestRewardType (XP, OR, EQUIPEMENT, REPUTATION...), voir [RecompenseDto].
     */
    @Serializable
    data class QueteDto(
        val titre: String,
        val description: String = "",
        val notesMj: String = "",
        val lieu: String = "",
        val donneur: String? = null,
        val statut: String = "EN_ATTENTE",
        val visible: Boolean = false,
        val recompensesVisibles: Boolean = false,
        val recompenses: List<RecompenseDto> = emptyList(),
    )

    @Serializable
    data class RecompenseDto(val type: String, val montant: Int = 0, val libelle: String = "", val detail: String = "")

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
        // Position précise du centre de l'icône, en fractions de l'image de la carte (0..1, voir
        // PointInteret.fx) ; à défaut, la case [x], [y].
        val fx: Float? = null,
        val fy: Float? = null,
        val description: String = "",
        val scenarios: List<String> = emptyList(),
        val lieuxNotables: List<LieuNotableDto> = emptyList(),
        val boutiques: List<BoutiqueDto> = emptyList(),
        // Illustration du lieu (fichier voisin du json) et icône sur la carte (clé de IconesPointInteret.PALETTE).
        val image: String? = null,
        val icone: String? = null,
        // Lieu montré aux joueurs dès le départ (sinon révélé plus tard par le MJ ou un événement).
        val visible: Boolean = false,
        // Événements de la bibliothèque rattachés au lieu (ex. les rumeurs d'une ville).
        val evenements: List<EvenementDto> = emptyList(),
        // Emblème de la ville (clé de VilleEmblemeStore, ex. "port"), miniature des listes de villes.
        val embleme: String? = null,
    )

    @Serializable
    data class LieuxDto(val lieux: List<LieuDto> = emptyList())

    /** Lieux seuls lus dans `lieux.json`, avec leurs images (par nom de lieu). */
    class LieuxContenu(val lieux: List<LieuDto>, val images: Map<String, ByteArray>, val imagesEvenements: Map<String, ByteArray> = emptyMap())

    @Serializable
    data class LieuNotableDto(val nom: String, val description: String = "")

    @Serializable
    data class BoutiqueDto(
        val nom: String,
        val type: String = "MARCHAND",
        val standing: String = "CORRECT",
        val marchand: MarchandDto = MarchandDto("Marchand"),
        // Montants en po, décimales permises (0.5 = 5 pa), convertis en pièces de cuivre à l'import.
        val argent: Double = 0.0,
        val articles: List<ArticleDto> = emptyList(),
        val services: List<ServiceDto> = emptyList(),
        // Clé d'une enseigne fournie avec l'app (BoutiqueImageStore.ENSEIGNES), ex. "forge".
        val enseigne: String? = null,
    )

    @Serializable
    data class MarchandDto(val nom: String, val description: String = "", val trait: String = "")

    @Serializable
    data class ArticleDto(val nom: String, val prix: Double, val type: String = "Équipement", val quantite: Int = 1)

    @Serializable
    data class ServiceDto(val nom: String, val description: String = "", val prix: Double = 0.0)

    /** Contenu lu dans l'archive extraite : le json et les images de carte et de présentation (lues avant le nettoyage du dossier). */
    class Contenu(
        val dto: Dto,
        val imageCarte: ByteArray?,
        val imageCampagne: ByteArray? = null,
        val imagesLieux: Map<String, ByteArray> = emptyMap(),
        // Illustrations des événements, par nom de fichier (EvenementDto.image).
        val imagesEvenements: Map<String, ByteArray> = emptyMap(),
        // Fichiers audio des environnements, par nom de fichier (EnvironnementDto.musique).
        val musiques: Map<String, ByteArray> = emptyMap(),
    )

    /** Cherche `campagne.json` dans le dossier extrait ; null s'il n'y en a pas ou s'il est invalide. */
    fun lire(dossier: File): Contenu? {
        val fichier = dossier.walkTopDown().firstOrNull { it.isFile && it.name.equals(NOM_FICHIER, ignoreCase = true) } ?: return null
        val dto = runCatching { json.decodeFromString(Dto.serializer(), fichier.readText()) }
            .getOrNull() ?: return null // json invalide : l'archive est importée sans campagne
        fun image(nom: String?) = imageVoisine(dossier, fichier, nom)
        val evenements = dto.evenements + dto.lieux.flatMap { it.evenements } + dto.environnements.flatMap { it.evenements }
        return Contenu(
            dto, image(dto.carte?.image)?.readBytes(), image(dto.image)?.readBytes(), imagesLieux(dossier, fichier, dto.lieux),
            imagesEvenements(dossier, fichier, evenements),
            dto.environnements.mapNotNull { it.musique }.distinct()
                .mapNotNull { nom -> imageVoisine(dossier, fichier, nom)?.let { nom to it.readBytes() } }.toMap(),
        )
    }

    /** Cherche `lieux.json` dans le dossier extrait ; null s'il n'y en a pas, s'il est invalide ou vide. */
    fun lireLieux(dossier: File): LieuxContenu? {
        val fichier = dossier.walkTopDown().firstOrNull { it.isFile && it.name.equals(NOM_FICHIER_LIEUX, ignoreCase = true) } ?: return null
        val lieux = runCatching { json.decodeFromString(LieuxDto.serializer(), fichier.readText()) }
            .getOrNull()?.lieux?.filter { it.nom.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: return null
        return LieuxContenu(lieux, imagesLieux(dossier, fichier, lieux), imagesEvenements(dossier, fichier, lieux.flatMap { it.evenements }))
    }

    /** Fichier [nom] à côté du json, à défaut à la racine de l'archive. */
    private fun imageVoisine(dossier: File, json: File, nom: String?): File? =
        nom?.let { File(json.parentFile, it).takeIf { f -> f.isFile } ?: File(dossier, it).takeIf { f -> f.isFile } }

    private fun imagesLieux(dossier: File, json: File, lieux: List<LieuDto>): Map<String, ByteArray> =
        lieux.mapNotNull { lieu -> imageVoisine(dossier, json, lieu.image)?.let { lieu.nom to it.readBytes() } }.toMap()

    private fun imagesEvenements(dossier: File, json: File, evenements: List<EvenementDto>): Map<String, ByteArray> =
        evenements.mapNotNull { it.image }.distinct().mapNotNull { nom -> imageVoisine(dossier, json, nom)?.let { nom to it.readBytes() } }.toMap()

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
        dto.lieux.forEach { lieu0 ->
            // "x"/"y" sont des cases de la grille de la carte ("largeurCases" × "hauteurCases") :
            // converties en position libre sur l'image (centre de la case). Sans cela l'écran de
            // carte les lisait comme des cases de sa grille de référence (36 dp), et les lieux
            // atterrissaient n'importe où (souvent collés au bord droit) sur une grande image.
            val lieu = dto.carte?.takeIf { lieu0.fx == null || lieu0.fy == null }
                ?.takeIf { it.largeurCases > 0 && it.hauteurCases > 0 }
                ?.let { c -> lieu0.copy(fx = (lieu0.x + 0.5f) / c.largeurCases, fy = (lieu0.y + 0.5f) / c.hauteurCases) }
                ?: lieu0
            boutiques += creerLieu(
                context, lieu, campagneId, worldId,
                carteId = if (dto.carte != null) campagneId else null,
                image = contenu.imagesLieux[lieu.nom],
                scenarioIds = lieu.scenarios.mapNotNull { scenarioId(it) },
                imagesEvenements = contenu.imagesEvenements,
            )
            lieux++
        }
        val environnementIds = dto.environnements.filter { it.nom.isNotBlank() }
            .associate { it.nom to creerEnvironnement(context, it, campagneId, worldId, contenu.imagesEvenements, contenu.musiques) }
        val environnementId = (dto.environnement?.let { nom -> environnementIds.entries.firstOrNull { it.key.equals(nom.trim(), ignoreCase = true) }?.value }
            ?: environnementIds.values.firstOrNull())

        // Événements de la campagne : dans l'outil ÉVÉNEMENTS, appelés par les scénarios (#evenement:[Titre]).
        val evenementsCampagne = dto.evenements.filter { it.titre.isNotBlank() }
            .map { avecImage(context, evenement(it, campagneId, worldId), it, contenu.imagesEvenements) }
        if (evenementsCampagne.isNotEmpty()) EvenementDependencies.repository.sauvegarderTous(evenementsCampagne)

        GameState.addMjCampaign(
            GameState.MjCampaign(
                id = campagneId,
                title = dto.titre,
                description = dto.description,
                worldId = worldId,
                scenarioIds = scenarioIds,
                // Objectifs du dossier de campagne, cachés aux joueurs : seul le premier (le point de
                // départ, ex. lire le panneau de quêtes) est en cours ; les suivants sont en attente
                // jusqu'à ce que le MJ les reprenne quand les PJ les découvrent.
                quests = if (dto.quetes.isNotEmpty()) quetes(dto.quetes, worldId, pnjIds) else dto.objectifs.filter { it.isNotBlank() }.mapIndexed { index, titre ->
                    com.jc2.jdrcompagnon.feature_quete.domain.model.Quest(
                        title = titre.trim(),
                        status = if (index == 0) com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_COURS
                        else com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_ATTENTE,
                    )
                },
                calendarId = calendrierId,
                pnjIds = pnjIds.distinct(),
                livreIds = listOfNotNull(livreId),
                monsterIds = monstres.distinctBy { it.lowercase() },
                imageFileName = contenu.imageCampagne?.let { CampagneImageStore.ecrire(context, campagneId, it) },
                environnementId = environnementId,
            )
        )
        GameState.setCurrentCampaignId(campagneId)

        val evenements = evenementsCampagne.size + dto.lieux.sumOf { it.evenements.size } + dto.environnements.sumOf { it.evenements.size }
        return "Campagne « ${dto.titre} » (sélectionnée) : ${scenarioIds.size} scénario(s), ${pnjIds.size} fiche(s), " +
            "${monstres.distinctBy { it.lowercase() }.size} monstre(s) au bestiaire, " +
            "$lieux lieu(x) sur la carte, $boutiques boutique(s)" +
            (if (environnementIds.isNotEmpty()) ", ${environnementIds.size} environnement(s)" else "") +
            (if (evenements > 0) ", $evenements événement(s)" else "") + "."
    }

    /**
     * Crée le lieu [lieu] de la campagne [campagneId] (placé sur [carteId], ou non placé si null),
     * ses lieux notables, ses boutiques et son illustration [image]. Retourne le nombre de boutiques.
     */
    private suspend fun creerLieu(
        context: Context,
        lieu: LieuDto,
        campagneId: String,
        worldId: String,
        carteId: String?,
        image: ByteArray?,
        scenarioIds: List<String>,
        imagesEvenements: Map<String, ByteArray> = emptyMap(),
    ): Int {
        val pointId = UUID.randomUUID().toString()
        val boutiqueIds = lieu.boutiques.map { b ->
            val boutique = Boutique(
                id = UUID.randomUUID().toString(),
                nom = b.nom,
                standing = enumOu(b.standing, StandingBoutique.CORRECT),
                marchand = Marchand(b.marchand.nom, b.marchand.description, b.marchand.trait),
                employes = emptyList(),
                inventaire = b.articles.map { a ->
                    val prixPc = enPc(a.prix)
                    ArticleEnVente(EquipementReference(a.nom, prixPc, a.type), prixPc, a.quantite, toujoursDisponible = true)
                },
                type = enumOu(b.type, TypeBoutique.MARCHAND),
                services = b.services.map { s -> Service(s.nom, s.description, enPc(s.prix), personnalise = true) },
                argentDisponibleEnPc = enPc(b.argent),
                villeId = pointId,
                campagneId = campagneId,
                image = BoutiqueImageStore.valeurSiConnue(b.enseigne),
            )
            BoutiqueDependencies.repository.sauvegarderBoutique(boutique)
            boutique.id
        }
        val evenements = lieu.evenements.filter { it.titre.isNotBlank() }.map { avecImage(context, evenement(it, campagneId, worldId), it, imagesEvenements) }
        if (evenements.isNotEmpty()) EvenementDependencies.repository.sauvegarderTous(evenements)
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
                scenarioIds = scenarioIds,
                iconKey = lieu.icone?.trim()?.lowercase()?.takeIf { it.isNotBlank() },
                carteId = carteId,
                fx = lieu.fx?.coerceIn(0f, 1f),
                fy = lieu.fy?.coerceIn(0f, 1f),
                visibleJoueurs = lieu.visible,
                evenementIds = evenements.map { it.id },
                embleme = VilleEmblemeStore.valeurSiConnue(lieu.embleme),
            )
        )
        lieu.lieuxNotables.forEach { notable ->
            CarteDependencies.repository.sauvegarderLieuNotable(
                LieuNotable(UUID.randomUUID().toString(), pointId, notable.nom, notable.description)
            )
        }
        image?.let { runCatching { VilleImageStore.ecrire(context, pointId, it) } }
        return boutiqueIds.size
    }

    /**
     * Crée (ou met à jour, s'il en existe un du même nom dans [worldId]) l'environnement [env] avec
     * ses événements, propres à la campagne [campagneId]. Retourne son id.
     */
    private suspend fun creerEnvironnement(
        context: Context,
        env: EnvironnementDto,
        campagneId: String,
        worldId: String,
        imagesEvenements: Map<String, ByteArray>,
        musiques: Map<String, ByteArray> = emptyMap(),
    ): String {
        val repo = EnvironmentDependencies.repository
        val musique = env.musique?.trim()?.takeIf { it.isNotBlank() }?.let { m ->
            musiques[m]?.let { bytes ->
                val nom = env.musiqueNom?.trim()?.takeIf { it.isNotBlank() } ?: m.substringAfterLast('/').substringBeforeLast('.')
                ImportedMusicStore.importOctets(context, bytes, nom, m.substringAfterLast('.', "mp3"))?.id
            } ?: availableLoopTracks.firstOrNull { it.id == m || it.displayName.equals(m, ignoreCase = true) }?.id
        }
        val evenements = env.evenements.filter { it.titre.isNotBlank() }.map { avecImage(context, evenement(it, campagneId, worldId), it, imagesEvenements) }
        if (evenements.isNotEmpty()) EvenementDependencies.repository.sauvegarderTous(evenements)
        val existant = repo.observerEnvironnements(worldId).first().firstOrNull { it.nom.equals(env.nom.trim(), ignoreCase = true) }
        // Réimport : les événements d'une ancienne importation (campagne supprimée depuis, ou même
        // titre qu'un nouvel événement) sont remplacés ; ceux ajoutés par le MJ restent.
        val campagnes = GameState.mjCampaigns.value.map { it.id }.toSet()
        val titres = evenements.map { it.titre.lowercase() }.toSet()
        val anciensDeCampagne = existant?.evenementIds.orEmpty().let { ids ->
            EvenementDependencies.repository.getEvenements(ids)
                .filter { it.campagneId != null && (it.campagneId !in campagnes || it.titre.lowercase() in titres) }
                .map { it.id }.toSet()
        }
        anciensDeCampagne.forEach { id ->
            if (EvenementDependencies.repository.getEvenement(id)?.campagneId !in campagnes) {
                runCatching { EvenementDependencies.repository.supprimer(id) }
            }
        }
        val environnement = (existant ?: Environnement(nom = env.nom.trim(), worldId = worldId)).copy(
            description = env.description.ifBlank { existant?.description.orEmpty() },
            terrains = env.terrains.ifEmpty { existant?.terrains.orEmpty() },
            musicTrackId = musique ?: existant?.musicTrackId,
            evenementIds = (existant?.evenementIds.orEmpty().filterNot { it in anciensDeCampagne } + evenements.map { it.id }).distinct(),
        )
        repo.sauvegarder(environnement)
        return environnement.id
    }

    /** [evenement] avec son illustration [dto].image copiée depuis l'archive (EvenementImageStore). */
    private fun avecImage(context: Context, evenement: Evenement, dto: EvenementDto, images: Map<String, ByteArray>): Evenement {
        val bytes = dto.image?.let { images[it] } ?: return evenement
        return evenement.copy(imageFileName = EvenementImageStore.ecrire(context, evenement.id, bytes))
    }

    /** Événement de la bibliothèque, propre à la campagne [campagneId], depuis sa description json. */
    internal fun evenement(dto: EvenementDto, campagneId: String, worldId: String): Evenement {
        val quantiteRegex = Regex("""^(.*?)\s*[x×](\d+)$""", RegexOption.IGNORE_CASE)
        fun profil(type: TypeProfil, texte: String): ProfilEvenement {
            val m = quantiteRegex.find(texte.trim())
            return if (m != null) ProfilEvenement(type, m.groupValues[1].trim(), m.groupValues[2].toIntOrNull() ?: 1)
            else ProfilEvenement(type, texte.trim())
        }
        fun effet(e: EffetDto) = EffetEvenement(
            categorie = enumOu(e.categorie, CategorieEffet.AUTRE),
            gain = e.gain,
            quantite = e.quantite.coerceAtLeast(1),
            cible = e.cible.trim(),
            description = e.description.trim(),
        )
        return Evenement(
            worldId = worldId,
            campagneId = campagneId,
            type = enumOu(dto.type, TypeEvenement.RENCONTRE),
            titre = dto.titre.trim(),
            description = dto.description.trim(),
            objectif = dto.objectif.trim(),
            effets = dto.effets.map(::effet),
            issues = dto.issues.map { i ->
                IssueEvenement(
                    nature = enumOu(i.nature, NatureIssue.REUSSITE),
                    titre = i.titre.trim(),
                    description = i.description.trim(),
                    effets = i.effets.map(::effet),
                    declencheCombat = i.combat,
                )
            },
            profils = dto.monstres.filter { it.isNotBlank() }.map { profil(TypeProfil.MONSTRE, it) } +
                dto.pnj.filter { it.isNotBlank() }.map { profil(TypeProfil.PNJ, it) },
        )
    }

    /**
     * Ajoute les lieux de `lieux.json` à la campagne sélectionnée de [worldId], sans les placer sur
     * la carte (le MJ les place ensuite : « Placer un lieu existant »). Retourne le compte rendu.
     */
    suspend fun importerLieux(context: Context, contenu: LieuxContenu, worldId: String): String {
        val noms = contenu.lieux.joinToString(", ") { "« ${it.nom} »" }
        val campagne = GameState.currentCampaignId.value
            ?.let { id -> GameState.mjCampaigns.value.firstOrNull { it.id == id } }
            ?: return "Lieu(x) $noms non ajouté(s) : aucune campagne sélectionnée (choisissez-en une, puis réimportez)."
        val scenarios = GameState.mjScenarios.value.filter { it.worldId == worldId }
        var boutiques = 0
        contenu.lieux.forEach { lieu ->
            boutiques += creerLieu(
                context, lieu, campagne.id, worldId, carteId = null, image = contenu.images[lieu.nom],
                scenarioIds = lieu.scenarios.mapNotNull { t -> scenarios.lastOrNull { it.title.equals(t, ignoreCase = true) }?.id },
                imagesEvenements = contenu.imagesEvenements,
            )
        }
        return "Lieu(x) $noms ajouté(s) à la campagne « ${campagne.title} » ($boutiques commerce(s)) : " +
            "à placer depuis la carte (« Placer un lieu existant »)."
    }

    /** Lieux de toutes les campagnes de [worldId] (pour retrouver ce qu'un import a ajouté). */
    suspend fun lieuxDuMonde(worldId: String): List<PointInteret> =
        GameState.mjCampaigns.value.filter { it.worldId == worldId }
            .flatMap { CarteDependencies.repository.observerPoints(it.id).first() }

    /** Supprime un lieu avec ses lieux notables, ses boutiques et son illustration. */
    suspend fun supprimerLieu(context: Context, point: PointInteret) {
        val repo = CarteDependencies.repository
        repo.observerLieuxNotables(point.id).first().forEach { repo.supprimerLieuNotable(it.id) }
        point.boutiqueIds.forEach { BoutiqueDependencies.repository.supprimerBoutique(it) }
        VilleImageStore.supprimer(context, point.id)
        repo.supprimerPoint(point.id)
    }

    /**
     * Quêtes du dossier de campagne : le donneur est retrouvé par son nom parmi les fiches créées
     * par l'archive ([pnjIds]), à défaut parmi les PNJ du monde ; statut ou type de récompense
     * inconnu → en attente / récompense ignorée.
     */
    internal fun quetes(dtos: List<QueteDto>, worldId: String, pnjIds: List<String>): List<com.jc2.jdrcompagnon.feature_quete.domain.model.Quest> {
        val fiches = GameState.characters.value
        fun donneur(nom: String?): String? {
            val n = nom?.trim()?.takeIf { it.isNotBlank() } ?: return null
            return (fiches.firstOrNull { it.id in pnjIds && it.name.equals(n, ignoreCase = true) }
                ?: fiches.firstOrNull { it.type == "PNJ" && it.worldId == worldId && it.name.equals(n, ignoreCase = true) })?.id
        }
        return dtos.filter { it.titre.isNotBlank() }.map { q ->
            com.jc2.jdrcompagnon.feature_quete.domain.model.Quest(
                title = q.titre.trim(),
                description = q.description.trim(),
                mjNotes = q.notesMj.trim(),
                location = q.lieu.trim(),
                giverCharacterId = donneur(q.donneur),
                status = enumOu(q.statut, com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_ATTENTE),
                visibleToPlayers = q.visible,
                rewardsVisibleToPlayers = q.recompensesVisibles,
                rewards = q.recompenses.mapNotNull { r ->
                    val type = runCatching { com.jc2.jdrcompagnon.feature_quete.domain.model.QuestRewardType.valueOf(r.type.trim().uppercase()) }.getOrNull()
                        ?: return@mapNotNull null
                    com.jc2.jdrcompagnon.feature_quete.domain.model.QuestReward(type = type, label = r.libelle.trim(), amount = r.montant, detail = r.detail.trim())
                },
            )
        }
    }

    /**
     * Supprime la carte, les lieux (et leurs lieux notables), les boutiques d'une campagne et les
     * événements créés avec elle, y compris ceux de son environnement [environnementId] (supprimé
     * s'il ne lui reste plus aucun événement).
     */
    suspend fun supprimerDonnees(campagneId: String, environnementId: String? = null) {
        val repo = CarteDependencies.repository
        val evenements = EvenementDependencies.repository
        val points = repo.observerPoints(campagneId).first()
        val environnement = environnementId?.let { EnvironmentDependencies.repository.getEnvironnementParId(it) }
        val idsEvenements = points.flatMap { it.evenementIds } + environnement?.evenementIds.orEmpty()
        val propres = evenements.getEvenements(idsEvenements.distinct()).filter { it.campagneId == campagneId }.map { it.id }.toSet() +
            // Événements de la campagne elle-même (dto.evenements), rattachés à aucun lieu.
            evenements.idsDeCampagne(campagneId)
        propres.forEach { runCatching { evenements.supprimer(it) } }
        environnement?.let { env ->
            val restants = env.evenementIds.filterNot { it in propres }
            if (restants.isEmpty()) EnvironmentDependencies.repository.supprimer(env.id)
            else EnvironmentDependencies.repository.sauvegarder(env.copy(evenementIds = restants))
        }
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

    /** Montant de l'archive (po, décimales permises) en pièces de cuivre. */
    private fun enPc(po: Double): Int = kotlin.math.round(po * com.jc2.jdrcompagnon.ui.Monnaie.PC_PAR_PO).toInt().coerceAtLeast(0)

    private inline fun <reified E : Enum<E>> enumOu(nom: String, defaut: E): E =
        enumValues<E>().firstOrNull { it.name.equals(nom.trim(), ignoreCase = true) } ?: defaut
}
