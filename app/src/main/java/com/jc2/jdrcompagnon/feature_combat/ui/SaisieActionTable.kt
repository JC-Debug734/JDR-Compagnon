package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.ResultatTable
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Tour d'un personnage joué à la table (joueur sans l'appli) : tout se passe sur la table, avec
 * les vrais dés. Le MJ note seulement le résultat : qui est touché et combien de dégâts, raté, ou
 * un soin. Une attaque de plus dans le même tour (Attaque supplémentaire) : on recommence.
 * Le reste (sort sans dégâts, objet, déplacement) passe par [onAutreAction].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SaisieActionTable(combattant: Combattant, etat: CombatEnCours, onAutreAction: () -> Unit) {
    // Adversaires debout d'abord, puis les alliés (soins).
    val cibles = etat.combattants.filter { it.estMonstre != combattant.estMonstre && !it.horsCombat } +
        etat.combattants.filter { it.estMonstre == combattant.estMonstre && it.id != combattant.id && !it.mort }
    var cibleId by remember(combattant.id, etat.round) { mutableStateOf(cibles.firstOrNull()?.id) }
    var montant by remember(combattant.id, etat.round) { mutableStateOf("") }
    val cible = cibles.firstOrNull { it.id == cibleId }
    val allie = cible != null && cible.estMonstre == combattant.estMonstre
    val n = montant.toIntOrNull()?.takeIf { it > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("🎲 Résultat annoncé par ${combattant.nom}", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
        Text("Le joueur lance ses dés sur la table : notez seulement qui est touché et les dégâts.", style = MaterialTheme.typography.bodySmall)
        if (cibles.isEmpty()) {
            Text("Plus aucune cible.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Cible", style = MaterialTheme.typography.labelMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cibles.forEach { c ->
                    FilterChip(
                        selected = c.id == cibleId,
                        onClick = { cibleId = c.id },
                        label = { Text((if (c.estMonstre == combattant.estMonstre) "🛡 " else "") + c.nom + " · CA ${c.ca}") },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = montant,
                    onValueChange = { v -> montant = v.filter(Char::isDigit).take(3) },
                    label = { Text(if (allie) "PV soignés" else "Dégâts") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(120.dp),
                )
            }
            if (cible != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (allie) {
                        Button(
                            enabled = n != null,
                            onClick = { CombatSession.resultatTable(combattant.id, cible.id, ResultatTable.SOIN, n ?: 0); montant = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                        ) { Text("💚 Soigner ${cible.nom}" + (n?.let { " de $it" } ?: "")) }
                    } else {
                        Button(
                            enabled = n != null,
                            onClick = { CombatSession.resultatTable(combattant.id, cible.id, ResultatTable.TOUCHE, n ?: 0); montant = "" },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.weight(1f),
                        ) { Text("✔ Touché" + (n?.let { " : $it dégâts" } ?: "")) }
                        OutlinedButton(
                            onClick = { CombatSession.resultatTable(combattant.id, cible.id, ResultatTable.RATE); montant = "" },
                            modifier = Modifier.weight(1f),
                        ) { Text("✖ Raté") }
                    }
                }
            }
        }
        TextButton(onClick = onAutreAction) { Text("Autre action (sort, objet, déplacement…)") }
    }
}
