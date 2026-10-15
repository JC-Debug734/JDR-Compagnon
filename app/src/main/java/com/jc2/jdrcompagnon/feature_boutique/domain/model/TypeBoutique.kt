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
    GUILDE_MAGES(label = "Guilde des mages"),
    // Marchand itinérant : vend un petit inventaire et quelques services (repas), sans ville ;
    // le MJ le fait venir auprès des joueurs où qu'ils soient (invitation réseau).
    VENDEUR_AMBULANT(label = "Vendeur ambulant");

    /** Vend des objets (inventaire SRD) plutôt que seulement des services. */
    val vendDesObjets: Boolean get() = this == MARCHAND || this == VENDEUR_AMBULANT
}
