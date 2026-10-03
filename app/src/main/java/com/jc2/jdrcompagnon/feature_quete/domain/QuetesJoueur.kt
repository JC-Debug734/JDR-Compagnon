package com.jc2.jdrcompagnon.feature_quete.domain

import com.jc2.jdrcompagnon.feature_quete.domain.model.Quest
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus
import com.jc2.jdrcompagnon.network.QuestJoueurData
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

/** Ce que les joueurs voient des quêtes : celles que le MJ a rendues visibles, sans ses notes. */
object QuetesJoueur {

    fun pourJoueurs(campaign: GameState.MjCampaign?, characters: List<Character>): List<QuestJoueurData> =
        campaign?.quests.orEmpty()
            .filter { it.visibleToPlayers }
            .map { it.versJoueur(characters) }

    private fun Quest.versJoueur(characters: List<Character>) = QuestJoueurData(
        id = id,
        title = title,
        description = description,
        location = location,
        status = status.name,
        giverName = giverCharacterId?.let { id -> characters.firstOrNull { it.id == id }?.name },
        rewards = if (rewardsVisibleToPlayers || status == QuestStatus.TERMINEE) rewards.map { it.resume } else emptyList(),
    )

    /** Quêtes visibles de la campagne sélectionnée sur cet appareil (MJ et joueurs sur le même appareil). */
    fun locales(): List<QuestJoueurData> {
        val id = GameState.currentCampaignId.value ?: return emptyList()
        return pourJoueurs(GameState.mjCampaigns.value.firstOrNull { it.id == id }, GameState.characters.value)
    }
}
