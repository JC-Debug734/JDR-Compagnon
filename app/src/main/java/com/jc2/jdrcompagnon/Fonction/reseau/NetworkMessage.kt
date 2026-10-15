package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.CalendarConfig
import com.jc2.jdrcompagnon.ui.ScenarioClockData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Message échangé entre MJ et Joueur sur la socket TCP existante.
 * Le canal transporte du texte ligne par ligne (BufferedReader.readLine()) :
 * un message = une ligne JSON, encodée avec networkJson puis envoyée via
 * PrintWriter(...).println(...).
 */
@Serializable
data class NetworkMessage(
    val type: String,
    val character: Character? = null,
    val characterId: String? = null,
    val characters: List<CharacterSummary>? = null,
    val accepted: Boolean? = null,
    val scenarioClock: ScenarioClockData? = null,
    // TYPE_TIME_STATE : calendrier actif du MJ (éventuellement personnalisé), pour que les
    // joueurs affichent la même date que lui.
    val calendar: CalendarConfig? = null,
    // TYPE_PNJ_BRIEFING : nom du PNJ + id de portrait (résolu localement via
    // characterPortraitOptions, même liste des deux côtés — pas de transfert d'image).
    val pnjName: String? = null,
    val pnjPortraitId: String? = null,
    // TYPE_ITEM_DELIVERY / TYPE_LOOT_OFFER : nom de l'objet SRD concerné.
    val itemName: String? = null,
    // TYPE_LOOT_OFFER / TYPE_LOOT_CLAIM_RESPONSE / TYPE_LOOT_OFFER_CLOSED : identifiant de
    // l'offre de butin de groupe en cours. `accepted` est réutilisé pour la réponse du joueur
    // (true = "je le veux").
    val lootOfferId: String? = null,
    // TYPE_GROUP_PROPOSAL / TYPE_PROPOSAL_DECISION / TYPE_PROPOSAL_STATE / TYPE_PROPOSAL_FINAL_CALL :
    // proposition de récompense de groupe soumise au vote de tous les joueurs connectés.
    val proposalId: String? = null,
    val proposalTitle: String? = null,
    val proposalDescription: String? = null,
    val proposalReward: String? = null,
    // TYPE_PROPOSAL_DECISION (joueur → MJ) : "accepted" / "declined" / "passed".
    val proposalDecision: String? = null,
    // TYPE_PROPOSAL_STATE (MJ → tous) : état agrégé après chaque décision — clientId → décision
    // ("pending" tant qu'aucune réponse), et clientId → nom affiché pour ne pas dépendre d'un
    // état MJ-only côté joueur.
    val proposalDecisions: Map<String, String>? = null,
    val proposalClientNames: Map<String, String>? = null,
    // TYPE_PROPOSAL_STATE : true quand le MJ a clôturé la proposition (bouton "Clôturer") — le
    // joueur doit alors la retirer de son écran "Proposition en cours" quel que soit son état.
    val proposalClosed: Boolean? = null,
    // TYPE_EPREUVE_STATE : vue joueur de l'épreuve environnementale en cours (null = aucune).
    val epreuve: EpreuveJoueurData? = null,
    // TYPE_GROUP_INFO : nom du groupe actif (null = aucun) ; ses PJ passent dans `characters`.
    val groupName: String? = null,
    // TYPE_SOCIAL_ROLL_* : jet de compétence sociale pendant une discussion avec un PNJ.
    val socialRequestId: String? = null,
    val socialCharacterName: String? = null,
    val socialSkill: String? = null,
    val socialBonus: Int? = null,
    // TYPE_SOCIAL_ROLL_MODE : "NORMAL" / "AVANTAGE" / "DESAVANTAGE" (la ligne rouge n'est jamais
    // transmise au joueur : il reçoit NORMAL).
    val socialMode: String? = null,
    // TYPE_SOCIAL_ROLL_RESULT : d20 lancés (2 avec avantage/désavantage) et total retenu + bonus.
    val socialDice: List<Int>? = null,
    val socialTotal: Int? = null,
    // TYPE_COMBAT_STATE : vue joueur du combat (null = plus de combat).
    val combat: CombatJoueurData? = null,
    // TYPE_COMBAT_DECLARATION : action choisie par le joueur pour le round.
    val combatDeclaration: DeclarationJoueurData? = null,
    // TYPE_COMBAT_ROLL : jet de dés du joueur pendant le combat.
    val combatJet: JetCombatData? = null,
    // TYPE_QUESTS_STATE : quêtes de la campagne rendues visibles aux joueurs par le MJ.
    val quests: List<QuestJoueurData>? = null,
    // TYPE_QUEST_ACCEPT : quête acceptée par le joueur.
    val questId: String? = null,
    // TYPE_GROUP_INFO : vue joueur complète du groupe actif (lieu, position, montures, inventaire...).
    val groupe: GroupeJoueurData? = null,
    // TYPE_CAMPAIGN_STATE : campagne sélectionnée, ses cartes et ses lieux révélés (null = aucune).
    val campagne: CampagneJoueurData? = null,
    // TYPE_MAP_IMAGE : image de fond d'une carte (envoyée à part, seulement quand elle change).
    val carteImage: CarteImageData? = null,
    // TYPE_CITY_IMAGE : image d'une ville révélée (envoyée à part, seulement quand elle change).
    val villeImage: VilleImageData? = null,
    // TYPE_STORY_IMAGE : image de l'histoire personnelle d'un personnage (fiche dans characterId).
    val imageHistoire: ImageHistoireData? = null,
    // TYPE_REST_OFFER : texte de la proposition de repos long du MJ.
    val reposMessage: String? = null,
    // TYPE_SERVICE_USED : boutique et service utilisés (le personnage est dans characterId).
    val serviceBoutiqueId: String? = null,
    val serviceNom: String? = null,
    // TYPE_VOYAGE_STATE : voyage en préparation ou en route (null = aucun) ; TYPE_VOYAGE_PRET :
    // route proposée par le joueur (null = il valide la route déjà partagée).
    val voyage: VoyageData? = null,
    // TYPE_MOUNT_BAGGAGE : monture, objet, et sens du transfert (true = sac du personnage → sacoches).
    val montureId: String? = null,
    val objetNom: String? = null,
    val versMonture: Boolean? = null,
    // TYPE_MAP_OPENED : carte de campagne ouverte par le MJ en mode exploration (null = aucune).
    val carteOuverte: CarteOuverteData? = null,
    // TYPE_MAP_TRACE : tracé en cours sur une carte de campagne (null = aucun).
    val trace: TraceCarteData? = null,
    // TYPE_SHARED_IMPORTS : imports partagés par le MJ, et adresse de sa page table d'où les télécharger.
    val imports: List<ImportPartageData>? = null,
    val urlTable: String? = null,
    // TYPE_CONTENT_MISSING : éléments introuvables chez le joueur, et ce qu'il vient de récupérer
    // (nom du personnage ou de l'objet).
    val manquants: List<String>? = null,
    val sujetManquant: String? = null,
    // TYPE_CITY_POSITION (joueur → MJ) : lieu de la ville où se rend le joueur (null = hors ville).
    val positionVille: PositionVilleData? = null,
    // TYPE_CITY_POSITIONS (MJ → tous) : lieu de chaque joueur connecté qui visite une ville.
    val positionsVille: List<PositionVilleData>? = null,
    // TYPE_SHOP_TRADE : boutique, objet (objetNom) et sens (true = achat, false = vente) ;
    // TYPE_SHOP_TRADE_RESULT : `accepted` + compte rendu de la transaction.
    val commerceBoutiqueId: String? = null,
    val commerceAchat: Boolean? = null,
    val commerceMessage: String? = null,
    // TYPE_SHOP_INVITE : boutique où le MJ invite les joueurs (lecture de scénario) ; `accepted` =
    // true pour une nouvelle invitation, false pour une simple mise à jour (stock, caisse...).
    val boutiqueInvitation: BoutiqueJoueurData? = null,
) {
    companion object {
        /** MJ → Joueur : envoie un personnage complet (envoi ponctuel). */
        const val TYPE_CHARACTER_PUSH = "character_push"

        /** MJ → Joueur : demande de renvoyer l'état actuel d'un personnage. */
        const val TYPE_CHARACTER_SYNC_REQUEST = "character_sync_request"

        /** Joueur → MJ : réponse à une demande de synchronisation. */
        const val TYPE_CHARACTER_SYNC_RESPONSE = "character_sync_response"

        /**
         * MJ → Joueur : liste des personnages du groupe encore disponibles
         * (pas déjà réservés par un autre joueur). Envoyé à la connexion et
         * chaque fois que la disponibilité change (réservation, libération).
         */
        const val TYPE_AVAILABLE_CHARACTERS = "available_characters"

        /** Joueur → MJ : demande à réserver un personnage disponible du groupe. */
        const val TYPE_CHARACTER_CLAIM_REQUEST = "character_claim_request"

        /** MJ → Joueur : résultat de la réservation (accepted + personnage complet si oui). */
        const val TYPE_CHARACTER_CLAIM_RESPONSE = "character_claim_response"

        /**
         * Joueur → MJ : propose un des personnages de son propre appareil
         * (aucun personnage du groupe n'était disponible).
         */
        const val TYPE_CHARACTER_PROPOSAL = "character_proposal"

        /** MJ → Joueur : le MJ a accepté ou refusé la proposition. */
        const val TYPE_CHARACTER_PROPOSAL_RESULT = "character_proposal_result"

        /**
         * MJ → Joueurs : imports (livres) que le MJ partage, à la connexion et à chaque
         * changement de sa sélection. Le joueur est invité à télécharger ceux qu'il n'a pas.
         */
        const val TYPE_SHARED_IMPORTS = "shared_imports"

        /**
         * Joueur → MJ : un personnage ou un objet récupéré référence du contenu (classe,
         * sous-classe, sort, objet...) absent de la bibliothèque du joueur.
         */
        const val TYPE_CONTENT_MISSING = "content_missing"

        /**
         * MJ → Joueur : état complet de l'horloge de scénario (activée, défilement auto,
         * météo auto, heure, météo). Envoyé à la connexion d'un client puis à chaque
         * changement côté MJ (y compris chaque tic pendant que le temps défile), pour que
         * l'arrêt du défilement ou de la fonction par le MJ se propage immédiatement.
         */
        const val TYPE_TIME_STATE = "time_state"

        /** MJ → Joueurs (diffusion) : briefing PNJ déclenché par un événement de scène (#event:). */
        const val TYPE_PNJ_BRIEFING = "pnj_briefing"

        /** MJ → Joueur : livre directement un objet dans l'inventaire (sac) du joueur ciblé. */
        const val TYPE_ITEM_DELIVERY = "item_delivery"

        /** MJ → Joueurs d'un groupe : propose un objet, chacun peut cliquer "je le veux". */
        const val TYPE_LOOT_OFFER = "loot_offer"

        /** Joueur → MJ : réponse à une offre de butin de groupe (accepted = "je le veux"). */
        const val TYPE_LOOT_CLAIM_RESPONSE = "loot_claim_response"

        /** MJ → Joueurs : ferme une offre de butin de groupe (tous ont répondu, ou fermeture manuelle). */
        const val TYPE_LOOT_OFFER_CLOSED = "loot_offer_closed"

        /** MJ → Joueurs (diffusion) : propose une récompense de groupe soumise au vote. */
        const val TYPE_GROUP_PROPOSAL = "group_proposal"

        /** Joueur → MJ : décision (accepter/décliner/passer) sur une proposition de groupe. */
        const val TYPE_PROPOSAL_DECISION = "proposal_decision"

        /** MJ → Joueurs (diffusion) : état agrégé des décisions d'une proposition de groupe. */
        const val TYPE_PROPOSAL_STATE = "proposal_state"

        /**
         * MJ → Joueur(s) encore en attente : la scène a changé alors que leur décision manque
         * toujours — dernier appel avant perte de la récompense.
         */
        const val TYPE_PROPOSAL_FINAL_CALL = "proposal_final_call"

        /**
         * MJ → Joueurs (diffusion) : état de l'épreuve environnementale en cours, à chaque
         * changement et à la connexion d'un client. `epreuve` null = plus d'épreuve active.
         */
        const val TYPE_EPREUVE_STATE = "epreuve_state"

        /**
         * MJ → Joueurs (diffusion) : groupe actif de la partie (nom + PJ membres). Le joueur n'a
         * pas les groupes du MJ en local, son menu latéral l'affiche à partir de ce message.
         * Envoyé à la connexion et à chaque changement du groupe.
         */
        const val TYPE_GROUP_INFO = "group_info"

        /** Joueur → MJ : le joueur veut utiliser une compétence sociale dans la discussion. */
        const val TYPE_SOCIAL_ROLL_REQUEST = "social_roll_request"

        /** MJ → Joueur : décision du MJ (normal / avantage / désavantage), le joueur peut lancer. */
        const val TYPE_SOCIAL_ROLL_MODE = "social_roll_mode"

        /** Joueur → MJ : résultat du jet (le joueur ne sait pas s'il a réussi). */
        const val TYPE_SOCIAL_ROLL_RESULT = "social_roll_result"

        /**
         * MJ → Joueur : le MJ annule la demande de jet (le joueur ne veut plus agir, ou la
         * discussion est déjà prise par un autre joueur). Le joueur revient au choix des compétences.
         */
        const val TYPE_SOCIAL_ROLL_CANCEL = "social_roll_cancel"

        /**
         * MJ → Joueurs (diffusion) : joueur en train d'agir dans la discussion (socialRequestId +
         * socialCharacterName) ; les autres sont bloqués. socialRequestId null = discussion libre.
         */
        const val TYPE_DISCUSSION_LOCK = "discussion_lock"

        /** Joueur → MJ : le joueur a quitté la discussion avec le PNJ. */
        const val TYPE_DISCUSSION_LEFT = "discussion_left"

        /** MJ → Joueurs (diffusion) : le MJ ferme la discussion pour tout le monde. */
        const val TYPE_DISCUSSION_CLOSED = "discussion_closed"

        /**
         * MJ → Joueur : état du combat vu par CE joueur (sa propre déclaration uniquement), à chaque
         * changement et à la connexion. `combat` null = combat terminé.
         */
        const val TYPE_COMBAT_STATE = "combat_state"

        /** Joueur → MJ : action déclarée pour le round en cours (remplace la précédente). */
        const val TYPE_COMBAT_DECLARATION = "combat_declaration"

        /** Joueur → MJ : résultat d'un jet de dés pendant le combat. */
        const val TYPE_COMBAT_ROLL = "combat_roll"

        /**
         * MJ → Joueurs (diffusion) : quêtes visibles de la campagne sélectionnée, à la connexion et
         * à chaque changement (création, validation, visibilité, campagne sélectionnée).
         */
        const val TYPE_QUESTS_STATE = "quests_state"

        /**
         * MJ → Joueurs (diffusion) : campagne sélectionnée, ses cartes (échelle, cases) et ses lieux
         * révélés aux joueurs — jamais les lieux cachés. Les joueurs l'enregistrent localement pour
         * consulter la carte et les villes.
         */
        const val TYPE_CAMPAIGN_STATE = "campaign_state"

        /** MJ → Joueur(s) : image de fond d'une carte, à la connexion et quand elle change. */
        const val TYPE_MAP_IMAGE = "map_image"

        /**
         * MJ → Joueurs (diffusion) : carte de campagne que le MJ a ouverte en mode exploration, à
         * chaque changement et à la connexion ; l'appareil du joueur l'affiche automatiquement.
         * `carteOuverte` null = le MJ l'a quittée.
         */
        const val TYPE_MAP_OPENED = "map_opened"

        /**
         * Tracé sur une carte de campagne (voir TraceCarteSession) — Joueur → MJ : le joueur trace ;
         * MJ → Joueurs (diffusion) : tracé du MJ ou d'un joueur, à chaque changement et à la connexion.
         */
        const val TYPE_MAP_TRACE = "map_trace"

        /** MJ → Joueur(s) : image d'une ville révélée, à la connexion et quand elle change. */
        const val TYPE_CITY_IMAGE = "city_image"

        /**
         * Dans les deux sens : une fiche reçue référence dans son histoire personnelle des images
         * absentes de l'appareil ; leurs noms sont dans `manquants`, la fiche dans `characterId`.
         */
        const val TYPE_STORY_IMAGES_REQUEST = "story_images_request"

        /** Réponse à TYPE_STORY_IMAGES_REQUEST : une image d'histoire par message (`imageHistoire`). */
        const val TYPE_STORY_IMAGE = "story_image"

        /** MJ → Joueurs (diffusion) : le groupe s'arrête pour un repos long, chacun peut le prendre. */
        const val TYPE_REST_OFFER = "rest_offer"

        /**
         * Joueur → MJ : le personnage a utilisé (et payé) un service d'une boutique du lieu où se
         * trouve le groupe ; le MJ décompte la place utilisée et en est averti.
         */
        const val TYPE_SERVICE_USED = "service_used"

        /** Joueur → MJ : le personnage [characterId] accepte la quête [questId] (panneau de quêtes). */
        const val TYPE_QUEST_ACCEPT = "quest_accept"

        /** Joueur → MJ : le personnage [characterId] abandonne la quête [questId] (écran Quêtes). */
        const val TYPE_QUEST_ABANDON = "quest_abandon"

        /**
         * MJ → Joueurs (diffusion) : voyage partagé (route tracée sur la carte, vitesse, qui est
         * prêt au départ), à chaque changement et à la connexion. `voyage` null = aucun voyage.
         */
        const val TYPE_VOYAGE_STATE = "voyage_state"

        /** Joueur → MJ : prêt au départ, avec sa propre route s'il en propose une nouvelle. */
        const val TYPE_VOYAGE_PRET = "voyage_pret"

        /**
         * Joueur → MJ : déplacer un objet entre le sac de son personnage et les sacoches d'une
         * monture du groupe. Le MJ fait les deux changements (fiche et groupe), puis les rediffuse.
         */
        const val TYPE_MOUNT_BAGGAGE = "mount_baggage"

        /** Joueur → MJ : lieu de la ville où il se rend (positionVille), à chaque déplacement et à la connexion. */
        const val TYPE_CITY_POSITION = "city_position"

        /**
         * MJ → Joueurs (diffusion) : où se trouve chaque joueur dans la ville, à chaque déplacement,
         * connexion ou déconnexion — chacun voit où vont les autres.
         */
        const val TYPE_CITY_POSITIONS = "city_positions"

        /**
         * Joueur → MJ : acheter (commerceAchat = true) ou vendre un objet (objetNom) dans une boutique
         * du lieu. Le MJ vérifie le stock, la bourse et la caisse, applique (fiche et boutique), puis
         * répond par TYPE_SHOP_TRADE_RESULT ; fiche et boutique repartent par leurs diffusions habituelles.
         */
        const val TYPE_SHOP_TRADE = "shop_trade"

        /** MJ → Joueur : transaction acceptée ou refusée (accepted), avec son compte rendu (commerceMessage). */
        const val TYPE_SHOP_TRADE_RESULT = "shop_trade_result"

        /**
         * MJ → Joueurs : invitation à afficher l'écran d'une boutique (lien #boutique: d'un scénario),
         * même hors d'une ville révélée. Renvoyé (accepted = false) à chaque changement de la
         * boutique pour tenir à jour stock et caisse ; le commerce passe ensuite par TYPE_SHOP_TRADE.
         */
        const val TYPE_SHOP_INVITE = "shop_invite"
    }
}

