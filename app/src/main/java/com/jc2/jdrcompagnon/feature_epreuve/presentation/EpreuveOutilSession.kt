package com.jc2.jdrcompagnon.feature_epreuve.presentation

import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.GroupeEpreuve
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ReglesEpreuve
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Une tentative annoncée par un joueur : [joueur]/[action] facultatifs, saisis par le MJ. */
data class TentativeEpreuve(
    val joueur: String?,
    val action: String?,
    val reussite: Boolean,
    val complication: ComplicationEpreuve? = null,
)

/**
 * Épreuve en cours de résolution. [numeroComplication] s'incrémente à chaque complication tirée :
 * la page table s'en sert pour rejouer son animation même si le tirage retombe sur un titre déjà vu.
 */
data class EpreuveActive(
    // Épreuve adaptée au groupe (ReglesEpreuve.adapter) : c'est elle qu'affichent l'écran et la table.
    val epreuve: Epreuve,
    // Épreuve telle qu'enregistrée, pour recalculer [epreuve] quand le MJ corrige le groupe.
    val source: Epreuve = epreuve,
    val groupe: GroupeEpreuve = GroupeEpreuve(4, 1, parDefaut = true),
    // Dernier groupe détecté (groupe sélectionné) : seul un changement de celui-ci écrase une
    // correction manuelle du MJ (voir EpreuveOutilSession.suivreGroupe).
    val groupeDetecte: GroupeEpreuve = groupe,
    // Écart du DD (ReglesEpreuve.PLAGE_VARIATION_DD), retiré au lancement et à chaque réussite.
    val variationDd: Int = 0,
    val tentatives: List<TentativeEpreuve> = emptyList(),
    val numeroComplication: Int = 0,
    val terminee: Boolean = false,
    // Personnage qui agit (« Qui agit ? ») : son niveau fixe DD et dégâts. Null = aucun participant connu.
    val acteur: String? = null,
    // Instant du démarrage : distingue deux lancements successifs de la même épreuve (musique...).
    val lancement: Long = System.currentTimeMillis(),
) {
    val reussites: Int get() = tentatives.count { it.reussite }
    val echecs: Int get() = tentatives.count { !it.reussite }
    val reussie: Boolean get() = reussites >= epreuve.reussitesRequises
    // Trop d'échecs : l'épreuve est perdue (issue d'échec, éventuel combat final).
    val echouee: Boolean get() = !reussie && echecs >= epreuve.echecsMax
    val derniereComplication: ComplicationEpreuve?
        get() = tentatives.lastOrNull()?.takeIf { !it.reussite }?.complication
}

/**
 * Épreuve active côté MJ, au niveau application (comme CombatSession) : survit à
 * la navigation, et MjWebServer la lit directement pour l'afficher sur la page table.
 */
object EpreuveOutilSession {

    private val _etat = MutableStateFlow<EpreuveActive?>(null)
    val etat: StateFlow<EpreuveActive?> = _etat.asStateFlow()

    // Piste lancée pour l'épreuve en cours (nom affiché par MusicManager), coupée à sa fermeture.
    private var musiqueEpreuve: String? = null

    /**
     * Démarre [epreuve] adaptée au groupe courant ([groupeCourant]), au niveau du premier
     * participant, que le MJ peut corriger ensuite.
     */
    fun demarrer(epreuve: Epreuve, random: Random = Random.Default) {
        arreterMusique()
        val acteur = participants().firstOrNull()?.first
        val groupe = groupeCourant(acteur)
        val variation = tirerVariation(random)
        _etat.value = EpreuveActive(
            ReglesEpreuve.adapter(epreuve, groupe, variation),
            source = epreuve,
            groupe = groupe,
            variationDd = variation,
            acteur = acteur,
        )
    }

