package com.jc2.jdrcompagnon.feature_carte.domain.model

/**
 * [campagneId] null = événement générique fourni par l'app (non stocké en base, voir
 * GenererEvenementsGeneriquesUseCase) ; non-null = événement personnalisé écrit par le MJ pour
 * cette campagne, persisté. [villeId] optionnel : événement rattaché à une ville précise plutôt
 * qu'à la campagne entière (nécessite alors un campagneId non nul).
 */
data class EvenementAleatoire(
    val id: String,
    val campagneId: String?,
    val villeId: String? = null,
    val titre: String,
    val description: String,
    val effets: List<EffetEvenement> = emptyList()
)
