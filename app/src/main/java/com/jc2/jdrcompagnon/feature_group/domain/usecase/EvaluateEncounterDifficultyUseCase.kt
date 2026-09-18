package com.jc2.jdrcompagnon.feature_group.domain.usecase

import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_group.domain.MonsterXpMultiplier

data class EncounterEvaluation(
    val adjustedMonsterXp: Int,
    val budgets: Map<EncounterDifficulty, Int>,
    val resultingDifficulty: EncounterDifficulty?
)

/**
 * Évalue la difficulté réelle d'une rencontre déjà composée : additionne l'XP brut des
 * monstres, applique le multiplicateur lié à leur nombre, compare aux seuils du groupe.
 */
class EvaluateEncounterDifficultyUseCase(
    private val calculateGroupEncounterBudget: CalculateGroupEncounterBudgetUseCase =
        CalculateGroupEncounterBudgetUseCase()
) {
    operator fun invoke(memberLevels: List<Int>, monsterXpValues: List<Int>): EncounterEvaluation {
        val rawXp = monsterXpValues.sum()
        val multiplier = MonsterXpMultiplier.multiplierFor(
            monsterCount = monsterXpValues.size,
            combatantCount = memberLevels.size
        )
        val adjustedXp = (rawXp * multiplier).toInt()

        val budgets = EncounterDifficulty.entries.associateWith {
            calculateGroupEncounterBudget(memberLevels, it)
        }

        val resultingDifficulty = EncounterDifficulty.entries
            .sortedByDescending { it.ordinal }
            .firstOrNull { adjustedXp >= (budgets[it] ?: Int.MAX_VALUE) }

        return EncounterEvaluation(adjustedXp, budgets, resultingDifficulty)
    }
}
