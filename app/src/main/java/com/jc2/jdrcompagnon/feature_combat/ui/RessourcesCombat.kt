package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.hitDieForClass
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Une ressource du personnage en combat : libellé, valeur, et si elle est encore disponible. */
private data class Ressource(val libelle: String, val valeur: String, val disponible: Boolean)

/**
 * Ressources utiles pendant le tour du personnage : emplacements de sort restants par niveau,
 * Magie de pacte, Puissance curative et Châtiment gratuit du Paladin, Restauration magique,
 * sorts de prédilection, dés de vie, inspiration héroïque. [emplacementsSimulation] remplace les
 * emplacements de la fiche (simulation : ils y sont dépensés sans toucher la fiche).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CarteRessourcesCombat(
    personnage: Character,
    arsenal: ArsenalJoueur,
    emplacementsSimulation: Map<Int, Int>? = null,
) {
    val restants = emplacementsSimulation ?: arsenal.restants(personnage)
    val emplacements = arsenal.emplacements.classiques.toSortedMap().map { (niveau, max) ->
        val reste = restants[niveau] ?: 0
        Ressource("Niv. $niveau", "$reste/$max", reste > 0)
    }
    val autres = buildList {
        arsenal.emplacements.pacte?.let { (nombre, niveau) ->
            val reste = arsenal.pacteRestant(personnage)
            add(Ressource("Pacte (niv. $niveau)", "$reste/$nombre", reste > 0))
        }
        ArsenalPersonnage.reservePuissanceCurative(personnage).takeIf { it > 0 }?.let { reserve ->
            val reste = (reserve - personnage.layOnHandsUsed).coerceAtLeast(0)
            add(Ressource("Puissance curative", "$reste/$reserve PV", reste > 0))
        }
        if (ArsenalPersonnage.aChatimentDePaladin(personnage)) {
            add(Ressource("Châtiment gratuit", if (personnage.divineSmiteFreeUsed) "utilisé" else "disponible", !personnage.divineSmiteFreeUsed))
        }
        if (ArsenalPersonnage.budgetRestaurationMagique(personnage) > 0) {
            add(Ressource("Restauration magique", if (personnage.arcaneRecoveryUsed) "utilisée" else "disponible", !personnage.arcaneRecoveryUsed))
        }
        personnage.sortsSpeciaux.filterValues { it == ArsenalPersonnage.SORT_PREDILECTION }.keys.forEach { sort ->
            val utilise = personnage.sortsPredilectionUtilises.any { ArsenalPersonnage.memeSort(it, sort) }
            add(Ressource("$sort (prédilection)", if (utilise) "utilisé" else "disponible", !utilise))
        }
        val desVie = (personnage.level - personnage.hitDiceUsed).coerceIn(0, personnage.level)
        add(Ressource("Dés de vie (d${hitDieForClass(personnage.characterClass)})", "$desVie/${personnage.level}", desVie > 0))
        if (personnage.heroicInspiration) add(Ressource("Inspiration héroïque", "oui", true))
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ressources de ${personnage.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = ForcedDarkPalette.AccentGold)
            if (emplacements.isNotEmpty()) {
                Text("Emplacements de sort", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    emplacements.forEach { CaseRessource(it) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                autres.forEach { CaseRessource(it) }
            }
            if (personnage.effetsActifs.isNotEmpty()) {
                Text(
                    "Effets en cours : " + personnage.effetsActifs.joinToString { it.nom + if (it.concentration) " (concentration)" else "" },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Case d'une ressource : dorée si disponible, rouge si épuisée. */
@Composable
private fun CaseRessource(r: Ressource) {
    val couleur = if (r.disponible) ForcedDarkPalette.AccentGold else MaterialTheme.colorScheme.error
    Surface(
        shape = MaterialTheme.shapes.small,
        color = couleur.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, couleur.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(r.libelle, style = MaterialTheme.typography.labelSmall, color = Color.White)
            Text(r.valeur, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = couleur)
        }
    }
}