/** Vue joueur du groupe actif (lecture seule) : tout ce que le MJ a noté, localisé. */
@Serializable
data class GroupeJoueurData(
    val id: String,
    val name: String,
    val location: String = "",
    val gold: Int = 0,
    val carteId: String? = null,
    val carteFx: Float? = null,
    val carteFy: Float? = null,
    val membres: List<MembreJoueurData> = emptyList(),
    val montures: List<MontureJoueurData> = emptyList(),
    val vehicules: List<VehiculeJoueurData> = emptyList(),
    val reputations: List<ReputationJoueurData> = emptyList(),
    val inventaire: List<ObjetJoueurData> = emptyList(),
    val biens: List<ObjetJoueurData> = emptyList(),
    // Vitesse de déplacement du groupe (mètres) = celle de son membre le plus lent, et qui c'est.
    val vitesseM: Int? = null,
    val plusLent: String? = null,
    // Icône du groupe sur la carte (clé de IconesPointInteret.PALETTE_GROUPE) et sa couleur ; null = par défaut.
    val iconKey: String? = null,
    val couleurArgb: Int? = null,
)

@Serializable
data class MembreJoueurData(
    val id: String,
    val name: String,
    val type: String,
    val speed: Int,
    val location: String = "",
    val detail: String = "",
    // Points de vie (null pour un joueur sans fiche) et état (à terre, états, épuisement).
    val pv: Int? = null,
    val pvMax: Int? = null,
    val pvTemp: Int = 0,
    val etat: String = "",
)

