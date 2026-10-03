package com.jc2.jdrcompagnon.ui

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.sceneProfiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Portée des éléments propres à une campagne : scénarios ([GameState.MjCampaign.scenarioIds]),
 * fiches PNJ/créatures ([GameState.MjCampaign.pnjIds]) et livres personnalisés
 * ([GameState.MjCampaign.livreIds]).
 *
 * Règle : un élément qui n'appartient à aucune campagne est toujours visible. Un élément qui
 * appartient à une ou plusieurs campagnes n'est visible que si l'une d'elles est la campagne
 * sélectionnée (GameState.currentCampaignId) — ou, pour une fiche, si elle est utilisée par un
 * scénario visible (lien #pnj/#event, discussion, profil de combat). Ainsi, le contenu d'une
 * campagne importée n'encombre pas les listes tant qu'on joue autre chose, mais un PNJ repris
 * dans un autre scénario ou une autre campagne sélectionnée y reste disponible.
 */
object PorteeCampagne {

    private val _versionLivres = MutableStateFlow(0)
    /** Incrémenté quand les livres visibles peuvent avoir changé : la Bibliothèque se recharge. */
    val versionLivres: StateFlow<Int> = _versionLivres.asStateFlow()

    /** Campagne sélectionnée ou livres d'une campagne modifiés : contenu personnalisé à refusionner. */
    fun livresChanges() {
        GameState.mjCampaigns.value.map { it.worldId }.plus(GameState.currentWorldId()).distinct()
            .forEach { SrdRepository.invalidateWorld(it) }
        _versionLivres.value++
    }

    private fun campagnesDuMonde(worldId: String?): List<GameState.MjCampaign> =
        GameState.mjCampaigns.value.filter { worldId == null || it.worldId.isBlank() || it.worldId == worldId }

    private fun campagneCourante(): String? = GameState.currentCampaignId.value

    fun scenarioVisible(scenario: GameState.MjScenario): Boolean {
        val proprietaires = campagnesDuMonde(scenario.worldId.ifBlank { null }).filter { scenario.id in it.scenarioIds }
        return proprietaires.isEmpty() || proprietaires.any { it.id == campagneCourante() }
    }

    fun livreVisible(livreId: String, worldId: String?): Boolean {
        val proprietaires = campagnesDuMonde(worldId).filter { livreId in it.livreIds }
        return proprietaires.isEmpty() || proprietaires.any { it.id == campagneCourante() }
    }

    /** Filtre une liste de fiches : les PJ ne sont jamais concernés. */
    fun personnagesVisibles(personnages: List<Character>, worldId: String?): List<Character> {
        val campagnes = campagnesDuMonde(worldId)
        val possedes = campagnes.flatMap { it.pnjIds }.toSet()
        if (possedes.isEmpty()) return personnages
        val courante = campagnes.firstOrNull { it.id == campagneCourante() }
        val citesParScenariosVisibles by lazy { nomsCitesParScenariosVisibles(worldId) }
        return personnages.filter { p ->
            p.type == "PJ" || p.id !in possedes || (courante != null && p.id in courante.pnjIds) ||
                p.name.lowercase() in citesParScenariosVisibles
        }
    }

    /** Noms (en minuscules) des profils cités par les scénarios visibles du monde. */
    private fun nomsCitesParScenariosVisibles(worldId: String?): Set<String> =
        GameState.mjScenarios.value
            .filter { (worldId == null || it.worldId == worldId) && scenarioVisible(it) }
            .flatMap { scenario ->
                val textes = scenario.scenes.map { it.markdownContent } + scenario.markdownContent + scenario.description
                textes.flatMap { texte -> sceneProfiles(texte, emptyList()).map { it.name } } +
                    scenario.scenes.flatMap { scene -> scene.discussions.map { it.pnjName } }
            }
            .map { it.trim().lowercase() }
            .toSet()
}
