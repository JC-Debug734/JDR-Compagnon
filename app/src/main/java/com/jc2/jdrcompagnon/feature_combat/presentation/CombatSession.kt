package com.jc2.jdrcompagnon.feature_combat.presentation

import com.jc2.jdrcompagnon.feature_combat.domain.model.Terrain
import com.jc2.jdrcompagnon.feature_combat.domain.model.Dons
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.EffetDefense
import com.jc2.jdrcompagnon.feature_combat.domain.model.Occultiste
import com.jc2.jdrcompagnon.feature_combat.domain.model.Paladin
import com.jc2.jdrcompagnon.feature_combat.domain.model.Guerrier
import com.jc2.jdrcompagnon.feature_combat.domain.model.Roublard
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypesCreature
import com.jc2.jdrcompagnon.feature_combat.domain.model.Sauvegardes
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerFormuleDes
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneCompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportement
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportements
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeZone
import com.jc2.jdrcompagnon.feature_combat.domain.model.ZoneEffet
import com.jc2.jdrcompagnon.feature_combat.domain.model.formatMetres
import com.jc2.jdrcompagnon.feature_combat.domain.model.vitesseEnMetres
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.AnnonceBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeActionBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.IaMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ModeJet
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.RegleProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.ResultatAttaque
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerAttaqueMonstre
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Déroulé du combat, en tour par tour : chaque combattant agit À SON TOUR, dans l'ordre
 * d'initiative. Les monstres (IA) décident au début de leur tour avec la situation du moment, les
 * joueurs choisissent leur action quand vient le leur (sur leur appareil, sur la page web de la
 * table, ou annoncée au MJ qui la saisit).
 */
enum class PhaseCombat(val label: String) {
    PREPARATION("Préparation"),
    EN_COURS("Combat en cours")
}

/**
 * Action d'un combattant pour son tour. [actionId] renvoie au catalogue ActionsCombat (null =
 * action libre ou décision de l'IA d'un monstre).
 */
data class DeclarationAction(
    val combattantId: String,
    val libelle: String,
    val actionId: String? = null,
    val cibleId: String? = null,
    val cibleNom: String? = null,
    val detail: String? = null,
    val parIa: Boolean = false,
    val parMj: Boolean = false,
    val attaque: AttaqueMonstre? = null,
    val nbAttaques: Int = 1,
    // Distance à chaque adversaire concerné après le déplacement (appliquée quand l'action est résolue).
    val deplacements: Map<String, Distance> = emptyMap(),
    // Zone du terrain visée (joueur, MJ) ; prioritaire sur [deplacements], que l'IA exprime par
    // rapport à ses cibles (traduits en zone au moment du déplacement).
    val zoneVisee: Int? = null,
    // Quitte le contact sans provoquer d'attaque d'opportunité (action Se désengager).
    val desengage: Boolean = false,
    // Monstres et PNJ : toucher et dégâts lancés dès la décision (contre la CA de la cible), pour
    // que le MJ n'ait plus qu'à appliquer.
    val resultats: List<ResultatAttaque>? = null,
    val degatsAppliques: Boolean = false,
    // Joueur : attaques choisies pour l'action Attaquer (une par jet « Attaque » autorisé).
    val attaquesJoueur: List<String> = emptyList(),
    // Joueur, sort de zone : toutes les créatures visées (le MJ leur applique les dégâts du jet).
    val ciblesZone: List<String> = emptyList(),
)

/** Avantage/désavantage d'une attaque d'après les états, et critique automatique au contact. */
data class ModeAttaque(
    val mode: ModeJet = ModeJet.NORMAL,
    val raisons: List<String> = emptyList(),
    val critiqueAuto: Boolean = false,
)

/** Clé d'une paire de combattants dans [CombatEnCours.distances] (ordre indifférent). */
internal fun cleDistance(a: String, b: String): String = if (a < b) "$a|$b" else "$b|$a"

/** Jet de dés envoyé par un joueur (ou saisi par le MJ) pendant le combat. */
data class JetCombat(
    val id: String = UUID.randomUUID().toString(),
    val combattantId: String,
    val round: Int,
    val libelle: String,
    val formule: String,
    val des: List<Int>,
    val bonus: Int,
    val total: Int,
    val manuel: Boolean,
    // Jet d'attaque d'un joueur : dégâts lancés en même temps, le MJ décide de les appliquer.
    val formuleDegats: String? = null,
    val desDegats: List<Int> = emptyList(),
    val totalDegats: Int? = null,
    // Cible visée par ce jet (ex. Imposition des mains), à défaut celle de la déclaration.
    val cibleId: String? = null,
    // Dégâts ou soins déjà appliqués (ou écartés) par le MJ.
    val applique: Boolean = false,
    // Arme ou sort du jet et type de ses dégâts : résistances, immunités, bonus contre un type de créature.
    val arme: String? = null,
    val typeDegats: String? = null,
)

/**
 * Combat en cours côté MJ. [combattants] est trié par ordre d'initiative une fois celle-ci
 * lancée ; [tourIndex] désigne le combattant dont c'est le tour dans cette liste. [journal] garde
 * le détail technique (dés, bonus, DD) ; [recit] raconte le combat, lisible par toute la table.
 */
data class CombatEnCours(
    val id: String,
    val titre: String,
    val combattants: List<Combattant>,
    val round: Int = 0,
    val tourIndex: Int = 0,
    val journal: List<String> = emptyList(),
    val phase: PhaseCombat = PhaseCombat.PREPARATION,
    val declarations: Map<String, DeclarationAction> = emptyMap(),
    val jets: List<JetCombat> = emptyList(),
    // Distance entre chaque monstre et chaque personnage (clé cleDistance) ; courte par défaut.
    val distances: Map<String, Distance> = emptyMap(),
    // Jets supplémentaires accordés par le MJ à un combattant ce round (sinon un seul jet par type).
    val relances: Map<String, Int> = emptyMap(),
    // Tous les ennemis sont tombés : le combat se termine (butin envoyé au groupe, voir FinDeCombat).
    val termine: Boolean = false,
    // Boss du combat (clé = id du combattant) : phases, actions légendaires, annonces…
    val boss: Map<String, EtatBoss> = emptyMap(),
    val recit: List<Recit> = emptyList(),
) {
    /** Il y avait des ennemis, et aucun n'est encore debout (les points faibles inertes ne comptent pas). */
    val victoire: Boolean get() = combattants.any { it.estMonstre && !it.inerte } && combattants.none { it.estMonstre && !it.inerte && !it.horsCombat }

    /**
     * Distance entre deux combattants, d'après leurs zones du champ de bataille (voir Terrain) :
     * même zone = au contact (adversaires) ou groupés (alliés), puis 9 m par zone d'écart.
     */
    fun distance(a: String, b: String): Distance {
        val ca = combattants.firstOrNull { it.id == a }
        val cb = combattants.firstOrNull { it.id == b }
        return if (ca != null && cb != null) Terrain.distance(ca, cb) else Distance.COURTE
    }

    /** Combattants debout dans [zone]. */
    fun dansZone(zone: Int): List<Combattant> = combattants.filter { !it.inerte && !it.mort && Terrain.zoneDe(it) == zone }

    /**
     * Créatures prises dans [zone] : autour de la cible visée (sphère, cube, cylindre), ou du
     * lanceur pour un cône, une ligne ou une émanation (le lanceur lui-même exclu). Un cône ou une
     * ligne ne touche que le camp adverse du lanceur (la zone part dans une direction).
     */
    fun dansLaZone(zone: ZoneEffet, lanceurId: String, cibleId: String?): List<Combattant> {
        val lanceur = combattants.firstOrNull { it.id == lanceurId }
        val centreId = if (zone.forme.depuisLanceur || cibleId == null) lanceurId else cibleId
        return combattants.filter { c ->
            !c.horsCombat && c.id != lanceurId &&
                (c.id == centreId || distance(centreId, c.id).metres <= zone.portee + 0.01) &&
                !(zone.forme in setOf(FormeZone.CONE, FormeZone.LIGNE) && lanceur != null && c.estMonstre == lanceur.estMonstre)
        }
    }

    /** Distances de [combattantId] à chaque combattant du camp adverse. */
    fun distancesDe(combattantId: String): Map<String, Distance> {
        val c = combattants.firstOrNull { it.id == combattantId } ?: return emptyMap()
        return combattants.filter { it.estMonstre != c.estMonstre }.associate { it.id to distance(c.id, it.id) }
    }

    /** Combattants encore debout au contact de [combattantId] (tous camps confondus). */
    fun auContactDe(combattantId: String): List<Combattant> =
        combattants.filter { it.id != combattantId && !it.horsCombat && distance(combattantId, it.id) == Distance.CONTACT }

    /** « Au contact de X, Y » ou « À distance » : position de [combattantId] pour les vues tactiques. */
    fun libelleContact(combattantId: String): String =
        auContactDe(combattantId).takeIf { it.isNotEmpty() }?.joinToString(prefix = "Au contact de ") { it.nom } ?: "À distance"

    /** Tant que round == 0, on est en préparation (initiatives à lancer/saisir). */
    val demarre: Boolean get() = round > 0
    val actif: Combattant? get() = if (phase == PhaseCombat.EN_COURS) combattants.getOrNull(tourIndex) else null
    val monstresRestants: Int get() = combattants.count { it.estMonstre && !it.inerte && !it.horsCombat }

    /**
     * Nom affiché du combat, d'après les adversaires réellement présents (« Nécronte », « Gobelin x3,
     * Loup ») : [titre] garde la composition prévue par le scénario, qui a pu être ajustée au
     * lancement ou en cours de combat. Sans adversaire, [titre].
     */
    val nomCombat: String
        get() = combattants.filter { it.estMonstre && !it.inerte }
            .groupingBy { it.monstreNom ?: it.nom }.eachCount()
            .map { (nom, n) -> LigneCompositionCombat(nom, n) }
            .takeIf { it.isNotEmpty() }
            ?.let { CompositionCombat.formater(it) } ?: titre

    /** Combattant qui jouera après l'actif (ce round, sinon le premier du round suivant). */
    val prochain: Combattant?
        get() {
            if (phase != PhaseCombat.EN_COURS) return null
            val suivants = combattants.drop(tourIndex + 1) + combattants.take(tourIndex + 1)
            return suivants.firstOrNull { !it.inerte && !(it.estMonstre && it.horsCombat) && !it.mort && it.id != actif?.id }
        }

    /** Dernier combattant à jouer ce round : le bouton « Tour suivant » ouvre alors un nouveau round. */
    val dernierDuRound: Boolean
        get() = combattants.drop(tourIndex + 1).none { !it.inerte && !(it.estMonstre && it.horsCombat) && !it.mort }

    val jetsDuRound: List<JetCombat> get() = jets.filter { it.round == round }
}

/**
 * Combat actif au niveau application (comme EpreuveOutilSession) : survit à la navigation, pour que
 * le MJ puisse revenir au scénario puis reprendre le combat depuis le même lien #combat:.
 *
 * Temps de fiction : tant qu'un combat est ouvert, l'horloge de scénario est en pause
 * (ScenarioClockState.setPauseCombat) et chaque round écoulé l'avance de 6 secondes, durée d'un
 * round dans les règles — les effets à durée (potions, sorts) s'écoulent ainsi round par round.
 */
object CombatSession {

    private val _etat = MutableStateFlow<CombatEnCours?>(null)
    val etat: StateFlow<CombatEnCours?> = _etat.asStateFlow()

    /** Tirages de l'IA et des jets automatiques (remplaçable dans les tests). */
    internal var aleatoire: Random = Random

    fun demarrer(titre: String, combattants: List<Combattant>, boss: List<BossPrepare> = emptyList()) {
        // Temps de combat : l'horloge de scénario s'arrête, chaque round l'avancera de 6 s.
        ScenarioClockState.setPauseCombat(true)
        val tous = combattants + boss.flatMap { it.combattants }
        val initial = CombatEnCours(
            id = UUID.randomUUID().toString(),
            titre = titre,
            // Initiative lancée d'office pour tout le monde : le MJ corrige ensuite à la main la
            // valeur d'un joueur qui a lancé son propre dé.
            combattants = trier(tous.map { it.copy(initiative = it.initiative ?: jetInitiative(it)) }),
            journal = listOf("Combat préparé : ${tous.size} combattant(s)")
        )
        _etat.value = boss.fold(initial) { e, b -> inscrireBoss(e, b) }
    }

    /** Fait entrer un boss préparé (PreparationBoss) dans le combat en cours. */
    fun ajouterBoss(prepare: BossPrepare) = modifier { etat ->
        insererCombattants(inscrireBoss(etat, prepare), prepare.combattants)
    }

    private fun inscrireBoss(etat: CombatEnCours, prepare: BossPrepare): CombatEnCours {
        val inscrit = etat.copy(boss = etat.boss + (prepare.bossCombattantId to prepare.etat))
        val def = prepare.etat.definition
        val premiere = def.phases.firstOrNull()?.let { " ${it.annonce}" }.orEmpty()
        return annoncer(inscrit, prepare.bossCombattantId, "${def.nom}, ${def.titre.replaceFirstChar { it.lowercase() }} : ${def.presentation}$premiere", table = "${def.nom} entre en scène")
    }

    fun fermer() {
        // Le dernier round entamé compte aussi, puis l'horloge reprend son cours.
        if (_etat.value?.demarre == true) ScenarioClockState.advanceSeconds(ScenarioClockState.SECONDES_PAR_ROUND)
        _etat.value = null
        ScenarioClockState.setPauseCombat(false)
    }

    /**
     * PV et états des combattants liés à une fiche, relus depuis GameState : un joueur qui boit
     * une potion sur son appareil, ou un état coché dans le menu latéral MJ, doivent compter
     * dans le combat.
     */
    fun synchroniserFiches(personnages: List<Character>) = modifier { etat ->
        var courant = etat
        etat.combattants.forEach { c ->
            val fiche = c.characterId?.let { id -> personnages.firstOrNull { it.id == id } } ?: return@forEach
            // PV temporaires non relus : le combat les décompte sans les reporter sur la fiche.
            if (fiche.currentHitPoints != c.pv) {
                courant = remplacer(courant, c.copy(pv = fiche.currentHitPoints.coerceIn(0, c.pvMax)))
            }
            val etatsFiche = Etats.lire(fiche.condition)
            if (etatsFiche != c.conditions) {
                val actuel = courant.combattants.first { it.id == c.id }
                courant = changerConditions(courant, actuel, etatsFiche, reporterSurFiche = false)
            }
        }
        courant
    }

    /**
     * Personnages incarnés par un joueur connecté (ou momentanément déconnecté) : branché par
     * NetworkSessionManager. Ceux-là choisissent eux-mêmes leur action.
     */
    var personnagesJoues: () -> Set<String> = { emptySet() }

    /** Comportement par défaut d'un personnage : choisi sur sa fiche, sinon celui de sa classe (grille). */
    fun comportementParDefaut(character: Character): Comportement =
        Comportements.parId(character.profilIA) ?: Comportements.pourClasse(character.characterClass)

    /**
     * Crée un combattant à partir d'une fiche de personnage (PJ ou PNJ) de GameState. Un PNJ
     * [ennemi] rejoint le camp des monstres (piloté par l'IA, PV cachés aux joueurs). Un PJ est
     * joué par son joueur : depuis l'appli s'il est connecté, sinon à la table (le joueur annonce,
     * le MJ saisit, ou le joueur passe par la page web). Un PNJ allié agit seul, selon son
     * comportement par défaut (fiche, sinon grille par classe) ; le MJ peut reprendre la main.
     */
    fun combattantDepuisPersonnage(character: Character, ennemi: Boolean = false): Combattant {
        val base = Combattant(
            id = UUID.randomUUID().toString(),
            nom = character.name,
            estMonstre = ennemi,
            characterId = character.id,
            ca = GameState.caCombat(character),
            pvMax = character.maxHitPoints,
            pv = character.currentHitPoints,
            pvTemporaires = character.temporaryHitPoints,
            // Dextérité + bonus saisi sur la fiche + don Vigilant (bonus de maîtrise).
            bonusInitiative = Dons.initiative(character),
            // États du champ condition de la fiche (Paralysé, Empoisonné…), suivis ensuite dans les deux sens.
            conditions = Etats.lire(character.condition),
            // Serment de Gloire niv. 7 — Aura d'entrain : +3 m.
            vitesse = vitesseEnMetres(character.speed) + Paladin.bonusVitesse(character),
            sauvegardes = Sauvegardes.depuisPersonnage(character),
            typeCreature = TypesCreature.normaliser(character.typeCreature) ?: "Humanoïde",
            defenses = TypesCreature.defensesPersonnage(character),
        )
        if (!ennemi && character.id in personnagesJoues()) return base.copy(joueParJoueur = true)
        if (!ennemi && character.type == "PJ") return base
        val comportement = comportementParDefaut(character)
        val raison = if (Comportements.parId(character.profilIA) != null) "Choisi par le MJ sur la fiche"
        else "Grille de comportement : ${character.characterClass.ifBlank { Comportements.CLASSE_PAR_DEFAUT }}"
        return base.avecComportement(comportement).copy(raisonProfil = raison)
    }

    /** Applique [c] : profil intégré, ou comportement créé par le MJ. */
    fun Combattant.avecComportement(c: Comportement): Combattant =
        copy(profilIA = c.profilIntegre ?: ProfilIA.BRUTE, comportementId = c.id.takeIf { c.profilIntegre == null })

    /**
     * Personnages joués par des joueurs (réservations réseau) : un nouveau venu reprend la main sur
     * son personnage — la décision que l'IA avait prise pour lui est retirée pour qu'il choisisse
     * lui-même. Une déconnexion ne le rend pas à l'IA (il retrouve sa place en revenant).
     */
    fun marquerJoues(characterIds: Set<String>) = modifier { etat ->
        var courant = etat
        etat.combattants.filter { it.characterId in characterIds && !it.joueParJoueur && !it.estMonstre }.forEach { c ->
            val joue = c.copy(joueParJoueur = true)
            val ancienne = courant.declarations[c.id]?.takeIf { it.parIa }
            courant = remplacer(courant, liberer(joue, ancienne))
            if (ancienne != null) courant = courant.copy(declarations = courant.declarations - c.id)
            courant = courant.copy(journal = courant.journal + "${c.nom} est joué par son joueur")
        }
        courant
    }