@Serializable
data class MontureJoueurData(
    val name: String,
    val kind: String,
    val species: String = "",
    val speed: Int = 0,
    val rider: String? = null,
    val location: String = "",
    // Identifiant de la monture (Mount.id) et contenu de ses sacoches.
    val id: String = "",
    val bagages: List<String> = emptyList(),
    val pv: Int? = null,
    val pvMax: Int? = null,
    val etat: String = "",
)

@Serializable
data class VehiculeJoueurData(val name: String, val type: String = "", val speed: Int? = null, val tirePar: List<String> = emptyList(), val location: String = "")

@Serializable
data class ReputationJoueurData(val faction: String, val score: Int)

/** Objet de l'inventaire commun ou bien du groupe ([detail] = description d'un bien). */
@Serializable
data class ObjetJoueurData(val name: String, val quantity: Int = 1, val location: String = "", val detail: String = "")

/** Campagne vue par les joueurs : cartes et lieux révélés seulement. */
@Serializable
data class CampagneJoueurData(
    val id: String,
    val titre: String,
    val cartes: List<CarteJoueurData> = emptyList(),
    val points: List<com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret> = emptyList(),
    // Boutiques des villes révélées, avec leurs services actifs (ni stock ni caisse du marchand).
    val boutiques: List<BoutiqueJoueurData> = emptyList(),
    // Lieux à visiter des villes révélées (auberge, temple...), listés dans le menu joueur.
    val lieux: List<LieuJoueurData> = emptyList(),
    // Villes révélées qui ont une image : id de la ville → signature de l'image (voir VilleImageStore).
    val imagesVilles: Map<String, String> = emptyMap(),
    // Réglages d'affichage des lieux révélés, choisis par le MJ (taille d'icône, nom montré ou caché) : id → réglage.
    val affichageLieux: Map<String, AffichageLieuData> = emptyMap(),
    // Noms des lieux montrés (true) ou cachés sur chaque carte (réglage de la grille du MJ) : id de carte → montrés.
    val nomsCartes: Map<String, Boolean> = emptyMap(),
)

