package com.jc2.jdrcompagnon.feature_group.domain.usecase

import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_group.domain.XpThresholdTable

/**
 * Calcule le budget d'XP total que le groupe peut absorber pour une difficulté donnée.
 * Ne dépend d'aucune librairie Android ni de GameState (Règle B) : prend directement les
 * niveaux des combattants (résolus par l'appelant depuis MjGroup + Character), 100% testable.
 */
class CalculateGroupEncounterBudgetUseCase {
    operator fun invoke(memberLevels: List<Int>, difficulty: EncounterDifficulty): Int =
        memberLevels.sumOf { level -> XpThresholdTable.thresholdFor(level, difficulty) }
}