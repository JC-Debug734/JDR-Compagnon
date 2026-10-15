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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportement
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportements
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import androidx.compose.runtime.collectAsState
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Comportement IA d'un personnage : celui choisi sur sa fiche, sinon celui de sa classe (grille de comportement). */
internal fun comportementPersonnage(perso: Character): Comportement = CombatSession.comportementParDefaut(perso)

@Composable
private fun CarteComportement(titre: String, contenu: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titre, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
            contenu()
        }
    }
}

/**
 * Comportement IA de combat d'un personnage, sur sa fiche (MJ uniquement) : automatique d'après
 * sa classe (grille de comportement), ou comportement choisi, repris à chaque entrée en combat.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComportementIaPnjCard(character: Character) {
    val tous by Comportements.tous.collectAsState()
    val grille by Comportements.grille.collectAsState()
    val auto = remember(character.characterClass, tous, grille) { Comportements.pourClasse(character.characterClass) }
    val choisi = Comportements.parId(character.profilIA)
    val actif = choisi ?: auto
    // Seul le nom du comportement retenu est affiché, pas sa description.
    CarteComportement("🧠 Comportement en combat (IA) : ${actif.nom}") {
        Text(
            "Visible du MJ uniquement. Utilisé en combat quand aucun joueur n'incarne ce personnage. " +
                "Automatique : selon sa classe (grille de l'outil COMPORTEMENT).",
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = choisi == null,
                onClick = { GameState.setProfilIA(character.id, null) },
                label = { Text("Automatique (${auto.nom})") },
            )
            tous.forEach { c ->
                FilterChip(selected = choisi?.id == c.id, onClick = { GameState.setProfilIA(character.id, c.id) }, label = { Text(c.nom) })
            }
        }
    }
}

/** Comportement IA d'un monstre du bestiaire (fiche de la bibliothèque, MJ uniquement). */
@Composable
fun ComportementIaMonstreCard(rawMarkdown: String) {
    val comportement = remember(rawMarkdown) { ProfilCombatMonstre.depuisFiche(rawMarkdown).comportement }
    CarteComportement("🧠 Comportement en combat (IA) : ${comportement.profil.label}") {}
}
