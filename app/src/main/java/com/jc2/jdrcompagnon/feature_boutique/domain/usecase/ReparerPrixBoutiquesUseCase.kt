package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Répare les prix à 0 des boutiques créées avant le passage des prix en pièces de cuivre
 * (version 29) : les prix inférieurs à 1 po avaient alors été arrondis à 0 et s'affichaient
 * « Gratuit ». Un article retrouve le coût SRD de son objet (ajusté au standing) ; un service
 * généré retrouve son prix de catalogue. Les services gratuits par nature (don au temple, change)
 * restent à 0. Sans effet sur les boutiques déjà correctes.
 */
class ReparerPrixBoutiquesUseCase(
    private val repository: BoutiqueRepository,
    private val equipementSource: EquipementSourcePort,
    private val calculerPrixArticle: CalculerPrixArticleUseCase,
    private val genererServices: GenererServicesUseCase,
) {
    suspend operator fun invoke(mondes: List<String>) {
        val references = mondes.distinct().flatMap { runCatching { equipementSource.getEquipementsDisponibles(it) }.getOrDefault(emptyList()) }
            .associateBy { it.nom.lowercase() }
        repository.observerToutesLesBoutiques().first().forEach { boutique ->
            var modifiee = false
            val inventaire = boutique.inventaire.map { article ->
                if (article.prixEnPc > 0 && article.equipement.coutBaseEnPc > 0) return@map article
                val ref = references[article.equipement.nom.lowercase()] ?: return@map article
                modifiee = true
                article.copy(
                    equipement = article.equipement.copy(coutBaseEnPc = ref.coutBaseEnPc),
                    prixEnPc = if (article.prixEnPc > 0) article.prixEnPc else calculerPrixArticle(ref.coutBaseEnPc, boutique.standing),
                )
            }
            val services = boutique.services.map { service ->
                if (service.prixEnPc > 0) return@map service
                val base = genererServices.prixDeBase(service.nom)?.takeIf { it > 0 } ?: return@map service
                modifiee = true
                service.copy(prixEnPc = (base * boutique.standing.multiplicateurPrix).roundToInt().coerceAtLeast(1))
            }
            if (modifiee) repository.sauvegarderBoutique(boutique.copy(inventaire = inventaire, services = services))
        }
    }
}
