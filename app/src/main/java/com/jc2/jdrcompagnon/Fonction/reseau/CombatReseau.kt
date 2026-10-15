package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatBoss
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombatStore
import com.jc2.jdrcompagnon.feature_combat.presentation.VisibiliteSante
import kotlinx.serialization.Serializable

/**
 * Vue joueur du combat en cours (TYPE_COMBAT_STATE), construite pour UN joueur : il ne reçoit
 * que sa propre déclaration — les actions des autres joueurs et des monstres restent chez le MJ.
 * Les PV exacts des monstres ne sont pas transmis, seulement un état (Indemne, Blessé…).
 */
@Serializable
data class CombatJoueurData(
    val id: String,
    val titre: String,
    val round: Int,
    // Nom d'une PhaseCombat (PREPARATION / EN_COURS).
    val phase: String,
    val ordre: List<CombattantJoueurData>,
    val actifId: String? = null,
    // Combattant qui jouera après l'actif (« Tu es le prochain »).
    val prochainId: String? = null,
    // Combattant incarné par ce joueur (null s'il n'est pas dans le combat).
    val monCombattantId: String? = null,
    val maDeclaration: DeclarationJoueurData? = null,
    // Types de jets (Attaque, Dégâts…) déjà envoyés ce round, et jets supplémentaires accordés
    // par le MJ : un seul jet par type sinon.
    val typesLances: List<String> = emptyList(),
    val relances: Int = 0,
    // Nombre de jets envoyés ce round par type (plusieurs « Attaque » avec Attaque supplémentaire).
    val jetsLances: Map<String, Int> = emptyMap(),
    // Boss du combat : barre de vie par phases, actions légendaires, annonces lues à la table.
    val boss: List<BossJoueurData> = emptyList(),
    // Dernières lignes du récit (les plus anciennes d'abord), lisibles par toute la table.
    val recit: List<RecitData> = emptyList(),
)

/** Ligne du récit de combat (voir Recit) : [type] est le nom d'un TypeRecit. */
@Serializable
data class RecitData(
    val numero: Int,
    val round: Int,
    val type: String,
    val texte: String,
    val cibleId: String? = null,
    val montant: Int? = null,
)

/** Nombre de lignes du récit transmises aux joueurs et à la page de la table. */
internal const val RECIT_TRANSMIS = 40

/** Ligne du récit pour le MJ : la version racontée. */
internal fun com.jc2.jdrcompagnon.feature_combat.presentation.Recit.versData() =
    RecitData(numero, round, type.name, texte, cibleId, montant)

/**
 * Récit vu par les joueurs (appli et page de la table) : seulement les faits (« Nécronte attaque
 * Grèche : réussie, 10 dégâts »). Une ligne réservée au MJ qui porte des dégâts garde sa bulle
 * animée, sans texte.
 */
internal fun List<com.jc2.jdrcompagnon.feature_combat.presentation.Recit>.pourLaTable(): List<RecitData> =
    filter { it.table != null || it.montant != null }
        .takeLast(RECIT_TRANSMIS)
        .map { RecitData(it.numero, it.round, it.type.name, it.table.orEmpty(), it.cibleId, it.montant) }

/**
 * Ce que la table sait d'un boss : sa phase, une barre de vie approximative (au 5 % près, découpée
 * aux seuils de phase), ses ressources légendaires et les dernières annonces. Ni PV exacts, ni
 * comportement IA.
 */
@Serializable
data class BossJoueurData(
    val combattantId: String,
    val nom: String,
    val titre: String,
    val phase: Int,
    val phaseNom: String? = null,
    // Seuils (en % de PV) des phases suivantes, pour graduer la barre.
    val seuils: List<Int> = emptyList(),
    val ratioPct: Int,
    val pointsLegendaires: Int = 0,
    val pointsLegendairesMax: Int = 0,
    val resistances: Int = 0,
    val resistancesMax: Int = 0,
    // Attaque annoncée (souffle…) qui partira à son tour, sauf s'il est déséquilibré avant.
    val prepare: Boolean = false,
    val desequilibre: Boolean = false,
    // Remplissage de la jauge de déséquilibre (0–100).
    val jaugePct: Int = 0,
    val pointsFaiblesRestants: Int = 0,
    val vaincu: Boolean = false,
    val enFuite: Boolean = false,
    // Annonces, de la plus récente à la plus ancienne (« R3 · … »).
    val annonces: List<String> = emptyList(),
    // Récapitulatif (à sa chute) : dégâts par personnage, coup final.
    val degatsParAuteur: Map<String, Int> = emptyMap(),
    val coupFinal: String? = null,
    // Illustration du boss (chemin d'assets, présent dans l'appli de chaque joueur).
    val image: String? = null,
)

