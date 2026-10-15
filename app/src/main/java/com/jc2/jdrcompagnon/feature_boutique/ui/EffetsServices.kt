package com.jc2.jdrcompagnon.feature_boutique.ui

import android.content.Context
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EffetService
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.CoroutineScope

/**
 * Action d'un service sur la fiche du personnage qui vient de l'utiliser (et de le payer) : repas,
 * nuit en chambre (repos long), soins... Les changements passent par GameState, relayé au MJ en
 * partie réseau. Retourne le compte rendu à afficher au joueur, null si le service n'a pas d'effet.
 */
object EffetsServices {

    fun appliquer(
        context: Context,
        scope: CoroutineScope,
        personnage: Character,
        nomService: String,
        effet: EffetService,
        onResultatDiffere: (String) -> Unit = {},
    ): String? = when (effet) {
        EffetService.AUTO -> appliquer(context, scope, personnage, nomService, EffetService.deduire(nomService), onResultatDiffere)
        EffetService.AUCUN -> null
        EffetService.REPAS -> {
            GameState.nourrir(personnage.id)
            "Repas pris : compteur de faim remis à zéro."
        }
        EffetService.NUIT -> {
            var resume: String? = null
            com.jc2.jdrcompagnon.ui.screens.joueur.effectuerReposLong(context, scope, personnage) {
                if (resume == null) resume = it else onResultatDiffere(it)
            }
            resume ?: "Nuit passée."
        }
        EffetService.SOINS -> {
            GameState.soignerOuGuerirPoison(personnage.id, personnage.maxHitPoints, retirerPoison = false)
            "Soigné : PV au maximum."
        }
        EffetService.GUERISON -> {
            val restantes = personnage.condition.split(",").map { it.trim() }.filter {
                it.isNotEmpty() && !it.startsWith("Empoisonn", ignoreCase = true) && !it.startsWith("Malad", ignoreCase = true)
            }
            GameState.setCondition(personnage.id, restantes.joinToString(", "))
            "Guéri : poison et maladie retirés."
        }
        EffetService.BENEDICTION -> {
            GameState.setHeroicInspiration(personnage.id, true)
            "Béni : inspiration héroïque obtenue."
        }
        EffetService.BAIN -> {
            if (personnage.exhaustionLevel > 0) {
                GameState.setExhaustionLevel(personnage.id, personnage.exhaustionLevel - 1)
                "Délassé : épuisement ${personnage.exhaustionLevel} → ${personnage.exhaustionLevel - 1}."
            } else "Un bain agréable."
        }
        EffetService.MONTURE -> {
            GameState.addItemToBackpack(personnage.id, "Monture : $nomService")
            "« $nomService » ajouté à vos possessions."
        }
        EffetService.RECRUE -> {
            GameState.addItemToBackpack(personnage.id, "Suivant : $nomService")
            "« $nomService » vous accompagne (noté dans vos possessions)."
        }
        // Les cartes sont tirées et montrées par TirageTarokkaDialog (voir CommerceLieuDialog).
        EffetService.TAROKKA -> "Les cartes sont tirées."
    }
}