/** Affichage d'un lieu sur la carte (voir ReglagePoint) : [positionTexte] = nom de PositionTexte. */
@Serializable
data class AffichageLieuData(val taille: Float = 1f, val afficherTexte: Boolean = true, val positionTexte: String = "BAS")

/**
 * Tracé sur une carte de campagne (mode exploration) : [etapes] en fractions de la carte, [termine]
 * après « Fin de tracé » (toutes les vues cadrent alors le trajet), [auteur] = qui l'a tracé.
 * Aucune étape = tracé effacé.
 */
@Serializable
data class TraceCarteData(
    val campagneId: String,
    val carteId: String,
    val etapes: List<EtapeVoyageData> = emptyList(),
    val termine: Boolean = false,
    val auteur: String = "",
) {
    /** Même carte, mêmes étapes, même état (l'auteur ne compte pas). */
    fun memeTrace(autre: TraceCarteData?): Boolean =
        autre != null && carteId == autre.carteId && etapes == autre.etapes && termine == autre.termine
}

/** Image d'une ville (JPEG en base64), [signature] identifiant la version envoyée. */
@Serializable
data class VilleImageData(val villeId: String, val signature: String, val base64: String)

/** Image de l'histoire personnelle d'un personnage, sous son nom de fichier (référencé dans le .md). */
@Serializable
data class ImageHistoireData(val nom: String, val base64: String)

