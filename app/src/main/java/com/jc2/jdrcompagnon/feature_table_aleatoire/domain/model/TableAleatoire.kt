package com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model

import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import java.util.UUID

/** Nature du contenu d'une table aléatoire : détermine quelle liste d'entrées est utilisée. */
enum class TypeTable(val label: String) {
    EVENEMENTS("Événements"),
    LOOT("Loot")
}

/**
 * Une entrée d'une table d'événements : référence un événement de la bibliothèque
 * (feature_evenement) avec son poids de tirage (voir echantillonnerSansRemise). Seuls
 * [evenementId] et [poids] sont stockés ; [evenement] est résolu par le repository à la lecture,
 * null si l'événement a été supprimé de la bibliothèque (entrée alors ignorée au tirage).
 */
data class EntreeEvenement(
    val id: String = UUID.randomUUID().toString(),
    val evenementId: String,
    val poids: Int = 1,
    val evenement: Evenement? = null
)

/**
 * Une entrée d'une table de loot : référence un équipement du SRD par nom (même principe que
 * feature_boutique.EquipementReference/feature_environnement.LootEntry), avec un poids de tirage
 * et une quantité tirée entre [quantiteMin] et [quantiteMax] bornes incluses.
 */
data class EntreeLoot(
    val id: String = UUID.randomUUID().toString(),
    val equipementNom: String,
    val poids: Int = 1,
    val quantiteMin: Int = 1,
    val quantiteMax: Int = 1
)

/**
 * Une table aléatoire (événements ou loot) que le MJ configure avec un intervalle en heures de
 * jeu. [derniereDeclenchementMinutes] mémorise le temps de lecture du scénario
 * (LectureScenarioState.minutesEcoulees) au dernier tirage acquitté ; null tant que la table n'a
 * pas été tirée depuis l'ouverture du scénario, auquel cas son intervalle court depuis cette
 * ouverture (voir VerifierDeclenchementTableUseCase). Une table de type LOOT peut aussi être tirée
 * manuellement sans jamais dépendre de l'horloge (voir TableAleatoireDetailScreen).
 */
data class TableAleatoire(
    val id: String = UUID.randomUUID().toString(),
    val nom: String,
    val worldId: String = "",
    val type: TypeTable = TypeTable.EVENEMENTS,
    val intervalleHeures: Int = 4,
    val active: Boolean = true,
    val derniereDeclenchementMinutes: Long? = null,
    val entreesEvenements: List<EntreeEvenement> = emptyList(),
    val entreesLoot: List<EntreeLoot> = emptyList()
) {
    /** Nombre d'entrées de la liste effectivement utilisée par [type], pour l'affichage liste. */
    val nombreEntrees: Int
        get() = when (type) {
            TypeTable.EVENEMENTS -> entreesEvenements.size
            TypeTable.LOOT -> entreesLoot.size
        }
}
