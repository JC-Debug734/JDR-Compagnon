package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Combat de boss : un adversaire unique qui doit tenir tête à tout le groupe. Ce qui le rend
 * intéressant (et le distingue d'un gros sac de PV) :
 * - il agit hors de son tour : ACTIONS LÉGENDAIRES après le tour d'un adversaire, ACTIONS DE
 *   REPAIRE à la fin de chaque round et quand un adversaire rate une attaque sans avoir été
 *   blessé du round ;
 * - il change pendant le combat : PHASES déclenchées par ses PV (profil IA, CA, renforts…) ;
 * - il ne se fait pas neutraliser d'un seul sort : RÉSISTANCE LÉGENDAIRE (JS raté → réussi, x/jour)
 *   et CONTRÔLES ATTÉNUÉS (il se libère d'un état bloquant au début de son tour) ;
 * - il récompense la tactique : ATTAQUE ANNONCÉE un round à l'avance (les joueurs peuvent
 *   l'interrompre en le frappant assez fort avant son tour), JAUGE DE DÉSÉQUILIBRE, POINTS
 *   FAIBLES à détruire (objectifs secondaires).
 *
 * Un boss part d'une fiche du bestiaire ([BossDefinition.monstreBase]) : CA, PV, attaques et
 * sauvegardes en viennent ; la définition n'ajoute que ce qui en fait un boss.
 */

/** Ce que fait une action légendaire ou de repaire. */
enum class FormeActionBoss(val label: String) {
    // Jet d'attaque contre une cible à portée.
    ATTAQUE("Attaque"),
    // JS imposé à [ActionBoss.nbCibles] adversaire(s) à portée (au hasard pour le repaire).
    SAUVEGARDE_CIBLES("Jet de sauvegarde"),
    // JS imposé à chaque adversaire au contact du boss.
    SAUVEGARDE_CONTACT("Autour de lui"),
    // JS imposé à tous les adversaires debout.
    SAUVEGARDE_TOUS("Tout le monde"),
}

/**
 * Action légendaire (coût en points) ou de repaire (coût ignoré). [description] est le texte
 * d'ambiance lu aux joueurs ; les dégâts (moitié sur un JS réussi si [moitieSiReussi]) et les
 * [etats] (sur une attaque qui touche ou un JS raté) sont appliqués par l'application.
 * [repli] : après l'action, le boss s'écarte de ses adversaires au contact, sans attaque
 * d'opportunité (battement d'ailes, bond…).
 */
@Serializable
data class ActionBoss(
    val nom: String,
    val description: String,
    val forme: FormeActionBoss,
    val cout: Int = 1,
    val bonusToucher: Int? = null,
    val sauvegarde: String? = null,
    val dd: Int? = null,
    val degats: String? = null,
    val typeDegats: String? = null,
    val moitieSiReussi: Boolean = true,
    val etats: List<ConditionCombat> = emptyList(),
    val portee: Double = Distance.CONTACT.metres,
    val nbCibles: Int = 1,
    val repli: Boolean = false,
) {
    /** « Coup de queue (1) · +7 · 2d8 + 4 contondants », « Verglas · JS Dextérité DD 13 · À terre ». */
    val resume: String
        get() = buildString {
            append(nom)
            if (cout > 1) append(" ($cout points)")
            bonusToucher?.let { append(" · ").append(if (it >= 0) "+$it" else "$it") }
            if (sauvegarde != null && dd != null) append(" · JS $sauvegarde DD $dd")
            degats?.let { append(" · ").append(it) }
            typeDegats?.let { append(" ").append(it) }
            if (etats.isNotEmpty()) append(" · ").append(etats.joinToString { it.label })
        }
}

/**
 * Phase du boss, active dès que ses PV passent sous [seuilPv] % (la première : 100). En y
 * entrant, [annonce] est lue à toute la table, son comportement passe à [profil], sa CA varie de
 * [modificateurCa] (par rapport à la fiche), il gagne [bonusPointsLegendaires], récupère ses
 * capacités à recharge ([rechargeImmediate]), se libère de ses états bloquants et appelle ses
 * [renforts] (fiches du bestiaire). [repaireActif] : le repaire agit à chaque round.
 */
@Serializable
data class PhaseBoss(
    val nom: String,
    val seuilPv: Int,
    val annonce: String,
    val profil: ProfilIA? = null,
    val modificateurCa: Int = 0,
    val bonusPointsLegendaires: Int = 0,
    val rechargeImmediate: Boolean = false,
    val repaireActif: Boolean = false,
    val renforts: List<LigneCompositionCombat> = emptyList(),
)

/** Attaque du boss annoncée aux joueurs pendant la déclaration, avant d'être résolue à son tour. */
@Serializable
data class AttaqueAnnoncee(
    // Nom de l'action sur la fiche du bestiaire (ex. « Souffle glacial »).
    val attaqueNom: String,
    val annonce: String,
    val interruption: String,
)

/**
 * Objectif secondaire : élément du décor qui soutient le boss (pilier, cristal, autel…). Il ne
 * joue pas, se cible comme un adversaire et le boss [BossDefinition.regeneration] tant qu'il en
 * reste un debout.
 */
@Serializable
data class PointFaibleBoss(
    val nom: String,
    val ca: Int,
    val pv: Int,
    val description: String,
    val defenses: DefensesDegats = DefensesDegats(),
)

@Serializable
data class BossDefinition(
    val id: String,
    val nom: String,
    val titre: String,
    val monstreBase: String,
    // Texte lu à la table au début du combat, et à sa chute.
    val presentation: String,
    val defaite: String,
    // PV prévus pour un groupe de 4 (sinon ceux de la fiche) ; ajustés au nombre de personnages.
    val pvPour4: Int? = null,
    val pointsLegendaires: Int = 3,
    val resistancesLegendaires: Int = 3,
    val actionsLegendaires: List<ActionBoss> = emptyList(),
    val actionsRepaire: List<ActionBoss> = emptyList(),
    val phases: List<PhaseBoss> = emptyList(),
    val attaqueAnnoncee: AttaqueAnnoncee? = null,
    // Dégâts (en % de ses PV max) à lui infliger entre deux de ses tours pour le déséquilibrer.
    val seuilDesequilibrePct: Int = 20,
    val pointsFaibles: List<PointFaibleBoss> = emptyList(),
    // PV regagnés au début de son tour tant qu'un point faible tient debout.
    val regeneration: Int = 0,
    // Malus de CA quand tous les points faibles sont détruits, et l'annonce qui l'accompagne.
    val malusCaSansPointsFaibles: Int = 0,
    val annoncePointsFaiblesDetruits: String? = null,
    // Butin propre au boss, ajouté à celui de sa fiche.
    val butin: List<String> = emptyList(),
    // Illustration du boss, chemin dans les assets de l'appli (ex. "dnd/boss/cryovain.webp") :
    // présente chez le MJ comme chez les joueurs. Voir BossCatalogue.imageDe.
    val image: String? = null,
)

/**
 * État d'un boss pendant le combat (CombatEnCours.boss, clé = id du combattant). [jauge] : dégâts
 * subis depuis son dernier tour (ou depuis l'annonce de son attaque) ; [prepare] : son attaque
 * annoncée n'a pas encore été résolue ; [desequilibre] : jusqu'à son prochain tour, plus d'action
 * légendaire. [annonces] : dernières annonces lues à la table (visibles des joueurs).
 */
data class EtatBoss(
    val definition: BossDefinition,
    val caBase: Int,
    val pointsMaxBase: Int,
    val phase: Int = 0,
    val points: Int = pointsMaxBase,
    val resistances: Int = definition.resistancesLegendaires,
    val jauge: Int = 0,
    val prepare: Boolean = false,
    val desequilibre: Boolean = false,
    val dernierRepaire: String? = null,
    // Adversaires blessés ce round, et ceux dont l'attaque ratée a déjà réveillé le repaire ce
    // round (une fois par personnage et par round) ; remis à zéro à chaque nouveau round.
    val blessesDuRound: Set<String> = emptySet(),
    val repaireRatesDuRound: Set<String> = emptySet(),
    val pointsFaiblesIds: List<String> = emptyList(),
    val pointsFaiblesDetruits: Boolean = false,
    val renforts: Map<Int, List<Combattant>> = emptyMap(),
    val annonces: List<AnnonceBoss> = emptyList(),
    // Actions légendaires jouées seules par l'IA (sinon le MJ les déclenche).
    val legendairesAuto: Boolean = true,
    val vaincu: Boolean = false,
    // Récapitulatif de fin de combat.
    val degatsParAuteur: Map<String, Int> = emptyMap(),
    val coupFinal: String? = null,
    val resistancesForcees: Int = 0,
    val interruptions: Int = 0,
) {
    val phaseCourante: PhaseBoss? get() = definition.phases.getOrNull(phase)
    val pointsMax: Int get() = pointsMaxBase + (phaseCourante?.bonusPointsLegendaires ?: 0)
    val repaireActif: Boolean get() = phaseCourante?.repaireActif == true && definition.actionsRepaire.isNotEmpty()
    val caActuelle: Int
        get() = caBase + (phaseCourante?.modificateurCa ?: 0) - if (pointsFaiblesDetruits) definition.malusCaSansPointsFaibles else 0

    fun seuilDesequilibre(pvMax: Int): Int = (pvMax * definition.seuilDesequilibrePct / 100).coerceAtLeast(5)
}

/**
 * Texte du boss pour le MJ, daté du round. [aLire] : mise en scène que le MJ lit à voix haute
 * (entrée en scène, phase, attaque annoncée…), affichée dans l'encadré « À lire » jusqu'à ce
 * qu'il la marque [lue] ; sinon simple information de règle (résistance légendaire…). Jamais
 * envoyé tel quel aux joueurs ni à l'écran de la table.
 */
data class AnnonceBoss(
    val round: Int,
    val texte: String,
    val aLire: Boolean = true,
    val lue: Boolean = false,
    // Ce que l'application a résolu (jets, dégâts et états déjà appliqués), affiché sous le texte.
    val resultat: List<String> = emptyList(),
    // Combattants touchés tels qu'avant l'action : « Ne pas appliquer » les remet ainsi.
    val avant: List<Combattant> = emptyList(),
    val annulee: Boolean = false,
)

/**
 * Boss disponibles (outil BOSS ; en combat : lien `#combat:[Cryovain]` ou ajout en cours de
 * combat). Les boss intégrés (Cryovain…) restent modifiables et réinitialisables ; le MJ peut en
 * créer d'autres. Persisté en SharedPreferences, comme les comportements.
 */
object BossCatalogue {

    private const val PREFS = "boss_mj"
    private const val CLE = "boss_json"
    private val json = Json { ignoreUnknownKeys = true }
    private var appContext: Context? = null

    /**
     * Cryovain, le jeune dragon blanc qui terrorise la région de Phandaline depuis les ruines au
     * sommet du Pic de Givre (campagne « Le dragon du Pic de Givre »). Combat prévu pour 4
     * personnages de niveau 5–6 ; les PV et les actions légendaires s'ajustent au groupe.
     */
    val CRYOVAIN = BossDefinition(
        id = "cryovain",
        nom = "Cryovain",
        titre = "Le fléau du Pic de Givre",
        monstreBase = "Jeune dragon blanc",
        image = "dnd/boss/cryovain.webp",
        presentation = "Un cri strident déchire le vent : Cryovain fond du ciel et se pose sur les ruines du sommet. " +
            "Ses écailles blanches crissent comme la glace, ses yeux d'un bleu pâle vous jaugent comme des proies.",
        defaite = "Cryovain s'effondre dans un fracas de glace brisée. Le vent retombe : le Pic de Givre est libéré.",
        pvPour4 = 123,
        pointsLegendaires = 3,
        resistancesLegendaires = 2,
        actionsLegendaires = listOf(
            ActionBoss(
                "Coup de queue", "Cryovain fouette l'air de sa queue hérissée de glace.",
                FormeActionBoss.ATTAQUE, cout = 1, bonusToucher = 7, degats = "2d8+4", typeDegats = "contondants",
                portee = Distance.M4_5.metres,
            ),
            ActionBoss(
                "Rafale glaciale", "Cryovain souffle un trait de givre sur un intrus isolé.",
                FormeActionBoss.SAUVEGARDE_CIBLES, cout = 1, sauvegarde = "Constitution", dd = 13,
                degats = "2d6", typeDegats = "froid", portee = Distance.M18.metres,
            ),
            ActionBoss(
                "Battement d'ailes", "Cryovain déploie ses ailes : une bourrasque de neige renverse tout ce qui l'entoure, puis il s'élève hors d'atteinte.",
                FormeActionBoss.SAUVEGARDE_CONTACT, cout = 2, sauvegarde = "Dextérité", dd = 15,
                degats = "2d6+4", typeDegats = "contondants", etats = listOf(ConditionCombat.A_TERRE),
                portee = Distance.M3.metres, repli = true,
            ),
        ),
        actionsRepaire = listOf(
            ActionBoss(
                "Chute de stalactites", "Le cri du dragon fait trembler la tour : des stalactites se détachent de la voûte.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Dextérité", dd = 13,
                degats = "3d6", typeDegats = "perforants", portee = Distance.M120.metres, nbCibles = 1,
            ),
            ActionBoss(
                "Bourrasque de neige", "Une rafale glacée balaie le sommet et mord la chair.",
                FormeActionBoss.SAUVEGARDE_TOUS, sauvegarde = "Constitution", dd = 12,
                degats = "1d10", typeDegats = "froid",
            ),
            ActionBoss(
                "Verglas", "Le sol se couvre d'une pellicule de glace vive.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Dextérité", dd = 13,
                etats = listOf(ConditionCombat.A_TERRE), portee = Distance.M120.metres, nbCibles = 2, moitieSiReussi = false,
            ),
            ActionBoss(
                "Crevasse", "La glace craque sous ses pas : sa jambe s'enfonce jusqu'au genou et reste coincée dans la fissure.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Force", dd = 13,
                degats = "1d6", typeDegats = "froid", etats = listOf(ConditionCombat.ENTRAVE),
                portee = Distance.M120.metres, moitieSiReussi = false,
            ),
            ActionBoss(
                "Glissade vers le vide", "Le sol en pente se dérobe : il glisse sur la glace vers le bord de la falaise et heurte la roche.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Dextérité", dd = 13,
                degats = "2d6", typeDegats = "contondants", etats = listOf(ConditionCombat.A_TERRE),
                portee = Distance.M120.metres, moitieSiReussi = false,
            ),
            ActionBoss(
                "Coulée de neige", "Un pan de neige se détache de la corniche et s'abat sur le groupe.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Force", dd = 13,
                degats = "2d8", typeDegats = "contondants", etats = listOf(ConditionCombat.A_TERRE),
                portee = Distance.M120.metres, nbCibles = 2,
            ),
            ActionBoss(
                "Éclats de glace", "Une colonne de glace se fend et explose en une gerbe d'éclats tranchants.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Dextérité", dd = 13,
                degats = "2d6", typeDegats = "tranchants", portee = Distance.M120.metres, nbCibles = 2,
            ),
            ActionBoss(
                "Pointe de glace", "Une stalagmite de glace jaillit du sol, juste sous ses pieds.",
                FormeActionBoss.ATTAQUE, bonusToucher = 6, degats = "2d6", typeDegats = "perforants",
                portee = Distance.M120.metres,
            ),
            ActionBoss(
                "Givre aveuglant", "Un tourbillon de cristaux de glace lui fouette le visage et lui colle aux paupières.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Constitution", dd = 12,
                etats = listOf(ConditionCombat.AVEUGLE), portee = Distance.M120.metres, moitieSiReussi = false,
            ),
            ActionBoss(
                "Morsure du froid", "Le froid du sommet s'insinue sous l'armure : ses doigts s'engourdissent, ses articulations se figent.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Constitution", dd = 13,
                degats = "2d6", typeDegats = "froid", portee = Distance.M120.metres,
            ),
            ActionBoss(
                "Écho du rugissement", "Le cri du dragon se répercute contre les parois et revient, amplifié, glacer le sang.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Sagesse", dd = 13,
                etats = listOf(ConditionCombat.EFFRAYE), portee = Distance.M120.metres, moitieSiReussi = false,
            ),
            ActionBoss(
                "Mains gelées", "Les restes gelés d'anciens aventuriers, prisonniers de la glace, lui agrippent les chevilles.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Force", dd = 12,
                etats = listOf(ConditionCombat.AGRIPPE), portee = Distance.M120.metres, moitieSiReussi = false,
            ),
            ActionBoss(
                "Vent hurlant", "Une rafale assourdissante s'engouffre entre les ruines et couvre tous les sons.",
                FormeActionBoss.SAUVEGARDE_CIBLES, sauvegarde = "Constitution", dd = 12,
                degats = "1d6", typeDegats = "froid", etats = listOf(ConditionCombat.SOURD),
                portee = Distance.M120.metres, moitieSiReussi = false,
            ),
        ),
        phases = listOf(
            PhaseBoss(
                "Le seigneur des cimes", 100,
                "Cryovain jauge ses proies avec mépris et choisit la plus fragile.",
                profil = ProfilIA.TACTICIEN,
                repaireActif = true,
            ),
            PhaseBoss(
                "Tempête sur le pic", 66,
                "Cryovain pousse un rugissement qui fait vibrer la montagne : le ciel s'assombrit, " +
                    "une tempête de neige s'abat sur le sommet et un loup arctique surgit de la tourmente !",
                profil = ProfilIA.TACTICIEN,
                repaireActif = true,
                renforts = listOf(LigneCompositionCombat("Loup arctique", 1)),
            ),
            PhaseBoss(
                "Fureur du dragon", 33,
                "Blessé, Cryovain devient fou de rage ! Ses écailles se fendent, mais le givre se reforme déjà dans sa gorge…",
                profil = ProfilIA.BERSERKER,
                modificateurCa = -2,
                bonusPointsLegendaires = 1,
                rechargeImmediate = true,
                repaireActif = true,
            ),
        ),
        attaqueAnnoncee = AttaqueAnnoncee(
            attaqueNom = "Souffle glacial",
            annonce = "Cryovain se cabre et inspire profondément : du givre s'échappe de ses naseaux, l'air se fige autour de lui… " +
                "Frappez-le fort avant son tour, ou dispersez-vous !",
            interruption = "Sous vos coups, Cryovain s'étrangle : son souffle se brise en une pluie de cristaux inoffensifs. Il vacille, déséquilibré !",
        ),
        seuilDesequilibrePct = 20,
        butin = listOf("Écaille de Cryovain", "Croc de dragon blanc", "Cœur de givre"),
    )

    /** Boss livrés avec l'appli. */
    val integres: List<BossDefinition> = listOf(CRYOVAIN)

    // Boss enregistrés par le MJ : intégrés modifiés (même id) et nouveaux.
    private val _personnalises = MutableStateFlow<List<BossDefinition>>(emptyList())
    private val _tous = MutableStateFlow(calculerTous())
    /** Tous les boss : intégrés (modifiés le cas échéant) puis ceux créés par le MJ. */
    val tous: StateFlow<List<BossDefinition>> = _tous.asStateFlow()

    private fun calculerTous(): List<BossDefinition> {
        val perso = _personnalises.value
        return integres.map { b -> perso.firstOrNull { it.id == b.id } ?: b } + perso.filter { p -> integres.none { it.id == p.id } }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        runCatching { json.decodeFromString<List<BossDefinition>>(prefs.getString(CLE, "[]").orEmpty()) }
            .getOrNull()?.let { _personnalises.value = it }
        _tous.value = calculerTous()
    }

    private fun sauvegarder() {
        _tous.value = calculerTous()
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()
            ?.putString(CLE, json.encodeToString(_personnalises.value))
            ?.apply()
    }

    fun estIntegre(id: String): Boolean = integres.any { it.id == id }

    /** Boss intégré modifié par le MJ (« Réinitialiser » le ramène à l'original). */
    fun estModifie(id: String): Boolean = estIntegre(id) && _personnalises.value.any { it.id == id }

    /** Crée ou modifie un boss (un intégré modifié garde son id). */
    fun enregistrer(def: BossDefinition) {
        _personnalises.value = _personnalises.value.filterNot { it.id == def.id } + def
        sauvegarder()
    }

    /** Intégré : retour à l'original. Créé par le MJ : supprimé. */
    fun supprimerOuReinitialiser(id: String) {
        _personnalises.value = _personnalises.value.filterNot { it.id == id }
        sauvegarder()
    }

    fun parId(id: String?): BossDefinition? = id?.let { i -> _tous.value.firstOrNull { it.id == i } }

    /**
     * Illustration de [def] : la sienne, sinon celle du boss intégré de même id (un Cryovain
     * modifié et enregistré avant l'ajout des images garde ainsi la sienne).
     */
    fun imageDe(def: BossDefinition): String? =
        if (def.image == SANS_IMAGE) null else def.image ?: integres.firstOrNull { it.id == def.id }?.image

    /** Valeur de BossDefinition.image pour un boss dont le MJ a retiré l'image (même un intégré). */
    const val SANS_IMAGE = ""

    /**
     * Copie l'image choisie par le MJ ([uri]) dans le stockage interne de l'appli et renvoie son
     * chemin absolu, à ranger dans BossDefinition.image ; null si elle n'a pas pu être lue. Un
     * nouveau fichier par choix : l'ancienne image reste valable tant que le boss n'est pas enregistré.
     */
    fun importerImage(context: Context, bossId: String, uri: android.net.Uri): String? = runCatching {
        val dossier = java.io.File(context.filesDir, "boss_images").apply { mkdirs() }
        val fichier = java.io.File(dossier, "${bossId}_${System.currentTimeMillis()}.img")
        context.contentResolver.openInputStream(uri)?.use { entree -> fichier.outputStream().use { entree.copyTo(it) } }
            ?: return@runCatching null
        // Images des choix précédents de ce boss (sauf la nouvelle et celle enregistrée) : supprimées.
        val enregistree = parId(bossId)?.image
        dossier.listFiles()?.filter { it.name.startsWith("${bossId}_") && it != fichier && it.absolutePath != enregistree }?.forEach { it.delete() }
        fichier.absolutePath
    }.getOrNull()

    /** Boss d'un lien de combat (« Cryovain »), sans tenir compte de la casse. */
    fun parNom(nom: String): BossDefinition? = _tous.value.firstOrNull { it.nom.equals(nom.trim(), ignoreCase = true) }

    /**
     * Ajustement au groupe (prévu pour 4) : PV ×(n / 4) entre 60 % et 150 %, et 2 points
     * d'actions légendaires jusqu'à 2 personnages, 3 jusqu'à 4, 4 au-delà (une action de plus que
     * prévu tous les 2 joueurs supplémentaires, en somme).
     *
     * Puis la [difficulte] choisie au lancement (null = celle prévue par le scénario) : PV ×0,7
     * (Facile), ×0,85 (Moyenne), ×1,2 (Difficile), ×1,4 (Mortelle) ; une action et une résistance
     * légendaires de moins en Facile, de plus en Mortelle.
     */
    fun pvAjustes(def: BossDefinition, pvFiche: Int, nbPersonnages: Int, difficulte: EncounterDifficulty? = null): Int {
        val base = def.pvPour4 ?: pvFiche
        val facteur = (nbPersonnages.coerceAtLeast(1) / 4.0).coerceIn(0.6, 1.5)
        return (base * facteur * facteurPv(difficulte)).toInt().coerceAtLeast(1)
    }

    fun pointsAjustes(def: BossDefinition, nbPersonnages: Int, difficulte: EncounterDifficulty? = null): Int {
        val groupe = when {
            def.pointsLegendaires <= 0 -> return 0
            nbPersonnages <= 2 -> (def.pointsLegendaires - 1).coerceAtLeast(1)
            nbPersonnages <= 4 -> def.pointsLegendaires
            else -> def.pointsLegendaires + 1
        }
        return (groupe + ecartLegendaire(difficulte)).coerceAtLeast(1)
    }

    fun resistancesAjustees(def: BossDefinition, difficulte: EncounterDifficulty? = null): Int =
        if (def.resistancesLegendaires <= 0) 0 else (def.resistancesLegendaires + ecartLegendaire(difficulte)).coerceAtLeast(0)

    private fun facteurPv(difficulte: EncounterDifficulty?): Double = when (difficulte) {
        EncounterDifficulty.FACILE -> 0.7
        EncounterDifficulty.MOYENNE -> 0.85
        EncounterDifficulty.DIFFICILE -> 1.2
        EncounterDifficulty.MORTELLE -> 1.4
        null -> 1.0
    }

    private fun ecartLegendaire(difficulte: EncounterDifficulty?): Int = when (difficulte) {
        EncounterDifficulty.FACILE -> -1
        EncounterDifficulty.MORTELLE -> 1
        else -> 0
    }

    /** Phase à atteindre pour un boss à [ratioPct] % de ses PV (la dernière dont le seuil est dépassé). */
    fun phasePour(def: BossDefinition, ratioPct: Int): Int =
        def.phases.indices.lastOrNull { i -> i == 0 || ratioPct < def.phases[i].seuilPv } ?: 0
}