/** Lieu à visiter d'une ville révélée (LieuNotable), vu par les joueurs. */
@Serializable
data class LieuJoueurData(
    val id: String,
    val villeId: String,
    val nom: String,
    val description: String = "",
    // Fonction du lieu (nom de FonctionLieu) et ses réglages, voir LieuNotable.
    val fonction: String = "AUCUNE",
    val chancePourcent: Int = 50,
    val prixPc: Int = 0,
    val texte: String = "",
    val queteIds: List<String> = emptyList(),
    val dd: Int = 12,
    val valeur: Int = 0,
)

/**
 * Voyage préparé sur une carte de campagne (voir VoyageSession) : route ([etapes], en fractions de
 * la carte), allure ([vitesse] = nom de VitesseDeplacement, null = vitesse du groupe), heures de
 * marche par jour, et le plan calculé (distance, haltes de fin de journée). [prets] : pseudos des
 * joueurs prêts au départ ; [attendus] : joueurs connectés dont on attend la validation.
 * [version] change à chaque modification de la route, de l'allure ou des haltes.
 */
@Serializable
data class VoyageData(
    val id: String,
    val version: Int = 0,
    val campagneId: String,
    val carteId: String,
    val etapes: List<EtapeVoyageData>,
    val vitesse: String? = null,
    val heuresMax: Int,
    val distanceKm: Double,
    val kmParHeure: Double,
    val arrets: List<ArretVoyageData> = emptyList(),
    val arriveeNom: String,
    val arriveePointId: String? = null,
    val proposePar: String,
    val prets: List<String> = emptyList(),
    val mjPret: Boolean = false,
    val attendus: List<String> = emptyList(),
    val enRoute: Boolean = false,
) {
    val heures: Double get() = if (kmParHeure > 0) distanceKm / kmParHeure else 0.0

    /** Même route, même allure, mêmes haltes. */
    fun memeTrajet(autre: VoyageData): Boolean =
        carteId == autre.carteId && etapes == autre.etapes && vitesse == autre.vitesse && heuresMax == autre.heuresMax
}

