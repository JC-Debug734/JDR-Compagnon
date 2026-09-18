package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import kotlin.math.roundToInt

/**
 * Applique la marge du standing au coût de base SRD d'un objet.
 * Le prix de base (source de vérité = fichier SRD) n'est jamais dupliqué :
 * seul ce prix ajusté est persisté sur ArticleEnVente.prixApplique.
 */
class CalculerPrixArticleUseCase {

    operator fun invoke(coutBaseEnPo: Int, standing: StandingBoutique): Int {
        return (coutBaseEnPo * standing.multiplicateurPrix).roundToInt().coerceAtLeast(1)
    }
}