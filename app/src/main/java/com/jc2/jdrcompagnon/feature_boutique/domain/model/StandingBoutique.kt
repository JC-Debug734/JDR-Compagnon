package com.jc2.jdrcompagnon.feature_boutique.domain.model

/**
 * Niveau de standing d'une boutique. Détermine :
 *  - la marge appliquée sur le prix de base des articles (voir CalculerPrixArticleUseCase)
 *  - la rareté des objets accessibles à la génération d'inventaire (voir GenererInventaireUseCase)
 *  - le nombre et les rôles des employés générés (voir GenererEmployesUseCase)
 */
enum class StandingBoutique(val label: String, val multiplicateurPrix: Double) {
    MODESTE(label = "Modeste", multiplicateurPrix = 0.9),
    CORRECT(label = "Correct", multiplicateurPrix = 1.0),
    PROSPERE(label = "Prospère", multiplicateurPrix = 1.25),
    LUXUEUX(label = "Luxueux", multiplicateurPrix = 1.6);

    companion object {
        /** Pondération utilisée par GenererStandingAleatoireUseCase (doit sommer à 100). */
        val poidsTirageAleatoire: Map<StandingBoutique, Int> = mapOf(
            MODESTE to 40,
            CORRECT to 35,
            PROSPERE to 20,
            LUXUEUX to 5
        )
    }
}