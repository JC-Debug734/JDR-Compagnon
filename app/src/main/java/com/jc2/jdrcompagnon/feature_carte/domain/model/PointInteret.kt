package com.jc2.jdrcompagnon.feature_carte.domain.model

/**
 * Point placé sur la carte d'une campagne. [boutiqueIds] et [scenarioIds] n'ont de sens que pour
 * [TypePointInteret.VILLE] (boutiques liées depuis feature_boutique via Boutique.villeId,
 * scénarios liés depuis GameState.mjScenarios), mais restent des champs génériques plutôt qu'une
 * hiérarchie de sous-types pour un seul cas d'usage.
 */
data class PointInteret(
    val id: String,
    val campagneId: String,
    val nom: String,
    val type: TypePointInteret,
    val x: Int,
    val y: Int,
    val description: String = "",
    val boutiqueIds: List<String> = emptyList(),
    val scenarioIds: List<String> = emptyList()
)