    /**
     * Qui décide pour un personnage : son joueur ([joueur]), le MJ ([comportement] null) ou l'IA
     * avec [comportement]. Reprendre la main retire la décision de l'IA en cours.
     */
    fun changerPilotage(combattantId: String, joueur: Boolean, comportement: Comportement?) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        val maj = when {
            joueur -> c.copy(joueParJoueur = true)
            comportement == null -> c.copy(joueParJoueur = false, profilIA = null, comportementId = null)
            else -> c.copy(joueParJoueur = false).avecComportement(comportement)
        }
        var courant = remplacer(etat, maj)
        val ancienne = courant.declarations[c.id]?.takeIf { it.parIa }
        if (ancienne != null && !maj.piloteParIa) {
            courant = remplacer(courant, liberer(maj, ancienne)).let { it.copy(declarations = it.declarations - c.id) }
        }
        when {
            !maj.piloteParIa || courant.phase == PhaseCombat.PREPARATION || maj.horsCombat -> courant
            else -> redecider(courant, courant.combattants.first { it.id == c.id })
        }
    }

    fun ajouter(combattants: List<Combattant>) = modifier { etat -> insererCombattants(etat, combattants) }

    private fun insererCombattants(etat: CombatEnCours, combattants: List<Combattant>): CombatEnCours {
        val dejaPresents = etat.combattants.mapNotNull { it.characterId }.toSet()
        val nouveaux = combattants.filter { it.characterId == null || it.characterId !in dejaPresents }
        if (nouveaux.isEmpty()) return etat
        // Un arrivant sans initiative en reçoit une tout de suite et est inséré à sa place dans
        // l'ordre, sans décaler le combattant actif.
        val prets = nouveaux.map { it.copy(initiative = it.initiative ?: jetInitiative(it)) }
        val actifId = etat.actif?.id
        val liste = trier(etat.combattants + prets)
        val maj = etat.copy(
            combattants = liste,
            tourIndex = actifId?.let { id -> liste.indexOfFirst { it.id == id }.coerceAtLeast(0) } ?: etat.tourIndex,
            journal = etat.journal + "Rejoint le combat : ${prets.joinToString { it.nom }}"
        )
        return if (!maj.demarre) maj
        else maj.raconter(TypeRecit.INFO, "${prets.joinToString { it.nom }} ${if (prets.size > 1) "rejoignent" else "rejoint"} le combat !", table = "${prets.joinToString { it.nom }} ${if (prets.size > 1) "rejoignent" else "rejoint"} le combat")
    }

    fun retirer(combattantId: String) = modifier { etat ->
        val index = etat.combattants.indexOfFirst { it.id == combattantId }
        if (index < 0) return@modifier etat
        val retire = etat.combattants[index]
        val liste = etat.combattants.filterIndexed { i, _ -> i != index }
        val tour = when {
            liste.isEmpty() -> 0
            index < etat.tourIndex -> etat.tourIndex - 1
            else -> etat.tourIndex.coerceAtMost(liste.size - 1)
        }
        val maj = etat.copy(
            combattants = liste,
            tourIndex = tour,
            declarations = etat.declarations - combattantId,
            journal = etat.journal + "${retire.nom} quitte le combat"
        ).let { if (it.demarre) it.raconter(TypeRecit.INFO, "${retire.nom} quitte le combat.") else it }
        // C'était son tour : le suivant dans l'ordre prend la main.
        if (index == etat.tourIndex && maj.phase == PhaseCombat.EN_COURS) deciderAuTour(maj) else maj
    }

    fun definirInitiative(combattantId: String, valeur: Int) = modifier { etat ->
        val actifId = etat.actif?.id
        val liste = trier(etat.combattants.map { if (it.id == combattantId) it.copy(initiative = valeur) else it })
        etat.copy(
            combattants = liste,
            tourIndex = actifId?.let { id -> liste.indexOfFirst { it.id == id }.coerceAtLeast(0) } ?: etat.tourIndex
        )
    }

    /**
     * Relance l'initiative (d20 + bonus) des monstres, ou de tous les combattants, pendant la
     * préparation. Les joueurs lançant souvent leurs propres dés, le MJ peut aussi saisir leur
     * résultat à la main ([definirInitiative]).
     */
    fun lancerInitiatives(seulementMonstres: Boolean) = modifier { etat ->
        etat.copy(combattants = trier(etat.combattants.map { c ->
            if (seulementMonstres && !c.estMonstre) c else c.copy(initiative = jetInitiative(c))
        }))
    }

    /**
     * Démarre le round 1 : le premier dans l'ordre d'initiative prend la main ; les combattants
     * encore sans initiative la lancent automatiquement.
     */
    fun commencer() {
        modifier { etat ->
            val avecInit = etat.combattants.map { c -> if (c.initiative != null) c else c.copy(initiative = jetInitiative(c)) }
            val liste = trier(avecInit)
            val demarre = etat.copy(
                combattants = liste,
                round = 1,
                tourIndex = 0,
                journal = etat.journal + "Round 1 — ordre : ${liste.joinToString { "${it.nom} (${it.initiative})" }}"
            )
            ouvrirRound(demarre.raconter(TypeRecit.ROUND, Narrateur.round(1), table = "Round 1"))
        }
        attaqueIaAuto()
    }

    /**
     * Enregistre (ou remplace) l'action d'un combattant, en principe pendant son tour. Une action
     * déjà résolue (dégâts appliqués) ne se remplace plus.
     */
    fun declarer(declaration: DeclarationAction) = modifier { etat ->
        if (etat.phase != PhaseCombat.EN_COURS) return@modifier etat
        val c = etat.combattants.firstOrNull { it.id == declaration.combattantId } ?: return@modifier etat
        // Neutralisé (Paralysé, Étourdi…) : l'« Aucune action » imposée reste, quoi qu'envoie le joueur.
        if (!Etats.peutAgir(c.conditions) && !c.horsCombat) return@modifier etat
        val ancienne = etat.declarations[c.id]
        if (ancienne?.degatsAppliques == true) return@modifier etat
        val maj = remplacer(etat, liberer(c, ancienne))
        val d = avecJets(maj, sansDeplacementSiImmobile(c, declaration))
        val avec = maj.copy(declarations = maj.declarations + (c.id to d))
        if (ancienne?.libelle == d.libelle && ancienne.cibleId == d.cibleId) avec
        else avec.raconter(TypeRecit.ACTION, Narrateur.action(c.nom, d), acteurId = c.id, cibleId = d.cibleId)
    }

    /** Nouvelle décision de l'IA pour un monstre ou un PNJ piloté (après un changement de situation). */
    fun redeciderMonstre(combattantId: String) = modifier { etat ->
        if (etat.phase == PhaseCombat.PREPARATION) return@modifier etat
        val monstre = etat.combattants.firstOrNull { it.id == combattantId && it.piloteParIa } ?: return@modifier etat
        redecider(etat, monstre)
    }

    /**
     * Profil de base d'un combattant. Pour un personnage (PNJ), null rend la main au MJ : sa
     * décision de l'IA en cours est retirée et il redevient « à déclarer ».
     */
    fun changerProfilIA(combattantId: String, profil: ProfilIA?) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId }?.copy(profilIA = profil, comportementId = null) ?: return@modifier etat
        val maj = remplacer(etat, c)
        when {
            !c.piloteParIa -> {
                val ancienne = maj.declarations[c.id]?.takeIf { it.parIa }
                if (ancienne == null) maj else remplacer(maj, liberer(c, ancienne)).let { it.copy(declarations = it.declarations - c.id) }
            }
            maj.phase == PhaseCombat.PREPARATION || c.horsCombat -> maj
            else -> redecider(maj, c)
        }
    }

    /** Règles de changement de profil selon la situation (combattant complexe). */
    fun definirReglesIA(combattantId: String, regles: List<RegleProfilIA>) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId }?.copy(reglesIA = regles) ?: return@modifier etat
        val maj = remplacer(etat, c)
        if (maj.phase == PhaseCombat.PREPARATION || c.horsCombat || !c.piloteParIa || maj.declarations[c.id]?.parIa == false) maj
        else redecider(maj, c)
    }

    /** Attaques utilisées par l'IA (ex. tirées des armes d'un PNJ). */
    fun definirAttaques(combattantId: String, attaques: List<AttaqueMonstre>) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId }?.copy(attaques = attaques) ?: return@modifier etat
        remplacer(etat, c)
    }

    /**
     * Le MJ choisit les actions de la fiche que ce combattant peut réaliser. Si l'IA avait déjà
     * décidé (et que rien n'est encore appliqué), elle redécide avec les seules actions autorisées.
     */
    fun definirActionsDesactivees(combattantId: String, desactivees: Set<String>) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId }?.copy(actionsDesactivees = desactivees) ?: return@modifier etat
        val maj = remplacer(etat, c)
        if (maj.phase == PhaseCombat.PREPARATION || c.horsCombat || !c.piloteParIa || maj.declarations[c.id]?.parIa != true) maj
        else redecider(maj, c)
    }

    /** Fait passer un personnage (PNJ) dans l'autre camp : allié des joueurs ou ennemi. */
    fun changerCamp(combattantId: String, ennemi: Boolean) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId && it.characterId != null } ?: return@modifier etat
        if (c.estMonstre == ennemi) return@modifier etat
        val maj = remplacer(etat, c.copy(estMonstre = ennemi))
        // Les distances n'ont de sens qu'entre camps opposés : on repart de zéro pour lui.
        val nettoye = maj.copy(
            distances = maj.distances.filterKeys { cle -> c.id !in cle.split('|') },
            journal = maj.journal + "${c.nom} passe ${if (ennemi) "dans le camp ennemi" else "du côté des joueurs"}"
        )
        // C'est son tour et l'IA avait décidé pour lui : elle redécide contre ses nouveaux adversaires.
        val ancienne = nettoye.declarations[c.id]
        if (nettoye.actif?.id == c.id && ancienne != null && ancienne.parIa && !ancienne.degatsAppliques) {
            val sans = nettoye.copy(declarations = nettoye.declarations - c.id)
            deciderAuTour(remplacer(sans, liberer(sans.combattants.first { it.id == c.id }, ancienne)))
        } else nettoye
    }

    /**
     * Le MJ remplace l'action d'un combattant (action devenue impossible, cible changée…), ou la
     * fixe pour un personnage joué à la table.
     */
    fun modifierDeclaration(declaration: DeclarationAction) = modifier { etat ->
        if (etat.phase == PhaseCombat.PREPARATION) return@modifier etat
        val c = etat.combattants.firstOrNull { it.id == declaration.combattantId } ?: return@modifier etat
        val ancienne = etat.declarations[c.id]
        val maj = remplacer(etat, liberer(c, ancienne))
        val d = avecJets(maj, sansDeplacementSiImmobile(c, declaration))
        maj.copy(
            declarations = maj.declarations + (c.id to d),
            journal = if (ancienne != null) maj.journal + "${c.nom} change d'action : ${declaration.libelle}" else maj.journal
        ).raconter(TypeRecit.ACTION, Narrateur.action(c.nom, d), acteurId = c.id, cibleId = d.cibleId)
    }

    /**
     * Le MJ place un monstre par rapport à un personnage (au contact, courte, longue). Un monstre
     * dont l'IA a déjà décidé (et rien appliqué) revoit sa décision.
     */
    fun definirDistance(aId: String, bId: String, distance: Distance) = modifier { etat ->
        // Zones : on déplace le monstre (sinon [bId]) dans la zone qui donne cette distance.
        val a = etat.combattants.firstOrNull { it.id == aId } ?: return@modifier etat
        val b = etat.combattants.firstOrNull { it.id == bId } ?: return@modifier etat
        val (bouge, repere) = if (b.estMonstre && !a.estMonstre) b to a else a to b
        placerDans(etat, bouge.id, Terrain.zonePour(bouge, repere, distance))
    }

    /** Place un combattant à la distance voulue de ses adversaires (de la zone du premier). */
    fun definirDistancesMonstre(monstreId: String, distance: Distance) = modifier { etat ->
        val soi = etat.combattants.firstOrNull { it.id == monstreId } ?: return@modifier etat
        val repere = etat.combattants.firstOrNull { it.estMonstre != soi.estMonstre && !it.horsCombat } ?: return@modifier etat
        placerDans(etat, monstreId, Terrain.zonePour(soi, repere, distance))
    }

    /**
     * Le MJ place [combattantId] dans [zone] (terrain du combat), sans attaque d'opportunité ni
     * récit : c'est un ajustement de la scène, pas une action. L'IA qui avait déjà décidé revoit
     * sa décision.
     */
    fun placer(combattantId: String, zone: Int) = modifier { etat -> placerDans(etat, combattantId, zone) }

    private fun placerDans(etat: CombatEnCours, combattantId: String, zone: Int): CombatEnCours {
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return etat
        val z = zone.coerceIn(0, Terrain.derniere)
        if (Terrain.zoneDe(c) == z) return etat
        val place = remplacer(etat, c.copy(zone = z))
        // Tout monstre peut revoir son choix : sa cible a pu se mettre hors de portée.
        return redeciderApresPlacement(place, etat.combattants.filter { it.piloteParIa }.map { it.id }.toSet() + c.id)
    }

    /**
     * L'action du combattant s'avère impossible (coincé, cible hors d'atteinte…) : elle est
     * annulée, son déplacement aussi.
     */
    fun annulerAction(combattantId: String, raison: String? = null) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        val ancienne = etat.declarations[combattantId]
        val maj = remplacer(etat, liberer(c, ancienne))
        val libelle = "Action impossible" + (ancienne?.let { " (${it.libelle})" } ?: "")
        maj.copy(
            declarations = maj.declarations + (combattantId to DeclarationAction(combattantId, libelle, detail = raison, parMj = true)),
            journal = maj.journal + ("${c.nom} : $libelle" + (raison?.let { " — $it" } ?: ""))
        ).raconter(TypeRecit.INFO, "${c.nom} ne parvient pas à agir" + (raison?.let { " : $it" } ?: "") + ".", acteurId = c.id)
    }

    /** Relance le toucher et les dégâts déjà tirés d'un combattant (nouvelle cible, contestation…). */
    fun relancerJets(combattantId: String, cibleId: String? = null) = modifier { etat ->
        val d = etat.declarations[combattantId] ?: return@modifier etat
        val cible = cibleId?.let { id -> etat.combattants.firstOrNull { it.id == id } }
        val nouvelle = d.copy(resultats = null, cibleId = cible?.id ?: d.cibleId, cibleNom = cible?.nom ?: d.cibleNom)
        etat.copy(declarations = etat.declarations + (combattantId to avecJets(etat, nouvelle)))
    }

    /** Remplace la cible et les jets d'une déclaration (choisis dans la fenêtre de résolution). */
    fun remplacerJets(combattantId: String, cibleId: String, resultats: List<ResultatAttaque>) = modifier { etat ->
        val d = etat.declarations[combattantId] ?: return@modifier etat
        val cible = etat.combattants.firstOrNull { it.id == cibleId } ?: return@modifier etat
        etat.copy(declarations = etat.declarations + (combattantId to d.copy(cibleId = cible.id, cibleNom = cible.nom, resultats = resultats, degatsAppliques = false)))
    }

    /**
     * Applique à la cible les dégâts déjà lancés (attaques qui touchent) d'un combattant, une
     * seule fois, et note le détail au journal. [montant] : dégâts choisis par le MJ à la place.
     */
    fun appliquerJets(combattantId: String, montant: Int? = null) {
        val etat = _etat.value ?: return
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return
        val d = etat.declarations[combattantId] ?: return
        val resultats = d.resultats ?: return
        if (d.degatsAppliques) return
        val cible = etat.combattants.firstOrNull { it.id == d.cibleId } ?: return
        val lances = resultats.sumOf { it.degats }
        val total = montant?.coerceAtLeast(0) ?: lances
        if (montant != null && montant != lances) noter("${c.nom} — ${d.libelle} : dégâts ajustés par le MJ ($lances → $total)")
        // Il se déplace d'abord (il fonce au contact…), puis frappe : le récit suit cet ordre.
        modifier { e ->
            val deplace = appliquerDeplacements(e, combattantId)
            var courant = deplace.copy(
                declarations = deplace.declarations + (combattantId to d.copy(degatsAppliques = true, deplacements = emptyMap())),
                journal = deplace.journal + ("${c.nom} — ${d.libelle} sur ${cible.nom} : " +
                    resultats.joinToString { r ->
                        val mode = if (r.mode != ModeJet.NORMAL) " avec ${r.mode.label}" else ""
                        if (r.touche) "${r.totalToucher ?: "auto"}$mode touché${if (r.critique) " CRITIQUE" else ""} (${r.degats})" else "${r.totalToucher}$mode raté"
                    })
            )
            resultats.forEach { res ->
                val (type, texte) = Narrateur.attaque(
                    c.nom, cible.nom, d.attaque?.nom, res.touche, res.critique, res.d20 == 1,
                    Narrateur.aDistance(d.attaque?.type), aleatoire,
                )
                courant = courant.raconter(
                    type, texte, acteurId = c.id, cibleId = cible.id,
                    table = Narrateur.tableAttaque(c.nom, cible.nom, res.touche, res.critique, res.degats),
                )
            }
            courant
        }
        // Les dégâts de chaque attaque sont déjà dans les lignes de la table.
        if (total > 0) modifier { e -> pv(e, cible.id, total, c.id, d.attaque?.typeDegats, tableDite = true) }
        // Attaque entièrement ratée : le repaire d'un boss peut s'en prendre à l'attaquant.
        if (resultats.isNotEmpty() && resultats.none { it.touche }) modifier { e -> repaireSurRate(e, c.id) }
        if (resultats.any { it.touche }) bonusContreType(c.id, cible.id, d.attaque?.nom, d.attaque?.typeDegats)
        // États de l'attaque (« subit l'état Agrippé ») : d'office si elle a touché ; ceux qui
        // demandent un JS (« JS Constitution DD 10 ou Paralysé ») : l'application lance le JS de la cible.
        val attaque = d.attaque ?: return
        if (resultats.none { it.touche } || cible.horsCombat) return
        attaque.conditions.filter { !it.surEchecJs }.forEach { inflige ->
            appliquerCondition(cible.id, inflige.condition, "${c.nom} — ${attaque.nom}" + (inflige.evasionDd?.let { " (évasion DD $it)" } ?: ""))
        }
        attaque.conditions.filter { it.surEchecJs }.forEach { inflige ->
            modifier { e ->
                if (e.combattants.firstOrNull { it.id == cible.id }?.horsCombat != false) e
                else subirAvecSauvegarde(e, cible.id, inflige.sauvegarde, inflige.dd ?: return@modifier e, 0, false, listOf(inflige.condition), "${c.nom} — ${attaque.nom}")
            }
        }
    }

    /** Le MJ décide de ne pas appliquer l'attaque déclarée de [combattantId] (dégâts et états). */
    fun ignorerAttaque(combattantId: String) = modifier { e ->
        val c = e.combattants.firstOrNull { it.id == combattantId } ?: return@modifier e
        val d = e.declarations[combattantId]?.takeIf { it.attaque != null && !it.degatsAppliques } ?: return@modifier e
        e.copy(
            declarations = e.declarations + (combattantId to d.copy(degatsAppliques = true, detail = "Non appliqué par le MJ")),
            journal = e.journal + "${c.nom} — ${d.libelle} : non appliqué par le MJ",
        )
    }

    /** Ajoute un état à un combattant (attaque de monstre, bouton du MJ), noté au journal. */
    fun appliquerCondition(combattantId: String, condition: ConditionCombat, source: String? = null) = modifier { etat ->
        ajouterCondition(etat, combattantId, condition, source)
    }

    /**
     * Mode du jet d'attaque de [attaquantId] contre [cibleId] d'après leurs états et leur distance
     * (après le déplacement déclaré), raisons et critique automatique au contact.
     */
    fun modeAttaque(etat: CombatEnCours, attaquantId: String, cibleId: String?, attaque: AttaqueMonstre?): ModeAttaque {
        val attaquant = etat.combattants.firstOrNull { it.id == attaquantId }
        val cible = etat.combattants.firstOrNull { it.id == cibleId }
        if (attaquant == null || cible == null || attaque == null || attaque.zone || attaque.bonusToucher == null) return ModeAttaque()
        val distance = etat.declarations[attaquantId]?.deplacements?.get(cible.id) ?: etat.distance(attaquantId, cible.id)
        val auContact = distance == Distance.CONTACT
        val (mode, raisons) = Etats.modeAttaque(attaquant.conditions, cible.conditions, auContact)
        // Roublard niv. 18 — Insaisissable : aucune attaque n'a l'Avantage contre lui (sauf Neutralisé).
        if (mode == ModeJet.AVANTAGE && Etats.peutAgir(cible.conditions) && fiche(cible)?.let { Roublard.insaisissable(it) } == true) {
            return ModeAttaque(ModeJet.NORMAL, raisons + "Insaisissable : pas d'Avantage contre ${cible.nom}", Etats.critiqueAuContact(cible.conditions, auContact))
        }
        return ModeAttaque(mode, raisons, Etats.critiqueAuContact(cible.conditions, auContact))
    }

    /** Enregistre un jet de dés envoyé par un joueur pour son combattant. */
    fun enregistrerJet(jet: JetCombat) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == jet.combattantId } ?: return@modifier etat
        // Un seul jet par type (Attaque, Dégâts…) et par round — autant de jets « Attaque » que
        // d'attaques déclarées (Attaque supplémentaire) — sauf jet supplémentaire accordé par le
        // MJ : empêche de relancer jusqu'à obtenir un bon résultat.
        val autorises = when (jet.libelle) {
            "Attaque" -> etat.declarations[c.id]?.attaquesJoueur?.size?.coerceAtLeast(1) ?: 1
            // Fougue : une action Attaquer de plus, avec toutes ses attaques (Attaque supplémentaire).
            "Attaque (Fougue)" -> fiche(c)?.let { ArsenalPersonnage.nbAttaques(it) } ?: 1
            else -> 1
        }
        val dejaLance = etat.jetsDuRound.count { it.combattantId == c.id && it.libelle == jet.libelle } >= autorises
        val relances = etat.relances[c.id] ?: 0
        if (dejaLance && relances <= 0) return@modifier etat
        val detail = if (jet.manuel) "dés physiques" else "dés de l'appli"
        val enregistre = etat.copy(
            jets = (etat.jets + jet.copy(round = etat.round)).takeLast(200),
            relances = if (dejaLance) etat.relances + (c.id to relances - 1) else etat.relances,
            journal = etat.journal + ("${c.nom} — ${jet.libelle} : ${jet.total} (${jet.formule}, $detail)" +
                (jet.totalDegats?.let { " · dégâts $it (${jet.formuleDegats})" } ?: ""))
        )
        // Jet d'attaque d'un joueur : raconté d'après la CA de la cible (le MJ tranche ensuite).
        val cible = (jet.cibleId ?: etat.declarations[c.id]?.cibleId)?.let { id -> etat.combattants.firstOrNull { it.id == id } }
        if (!jet.libelle.startsWith("Attaque") || cible == null || jet.des.isEmpty()) return@modifier enregistre
        val d20 = jet.des.singleOrNull()?.takeIf { jet.formule.startsWith("1d20") }
        val critique = d20 == 20 || jet.formuleDegats?.contains("(critique)") == true
        val touche = critique || (d20 != 1 && jet.total >= cible.ca)
        val (type, texte) = Narrateur.attaque(
            c.nom, cible.nom, jet.arme, touche = touche, critique = critique,
            maladresse = d20 == 1, aDistance = false, aleatoire = aleatoire,
        )
        // Table : un raté se dit tout de suite ; un coup qui porte, quand le MJ applique ses dégâts.
        val raconte = enregistre.raconter(type, texte, acteurId = c.id, cibleId = cible.id, table = if (touche) null else Narrateur.tableAttaque(c.nom, cible.nom, false, false, null))
        // Raté : le repaire d'un boss peut s'en prendre à l'attaquant (s'il n'a pas été blessé ce round).
        if (touche) raconte else repaireSurRate(raconte, c.id)
    }

    /** Le MJ accorde un jet supplémentaire ce round (attaque supplémentaire, nouveau JS…). */
    fun autoriserRelance(combattantId: String) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        etat.copy(
            relances = etat.relances + (c.id to (etat.relances[c.id] ?: 0) + 1),
            journal = etat.journal + "${c.nom} peut faire un jet de plus ce round"
        )
    }

    /**
     * Applique à [cibleId] un jet de dégâts (ou de soins, [soin]) envoyé par un joueur, une seule
     * fois. Les PV d'un personnage sont reportés sur sa fiche.
     */
    fun appliquerJet(jetId: String, cibleId: String, soin: Boolean) {
        val etat = _etat.value ?: return
        val jet = etat.jets.firstOrNull { it.id == jetId }?.takeIf { !it.applique } ?: return
        val auteur = etat.combattants.firstOrNull { it.id == jet.combattantId }?.nom ?: "?"
        val cible = etat.combattants.firstOrNull { it.id == cibleId } ?: return
        val montant = jet.totalDegats ?: jet.total
        modifier { e -> appliquerDeplacements(e, jet.combattantId).let { d -> d.copy(jets = d.jets.map { if (it.id == jetId) it.copy(applique = true) else it }) } }
        // Sort à jet de sauvegarde (« JS Dextérité DD 15 ») : la cible fait son JS, lancé par l'application.
        val js = if (soin) null else Sauvegardes.duResume(etat.declarations[jet.combattantId]?.detail)
        if (js != null) {
            modifier { e -> subirAvecSauvegarde(e, cible.id, js.caracteristique, js.dd, montant, js.moitieSiReussi, emptyList(), "$auteur — ${jet.libelle}", auteurId = jet.combattantId, typeDegats = jet.typeDegats) }
            return
        }
        // Table : « Aldric attaque Gobelin : réussie, 8 dégâts » (dégâts réellement subis).
        val attaque = jet.libelle.startsWith("Attaque")
        modifier { e ->
            pv(
                e, cible.id, if (soin) -montant else montant, jet.combattantId, jet.typeDegats.takeIf { !soin },
                tableDegats = { n -> if (attaque) Narrateur.tableAttaque(auteur, cible.nom, true, false, n) else "$auteur → ${cible.nom} : $n dégât${if (n > 1) "s" else ""}" },
            )
        }
        noter("$auteur → ${cible.nom} : $montant ${if (soin) "PV soignés" else "dégâts"} (${jet.libelle})")
        if (!soin) bonusContreType(jet.combattantId, cible.id, jet.arme, jet.typeDegats)
    }

    /**
     * Personnage joué à la table (joueur sans l'appli) : tout se passe sur la table, le MJ note
     * seulement le résultat — qui est touché et combien de dégâts, raté, ou un soin. Rien n'est
     * lancé par l'application. Plusieurs attaques dans le même tour : un appel par attaque.
     */
    fun resultatTable(acteurId: String, cibleId: String, resultat: ResultatTable, montant: Int = 0) {
        val etat = _etat.value ?: return
        val a = etat.combattants.firstOrNull { it.id == acteurId } ?: return
        val c = etat.combattants.firstOrNull { it.id == cibleId } ?: return
        val libelle = when (resultat) {
            ResultatTable.TOUCHE -> "Attaque réussie ($montant dégâts)"
            ResultatTable.RATE -> "Attaque ratée"
            ResultatTable.SOIN -> "Soin (+$montant PV)"
        }
        modifier { e ->
            val note = e.copy(
                declarations = e.declarations + (a.id to DeclarationAction(a.id, libelle, cibleId = c.id, cibleNom = c.nom, parMj = true, degatsAppliques = true)),
                journal = e.journal + "${a.nom} → ${c.nom} : $libelle (annoncé à la table)",
            )
            when (resultat) {
                ResultatTable.SOIN -> note
                else -> {
                    val (type, texte) = Narrateur.attaque(a.nom, c.nom, null, resultat == ResultatTable.TOUCHE, false, false, false, aleatoire)
                    note.raconter(
                        type, texte, acteurId = a.id, cibleId = c.id,
                        table = if (resultat == ResultatTable.RATE) Narrateur.tableAttaque(a.nom, c.nom, false, false, null) else null,
                    )
                }
            }
        }
        when (resultat) {
            ResultatTable.TOUCHE -> if (montant > 0) modifier { e ->
                pv(e, c.id, montant, a.id, tableDegats = { n -> Narrateur.tableAttaque(a.nom, c.nom, true, false, n) })
            }
            ResultatTable.SOIN -> if (montant > 0) modifier { e -> pv(e, c.id, -montant, a.id) }
            // Raté annoncé à la table : le repaire d'un boss peut s'en prendre à l'attaquant.
            ResultatTable.RATE -> modifier { e -> repaireSurRate(e, a.id) }
        }
    }

    /**
     * Arme efficace contre le type de [cibleId] (Tueuse de dragons contre un Dragon, Masse
     * d'anéantissement contre un Fiélon ou un Mort-vivant…) : ses dés supplémentaires sont lancés
     * et appliqués, avec leur propre type de dégâts.
     */
    private fun bonusContreType(auteurId: String, cibleId: String, arme: String?, typeArme: String?) {
        val cible = _etat.value?.combattants?.firstOrNull { it.id == cibleId } ?: return
        val type = cible.typeCreature ?: return
        val description = arme?.let { com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository.findEquipmentItemCached(it)?.takeIf { i -> arme.contains(i.name, ignoreCase = true) }?.rawMarkdown }
        val bonus = TypesCreature.bonusArme(arme, description)?.takeIf { type in it.types } ?: return
        val degats = lancerFormuleDes(bonus.formule, aleatoire) ?: return
        noter("${bonus.source} contre un $type : +$degats dégâts (${bonus.formule})")
        modifier { e -> pv(e, cibleId, degats, auteurId, bonus.typeDegats ?: typeArme) }
    }

    /** Jet de joueur entièrement traité par le MJ (dégâts de zone appliqués cible par cible). */
    fun marquerJetTraite(jetId: String) = modifier { e ->
        e.copy(jets = e.jets.map { if (it.id == jetId) it.copy(applique = true) else it })
    }

    /** Le MJ écarte les dégâts d'un jet de joueur (attaque ratée, action refusée…). */
    /**
     * Imposition des mains d'un joueur (« retire Empoisonné, Paralysé de Nom ») : les états
     * reconnus dans l'annonce sont retirés de la cible du jet.
     */
    fun appliquerRetraitEtats(jetId: String) = modifier { e ->
        val jet = e.jets.firstOrNull { it.id == jetId && !it.applique } ?: return@modifier e
        val cible = e.combattants.firstOrNull { it.id == jet.cibleId } ?: return@modifier e
        val auteur = e.combattants.firstOrNull { it.id == jet.combattantId }?.nom ?: "?"
        val retires = Etats.lire(jet.formule.substringAfter("retire ").substringBefore(" de ")).intersect(cible.conditions)
        val maj = changerConditions(e, cible, cible.conditions - retires)
        maj.copy(
            jets = maj.jets.map { if (it.id == jetId) it.copy(applique = true) else it },
            journal = maj.journal + ("$auteur → ${cible.nom} : " +
                (if (retires.isEmpty()) "aucun état à retirer" else retires.joinToString { it.label } + " retiré(s)") + " (${jet.libelle})"),
        )
    }

    fun ignorerJet(jetId: String) = modifier { e ->
        val jet = e.jets.firstOrNull { it.id == jetId && !it.applique } ?: return@modifier e
        val auteur = e.combattants.firstOrNull { it.id == jet.combattantId }?.nom ?: "?"
        e.copy(
            jets = e.jets.map { if (it.id == jetId) it.copy(applique = true) else it },
            journal = e.journal + "$auteur — ${jet.libelle} : résultat non appliqué par le MJ"
        )
    }

    /** Note au journal une ligne de résolution (attaque de monstre lancée, action impossible…). */
    fun noter(ligne: String) = modifier { it.copy(journal = it.journal + ligne) }

    /**
     * Ce qui doit être réglé avant de passer au tour suivant (annonce du boss ou action de repaire
     * à lire, jet contre la mort à noter), null si rien ne bloque.
     */
    fun blocageTourSuivant(etat: CombatEnCours): String? = when {
        etat.boss.values.any { b -> b.annonces.any { it.aLire && !it.lue } } -> "Lisez d'abord l'annonce, puis « C'est lu »"
        etat.actif?.let { jetContreLaMortEnAttente(etat, it) } == true -> "Notez d'abord le jet contre la mort"
        else -> null
    }

    /**
     * Fin du tour du combattant actif, au suivant (les monstres à 0 PV sont sautés). Après le
     * dernier, un nouveau round commence.
     */
    fun tourSuivant() {
        modifier { avant ->
            if (avant.phase != PhaseCombat.EN_COURS || avant.combattants.isEmpty() || blocageTourSuivant(avant) != null) return@modifier avant
            // Le combattant qui vient d'agir a effectué son déplacement ; un boss peut alors réagir
            // (action légendaire après le tour d'un adversaire).
            val deplace = avant.actif?.let { appliquerDeplacements(avant, it.id) } ?: avant
            val etat = avant.actif?.let { fini -> legendairesApresTour(deplace, fini.id) } ?: deplace
            var index = etat.tourIndex + 1
            while (index < etat.combattants.size) {
                if (peutAgir(etat.combattants[index])) return@modifier debutDeTour(etat.copy(tourIndex = index))
                index++
            }
            nouveauRound(etat)
        }
        attaqueIaAuto()
    }

    /**
     * Réglage « attaques des monstres automatiques » : l'attaque que l'IA vient de décider pour le
     * combattant actif (jets déjà lancés contre une cible) est appliquée sans attendre le MJ. Les
     * attaques de zone et à sauvegarde restent à résoudre à la main.
     */
    private fun attaqueIaAuto() {
        if (!ReglagesCombatStore.courant.attaquesMonstresAuto) return
        val etat = _etat.value ?: return
        val actif = etat.actif ?: return
        val d = etat.declarations[actif.id] ?: return
        if (!d.parIa || d.resultats.isNullOrEmpty() || d.degatsAppliques || d.cibleId == null) return
        appliquerJets(actif.id)
    }

    fun tourPrecedent() = modifier { etat ->
        if (etat.phase != PhaseCombat.EN_COURS) return@modifier etat
        var index = etat.tourIndex - 1
        while (index >= 0) {
            if (peutAgir(etat.combattants[index])) return@modifier etat.copy(tourIndex = index)
            index--
        }
        etat
    }

    /**
     * Applique des dégâts (montant positif) ou des soins (montant négatif). Les dégâts entament
     * d'abord les PV temporaires. Pour un personnage, les PV sont reportés sur sa fiche.
     */
    fun appliquerPv(combattantId: String, degatsBruts: Int) = modifier { etat -> pv(etat, combattantId, degatsBruts) }

    /**
     * Dégâts ou soins appliqués à [combattantId] (voir [appliquerPv]). Un personnage déjà à 0 PV
     * qui subit des dégâts rate un jet contre la mort (trois échecs : il meurt) ; soigné, il
     * reprend conscience et ses jets contre la mort repartent de zéro. Un mort ne se soigne plus.
     */
    private fun pv(etat: CombatEnCours, combattantId: String, degatsBruts: Int, auteurId: String? = null, typeDegats: String? = null,
        // Version table de la ligne de dégâts : déjà dite par l'appelant ([tableDite]), ou formulée par lui.
        tableDite: Boolean = false,
        tableDegats: ((Int) -> String)? = null,
    ): CombatEnCours {
        val lignes = mutableListOf<String>()
        // Récit de ce qui arrive à la cible : (type, texte, montant de la bulle animée).
        val recits = mutableListOf<LigneRecit>()
        // Grand Ancien niveau 10 — Bouclier mental : dégâts psychiques subis renvoyés à l'assaillant.
        var renvoiPsychique = 0
        // Dégâts réellement subis par un boss : jauge de déséquilibre et récapitulatif.
        var subisBoss = 0
        val liste = etat.combattants.map { c ->
            if (c.id != combattantId) return@map c
            if (c.mort) {
                lignes += "${c.nom} est mort : rien n'y fait"
                return@map c
            }
            // Pétrifié : résistance à tous les dégâts (moitié, arrondie à l'inférieur) ; Résistance
            // fiélonne de l'Occultiste au type de dégâts choisi au dernier repos.
            val fielonne = degatsBruts > 0 && fiche(c)?.let { Occultiste.resiste(it, typeDegats) } == true
            // Défenses de la fiche (bestiaire ou personnage) au type de dégâts, quand il est connu.
            val defense = if (degatsBruts > 0) c.defenses.effet(typeDegats) else EffetDefense.AUCUN
            // Serment des Anciens — Aura de garde d'un Paladin allié à portée (nécrotiques, psychiques, radiants).
            val gardien = if (degatsBruts > 0 && defense == EffetDefense.AUCUN && typeDegats != null) {
                paladinsAuraAllies(etat, c).firstOrNull { (_, f) ->
                    Paladin.resistancesAura(f).any { TypesCreature.radical(it) == TypesCreature.radical(typeDegats) }
                }?.first
            } else null
            val resistant = degatsBruts > 0 && (Etats.resistanceTousDegats(c.conditions) || fielonne || defense == EffetDefense.RESISTANCE || gardien != null)
            // Maître des armures lourdes : dégâts contondants, perforants et tranchants réduits du bonus de maîtrise.
            val reductionArmure = if (degatsBruts > 0) fiche(c)?.let { Dons.reductionDegats(it, typeDegats) } ?: 0 else 0
            val degats = when {
                defense == EffetDefense.IMMUNITE -> 0
                defense == EffetDefense.VULNERABILITE && !resistant -> degatsBruts * 2
                resistant -> degatsBruts / 2
                else -> degatsBruts
            }.let { if (reductionArmure > 0) (it - reductionArmure).coerceAtLeast(0) else it }
            if (reductionArmure > 0) lignes += "🛡️ ${c.nom} — Maître des armures lourdes : dégâts réduits de $reductionArmure"
            if (degats > 0 && c.id in etat.boss && !c.horsCombat) subisBoss = minOf(degats, c.pv + c.pvTemporaires)
            if (defense == EffetDefense.IMMUNITE) lignes += "${c.nom} est immunisé contre les dégâts ${typeDegats.orEmpty()}"
            else if (defense == EffetDefense.VULNERABILITE && !resistant) lignes += "${c.nom} est vulnérable aux dégâts ${typeDegats.orEmpty()} : $degatsBruts × 2"
            var maj = if (degats >= 0) {
                val absorbe = minOf(c.pvTemporaires, degats)
                c.copy(pvTemporaires = c.pvTemporaires - absorbe, pv = (c.pv - (degats - absorbe)).coerceAtLeast(0))
            } else {
                c.copy(pv = (c.pv - degats).coerceAtMost(c.pvMax), succesMort = 0, echecsMort = 0, stable = false)
            }
            // Serment des Anciens niv. 15 — Sentinelle impérissable (1/repos long) : réduit à 0 PV,
            // le Paladin reste à 1 PV et regagne 3 × son niveau de Paladin.
            val ficheSentinelle = if (degats > 0 && c.pv > 0 && maj.pv == 0) fiche(c)?.takeIf { Paladin.sentinelleDisponible(it) } else null
            if (ficheSentinelle != null) {
                GameState.utiliserSentinelle(ficheSentinelle.id)
                maj = maj.copy(pv = (1 + Paladin.pvSentinelle(ficheSentinelle)).coerceAtMost(maj.pvMax))
            }
            // Faveur de récupération — Baroud d'honneur (1/repos long) : à 0 PV, 1 PV + la moitié des PV max.
            val ficheBaroud = if (ficheSentinelle == null && degats > 0 && c.pv > 0 && maj.pv == 0) fiche(c)?.takeIf { Dons.baroudDisponible(it) } else null
            if (ficheBaroud != null) {
                GameState.utiliserCompteur(ficheBaroud.id, Dons.BAROUD, 1)
                maj = maj.copy(pv = Dons.pvBaroud(ficheBaroud).coerceAtMost(maj.pvMax))
                lignes += "🔥 ${c.nom} — Baroud d'honneur : tombé à 0 PV, se relève avec ${maj.pv} PV"
            }
            val raisonResistance = when {
                fielonne -> "Résistance fiélonne"
                gardien != null -> "Aura de garde de ${gardien.nom}"
                else -> "résistance"
            }
            lignes += if (degats >= 0) {
                "${c.nom} subit $degats dégât(s)" + (if (resistant) " ($raisonResistance : $degatsBruts ÷ 2)" else "") +
                    " → ${maj.pv}/${maj.pvMax}" + if (maj.horsCombat) " — hors de combat" else ""
            } else {
                "${c.nom} récupère ${-degats} PV → ${maj.pv}/${maj.pvMax}"
            }
            when {
                defense == EffetDefense.IMMUNITE ->
                    recits += LigneRecit(TypeRecit.INFO, "${c.nom} ne sent rien : insensible aux dégâts${typeDegats?.let { " $it" }.orEmpty()}.", null, "${c.nom} : aucun dégât (immunisé)")
                degats > 0 -> recits += LigneRecit(
                    TypeRecit.DEGATS,
                    Narrateur.encaisse(maj, degats, typeDegats, when {
                        resistant -> "(résistance)"
                        defense == EffetDefense.VULNERABILITE -> "(point faible !)"
                        else -> null
                    }),
                    degats,
                    if (tableDite) null else tableDegats?.invoke(degats) ?: Narrateur.tableDegats(c.nom, degats),
                )
                degats < 0 -> recits += LigneRecit(TypeRecit.SOIN, Narrateur.soin(maj, -degats, releve = c.pv <= 0 && maj.pv > 0), -degats, Narrateur.tableSoin(c.nom, -degats))
            }
            if (ficheSentinelle != null) {
                lignes += "🌿 ${c.nom} — Sentinelle impérissable : tombé à 0 PV, reste debout avec ${maj.pv} PV (1 + ${Paladin.pvSentinelle(ficheSentinelle)})"
            }
            if (ficheSentinelle != null || ficheBaroud != null) recits += LigneRecit(TypeRecit.INFO, "${c.nom} refuse de tomber et reste debout !", null, "${c.nom} reste debout")
            if (degats > 0 && c.pv > 0 && maj.pv == 0) recits += LigneRecit(TypeRecit.KO, Narrateur.chute(maj, aleatoire), null, Narrateur.tableChute(maj))
            // Personnage frappé alors qu'il est déjà à terre : un échec au jet contre la mort.
            if (!c.estMonstre && c.horsCombat && degats > 0) {
                maj = maj.copy(echecsMort = c.echecsMort + 1, stable = false)
                lignes += "${c.nom} : échec au jet contre la mort (frappé à terre) — ${maj.succesMort} réussite(s), ${maj.echecsMort} échec(s)"
                recits += LigneRecit(TypeRecit.INFO, "${c.nom}, à terre, est frappé : la mort se rapproche (${maj.echecsMort}/3 échecs).", null, "${c.nom} : un échec contre la mort (${maj.echecsMort}/3)")
                if (maj.echecsMort >= 3) {
                    maj = maj.copy(mort = true)
                    lignes += "💀 ${c.nom} meurt"
                    recits += LigneRecit(TypeRecit.MORT, Narrateur.mort(c.nom), null, "${c.nom} est mort")
                }
            }
            maj.characterId?.let { GameState.updateCharacterHp(it, maj.pv) }
            if (degats > 0 && auteurId != null && auteurId != c.id && TypesCreature.radical(typeDegats.orEmpty()) == "psychique" &&
                fiche(c)?.let { Occultiste.bouclierMental(it) } == true
            ) renvoiPsychique = degats
            // Concentration : dégâts subis → JS de Constitution (DD 10 ou la moitié des dégâts, 30 au
            // plus) ; raté, ou tombé à 0 PV, le sort maintenu prend fin.
            if (degats > 0 && concentre(maj)) {
                val raison = if (maj.horsCombat) "tombe à 0 PV" else {
                    val dd = maxOf(10, degats / 2).coerceAtMost(30)
                    // Don Tueur de mages de l'assaillant : Désavantage au JS de Concentration.
                    val tueurDeMages = auteurId?.let { id -> etat.combattants.firstOrNull { it.id == id } }?.let { fiche(it) }?.let { Dons.a(it, "Tueur de mages") } == true
                    val avantage = fiche(maj)?.let { Occultiste.avantageConcentration(it) } == true
                    val (majAura, mentionAura) = avecAuraProtection(etat, maj)
                    val js = Sauvegardes.lancer(majAura, "Constitution", dd, aleatoire, avantage = avantage, desavantageImpose = tueurDeMages)
                    lignes += "🎲 ${maj.nom} — concentration : ${js.texte}$mentionAura" + (if (avantage) " (Avantage : ${fiche(maj)?.let { Occultiste.raisonAvantageConcentration(it) }})" else "") +
                        (if (tueurDeMages) " (Désavantage : Tueur de mages)" else "")
                    if (js.reussi) null else "JS de concentration raté"
                }
                if (raison != null) {
                    maj = maj.copy(conditions = maj.conditions - ConditionCombat.CONCENTRATION)
                    rompreConcentrationFiche(maj)?.let { sorts -> lignes += "${maj.nom} perd sa concentration ($raison)" + if (sorts.isNotEmpty()) " : fin de $sorts" else "" }
                    recits += LigneRecit(TypeRecit.INFO, "${maj.nom} perd sa concentration : son sort se dissipe.", null)
                }
            }
            maj
        }
        // Occultiste Fiélon — Bénédiction du ténébreux : un adversaire tombe à 0 PV, abattu par
        // l'occultiste ou à 3 m de lui → PV temporaires (Charisme + niveau d'Occultiste).
        val avant = etat.combattants.firstOrNull { it.id == combattantId }
        val tombe = liste.firstOrNull { it.id == combattantId }
        var combattants = liste
        if (avant != null && tombe != null && !avant.horsCombat && tombe.horsCombat) {
            combattants = liste.map { o ->
                val pvBenediction = fiche(o)?.let { Occultiste.pvBenediction(it) } ?: 0
                val proche = o.id == auteurId || etat.distance(o.id, tombe.id).metres <= Distance.M3.metres + 0.01
                if (pvBenediction <= 0 || o.horsCombat || o.estMonstre == tombe.estMonstre || !proche || o.pvTemporaires >= pvBenediction) o
                else {
                    lignes += "${o.nom} — Bénédiction du ténébreux : $pvBenediction PV temporaires (${tombe.nom} est tombé)"
                    o.copy(pvTemporaires = pvBenediction)
                }
            }
        }
        var resultat = recits.fold(etat.copy(combattants = combattants, journal = etat.journal + lignes)) { e, l ->
            e.raconter(l.type, l.texte, acteurId = auteurId, cibleId = combattantId, montant = l.montant, table = l.table)
        }
        if (subisBoss > 0) {
            val auteurNom = auteurId?.let { id -> etat.combattants.firstOrNull { it.id == id }?.nom }
            resultat = majBoss(resultat, combattantId) { b ->
                b.copy(
                    jauge = b.jauge + subisBoss,
                    degatsParAuteur = auteurNom?.let { b.degatsParAuteur + (it to (b.degatsParAuteur[it] ?: 0) + subisBoss) } ?: b.degatsParAuteur,
                    coupFinal = if (tombe?.horsCombat == true && avant?.horsCombat == false) auteurNom ?: b.coupFinal else b.coupFinal,
                )
            }
        }
        // Adversaire d'un boss blessé ce round : son attaque ratée ne réveillera pas le repaire.
        if (avant != null && tombe != null && tombe.pv + tombe.pvTemporaires < avant.pv + avant.pvTemporaires) {
            resultat.boss.forEach { (bossId, _) ->
                val boss = resultat.combattants.firstOrNull { it.id == bossId } ?: return@forEach
                if (boss.estMonstre != tombe.estMonstre) resultat = majBoss(resultat, bossId) { it.copy(blessesDuRound = it.blessesDuRound + tombe.id) }
            }
        }
        if (renvoiPsychique <= 0 || auteurId == null) return resultat
        val renvoye = resultat.copy(journal = resultat.journal + "Bouclier mental : ${tombe?.nom ?: "?"} renvoie $renvoiPsychique dégâts psychiques à son assaillant")
        return pv(renvoye, auteurId, renvoiPsychique, combattantId, null)
    }

    /** Le combattant maintient-il un sort de concentration (état Concentration, ou effet de concentration sur sa fiche) ? */
    fun concentre(c: Combattant): Boolean =
        ConditionCombat.CONCENTRATION in c.conditions || fiche(c)?.effetsActifs?.any { it.concentration } == true

    /**
     * Fin de la concentration de [c] : ses effets de concentration quittent sa fiche (sort lancé
     * depuis la fiche ou en combat). Retourne leurs noms (vide sans effet suivi), null si rien à rompre.
     */
    private fun rompreConcentrationFiche(c: Combattant): String? {
        val f = fiche(c)
        val effets = f?.effetsActifs?.filter { it.concentration }.orEmpty()
        effets.forEach { GameState.retirerEffet(f!!.id, it.id) }
        f?.takeIf { Etats.lire(it.condition).contains(ConditionCombat.CONCENTRATION) }?.let { fi ->
            GameState.setCondition(fi.id, Etats.ecrire(fi.condition, Etats.lire(fi.condition) - ConditionCombat.CONCENTRATION))
        }
        return effets.joinToString { it.nom }
    }

    /** Fiche du personnage d'un combattant (null pour un monstre du bestiaire). */
    private fun fiche(c: Combattant): Character? = c.characterId?.let { id -> GameState.characters.value.firstOrNull { it.id == id } }

    // ── Paladin : auras ──

    /**
     * Paladins du camp de [cible] (autres qu'elle) dont l'Aura de protection la couvre : debout,
     * pas Neutralisés, à portée de leur aura (3 m, 9 m avec Aura étendue).
     */
    private fun paladinsAuraAllies(etat: CombatEnCours, cible: Combattant): List<Pair<Combattant, Character>> =
        etat.combattants.mapNotNull { o ->
            if (o.id == cible.id || o.estMonstre != cible.estMonstre || o.horsCombat || o.mort || !Etats.peutAgir(o.conditions)) return@mapNotNull null
            val f = fiche(o)?.takeIf { Paladin.bonusAuraProtection(it) > 0 } ?: return@mapNotNull null
            if (etat.distance(o.id, cible.id).metres > Paladin.rayonAura(f) + 0.01) return@mapNotNull null
            o to f
        }

    /**
     * Aura de protection appliquée au JS de [cible] : la meilleure aura d'un Paladin allié à portée
     * (les auras ne se cumulent pas), et celle du Paladin lui-même retirée s'il est Neutralisé (elle
     * est déjà comprise dans ses sauvegardes). Retourne le combattant ajusté et la mention du journal.
     */
    private fun avecAuraProtection(etat: CombatEnCours, cible: Combattant): Pair<Combattant, String> {
        val propre = fiche(cible)?.let { Paladin.bonusAuraProtection(it) } ?: 0
        val propreActive = if (Etats.peutAgir(cible.conditions)) propre else 0
        val allie = paladinsAuraAllies(etat, cible).maxByOrNull { (_, f) -> Paladin.bonusAuraProtection(f) }
        val bonusAllie = allie?.let { Paladin.bonusAuraProtection(it.second) } ?: 0
        val delta = maxOf(propreActive, bonusAllie) - propre
        if (delta == 0) return cible to ""
        val mention = if (bonusAllie > propreActive) " (Aura de protection de ${allie!!.first.nom} : +$bonusAllie)"
        else " (Aura de protection inactive : Neutralisé)"
        return cible.copy(sauvegardes = cible.sauvegardes.mapValues { it.value + delta }) to mention
    }

    // ── Jets de sauvegarde lancés par l'application ──

    /**
     * [cibleId] fait un JS de [caracteristique] contre [dd] : sur un échec il subit [degats] et les
     * états [conditionsEchec] ; sur une réussite la moitié des dégâts ([moitieSiReussi]) ou rien.
     * Le jet et son résultat sont notés au journal sous [source].
     */
    private fun subirAvecSauvegarde(
        etat: CombatEnCours,
        cibleId: String,
        caracteristique: String?,
        dd: Int,
        degats: Int,
        moitieSiReussi: Boolean,
        conditionsEchec: List<ConditionCombat>,
        source: String,
        choix: List<String> = emptyList(),
        auteurId: String? = null,
        typeDegats: String? = null,
    ): CombatEnCours {
        val cible = etat.combattants.firstOrNull { it.id == cibleId } ?: return etat
        val (cibleAura, mentionAura) = avecAuraProtection(etat, cible)
        var js = Sauvegardes.lancer(cibleAura, caracteristique, dd, aleatoire, choix)
        // Occultiste Fiélon — Chance du ténébreux : +1d10 au JS raté, si le d10 peut encore le sauver.
        var chance = ""
        val ficheCible = fiche(cible)
        val total = js.total
        if (!js.reussi && total != null && total + 10 >= dd && ficheCible != null && Occultiste.chanceRestante(ficheCible) > 0) {
            val d10 = aleatoire.nextInt(1, 11)
            GameState.utiliserChanceTenebreux(ficheCible.id)
            js = js.copy(bonus = js.bonus + d10, reussi = total + d10 >= dd)
            chance = " (Chance du ténébreux : +$d10)"
        }
        // Boss — Résistance légendaire : un JS raté qui coûterait cher (un état, ou au moins le
        // quart de ses PV) devient une réussite, tant qu'il lui en reste.
        var base = etat
        val etatBoss = etat.boss[cibleId]
        if (!js.reussi && etatBoss != null && etatBoss.resistances > 0 && !cible.horsCombat &&
            (conditionsEchec.any { it !in cible.defenses.etats } || degats * 4 >= cible.pvMax)
        ) {
            js = js.copy(reussi = true)
            chance += " (Résistance légendaire)"
            base = majBoss(etat, cibleId) { it.copy(resistances = it.resistances - 1, resistancesForcees = it.resistancesForcees + 1) }
            base = annoncer(base, cibleId, "🛡 ${cible.nom} rate son jet de sauvegarde… et le réussit malgré tout : Résistance légendaire ! (${etatBoss.resistances - 1} restante(s))", table = "${cible.nom} : résistance légendaire", aLire = false)
        }
        // Esquive totale (Roublard / Moine niv. 7, pas Neutralisé) : JS de Dextérité à demi-dégâts →
        // aucun dégât s'il est réussi, la moitié s'il est raté.
        val esquiveTotale = degats > 0 && moitieSiReussi && js.caracteristique == "Dextérité" &&
            ficheCible?.let { Roublard.esquiveTotale(it) } == true && Etats.peutAgir(cible.conditions)
        val subis = when {
            esquiveTotale -> if (js.reussi) 0 else degats / 2
            !js.reussi -> degats
            moitieSiReussi -> degats / 2
            else -> 0
        }
        if (esquiveTotale) chance += " (Esquive totale)"
        val consequence = buildList {
            if (degats > 0) add("$subis dégât(s)")
            if (!js.reussi && conditionsEchec.isNotEmpty()) add(conditionsEchec.joinToString { it.label })
            if (js.reussi && degats == 0) add("aucun effet")
        }.joinToString(", ")
        var courant = base.copy(journal = base.journal + ("🎲 $source → ${cible.nom} : ${js.texte}$mentionAura$chance" + if (consequence.isNotEmpty()) " — $consequence" else ""))
            .raconter(
                TypeRecit.SAUVEGARDE,
                Narrateur.sauvegarde(cible.nom, source, js.reussi, js.caracteristique == "Dextérité", sansEffet = subis == 0),
                acteurId = auteurId, cibleId = cibleId, table = Narrateur.tableSauvegarde(cible.nom, js.reussi),
            )
        if (subis > 0) courant = pv(courant, cibleId, subis, auteurId, typeDegats)
        if (!js.reussi) conditionsEchec.forEach { courant = ajouterCondition(courant, cibleId, it, source) }
        return courant
    }

    /**
     * Créatures touchées par l'action à jet de sauvegarde (ou de zone) de [monstreId] : la zone
     * autour de [centreId] (sphère…) ou devant lui (cône, ligne) d'après les distances, sinon la
     * cible seule ; tous les adversaires debout pour une zone sans forme connue.
     */
    fun ciblesAttaqueSauvegarde(etat: CombatEnCours, monstreId: String, centreId: String? = null): List<Combattant> {
        val monstre = etat.combattants.firstOrNull { it.id == monstreId } ?: return emptyList()
        val attaque = etat.declarations[monstreId]?.attaque ?: return emptyList()
        val adversaires = etat.combattants.filter { it.estMonstre != monstre.estMonstre && !it.horsCombat }
        val centre = centreId ?: etat.declarations[monstreId]?.cibleId?.takeIf { id -> adversaires.any { it.id == id } }
            ?: adversaires.firstOrNull()?.id
        val zone = attaque.zoneEffet?.takeIf { attaque.zone }
        return when {
            zone != null -> etat.dansLaZone(zone, monstre.id, centre)
            attaque.zone -> adversaires
            else -> adversaires.filter { it.id == centre }
        }
    }

    /**
     * Résout l'action à jet de sauvegarde (souffle, sort…) déclarée par [monstreId] : chaque
     * créature touchée fait son JS, l'application applique les dégâts (moitié sur une réussite)
     * et les états sur un échec. Une seule fois par déclaration.
     */
    fun resoudreAttaqueSauvegarde(monstreId: String, centreId: String? = null, totalChoisi: Int? = null) = modifier { avant ->
        val monstre = avant.combattants.firstOrNull { it.id == monstreId } ?: return@modifier avant
        val d = avant.declarations[monstreId]?.takeIf { !it.degatsAppliques } ?: return@modifier avant
        val attaque = d.attaque ?: return@modifier avant
        // Il se place d'abord (zone tracée depuis sa nouvelle position), puis déchaîne son attaque.
        val etat = appliquerDeplacements(avant, monstreId)
            .raconter(TypeRecit.ACTION, "${monstre.nom} déchaîne ${attaque.nom} !", acteurId = monstreId, cibleId = centreId ?: d.cibleId)
        val touchees = ciblesAttaqueSauvegarde(etat, monstreId, centreId)
        val total = totalChoisi ?: d.resultats?.sumOf { it.degatsLances }?.takeIf { it > 0 }
            ?: attaque.formuleDegats?.let { lancerFormuleDes(it, aleatoire) } ?: attaque.degatsMoyens
        val source = "${monstre.nom} — ${attaque.nom}"
        var courant = etat.copy(
            declarations = etat.declarations + (monstreId to d.copy(degatsAppliques = true, deplacements = emptyMap())),
            journal = etat.journal + ("$source : ${touchees.size} créature(s) touchée(s), $total dégâts" +
                (attaque.dd?.let { " (JS ${attaque.sauvegarde ?: ""} DD $it)".replace("  ", " ") } ?: ""))
        )
        touchees.forEach { c ->
            courant = if (attaque.dd != null) {
                subirAvecSauvegarde(courant, c.id, attaque.sauvegarde, attaque.dd, total, moitieSiReussi = true, attaque.conditions.map { it.condition }, source, auteurId = monstreId, typeDegats = attaque.typeDegats)
            } else {
                pv(courant, c.id, total, monstreId, attaque.typeDegats)
            }
        }
        courant
    }

    /**
     * Applique à plusieurs créatures (sort de zone d'un joueur) les dégâts du jet [jetId] : chacune
     * fait le JS du sort déclaré (moitié ou rien sur une réussite, voir Sauvegardes.duResume),
     * ou subit les dégâts entiers si le sort n'impose pas de JS.
     */
    fun appliquerJetZone(jetId: String, cibleIds: List<String>) = modifier { etat ->
        val jet = etat.jets.firstOrNull { it.id == jetId && !it.applique } ?: return@modifier etat
        val auteur = etat.combattants.firstOrNull { it.id == jet.combattantId }?.nom ?: "?"
        val montant = jet.totalDegats ?: jet.total
        val js = Sauvegardes.duResume(etat.declarations[jet.combattantId]?.detail)
        val source = "$auteur — ${jet.libelle}"
        var courant = etat.copy(
            jets = etat.jets.map { if (it.id == jetId) it.copy(applique = true) else it },
            journal = etat.journal + "$source : $montant dégâts sur ${cibleIds.size} créature(s)"
        )
        cibleIds.distinct().forEach { id ->
            courant = if (js != null) subirAvecSauvegarde(courant, id, js.caracteristique, js.dd, montant, js.moitieSiReussi, emptyList(), source, auteurId = jet.combattantId, typeDegats = jet.typeDegats)
            else pv(courant, id, montant, jet.combattantId, jet.typeDegats)
        }
        courant
    }

    private val ddRegex = Regex("""DD\s*(\d+)""")

    /**
     * Empoignade ou bousculade déclarée par [combattantId] : la cible fait un JS de Force ou de
     * Dextérité (la meilleure) contre le DD du joueur ; ratée, elle est Agrippée ou À terre.
     */
    fun resoudreManoeuvre(combattantId: String) = modifier { avant ->
        val etat = appliquerDeplacements(avant, combattantId)
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        val d = etat.declarations[combattantId]?.takeIf { !it.degatsAppliques } ?: return@modifier etat
        val cible = etat.combattants.firstOrNull { it.id == d.cibleId && !it.horsCombat } ?: return@modifier etat
        val manoeuvres = d.attaquesJoueur.filter { it == ArsenalPersonnage.EMPOIGNADE || it == ArsenalPersonnage.BOUSCULADE }
        val dd = d.detail?.let { ddRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: return@modifier etat
        var courant = etat.copy(declarations = etat.declarations + (c.id to d.copy(degatsAppliques = true)))
        manoeuvres.forEach { m ->
            val etatInflige = if (m == ArsenalPersonnage.EMPOIGNADE) ConditionCombat.AGRIPPE else ConditionCombat.A_TERRE
            courant = subirAvecSauvegarde(courant, cible.id, null, dd, 0, false, listOf(etatInflige), "${c.nom} — $m", choix = listOf("Force", "Dextérité"))
        }
        courant
    }

    /**
     * Occultiste Fiélon (niveau 14) — Traversée des enfers, après une attaque qui touche : la cible
     * de [occultisteId] fait un JS de Charisme contre son DD des sorts ; ratée, elle subit 8d10
     * dégâts psychiques (sauf un fiélon) et l'état Neutralisé. Une fois par repos long.
     */
    fun traverseeDesEnfers(occultisteId: String, cibleId: String) = modifier { etat ->
        val o = etat.combattants.firstOrNull { it.id == occultisteId } ?: return@modifier etat
        val ficheO = fiche(o)?.takeIf { Occultiste.traverseeDisponible(it) } ?: return@modifier etat
        val cible = etat.combattants.firstOrNull { it.id == cibleId && !it.horsCombat } ?: return@modifier etat
        GameState.traverseeDesEnfers(ficheO.id, utilisee = true)
        // Un Fiélon projeté dans les Plans Inférieurs y est chez lui : ni dégâts ni état.
        val fielon = cible.typeCreature == "Fiélon"
        val degats = if (fielon) 0 else lancerFormuleDes("8d10", aleatoire) ?: 44
        subirAvecSauvegarde(
            etat, cible.id, "Charisme", Occultiste.ddSorts(ficheO), degats, moitieSiReussi = false,
            if (fielon) emptyList() else listOf(ConditionCombat.INCAPABLE_D_AGIR), "${o.nom} — Traversée des enfers", auteurId = o.id, typeDegats = "psychiques",
        )
    }

    /** Ajoute un état à un combattant, noté au journal (sans effet s'il l'a déjà). */
    private fun ajouterCondition(etat: CombatEnCours, combattantId: String, condition: ConditionCombat, source: String?): CombatEnCours {
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return etat
        if (condition in c.conditions) return etat
        // Immunité (fiche du bestiaire, Charmantes défenses de l'Archifée…) : l'état n'est pas subi.
        if (condition in c.defenses.etats) {
            return etat.copy(journal = etat.journal + ("${c.nom} est immunisé contre l'état ${condition.label}" + (source?.let { " — $it" } ?: "")))
                .raconter(TypeRecit.INFO, "${c.nom} ne peut pas être ${condition.label.lowercase()}.", cibleId = c.id)
        }
        // Aura de courage / Aura de dévotion d'un Paladin allié à portée.
        paladinsAuraAllies(etat, c).firstOrNull { (_, f) -> condition in Paladin.immunitesAura(f) }?.let { (paladin, _) ->
            val aura = if (condition == ConditionCombat.EFFRAYE) "Aura de courage" else "Aura de dévotion"
            return etat.copy(journal = etat.journal + ("${c.nom} est immunisé contre l'état ${condition.label} ($aura de ${paladin.nom})" + (source?.let { " — $it" } ?: "")))
                .raconter(TypeRecit.INFO, "L'aura de ${paladin.nom} protège ${c.nom}.", cibleId = c.id)
        }
        val maj = changerConditions(etat, c, c.conditions + condition)
        return maj.copy(journal = maj.journal + ("${c.nom} subit l'état ${condition.label}" + (source?.let { " — $it" } ?: "")))
            .raconter(TypeRecit.ETAT, Narrateur.etat(c.nom, condition), cibleId = c.id, table = Narrateur.tableEtat(c.nom, condition))
    }

    /**
     * Jet de sauvegarde contre la mort d'un personnage à 0 PV, au début de son tour : 10 ou plus
     * réussite, sinon échec ; 1 naturel = deux échecs ; 20 naturel = il reprend conscience avec
     * 1 PV. Trois réussites : stabilisé ; trois échecs : mort. Une fois par round : au début de son
     * tour, le jet attend le MJ ([noterJetContreLaMort]), qui note le d20 du joueur ou le fait lancer.
     */
    private fun preparerJetContreLaMort(etat: CombatEnCours, c: Combattant): CombatEnCours {
        val d = etat.declarations[c.id]
        if (d?.degatsAppliques == true || c.stable || c.mort) return etat
        vengeanceAveuglante(etat, c)?.let { return it }
        return etat.copy(declarations = etat.declarations + (c.id to (d ?: DeclarationAction(c.id, "Jet de sauvegarde contre la mort", parMj = true))))
    }

    /** Le MJ note le d20 du jet contre la mort de [combattantId] ([d20] null : l'appli le lance). */
    fun noterJetContreLaMort(combattantId: String, d20: Int?) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId && !it.estMonstre && it.horsCombat } ?: return@modifier etat
        jetContreLaMort(etat, c, d20?.coerceIn(1, 20))
    }

    /** Un jet contre la mort attend-il encore le MJ (personnage à 0 PV dont c'est le tour) ? */
    fun jetContreLaMortEnAttente(etat: CombatEnCours, c: Combattant): Boolean =
        !c.estMonstre && c.horsCombat && !c.stable && !c.mort && etat.actif?.id == c.id &&
            etat.declarations[c.id]?.let { it.libelle == "Jet de sauvegarde contre la mort" && !it.degatsAppliques } == true

    private fun jetContreLaMort(etat: CombatEnCours, c: Combattant, d20Saisi: Int? = null): CombatEnCours {
        val d = etat.declarations[c.id]
        if (d?.degatsAppliques == true || c.stable || c.mort) return etat
        // Champion niv. 18 — Trompe-la-mort : Avantage, et un 18 ou 19 compte comme un 20.
        val trompeLaMort = fiche(c)?.let { Guerrier.trompeLaMort(it) } == true
        // Don Gaillard — Trompe-la-mort : Avantage aux jets contre la mort (sans le 18-19 du Champion).
        val gaillard = fiche(c)?.let { Dons.avantageJetMort(it) } == true
        // d20 saisi par le MJ : le dé gardé (Avantage déjà appliqué à la table).
        val premier = d20Saisi ?: aleatoire.nextInt(1, 21)
        val ecarte = if (d20Saisi == null && (trompeLaMort || gaillard)) aleatoire.nextInt(1, 21) else null
        val d20 = maxOf(premier, ecarte ?: 0)
        val mention = ecarte?.let { " (${if (trompeLaMort) "Trompe-la-mort" else "Gaillard"} : Avantage ${minOf(premier, it)} écarté)" }
            ?: if (d20Saisi != null) " (dé de la table)" else ""
        var maj = c
        val resultat = when {
            d20 == 20 || (trompeLaMort && d20 >= 18) -> {
                maj = c.copy(pv = 1, succesMort = 0, echecsMort = 0, stable = false)
                maj.characterId?.let { GameState.updateCharacterHp(it, 1) }
                (if (d20 == 20) "20 naturel" else "$d20 (compte comme 20)") + " : ${c.nom} reprend conscience avec 1 PV"
            }
            d20 == 1 -> { maj = c.copy(echecsMort = c.echecsMort + 2); "1 naturel : deux échecs" }
            d20 >= 10 -> { maj = c.copy(succesMort = c.succesMort + 1); "réussite" }
            else -> { maj = c.copy(echecsMort = c.echecsMort + 1); "échec" }
        }
        val suite = when {
            maj.echecsMort >= 3 -> { maj = maj.copy(mort = true); " — 💀 ${c.nom} meurt" }
            maj.succesMort >= 3 -> { maj = maj.copy(stable = true, succesMort = 0, echecsMort = 0); " — ${c.nom} est stabilisé (inconscient)" }
            maj.pv > 0 -> ""
            else -> " (${maj.succesMort} réussite(s), ${maj.echecsMort} échec(s))"
        }
        val ligne = "🎲 ${c.nom} — jet contre la mort : d20 $d20$mention, $resultat$suite"
        val declaration = (d ?: DeclarationAction(c.id, "Jet de sauvegarde contre la mort", parMj = true))
            .copy(detail = "d20 $d20$mention, $resultat$suite", degatsAppliques = true)
        val (type, recit) = when {
            maj.mort -> TypeRecit.MORT to Narrateur.mort(c.nom)
            maj.pv > 0 -> TypeRecit.SOIN to "Miracle ! ${c.nom} rouvre les yeux et se relève avec 1 PV !"
            maj.stable -> TypeRecit.INFO to "${c.nom} cesse de saigner : stabilisé, mais toujours inconscient."
            d20 == 1 -> TypeRecit.INFO to "${c.nom} lutte contre la mort… et faiblit dangereusement (${maj.succesMort} ✓ / ${maj.echecsMort} ✗)."
            d20 >= 10 -> TypeRecit.INFO to "${c.nom} lutte contre la mort et s'accroche à la vie (${maj.succesMort} ✓ / ${maj.echecsMort} ✗)."
            else -> TypeRecit.INFO to "${c.nom} lutte contre la mort… et faiblit (${maj.succesMort} ✓ / ${maj.echecsMort} ✗)."
        }
        return remplacer(etat, maj).copy(
            declarations = etat.declarations + (c.id to declaration),
            journal = etat.journal + ligne,
        ).raconter(
            type, recit, acteurId = c.id, cibleId = c.id,
            table = when {
                maj.mort -> "${c.nom} est mort"
                maj.pv > 0 -> "${c.nom} se relève (1 PV)"
                maj.stable -> "${c.nom} est stabilisé"
                else -> "${c.nom} : jet contre la mort ${if (d20 >= 10) "réussi" else "raté"} (${maj.succesMort} ✓ / ${maj.echecsMort} ✗)"
            },
        )
    }

    /**
     * Occultiste Céleste (niveau 14) — Vengeance aveuglante, 1/repos long : quand lui-même ou un
     * allié à 18 m va faire un jet contre la mort, ce dernier regagne la moitié de ses PV max (et se
     * relève) ; chaque ennemi à 9 m de lui subit 2d8 + Charisme dégâts radiants et est Aveuglé.
     * Null si aucun occultiste céleste ne peut le faire.
     */
    private fun vengeanceAveuglante(etat: CombatEnCours, aTerre: Combattant): CombatEnCours? {
        val (celeste, ficheC) = etat.combattants.asSequence()
            .filter { it.estMonstre == aTerre.estMonstre && !it.mort }
            .mapNotNull { o -> fiche(o)?.takeIf { Occultiste.vengeanceDisponible(it) }?.let { o to it } }
            .firstOrNull { (o, _) -> o.id == aTerre.id || (!o.horsCombat && etat.distance(o.id, aTerre.id).metres <= Distance.M18.metres + 0.01) }
            ?: return null
        GameState.aptitudeOccultiste(ficheC.id, "vengeance-aveuglante", utilisee = true)
        val soin = (aTerre.pvMax / 2).coerceAtLeast(1)
        var courant = remplacer(etat, aTerre.copy(pv = soin, succesMort = 0, echecsMort = 0, stable = false, conditions = aTerre.conditions - ConditionCombat.A_TERRE))
        aTerre.characterId?.let { GameState.updateCharacterHp(it, soin) }
        val declaration = (etat.declarations[aTerre.id] ?: DeclarationAction(aTerre.id, "Jet de sauvegarde contre la mort", parMj = true))
            .copy(detail = "Vengeance aveuglante de ${celeste.nom} : ${aTerre.nom} regagne $soin PV", degatsAppliques = true)
        courant = courant.copy(
            declarations = courant.declarations + (aTerre.id to declaration),
            journal = courant.journal + "☀ ${celeste.nom} — Vengeance aveuglante : ${aTerre.nom} regagne $soin PV et se relève",
        ).raconter(TypeRecit.SOIN, "☀ Une lumière aveuglante jaillit de ${celeste.nom} : ${aTerre.nom} se relève !", acteurId = celeste.id, cibleId = aTerre.id, montant = soin, table = Narrateur.tableSoin(aTerre.nom, soin))
        val bonus = ArsenalPersonnage.mod(ficheC.charisma)
        courant.combattants.filter { it.estMonstre != aTerre.estMonstre && !it.horsCombat && courant.distance(aTerre.id, it.id).metres <= Distance.COURTE.metres + 0.01 }
            .forEach { ennemi ->
                val degats = ((lancerFormuleDes("2d8", aleatoire) ?: 9) + bonus).coerceAtLeast(1)
                courant = pv(courant, ennemi.id, degats, celeste.id, "radiants")
                courant = ajouterCondition(courant, ennemi.id, ConditionCombat.AVEUGLE, "Vengeance aveuglante")
            }
        return courant
    }

    /**
     * Début du tour du combattant actif : effets de Paladin (Régénération du Champion des Anciens,
     * auras du Nimbe sacré et de l'Ange vengeur), jet contre la mort d'un personnage à 0 PV, puis
     * décision de l'IA s'il est piloté (voir [deciderAuTour]).
     */
    private fun debutDeTour(etat: CombatEnCours): CombatEnCours {
        val debut = etat.actif?.let { etat.raconter(TypeRecit.TOUR, Narrateur.tour(it), acteurId = it.id) } ?: return etat
        var courant = debutDeTourBoss(effetsChampionDebutDeTour(effetsPaladinDebutDeTour(debut, debut.actif!!), debut.actif!!.id))
        val actif = courant.combattants.firstOrNull { it.id == etat.actif?.id } ?: return courant
        if (!actif.estMonstre && actif.horsCombat) return preparerJetContreLaMort(courant, actif)
        // Décision prise d'avance (attaque annoncée d'un boss) devenue sans objet : sa cible est tombée.
        val prevue = courant.declarations[actif.id]
        val cibleTombee = prevue?.cibleId?.let { id -> courant.combattants.firstOrNull { it.id == id }?.horsCombat } == true
        if (prevue != null && prevue.parIa && !prevue.degatsAppliques && cibleTombee) {
            courant = remplacer(courant, liberer(actif, prevue)).let { it.copy(declarations = it.declarations - actif.id) }
        }
        return deciderAuTour(courant)
    }

    /**
     * Le combattant actif n'a pas encore d'action : l'IA décide pour un monstre ou un PNJ piloté,
     * « Aucune action » s'il est Neutralisé. Un personnage joué choisit lui-même (appli, page web
     * de la table, ou le MJ saisit ce qu'il annonce).
     */
    private fun deciderAuTour(etat: CombatEnCours): CombatEnCours {
        val actif = etat.actif ?: return etat
        if (actif.inerte || actif.horsCombat || actif.id in etat.declarations) return etat
        if (!Etats.peutAgir(actif.conditions)) {
            val etatBloquant = Etats.bloquant(actif.conditions)?.label
            return etat.copy(
                declarations = etat.declarations + (actif.id to DeclarationAction(actif.id, "Aucune action", detail = "$etatBloquant : ne peut pas agir", parMj = true)),
            ).raconter(TypeRecit.INFO, "${actif.nom} ne peut pas agir (${etatBloquant?.lowercase()}).", acteurId = actif.id)
        }
        if (!actif.piloteParIa) return etat
        val decide = appliquerDecisionIa(etat, actif)
        // Une attaque est racontée quand le MJ l'applique ; les autres décisions tout de suite.
        val d = decide.declarations[actif.id]?.takeIf { it.attaque == null } ?: return decide
        return decide.raconter(TypeRecit.ACTION, Narrateur.action(actif.nom, d), acteurId = actif.id, cibleId = d.cibleId)
    }

    /**
     * Guerrier Champion au début de son tour : Héros du champ d'honneur (niv. 10, Inspiration
     * héroïque s'il n'en a pas) et Résilience héroïque (niv. 18, En péril avec au moins 1 PV :
     * 5 + Constitution PV).
     */
    private fun effetsChampionDebutDeTour(etat: CombatEnCours, actifId: String): CombatEnCours {
        val actif = etat.combattants.firstOrNull { it.id == actifId }?.takeIf { !it.mort } ?: return etat
        val f = fiche(actif) ?: return etat
        var courant = etat
        if (Guerrier.herosDuChampDHonneur(f) && !f.heroicInspiration) {
            GameState.setHeroicInspiration(f.id, true)
            courant = courant.copy(journal = courant.journal + "⚔ ${actif.nom} — Héros du champ d'honneur : Inspiration héroïque")
        }
        val pvSurvivant = Guerrier.pvResilienceHeroique(f)
        if (pvSurvivant > 0 && actif.pv in 1..(actif.pvMax / 2)) {
            courant = courant.copy(journal = courant.journal + "⚔ ${actif.nom} — Survivant (Résilience héroïque)")
            courant = pv(courant, actif.id, -pvSurvivant)
        }
        return courant
    }

    /**
     * Aptitudes de niveau 20 des serments, actives sur la fiche (effet en cours) :
     * - Champion des Anciens : le Paladin regagne 10 PV au début de son tour ;
     * - Nimbe sacré : un ennemi qui commence son tour dans l'aura subit Cha + maîtrise dégâts radiants ;
     * - Ange vengeur : un ennemi qui commence son tour dans l'aura fait un JS de Sagesse ou est Effrayé.
     */
    private fun effetsPaladinDebutDeTour(etat: CombatEnCours, actif: Combattant): CombatEnCours {
        if (actif.mort) return etat
        var courant = etat
        fiche(actif)?.takeIf { Paladin.effetActif(it, Paladin.CHAMPION_DES_ANCIENS) && actif.pv < actif.pvMax }?.let {
            courant = courant.copy(journal = courant.journal + "🌿 ${actif.nom} — Champion des Anciens : régénération")
            courant = pv(courant, actif.id, -Paladin.REGENERATION_CHAMPION)
        }
        if (actif.horsCombat) return courant
        etat.combattants.filter { o -> o.estMonstre != actif.estMonstre && !o.horsCombat && !o.mort && Etats.peutAgir(o.conditions) }
            .forEach { o ->
                val f = fiche(o) ?: return@forEach
                if (courant.distance(o.id, actif.id).metres > Paladin.rayonAura(f) + 0.01) return@forEach
                val cible = courant.combattants.firstOrNull { it.id == actif.id }?.takeIf { !it.horsCombat } ?: return courant
                if (Paladin.effetActif(f, Paladin.NIMBE_SACRE)) {
                    courant = courant.copy(journal = courant.journal + "☀ ${o.nom} — Nimbe sacré : ${cible.nom} commence son tour dans l'aura")
                    courant = pv(courant, cible.id, Paladin.degatsNimbe(f), o.id, "radiants")
                }
                if (Paladin.effetActif(f, Paladin.ANGE_VENGEUR) && ConditionCombat.EFFRAYE !in cible.conditions) {
                    courant = subirAvecSauvegarde(
                        courant, cible.id, "Sagesse", Paladin.ddSorts(f), 0, moitieSiReussi = false,
                        listOf(ConditionCombat.EFFRAYE), "${o.nom} — Ange vengeur (aura effrayante)", auteurId = o.id,
                    )
                }
            }
        return courant
    }

    fun basculerCondition(combattantId: String, condition: ConditionCombat) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        changerConditions(etat, c, if (condition in c.conditions) c.conditions - condition else c.conditions + condition)
    }

    /**
     * Nouveaux états de [c] et leurs conséquences : un combattant qui devient Neutralisé perd sa
     * concentration et, si c'est son tour, reçoit « Aucune action » à la place de son choix ;
     * redevenu capable d'agir pendant son tour, il peut de nouveau choisir (l'IA redécide). Les
     * états d'un personnage sont reportés sur sa fiche ([reporterSurFiche]), mentions libres conservées.
     */
    private fun changerConditions(
        etat: CombatEnCours,
        c: Combattant,
        nouvelles: Set<ConditionCombat>,
        reporterSurFiche: Boolean = true,
    ): CombatEnCours {
        val avantPeutAgir = Etats.peutAgir(c.conditions)
        val apresPeutAgir = Etats.peutAgir(nouvelles)
        var conditions = nouvelles
        val journal = mutableListOf<String>()
        if (!apresPeutAgir && ConditionCombat.CONCENTRATION in conditions) {
            conditions = conditions - ConditionCombat.CONCENTRATION
            journal += "${c.nom} perd sa concentration (${Etats.bloquant(nouvelles)?.label})"
        }
        val maj = c.copy(conditions = conditions)
        if (reporterSurFiche) {
            maj.characterId?.let { id -> GameState.characters.value.firstOrNull { it.id == id } }?.let { fiche ->
                val nouveauTexte = Etats.ecrire(fiche.condition, conditions)
                if (nouveauTexte != fiche.condition) GameState.setCondition(fiche.id, nouveauTexte)
            }
        }
        var courant = remplacer(etat, maj).let { it.copy(journal = it.journal + journal) }
        if (courant.phase != PhaseCombat.EN_COURS || maj.horsCombat || avantPeutAgir == apresPeutAgir || courant.actif?.id != maj.id) return courant
        val ancienne = courant.declarations[maj.id]
        if (ancienne?.degatsAppliques == true) return courant
        courant = remplacer(courant, liberer(maj, ancienne)).let { it.copy(declarations = it.declarations - maj.id) }
        return deciderAuTour(courant)
    }

    /** Vitesse 0 : le déplacement annoncé est retiré, et l'action le signale. */
    private fun sansDeplacementSiImmobile(c: Combattant, d: DeclarationAction): DeclarationAction {
        val immobilise = Etats.immobilisant(c.conditions) ?: return d
        if (d.deplacements.isEmpty() && d.zoneVisee == null) return d
        val note = "${immobilise.label} : vitesse 0, déplacement annulé"
        return d.copy(deplacements = emptyMap(), zoneVisee = null, desengage = false, detail = listOfNotNull(d.detail, note).joinToString(" — "))
    }

    // ── Boss (voir Boss.kt) ──

    /** Le MJ active ou coupe les actions légendaires jouées seules par l'IA du boss. */
    fun basculerLegendairesAuto(bossId: String) = modifier { e -> majBoss(e, bossId) { it.copy(legendairesAuto = !it.legendairesAuto) } }

    /**
     * Le MJ fait jouer une action légendaire au boss pendant la résolution : sur [cibleId], sinon
     * sur les cibles que choisirait l'IA (à portée).
     */
    fun actionLegendaire(bossId: String, actionNom: String, cibleId: String? = null) = modifier { e ->
        val b = e.boss[bossId] ?: return@modifier e
        val boss = e.combattants.firstOrNull { it.id == bossId } ?: return@modifier e
        val action = b.definition.actionsLegendaires.firstOrNull { it.nom == actionNom && it.cout <= b.points } ?: return@modifier e
        if (!peutAgirLegendaire(e, boss, b)) return@modifier e
        val cibles = cibleId?.let { id -> e.combattants.filter { it.id == id && !it.horsCombat } } ?: ciblesActionBoss(e, boss, action, null, repaire = false)
        jouerLegendaire(e, boss, action, cibles)
    }

    /** Le MJ déclenche une action de repaire ([actionNom], sinon tirée au hasard). */
    fun actionRepaire(bossId: String, actionNom: String? = null) = modifier { e -> declencherRepaire(e, bossId, actionNom) }

    /** Le MJ fait passer le boss à sa phase suivante sans attendre ses PV. */
    fun phaseSuivante(bossId: String) = modifier { e ->
        val b = e.boss[bossId] ?: return@modifier e
        if (b.phase + 1 >= b.definition.phases.size) e else entrerEnPhase(e, bossId, b.phase + 1)
    }

    /** Le boss prend la fuite : annoncé à la table, puis il quitte le combat (sans butin). */
    fun bossFuit(bossId: String) {
        val nom = _etat.value?.combattants?.firstOrNull { it.id == bossId }?.nom ?: return
        modifier { e -> annoncer(e, bossId, "🏃 $nom prend la fuite… Ce n'est que partie remise !", table = "$nom prend la fuite") }
        retirer(bossId)
    }

    private fun majBoss(etat: CombatEnCours, bossId: String, f: (EtatBoss) -> EtatBoss): CombatEnCours {
        val b = etat.boss[bossId] ?: return etat
        return etat.copy(boss = etat.boss + (bossId to f(b)))
    }

    /**
     * Texte du boss pour le MJ : mise en scène à lire à voix haute ([aLire], encadré « À lire »
     * de son écran) ou information de règle. Les joueurs et l'écran de la table n'en voient que
     * le fait, [table] (« Cryovain : phase 2 »), ou rien.
     */
    private fun annoncer(etat: CombatEnCours, bossId: String, texte: String, table: String? = null, aLire: Boolean = true): CombatEnCours =
        majBoss(etat, bossId) { it.copy(annonces = (it.annonces + AnnonceBoss(etat.round, texte, aLire)).takeLast(12)) }
            .let { it.copy(journal = it.journal + "👑 $texte") }
            .raconter(TypeRecit.BOSS, (if (aLire) "📢 " else "") + texte, acteurId = bossId, table = table)

    /** Le MJ a lu à voix haute les textes en attente de [bossId] (encadré « À lire »). */
    fun marquerAnnoncesLues(bossId: String) = modifier { e ->
        majBoss(e, bossId) { b -> b.copy(annonces = b.annonces.map { it.copy(lue = true) }) }
    }

    /** CA du boss d'après sa phase et ses points faibles. */
    private fun appliquerCaBoss(etat: CombatEnCours, bossId: String): CombatEnCours {
        val b = etat.boss[bossId] ?: return etat
        val boss = etat.combattants.firstOrNull { it.id == bossId } ?: return etat
        return if (boss.ca == b.caActuelle) etat else remplacer(etat, boss.copy(ca = b.caActuelle))
    }

    private fun etatsBloquants(c: Combattant): Set<ConditionCombat> =
        c.conditions.filter { !Etats.peutAgir(listOf(it)) || Etats.immobilisant(listOf(it)) != null }.toSet()

    /**
     * Après chaque changement : chute du boss, passage de phase (seuils de PV), points faibles tous
     * détruits, jauge de déséquilibre pleine (attaque annoncée interrompue, ou plus d'action
     * légendaire jusqu'à son tour).
     */
    private fun suiviDesBoss(etat: CombatEnCours): CombatEnCours =
        if (etat.boss.isEmpty()) etat else etat.boss.keys.fold(etat) { e, id -> suiviBoss(e, id) }

    private fun suiviBoss(etat: CombatEnCours, bossId: String): CombatEnCours {
        val boss = etat.combattants.firstOrNull { it.id == bossId } ?: return etat
        var b = etat.boss[bossId] ?: return etat
        var courant = etat
        if (boss.horsCombat) {
            if (b.vaincu) return courant
            courant = majBoss(courant, bossId) { it.copy(vaincu = true, prepare = false) }
            return annoncer(courant, bossId, "🏆 ${b.definition.defaite}" + (b.coupFinal?.let { " Coup final : $it !" } ?: ""))
        }
        if (courant.demarre) {
            val cible = BossCatalogue.phasePour(b.definition, boss.pv * 100 / boss.pvMax.coerceAtLeast(1))
            for (i in (b.phase + 1)..cible) courant = entrerEnPhase(courant, bossId, i)
        }
        b = courant.boss[bossId] ?: return courant
        val pointsFaibles = b.pointsFaiblesIds.mapNotNull { id -> courant.combattants.firstOrNull { it.id == id } }
        if (!b.pointsFaiblesDetruits && b.pointsFaiblesIds.isNotEmpty() && pointsFaibles.all { it.horsCombat }) {
            courant = appliquerCaBoss(majBoss(courant, bossId) { it.copy(pointsFaiblesDetruits = true) }, bossId)
            courant = annoncer(courant, bossId, "💎 " + (b.definition.annoncePointsFaiblesDetruits ?: "Tous les points faibles de ${boss.nom} sont détruits !"), table = "Points faibles de ${boss.nom} détruits")
        }
        b = courant.boss[bossId] ?: return courant
        if (courant.phase == PhaseCombat.PREPARATION || b.jauge < b.seuilDesequilibre(boss.pvMax)) return courant
        val annoncee = b.definition.attaqueAnnoncee
        return when {
            // Frappé assez fort avant son tour : l'attaque annoncée avorte, il perd son action.
            b.prepare && annoncee != null -> {
                courant = majBoss(courant, bossId) { it.copy(prepare = false, desequilibre = true, points = 0, jauge = 0, interruptions = it.interruptions + 1) }
                courant = courant.copy(
                    declarations = courant.declarations + (bossId to DeclarationAction(
                        bossId, "${annoncee.attaqueNom} interrompu", detail = "Déséquilibré : perd son action ce round", parMj = true, degatsAppliques = true,
                    ))
                )
                annoncer(courant, bossId, "💥 ${annoncee.interruption}", table = "${boss.nom} : ${annoncee.attaqueNom} interrompu")
            }
            !b.desequilibre -> {
                courant = majBoss(courant, bossId) { it.copy(desequilibre = true, points = 0, jauge = 0) }
                annoncer(courant, bossId, "💥 ${boss.nom} vacille sous vos coups : déséquilibré, plus d'action légendaire avant son prochain tour !", table = "${boss.nom} est déséquilibré")
            }
            else -> courant
        }
    }

    /**
     * Entrée dans la phase [index] : annonce, comportement, CA, points légendaires gagnés, capacités
     * rechargées, états bloquants retirés et renforts.
     */
    private fun entrerEnPhase(etat: CombatEnCours, bossId: String, index: Int): CombatEnCours {
        val b = etat.boss[bossId] ?: return etat
        val phase = b.definition.phases.getOrNull(index) ?: return etat
        val boss = etat.combattants.firstOrNull { it.id == bossId } ?: return etat
        val gain = phase.bonusPointsLegendaires - (b.phaseCourante?.bonusPointsLegendaires ?: 0)
        var courant = majBoss(etat, bossId) { it.copy(phase = index, points = (it.points + gain).coerceAtLeast(0)) }
        courant = annoncer(courant, bossId, "🔥 Phase ${index + 1} — ${phase.nom} : ${phase.annonce}", table = "${boss.nom} : phase ${index + 1}")
        val rechargees = if (phase.rechargeImmediate) boss.attaques.filter { it.recharge != null }.map { it.nom }.toSet() else emptySet()
        val maj = boss.copy(
            profilIA = phase.profil ?: boss.profilIA,
            comportementId = if (phase.profil != null) null else boss.comportementId,
            raisonProfil = "Boss — phase « ${phase.nom} »",
            capacitesUtilisees = boss.capacitesUtilisees - rechargees,
        )
        courant = remplacer(courant, maj)
        val bloquants = etatsBloquants(maj)
        if (bloquants.isNotEmpty()) {
            courant = changerConditions(courant, maj, maj.conditions - bloquants)
            courant = courant.copy(journal = courant.journal + "${boss.nom} se libère : ${bloquants.joinToString { it.label }}")
        }
        courant = appliquerCaBoss(courant, bossId)
        b.renforts[index]?.takeIf { it.isNotEmpty() }?.let { renforts ->
            courant = insererCombattants(courant, renforts)
            // Arrivée annoncée à part : le MJ sait qui entre en scène et quand il jouera.
            val arrives = renforts.mapNotNull { r -> courant.combattants.firstOrNull { it.id == r.id } }
            if (arrives.isNotEmpty()) courant = annoncer(
                courant, bossId,
                "🐺 Renfort : ${arrives.joinToString { "${it.nom} (initiative ${it.initiative ?: "?"})" }} " +
                    (if (arrives.size > 1) "rejoignent" else "rejoint") + " le combat aux côtés de ${boss.nom} !",
                table = "${arrives.joinToString { it.nom }} ${if (arrives.size > 1) "rejoignent" else "rejoint"} le combat",
            )
        }
        return courant
    }

    /**
     * Début du tour du boss : actions légendaires et jauge remises à zéro, attaque annoncée
     * désormais résolue ; il se libère de ses états bloquants (contrôles atténués, une action
     * légendaire de moins ce round) et régénère tant qu'un point faible tient.
     */
    private fun debutDeTourBoss(etat: CombatEnCours): CombatEnCours {
        val actif = etat.actif ?: return etat
        val b = etat.boss[actif.id] ?: return etat
        if (actif.horsCombat) return etat
        val bloquants = etatsBloquants(actif)
        var courant = majBoss(etat, actif.id) {
            it.copy(points = if (bloquants.isEmpty()) it.pointsMax else (it.pointsMax - 1).coerceAtLeast(0), jauge = 0, prepare = false, desequilibre = false)
        }
        if (bloquants.isNotEmpty()) {
            courant = changerConditions(courant, actif, actif.conditions - bloquants)
            courant = annoncer(courant, actif.id, "${actif.nom} se libère : ${bloquants.joinToString { it.label }} (une action légendaire de moins ce round)", table = "${actif.nom} se libère (${bloquants.joinToString { it.label.lowercase() }})", aLire = false)
        }
        val debout = b.pointsFaiblesIds.mapNotNull { id -> courant.combattants.firstOrNull { it.id == id } }.filter { !it.horsCombat }
        val bossActuel = courant.combattants.firstOrNull { it.id == actif.id } ?: return courant
        if (b.definition.regeneration > 0 && debout.isNotEmpty() && bossActuel.pv < bossActuel.pvMax) {
            courant = annoncer(courant, actif.id, "❄ ${debout.joinToString { it.nom }} : ${actif.nom} régénère ${b.definition.regeneration} PV", aLire = false)
            courant = pv(courant, actif.id, -b.definition.regeneration)
        }
        return courant
    }

    /**
     * Décision de l'IA d'un boss prise en début de round ([anticipee]) : son attaque annoncée
     * (souffle…) est lue à la table avant son tour, les joueurs peuvent tenter de l'interrompre.
     */
    private fun annonceAttaqueBoss(etat: CombatEnCours, bossId: String, d: DeclarationAction, anticipee: Boolean): CombatEnCours {
        val b = etat.boss[bossId] ?: return etat
        val annoncee = b.definition.attaqueAnnoncee ?: return etat
        val prepare = anticipee && d.attaque?.nom == annoncee.attaqueNom
        if (prepare == b.prepare) return etat
        val maj = majBoss(etat, bossId) { it.copy(prepare = prepare, jauge = if (prepare) 0 else it.jauge) }
        return if (prepare) annoncer(maj, bossId, "⚠ ${annoncee.annonce}", table = "⚠ ${etat.combattants.firstOrNull { it.id == bossId }?.nom ?: "Le boss"} prépare ${annoncee.attaqueNom} !") else maj
    }

    private fun peutAgirLegendaire(e: CombatEnCours, boss: Combattant, b: EtatBoss): Boolean =
        e.phase == PhaseCombat.EN_COURS && !boss.horsCombat && Etats.peutAgir(boss.conditions) && !b.desequilibre && b.points > 0

    /** Après le tour d'un adversaire, chaque boss (IA) peut dépenser ses points d'actions légendaires. */
    private fun legendairesApresTour(etat: CombatEnCours, finiId: String): CombatEnCours {
        val fini = etat.combattants.firstOrNull { it.id == finiId } ?: return etat
        return etat.boss.keys.fold(etat) { e, bossId ->
            val b = e.boss[bossId] ?: return@fold e
            val boss = e.combattants.firstOrNull { it.id == bossId } ?: return@fold e
            if (!b.legendairesAuto || fini.estMonstre == boss.estMonstre || !peutAgirLegendaire(e, boss, b)) return@fold e
            val (action, cibles) = choisirLegendaire(e, boss, b, finiId) ?: return@fold e
            jouerLegendaire(e, boss, action, cibles)
        }
    }

    /**
     * Action légendaire choisie par l'IA : une action autour de lui s'il est encerclé (2 adversaires
     * ou plus), sinon une attaque, sinon une action à distance — de préférence contre celui qui
     * vient d'agir.
     */
    private fun choisirLegendaire(e: CombatEnCours, boss: Combattant, b: EtatBoss, apresId: String): Pair<ActionBoss, List<Combattant>>? =
        b.definition.actionsLegendaires.filter { it.cout <= b.points }
            .mapNotNull { a ->
                val cibles = ciblesActionBoss(e, boss, a, apresId, repaire = false)
                val utile = if (a.forme == FormeActionBoss.SAUVEGARDE_CONTACT) cibles.size >= 2 else cibles.isNotEmpty()
                if (utile) a to cibles else null
            }
            .maxByOrNull { (a, cibles) ->
                when (a.forme) {
                    FormeActionBoss.SAUVEGARDE_CONTACT, FormeActionBoss.SAUVEGARDE_TOUS -> 100 + cibles.size
                    FormeActionBoss.ATTAQUE -> 50
                    FormeActionBoss.SAUVEGARDE_CIBLES -> 40
                } + aleatoire.nextInt(5)
            }

    private fun jouerLegendaire(e: CombatEnCours, boss: Combattant, action: ActionBoss, cibles: List<Combattant>): CombatEnCours {
        val courant = majBoss(e, boss.id) { it.copy(points = (it.points - action.cout).coerceAtLeast(0)) }
        val restants = courant.boss[boss.id]?.points ?: 0
        val visees = cibles.takeIf { it.isNotEmpty() && action.forme != FormeActionBoss.SAUVEGARDE_TOUS }?.joinToString(prefix = " → ") { it.nom }.orEmpty()
        // À lire, avec son résultat : sinon des PV baissaient sans que le MJ voie pourquoi.
        return annoncerEtExecuter(
            courant, boss.id, action, cibles,
            texte = "⚡ Action légendaire — ${action.nom}$visees : ${action.description} ($restants point(s) restant(s))",
            table = "${boss.nom} : action légendaire (${action.nom})",
            source = "${boss.nom} — ${action.nom} (action légendaire)",
        )
    }

    /** Action de repaire automatique de fin de round, si la phase du boss l'active. */
    private fun actionRepaireAuto(e: CombatEnCours, bossId: String): CombatEnCours {
        val b = e.boss[bossId] ?: return e
        val boss = e.combattants.firstOrNull { it.id == bossId } ?: return e
        return if (!b.repaireActif || boss.horsCombat || e.phase != PhaseCombat.EN_COURS) e else declencherRepaire(e, bossId, null)
    }

    /**
     * [attaquantId] vient de rater une attaque : s'il n'a pas été blessé ce round, le repaire de
     * chaque boss adverse (phase active) se retourne contre lui — une fois par round et par personnage.
     */
    private fun repaireSurRate(e: CombatEnCours, attaquantId: String): CombatEnCours {
        val attaquant = e.combattants.firstOrNull { it.id == attaquantId } ?: return e
        return e.boss.keys.fold(e) { courant, bossId ->
            val b = courant.boss[bossId] ?: return@fold courant
            val boss = courant.combattants.firstOrNull { it.id == bossId } ?: return@fold courant
            if (!b.repaireActif || boss.horsCombat || boss.estMonstre == attaquant.estMonstre || courant.phase != PhaseCombat.EN_COURS ||
                attaquantId in b.blessesDuRound || attaquantId in b.repaireRatesDuRound
            ) return@fold courant
            val marque = majBoss(courant, bossId) { it.copy(repaireRatesDuRound = it.repaireRatesDuRound + attaquantId) }
            declencherRepaire(marque, bossId, null, prefereeId = attaquantId)
        }
    }

    /** Action de repaire [nom] (sinon au hasard, jamais deux fois la même de suite), [prefereeId] visé en premier. */
    private fun declencherRepaire(e: CombatEnCours, bossId: String, nom: String?, prefereeId: String? = null): CombatEnCours {
        val b = e.boss[bossId] ?: return e
        val boss = e.combattants.firstOrNull { it.id == bossId } ?: return e
        val actions = b.definition.actionsRepaire
        val action = nom?.let { n -> actions.firstOrNull { it.nom == n } }
            ?: actions.filter { it.nom != b.dernierRepaire }.ifEmpty { actions }.randomOrNull(aleatoire) ?: return e
        val cibles = ciblesActionBoss(e, boss, action, prefereeId, repaire = true)
        val visees = cibles.takeIf { it.isNotEmpty() && action.forme != FormeActionBoss.SAUVEGARDE_TOUS }?.joinToString(prefix = " → ") { it.nom }.orEmpty()
        var courant = majBoss(e, bossId) { it.copy(dernierRepaire = action.nom) }
        return annoncerEtExecuter(
            courant, bossId, action, cibles,
            texte = "🏔 Repaire — ${action.nom}$visees : ${action.description}",
            table = "Repaire : ${action.nom}",
            source = "Repaire de ${boss.nom} — ${action.nom}",
        )
    }

    /**
     * Annonce à lire d'une action de boss jouée hors de son tour (repaire, action légendaire), puis
     * sa résolution : le résultat (jets, dégâts, états déjà appliqués) est joint à l'annonce, pour
     * que le MJ sache pourquoi des PV ont changé.
     */
    private fun annoncerEtExecuter(
        e: CombatEnCours, bossId: String, action: ActionBoss, cibles: List<Combattant>,
        texte: String, table: String, source: String,
    ): CombatEnCours {
        val courant = annoncer(e, bossId, texte, table = table)
        val avant = courant.journal.size
        val resolu = executerActionBoss(courant, bossId, action, cibles, source)
        val resultat = resolu.journal.drop(avant).map { it.removePrefix("🎲 ").removePrefix("$source → ") }
            .ifEmpty { listOf("Personne n'est touché.") }
        val touches = courant.combattants.filter { c -> resolu.combattants.firstOrNull { it.id == c.id } != c }
        return majBoss(resolu, bossId) { b ->
            val i = b.annonces.indexOfLast { it.texte == texte }
            if (i < 0) b else b.copy(annonces = b.annonces.toMutableList().also { it[i] = it[i].copy(resultat = resultat, avant = touches) })
        }
    }

    /**
     * Le MJ fixe lui-même les dégâts d'une action de repaire ou légendaire encore affichée :
     * [montants] par combattant touché (0 = aucun). Ses PV repartent d'avant l'action, ses états restent.
     */
    fun ajusterDegatsAnnonce(bossId: String, index: Int, montants: Map<String, Int>) = modifier { e ->
        val annonce = e.boss[bossId]?.annonces?.getOrNull(index)?.takeIf { !it.lue && !it.annulee && it.avant.isNotEmpty() } ?: return@modifier e
        var courant = e
        val lignes = mutableListOf<String>()
        annonce.avant.forEach { avant ->
            val montant = montants[avant.id]?.coerceAtLeast(0) ?: return@forEach
            val actuel = courant.combattants.firstOrNull { it.id == avant.id } ?: return@forEach
            courant = remplacer(courant, actuel.copy(
                pv = avant.pv, pvTemporaires = avant.pvTemporaires, succesMort = avant.succesMort,
                echecsMort = avant.echecsMort, stable = avant.stable, mort = avant.mort,
            ))
            if (montant > 0) courant = pv(courant, avant.id, montant)
            else avant.characterId?.let { GameState.updateCharacterHp(it, avant.pv) }
            val apres = courant.combattants.first { it.id == avant.id }
            lignes += "Ajusté par le MJ : ${avant.nom} subit $montant dégât(s) → ${apres.pv}/${apres.pvMax}"
        }
        if (lignes.isEmpty()) return@modifier e
        majBoss(courant, bossId) { b ->
            // Avant gardé : le MJ peut encore corriger ou annuler tant que l'annonce est affichée.
            b.copy(annonces = b.annonces.toMutableList().also { it[index] = annonce.copy(resultat = annonce.resultat.filterNot { l -> l.startsWith("Ajusté par le MJ") } + lignes) })
        }.let { it.copy(journal = it.journal + lignes) }
    }

    /**
     * Le MJ choisit de ne pas appliquer une action de repaire ou légendaire encore affichée
     * (annonce non lue) : les combattants touchés retrouvent leurs PV et leurs états d'avant.
     */
    fun annulerAnnonce(bossId: String, index: Int) = modifier { e ->
        val annonce = e.boss[bossId]?.annonces?.getOrNull(index)?.takeIf { !it.lue && !it.annulee && it.avant.isNotEmpty() } ?: return@modifier e
        var courant = e
        annonce.avant.forEach { avant ->
            val actuel = courant.combattants.firstOrNull { it.id == avant.id } ?: return@forEach
            // États remis aussi sur la fiche, puis PV et le reste tels qu'avant l'action.
            if (actuel.conditions != avant.conditions) courant = changerConditions(courant, actuel, avant.conditions.toSet())
            courant = remplacer(courant, avant)
            avant.characterId?.let { GameState.updateCharacterHp(it, avant.pv) }
        }
        majBoss(courant, bossId) { b ->
            b.copy(annonces = b.annonces.toMutableList().also { it[index] = annonce.copy(annulee = true, resultat = listOf("Non appliqué par le MJ : PV et états rétablis.")) })
        }.let { it.copy(journal = it.journal + "↩ ${annonce.texte.substringBefore(" :")} : non appliqué par le MJ (${annonce.avant.joinToString { c -> c.nom }})") }
    }

    /**
     * Adversaires visés par une action de boss : tous, ceux au contact, ou [ActionBoss.nbCibles]
     * à portée (n'importe où pour le repaire), [prefereeId] en premier s'il est à portée.
     */
    private fun ciblesActionBoss(e: CombatEnCours, boss: Combattant, action: ActionBoss, prefereeId: String?, repaire: Boolean): List<Combattant> {
        val adversaires = e.combattants.filter { it.estMonstre != boss.estMonstre && !it.horsCombat && !it.inerte }
        fun aPortee(c: Combattant) = repaire || e.distance(boss.id, c.id).metres <= action.portee + 0.01
        return when (action.forme) {
            FormeActionBoss.SAUVEGARDE_TOUS -> adversaires
            FormeActionBoss.SAUVEGARDE_CONTACT -> adversaires.filter { e.distance(boss.id, it.id).metres <= action.portee + 0.01 }
            FormeActionBoss.ATTAQUE, FormeActionBoss.SAUVEGARDE_CIBLES -> {
                val candidats = adversaires.filter(::aPortee)
                val preferee = candidats.firstOrNull { it.id == prefereeId }
                (listOfNotNull(preferee) + candidats.filter { it.id != prefereeId }.shuffled(aleatoire)).take(action.nbCibles.coerceAtLeast(1))
            }
        }
    }

    /** Résout une action de boss (légendaire ou de repaire) : jets d'attaque ou de sauvegarde, dégâts, états, repli. */
    private fun executerActionBoss(e: CombatEnCours, bossId: String, action: ActionBoss, cibles: List<Combattant>, source: String): CombatEnCours {
        val boss = e.combattants.firstOrNull { it.id == bossId } ?: return e
        var courant = e
        if (action.forme == FormeActionBoss.ATTAQUE) {
            val attaque = AttaqueMonstre(
                action.nom, TypeAttaqueMonstre.CORPS_A_CORPS, bonusToucher = action.bonusToucher ?: 0,
                formuleDegats = action.degats, typeDegats = action.typeDegats,
            )
            cibles.forEach { cible ->
                val c = courant.combattants.firstOrNull { it.id == cible.id && !it.horsCombat } ?: return@forEach
                val auContact = courant.distance(bossId, c.id) == Distance.CONTACT
                val (mode, _) = Etats.modeAttaque(boss.conditions, c.conditions, auContact)
                val r = lancerAttaqueMonstre(attaque, 1, c.ca, aleatoire, mode, Etats.critiqueAuContact(c.conditions, auContact)).first()
                val (type, texte) = Narrateur.attaque(boss.nom, c.nom, action.nom, r.touche, r.critique, r.d20 == 1, aDistance = false, aleatoire = aleatoire)
                courant = courant.copy(
                    journal = courant.journal + ("$source → ${c.nom} : " +
                        if (r.touche) "${r.totalToucher} touché${if (r.critique) " CRITIQUE" else ""} (${r.degats})" else "${r.totalToucher} raté")
                ).raconter(type, texte, acteurId = bossId, cibleId = c.id, table = Narrateur.tableAttaque(boss.nom, c.nom, r.touche, r.critique, r.degats))
                if (r.touche) {
                    if (r.degats > 0) courant = pv(courant, c.id, r.degats, bossId, action.typeDegats, tableDite = true)
                    action.etats.forEach { courant = ajouterCondition(courant, c.id, it, source) }
                }
            }
        } else {
            val degats = action.degats?.let { lancerFormuleDes(it, aleatoire) } ?: 0
            cibles.forEach { cible ->
                if (courant.combattants.firstOrNull { it.id == cible.id }?.horsCombat != false) return@forEach
                courant = subirAvecSauvegarde(
                    courant, cible.id, action.sauvegarde, action.dd ?: 10, degats, action.moitieSiReussi, action.etats, source,
                    auteurId = bossId, typeDegats = action.typeDegats,
                )
            }
        }
        if (action.repli) {
            val auContact = courant.combattants.filter { it.estMonstre != boss.estMonstre && courant.distance(bossId, it.id) == Distance.CONTACT }
            val recul = Terrain.recul(boss)
            if (auContact.isNotEmpty() && recul != Terrain.zoneDe(boss)) {
                courant = remplacer(courant, courant.combattants.first { it.id == bossId }.copy(zone = recul)).let { e ->
                    e.copy(journal = e.journal + "${boss.nom} recule vers ${Terrain.nom(recul)}, loin de ${auContact.joinToString { it.nom }} (sans attaque d'opportunité)")
                }.raconter(TypeRecit.DEPLACEMENT, "${boss.nom} bondit en arrière, hors de portée de ${auContact.joinToString { it.nom }}.", acteurId = bossId)
            }
        }
        return courant
    }

    // ── Déroulé des rounds ──

    private fun peutAgir(c: Combattant) = !c.inerte && !(c.estMonstre && c.horsCombat) && !c.mort

    /**
     * Applique le déplacement annoncé par [combattantId] (au moment où son action se résout, sinon
     * à la fin de son tour). Quitter le contact d'un adversaire sans se désengager lui ouvre une
     * attaque d'opportunité : la table en est avertie.
     */
    private fun appliquerDeplacements(etat: CombatEnCours, combattantId: String): CombatEnCours {
        val acteur = etat.combattants.firstOrNull { it.id == combattantId } ?: return etat
        val declaration = etat.declarations[acteur.id]?.takeIf { it.deplacements.isNotEmpty() || it.zoneVisee != null } ?: return etat
        // Déplacement consommé : revenir en arrière puis repasser ne le rejoue pas.
        val consomme = etat.copy(declarations = etat.declarations + (acteur.id to declaration.copy(deplacements = emptyMap(), zoneVisee = null)))
        // Zone visée : celle annoncée, sinon celle qui donne la distance voulue à la cible (au
        // contact d'abord) — c'est ainsi que l'IA exprime ses déplacements.
        val visee = declaration.zoneVisee ?: run {
            val (autreId, distance) = declaration.deplacements.entries.firstOrNull { it.value == Distance.CONTACT }?.toPair()
                ?: declaration.deplacements.entries.first().toPair()
            val autre = etat.combattants.firstOrNull { it.id == autreId } ?: return consomme
            Terrain.zonePour(acteur, autre, distance)
        }
        // Déplacement limité à la vitesse du combattant (doublée s'il se précipite).
        val precipite = declaration.actionId == "dash" || declaration.libelle.contains("précipit", ignoreCase = true)
        val maxMetres = acteur.vitesse * if (precipite) 2 else 1
        val depart = Terrain.zoneDe(acteur)
        val arrivee = Terrain.versZone(depart, visee, Terrain.pasMax(maxMetres))
        if (arrivee == depart) return consomme
        val quittes = etat.combattants.filter { it.estMonstre != acteur.estMonstre && !it.horsCombat && !it.inerte && Terrain.zoneDe(it) == depart }
        val rejoints = etat.combattants.filter { it.estMonstre != acteur.estMonstre && !it.horsCombat && !it.inerte && Terrain.zoneDe(it) == arrivee }
        var courant = remplacer(consomme, acteur.copy(zone = arrivee))
        val lignes = mutableListOf("${acteur.nom} → ${Terrain.nom(arrivee)}" +
            if (arrivee != visee) " (déplacement limité à ${formatMetres(maxMetres)}, visait ${Terrain.nom(visee)})" else "")
        courant = courant.raconter(
            TypeRecit.DEPLACEMENT,
            Narrateur.deplacementZone(acteur.nom, Terrain.phrase(arrivee), rejoints.map { it.nom }),
            acteurId = acteur.id, cibleId = rejoints.firstOrNull()?.id,
            table = "${acteur.nom} → ${Terrain.nom(arrivee)}" + rejoints.takeIf { it.isNotEmpty() }?.joinToString(prefix = ", au contact de ") { it.nom }.orEmpty(),
        )
        if (!declaration.desengage) quittes.forEach { autre ->
            lignes += "⚠ Attaque d'opportunité : ${autre.nom} peut frapper ${acteur.nom} qui quitte son contact"
            courant = courant.raconter(TypeRecit.INFO, Narrateur.opportunite(acteur.nom, autre.nom), acteurId = autre.id, cibleId = acteur.id)
        }
        return courant.copy(journal = courant.journal + lignes)
    }

    /** Placement changé par le MJ : l'IA qui avait déjà décidé (rien d'appliqué) revoit sa décision. */
    private fun redeciderApresPlacement(etat: CombatEnCours, concernes: Set<String>): CombatEnCours {
        if (etat.phase == PhaseCombat.PREPARATION) return etat
        var courant = etat
        etat.combattants
            .filter { c -> c.id in concernes && c.piloteParIa && !c.horsCombat && etat.declarations[c.id]?.let { it.parIa && !it.degatsAppliques } == true }
            .forEach { m -> courant = redecider(courant, courant.combattants.first { it.id == m.id }) }
        return courant
    }

    private fun nouveauRound(fin: CombatEnCours): CombatEnCours {
        // Fin du round : le repaire de chaque boss agit, puis ses compteurs du round repartent de zéro.
        val etat = fin.boss.keys.fold(fin) { e, id -> actionRepaireAuto(e, id) }.let { e ->
            e.copy(boss = e.boss.mapValues { (_, b) -> b.copy(blessesDuRound = emptySet(), repaireRatesDuRound = emptySet()) })
        }
        val round = etat.round + 1
        // Le round qui s'achève a duré 6 secondes de fiction (effets à durée, horloge).
        ScenarioClockState.advanceSeconds(ScenarioClockState.SECONDES_PAR_ROUND)
        // Recharge des capacités (d6 ≥ seuil) au début de chaque round.
        val journalRecharge = mutableListOf<String>()
        val combattants = etat.combattants.map { c ->
            val rechargees = c.attaques.filter { a -> a.recharge != null && (c.capacitesUtilisees[a.nom] ?: 0) > 0 }
                .filter { a -> aleatoire.nextInt(1, 7) >= a.recharge!! }
            if (rechargees.isEmpty()) c
            else {
                journalRecharge += "${c.nom} récupère ${rechargees.joinToString { it.nom }}"
                c.copy(capacitesUtilisees = c.capacitesUtilisees - rechargees.map { it.nom }.toSet())
            }
        }
        val suivant = etat.copy(
            combattants = combattants,
            round = round,
            tourIndex = 0,
            journal = etat.journal + journalRecharge + "Round $round"
        )
        return ouvrirRound(suivant.raconter(TypeRecit.ROUND, Narrateur.round(round), table = "Round $round"))
    }

    /**
     * Ouvre un round : actions du round précédent effacées. Un boss qui choisit son attaque annoncée
     * (souffle…) la prépare dès maintenant, lue à la table avant son tour ; puis le premier dans
     * l'ordre prend la main (le repaire, lui, agit à la fin du round : voir [nouveauRound]).
     */
    private fun ouvrirRound(etat: CombatEnCours): CombatEnCours {
        var courant = etat.copy(phase = PhaseCombat.EN_COURS, declarations = emptyMap(), relances = emptyMap())
        val premierId = courant.combattants.firstOrNull(::peutAgir)?.id
        courant.boss.forEach { (id, b) ->
            val annoncee = b.definition.attaqueAnnoncee ?: return@forEach
            // Premier à jouer : personne ne pourrait l'interrompre, l'annoncer n'aurait pas de sens.
            if (id == premierId) return@forEach
            val boss = courant.combattants.firstOrNull { it.id == id } ?: return@forEach
            if (!boss.piloteParIa || boss.horsCombat || !Etats.peutAgir(boss.conditions)) return@forEach
            val decision = decisionIa(boss, courant)
            if (decision.attaque?.nom == annoncee.attaqueNom) courant = appliquerDecisionIa(courant, boss, decision, anticipee = true)
        }
        val premier = courant.combattants.indexOfFirst(::peutAgir).coerceAtLeast(0)
        return debutDeTour(courant.copy(tourIndex = premier))
    }

    /**
     * Nouvelle décision pour [monstre] : la capacité réservée par l'ancienne redevient disponible.
     * Un combattant dont ce n'est pas le tour (et sans décision prise d'avance) décidera à son tour.
     */
    private fun redecider(etat: CombatEnCours, monstre: Combattant): CombatEnCours {
        val existante = etat.declarations[monstre.id]
        if (existante?.degatsAppliques == true) return etat
        if (existante == null && etat.actif?.id != monstre.id) return etat
        val restaure = liberer(monstre, existante)
        return appliquerDecisionIa(remplacer(etat, restaure), restaure)
    }

    /** Fait décider l'IA et réserve aussitôt la capacité à usage limité choisie. */
    private fun appliquerDecisionIa(
        etat: CombatEnCours,
        monstre: Combattant,
        decision: DeclarationAction = decisionIa(monstre, etat),
        anticipee: Boolean = false,
    ): CombatEnCours {
        val maj = annonceAttaqueBoss(etat.copy(declarations = etat.declarations + (monstre.id to decision)), monstre.id, decision, anticipee)
        val a = decision.attaque?.takeIf { it.estSpeciale } ?: return maj
        return remplacer(maj, monstre.copy(capacitesUtilisees = monstre.capacitesUtilisees + (a.nom to ((monstre.capacitesUtilisees[a.nom] ?: 0) + 1))))
    }

    private fun decisionIa(monstre: Combattant, etat: CombatEnCours): DeclarationAction {
        val decision = IaMonstre.decider(
            monstre = monstre,
            adversaires = etat.combattants.filter { it.estMonstre != monstre.estMonstre },
            distances = etat.distancesDe(monstre.id),
            aleatoire = aleatoire,
            allies = etat.combattants.filter { it.estMonstre == monstre.estMonstre && it.id != monstre.id },
            distanceEntre = etat::distance,
            round = etat.round,
        )
        return avecJets(etat, DeclarationAction(
            deplacements = decision.deplacements,
            desengage = decision.desengage,
            combattantId = monstre.id,
            libelle = decision.libelle,
            cibleId = decision.cibleId,
            cibleNom = decision.cibleNom,
            detail = decision.raison,
            parIa = true,
            attaque = decision.attaque,
            nbAttaques = decision.nbAttaques,
        ))
    }

    /**
     * Lance d'avance le toucher (contre la CA de la cible) et les dégâts d'une action avec
     * attaque, s'ils ne le sont pas déjà. Sans attaque, efface les jets éventuels.
     */
    private fun avecJets(etat: CombatEnCours, d: DeclarationAction): DeclarationAction {
        val attaque = d.attaque ?: return d.copy(resultats = null)
        if (d.resultats != null) return d
        val ca = if (attaque.zone) null else etat.combattants.firstOrNull { it.id == d.cibleId }?.ca
        // États de l'attaquant et de la cible (distance après le déplacement annoncé).
        val avecDeplacement = etat.copy(declarations = etat.declarations + (d.combattantId to d))
        val mode = modeAttaque(avecDeplacement, d.combattantId, d.cibleId, attaque)
        return d.copy(
            resultats = lancerAttaqueMonstre(attaque, d.nbAttaques, ca, aleatoire, mode.mode, mode.critiqueAuto),
            degatsAppliques = false,
        )
    }

    private fun liberer(monstre: Combattant, declaration: DeclarationAction?): Combattant {
        val a = declaration?.attaque?.takeIf { it.estSpeciale } ?: return monstre
        val n = (monstre.capacitesUtilisees[a.nom] ?: 0) - 1
        return monstre.copy(capacitesUtilisees = if (n <= 0) monstre.capacitesUtilisees - a.nom else monstre.capacitesUtilisees + (a.nom to n))
    }

    private fun remplacer(etat: CombatEnCours, c: Combattant) =
        etat.copy(combattants = etat.combattants.map { if (it.id == c.id) c else it })

    /** d20 + bonus, avec l'Avantage pour un personnage qui l'a à l'Initiative (Assassin, Champion, Barbare 7). */
    fun jetInitiative(c: Combattant): Int {
        val premier = aleatoire.nextInt(1, 21)
        val avantage = fiche(c)?.let { avantageInitiative(it) } == true
        return (if (avantage) maxOf(premier, aleatoire.nextInt(1, 21)) else premier) + c.bonusInitiative
    }

    /** Avantage à l'Initiative : Assassin (Assassinat), Champion (Athlète accompli), Barbare niv. 7 (Instinct sauvage). */
    fun avantageInitiative(f: Character): Boolean =
        Roublard.niveauAssassin(f) >= 3 || Guerrier.niveauChampion(f) >= 3 ||
            (ArsenalPersonnage.niveauxParClasse(f).entries.firstOrNull { it.key.equals("Barbare", ignoreCase = true) }?.value ?: 0) >= 7

    /** Tri par initiative décroissante, puis bonus (départage), puis PJ avant monstres. */
    private fun trier(liste: List<Combattant>): List<Combattant> =
        liste.sortedWith(
            compareByDescending<Combattant> { it.initiative ?: Int.MIN_VALUE }
                .thenByDescending { it.bonusInitiative }
                .thenBy { it.estMonstre }
        )

    private inline fun modifier(crossinline transformation: (CombatEnCours) -> CombatEnCours) {
        _etat.update { courant -> courant?.let(transformation)?.let(::suiviDesBoss)?.let(::verifierVictoire) }
    }

    /** Dernier ennemi tombé : le combat est marqué terminé (l'écran de combat distribue le butin et le ferme). */
    private fun verifierVictoire(etat: CombatEnCours): CombatEnCours =
        if (!etat.termine && etat.demarre && etat.victoire) {
            etat.copy(termine = true, journal = etat.journal + "Tous les ennemis sont vaincus : fin du combat")
                .raconter(TypeRecit.VICTOIRE, Narrateur.victoire(), table = "Victoire !")
        } else etat
}

/** Ligne du récit en préparation : version MJ, bulle animée, version table (null = MJ seul). */
private data class LigneRecit(val type: TypeRecit, val texte: String, val montant: Int?, val table: String? = null)

/** Résultat annoncé à la table pour un personnage joué sans l'appli (voir CombatSession.resultatTable). */
enum class ResultatTable { TOUCHE, RATE, SOIN }
