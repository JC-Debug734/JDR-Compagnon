package com.jc2.jdrcompagnon.feature_carte.presentation

import com.jc2.jdrcompagnon.network.VoyageData
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Voyage préparé sur la carte de campagne et partagé avec les joueurs connectés.
 *
 * Un joueur (ou le MJ) trace une route puis se déclare « Prêt au départ » : la route devient le
 * voyage partagé, diffusé à tous (NetworkSessionManager, TYPE_VOYAGE_STATE). Le MJ peut changer la
 * route, l'allure ou les heures de marche par jour (donc les haltes) : chacun doit alors la
 * revalider. Quand le MJ et tous les joueurs connectés ([attendusProvider]) sont prêts, le voyage
 * part ([VoyageData.enRoute]) : côté MJ, VoyageMjOverlay enchaîne pour chaque étape (haltes puis
 * arrivée) un événement de voyage et l'écran de repos.
 *
 * Côté MJ cet objet fait foi ; côté joueur il ne fait que refléter l'état reçu ([recevoir]).
 */
object VoyageSession {

    /** Libellé de celui qui propose ou modifie une route depuis l'appareil du MJ. */
    const val NOM_MJ = "le MJ"

    private val _etat = MutableStateFlow<VoyageData?>(null)
    val etat: StateFlow<VoyageData?> = _etat.asStateFlow()

    /** MJ : pseudos des joueurs connectés, qui doivent tous valider le départ. */
    var attendusProvider: () -> List<String> = { emptyList() }

    // ── Côté MJ ──

    /**
     * Nouvelle route (ou route modifiée) proposée par [par] : tout le monde doit la (re)valider,
     * sauf [pretNom] (le joueur qui la propose en se déclarant prêt) et le MJ si [mjPret].
     */
    fun proposer(route: VoyageData, par: String, pretNom: String?, mjPret: Boolean) {
        val actuel = _etat.value
        if (actuel?.enRoute == true) return
        _etat.value = verifie(
            route.copy(
                id = actuel?.id ?: route.id,
                version = (actuel?.version ?: 0) + 1,
                proposePar = par,
                prets = listOfNotNull(pretNom),
                mjPret = mjPret,
                enRoute = false,
            )
        )
    }

    /** Un joueur valide la route actuelle. */
    fun marquerPret(nom: String) {
        val v = _etat.value ?: return
        if (v.enRoute || nom in v.prets) return
        _etat.value = verifie(v.copy(prets = v.prets + nom))
    }

    /** Le MJ valide la route actuelle. */
    fun marquerMjPret() {
        val v = _etat.value ?: return
        if (v.enRoute) return
        _etat.value = verifie(v.copy(mjPret = true))
    }

    /** À rappeler quand les joueurs connectés changent : un départ peut être débloqué. */
    fun reverifier() {
        val v = _etat.value ?: return
        if (!v.enRoute) _etat.value = verifie(v)
    }

    fun annuler() {
        _etat.value = null
        progression.value = Progression()
    }

    private fun verifie(v: VoyageData): VoyageData {
        val attendus = attendusProvider().distinct()
        val tousPrets = v.mjPret && attendus.all { it in v.prets }
        if (tousPrets && !v.enRoute) progression.value = Progression()
        return v.copy(attendus = attendus, enRoute = tousPrets)
    }

    /** Étape du voyage en cours côté MJ : [etape] parmi les haltes puis l'arrivée ; [repos] = écran de repos. */
    data class Progression(val etape: Int = 0, val repos: Boolean = false, val etapesAppliquees: Set<Int> = emptySet())

    val progression = MutableStateFlow(Progression())

    /** Nombre d'étapes : chaque halte de fin de journée, puis l'arrivée. */
    fun nombreEtapes(v: VoyageData): Int = v.arrets.size + 1

    fun libelleEtape(v: VoyageData, etape: Int): String =
        v.arrets.getOrNull(etape)?.let { "Halte ${it.numero} (fin du jour ${it.numero}, ${it.km.toInt()} km parcourus)" }
            ?: "Arrivée : ${v.arriveeNom}"

    fun lieuEtape(v: VoyageData, etape: Int): String =
        v.arrets.getOrNull(etape)?.let { "halte ${it.numero}" } ?: v.arriveeNom

    /**
     * Entrée dans l'étape [etape] (une seule fois) : l'horloge avance des heures de marche du jour
     * et l'icône du groupe courant est déplacée à la halte, puis à l'arrivée où il prend le nom du
     * lieu (vide sur un point libre : le groupe n'est plus « dans » son ancienne ville).
     */
    fun entrerEtape(v: VoyageData, etape: Int) {
        val p = progression.value
        if (etape in p.etapesAppliquees) return
        progression.value = p.copy(etapesAppliquees = p.etapesAppliquees + etape)
        val heures = if (etape < v.arrets.size) v.heuresMax.toDouble()
        else (v.heures - v.heuresMax * v.arrets.size).coerceAtLeast(0.0)
        ScenarioClockState.advanceManually((heures * 60).toLong())
        val (fx, fy) = v.arrets.getOrNull(etape)?.let { it.fx to it.fy } ?: v.etapes.last().let { it.fx to it.fy }
        val groupe = GameState.mjGroups.value.firstOrNull { it.id == GameState.currentGroupId.value } ?: return
        val lieu = if (etape < v.arrets.size) "" else if (v.arriveePointId != null) v.arriveeNom else ""
        GameState.updateMjGroup(groupe.copy(carteId = v.carteId, carteFx = fx, carteFy = fy, location = lieu))
    }

    /** Événement de l'étape vu : place à l'écran de repos. */
    fun versRepos() {
        progression.value = progression.value.copy(repos = true)
    }

    /** Repos terminé (ou passé) : étape suivante, ou fin du voyage après l'arrivée. */
    fun etapeSuivante() {
        val v = _etat.value ?: return
        val p = progression.value
        if (p.etape + 1 >= nombreEtapes(v)) annuler()
        else progression.value = p.copy(etape = p.etape + 1, repos = false)
    }

    // ── Côté joueur ──

    fun recevoir(data: VoyageData?) {
        _etat.value = data
    }
}
