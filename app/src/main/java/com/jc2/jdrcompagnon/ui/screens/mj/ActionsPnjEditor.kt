package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionPnj
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmesPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeActionPnj
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.launch

private val ActionHint = Color.White.copy(alpha = 0.8f)
private val typesDegats = listOf("tranchants", "perforants", "contondants", "feu", "froid", "foudre", "poison", "acide", "nécrotiques", "radiants", "psychiques", "force", "tonnerre")
private val caracteristiques = listOf("Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme")

private fun mod(score: Int) = Math.floorDiv(score - 10, 2)

/**
 * Actions du PNJ, à bonus et dégâts fixes comme dans un bloc de monstre. Un PNJ sans action garde
 * les attaques calculées depuis ses armes ; « Reprendre ses armes » les fige en actions.
 */
@Composable
internal fun ActionsPnjEditor(pnj: Character, onChange: (List<ActionPnj>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var edition by remember { mutableStateOf<ActionPnj?>(null) }
    var nouvelle by remember { mutableStateOf(false) }
    val actions = pnj.actionsPnj

    Text("Actions", style = MaterialTheme.typography.labelLarge, color = Color.White)
    Text(
        "Bonus et dégâts fixes, comme dans le bestiaire : utilisés par le bloc de stats et en combat.",
        style = MaterialTheme.typography.bodySmall,
        color = ActionHint,
    )
    actions.forEach { action ->
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth().clickable { edition = action },
        ) {
            Text(action.ligneBloc, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
        }
    }
    val armes = (pnj.weapons + pnj.equippedSlots.values + pnj.equippedItems).distinct()
    if (actions.isEmpty() && armes.isNotEmpty()) {
        Text(
            "Pour l'instant, ses attaques sont calculées depuis ses armes (${armes.joinToString(", ")}).",
            style = MaterialTheme.typography.bodySmall,
            color = ActionHint,
        )
        OutlinedButton(
            onClick = {
                scope.launch {
                    val srd = SrdRepository.loadEquipmentList(context, pnj.worldId.ifBlank { "donjon_et_dragon" })
                        .filter { it.damage.isNotBlank() }
                        .map { ArmeSrd.depuisFiche(it.name, it.damage, it.properties, it.rawMarkdown) }
                    val attaques = ArmesPersonnage.attaques(armes, srd, pnj.strength, pnj.dexterity, pnj.proficiencyBonus)
                    if (attaques.isNotEmpty()) onChange(attaques.map { ActionPnj.depuis(it) })
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Reprendre ses armes en actions fixes", color = Color.White) }
    }
    OutlinedButton(onClick = { nouvelle = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Ajouter une action", color = Color.White)
    }

    if (nouvelle || edition != null) {
        val modificateur = maxOf(mod(pnj.strength), mod(pnj.dexterity))
        val initiale = edition ?: ActionPnj(
            nom = "",
            bonus = pnj.proficiencyBonus + modificateur,
            degats = "1d6" + if (modificateur > 0) "+$modificateur" else if (modificateur < 0) "$modificateur" else "",
            typeDegats = "tranchants",
            dd = 8 + pnj.proficiencyBonus + modificateur,
        )
        ActionPnjDialog(
            initiale = initiale,
            nouvelle = edition == null,
            onDismiss = { nouvelle = false; edition = null },
            onSave = { a ->
                onChange(if (actions.any { it.id == a.id }) actions.map { if (it.id == a.id) a else it } else actions + a)
                nouvelle = false; edition = null
            },
            onDelete = { a -> onChange(actions.filterNot { it.id == a.id }); edition = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionPnjDialog(
    initiale: ActionPnj,
    nouvelle: Boolean,
    onDismiss: () -> Unit,
    onSave: (ActionPnj) -> Unit,
    onDelete: (ActionPnj) -> Unit,
) {
    var a by remember { mutableStateOf(initiale) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (nouvelle) "Nouvelle action" else "Modifier l'action") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = a.nom,
                    onValueChange = { a = a.copy(nom = it) },
                    label = { Text("Nom (ex. Épée, Souffle (Recharge 5–6))") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeActionPnj.entries.forEach { t ->
                        FilterChip(selected = a.type == t, onClick = { a = a.copy(type = t) }, label = { Text(t.label) })
                    }
                }
                if (a.type == TypeActionPnj.SAUVEGARDE) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        caracteristiques.forEach { c ->
                            FilterChip(selected = a.sauvegarde == c, onClick = { a = a.copy(sauvegarde = c) }, label = { Text(c) })
                        }
                    }
                    Entier("DD", a.dd) { a = a.copy(dd = it) }
                } else {
                    Entier("Bonus d'attaque", a.bonus) { a = a.copy(bonus = it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = a.degats,
                        onValueChange = { a = a.copy(degats = it) },
                        label = { Text("Dégâts (1d8+1)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (a.degats.isBlank()) "" else "≈ ${a.moyenne}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
                OutlinedTextField(
                    value = a.typeDegats,
                    onValueChange = { a = a.copy(typeDegats = it) },
                    label = { Text("Type de dégâts") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    typesDegats.forEach { t ->
                        FilterChip(selected = a.typeDegats == t, onClick = { a = a.copy(typeDegats = t) }, label = { Text(t) })
                    }
                }
                OutlinedTextField(
                    value = a.portee,
                    onValueChange = { a = a.copy(portee = it) },
                    label = {
                        Text(
                            when (a.type) {
                                TypeActionPnj.CORPS_A_CORPS -> "Allonge en m (1,50 par défaut)"
                                TypeActionPnj.DISTANCE -> "Portée en m (ex. 24/96)"
                                TypeActionPnj.SAUVEGARDE -> "Zone (ex. cône de 9 m)"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = a.effet,
                    onValueChange = { a = a.copy(effet = it) },
                    label = { Text("Effet en plus (ex. la cible subit l'état À terre)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Aperçu", style = MaterialTheme.typography.labelLarge)
                Text(a.ligneBloc, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
            }
        },
        confirmButton = { TextButton(enabled = a.nom.isNotBlank(), onClick = { onSave(a.copy(nom = a.nom.trim())) }) { Text("Enregistrer") } },
        dismissButton = {
            Row {
                if (!nouvelle) TextButton(onClick = { onDelete(a) }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
}

/** Entier relatif ; la saisie vide ou « - » est tolérée pendant la frappe. */
@Composable
private fun Entier(label: String, valeur: Int, onChange: (Int) -> Unit) {
    var texte by remember { mutableStateOf(valeur.toString()) }
    OutlinedTextField(
        value = texte,
        onValueChange = { saisie ->
            texte = saisie.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(3)
            texte.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
