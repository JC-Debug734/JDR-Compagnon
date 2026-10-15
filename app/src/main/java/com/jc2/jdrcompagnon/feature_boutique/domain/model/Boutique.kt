package com.jc2.jdrcompagnon.feature_boutique.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Marchand(
    val nom: String,
    val description: String = "",
    val trait: String = "" // courte description de caractère générée (ex: "gentil et peu enclin à la négociation")
)

@Serializable
data class Boutique(
    val id: String,
    val nom: String,
    val standing: StandingBoutique,
    val marchand: Marchand,
    val employes: List<Employe>,
    val inventaire: List<ArticleEnVente>,
    val type: TypeBoutique = TypeBoutique.MARCHAND,
    val services: List<Service> = emptyList(),
    val argentDisponibleEnPc: Int = 0, // caisse du marchand, en pièces de cuivre
    val villeId: String? = null, // lien optionnel vers un PointInteret (feature_carte) de type VILLE
    // Campagne de la boutique (celle en cours à sa création) ; null = commune à toutes les campagnes.
    val campagneId: String? = null,
    // Image de la boutique (miniature de la liste) : "enseigne:<clé>" pour une enseigne fournie avec
    // l'app, sinon nom du fichier copié en stockage interne (voir BoutiqueImageStore) ; null = aucune.
    val image: String? = null
)
