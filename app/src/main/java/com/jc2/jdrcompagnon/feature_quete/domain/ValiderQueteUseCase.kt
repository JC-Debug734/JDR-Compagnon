package com.jc2.jdrcompagnon.feature_quete.domain

import com.jc2.jdrcompagnon.feature_carte.presentation.appliquerReputationAuGroupe
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupAsset
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupItem
import com.jc2.jdrcompagnon.feature_group.domain.model.Mount
import com.jc2.jdrcompagnon.feature_group.domain.model.MountKind
import com.jc2.jdrcompagnon.feature_group.domain.model.Transport
import com.jc2.jdrcompagnon.feature_quete.domain.model.Quest
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestRewardType
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Partage des récompenses d'une quête entre les joueurs d'un groupe : ses PJ (fiches) et ses
 * joueurs sans fiche comptent chacun pour une part. L'XP est divisée à l'entier inférieur ; le
 * reste de l'or (non divisible) va au trésor commun du groupe.
 */
data class RepartitionQuete(
    val pj: List<Character>,
    val joueursSansFiche: List<GameState.TablePlayer>,
    val xpTotal: Int,
    val orTotal: Int,
) {
    val parts: Int get() = pj.size + joueursSansFiche.size
    val xpParJoueur: Int get() = if (parts == 0) 0 else xpTotal / parts
    val orParJoueur: Int get() = if (parts == 0) 0 else orTotal / parts
    val orAuTresor: Int get() = orTotal - orParJoueur * parts
}

object ValiderQueteUseCase {

    fun repartition(quest: Quest, group: GameState.MjGroup, characters: List<Character>): RepartitionQuete =
        RepartitionQuete(
            pj = characters.filter { it.id in group.memberIds && it.type == "PJ" },
            joueursSansFiche = group.tablePlayers,
            xpTotal = quest.rewards.filter { it.type == QuestRewardType.XP }.sumOf { it.amount.coerceAtLeast(0) },
            orTotal = quest.rewards.filter { it.type == QuestRewardType.OR }.sumOf { it.amount.coerceAtLeast(0) },
        )

    /**
     * Valide la quête : distribue ses récompenses au groupe [groupId] (une seule fois) et la marque
     * terminée. Retourne le compte rendu de la distribution (aussi enregistré sur la quête).
     */
    fun valider(campaignId: String, quest: Quest, groupId: String): String? {
        if (quest.rewardedGroupId != null) return null
        val group = GameState.mjGroups.value.firstOrNull { it.id == groupId } ?: return null
        val characters = GameState.characters.value
        val rep = repartition(quest, group, characters)
        val lignes = mutableListOf("Groupe « ${group.name} »")

        // XP et or : une part par joueur.
        if (rep.parts > 0) {
            rep.pj.forEach { pj ->
                if (rep.xpParJoueur > 0) GameState.addExperience(pj.id, rep.xpParJoueur)
                if (rep.orParJoueur > 0) GameState.addGold(pj.id, rep.orParJoueur)
            }
            val parJoueur = listOfNotNull(
                rep.xpParJoueur.takeIf { it > 0 }?.let { "$it XP" },
                rep.orParJoueur.takeIf { it > 0 }?.let { "$it po" },
            ).joinToString(" et ")
            if (parJoueur.isNotBlank()) lignes += "$parJoueur par joueur (${rep.parts} joueur(s))"
            if (rep.joueursSansFiche.isNotEmpty() && parJoueur.isNotBlank()) {
                lignes += "À reporter à la main pour : " + rep.joueursSansFiche.joinToString { it.name }
            }
        } else if (rep.xpTotal > 0) {
            lignes += "XP non distribuée : aucun joueur dans le groupe"
        }

        // Réputation (chaque faction relit le groupe à jour).
        quest.rewards.filter { it.type == QuestRewardType.REPUTATION && it.label.isNotBlank() }.forEach { r ->
            appliquerReputationAuGroupe(groupId, r.label.trim(), r.amount)
            lignes += "Réputation ${if (r.amount >= 0) "+" else ""}${r.amount} auprès de ${r.label.trim()}"
        }

        // Tout le reste rejoint les possessions du groupe.
        var g = GameState.mjGroups.value.firstOrNull { it.id == groupId } ?: return null
        val orAuTresor = if (rep.parts > 0) rep.orAuTresor else rep.orTotal
        if (orAuTresor > 0) {
            g = g.copy(gold = g.gold + orAuTresor)
            lignes += "$orAuTresor po au trésor du groupe"
        }
        quest.rewards.forEach { r ->
            when (r.type) {
                QuestRewardType.EQUIPEMENT -> if (r.label.isNotBlank()) {
                    val quantite = r.amount.coerceAtLeast(1)
                    val existant = g.inventory.firstOrNull { it.name.equals(r.label.trim(), ignoreCase = true) && it.location.isBlank() }
                    g = g.copy(
                        inventory = if (existant != null) {
                            g.inventory.map { if (it.id == existant.id) it.copy(quantity = it.quantity + quantite) else it }
                        } else {
                            g.inventory + GroupItem(name = r.label.trim(), quantity = quantite)
                        }
                    )
                    lignes += "Inventaire du groupe : ${r.resume}"
                }
                QuestRewardType.PNJ -> r.characterId?.takeIf { id -> characters.any { it.id == id } }?.let { id ->
                    if (id !in g.memberIds) g = g.copy(memberIds = g.memberIds + id)
                    lignes += "${r.label} rejoint le groupe"
                }
                QuestRewardType.MONTURE, QuestRewardType.ANIMAL -> if (r.label.isNotBlank()) {
                    val kind = if (r.type == QuestRewardType.MONTURE) MountKind.MONTURE else MountKind.ANIMAL
                    g = g.copy(
                        mounts = g.mounts + Mount(
                            name = r.label.trim(),
                            species = r.detail.trim(),
                            kind = kind,
                            speed = r.amount.takeIf { it > 0 } ?: 18,
                        )
                    )
                    lignes += r.resume
                }
                QuestRewardType.VEHICULE -> if (r.label.isNotBlank()) {
                    g = g.copy(transports = g.transports + Transport(name = r.label.trim(), type = r.detail.trim()))
                    lignes += r.resume
                }
                QuestRewardType.BIEN -> if (r.label.isNotBlank()) {
                    g = g.copy(assets = g.assets + GroupAsset(name = r.label.trim(), description = r.detail.trim()))
                    lignes += r.resume
                }
                QuestRewardType.XP, QuestRewardType.OR, QuestRewardType.REPUTATION -> Unit
            }
        }
        GameState.updateMjGroup(g)

        val resume = lignes.joinToString("\n")
        GameState.upsertQuest(
            campaignId,
            quest.copy(status = QuestStatus.TERMINEE, rewardedGroupId = groupId, rewardSummary = resume),
        )
        return resume
    }
}
