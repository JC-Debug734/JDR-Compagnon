package com.jc2.jdrcompagnon.feature_epreuve.presentation

import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.GroupeEpreuve
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
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
    val tentatives: List<TentativeEpreuve> = emptyList(),
    val numeroComplication: Int = 0,
    val terminee: Boolean = false,
    // Instant du démarrage : distingue deux lancements successifs de la même épreuve (musique...).
    val lancement: Long = System.currentTimeMillis(),
) {
    val reussites: Int get() = tentatives.count { it.reussite }
    val echecs: Int get() = tentatives.count { !it.reussite }
    val reussie: Boolean get() = reussites >= epreuve.reussitesRequises
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

    /** Démarre [epreuve] adaptée au groupe courant ([groupeCourant]), que le MJ peut corriger ensuite. */
    fun demarrer(epreuve: Epreuve) {
        val groupe = groupeCourant()
        _etat.value = EpreuveActive(ReglesEpreuve.adapter(epreuve, groupe), source = epreuve, groupe = groupe)
    }

    /** L'épreuve en cours a été modifiée dans l'éditeur (image, complications...) : la session suit. */
    fun epreuveModifiee(epreuve: Epreuve) {
        _etat.update { courant ->
            if (courant == null || courant.epreuve.id != epreuve.id) courant
            else courant.copy(epreuve = ReglesEpreuve.adapter(epreuve, courant.groupe), source = epreuve)
        }
    }

    /** Le MJ corrige le nombre de joueurs ou le niveau : réussites et DD recalculés, tentatives gardées. */
    fun ajusterGroupe(joueurs: Int, niveau: Int) = modifier { etat ->
        val groupe = GroupeEpreuve(joueurs.coerceIn(1, ReglesEpreuve.JOUEURS_MAX), niveau.coerceIn(1, ReglesEpreuve.NIVEAU_MAX))
        val suivant = etat.copy(epreuve = ReglesEpreuve.adapter(etat.source, groupe), groupe = groupe)
        if (suivant.reussie) suivant.copy(terminee = true) else suivant
    }

    /**
     * Groupe qui affronte l'épreuve : celui sélectionné par le MJ (fiches membres et joueurs sans
     * fiche), sinon les personnages des joueurs connectés, sinon 4 joueurs de niveau 1 supposés.
     * Niveau = moyenne arrondie.
     */
    fun groupeCourant(): GroupeEpreuve {
        val groupe = GameState.mjGroups.value.firstOrNull { it.id == GameState.currentGroupId.value }
        val niveaux = if (groupe != null) {
            GameState.characters.value.filter { it.id in groupe.memberIds && it.type == "PJ" }.map { it.level } +
                groupe.tablePlayers.map { it.level }
        } else {
            NetworkSessionManager.connectedCharacters().map { it.level }
        }
        if (niveaux.isEmpty()) return GroupeEpreuve(4, 1, parDefaut = true)
        return GroupeEpreuve(niveaux.size, kotlin.math.round(niveaux.average()).toInt().coerceIn(1, ReglesEpreuve.NIVEAU_MAX))
    }

    fun reussite(joueur: String?, action: String?) = modifier { etat ->
        val suivant = etat.copy(tentatives = etat.tentatives + TentativeEpreuve(joueur.nettoye(), action.nettoye(), reussite = true))
        if (suivant.reussie) suivant.copy(terminee = true) else suivant
    }

    /** Un échec déclenche toujours une complication de l'épreuve (si elle en a). */
    fun echec(joueur: String?, action: String?, random: Random = Random.Default) = modifier { etat ->
        val precedente = etat.tentatives.lastOrNull { it.complication != null }?.complication
        val complication = ReglesEpreuve.tirerComplication(etat.epreuve.complications, precedente, random)
        etat.copy(
            tentatives = etat.tentatives + TentativeEpreuve(joueur.nettoye(), action.nettoye(), reussite = false, complication = complication),
            numeroComplication = if (complication != null) etat.numeroComplication + 1 else etat.numeroComplication,
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

    fun fermer() {
        _etat.value = null
    }

    private fun modifier(transformation: (EpreuveActive) -> EpreuveActive) {
        _etat.update { courant -> if (courant == null || courant.terminee) courant else transformation(courant) }
    }

    private fun String?.nettoye(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
