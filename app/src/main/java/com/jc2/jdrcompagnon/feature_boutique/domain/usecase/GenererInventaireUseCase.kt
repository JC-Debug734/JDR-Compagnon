package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.estConsommable
import kotlin.random.Random

/**
 * Génère l'inventaire d'une boutique à partir des équipements disponibles pour le monde actif.
 *
 * Règle de rareté par standing :
 *  - MODESTE / CORRECT : uniquement objets non-magiques (coût de base modeste)
 *  - PROSPERE : ajoute des objets de coût moyen/élevé
 *  - LUXUEUX : accès à tout, y compris objets magiques/artefacts
 *
 * ⚠️ Le seuil de "coût élevé" (500 po) est un choix par défaut à ajuster selon l'équilibrage souhaité.
 */
class GenererInventaireUseCase(
    private val equipementSource: EquipementSourcePort,
    private val calculerPrixArticle: CalculerPrixArticleUseCase
) {
    private companion object {
        const val SEUIL_COUT_ELEVE_PO = 500
        const val NB_ARTICLES_PAR_DEFAUT = 8
        const val POIDS_CONSOMMABLE = 3.0
        const val POIDS_STANDARD = 1.0
    }

    suspend operator fun invoke(
        standing: StandingBoutique,
        monde: String,
        nbArticles: Int = NB_ARTICLES_PAR_DEFAUT,
        exclureNoms: Set<String> = emptySet(),
        random: Random = Random.Default
    ): List<ArticleEnVente> {
        val tousLesEquipements = equipementSource.getEquipementsDisponibles(monde)
            .filter { it.nom !in exclureNoms }

        val equipementsAccessibles = when (standing) {
            StandingBoutique.MODESTE, StandingBoutique.CORRECT ->
                tousLesEquipements.filter { it.coutBaseEnPo < SEUIL_COUT_ELEVE_PO }
            StandingBoutique.PROSPERE ->
                tousLesEquipements
            StandingBoutique.LUXUEUX ->
                tousLesEquipements
        }

        if (equipementsAccessibles.isEmpty()) return emptyList()

        return equipementsAccessibles
            .echantillonnerSansRemise(
                n = nbArticles,
                poids = { if (it.estConsommable()) POIDS_CONSOMMABLE else POIDS_STANDARD },
                random = random
            )
            .map { equipement ->
                ArticleEnVente(
                    equipement = equipement,
                    prixApplique = calculerPrixArticle(equipement.coutBaseEnPo, standing),
                    quantiteStock = random.nextInt(1, 6)
                )
            }
    }
}