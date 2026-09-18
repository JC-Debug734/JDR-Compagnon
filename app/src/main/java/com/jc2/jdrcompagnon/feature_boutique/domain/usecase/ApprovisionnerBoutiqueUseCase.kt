package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.estConsommable
import kotlin.random.Random

/**
 * Critères de sélection pour un réapprovisionnement en masse.
 * [types] filtre sur EquipementReference.type (ensemble vide = toutes catégories confondues,
 * sinon un objet est retenu si son type figure dans l'ensemble — sélection multiple).
 * [prixMaxEnPo] filtre sur le coût de base SRD, avant application du multiplicateur de
 * standing (null = pas de plafond).
 */
data class FiltresApprovisionnement(
    val types: Set<String> = emptySet(),
    val prixMaxEnPo: Int? = null,
    val nombreArticlesMax: Int = 8
)

/**
 * Réapprovisionne une boutique selon des critères choisis par le MJ (catégorie, prix
 * plafond, nombre d'articles) et un budget disponible : pioche dans le catalogue SRD tant
 * que le budget le permet, jusqu'à atteindre nombreArticlesMax. Le budget est comparé au
 * coût de base (avant marge du standing), pour rester une contrainte lisible et prévisible
 * pour le MJ plutôt que de varier selon le prix de vente final.
 */
class ApprovisionnerBoutiqueUseCase(
    private val equipementSource: EquipementSourcePort,
    private val calculerPrixArticle: CalculerPrixArticleUseCase
) {
    suspend operator fun invoke(
        monde: String,
        standing: StandingBoutique,
        filtres: FiltresApprovisionnement,
        budgetEnPo: Int,
        random: Random = Random.Default
    ): List<ArticleEnVente> {
        val candidats = equipementSource.getEquipementsDisponibles(monde)
            .filter { filtres.types.isEmpty() || it.type in filtres.types }
            .filter { filtres.prixMaxEnPo == null || it.coutBaseEnPo <= filtres.prixMaxEnPo }
            .let { pool ->
                pool.echantillonnerSansRemise(
                    n = pool.size,
                    poids = { if (it.estConsommable()) 3.0 else 1.0 },
                    random = random
                )
            }

        val articlesChoisis = mutableListOf<ArticleEnVente>()
        var budgetRestant = budgetEnPo

        for (equipement in candidats) {
            if (articlesChoisis.size >= filtres.nombreArticlesMax) break
            if (equipement.coutBaseEnPo > budgetRestant) continue

            articlesChoisis += ArticleEnVente(
                equipement = equipement,
                prixApplique = calculerPrixArticle(equipement.coutBaseEnPo, standing),
                quantiteStock = random.nextInt(1, 4)
            )
            budgetRestant -= equipement.coutBaseEnPo
        }

        return articlesChoisis
    }
}