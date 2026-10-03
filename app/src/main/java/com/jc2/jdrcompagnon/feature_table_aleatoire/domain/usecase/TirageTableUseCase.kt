package com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.echantillonnerSansRemise
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import kotlin.random.Random

/**
 * Logique de tirage pondéré d'une table aléatoire, partagée entre TableAleatoireDetailViewModel
 * (outil dédié) et ScenarioReaderContent (tirage rapide depuis la lecture du scénario) : les
 * deux points d'entrée doivent tirer et acquitter le déclenchement de la même façon. Pure (pas
 * d'accès repository ici) : chaque fonction retourne la table mise à jour (déclenchement
 * acquitté) à charge de l'appelant de la persister.
 */
class TirageTableUseCase {

    /** Les entrées dont l'événement a été supprimé de la bibliothèque sont ignorées. */
    fun tirerEvenement(
        table: TableAleatoire,
        typeFiltre: TypeEvenement?,
        minutesLecture: Long?, // LectureScenarioState.minutesEcoulees (null hors lecture)
        random: Random = Random(System.nanoTime())
    ): Pair<TableAleatoire, Evenement?> {
        val pool = table.entreesEvenements.filter { entree ->
            val evenement = entree.evenement
            evenement != null && (typeFiltre == null || evenement.type == typeFiltre)
        }
        val resultat = pool.echantillonnerSansRemise(n = 1, poids = { it.poids.toDouble() }, random = random)
            .firstOrNull()?.evenement
        val tableMiseAJour = if (resultat != null) table.copy(derniereDeclenchementMinutes = minutesLecture) else table
        return tableMiseAJour to resultat
    }

    fun tirerLoot(
        table: TableAleatoire,
        n: Int,
        minutesLecture: Long?, // LectureScenarioState.minutesEcoulees (null hors lecture)
        random: Random = Random(System.nanoTime())
    ): Pair<TableAleatoire, List<ResultatLoot>> {
        if (table.entreesLoot.isEmpty()) return table to emptyList()
        val tirage = table.entreesLoot.echantillonnerSansRemise(
            n = n.coerceAtMost(table.entreesLoot.size),
            poids = { it.poids.toDouble() },
            random = random
        )
        val tableMiseAJour = table.copy(derniereDeclenchementMinutes = minutesLecture)
        val resultats = tirage.map { entree ->
            val quantite = if (entree.quantiteMax > entree.quantiteMin) {
                random.nextInt(entree.quantiteMin, entree.quantiteMax + 1)
            } else {
                entree.quantiteMin
            }
            ResultatLoot(entree, quantite)
        }
        return tableMiseAJour to resultats
    }
}
