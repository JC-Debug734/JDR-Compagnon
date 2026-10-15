package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Occultiste
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Le personnage a-t-il le Pacte de la lame (manifestation de l'Occultiste) ? */
internal fun aPacteDeLaLame(c: Character): Boolean = Occultiste.a(c, "Pacte de la lame")

/** Armes à distance du SRD, jamais invoquées comme arme de pacte (arme de corps à corps). */
private val armesADistance = listOf("arbalète", "arc ", "arc court", "arc long", "fléchette", "fronde", "sarbacane", "mousquet", "pistolet")

/**
 * Pacte de la lame (action Bonus) : invoquer une arme de pacte de corps à corps, courante ou de
 * guerre, qui apparaît en main principale ; ou créer un lien avec une arme magique déjà portée
 * (sans autre harmonisation ni autre occultiste). L'arme de pacte est maîtrisée et utilise le
 * Charisme pour l'attaque et les dégâts (voir Occultiste.estArmeDePacte). Un nouveau lien met fin
 * au précédent : une arme invoquée disparaît alors.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActionPacteDeLaLame(personnage: Character, modifier: Modifier = Modifier) {
    if (!aPacteDeLaLame(personnage)) return
    val context = LocalContext.current
    var mode by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val armesInvocables by produceState(initialValue = emptyList<String>(), personnage.worldId) {
        value = SrdRepository.loadEquipmentList(context, personnage.worldId.ifBlank { "donjon_et_dragon" })
            .filter { a ->
                (a.category.startsWith("Armes Courante", ignoreCase = true) || a.category.startsWith("Armes Guerre", ignoreCase = true)) &&
                    !a.properties.contains("Munitions", ignoreCase = true) &&
                    armesADistance.none { d -> a.name.lowercase().startsWith(d.trim()) }
            }
            .map { it.name }.distinct().sorted()
    }
    // Armes magiques portées : bonus « +1 » dans le nom, ou objet du catalogue rangé parmi les armes magiques.
    val armesMagiques = (personnage.equippedItems + personnage.backpackItems).distinct().filter { nom ->
        nom != personnage.armeDePacte && (
            Regex("""\+\d+\s*$""").containsMatchIn(nom) ||
                SrdRepository.findEquipmentItemCached(nom)?.takeIf { it.name.equals(nom, ignoreCase = true) }?.category?.contains("magique", ignoreCase = true) == true
            )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pacte de la lame (action Bonus)", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
            Text(
                if (personnage.armeDePacte.isBlank()) "Aucune arme de pacte."
                else "Arme de pacte : ${personnage.armeDePacte}" + (if (personnage.armeDePacteInvoquee) " (invoquée)" else " (liée)") +
                    " — maîtrisée, Charisme à l'attaque et aux dégâts, dégâts nécrotiques, psychiques ou radiants au choix.",
                style = MaterialTheme.typography.bodySmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = mode == INVOQUER, onClick = { mode = if (mode == INVOQUER) null else INVOQUER }, label = { Text("Invoquer une arme") })
                FilterChip(
                    selected = mode == LIER, enabled = armesMagiques.isNotEmpty(),
                    onClick = { mode = if (mode == LIER) null else LIER },
                    label = { Text(if (armesMagiques.isEmpty()) "Aucune arme magique portée" else "Se lier à une arme magique") },
                )
                if (personnage.armeDePacte.isNotBlank()) {
                    TextButton(onClick = {
                        GameState.rompreArmeDePacte(personnage.id)
                        message = "Lien rompu" + if (personnage.armeDePacteInvoquee) " : l'arme invoquée disparaît." else "."
                    }) { Text("Rompre le lien") }
                }
            }
            when (mode) {
                INVOQUER -> {
                    Text("Arme de corps à corps courante ou de guerre :", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        armesInvocables.forEach { arme ->
                            FilterChip(selected = false, onClick = {
                                GameState.invoquerArmeDePacte(personnage.id, arme)
                                message = "$arme invoquée : elle apparaît dans votre main."
                                mode = null
                            }, label = { Text(arme) })
                        }
                    }
                }
                LIER -> {
                    Text(
                        "Impossible avec une arme dont une autre créature est harmonisée, ou déjà liée à un autre occultiste.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        armesMagiques.forEach { arme ->
                            FilterChip(selected = false, onClick = {
                                GameState.lierArmeDePacte(personnage.id, arme)
                                message = "Lien créé avec $arme."
                                mode = null
                            }, label = { Text(arme) })
                        }
                    }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
        }
    }
}

private const val INVOQUER = "invoquer"
private const val LIER = "lier"
