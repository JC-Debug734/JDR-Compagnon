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
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilDeduit
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Profil IA d'un personnage (PNJ) : celui choisi sur sa fiche, sinon déduit de sa classe et de ses armes. */
internal suspend fun profilPersonnage(context: android.content.Context, perso: Character): ProfilDeduit {
    perso.profilIA?.let { nom -> runCatching { ProfilIA.valueOf(nom) }.getOrNull() }?.let {
        return ProfilDeduit(it, "Choisi par le MJ sur la fiche")
    }
    return ProfilIA.analyserPersonnage(perso.characterClass, attaquesDuPersonnage(context, perso))
}

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
 * Comportement IA de combat d'un PNJ, sur sa fiche (MJ uniquement) : profil automatique d'après
 * la classe et les armes, ou profil choisi, repris à chaque entrée en combat.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComportementIaPnjCard(character: Character) {
    val context = LocalContext.current
    var auto by remember(character.characterClass, character.equippedSlots, character.equippedItems) { mutableStateOf<ProfilDeduit?>(null) }
    LaunchedEffect(character.characterClass, character.equippedSlots, character.equippedItems) {
        auto = ProfilIA.analyserPersonnage(character.characterClass, attaquesDuPersonnage(context, character))
    }
    val choisi = character.profilIA?.let { runCatching { ProfilIA.valueOf(it) }.getOrNull() }
    val actif = choisi ?: auto?.profil
    // Seul le nom du profil retenu est affiché, pas sa description.
    CarteComportement("🧠 Comportement en combat (IA)" + (actif?.let { " : ${it.label}" } ?: "")) {
        Text(
            "Visible du MJ uniquement. Utilisé quand ce PNJ est ennemi, ou allié confié à l'IA.",
            style = MaterialTheme.typography.bodySmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = choisi == null,
                onClick = { GameState.setProfilIA(character.id, null) },
                label = { Text("Automatique" + (auto?.let { " (${it.profil.label})" } ?: "")) },
            )
            ProfilIA.entries.forEach { p ->
                FilterChip(selected = choisi == p, onClick = { GameState.setProfilIA(character.id, p.name) }, label = { Text(p.label) })
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