@Serializable
data class CombattantJoueurData(
    val id: String,
    val nom: String,
    val estMonstre: Boolean,
    val initiative: Int? = null,
    val etat: String,
    val horsCombat: Boolean,
    // PV exacts : seulement pour les personnages (les monstres n'ont que [etat]).
    val pv: Int? = null,
    val pvMax: Int? = null,
    val conditions: List<String> = emptyList(),
    // Distance entre ce combattant et le personnage du joueur (nom d'une Distance), camp adverse seulement.
    val distance: String? = null,
    // Position : « Au contact de X, Y » ou « À distance » (voir CombatEnCours.libelleContact).
    val position: String? = null,
    // Comportement IA (vue tactique du MJ seulement ; jamais envoyé aux joueurs).
    val ia: String? = null,
    // Boss, ou point faible d'un boss à détruire (objectif secondaire).
    val boss: Boolean = false,
    val objectif: Boolean = false,
    // Zone du terrain (voir Terrain) et vitesse en mètres (zones atteignables).
    val zone: Int? = null,
    val vitesse: Double? = null,
)

/** Joueur → MJ (TYPE_COMBAT_DECLARATION) : action choisie pour le round, et écho MJ → joueur. */
@Serializable
data class DeclarationJoueurData(
    val combatId: String,
    val round: Int,
    val libelle: String,
    val actionId: String? = null,
    val cibleId: String? = null,
    val cibleNom: String? = null,
    val detail: String? = null,
    // Déplacement annoncé : se placer à [distanceVisee] (nom d'une Distance) de [deplacementCibleId].
    val deplacementCibleId: String? = null,
    val distanceVisee: String? = null,
    // Action Attaquer : attaques choisies dans l'ordre (nom d'arme, « Mains nues », « Empoignade »,
    // « Bousculade ») ; plusieurs avec Attaque supplémentaire.
    val attaques: List<String> = emptyList(),
    // Sort de zone : ids de toutes les créatures visées ([cibleId] est la première).
    val ciblesZone: List<String> = emptyList(),
    // Zone du sort (ZoneEffet.versTexte), pour proposer les créatures touchées autour de la cible.
    val zone: String? = null,
    // Zone du terrain où le joueur veut finir son tour (voir Terrain) ; null = il ne bouge pas.
    val zoneVisee: Int? = null,
)

/** Joueur → MJ (TYPE_COMBAT_ROLL) : jet de dés libre (tiré par l'appli ou saisi à la main). */
@Serializable
data class JetCombatData(
    val combatId: String,
    val libelle: String,
    val formule: String,
    val des: List<Int>,
    val bonus: Int,
    val total: Int,
    val manuel: Boolean,
    // Jet d'attaque : dégâts lancés en même temps (appliqués ou non par le MJ).
    val formuleDegats: String? = null,
    val desDegats: List<Int> = emptyList(),
    val totalDegats: Int? = null,
    // Cible visée par ce jet (ex. Imposition des mains), à défaut celle de la déclaration.
    val cibleId: String? = null,
    // Arme ou sort utilisé et type de ses dégâts : résistances, immunités et bonus contre un type de créature.
    val arme: String? = null,
    val typeDegats: String? = null,
)

internal fun etatSante(pv: Int, pvMax: Int, estMonstre: Boolean): String = when {
    pv <= 0 -> if (estMonstre) "Vaincu" else "À terre"
    pvMax <= 0 || pv >= pvMax -> "Indemne"
    pv * 2 > pvMax -> "Blessé"
    pv * 4 > pvMax -> "En sang"
    else -> "Mal en point"
}

/** Ce que la table sait de la santé d'un combattant : état, PV exacts (null = cachés) et barre (%). */
internal data class SanteVisible(val etat: String, val pv: Int?, val pvMax: Int?, val ratio: Int)

/**
 * Santé de [c] vue par la table, selon les réglages du MJ (santé des monstres, PV des alliés). Le
 * joueur [moiId] voit toujours ses propres PV. Hors PV exacts, la barre suit l'état (paliers) pour
 * ne pas trahir les PV.
 */
internal fun santeVisible(c: Combattant, moiId: String? = null): SanteVisible {
    val r = ReglagesCombatStore.courant
    val ratio = if (c.pvMax > 0) c.pv.coerceAtLeast(0) * 100 / c.pvMax else 0
    val exacte = if (c.estMonstre) r.santeMonstres == VisibiliteSante.EXACTE else r.pvAllies || c.id == moiId
    if (exacte) return SanteVisible(etatSante(c.pv, c.pvMax, c.estMonstre), c.pv, c.pvMax, ratio)
    if (c.estMonstre && r.santeMonstres == VisibiliteSante.CACHEE) {
        return if (c.pv <= 0) SanteVisible("Vaincu", null, null, 0) else SanteVisible("Debout", null, null, 100)
    }
    val palier = when {
        c.pv <= 0 -> 0
        c.pvMax <= 0 || c.pv >= c.pvMax -> 100
        c.pv * 2 > c.pvMax -> 75
        c.pv * 4 > c.pvMax -> 40
        else -> 15
    }
    return SanteVisible(etatSante(c.pv, c.pvMax, c.estMonstre), null, null, palier)
}

