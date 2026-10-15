package com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.echantillonnerSansRemise
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatNom
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

    /**
     * Tire un seul résultat : un prénom, un nom de famille et un pseudo. Chaque champ peut
     * ressortir vide (liste vide, ou chance de vide de la table) ; un résultat entièrement vide
     * est retiré, et null n'est retourné que si les trois listes sont vides. Sans effet sur le
     * déclenchement : une table de noms ne dépend jamais de l'horloge.
     */
    fun tirerNom(table: TableAleatoire, random: Random = Random(System.nanoTime())): ResultatNom? {
        val prenoms = table.prenoms.filter { it.isNotBlank() }
        val noms = table.noms.filter { it.isNotBlank() }
        val pseudos = table.pseudos.filter { it.isNotBlank() }
        if (prenoms.isEmpty() && noms.isEmpty() && pseudos.isEmpty()) return null
        fun champ(valeurs: List<String>, chanceVide: Int): String =
            if (valeurs.isEmpty() || random.nextInt(100) < chanceVide.coerceIn(0, 100)) "" else valeurs.random(random)
        repeat(20) {
            val resultat = ResultatNom(
                prenom = champ(prenoms, table.chanceVidePrenom),
                nom = champ(noms, table.chanceVideNom),
                pseudo = champ(pseudos, table.chanceVidePseudo)
            )
            if (!resultat.estVide) return resultat
        }
        // Chances de vide à 100 % partout : on ignore la chance pour garantir un résultat.
        return ResultatNom(prenom = prenoms.randomOrNull(random).orEmpty(), nom = noms.randomOrNull(random).orEmpty(), pseudo = pseudos.randomOrNull(random).orEmpty())
    }
}