    /** Le MJ choisit qui agit : le niveau de l'épreuve devient celui de ce personnage. */
    fun choisirActeur(nom: String) = modifier { etat ->
        val detecte = groupeCourant(nom)
        appliquerGroupe(etat, etat.groupe.copy(niveau = detecte.niveau, parDefaut = detecte.parDefaut))
            .copy(acteur = nom, groupeDetecte = detecte)
    }

    /** L'épreuve en cours a été modifiée dans l'éditeur (image, complications...) : la session suit. */
    fun epreuveModifiee(epreuve: Epreuve) {
        _etat.update { courant ->
            if (courant == null || courant.epreuve.id != epreuve.id) courant
            else {
                // La difficulté choisie au lancement est gardée tant que l'épreuve reste adaptative.
                val source = if (epreuve.difficulte != null && courant.source.difficulte != null) epreuve.copy(difficulte = courant.source.difficulte) else epreuve
                courant.copy(epreuve = ReglesEpreuve.adapter(source, courant.groupe, courant.variationDd), source = source)
            }
        }
    }

    /** Le MJ corrige le nombre de joueurs ou le niveau : réussites et DD recalculés, tentatives gardées. */
    fun ajusterGroupe(joueurs: Int, niveau: Int) = modifier { etat ->
        appliquerGroupe(etat, GroupeEpreuve(joueurs.coerceIn(1, ReglesEpreuve.JOUEURS_MAX), niveau.coerceIn(1, ReglesEpreuve.NIVEAU_MAX)))
    }

    /**
     * Le groupe sélectionné (ou sa composition) a changé pendant l'épreuve : joueurs et niveau le
     * suivent. Sans changement du groupe détecté, une correction manuelle du MJ est conservée.
     */
    fun suivreGroupe() = modifier { etat ->
        // Personnage qui agit sorti du groupe : on repasse au premier de la liste.
        val noms = participants().map { it.first }
        val acteur = etat.acteur?.takeIf { it in noms } ?: noms.firstOrNull()
        val detecte = groupeCourant(acteur)
        if (detecte == etat.groupeDetecte && acteur == etat.acteur) etat
        else appliquerGroupe(etat, detecte).copy(groupeDetecte = detecte, acteur = acteur)
    }

    private fun appliquerGroupe(etat: EpreuveActive, groupe: GroupeEpreuve): EpreuveActive {
        val suivant = etat.copy(epreuve = ReglesEpreuve.adapter(etat.source, groupe, etat.variationDd), groupe = groupe)
        return if (suivant.reussie || suivant.echouee) suivant.copy(terminee = true) else suivant
    }

    /**
     * Participants (nom, niveau) : groupe sélectionné par le MJ (fiches membres PJ et PNJ, puis
     * joueurs sans fiche, comme pour un combat), sinon les personnages des joueurs connectés.
     */
    fun participants(): List<Pair<String, Int>> {
        val groupe = GameState.mjGroups.value.firstOrNull { it.id == GameState.currentGroupId.value }
        val liste = if (groupe != null) {
            GameState.characters.value.filter { it.id in groupe.memberIds && (it.type == "PJ" || it.type == "PNJ") }.map { it.name to it.level } +
                groupe.tablePlayers.map { it.name to it.level }
        } else {
            NetworkSessionManager.connectedCharacters().map { it.name to it.level }
        }
        return liste.filter { it.first.isNotBlank() }.distinctBy { it.first }
    }

    /**
     * Groupe qui affronte l'épreuve : nombre de [participants], niveau du personnage [acteur]
     * (le premier de la liste par défaut), sinon 4 joueurs de niveau 1 supposés.
     */
    fun groupeCourant(acteur: String? = null): GroupeEpreuve {
        val liste = participants()
        if (liste.isEmpty()) return GroupeEpreuve(4, 1, parDefaut = true)
        val niveau = (liste.firstOrNull { it.first == acteur } ?: liste.first()).second
        return GroupeEpreuve(
            liste.size.coerceIn(1, ReglesEpreuve.JOUEURS_MAX),
            niveau.coerceIn(1, ReglesEpreuve.NIVEAU_MAX),
        )
    }

