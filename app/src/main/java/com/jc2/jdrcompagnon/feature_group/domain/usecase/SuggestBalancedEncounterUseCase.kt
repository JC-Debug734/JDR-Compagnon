package com.jc2.jdrcompagnon.feature_group.domain.usecase

import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty

data class EncounterSuggestion(
    val difficulty: EncounterDifficulty,
    val totalXpBudget: Int,
    val recommendedMonsterCount: IntRange
)

/**
 * Propose un budget d'XP cible et une fourchette de nombre de monstres,
 * pour composer une rencontre équilibrée sans calcul manuel côté MJ.
 */
class SuggestBalancedEncounterUseCase(
    private val calculateGroupEncounterBudget: CalculateGroupEncounterBudgetUseCase =
        CalculateGroupEncounterBudgetUseCase()
) {
    operator fun invoke(memberLevels: List<Int>, difficulty: EncounterDifficulty): EncounterSuggestion {
        val budget = calculateGroupEncounterBudget(memberLevels, difficulty)
        val combatantCount = memberLevels.size.coerceAtLeast(1)
        val recommended = 1..(combatantCount + 2)
        return EncounterSuggestion(difficulty, budget, recommended)
    }
}
