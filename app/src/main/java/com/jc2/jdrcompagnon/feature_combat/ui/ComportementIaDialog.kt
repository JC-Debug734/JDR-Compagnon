package com.jc2.jdrcompagnon.feature_combat.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmesPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.DeclencheurIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.feature_combat.domain.model.RegleProfilIA
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Comportement d'un combattant piloté par l'IA (réservé au MJ, jamais montré aux joueurs) :
 * - pour un personnage (PNJ) : contrôle MJ ou IA, et camp (allié des joueurs ou ennemi) ;
 * - profil de base ;
 * - règles « selon la situation » : un monstre complexe change de profil quand elles se vérifient
 *   (évaluées dans l'ordre, la première l'emporte).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ComportementIADialog(combattant: Combattant, onDismiss: () -> Unit) {
    val estPersonnage = combattant.characterId != null
    var ajoutOuvert by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Un PNJ confié à l'IA a besoin d'attaques : elles sont tirées de ses armes équipées.
    LaunchedEffect(combattant.id, combattant.piloteParIa) {
        if (estPersonnage && combattant.piloteParIa && combattant.attaques.isEmpty()) {
            val perso = GameState.characters.value.firstOrNull { it.id == combattant.characterId } ?: return@LaunchedEffect
            CombatSession.definirAttaques(combattant.id, attaquesDuPersonnage(context, perso))
            // Décision recalculée avec ses vraies armes.
            CombatSession.redeciderMonstre(combattant.id)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Comportement — ${combattant.nom}") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "🔒 Visible du MJ uniquement : les joueurs ne voient ni le profil ni les décisions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (estPersonnage) {
                    Text("Camp", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !combattant.estMonstre, onClick = { CombatSession.changerCamp(combattant.id, ennemi = false) }, label = { Text("Allié des joueurs") })
                        FilterChip(selected = combattant.estMonstre, onClick = { CombatSession.changerCamp(combattant.id, ennemi = true) }, label = { Text("Ennemi") })
                    }
                    if (!combattant.estMonstre) {
                        Text("Qui décide de ses actions ?", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = combattant.profilIA == null,
                                onClick = { CombatSession.changerProfilIA(combattant.id, null) },
                                label = { Text("Le MJ") }
                            )
                            FilterChip(
                                selected = combattant.profilIA != null,
                                onClick = {
                                    if (combattant.profilIA == null) scope.launch {
                                        // Profil de sa fiche (ou déduit de sa classe et de ses armes).
                                        val perso = GameState.characters.value.firstOrNull { it.id == combattant.characterId }
                                        CombatSession.changerProfilIA(combattant.id, perso?.let { profilPersonnage(context, it).profil } ?: ProfilIA.PROTECTEUR)
                                    }
                                },
                                label = { Text("L'IA") }
                            )
                        }
                    }
                }

                if (combattant.piloteParIa) {
                    HorizontalDivider()
                    val base = combattant.profilIA ?: ProfilIA.BRUTE
                    Text("Profil de base", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProfilIA.entries.forEach { p ->
                            FilterChip(selected = p == base, onClick = { CombatSession.changerProfilIA(combattant.id, p) }, label = { Text(p.label) })
                        }
                    }

                    HorizontalDivider()
                    Text("Selon la situation", fontWeight = FontWeight.Bold)
                    Text(
                        "Le combattant change de profil dès qu'une règle se vérifie (la première de la liste l'emporte).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    combattant.reglesIA.forEachIndexed { i, regle ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}. Si ${regle.libelle.lowercase()} → ${regle.profil.label}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (i > 0) {
                                IconButton(onClick = {
                                    val l = combattant.reglesIA.toMutableList()
                                    l.add(i - 1, l.removeAt(i))
                                    CombatSession.definirReglesIA(combattant.id, l)
                                }) { Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", modifier = Modifier.size(18.dp)) }
                            }
                            IconButton(onClick = {
                                CombatSession.definirReglesIA(combattant.id, combattant.reglesIA.filterIndexed { j, _ -> j != i })
                            }) { Icon(Icons.Default.Close, contentDescription = "Supprimer", modifier = Modifier.size(18.dp)) }
                        }
                    }
                    if (ajoutOuvert) {
                        NouvelleRegle(
                            onAjouter = { regle ->
                                CombatSession.definirReglesIA(combattant.id, combattant.reglesIA + regle)
                                ajoutOuvert = false
                            },
                            onAnnuler = { ajoutOuvert = false }
                        )
                    } else {
                        TextButton(onClick = { ajoutOuvert = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(" Ajouter une règle")
                        }
                    }

                    if (combattant.attaques.isNotEmpty()) {
                        HorizontalDivider()
                        Text(if (estPersonnage) "Attaques (d'après ses armes)" else "Actions lues sur la fiche", style = MaterialTheme.typography.labelMedium)
                        if (combattant.nbAttaquesMultiples > 1) Text("Attaques multiples : ${combattant.nbAttaquesMultiples}", style = MaterialTheme.typography.bodySmall)
                        combattant.attaques.forEach { Text("• ${it.resume}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NouvelleRegle(onAjouter: (RegleProfilIA) -> Unit, onAnnuler: () -> Unit) {
    var declencheur by remember { mutableStateOf(DeclencheurIA.PV_SOUS) }
    var valeur by remember { mutableIntStateOf(DeclencheurIA.PV_SOUS.valeurParDefaut ?: 0) }
    var profil by remember { mutableStateOf(ProfilIA.BERSERKER) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Text("Quand…", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DeclencheurIA.entries.forEach { d ->
                FilterChip(
                    selected = d == declencheur,
                    onClick = { declencheur = d; valeur = d.valeurParDefaut ?: 0 },
                    label = { Text(d.label) }
                )
            }
        }
        if (declencheur.valeurParDefaut != null) {
            val pas = if (declencheur.unite.contains("%")) 5 else 1
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${declencheur.label} :", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = { valeur = (valeur - pas).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, contentDescription = "Moins") }
                Text("$valeur ${declencheur.unite}", fontWeight = FontWeight.Bold)
                IconButton(onClick = { valeur = (valeur + pas).coerceAtMost(if (pas == 5) 100 else 20) }) { Icon(Icons.Default.Add, contentDescription = "Plus") }
            }
        }
        Text("…adopter le profil", style = MaterialTheme.typography.labelMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ProfilIA.entries.forEach { p ->
                FilterChip(selected = p == profil, onClick = { profil = p }, label = { Text(p.label) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAjouter(RegleProfilIA(declencheur, profil, valeur)) }) { Text("Ajouter") }
            TextButton(onClick = onAnnuler) { Text("Annuler") }
        }
    }
}

/** Attaques d'un personnage (PNJ piloté par l'IA) d'après ses armes équipées et le SRD. */
internal suspend fun attaquesDuPersonnage(context: Context, perso: Character): List<AttaqueMonstre> {
    val armes = SrdRepository.loadEquipmentList(context, GameState.currentWorldId())
        .filter { it.damage.isNotBlank() }
        .map { ArmeSrd.depuisFiche(it.name, it.damage, it.properties, it.rawMarkdown) }
    return ArmesPersonnage.attaques(
        nomsArmes = perso.equippedItems + perso.weapons,
        armes = armes,
        force = perso.strength,
        dexterite = perso.dexterity,
        maitrise = perso.proficiencyBonus,
    )
}
