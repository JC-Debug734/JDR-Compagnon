package com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase

import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire

/**
 * Détermine si une table doit se déclencher au temps de lecture [minutesLecture] (voir
 * LectureScenarioState.minutesEcoulees : temps de scénario compté uniquement pendant la lecture
 * d'un scénario, remis à zéro à son ouverture). [minutesLecture] null = aucun scénario en
 * lecture : rien ne se déclenche. Sinon on regarde si l'intervalle de la table (en heures) a été
 * franchi depuis son dernier déclenchement acquitté par le MJ, ou depuis l'ouverture du scénario
 * si elle n'a pas encore été tirée. S'applique aux deux types de table (événements et loot).
 */
class VerifierDeclenchementTableUseCase {

    operator fun invoke(table: TableAleatoire, minutesLecture: Long?): Boolean {
        if (minutesLecture == null || !table.active || table.nombreEntrees == 0) return false
        return minutesLecture - depart(table, minutesLecture) >= intervalleMinutes(table)
    }

    /** Minutes restantes avant le prochain déclenchement (0 si déjà dû ou hors lecture). */
    fun minutesRestantes(table: TableAleatoire, minutesLecture: Long?): Long {
        if (minutesLecture == null) return 0
        return (intervalleMinutes(table) - (minutesLecture - depart(table, minutesLecture))).coerceAtLeast(0)
    }

    // Une valeur supérieure au compteur provient d'une lecture précédente : on repart de zéro.
    private fun depart(table: TableAleatoire, minutesLecture: Long): Long =
        table.derniereDeclenchementMinutes?.takeIf { it <= minutesLecture } ?: 0

    private fun intervalleMinutes(table: TableAleatoire): Long = table.intervalleHeures.coerceAtLeast(1) * 60L
}
