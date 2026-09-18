package com.jc2.jdrcompagnon.feature_boutique.domain.model

/**
 * Référence légère vers un objet de la bibliothèque SRD existante (equipement_srd521.md).
 *
 * ⚠️ À ADAPTER : ce type est un sous-ensemble minimal (nom + coût de base) découplé
 * volontairement de la classe Equipement réelle du module SRD, pour ne pas deviner son API.
 * Étape d'intégration : remplacer ce type par la vraie classe Equipement du projet,
 * ou mapper Equipement -> EquipementReference dans data/mapper/EquipementMapper.kt.
 */
data class EquipementReference(
    val nom: String,
    val coutBaseEnPo: Int,
    val type: String // Arme, Armure, Outil, etc. (cf. champ **Type** du SRD)
)

data class ArticleEnVente(
    val equipement: EquipementReference,
    val prixApplique: Int,   // prix de base ajusté par le standing de la boutique
    val quantiteStock: Int,
    val toujoursDisponible: Boolean = false // si true, jamais retiré lors d'une "nouvelle visite"
)

/**
 * Heuristique de détection des consommables, utilisée pour les favoriser lors de la génération
 * d'inventaire (potions, munitions, rations...). ⚠️ Liste de mots-clés à ajuster selon le
 * catalogue SRD réellement utilisé — pas de catégorie "consommable" dédiée dans les données SRD.
 */
private val MOTS_CLES_CONSOMMABLES = listOf(
    "potion", "ration", "parchemin", "flèche", "carreau", "huile", "torche", "bougie",
    "corde", "antipoison", "poison", "encens", "bandage"
)

fun EquipementReference.estConsommable(): Boolean =
    type.equals("Munition", ignoreCase = true) ||
        MOTS_CLES_CONSOMMABLES.any { nom.contains(it, ignoreCase = true) }