@Serializable
data class EtapeVoyageData(val pointId: String? = null, val fx: Float, val fy: Float)

@Serializable
data class ArretVoyageData(val numero: Int, val fx: Float, val fy: Float, val km: Double)

/**
 * Boutique d'une ville révélée, vue par les joueurs : services actifs, articles en vente (prix et
 * stock) et argent dont dispose le marchand pour racheter des objets.
 */
@Serializable
data class BoutiqueJoueurData(
    val id: String,
    val villeId: String,
    val nom: String,
    val type: String,
    val marchand: String,
    val services: List<ServiceJoueurData> = emptyList(),
    val articles: List<ArticleJoueurData> = emptyList(),
    // Caisse du marchand, en pièces de cuivre (non affichée aux joueurs).
    val argentPc: Int = 0,
    // Trait de caractère du marchand (« bourru mais honnête »...), affiché à l'écran de commerce.
    val traitMarchand: String = "",
    // Enseigne de la boutique (Boutique.image) : seule une enseigne fournie avec l'app ("enseigne:<clé>")
    // s'affiche chez un autre joueur ; une image de l'appareil du MJ n'existe que chez lui.
    val image: String? = null,
)

/** Article en vente dans une boutique : [prixPc] en pièces de cuivre (cf. Monnaie), [stock] 0 = épuisé. */
@Serializable
data class ArticleJoueurData(val nom: String, val type: String = "", val prixPc: Int, val stock: Int)

