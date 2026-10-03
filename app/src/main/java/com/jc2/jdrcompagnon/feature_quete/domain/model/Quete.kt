package com.jc2.jdrcompagnon.feature_quete.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class QuestStatus(val label: String) {
    EN_COURS("En cours"),
    // Quête mise de côté : masquée du menu latéral du MJ et non validable tant qu'elle n'est pas reprise.
    EN_ATTENTE("En attente"),
    TERMINEE("Terminée"),
    ECHOUEE("Échouée"),
}

/**
 * Type de récompense d'une quête, et ce que devient chaque type à la validation (voir
 * ValiderQueteUseCase) :
 * - XP et OR : répartis équitablement entre les joueurs du groupe (reste de l'or au trésor commun) ;
 * - REPUTATION : modifie la réputation du groupe auprès de la faction [QuestReward.label] ;
 * - PNJ : le PNJ rejoint le groupe ;
 * - tout le reste entre dans les possessions du groupe (inventaire, montures, véhicules, biens).
 */
@Serializable
enum class QuestRewardType(val label: String) {
    XP("Expérience"),
    OR("Or"),
    EQUIPEMENT("Équipement"),
    REPUTATION("Réputation"),
    PNJ("PNJ"),
    MONTURE("Monture"),
    ANIMAL("Animal"),
    VEHICULE("Véhicule"),
    BIEN("Bien"),
}

/**
 * Une récompense. Selon [type] :
 * - XP / OR : [amount] ;
 * - EQUIPEMENT : [label] = objet, [amount] = quantité ;
 * - REPUTATION : [label] = faction, [amount] = variation (négative possible) ;
 * - PNJ : [characterId] (+ [label] = nom, pour l'affichage si la fiche disparaît) ;
 * - MONTURE / ANIMAL : [label] = nom, [detail] = espèce, [amount] = vitesse en mètres ;
 * - VEHICULE : [label] = nom, [detail] = type ;
 * - BIEN : [label] = nom, [detail] = description.
 */
@Serializable
data class QuestReward(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: QuestRewardType,
    val label: String = "",
    val amount: Int = 0,
    val detail: String = "",
    val characterId: String? = null,
) {
    /** Libellé court, partagé par le MJ et les joueurs. */
    val resume: String
        get() = when (type) {
            QuestRewardType.XP -> "$amount XP"
            QuestRewardType.OR -> "$amount po"
            QuestRewardType.EQUIPEMENT -> if (amount > 1) "$label ×$amount" else label
            QuestRewardType.REPUTATION -> "Réputation ${if (amount >= 0) "+" else ""}$amount — $label"
            QuestRewardType.PNJ -> "Allié : $label"
            QuestRewardType.MONTURE, QuestRewardType.ANIMAL ->
                "${type.label} : $label" + (detail.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "")
            QuestRewardType.VEHICULE -> "Véhicule : $label"
            QuestRewardType.BIEN -> "Bien : $label"
        }
}

/**
 * Quête d'une campagne (remplace l'ancienne fiche de suivi). [description] est le texte montré
 * aux joueurs quand [visibleToPlayers] est vrai ; [mjNotes] reste toujours réservé au MJ.
 */
@Serializable
data class Quest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val mjNotes: String = "",
    val location: String = "",
    // PNJ qui a confié la quête (Character.id), null = aucun.
    val giverCharacterId: String? = null,
    val status: QuestStatus = QuestStatus.EN_COURS,
    val visibleToPlayers: Boolean = false,
    // Les joueurs voient-ils les récompenses promises avant la fin de la quête ?
    val rewardsVisibleToPlayers: Boolean = false,
    val rewards: List<QuestReward> = emptyList(),
    // Renseigné à la validation : groupe récompensé (les récompenses ne sont données qu'une fois).
    val rewardedGroupId: String? = null,
    // Compte rendu de la distribution, affiché ensuite sur la quête.
    val rewardSummary: String = "",
)
