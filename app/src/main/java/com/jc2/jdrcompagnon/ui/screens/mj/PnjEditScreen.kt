package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.ui.LocationField
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.screens.joueur.skillList
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val EditHint = Color.White.copy(alpha = 0.8f)

@Composable
private fun champCouleurs() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedLabelColor = ForcedDarkPalette.AccentGold,
    unfocusedLabelColor = EditHint,
    focusedBorderColor = ForcedDarkPalette.AccentGold,
    unfocusedBorderColor = EditHint.copy(alpha = 0.5f),
)

/**
 * Édition d'un PNJ, en une seule page : identité, de quoi l'incarner (comportement, intentions,
 * secrets...) et l'essentiel pour un combat. Remplace l'éditeur de fiche de PJ (classe, sous-
 * classe, inventaire détaillé...) inadapté aux PNJ ; l'équipement et les sorts restent modifiables
 * depuis les onglets de la fiche.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PnjEditScreen(
    character: Character,
    onSave: (Character) -> Unit,
    onCancel: () -> Unit,
) {
    var pnj by remember(character.id) { mutableStateOf(character) }

    fun enregistrer() {
        val pvMax = pnj.maxHitPoints.coerceAtLeast(1)
        // PV actuels : suivent le maximum s'ils étaient pleins, jamais au-dessus.
        val pvActuels = if (character.currentHitPoints >= character.maxHitPoints) pvMax else pnj.currentHitPoints.coerceAtMost(pvMax)
        onSave(pnj.copy(name = pnj.name.trim().ifBlank { character.name }, maxHitPoints = pvMax, currentHitPoints = pvActuels))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le PNJ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Annuler") }
                },
                actions = {
                    IconButton(onClick = ::enregistrer) { Icon(Icons.Default.Save, contentDescription = "Enregistrer", tint = ForcedDarkPalette.AccentGold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Section("Identité")
            Champ("Nom", pnj.name) { pnj = pnj.copy(name = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Champ("Espèce", pnj.race, Modifier.weight(1f)) { pnj = pnj.copy(race = it) }
                Champ("Rôle / métier", pnj.characterClass, Modifier.weight(1f)) { pnj = pnj.copy(characterClass = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Champ("Alignement", pnj.alignment, Modifier.weight(1f)) { pnj = pnj.copy(alignment = it) }
                Nombre("Niveau", pnj.level, Modifier.weight(0.5f)) { pnj = pnj.copy(level = it.coerceIn(1, 30)) }
            }
            LocationField(
                value = pnj.location,
                onValueChange = { pnj = pnj.copy(location = it) },
                label = "Où le trouver",
                modifier = Modifier.fillMaxWidth(),
                colors = champCouleurs(),
            )
            Champ("Apparence", pnj.appearance, lignes = 2) { pnj = pnj.copy(appearance = it) }

            Section("Pour l'incarner (MJ uniquement)")
            Champ("Personnalité, manière de parler", pnj.personalityTraits, lignes = 2) { pnj = pnj.copy(personalityTraits = it) }
            Champ("Comportement", pnj.comportement, lignes = 2) { pnj = pnj.copy(comportement = it) }
            Champ("Intentions", pnj.intentions, lignes = 2) { pnj = pnj.copy(intentions = it) }
            Champ("Objectif", pnj.objectif, lignes = 2) { pnj = pnj.copy(objectif = it) }
            Champ("Secrets", pnj.secrets, lignes = 2) { pnj = pnj.copy(secrets = it) }
            Text(
                "Les secrets à faire découvrir aux joueurs se gèrent aussi, cochables, dans « Secrets et indices » de la campagne.",
                style = MaterialTheme.typography.bodySmall,
                color = EditHint,
            )
            Champ("Notes du MJ", pnj.dmNotes, lignes = 2) { pnj = pnj.copy(dmNotes = it) }

            Section("Combat")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Nombre("CA", pnj.armorClass, Modifier.weight(1f)) { pnj = pnj.copy(armorClass = it) }
                Nombre("PV max", pnj.maxHitPoints, Modifier.weight(1f)) { pnj = pnj.copy(maxHitPoints = it) }
                Nombre("Vitesse", pnj.speed, Modifier.weight(1f)) { pnj = pnj.copy(speed = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Nombre("FOR", pnj.strength, Modifier.weight(1f)) { pnj = pnj.copy(strength = it) }
                Nombre("DEX", pnj.dexterity, Modifier.weight(1f)) { pnj = pnj.copy(dexterity = it) }
                Nombre("CON", pnj.constitution, Modifier.weight(1f)) { pnj = pnj.copy(constitution = it) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Nombre("INT", pnj.intelligence, Modifier.weight(1f)) { pnj = pnj.copy(intelligence = it) }
                Nombre("SAG", pnj.wisdom, Modifier.weight(1f)) { pnj = pnj.copy(wisdom = it) }
                Nombre("CHA", pnj.charisma, Modifier.weight(1f)) { pnj = pnj.copy(charisma = it) }
            }
            Text("Compétences maîtrisées", style = MaterialTheme.typography.labelLarge, color = Color.White)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                skillList.forEach { skill ->
                    val maitrisee = skill in pnj.skillProficiencies || skill in pnj.skillExpertise ||
                        (pnj.skills[skill] ?: ProficiencyLevel.NONE) != ProficiencyLevel.NONE
                    FilterChip(
                        selected = maitrisee,
                        onClick = {
                            pnj = if (maitrisee) {
                                pnj.copy(
                                    skillProficiencies = pnj.skillProficiencies - skill,
                                    skillExpertise = pnj.skillExpertise - skill,
                                    skills = pnj.skills - skill,
                                )
                            } else {
                                pnj.copy(skillProficiencies = pnj.skillProficiencies + skill)
                            }
                        },
                        label = { Text(skill) },
                        colors = FilterChipDefaults.filterChipColors(labelColor = Color.White, selectedLabelColor = Color.Black, selectedContainerColor = ForcedDarkPalette.AccentGold),
                    )
                }
            }
            ActionsPnjEditor(pnj) { pnj = pnj.copy(actionsPnj = it) }
            Champ("Capacités (« Nom. Description », une par ligne)", pnj.classFeatures, lignes = 2) { pnj = pnj.copy(classFeatures = it) }
            ChampListe("Sorts connus (un par ligne)", pnj.spells) { pnj = pnj.copy(spells = it) }

            Button(onClick = ::enregistrer, modifier = Modifier.fillMaxWidth()) { Text("Enregistrer") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(titre: String) {
    Text(
        titre,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ForcedDarkPalette.AccentGold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun Champ(label: String, valeur: String, modifier: Modifier = Modifier.fillMaxWidth(), lignes: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lignes == 1,
        minLines = lignes,
        modifier = modifier,
        colors = champCouleurs(),
    )
}

/** Liste saisie une valeur par ligne : le texte est gardé tel quel pendant la frappe (lignes vides comprises). */
@Composable
private fun ChampListe(label: String, valeurs: List<String>, onChange: (List<String>) -> Unit) {
    var texte by remember { mutableStateOf(valeurs.joinToString("\n")) }
    OutlinedTextField(
        value = texte,
        onValueChange = { saisie ->
            texte = saisie
            onChange(saisie.lines().map { it.trim() }.filter { it.isNotEmpty() })
        },
        label = { Text(label) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
        colors = champCouleurs(),
    )
}

/** Champ numérique : la saisie vide est tolérée pendant la frappe (valeur conservée). */
@Composable
private fun Nombre(label: String, valeur: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    var texte by remember(valeur) { mutableStateOf(valeur.toString()) }
    OutlinedTextField(
        value = texte,
        onValueChange = { saisie ->
            texte = saisie.filter { it.isDigit() }.take(3)
            texte.toIntOrNull()?.let(onChange)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        colors = champCouleurs(),
    )
}