    /** Une réussite fait avancer le compteur et retire l'écart du DD pour la suite de l'épreuve. */
    fun reussite(joueur: String?, action: String?, random: Random = Random.Default) = modifier { etat ->
        val variation = tirerVariation(random)
        val suivant = etat.copy(
            epreuve = ReglesEpreuve.adapter(etat.source, etat.groupe, variation),
            variationDd = variation,
            tentatives = etat.tentatives + TentativeEpreuve(joueur.nettoye(), action.nettoye(), reussite = true),
        )
        if (suivant.reussie) suivant.copy(terminee = true) else suivant
    }

    /**
     * Un échec déclenche une complication de l'épreuve (si elle en a), sauf celui qui atteint
     * [Epreuve.echecsMax] : l'épreuve est alors perdue et c'est son issue d'échec qui s'applique.
     */
    fun echec(joueur: String?, action: String?, random: Random = Random.Default) = modifier { etat ->
        val perdue = !etat.reussie && etat.echecs + 1 >= etat.epreuve.echecsMax
        val precedente = etat.tentatives.lastOrNull { it.complication != null }?.complication
        val complication = if (perdue) null else ReglesEpreuve.tirerComplication(etat.epreuve.complications, precedente, random)
        etat.copy(
            tentatives = etat.tentatives + TentativeEpreuve(joueur.nettoye(), action.nettoye(), reussite = false, complication = complication),
            numeroComplication = if (complication != null) etat.numeroComplication + 1 else etat.numeroComplication,
            terminee = perdue,
        )
    }

    /** Relance une autre complication pour le dernier échec (le MJ trouve la première inadaptée). */
    fun relancerComplication(random: Random = Random.Default) = modifier { etat ->
        val derniere = etat.tentatives.lastOrNull()?.takeIf { !it.reussite } ?: return@modifier etat
        val complication = ReglesEpreuve.tirerComplication(etat.epreuve.complications, derniere.complication, random)
            ?: return@modifier etat
        etat.copy(
            tentatives = etat.tentatives.dropLast(1) + derniere.copy(complication = complication),
            numeroComplication = etat.numeroComplication + 1,
        )
    }

    /** Annule la dernière tentative (erreur de saisie), y compris après la réussite finale. */
    fun annulerDerniere() {
        _etat.update { courant ->
            if (courant == null || courant.tentatives.isEmpty()) courant
            else courant.copy(tentatives = courant.tentatives.dropLast(1), terminee = false)
        }
    }

    /** Arrête l'épreuve (abandon ou fin décidée par le MJ) : elle reste affichée jusqu'à [fermer]. */
    fun terminer() = modifier { it.copy(terminee = true) }

    /** Ferme l'épreuve, et coupe sa musique si c'est encore elle qui joue. */
    fun fermer() {
        _etat.value = null
        arreterMusique()
    }

    /** Musique de l'épreuve démarrée (EpreuveResolutionScreen) : coupée à la fermeture de l'épreuve. */
    fun musiqueLancee(nomPiste: String) {
        musiqueEpreuve = nomPiste
    }

    private fun arreterMusique() {
        val piste = musiqueEpreuve ?: return
        musiqueEpreuve = null
        // Le MJ a pu choisir une autre musique entre-temps : on ne coupe que celle de l'épreuve.
        if (MusicManager.currentTrack.value == piste) MusicManager.stop()
    }

    private fun tirerVariation(random: Random): Int =
        random.nextInt(ReglesEpreuve.PLAGE_VARIATION_DD.first, ReglesEpreuve.PLAGE_VARIATION_DD.last + 1)

    private fun modifier(transformation: (EpreuveActive) -> EpreuveActive) {
        _etat.update { courant -> if (courant == null || courant.terminee) courant else transformation(courant) }
    }

    private fun String?.nettoye(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
