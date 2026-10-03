package com.jc2.jdrcompagnon.feature_combat.domain.model

import kotlin.random.Random

/**
 * Simulation d'un combat d'un personnage contre des monstres, pour l'outil Actions de combat
 * côté MJ. Indépendante du vrai combat (CombatSession) : aucune fiche n'est modifiée et un
 * combat en cours n'est pas touché. Le personnage déclare ses actions comme un joueur en partie
 * (mêmes actions, armes, sorts, objets, déplacement) ; tout est ensuite résolu automatiquement
 * — jets lancés, dégâts et soins appliqués — et les monstres jouent seuls (IaMonstre).
 */

/** Une attaque d'arme du personnage (arme équipée ou mains nues). */
data class AttaqueHeros(
    val nom: String,
    val bonusToucher: Int,
    val formuleDegats: String,
    val aDistance: Boolean,
)

/** Une frappe de l'action Attaquer : arme, ou manœuvre à mains nues (JS For/Dex du monstre contre [dd]). */
sealed interface FrappeHeros {
    data class Arme(val attaque: AttaqueHeros) : FrappeHeros
    data class Empoignade(val dd: Int) : FrappeHeros
    data class Bousculade(val dd: Int) : FrappeHeros
}

/** Sort prêt du personnage, tel que la simulation le résout. */
data class SortSimule(
    val nom: String,
    val niveau: Int,
    val jet: JetSort,
    val bonusAttaque: Int,
    val dd: Int,
    val sauvegarde: String?,
    val formuleDegats: String?,
    val soin: String?,
    // Sort mineur appuyé (Évocateur) : moitié des dégâts sur une attaque ratée.
    val demiDegatsSiEchec: Boolean = false,
    // Lancé sans emplacement (Maîtrise des sorts, Sorts de prédilection).
    val sansEmplacement: Boolean = false,
)

/**
 * Action déclarée par le personnage, comme un joueur en partie : [actionId] du catalogue
 * ActionsCombat, cible, et selon l'action ses frappes, son sort ou l'objet utilisé ; déplacement
 * du round (finir à [distanceVisee] du monstre [deplacementCibleId]).
 */
data class DeclarationHeros(
    val libelle: String,
    val actionId: String?,
    val cibleId: String? = null,
    // Sort de zone : tous les monstres pris dans la zone (chacun fait son JS).
    val ciblesZone: List<String> = emptyList(),
    val frappes: List<FrappeHeros> = emptyList(),
    val sort: SortSimule? = null,
    val niveauEmplacement: Int? = null,
    val objetNom: String? = null,
    val soinObjet: String? = null,
    val deplacementCibleId: String? = null,
    val distanceVisee: Distance? = null,
)

/**
 * Le personnage simulé : statistiques de combat, attaques (pour le jeu automatique), nombre
 * d'attaques par action Attaquer, bonus à chaque jet de sauvegarde (clé = caractéristique en
 * minuscules, ex. "dextérité") et emplacements de sort restants par niveau.
 */
data class HerosSimule(
    val nom: String,
    val ca: Int,
    val pvMax: Int,
    val pvDepart: Int = pvMax,
    val bonusInitiative: Int = 0,
    val attaques: List<AttaqueHeros>,
    val nbAttaques: Int = 1,
    val bonusSauvegardes: Map<String, Int> = emptyMap(),
    val emplacements: Map<Int, Int> = emptyMap(),
)

enum class CampLigne { HEROS, MONSTRE, INFO }

data class LigneSimulation(val texte: String, val camp: CampLigne)

enum class IssueSimulation(val label: String) {
    VICTOIRE("Victoire"),
    DEFAITE("Défaite"),
    INTERROMPU("Combat interrompu (trop long)")
}

data class EtatSimulation(
    val heros: Combattant,
    val monstres: List<Combattant>,
    val round: Int,
    // Ordre d'initiative (ids), et position du combattant qui agit.
    val ordre: List<String>,
    val tourIndex: Int,
    // Distance de chaque monstre au personnage (clé = id du monstre).
    val distances: Map<String, Distance>,
    val journal: List<LigneSimulation>,
    // Esquiver : attaques des monstres au désavantage jusqu'au prochain tour du personnage.
    val esquive: Boolean = false,
    // Se cacher : avantage à la prochaine attaque du personnage.
    val cache: Boolean = false,
    // Réaction du personnage (attaque d'opportunité), rendue à chacun de ses tours.
    val reactionDisponible: Boolean = true,
    val emplacements: Map<Int, Int> = emptyMap(),
    val issue: IssueSimulation? = null,
) {
    val monstresDebout: List<Combattant> get() = monstres.filter { !it.horsCombat }
    val tourDuHeros: Boolean get() = issue == null && ordre.getOrNull(tourIndex) == heros.id
    fun distance(monstreId: String): Distance = distances[monstreId] ?: Distance.COURTE
}

