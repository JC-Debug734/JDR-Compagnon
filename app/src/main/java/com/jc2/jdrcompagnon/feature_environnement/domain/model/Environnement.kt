package com.jc2.jdrcompagnon.feature_environnement.domain.model

/** Un objet de la table de butin d'un environnement, avec son poids de tirage. */
data class LootEntry(
    val nomObjet: String,
    val poids: Int = 1
)

/**
 * Un environnement (forêt, donjon, ville...) rattachable à un monstre du bestiaire (via
 * [com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry.environments]) ou à une scène de
 * scénario ([com.jc2.jdrcompagnon.ui.GameState.MjScene.environmentId]). Regroupe tout ce que
 * le MJ veut préparer à l'avance pour un lieu type : ambiance sonore, rumeurs, rencontres
 * aléatoires, bestiaire et table de butin (les épreuves ont leur propre outil, ÉPREUVES).
 */
data class Environnement(
    val id: String = java.util.UUID.randomUUID().toString(),
    val nom: String,
    val description: String = "",
    val worldId: String = "",
    val imageFileName: String? = null,
    val musicTrackId: String? = null,
    // Anciennes rumeurs/rencontres en texte libre : à l'écriture, le repository les convertit en
    // événements de la bibliothèque ajoutés à [evenementIds] ; toujours vides à la lecture.
    val rumeurs: List<String> = emptyList(),
    val rencontresAleatoires: List<String> = emptyList(),
    // Événements de la bibliothèque (feature_evenement) rattachés à ce lieu type.
    val evenementIds: List<String> = emptyList(),
    // Monstres ajoutés manuellement par le MJ, en plus de ceux suggérés automatiquement à
    // partir de SrdEntry.environments (voir SrdRepository.searchMonsters).
    val monstresIds: List<String> = emptyList(),
    val tableButin: List<LootEntry> = emptyList(),
    // Types de terrain SRD (voir TERRAINS_SRD) filtrant le bestiaire suggéré ; vide = devinés
    // depuis le nom (voir terrainsEffectifs).
    val terrains: List<String> = emptyList(),
    // Tables aléatoires (feature_table_aleatoire) liées à ce lieu, tirées depuis l'environnement.
    val tablesAleatoiresIds: List<String> = emptyList()
)
