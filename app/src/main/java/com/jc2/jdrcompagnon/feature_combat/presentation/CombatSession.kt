package com.jc2.jdrcompagnon.feature_combat.presentation

import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.FormeZone
import com.jc2.jdrcompagnon.feature_combat.domain.model.ZoneEffet
import com.jc2.jdrcompagnon.feature_combat.domain.model.formatMetres
import com.jc2.jdrcompagnon.feature_combat.domain.model.vitesseEnMetres
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.IaMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.ModeJet
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.RegleProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.ResultatAttaque
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
 * Déroulé d'un round : chacun DÉCLARE d'abord son action (joueurs sur leur appareil, monstres par
 * l'IA, PNJ par le MJ), puis le MJ RÉSOUT combattant par combattant dans l'ordre d'initiative —
 * une action déclarée peut alors devenir impossible (cible tombée, chemin bloqué…).
 */
enum class PhaseCombat(val label: String) {
    PREPARATION("Préparation"),
    DECLARATION("Déclaration des actions"),
    RESOLUTION("Résolution")
}

/**
 * Action annoncée pour un combattant pendant la phase de déclaration. [actionId] renvoie au
 * catalogue ActionsCombat (null = action libre ou décision de l'IA d'un monstre).
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
    // Distance à chaque adversaire concerné après le déplacement (appliquée à la résolution).
    val deplacements: Map<String, Distance> = emptyMap(),
    // Quitte le contact sans provoquer d'attaque d'opportunité (action Se désengager).
    val desengage: Boolean = false,
    // Monstres et PNJ : toucher et dégâts déjà lancés dès la déclaration (contre la CA de la
    // cible), pour que le MJ n'ait plus qu'à appliquer pendant la résolution.
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
)

/**
 * Combat en cours côté MJ. [combattants] est trié par ordre d'initiative une fois celle-ci
 * lancée ; [tourIndex] désigne le combattant en cours de résolution dans cette liste.
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
) {
    /**
     * Distance entre deux combattants. Non placés : 9 m entre adversaires, 3 m entre membres d'un
     * même camp (groupés), pour que les zones d'effet aient une base réaliste.
     */
    fun distance(a: String, b: String): Distance = distances[cleDistance(a, b)] ?: run {
        val ca = combattants.firstOrNull { it.id == a }
        val cb = combattants.firstOrNull { it.id == b }
        if (ca != null && cb != null && ca.estMonstre == cb.estMonstre) Distance.ALLIES_PAR_DEFAUT else Distance.COURTE
    }

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

    /** Tant que round == 0, on est en préparation (initiatives à lancer/saisir). */
    val demarre: Boolean get() = round > 0
    val actif: Combattant? get() = if (phase == PhaseCombat.RESOLUTION) combattants.getOrNull(tourIndex) else null
    val monstresRestants: Int get() = combattants.count { it.estMonstre && !it.horsCombat }

    /** Combattants qui doivent déclarer une action ce round (les monstres vaincus en sont exclus). */
    val attendus: List<Combattant> get() = combattants.filter { !(it.estMonstre && it.horsCombat) }
    val enAttente: List<Combattant> get() = attendus.filter { it.id !in declarations }
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

    fun demarrer(titre: String, combattants: List<Combattant>) {
        // Temps de combat : l'horloge de scénario s'arrête, chaque round l'avancera de 6 s.
        ScenarioClockState.setPauseCombat(true)
        _etat.value = CombatEnCours(
            id = UUID.randomUUID().toString(),
            titre = titre,
            // Initiative lancée d'office pour tout le monde : le MJ corrige ensuite à la main la
            // valeur d'un joueur qui a lancé son propre dé.
            combattants = trier(combattants.map { it.copy(initiative = it.initiative ?: jetInitiative(it)) }),
            journal = listOf("Combat préparé : ${combattants.size} combattant(s)")
        )
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
     * Crée un combattant à partir d'une fiche de personnage (PJ ou PNJ) de GameState. Un PNJ
     * [ennemi] rejoint le camp des monstres (piloté par l'IA, PV cachés aux joueurs).
     */
    fun combattantDepuisPersonnage(character: Character, ennemi: Boolean = false): Combattant = Combattant(
        id = UUID.randomUUID().toString(),
        nom = character.name,
        estMonstre = ennemi,
        characterId = character.id,
        ca = character.armorClass,
        pvMax = character.maxHitPoints,
        pv = character.currentHitPoints,
        pvTemporaires = character.temporaryHitPoints,
        bonusInitiative = character.initiativeBonus,
        // États du champ condition de la fiche (Paralysé, Empoisonné…), suivis ensuite dans les deux sens.
        conditions = Etats.lire(character.condition),
        vitesse = vitesseEnMetres(character.speed),
    )

    fun ajouter(combattants: List<Combattant>) = modifier { etat ->
        val dejaPresents = etat.combattants.mapNotNull { it.characterId }.toSet()
        val nouveaux = combattants.filter { it.characterId == null || it.characterId !in dejaPresents }
        if (nouveaux.isEmpty()) return@modifier etat
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
        // Un monstre qui arrive pendant la déclaration décide tout de suite de son action.
        if (maj.phase == PhaseCombat.DECLARATION) declarerAutomatiquement(maj) else maj
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
        verifierDeclarations(
            etat.copy(
                combattants = liste,
                tourIndex = tour,
                declarations = etat.declarations - combattantId,
                journal = etat.journal + "${retire.nom} quitte le combat"
            )
        )
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
     * Démarre le round 1 par la phase de déclaration ; les combattants encore sans initiative la
     * lancent automatiquement.
     */
    fun commencer() = modifier { etat ->
        val avecInit = etat.combattants.map { c -> if (c.initiative != null) c else c.copy(initiative = jetInitiative(c)) }
        val liste = trier(avecInit)
        ouvrirDeclarations(
            etat.copy(
                combattants = liste,
                round = 1,
                tourIndex = 0,
                journal = etat.journal + "Round 1 — ordre : ${liste.joinToString { "${it.nom} (${it.initiative})" }}"
            )
        )
    }

    /**
     * Enregistre (ou remplace) l'action d'un combattant pendant la déclaration. Dès que tout le
     * monde a déclaré, le combat passe seul en résolution.
     */
    fun declarer(declaration: DeclarationAction) = modifier { etat ->
        if (etat.phase != PhaseCombat.DECLARATION) return@modifier etat
        val c = etat.combattants.firstOrNull { it.id == declaration.combattantId } ?: return@modifier etat
        // Neutralisé (Paralysé, Étourdi…) : l'« Aucune action » imposée reste, quoi qu'envoie le joueur.
        if (!Etats.peutAgir(c.conditions) && !c.horsCombat) return@modifier etat
        verifierDeclarations(etat.copy(declarations = etat.declarations + (c.id to avecJets(etat, sansDeplacementSiImmobile(c, declaration)))))
    }

    /** Le MJ passe à la résolution sans attendre les retardataires (joueur absent, indécis…). */
    fun passerEnResolution() = modifier { etat ->
        if (etat.phase != PhaseCombat.DECLARATION) etat else versResolution(etat)
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
        val c = etat.combattants.firstOrNull { it.id == combattantId }?.copy(profilIA = profil) ?: return@modifier etat
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
        if (nettoye.phase == PhaseCombat.DECLARATION && nettoye.declarations[c.id]?.parIa != false) {
            val sans = nettoye.copy(declarations = nettoye.declarations - c.id)
            val cible = sans.combattants.first { it.id == c.id }
            if (cible.piloteParIa && !cible.horsCombat) verifierDeclarations(appliquerDecisionIa(sans, cible)) else sans
        } else nettoye
    }

    /**
     * Le MJ remplace l'action déclarée d'un combattant pendant la résolution (action devenue
     * impossible, cible changée…). Hors résolution, passer par [declarer].
     */
    fun modifierDeclaration(declaration: DeclarationAction) = modifier { etat ->
        if (etat.phase == PhaseCombat.PREPARATION) return@modifier etat
        val c = etat.combattants.firstOrNull { it.id == declaration.combattantId } ?: return@modifier etat
        val ancienne = etat.declarations[c.id]
        val maj = remplacer(etat, liberer(c, ancienne))
        verifierDeclarations(
            maj.copy(
                declarations = maj.declarations + (c.id to avecJets(maj, sansDeplacementSiImmobile(c, declaration))),
                journal = if (etat.phase == PhaseCombat.RESOLUTION) maj.journal + "${c.nom} change d'action : ${declaration.libelle}" else maj.journal
            )
        )
    }

    /**
     * Le MJ place un monstre par rapport à un personnage (au contact, courte, longue). Pendant la
     * déclaration, les monstres concernés dont l'IA avait décidé revoient leur décision.
     */
    fun definirDistance(aId: String, bId: String, distance: Distance) = modifier { etat ->
        redeciderApresPlacement(etat.copy(distances = etat.distances + (cleDistance(aId, bId) to distance)), setOf(aId, bId))
    }

    /** Place un combattant à la même distance de tous ses adversaires. */
    fun definirDistancesMonstre(monstreId: String, distance: Distance) = modifier { etat ->
        val soi = etat.combattants.firstOrNull { it.id == monstreId } ?: return@modifier etat
        val autres = etat.combattants.filter { it.estMonstre != soi.estMonstre }
        redeciderApresPlacement(
            etat.copy(distances = etat.distances + autres.associate { cleDistance(monstreId, it.id) to distance }),
            setOf(monstreId)
        )
    }

    /**
     * L'action du combattant s'avère impossible pendant la résolution (coincé, cible hors
     * d'atteinte…) : elle est annulée, son déplacement aussi.
     */
    fun annulerAction(combattantId: String, raison: String? = null) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        val ancienne = etat.declarations[combattantId]
        val maj = remplacer(etat, liberer(c, ancienne))
        val libelle = "Action impossible" + (ancienne?.let { " (${it.libelle})" } ?: "")
        maj.copy(
            declarations = maj.declarations + (combattantId to DeclarationAction(combattantId, libelle, detail = raison, parMj = true)),
            journal = maj.journal + "${c.nom} : $libelle" + (raison?.let { " — $it" } ?: "")
        )
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
     * seule fois, et note le détail au journal.
     */
    fun appliquerJets(combattantId: String) {
        val etat = _etat.value ?: return
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return
        val d = etat.declarations[combattantId] ?: return
        val resultats = d.resultats ?: return
        if (d.degatsAppliques) return
        val cible = etat.combattants.firstOrNull { it.id == d.cibleId } ?: return
        val total = resultats.sumOf { it.degats }
        if (total > 0) appliquerPv(cible.id, total)
        modifier { e ->
            e.copy(
                declarations = e.declarations + (combattantId to d.copy(degatsAppliques = true)),
                journal = e.journal + "${c.nom} — ${d.libelle} sur ${cible.nom} : " +
                    resultats.joinToString { r ->
                        val mode = if (r.mode != ModeJet.NORMAL) " avec ${r.mode.label}" else ""
                        if (r.touche) "${r.totalToucher ?: "auto"}$mode touché${if (r.critique) " CRITIQUE" else ""} (${r.degats})" else "${r.totalToucher}$mode raté"
                    }
            )
        }
        // États de l'attaque (« subit l'état Agrippé ») : d'office si elle a touché ; ceux qui
        // demandent un JS sont rappelés au MJ, qui les applique depuis la carte du combattant.
        val attaque = d.attaque ?: return
        if (resultats.none { it.touche } || cible.horsCombat) return
        attaque.conditions.filter { !it.surEchecJs }.forEach { inflige ->
            appliquerCondition(cible.id, inflige.condition, "${c.nom} — ${attaque.nom}" + (inflige.evasionDd?.let { " (évasion DD $it)" } ?: ""))
        }
        attaque.conditions.filter { it.surEchecJs }.takeIf { it.isNotEmpty() }?.let { aTrancher ->
            noter("⚠ ${cible.nom} : ${aTrancher.joinToString { "JS ${it.sauvegarde ?: ""} DD ${it.dd} ou ${it.condition.label}".replace("  ", " ") }} (${attaque.nom})")
        }
    }

    /** Ajoute un état à un combattant (attaque de monstre, bouton du MJ), noté au journal. */
    fun appliquerCondition(combattantId: String, condition: ConditionCombat, source: String? = null) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        if (condition in c.conditions) return@modifier etat
        val maj = changerConditions(etat, c, c.conditions + condition)
        maj.copy(journal = maj.journal + "${c.nom} subit l'état ${condition.label}" + (source?.let { " — $it" } ?: ""))
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
        return ModeAttaque(mode, raisons, Etats.critiqueAuContact(cible.conditions, auContact))
    }

    /** Enregistre un jet de dés envoyé par un joueur pour son combattant. */
    fun enregistrerJet(jet: JetCombat) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == jet.combattantId } ?: return@modifier etat
        // Un seul jet par type (Attaque, Dégâts…) et par round — autant de jets « Attaque » que
        // d'attaques déclarées (Attaque supplémentaire) — sauf jet supplémentaire accordé par le
        // MJ : empêche de relancer jusqu'à obtenir un bon résultat.
        val autorises = if (jet.libelle == "Attaque") etat.declarations[c.id]?.attaquesJoueur?.size?.coerceAtLeast(1) ?: 1 else 1
        val dejaLance = etat.jetsDuRound.count { it.combattantId == c.id && it.libelle == jet.libelle } >= autorises
        val relances = etat.relances[c.id] ?: 0
        if (dejaLance && relances <= 0) return@modifier etat
        val detail = if (jet.manuel) "dés physiques" else "dés de l'appli"
        etat.copy(
            jets = (etat.jets + jet.copy(round = etat.round)).takeLast(200),
            relances = if (dejaLance) etat.relances + (c.id to relances - 1) else etat.relances,
            journal = etat.journal + "${c.nom} — ${jet.libelle} : ${jet.total} (${jet.formule}, $detail)" +
                (jet.totalDegats?.let { " · dégâts $it (${jet.formuleDegats})" } ?: "")
        )
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
        modifier { e -> e.copy(jets = e.jets.map { if (it.id == jetId) it.copy(applique = true) else it }) }
        appliquerPv(cible.id, if (soin) -montant else montant)
        noter("$auteur → ${cible.nom} : $montant ${if (soin) "PV soignés" else "dégâts"} (${jet.libelle})")
    }

    /** Jet de joueur entièrement traité par le MJ (dégâts de zone appliqués cible par cible). */
    fun marquerJetTraite(jetId: String) = modifier { e ->
        e.copy(jets = e.jets.map { if (it.id == jetId) it.copy(applique = true) else it })
    }

    /** Le MJ écarte les dégâts d'un jet de joueur (attaque ratée, action refusée…). */
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
     * Combattant suivant pendant la résolution (les monstres à 0 PV sont sautés). Après le
     * dernier, un nouveau round commence par sa phase de déclaration.
     */
    fun tourSuivant() = modifier { avant ->
        if (avant.phase != PhaseCombat.RESOLUTION || avant.combattants.isEmpty()) return@modifier avant
        // Le combattant qui vient d'agir a effectué son déplacement.
        val etat = appliquerDeplacements(avant)
        var index = etat.tourIndex + 1
        while (index < etat.combattants.size) {
            if (peutAgir(etat.combattants[index])) return@modifier etat.copy(tourIndex = index)
            index++
        }
        nouveauRound(etat)
    }

    fun tourPrecedent() = modifier { etat ->
        if (etat.phase != PhaseCombat.RESOLUTION) return@modifier etat
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
    fun appliquerPv(combattantId: String, degatsBruts: Int) = modifier { etat ->
        var ligne: String? = null
        val liste = etat.combattants.map { c ->
            if (c.id != combattantId) return@map c
            // Pétrifié : résistance à tous les dégâts (moitié, arrondie à l'inférieur).
            val resistant = degatsBruts > 0 && Etats.resistanceTousDegats(c.conditions)
            val degats = if (resistant) degatsBruts / 2 else degatsBruts
            val maj = if (degats >= 0) {
                val absorbe = minOf(c.pvTemporaires, degats)
                c.copy(pvTemporaires = c.pvTemporaires - absorbe, pv = (c.pv - (degats - absorbe)).coerceAtLeast(0))
            } else {
                c.copy(pv = (c.pv - degats).coerceAtMost(c.pvMax))
            }
            ligne = if (degats >= 0) {
                "${c.nom} subit $degats dégât(s)" + (if (resistant) " (résistance : $degatsBruts ÷ 2)" else "") +
                    " → ${maj.pv}/${maj.pvMax}" + if (maj.horsCombat) " — hors de combat" else ""
            } else {
                "${c.nom} récupère ${-degats} PV → ${maj.pv}/${maj.pvMax}"
            }
            maj.characterId?.let { GameState.updateCharacterHp(it, maj.pv) }
            maj
        }
        verifierDeclarations(etat.copy(combattants = liste, journal = ligne?.let { etat.journal + it } ?: etat.journal))
    }

    fun basculerCondition(combattantId: String, condition: ConditionCombat) = modifier { etat ->
        val c = etat.combattants.firstOrNull { it.id == combattantId } ?: return@modifier etat
        changerConditions(etat, c, if (condition in c.conditions) c.conditions - condition else c.conditions + condition)
    }

    /**
     * Nouveaux états de [c] et leurs conséquences : un combattant qui devient Neutralisé perd sa
     * concentration et, pendant la déclaration, reçoit « Aucune action » à la place de son choix ;
     * redevenu capable d'agir, il peut de nouveau déclarer (l'IA redécide). Les états d'un
     * personnage sont reportés sur sa fiche ([reporterSurFiche]), mentions libres conservées.
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
        if (courant.phase != PhaseCombat.DECLARATION || maj.horsCombat || avantPeutAgir == apresPeutAgir) return courant
        val ancienne = courant.declarations[maj.id]
        courant = remplacer(courant, liberer(maj, ancienne)).let { it.copy(declarations = it.declarations - maj.id) }
        val libre = courant.combattants.first { it.id == maj.id }
        return when {
            !apresPeutAgir -> declarerAutomatiquement(courant)
            libre.piloteParIa -> verifierDeclarations(appliquerDecisionIa(courant, libre))
            else -> courant
        }
    }

    /** Vitesse 0 : le déplacement annoncé est retiré, et l'action le signale. */
    private fun sansDeplacementSiImmobile(c: Combattant, d: DeclarationAction): DeclarationAction {
        val immobilise = Etats.immobilisant(c.conditions) ?: return d
        if (d.deplacements.isEmpty()) return d
        val note = "${immobilise.label} : vitesse 0, déplacement annulé"
        return d.copy(deplacements = emptyMap(), desengage = false, detail = listOfNotNull(d.detail, note).joinToString(" — "))
    }

    // ── Déroulé des rounds ──

    private fun peutAgir(c: Combattant) = !(c.estMonstre && c.horsCombat)

    /**
     * Applique le déplacement déclaré du combattant actif. Quitter le contact d'un adversaire sans
     * se désengager lui ouvre une attaque d'opportunité : le MJ en est averti dans le journal.
     */
    private fun appliquerDeplacements(etat: CombatEnCours): CombatEnCours {
        val actif = etat.actif ?: return etat
        val declaration = etat.declarations[actif.id]?.takeIf { it.deplacements.isNotEmpty() } ?: return etat
        val lignes = mutableListOf<String>()
        var distances = etat.distances
        // Déplacement limité à la vitesse du combattant (doublée s'il se précipite).
        val precipite = declaration.actionId == "dash" || declaration.libelle.contains("précipit", ignoreCase = true)
        val maxMetres = actif.vitesse * if (precipite) 2 else 1
        declaration.deplacements.forEach { (autreId, visee) ->
            val autre = etat.combattants.firstOrNull { it.id == autreId } ?: return@forEach
            val ancienne = etat.distance(actif.id, autreId)
            val nouvelle = ancienne.versAvecDeplacement(visee, maxMetres)
            if (ancienne == nouvelle) return@forEach
            distances = distances + (cleDistance(actif.id, autreId) to nouvelle)
            lignes += "${actif.nom} → ${nouvelle.label.lowercase()} de ${autre.nom}" +
                if (nouvelle != visee) " (déplacement limité à ${formatMetres(maxMetres)}, visait ${visee.label.lowercase()})" else ""
            if (ancienne == Distance.CONTACT && !declaration.desengage && !autre.horsCombat) {
                lignes += "⚠ Attaque d'opportunité : ${autre.nom} peut frapper ${actif.nom} qui quitte son contact"
            }
        }
        return etat.copy(
            distances = distances,
            // Déplacement consommé : revenir en arrière puis repasser ne le rejoue pas.
            declarations = etat.declarations + (actif.id to declaration.copy(deplacements = emptyMap())),
            journal = etat.journal + lignes
        )
    }

    private fun redeciderApresPlacement(etat: CombatEnCours, concernes: Set<String>): CombatEnCours {
        if (etat.phase != PhaseCombat.DECLARATION) return etat
        var courant = etat
        etat.combattants
            .filter { it.id in concernes && it.piloteParIa && !it.horsCombat && etat.declarations[it.id]?.parIa == true }
            .forEach { m -> courant = redecider(courant, courant.combattants.first { it.id == m.id }) }
        return courant
    }

    private fun nouveauRound(etat: CombatEnCours): CombatEnCours {
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
        return ouvrirDeclarations(
            etat.copy(
                combattants = combattants,
                round = round,
                tourIndex = 0,
                journal = etat.journal + journalRecharge + "Round $round"
            )
        )
    }

    /** Vide les déclarations et fait décider les monstres et les personnages à terre. */
    private fun ouvrirDeclarations(etat: CombatEnCours): CombatEnCours =
        declarerAutomatiquement(etat.copy(phase = PhaseCombat.DECLARATION, declarations = emptyMap(), relances = emptyMap()))

    private fun declarerAutomatiquement(etat: CombatEnCours): CombatEnCours {
        var courant = etat
        courant.combattants.forEach { c ->
            if (c.id in courant.declarations) return@forEach
            when {
                c.piloteParIa && !c.horsCombat -> courant = appliquerDecisionIa(courant, c)
                !c.estMonstre && c.horsCombat -> courant = courant.copy(
                    declarations = courant.declarations + (c.id to DeclarationAction(c.id, "Jet de sauvegarde contre la mort", parMj = true))
                )
                // Joueur ou PNJ Neutralisé (Paralysé, Étourdi, Inconscient…) : rien à déclarer.
                !c.horsCombat && !Etats.peutAgir(c.conditions) -> courant = courant.copy(
                    declarations = courant.declarations + (c.id to DeclarationAction(
                        c.id, "Aucune action", detail = "${Etats.bloquant(c.conditions)?.label} : ne peut pas agir", parMj = true,
                    ))
                )
            }
        }
        return verifierDeclarations(courant)
    }

    /** Nouvelle décision pour [monstre] : la capacité réservée par l'ancienne redevient disponible. */
    private fun redecider(etat: CombatEnCours, monstre: Combattant): CombatEnCours {
        val restaure = liberer(monstre, etat.declarations[monstre.id])
        return verifierDeclarations(appliquerDecisionIa(remplacer(etat, restaure), restaure))
    }

    /** Fait décider l'IA et réserve aussitôt la capacité à usage limité choisie. */
    private fun appliquerDecisionIa(etat: CombatEnCours, monstre: Combattant): CombatEnCours {
        val decision = decisionIa(monstre, etat)
        val maj = etat.copy(declarations = etat.declarations + (monstre.id to decision))
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
     * Lance d'avance le toucher (contre la CA de la cible) et les dégâts d'une déclaration avec
     * attaque, s'ils ne le sont pas déjà. Sans attaque, efface les jets éventuels.
     */
    private fun avecJets(etat: CombatEnCours, d: DeclarationAction): DeclarationAction {
        val attaque = d.attaque ?: return d.copy(resultats = null)
        if (d.resultats != null) return d
        val ca = if (attaque.zone) null else etat.combattants.firstOrNull { it.id == d.cibleId }?.ca
        // États de l'attaquant et de la cible (distance après le déplacement déclaré).
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

    private fun verifierDeclarations(etat: CombatEnCours): CombatEnCours =
        if (etat.phase == PhaseCombat.DECLARATION && etat.attendus.isNotEmpty() && etat.enAttente.isEmpty()) versResolution(etat) else etat

    private fun versResolution(etat: CombatEnCours): CombatEnCours {
        val premier = etat.combattants.indexOfFirst(::peutAgir).coerceAtLeast(0)
        return etat.copy(
            phase = PhaseCombat.RESOLUTION,
            tourIndex = premier,
            journal = etat.journal + "Round ${etat.round} — résolution des actions"
        )
    }

    private fun remplacer(etat: CombatEnCours, c: Combattant) =
        etat.copy(combattants = etat.combattants.map { if (it.id == c.id) c else it })

    private fun jetInitiative(c: Combattant): Int = aleatoire.nextInt(1, 21) + c.bonusInitiative

    /** Tri par initiative décroissante, puis bonus (départage), puis PJ avant monstres. */
    private fun trier(liste: List<Combattant>): List<Combattant> =
        liste.sortedWith(
            compareByDescending<Combattant> { it.initiative ?: Int.MIN_VALUE }
                .thenByDescending { it.bonusInitiative }
                .thenBy { it.estMonstre }
        )

    private inline fun modifier(crossinline transformation: (CombatEnCours) -> CombatEnCours) {
        _etat.update { courant -> courant?.let(transformation) }
    }
}
