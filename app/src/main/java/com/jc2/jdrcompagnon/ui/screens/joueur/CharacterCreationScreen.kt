package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.CharacterCreationWizard
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Espece
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EspeceParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Historique
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.HistoriqueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Langue
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.LangueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.versCharacter
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdSectionEntry

/**
 * Écran de création de personnage joueur. Demande d'abord le nom (le wizard
 * lui-même n'a pas ce champ), puis enchaîne sur CharacterCreationWizard —
 * classe / origines / caractéristiques / alignement / récapitulatif, chaque
 * choix demandant confirmation. Les options viennent de la bibliothèque SRD
 * (classes_srd521.md, historiques_srd521.md, especes_srd521.md, langues.md) via
 * SrdRepository, converties en Classe/Historique/Espece par les parsers de
 * SrdCreationParsers.kt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterCreationScreen(
    currentWorld: WorldState?,
    isMjMode: Boolean = false,
    onCharacterCreated: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val worldId = currentWorld?.id ?: "donjon_et_dragon"
    var nomPersonnage by remember { mutableStateOf<String?>(null) }

    val nom = nomPersonnage
    if (nom == null) {
        EtapeNomPersonnage(onValider = { nomPersonnage = it }, onBack = onBack)
        return
    }

    var classes by remember { mutableStateOf<List<Classe>?>(null) }
    var historiques by remember { mutableStateOf<List<Historique>?>(null) }
    var especes by remember { mutableStateOf<List<Espece>?>(null) }
    var langues by remember { mutableStateOf<List<Langue>?>(null) }
    var sorts by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }

    LaunchedEffect(worldId) {
        classes = ClasseParser.parse(
            SrdRepository.loadClasses(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        )
        historiques = HistoriqueParser.parse(
            SrdRepository.loadHistoriques(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        )
        especes = EspeceParser.parse(
            SrdRepository.loadEspeces(context, worldId).joinToString("\n\n") { it.rawMarkdown }
        )
        langues = LangueParser.parse(SrdRepository.loadLangues(context, worldId))
        // Sorts mineurs + niveau 1 seulement : rien d'autre n'est castable au niveau 1.
        sorts = SrdRepository.loadSpellsIndex(context, worldId)
            .filter { it.category == "Sorts mineurs" || it.category == "Sorts de niveau 1" }
    }

    val classesChargees = classes
    val historiquesCharges = historiques
    val especesChargees = especes
    val languesChargees = langues
    val sortsCharges = sorts

    if (classesChargees == null || historiquesCharges == null || especesChargees == null ||
        languesChargees == null || sortsCharges == null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    CharacterCreationWizard(
        classes = classesChargees,
        historiques = historiquesCharges,
        especes = especesChargees,
        langues = languesChargees,
        sortsDisponibles = sortsCharges,
        onTermine = { draft ->
            val createdBy = if (isMjMode) "MJ" else "Joueur"
            GameState.addCharacter(draft.versCharacter(nom = nom, worldId = worldId, createdBy = createdBy))
            onCharacterCreated()
        },
        onAnnuler = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EtapeNomPersonnage(onValider: (String) -> Unit, onBack: () -> Unit) {
    var saisie by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un personnage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Quel est le nom de votre personnage ?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = saisie,
                onValueChange = { saisie = it },
                label = { Text("Nom du héros") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { onValider(saisie.trim()) },
                enabled = saisie.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Commencer la création") }
        }
    }
}