package com.jc2.jdrcompagnon.feature_group.domain

import com.jc2.jdrcompagnon.feature_group.domain.model.MountKind
import com.jc2.jdrcompagnon.network.GroupeJoueurData
import com.jc2.jdrcompagnon.network.MembreJoueurData
import com.jc2.jdrcompagnon.network.MontureJoueurData
import com.jc2.jdrcompagnon.network.ObjetJoueurData
import com.jc2.jdrcompagnon.network.ReputationJoueurData
import com.jc2.jdrcompagnon.network.VehiculeJoueurData
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Vue d'un groupe partagée par le MJ (carte, calcul de trajet) et diffusée aux joueurs
 * (GroupeJoueurData) : mêmes infos des deux côtés, sans rien de secret.
 */
object GroupeJoueur {

    /** Vitesse (m) d'un joueur sans fiche : celle d'un humanoïde de taille moyenne. */
    private const val VITESSE_SANS_FICHE = 9

    fun depuis(group: GameState.MjGroup, characters: List<Character>): GroupeJoueurData {
        val membres = characters.filter { it.id in group.memberIds }
        val nomDe = { id: String? -> id?.let { i -> membres.firstOrNull { it.id == i }?.name ?: characters.firstOrNull { it.id == i }?.name } }
        val avecLeGroupe = { lieu: String -> lieu.isBlank() || lieu.equals(group.location, ignoreCase = true) }

        // Vitesse du groupe : celle du plus lent de ceux qui voyagent (PJ, PNJ, créatures,
        // joueurs sans fiche, et véhicules en état de rouler qui accompagnent le groupe).
        val voyageurs = buildList {
            membres.filter { avecLeGroupe(it.location) }.forEach { add(it.name to it.speed) }
            group.tablePlayers.forEach { add(it.name to VITESSE_SANS_FICHE) }
            group.transports.filter { avecLeGroupe(it.location) }.forEach { t ->
                t.vitesse(group.mounts)?.let { add(t.name to it) }
            }
        }.filter { it.second > 0 }
        val plusLent = voyageurs.minByOrNull { it.second }

        return GroupeJoueurData(
            id = group.id,
            name = group.name,
            location = group.location,
            gold = group.gold,
            carteId = group.carteId,
            carteFx = group.carteFx,
            carteFy = group.carteFy,
            membres = membres.map { c ->
                MembreJoueurData(
                    id = c.id,
                    name = c.name,
                    type = c.type,
                    speed = c.speed,
                    location = c.location,
                    detail = listOf(c.race, c.characterClass, "niv.${c.level}".takeIf { c.type == "PJ" }.orEmpty())
                        .filter { it.isNotBlank() }.joinToString(" • "),
                )
            } + group.tablePlayers.map { MembreJoueurData(id = it.id, name = it.name, type = "PJ", speed = VITESSE_SANS_FICHE, detail = "niv.${it.level} (sans fiche)") },
            montures = group.mounts.map { m ->
                MontureJoueurData(
                    name = m.name,
                    kind = m.kind.label,
                    species = m.species,
                    speed = if (m.kind == MountKind.MONTURE) m.speed else 0,
                    rider = nomDe(m.riderCharacterId),
                    location = m.location,
                    id = m.id,
                    bagages = m.bagages,
                )
            },
            vehicules = group.transports.map { t ->
                VehiculeJoueurData(
                    name = t.name,
                    type = t.type,
                    speed = t.vitesse(group.mounts),
                    tirePar = group.mounts.filter { it.transportId == t.id }.map { it.name },
                    location = t.location,
                )
            },
            reputations = group.reputations.map { ReputationJoueurData(it.factionName, it.score) },
            inventaire = group.inventory.map { ObjetJoueurData(it.name, it.quantity, it.location) },
            biens = group.assets.map { ObjetJoueurData(it.name, 1, it.location, it.description) },
            vitesseM = plusLent?.second,
            plusLent = plusLent?.first,
            iconKey = group.iconKey,
            couleurArgb = group.couleurArgb,
        )
    }
}