/**
 * Lieu d'une ville où se trouve un joueur : [lieuId] null = entrée de la ville (voir VisiteVille) ;
 * [joueur] et [personnage] sont renseignés par le MJ avant la diffusion.
 */
@Serializable
data class PositionVilleData(
    val villeId: String,
    val lieuId: String? = null,
    val lieuNom: String = "",
    val joueur: String = "",
    val personnage: String = "",
    // Connexion du joueur (renseignée par le MJ) : rattache la position au personnage qu'il incarne.
    val clientId: String = "",
)

@Serializable
data class ServiceJoueurData(
    val nom: String,
    val description: String = "",
    val prixEnPc: Int, // pièces de cuivre (cf. Monnaie)
    // null = illimité.
    val quantiteDisponible: Int? = null,
    // Effet réel du service (nom de EffetService, déjà déduit du nom par le MJ).
    val effet: String = "AUCUN",
)

@Serializable
data class CarteJoueurData(
    val id: String,
    val nom: String,
    val largeurCases: Int,
    val hauteurCases: Int,
    val echelleKmParCase: Int,
    // Cases sur la largeur chez le MJ (voir CarteGrillePrefs.casesEnLargeur).
    val casesEnLargeur: Float,
    // Signature de l'image de fond (null = pas d'image) : l'image n'est renvoyée que si elle change.
    val imageSignature: String? = null,
)

