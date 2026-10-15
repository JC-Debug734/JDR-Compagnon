package com.jc2.jdrcompagnon.feature_preparation.domain

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.INTERNAL_LINK_PATTERN

/**
 * Liens inverses d'un PNJ : tout ce qui le cite dans son monde (scènes des scénarios, quêtes des
 * campagnes, et dans le carnet du MJ : secrets, fils en suspens, préparation de séance). Calcul
 * à la volée, rien n'est stocké : renommer le PNJ ou modifier un scénario suffit à mettre la
 * liste à jour.
 */
object ApparitionsPnj {

    sealed class Apparition {
        /** Scène (ou introduction si [sceneTitre] est null) d'un scénario. [parLien] : lien #pnj/#event/#perso, sinon nom cité. */
        data class Scene(val scenarioId: String, val scenarioTitre: String, val sceneTitre: String?, val parLien: Boolean) : Apparition()
        data class Quete(val campagneId: String, val titre: String, val role: String) : Apparition()
        data class Secret(val secret: com.jc2.jdrcompagnon.feature_preparation.domain.model.SecretIndice) : Apparition()
        data class Fil(val fil: com.jc2.jdrcompagnon.feature_preparation.domain.model.FilEnSuspens) : Apparition()
        data class Seance(val numero: Int) : Apparition()
        data class Faction(val factionId: String, val nom: String, val rang: String, val chef: Boolean) : Apparition()
    }

    private val typesPersonnage = setOf("pnj", "npc", "event", "perso")
    private val lienRegex = Regex(INTERNAL_LINK_PATTERN)

    private fun citeParLien(texte: String, nom: String): Boolean = lienRegex.findAll(texte).any { m ->
        m.groupValues[1] in typesPersonnage && m.groupValues[2].ifEmpty { m.groupValues[3] }.trim().equals(nom, ignoreCase = true)
    }

    /** Nom cité en toutes lettres (mot entier) ; ignoré pour les noms trop courts, source de faux positifs. */
    private fun citeEnTexte(texte: String, nom: String): Boolean =
        nom.length >= 3 && Regex("(?<![\\p{L}\\p{N}])${Regex.escape(nom)}(?![\\p{L}\\p{N}])", RegexOption.IGNORE_CASE).containsMatchIn(texte)

    fun trouver(
        pnj: Character,
        campagnes: List<GameState.MjCampaign>,
        scenarios: List<GameState.MjScenario>,
        carnets: Map<String, com.jc2.jdrcompagnon.feature_preparation.domain.model.CarnetMonde>,
        factions: List<com.jc2.jdrcompagnon.feature_faction.domain.model.Faction> = emptyList(),
    ): List<Apparition> {
        val nom = pnj.name.trim()
        if (nom.isEmpty()) return emptyList()
        val resultat = mutableListOf<Apparition>()

        // Factions dont il est membre (en premier : c'est ce qui le situe le mieux).
        factions.forEach { fac ->
            fac.membres.firstOrNull { it.pnjId == pnj.id }?.let { m -> resultat += Apparition.Faction(fac.id, fac.nom, m.rang, m.chef) }
        }

        scenarios
            .filter { it.worldId == pnj.worldId || it.worldId.isBlank() || pnj.worldId.isBlank() }
            .let { ChapitresScenarios.trier(it) }
            .forEach { scenario ->
                val titre = ChapitresScenarios.titreNumerote(scenario)
                val textes = listOf<Pair<String?, String>>(null to scenario.markdownContent) +
                    scenario.scenes.sortedBy { it.order }.map { it.title to it.markdownContent }
                textes.forEach { (scene, texte) ->
                    when {
                        citeParLien(texte, nom) -> resultat += Apparition.Scene(scenario.id, titre, scene, parLien = true)
                        citeEnTexte(texte, nom) -> resultat += Apparition.Scene(scenario.id, titre, scene, parLien = false)
                    }
                }
            }

        campagnes
            .filter { it.worldId == pnj.worldId || it.worldId.isBlank() || pnj.worldId.isBlank() }
            .forEach { c ->
                c.quests.forEach { q ->
                    val role = when {
                        q.giverCharacterId == pnj.id -> "confie la quête"
                        q.rewards.any { it.characterId == pnj.id } -> "rejoint le groupe en récompense"
                        citeEnTexte(q.description + "\n" + q.mjNotes, nom) -> "cité dans la quête"
                        else -> null
                    }
                    if (role != null) resultat += Apparition.Quete(c.id, q.title, role)
                }
            }

        // Carnet du MJ : celui du monde du PNJ (tous les carnets pour un PNJ sans monde).
        carnets
            .filterKeys { pnj.worldId.isBlank() || it == pnj.worldId || it.isBlank() }
            .values
            .forEach { carnet ->
                carnet.secrets.filter { pnj.id in it.pnjIds }.forEach { resultat += Apparition.Secret(it) }
                carnet.fils.filter { !it.resolu && pnj.id in it.pnjIds }.forEach { resultat += Apparition.Fil(it) }
                if (carnet.preparation.agenda.any { it.type == com.jc2.jdrcompagnon.feature_preparation.domain.model.TypeAgenda.PNJ && it.refId == pnj.id }) {
                    resultat += Apparition.Seance(carnet.preparation.numero)
                }
            }
        return resultat
    }
}