/**
 * Vue de [this] pour le joueur qui incarne [characterId] (null = spectateur). [pourMj] : vue
 * tactique du MJ, avec le comportement IA de chacun (jamais transmis aux joueurs).
 */
fun CombatEnCours.versJoueur(characterId: String?, pourMj: Boolean = false): CombatJoueurData {
    val moi = characterId?.let { id -> combattants.firstOrNull { it.characterId == id } }
    val maDeclaration = moi?.let { declarations[it.id] }?.let { d ->
        val (cibleDeplacement, visee) = d.deplacements.entries.singleOrNull()?.let { it.key to it.value.name } ?: (null to null)
        DeclarationJoueurData(id, round, d.libelle, d.actionId, d.cibleId, d.cibleNom, d.detail?.takeIf { !d.parIa }, cibleDeplacement, visee, d.attaquesJoueur, d.ciblesZone, zoneVisee = d.zoneVisee)
    }
    return CombatJoueurData(
        id = id,
        titre = nomCombat,
        round = round,
        phase = phase.name,
        ordre = combattants.map { c ->
            val sante = santeVisible(c, moi?.id)
            CombattantJoueurData(
                id = c.id,
                nom = c.nom,
                estMonstre = c.estMonstre,
                initiative = c.initiative,
                // Vue tactique du MJ : tout ; joueurs : selon les réglages de visibilité du MJ.
                etat = if (pourMj) etatSante(c.pv, c.pvMax, c.estMonstre) else sante.etat,
                horsCombat = c.horsCombat,
                pv = if (pourMj) c.pv.takeIf { !c.estMonstre } else sante.pv,
                pvMax = if (pourMj) c.pvMax.takeIf { !c.estMonstre } else sante.pvMax,
                conditions = c.conditions.map { it.label },
                distance = moi?.takeIf { it.estMonstre != c.estMonstre }?.let { distance(it.id, c.id).name },
                position = if (c.horsCombat) null else libelleContact(c.id),
                ia = if (pourMj && c.piloteParIa) c.comportement.nom else null,
                boss = c.id in boss,
                objectif = c.inerte,
                zone = com.jc2.jdrcompagnon.feature_combat.domain.model.Terrain.zoneDe(c),
                vitesse = c.vitesse.takeIf { c.id == moi?.id },
            )
        },
        actifId = actif?.id,
        prochainId = prochain?.id,
        monCombattantId = moi?.id,
        maDeclaration = maDeclaration,
        typesLances = moi?.let { m -> jetsDuRound.filter { it.combattantId == m.id }.map { it.libelle }.distinct() }.orEmpty(),
        relances = moi?.let { relances[it.id] } ?: 0,
        jetsLances = moi?.let { m -> jetsDuRound.filter { it.combattantId == m.id }.groupingBy { it.libelle }.eachCount() }.orEmpty(),
        boss = boss.map { (id, b) -> b.versJoueur(id, this, pourMj) },
        recit = if (pourMj) recit.takeLast(RECIT_TRANSMIS).map { it.versData() } else recit.pourLaTable(),
    )
}

private fun EtatBoss.versJoueur(id: String, combat: CombatEnCours, pourMj: Boolean): BossJoueurData {
    val c = combat.combattants.firstOrNull { it.id == id }
    val ratio = c?.let { (it.pv.coerceAtLeast(0) * 100 / it.pvMax.coerceAtLeast(1)) }?.let { r -> if (r in 1..4) 5 else (r + 2) / 5 * 5 } ?: 0
    return BossJoueurData(
        combattantId = id,
        nom = c?.nom ?: definition.nom,
        titre = definition.titre,
        phase = phase,
        phaseNom = phaseCourante?.nom,
        seuils = definition.phases.drop(1).map { it.seuilPv },
        ratioPct = ratio,
        pointsLegendaires = points,
        pointsLegendairesMax = pointsMax,
        resistances = resistances,
        resistancesMax = definition.resistancesLegendaires,
        prepare = prepare,
        desequilibre = desequilibre,
        jaugePct = c?.let { (jauge * 100 / seuilDesequilibre(it.pvMax)).coerceIn(0, 100) } ?: 0,
        pointsFaiblesRestants = pointsFaiblesIds.count { pf -> combat.combattants.firstOrNull { it.id == pf }?.horsCombat == false },
        vaincu = vaincu,
        enFuite = c == null,
        // Textes de mise en scène : pour le MJ seulement (il les lit), jamais affichés aux joueurs.
        annonces = if (pourMj) annonces.reversed().map { "R${it.round} · ${it.texte}" } else emptyList(),
        degatsParAuteur = if (vaincu) degatsParAuteur else emptyMap(),
        coupFinal = coupFinal.takeIf { vaincu },
        image = com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue.imageDe(definition),
    )
}