class SimulateurCombat(
    private val heros: HerosSimule,
    private val aleatoire: Random = Random,
    private val roundsMax: Int = 50,
) {
    private val herosId = "heros"

    /** Lance les initiatives, place les monstres à [distanceDepart] et joue jusqu'au tour du personnage. */
    fun demarrer(monstres: List<Combattant>, distanceDepart: Distance = Distance.COURTE): EtatSimulation {
        val combattantHeros = Combattant(
            id = herosId,
            nom = heros.nom,
            estMonstre = false,
            ca = heros.ca,
            pvMax = heros.pvMax,
            pv = heros.pvDepart.coerceIn(1, heros.pvMax),
            bonusInitiative = heros.bonusInitiative,
            initiative = d20() + heros.bonusInitiative,
        )
        val avecInit = monstres.map { it.copy(initiative = d20() + it.bonusInitiative) }
        val ordre = (avecInit + combattantHeros)
            .sortedWith(compareByDescending<Combattant> { it.initiative }.thenByDescending { it.bonusInitiative }.thenBy { it.estMonstre })
        val etat = EtatSimulation(
            heros = combattantHeros,
            monstres = avecInit,
            round = 1,
            ordre = ordre.map { it.id },
            tourIndex = 0,
            distances = avecInit.associate { it.id to distanceDepart },
            journal = listOf(
                LigneSimulation("Round 1 — initiative : ${ordre.joinToString { "${it.nom} (${it.initiative})" }}", CampLigne.INFO)
            ),
            emplacements = heros.emplacements,
        )
        return avancer(etat)
    }

    /** Résout l'action déclarée du personnage, puis fait jouer les monstres jusqu'à son prochain tour. */
    fun jouerHeros(etat: EtatSimulation, declaration: DeclarationHeros): EtatSimulation {
        if (!etat.tourDuHeros) return etat
        var courant = etat.copy(esquive = false, reactionDisponible = true)
            .noter("${heros.nom} — ${declaration.libelle}" + (declaration.cibleId?.let { id -> trouverMonstre(etat, id)?.let { " → ${it.nom}" } } ?: ""), CampLigne.HEROS)
        val desengage = declaration.actionId == "disengage"
        val precipite = declaration.actionId == "dash"

        // Déplacement vers un monstre : avant l'action (on s'approche pour frapper).
        val visee = declaration.distanceVisee
        val cibleDeplacement = declaration.deplacementCibleId
        val versLAvant = cibleDeplacement != null && visee != null && visee.ordinal < courant.distance(cibleDeplacement).ordinal
        if (versLAvant) courant = deplacer(courant, cibleDeplacement!!, visee!!, precipite, desengage)

        courant = when (declaration.actionId) {
            "attaquer" -> attaquer(courant, declaration, precipite)
            "magie" -> lancerSort(courant, declaration)
            "utilize" -> utiliserObjet(courant, declaration)
            "dodge" -> courant.copy(esquive = true).noter("${heros.nom} se met en garde : les attaques contre lui ont le désavantage", CampLigne.HEROS)
            "disengage" -> courant.noter("${heros.nom} se désengage : pas d'attaque d'opportunité ce tour", CampLigne.HEROS)
            "dash" -> courant.noter("${heros.nom} se précipite : déplacement doublé", CampLigne.HEROS)
            "hide" -> courant.copy(cache = true).noter("${heros.nom} se cache : avantage à sa prochaine attaque", CampLigne.HEROS)
            else -> courant.noter("Sans effet automatique dans la simulation.", CampLigne.INFO)
        }

        // Déplacement en s'éloignant : après l'action.
        if (cibleDeplacement != null && visee != null && !versLAvant) courant = deplacer(courant, cibleDeplacement, visee, precipite, desengage)
        return avancer(finDeTour(verifierFin(courant)))
    }

    /** Joue le personnage automatiquement jusqu'à la fin du combat. */
    fun simulerJusquALaFin(etat: EtatSimulation): EtatSimulation {
        var courant = etat
        while (courant.issue == null) {
            courant = jouerHeros(courant, actionAutomatique(courant) ?: DeclarationHeros("Esquiver", "dodge"))
        }
        return courant
    }

    /**
     * Action raisonnable pour le personnage : l'action Attaquer avec l'arme aux meilleurs dégâts
     * moyens (corps à corps s'il y a un monstre au contact, sinon à distance, sinon il charge),
     * sur le monstre le plus proche et le plus blessé. Null s'il n'a aucune attaque ou plus de cible.
     */
    fun actionAutomatique(etat: EtatSimulation): DeclarationHeros? {
        val debout = etat.monstresDebout.ifEmpty { return null }
        if (heros.attaques.isEmpty()) return null
        val auContact = debout.filter { etat.distance(it.id) == Distance.CONTACT }
        val melee = heros.attaques.filter { !it.aDistance }.maxByOrNull { degatsMoyens(it) }
        val distance = heros.attaques.filter { it.aDistance }.maxByOrNull { degatsMoyens(it) }
        val attaque = (if (auContact.isNotEmpty()) melee ?: distance else distance ?: melee) ?: return null
        val candidates = if (!attaque.aDistance && auContact.isNotEmpty()) auContact else debout
        val cible = candidates.minWith(compareBy<Combattant> { etat.distance(it.id).ordinal }.thenBy { it.pv })
        // Trop loin pour une arme de corps à corps : il se précipite d'abord vers la cible.
        val tropLoin = !attaque.aDistance && etat.distance(cible.id) == Distance.LONGUE
        return if (tropLoin) {
            DeclarationHeros("Se précipiter", "dash", cible.id, deplacementCibleId = cible.id, distanceVisee = Distance.CONTACT)
        } else {
            DeclarationHeros(
                libelle = "Attaquer : " + List(heros.nbAttaques.coerceAtLeast(1)) { attaque.nom }.joinToString(" + "),
                actionId = "attaquer",
                cibleId = cible.id,
                frappes = List(heros.nbAttaques.coerceAtLeast(1)) { FrappeHeros.Arme(attaque) },
            )
        }
    }

    // ── Personnage ──

    private fun trouverMonstre(etat: EtatSimulation, id: String) = etat.monstres.firstOrNull { it.id == id }

    /**
     * Finir à [visee] du monstre [monstreId]. Depuis la longue distance, le contact demande de se
     * précipiter. Quitter le contact sans se désengager ouvre une attaque d'opportunité au monstre.
     */
    private fun deplacer(etat: EtatSimulation, monstreId: String, visee: Distance, precipite: Boolean, desengage: Boolean): EtatSimulation {
        val monstre = trouverMonstre(etat, monstreId) ?: return etat
        val actuelle = etat.distance(monstreId)
        if (actuelle == visee) return etat
        val atteinte = if (actuelle == Distance.LONGUE && visee == Distance.CONTACT && !precipite) Distance.COURTE else visee
        var maj = etat.copy(distances = etat.distances + (monstreId to atteinte))
            .noter(
                "${heros.nom} → ${atteinte.label.lowercase()} de ${monstre.nom}" +
                    if (atteinte != visee) " (le contact demande de se précipiter)" else "",
                CampLigne.HEROS
            )
        if (actuelle == Distance.CONTACT && !desengage && !monstre.horsCombat) {
            maj = attaqueOpportuniteMonstre(maj, monstre)
        }
        return maj
    }

    private fun attaquer(etat: EtatSimulation, declaration: DeclarationHeros, precipite: Boolean): EtatSimulation {
        var courant = etat
        var cible = declaration.cibleId?.let { trouverMonstre(courant, it) }?.takeIf { !it.horsCombat }
            ?: courant.monstresDebout.minByOrNull { courant.distance(it.id).ordinal }
            ?: return courant
        declaration.frappes.forEach { frappe ->
            if (cible.horsCombat) {
                // Cible tombée : les frappes restantes passent à un autre monstre au contact.
                cible = courant.monstresDebout.firstOrNull { courant.distance(it.id) == Distance.CONTACT || frappe.aDistance() }
                    ?: return courant
            }
            if (!frappe.aDistance() && courant.distance(cible.id) != Distance.CONTACT) {
                // Corps à corps : il s'approche (courte distance), ou se précipite depuis la longue.
                if (courant.distance(cible.id) == Distance.LONGUE && !precipite) {
                    return courant.noter("${cible.nom} est trop loin pour frapper au corps à corps.", CampLigne.HEROS)
                }
                courant = courant.copy(distances = courant.distances + (cible.id to Distance.CONTACT))
                    .noter("${heros.nom} s'approche de ${cible.nom}", CampLigne.HEROS)
            }
            when (frappe) {
                is FrappeHeros.Arme -> {
                    val attaque = frappe.attaque
                    val desavantage = attaque.aDistance && courant.monstresDebout.any { courant.distance(it.id) == Distance.CONTACT }
                    val avantage = courant.cache || (!attaque.aDistance && ConditionCombat.A_TERRE in cible.conditions)
                    val (d20, detailD20) = jetD20Detail(avantage, desavantage)
                    courant = courant.copy(cache = false)
                    val total = d20 + attaque.bonusToucher
                    val critique = d20 == 20
                    val toucher = "$detailD20 ${signeBonus(attaque.bonusToucher)} = $total contre CA ${cible.ca}"
                    if (critique || (d20 != 1 && total >= cible.ca)) {
                        val (degats, detailDegats) = lancerDegats(attaque.formuleDegats, critique)
                        cible = cible.copy(pv = (cible.pv - degats).coerceAtLeast(0))
                        courant = courant.remplacerMonstre(cible).noter(
                            "${attaque.nom} sur ${cible.nom} : $toucher, ${if (critique) "CRITIQUE" else "touché"} — dégâts $detailDegats → ${cible.pv}/${cible.pvMax}" +
                                if (cible.horsCombat) " — ${cible.nom} tombe" else "",
                            CampLigne.HEROS
                        )
                    } else {
                        courant = courant.noter("${attaque.nom} sur ${cible.nom} : $toucher, raté", CampLigne.HEROS)
                    }
                }
                is FrappeHeros.Empoignade, is FrappeHeros.Bousculade -> {
                    val dd = if (frappe is FrappeHeros.Empoignade) frappe.dd else (frappe as FrappeHeros.Bousculade).dd
                    // Bonus de sauvegarde du monstre inconnu : jet sans modificateur.
                    val jet = d20()
                    val reussie = jet < dd
                    if (frappe is FrappeHeros.Empoignade) {
                        if (reussie) cible = cible.copy(conditions = cible.conditions + ConditionCombat.AGRIPPE)
                        courant = courant.remplacerMonstre(cible).noter(
                            "Empoignade sur ${cible.nom} : JS $jet contre DD $dd — " + if (reussie) "agrippé, il ne peut plus s'éloigner" else "il se dégage",
                            CampLigne.HEROS
                        )
                    } else {
                        if (reussie) cible = cible.copy(conditions = cible.conditions + ConditionCombat.A_TERRE)
                        courant = courant.remplacerMonstre(cible).noter(
                            "Bousculade sur ${cible.nom} : JS $jet contre DD $dd — " + if (reussie) "à terre (avantage au corps à corps)" else "il tient bon",
                            CampLigne.HEROS
                        )
                    }
                }
            }
        }
        return courant
    }

    private fun lancerSort(etat: EtatSimulation, declaration: DeclarationHeros): EtatSimulation {
        val sort = declaration.sort ?: return etat.noter("Aucun sort choisi.", CampLigne.INFO)
        var courant = etat
        // Emplacement : consommé dans la simulation seulement (la fiche n'est pas touchée).
        if (sort.niveau > 0 && !sort.sansEmplacement) {
            val niveau = declaration.niveauEmplacement ?: sort.niveau
            val restants = courant.emplacements[niveau] ?: 0
            if (restants <= 0) return courant.noter("Plus d'emplacement de niveau $niveau : ${sort.nom} échoue.", CampLigne.HEROS)
            courant = courant.copy(emplacements = courant.emplacements + (niveau to restants - 1))
        }
        sort.soin?.let { formule ->
            val (soin, detailSoin) = lancerFormuleDesDetail(formule, aleatoire) ?: (0 to formule)
            val pv = (courant.heros.pv + soin).coerceAtMost(courant.heros.pvMax)
            return courant.copy(heros = courant.heros.copy(pv = pv))
                .noter("${sort.nom} : ${heros.nom} récupère $soin PV ($detailSoin) → $pv/${courant.heros.pvMax}", CampLigne.HEROS)
        }
        val formule = sort.formuleDegats ?: return courant.noter("${sort.nom} : effet sans dégâts, à décrire.", CampLigne.INFO)
        val zone = declaration.ciblesZone.mapNotNull { trouverMonstre(courant, it) }.filter { !it.horsCombat }
        if (zone.size > 1 && sort.jet != JetSort.ATTAQUE) {
            // Sort de zone : un seul jet de dégâts, puis un JS par monstre (moitié s'il réussit).
            val (lances, detailDegats) = lancerFormuleDesDetail(formule, aleatoire) ?: (0 to formule)
            courant = courant.noter("${sort.nom} (zone, ${zone.size} cibles) : dégâts $detailDegats", CampLigne.HEROS)
            zone.forEach { monstre ->
                var cible = monstre
                val (degats, detail) = if (sort.jet == JetSort.SAUVEGARDE) {
                    val jet = d20()
                    if (jet >= sort.dd) lances / 2 to "JS ${sort.sauvegarde ?: ""} d20 $jet contre DD ${sort.dd}, réussi — moitié"
                    else lances to "JS ${sort.sauvegarde ?: ""} d20 $jet contre DD ${sort.dd}, raté"
                } else lances to "touché automatiquement"
                if (degats > 0) cible = cible.copy(pv = (cible.pv - degats).coerceAtLeast(0))
                courant = courant.remplacerMonstre(cible).noter(
                    "  ${cible.nom} : $detail → $degats dégâts, ${cible.pv}/${cible.pvMax}" + if (cible.horsCombat) " — ${cible.nom} tombe" else "",
                    CampLigne.HEROS
                )
            }
            return courant
        }
        var cible = declaration.cibleId?.let { trouverMonstre(courant, it) }?.takeIf { !it.horsCombat }
            ?: courant.monstresDebout.minByOrNull { courant.distance(it.id).ordinal }
            ?: return courant
        val (degats, detail) = when (sort.jet) {
            JetSort.ATTAQUE -> {
                val (d20, detailD20) = jetD20Detail(courant.cache, false)
                courant = courant.copy(cache = false)
                val total = d20 + sort.bonusAttaque
                val critique = d20 == 20
                val toucher = "$detailD20 ${signeBonus(sort.bonusAttaque)} = $total contre CA ${cible.ca}"
                if (critique || (d20 != 1 && total >= cible.ca)) {
                    val (lances, detailDegats) = lancerFormuleDesDetail(formule, aleatoire, critique) ?: (0 to formule)
                    lances to "$toucher, ${if (critique) "CRITIQUE" else "touché"} — dégâts $detailDegats"
                } else if (sort.demiDegatsSiEchec) {
                    // Sort mineur appuyé : moitié des dégâts même sur une attaque ratée.
                    val (lances, detailDegats) = lancerFormuleDesDetail(formule, aleatoire) ?: (0 to formule)
                    lances / 2 to "$toucher, raté — moitié (sort mineur appuyé) de $detailDegats"
                } else 0 to "$toucher, raté"
            }
            JetSort.SAUVEGARDE -> {
                // Bonus de sauvegarde du monstre inconnu : jet sans modificateur.
                val jet = d20()
                val (lances, detailDegats) = lancerFormuleDesDetail(formule, aleatoire) ?: (0 to formule)
                if (jet >= sort.dd) lances / 2 to "JS ${sort.sauvegarde ?: ""} d20 $jet contre DD ${sort.dd}, réussi — moitié de $detailDegats"
                else lances to "JS ${sort.sauvegarde ?: ""} d20 $jet contre DD ${sort.dd}, raté — dégâts $detailDegats"
            }
            JetSort.AUCUN -> (lancerFormuleDesDetail(formule, aleatoire) ?: (0 to formule)).let { (lances, d) -> lances to "touche automatiquement — dégâts $d" }
        }
        if (degats > 0) cible = cible.copy(pv = (cible.pv - degats).coerceAtLeast(0))
        return courant.remplacerMonstre(cible).noter(
            "${sort.nom} sur ${cible.nom} : $detail" + (if (degats > 0) " → $degats dégâts, ${cible.pv}/${cible.pvMax}" else "") +
                if (cible.horsCombat) " — ${cible.nom} tombe" else "",
            CampLigne.HEROS
        )
    }

    private fun utiliserObjet(etat: EtatSimulation, declaration: DeclarationHeros): EtatSimulation {
        val nom = declaration.objetNom ?: return etat.noter("Objet utilisé : effet à décrire.", CampLigne.INFO)
        val formule = declaration.soinObjet ?: return etat.noter("$nom : effet à décrire (pas de soins).", CampLigne.INFO)
        val (soin, detailSoin) = lancerFormuleDesDetail(formule, aleatoire) ?: (0 to formule)
        val pv = (etat.heros.pv + soin).coerceAtMost(etat.heros.pvMax)
        return etat.copy(heros = etat.heros.copy(pv = pv)).noter("$nom : ${heros.nom} récupère $soin PV ($detailSoin) → $pv/${etat.heros.pvMax}", CampLigne.HEROS)
    }

    /** Attaque d'opportunité du personnage contre un monstre qui quitte son contact sans se désengager. */
    private fun attaqueOpportuniteHeros(etat: EtatSimulation, monstre: Combattant): EtatSimulation {
        if (!etat.reactionDisponible || etat.heros.horsCombat) return etat
        val arme = heros.attaques.filter { !it.aDistance }.maxByOrNull { degatsMoyens(it) } ?: return etat
        var cible = monstre
        val d20 = d20()
        val total = d20 + arme.bonusToucher
        val critique = d20 == 20
        val toucher = "d20 $d20 ${signeBonus(arme.bonusToucher)} = $total contre CA ${cible.ca}"
        var maj = etat.copy(reactionDisponible = false)
        if (critique || (d20 != 1 && total >= cible.ca)) {
            val (degats, detailDegats) = lancerDegats(arme.formuleDegats, critique)
            cible = cible.copy(pv = (cible.pv - degats).coerceAtLeast(0))
            maj = maj.remplacerMonstre(cible).noter(
                "Attaque d'opportunité de ${heros.nom} (${arme.nom}) sur ${cible.nom} : $toucher, ${if (critique) "CRITIQUE" else "touché"} — dégâts $detailDegats → ${cible.pv}/${cible.pvMax}",
                CampLigne.HEROS
            )
        } else {
            maj = maj.noter("Attaque d'opportunité de ${heros.nom} (${arme.nom}) sur ${cible.nom} : $toucher, raté", CampLigne.HEROS)
        }
        return maj
    }

    // ── Monstres ──

    /** Le monstre frappe le personnage qui quitte son contact (réaction : une attaque au corps à corps). */
    private fun attaqueOpportuniteMonstre(etat: EtatSimulation, monstre: Combattant): EtatSimulation {
        val attaque = monstre.attaques.firstOrNull { it.corpsACorps && it.bonusToucher != null } ?: return etat
        val (degats, detail) = attaqueContreHeros(etat, attaque)
        val heros = etat.heros.copy(pv = (etat.heros.pv - degats).coerceAtLeast(0))
        return etat.copy(heros = heros).noter(
            "Attaque d'opportunité de ${monstre.nom} (${attaque.nom}) : $detail → ${heros.nom} ${heros.pv}/${heros.pvMax}",
            CampLigne.MONSTRE
        )
    }

    /** Un jet d'attaque (ou de dégâts) d'un monstre contre le personnage : (dégâts, détail). */
    private fun attaqueContreHeros(etat: EtatSimulation, attaque: AttaqueMonstre): Pair<Int, String> = when {
        attaque.bonusToucher != null -> {
            val (d20, detailD20) = jetD20Detail(false, etat.esquive)
            val total = d20 + attaque.bonusToucher
            val critique = d20 == 20
            val toucher = "$detailD20 ${signeBonus(attaque.bonusToucher)} = $total contre CA ${etat.heros.ca}"
            if (critique || (d20 != 1 && total >= etat.heros.ca)) {
                val (degats, detailDegats) = degatsMonstre(attaque, critique)
                degats to "$toucher, ${if (critique) "CRITIQUE" else "touché"} — dégâts $detailDegats"
            } else 0 to "$toucher, raté"
        }
        attaque.sauvegarde != null && attaque.dd != null -> {
            val de = d20()
            val bonus = heros.bonusSauvegardes[attaque.sauvegarde.lowercase()] ?: 0
            val jet = de + bonus
            val (degats, detailDegats) = degatsMonstre(attaque, false)
            val subis = if (jet >= attaque.dd) degats / 2 else degats
            subis to "JS ${attaque.sauvegarde} d20 $de ${signeBonus(bonus)} = $jet contre DD ${attaque.dd} : " +
                (if (jet >= attaque.dd) "réussi — moitié de $detailDegats = $subis" else "raté — dégâts $detailDegats")
        }
        else -> degatsMonstre(attaque, false).let { (degats, detail) -> degats to "dégâts $detail" }
    }

    /** Dégâts d'une attaque de monstre et leur détail ; sans formule lisible, les dégâts moyens de la fiche. */
    private fun degatsMonstre(attaque: AttaqueMonstre, critique: Boolean): Pair<Int, String> =
        attaque.formuleDegats?.let { lancerFormuleDesDetail(it, aleatoire, critique) }
            ?: (attaque.degatsMoyens to "${attaque.degatsMoyens} (moyenne de la fiche)")

    private fun tourMonstre(etat: EtatSimulation, depart: Combattant): EtatSimulation {
        var courant = etat
        var monstre = depart
        if (ConditionCombat.A_TERRE in monstre.conditions) {
            monstre = monstre.copy(conditions = monstre.conditions - ConditionCombat.A_TERRE)
            courant = courant.remplacerMonstre(monstre).noter("${monstre.nom} se relève", CampLigne.MONSTRE)
        }
        val decision = IaMonstre.decider(
            monstre = monstre,
            adversaires = listOf(courant.heros),
            distances = mapOf(courant.heros.id to courant.distance(monstre.id)),
            aleatoire = aleatoire,
            allies = courant.monstres.filter { it.id != monstre.id },
            distanceEntre = { a, b ->
                when {
                    a == courant.heros.id -> courant.distance(b)
                    b == courant.heros.id -> courant.distance(a)
                    else -> Distance.COURTE
                }
            },
            round = courant.round,
        )
        decision.deplacements[courant.heros.id]?.let { nouvelle ->
            val actuelle = courant.distance(monstre.id)
            when {
                ConditionCombat.AGRIPPE in monstre.conditions && nouvelle.ordinal > actuelle.ordinal ->
                    courant = courant.noter("${monstre.nom} est agrippé et ne peut pas s'éloigner", CampLigne.MONSTRE)
                else -> {
                    courant = courant.copy(distances = courant.distances + (monstre.id to nouvelle))
                    if (actuelle == Distance.CONTACT && nouvelle != Distance.CONTACT && !decision.desengage) {
                        courant = attaqueOpportuniteHeros(courant, monstre)
                        monstre = courant.monstres.first { it.id == monstre.id }
                        if (monstre.horsCombat) return courant.noter("${monstre.nom} tombe en fuyant", CampLigne.MONSTRE)
                    }
                }
            }
        }
        val attaque = decision.attaque
        if (attaque == null || decision.cibleId != courant.heros.id) {
            return courant.noter("${monstre.nom} — ${decision.libelle}", CampLigne.MONSTRE)
        }
        // Capacité à usage limité : consommée (recharge en début de round).
        if (attaque.estSpeciale) {
            monstre = monstre.copy(capacitesUtilisees = monstre.capacitesUtilisees + (attaque.nom to ((monstre.capacitesUtilisees[attaque.nom] ?: 0) + 1)))
            courant = courant.remplacerMonstre(monstre)
        }
        var degatsTotal = 0
        val details = (1..decision.nbAttaques.coerceAtLeast(1)).map {
            val (degats, detail) = attaqueContreHeros(courant, attaque)
            degatsTotal += degats
            detail
        }
        val herosMaj = courant.heros.copy(pv = (courant.heros.pv - degatsTotal).coerceAtLeast(0))
        return courant.copy(heros = herosMaj).noter(
            "${monstre.nom} — ${attaque.nom} : ${details.joinToString(" | ")} → ${heros.nom} ${herosMaj.pv}/${herosMaj.pvMax}" +
                if (herosMaj.horsCombat) " — ${heros.nom} tombe à 0 PV" else "",
            CampLigne.MONSTRE
        )
    }

    // ── Déroulé ──

    /** Fait jouer les monstres dans l'ordre d'initiative jusqu'au tour du personnage ou la fin. */
    private fun avancer(depart: EtatSimulation): EtatSimulation {
        var etat = verifierFin(depart)
        while (etat.issue == null && !etat.tourDuHeros) {
            val id = etat.ordre[etat.tourIndex]
            val monstre = etat.monstres.firstOrNull { it.id == id }
            if (monstre != null && !monstre.horsCombat) etat = verifierFin(tourMonstre(etat, monstre))
            if (etat.issue == null) etat = finDeTour(etat)
        }
        return etat
    }

    private fun finDeTour(etat: EtatSimulation): EtatSimulation {
        if (etat.issue != null) return etat
        val suivant = etat.tourIndex + 1
        if (suivant < etat.ordre.size) return verifierFin(etat.copy(tourIndex = suivant))
        val round = etat.round + 1
        if (round > roundsMax) return etat.copy(issue = IssueSimulation.INTERROMPU).noter("Combat interrompu après $roundsMax rounds", CampLigne.INFO)
        // Recharge des capacités (d6 ≥ seuil) au début de chaque round.
        val recharges = mutableListOf<String>()
        val monstres = etat.monstres.map { m ->
            val rechargees = m.attaques.filter { a -> a.recharge != null && (m.capacitesUtilisees[a.nom] ?: 0) > 0 }
                .filter { a -> aleatoire.nextInt(1, 7) >= a.recharge!! }
            if (rechargees.isEmpty()) m else {
                recharges += "${m.nom} récupère ${rechargees.joinToString { it.nom }}"
                m.copy(capacitesUtilisees = m.capacitesUtilisees - rechargees.map { it.nom }.toSet())
            }
        }
        return verifierFin(
            etat.copy(
                monstres = monstres,
                round = round,
                tourIndex = 0,
                journal = etat.journal + recharges.map { LigneSimulation(it, CampLigne.INFO) } + LigneSimulation("Round $round", CampLigne.INFO)
            )
        )
    }

    private fun verifierFin(etat: EtatSimulation): EtatSimulation = when {
        etat.issue != null -> etat
        etat.heros.horsCombat -> etat.copy(issue = IssueSimulation.DEFAITE)
            .noter("Défaite : ${heros.nom} est hors de combat au round ${etat.round}", CampLigne.INFO)
        etat.monstresDebout.isEmpty() -> etat.copy(issue = IssueSimulation.VICTOIRE)
            .noter("Victoire au round ${etat.round} : ${heros.nom} termine à ${etat.heros.pv}/${etat.heros.pvMax} PV", CampLigne.INFO)
        else -> etat
    }

    private fun d20() = aleatoire.nextInt(1, 21)

    /**
     * d20 avec avantage (meilleur de deux) ou désavantage (pire de deux) ; les deux s'annulent.
     * Avec le détail pour le journal : `d20 12`, `d20 avantage 7/15 → 15`.
     */
    private fun jetD20Detail(avantage: Boolean, desavantage: Boolean): Pair<Int, String> = when {
        avantage && !desavantage -> { val a = d20(); val b = d20(); maxOf(a, b) to "d20 avantage $a/$b → ${maxOf(a, b)}" }
        desavantage && !avantage -> { val a = d20(); val b = d20(); minOf(a, b) to "d20 désavantage $a/$b → ${minOf(a, b)}" }
        else -> d20().let { it to "d20 $it" }
    }

    /** Dégâts d'une arme du personnage ; une formule fixe (mains nues « 4 ») est prise telle quelle. */
    private fun lancerDegats(formule: String, critique: Boolean): Pair<Int, String> =
        lancerFormuleDesDetail(formule, aleatoire, critique)
            ?: (formule.trim().toIntOrNull() ?: 1).let { it to "$it (fixe)" }

    private fun signeBonus(v: Int) = if (v >= 0) "+$v" else "$v"

    private fun FrappeHeros.aDistance() = this is FrappeHeros.Arme && attaque.aDistance

    private fun EtatSimulation.noter(texte: String, camp: CampLigne) = copy(journal = journal + LigneSimulation(texte, camp))

    private fun EtatSimulation.remplacerMonstre(m: Combattant) = copy(monstres = monstres.map { if (it.id == m.id) m else it })

    companion object {
        /** Dégâts moyens d'une attaque (formule `2d6 + 3`), pour choisir la meilleure. */
        fun degatsMoyens(attaque: AttaqueHeros): Double {
            val (nb, faces, bonus) = decomposerFormule(attaque.formuleDegats) ?: return 1.0
            return nb * (faces + 1) / 2.0 + bonus
        }
    }
}
