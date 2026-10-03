package com.jc2.jdrcompagnon.feature_carte.domain.model

import kotlinx.serialization.Serializable

/**
 * Point placé sur la carte d'une campagne. [boutiqueIds] et [scenarioIds] n'ont de sens que pour
 * [TypePointInteret.VILLE] (boutiques liées depuis feature_boutique via Boutique.villeId,
 * scénarios liés depuis GameState.mjScenarios), mais restent des champs génériques plutôt qu'une
 * hiérarchie de sous-types pour un seul cas d'usage.
 */
@Serializable
data class PointInteret(
    val id: String,
    val campagneId: String,
    val nom: String,
    val type: TypePointInteret,
    val x: Int,
    val y: Int,
    val description: String = "",
    val boutiqueIds: List<String> = emptyList(),
    val scenarioIds: List<String> = emptyList(),
    // Personnalisation visuelle du marqueur sur la carte : iconKey référence une clé de
    // IconesPointInteret.PALETTE (feature_carte/ui), null/inconnue = icône par défaut selon [type].
    // couleurArgb (packé comme un android.graphics.Color) : null = couleur par défaut selon [type].
    val iconKey: String? = null,
    val couleurArgb: Int? = null,
    // Carte de la campagne où le MJ a placé ce lieu (voir CarteCampagne) : null = non placé.
    val carteId: String? = null,
    // Position libre du centre de l'icône, en fraction de la largeur/hauteur de la carte (0..1) :
    // le lieu n'est plus attaché à une case. null = ancien point, centré sur la case (x, y).
    val fx: Float? = null,
    val fy: Float? = null,
    // Lieu montré aux joueurs sur la carte d'exploration (page table), choix du MJ.
    val visibleJoueurs: Boolean = false,
    // Événements de la bibliothèque (feature_evenement) rattachés à ce lieu, quel que soit son type.
    val evenementIds: List<String> = emptyList(),
)
