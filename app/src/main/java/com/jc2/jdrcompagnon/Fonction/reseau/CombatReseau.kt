package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
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
    // Nom d'une PhaseCombat (PREPARATION / DECLARATION / RESOLUTION).
    val phase: String,
    val ordre: List<CombattantJoueurData>,
    val actifId: String? = null,
    val nbDeclares: Int = 0,
    val nbAttendus: Int = 0,
    // Combattant incarné par ce joueur (null s'il n'est pas dans le combat).
    val monCombattantId: String? = null,
    val maDeclaration: DeclarationJoueurData? = null,
    // Types de jets (Attaque, Dégâts…) déjà envoyés ce round, et jets supplémentaires accordés
    // par le MJ : un seul jet par type sinon.
    val typesLances: List<String> = emptyList(),
    val relances: Int = 0,
    // Nombre de jets envoyés ce round par type (plusieurs « Attaque » avec Attaque supplémentaire).
    val jetsLances: Map<String, Int> = emptyMap(),
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
)

internal fun etatSante(pv: Int, pvMax: Int, estMonstre: Boolean): String = when {
    pv <= 0 -> if (estMonstre) "Vaincu" else "À terre"
    pvMax <= 0 || pv >= pvMax -> "Indemne"
    pv * 2 > pvMax -> "Blessé"
    pv * 4 > pvMax -> "En sang"
    else -> "Mal en point"
}

/** Vue de [this] pour le joueur qui incarne [characterId] (null = spectateur). */
fun CombatEnCours.versJoueur(characterId: String?): CombatJoueurData {
    val moi = characterId?.let { id -> combattants.firstOrNull { it.characterId == id } }
    val maDeclaration = moi?.let { declarations[it.id] }?.let { d ->
        val (cibleDeplacement, visee) = d.deplacements.entries.singleOrNull()?.let { it.key to it.value.name } ?: (null to null)
        DeclarationJoueurData(id, round, d.libelle, d.actionId, d.cibleId, d.cibleNom, d.detail?.takeIf { !d.parIa }, cibleDeplacement, visee, d.attaquesJoueur, d.ciblesZone)
    }
    return CombatJoueurData(
        id = id,
        titre = titre,
        round = round,
        phase = phase.name,
        ordre = combattants.map { c ->
            CombattantJoueurData(
                id = c.id,
                nom = c.nom,
                estMonstre = c.estMonstre,
                initiative = c.initiative,
                etat = etatSante(c.pv, c.pvMax, c.estMonstre),
                horsCombat = c.horsCombat,
                pv = if (c.estMonstre) null else c.pv,
                pvMax = if (c.estMonstre) null else c.pvMax,
                conditions = c.conditions.map { it.label },
                distance = moi?.takeIf { it.estMonstre != c.estMonstre }?.let { distance(it.id, c.id).name },
            )
        },
        actifId = actif?.id,
        nbDeclares = attendus.count { it.id in declarations },
        nbAttendus = attendus.size,
        monCombattantId = moi?.id,
        maDeclaration = maDeclaration,
        typesLances = moi?.let { m -> jetsDuRound.filter { it.combattantId == m.id }.map { it.libelle }.distinct() }.orEmpty(),
        relances = moi?.let { relances[it.id] } ?: 0,
        jetsLances = moi?.let { m -> jetsDuRound.filter { it.combattantId == m.id }.groupingBy { it.libelle }.eachCount() }.orEmpty(),
    )
}
