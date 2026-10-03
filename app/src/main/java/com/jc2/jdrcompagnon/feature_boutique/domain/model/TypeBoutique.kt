package com.jc2.jdrcompagnon.feature_boutique.domain.model

import kotlinx.serialization.Serializable

/**
 * Type de boutique. MARCHAND vend de l'inventaire SRD (comportement historique).
 * Les autres types rendent des services générés (voir GenererServicesUseCase) au lieu de
 * vendre des objets.
 */
@Serializable
enum class TypeBoutique(val label: String) {
    MARCHAND(label = "Marchand"),
    AUBERGE(label = "Auberge"),
    TEMPLE(label = "Temple"),
    GUILDE_RECRUTEMENT(label = "Guilde de recrutement"),
    ECURIE(label = "Écurie"),
    BANQUE(label = "Banque"),
    GUILDE_MAGES(label = "Guilde des mages")
}
