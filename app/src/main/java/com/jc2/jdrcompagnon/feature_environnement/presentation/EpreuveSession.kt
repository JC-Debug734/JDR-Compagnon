package com.jc2.jdrcompagnon.feature_environnement.presentation

import com.jc2.jdrcompagnon.feature_environnement.domain.model.CapaciteEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DureeEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EchelleEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class IssueEpreuve(val label: String) {
    REUSSITE("Épreuve surmontée"),
    ECHEC("La menace s'abat sur le groupe"),
    ABANDON("Épreuve interrompue")
}

/**
 * Épreuve en cours de résolution. [niveau]/[nbJoueurs] fixent le dosage (tier → DD/dégâts,
 * nombre de joueurs → longueur du Progrès) ; [niveauAuto] indique qu'ils ont été calculés depuis
 * les personnages connectés plutôt que saisis par le MJ.
 *
 * [echecEnAttente] : un échec vient d'avoir lieu, le MJ peut déclencher une Réaction ou une
 * Action de l'environnement (seule source de "tension" de l'épreuve).
 */
data class EpreuveEnCours(
    val id: String,
    val epreuve: EpreuveEnvironnementale,
    val niveau: Int,
    val nbJoueurs: Int,
    val niveauAuto: Boolean,
    val duree: DureeEpreuve,
    val progres: Int,
    val progresMax: Int,
    val menace: Int,
    val menaceMax: Int,
    val echecEnAttente: Boolean = false,
    val journal: List<String> = emptyList(),
    val issue: IssueEpreuve? = null
) {
    val tier: Int get() = EchelleEpreuve.tier(niveau)
}

/**
 * Épreuve active côté MJ, au niveau application (comme ScenarioClockState) : survit à la
 * navigation, et NetworkSessionManager la rediffuse aux joueurs connectés à chaque changement.
 */
object EpreuveSession {

    private val _etat = MutableStateFlow<EpreuveEnCours?>(null)
    val etat: StateFlow<EpreuveEnCours?> = _etat.asStateFlow()

    fun demarrer(epreuve: EpreuveEnvironnementale, niveau: Int, nbJoueurs: Int, niveauAuto: Boolean, duree: DureeEpreuve = epreuve.duree) {
        val niveauBorne = niveau.coerceIn(1, 20)
        val tier = EchelleEpreuve.tier(niveauBorne)
        _etat.value = EpreuveEnCours(
            id = UUID.randomUUID().toString(),
            epreuve = epreuve,
            niveau = niveauBorne,
            nbJoueurs = nbJoueurs.coerceAtLeast(1),
            niveauAuto = niveauAuto,
            duree = duree,
            progres = 0,
            progresMax = EchelleEpreuve.progresMax(duree, nbJoueurs),
            menace = epreuve.menaceMax,
            menaceMax = epreuve.menaceMax,
            journal = listOf("Début — niveau $niveauBorne (tier $tier), $nbJoueurs joueur(s), durée ${duree.label.lowercase()}")
        )
    }

    fun reussite(critique: Boolean = false) = modifier { etat ->
        val gain = if (critique) 2 else 1
        etat.copy(
            progres = (etat.progres + gain).coerceAtMost(etat.progresMax),
            echecEnAttente = false,
            journal = etat.journal + if (critique) "20 naturel : Progrès +2" else "Réussite : Progrès +1"
        )
    }

    fun echec(critique: Boolean = false) = modifier { etat ->
        val perte = if (critique) 2 else 1
        etat.copy(
            menace = (etat.menace - perte).coerceAtLeast(0),
            echecEnAttente = true,
            journal = etat.journal + if (critique) "1 naturel : Menace -2" else "Échec : Menace -1"
        )
    }

    fun declencher(capacite: CapaciteEpreuve) = modifier { etat ->
        etat.copy(echecEnAttente = false, journal = etat.journal + "${capacite.type.label} : ${capacite.nom}")
    }

    fun ignorerEchec() = modifier { it.copy(echecEnAttente = false) }

    fun ajusterProgres(delta: Int) = modifier { it.copy(progres = (it.progres + delta).coerceIn(0, it.progresMax)) }

    fun ajusterMenace(delta: Int) = modifier { it.copy(menace = (it.menace + delta).coerceIn(0, it.menaceMax)) }

    /** Change le niveau de référence en cours d'épreuve : les DD/dégâts suivent, les jauges restent. */
    fun changerNiveau(niveau: Int) = modifier {
        val borne = niveau.coerceIn(1, 20)
        it.copy(niveau = borne, niveauAuto = false, journal = it.journal + "Niveau de référence → $borne (tier ${EchelleEpreuve.tier(borne)})")
    }

    fun abandonner() {
        val etat = _etat.value ?: return
        if (etat.issue == null) _etat.value = etat.copy(issue = IssueEpreuve.ABANDON, echecEnAttente = false, journal = etat.journal + IssueEpreuve.ABANDON.label)
    }

    fun fermer() {
        _etat.value = null
    }

    private fun modifier(transformation: (EpreuveEnCours) -> EpreuveEnCours) {
        _etat.update { courant ->
            if (courant == null || courant.issue != null) return@update courant
            val suivant = transformation(courant)
            val issue = when {
                suivant.progres >= suivant.progresMax -> IssueEpreuve.REUSSITE
                suivant.menace <= 0 -> IssueEpreuve.ECHEC
                else -> null
            }
            if (issue == null) suivant else suivant.copy(issue = issue, journal = suivant.journal + issue.label)
        }
    }
}