/** Carte de campagne ouverte par le MJ (voir TYPE_MAP_OPENED). */
@Serializable
data class CarteOuverteData(val campagneId: String, val carteId: String)

@Serializable
data class CarteImageData(val carteId: String, val signature: String, val base64: String)

/**
 * Vue joueur d'une quête : jamais les notes du MJ ; les récompenses seulement si le MJ les annonce
 * ou une fois la quête terminée (voir QuetesJoueur).
 */
@Serializable
data class QuestJoueurData(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    // Nom de QuestStatus (EN_COURS, EN_ATTENTE, TERMINEE, ECHOUEE).
    val status: String,
    val giverName: String? = null,
    val rewards: List<String> = emptyList(),
    // Personnages qui ont accepté la quête.
    val accepteePar: List<String> = emptyList(),
    // Personnages qui ont abandonné la quête.
    val abandonneePar: List<String> = emptyList(),
)

/**
 * Vue joueur d'une épreuve environnementale : seul le Progrès est partagé — la Menace, les DD et
 * les capacités restent côté MJ pour préserver la surprise.
 */
@Serializable
data class EpreuveJoueurData(
    val id: String,
    val nom: String,
    val type: String,
    val description: String,
    val competences: List<String>,
    val progres: Int,
    val progresMax: Int,
    // Libellé de l'issue une fois l'épreuve terminée, null tant qu'elle est en cours.
    val issue: String? = null,
    // Échecs subis sur échecs tolérés (0 = non suivi, hôte d'une ancienne version).
    val echecs: Int = 0,
    val echecsMax: Int = 0,
)

/**
 * Résumé léger d'un personnage, utilisé pour la liste des personnages
 * disponibles (évite d'envoyer les fiches complètes tant qu'elles ne sont
 * pas réservées).
 */
@Serializable
data class CharacterSummary(
    val id: String,
    val name: String,
    val race: String,
    val characterClass: String,
    val level: Int
)

/**
 * Un import (livre de la bibliothèque) partagé par le MJ. [empreinte] : empreinte de son texte,
 * pour reconnaître le même livre chez le joueur quel que soit son nom ; [taille] en octets.
 */
@Serializable
data class ImportPartageData(
    val id: String,
    val nom: String,
    val worldId: String,
    val empreinte: String,
    val taille: Long = 0,
)

/** Instance Json partagée pour l'encodage/décodage des messages réseau. */
val networkJson = Json { ignoreUnknownKeys = true }