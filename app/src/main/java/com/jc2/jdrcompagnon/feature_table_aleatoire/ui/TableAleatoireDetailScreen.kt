package com.jc2.jdrcompagnon.feature_table_aleatoire.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementEditorDialog
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementPickerDialog
import com.jc2.jdrcompagnon.feature_evenement.ui.EventResultDialog
import com.jc2.jdrcompagnon.feature_evenement.ui.TypeEvenementBadge
import com.jc2.jdrcompagnon.feature_evenement.ui.couleur
import com.jc2.jdrcompagnon.feature_evenement.ui.icone
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_table_aleatoire.presentation.TableAleatoireDetailViewModel
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlinx.coroutines.launch

/**
 * Écran d'édition d'une table aléatoire : intervalle en heures de jeu, activation, et entrées
 * dont la nature dépend de [com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable] —
 * événements groupés par catégorie (Rencontre / Découverte / Rumeur-Indice) ou objets de loot
 * (référence SRD, poids, quantité). Chaque liste est pondérée pour le tirage (voir
 * echantillonnerSansRemise). Le compte à rebours s'appuie sur ScenarioClockState.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableAleatoireDetailScreen(
    viewModel: TableAleatoireDetailViewModel,
    worldId: String?,
    onBack: () -> Unit,
) {
    val table by viewModel.table.collectAsState()
    val minutesLecture by viewModel.minutesLecture.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var resultatLoot by remember { mutableStateOf<List<ResultatLoot>?>(null) }
    var resultatEvenement by remember { mutableStateOf<Evenement?>(null) }
    var afficherSelecteur by remember { mutableStateOf(false) }
    var afficherCreation by remember { mutableStateOf(false) }
    var evenementEnEdition by remember { mutableStateOf<Evenement?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(table?.nom ?: "Table aléatoire", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val current = table
        if (current == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
                Text("Chargement...", color = Color.White)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = current.nom,
                onValueChange = { viewModel.mettreAJour(current.copy(nom = it)) },
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            SubsectionCard(current.id, "Déclenchement") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = current.intervalleHeures.toString(),
                        onValueChange = { text ->
                            val heures = text.filter(Char::isDigit).toIntOrNull()
                            if (heures != null) viewModel.mettreAJour(current.copy(intervalleHeures = heures.coerceIn(1, 999)))
                        },
                        label = { Text("Toutes les (heures de jeu)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        Switch(
                            checked = current.active,
                            onCheckedChange = { viewModel.mettreAJour(current.copy(active = it)) }
                        )
                    }
                }
                val statutTexte = when {
                    current.nombreEntrees == 0 -> "Ajoutez au moins une entrée pour activer le tirage."
                    !current.active -> "Table désactivée."
                    minutesLecture == null -> "Aucun scénario en lecture : le temps n'est pas compté."
                    viewModel.estDue(minutesLecture) -> "Prête à se déclencher maintenant."
                    else -> {
                        val restantes = viewModel.minutesRestantes(minutesLecture)
                        "Prochain déclenchement dans ${restantes / 60}h${(restantes % 60).toString().padStart(2, '0')} de jeu."
                    }
                }
                Text(statutTexte, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            when (current.type) {
                TypeTable.EVENEMENTS -> {
                    SubsectionCard(current.id, "Tirage") {
                        TirageEvenementsSection(
                            entrees = current.entreesEvenements,
                            onTirer = { typeFiltre ->
                                val resultat = viewModel.tirerEvenement(typeFiltre)
                                if (resultat != null) {
                                    resultatEvenement = resultat
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Aucun événement disponible pour ce type") }
                                }
                            }
                        )
                    }

                    SubsectionCard(current.id, "Événements (${current.entreesEvenements.size})") {
                        EntreesEvenementSection(
                            entrees = current.entreesEvenements,
                            onAjouter = { afficherSelecteur = true },
                            onCreer = { afficherCreation = true },
                            onModifier = { evenementEnEdition = it },
                            onChangerPoids = viewModel::changerPoids,
                            onRetirer = viewModel::retirerEntree
                        )
                    }
                }

                TypeTable.LOOT -> {
                    SubsectionCard(current.id, "Tirage") {
                        TirageLootSection(
                            entrees = current.entreesLoot,
                            onTirer = { n -> resultatLoot = viewModel.tirerLoot(n) }
                        )
                    }

                    SubsectionCard(current.id, "Objets") {
                        EntreesLootSection(
                            entrees = current.entreesLoot,
                            worldId = worldId,
                            onEntreesChanged = { misesAJour -> viewModel.mettreAJour(current.copy(entreesLoot = misesAJour)) }
                        )
                    }
                }
            }
        }
    }

    resultatLoot?.let { resultats ->
        LootResultDialog(
            titre = table?.nom ?: "Tirage",
            resultats = resultats,
            worldId = worldId,
            onDismiss = { resultatLoot = null }
        )
    }

    resultatEvenement?.let { evenement ->
        EventResultDialog(evenement = evenement, worldId = worldId, onDismiss = { resultatEvenement = null })
    }

    val monde = table?.worldId ?: worldId ?: ""
    if (afficherSelecteur) {
        EvenementPickerDialog(
            worldId = monde,
            exclus = table?.entreesEvenements?.map { it.evenementId }?.toSet() ?: emptySet(),
            onDismiss = { afficherSelecteur = false },
            onValider = { choisis ->
                viewModel.ajouterEvenements(choisis)
                afficherSelecteur = false
            }
        )
    }
    if (afficherCreation) {
        EvenementEditorDialog(
            initial = null,
            worldId = monde,
            onDismiss = { afficherCreation = false },
            onSave = { evenement ->
                viewModel.creerEvenement(evenement)
                afficherCreation = false
            }
        )
    }
    evenementEnEdition?.let { evenement ->
        EvenementEditorDialog(
            initial = evenement,
            worldId = monde,
            onDismiss = { evenementEnEdition = null },
            onSave = { modifie ->
                viewModel.modifierEvenement(modifie)
                evenementEnEdition = null
            }
        )
    }
}

/** Section repliable (flèche en haut à droite), état mémorisé par table et par section. */
@Composable
private fun SubsectionCard(tableId: String, title: String, content: @Composable () -> Unit) {
    CollapsibleSectionCard(ownerKey = "table-aleatoire:$tableId", title = title) { content() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TirageEvenementsSection(
    entrees: List<EntreeEvenement>,
    onTirer: (TypeEvenement?) -> Unit,
) {
    val typesPresents = entrees.mapNotNull { it.evenement?.type }.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = typesPresents.isNotEmpty(),
            onClick = { onTirer(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tirer un événement (tous types)")
        }
        // Un bouton par type présent dans la table, pour ne tirer qu'une seule famille.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeEvenement.entries.filter { it in typesPresents }.forEach { type ->
                OutlinedButton(onClick = { onTirer(type) }) {
                    Icon(type.icone, contentDescription = null, tint = type.couleur, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(type.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

/**
 * Événements de la table, groupés par type : chaque ligne référence un événement de la
 * bibliothèque (tap = le modifier, répercuté partout où il sert), avec son poids de tirage.
 * Retirer une entrée ne supprime pas l'événement de la bibliothèque.
 */
@Composable
private fun EntreesEvenementSection(
    entrees: List<EntreeEvenement>,
    onAjouter: () -> Unit,
    onCreer: () -> Unit,
    onModifier: (Evenement) -> Unit,
    onChangerPoids: (String, Int) -> Unit,
    onRetirer: (String) -> Unit,
) {
    val triees = entrees.sortedWith(
        compareBy<EntreeEvenement>({ it.evenement?.type?.ordinal ?: Int.MAX_VALUE }, { it.evenement?.titre?.lowercase() ?: "" })
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (entrees.isEmpty()) {
            Text(
                "Aucun événement. Ajoutez-en depuis la bibliothèque ou créez-en un.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        triees.forEach { entree ->
            val evenement = entree.evenement
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = evenement != null) { evenement?.let(onModifier) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (evenement != null) TypeEvenementBadge(evenement.type, taille = 28.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        evenement?.titre ?: "Événement supprimé de la bibliothèque",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (evenement != null) Color.White else MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (evenement != null) {
                        Text(
                            evenement.type.label + if (evenement.effets.isNotEmpty()) " · ${evenement.effets.size} effet(s)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = evenement.type.couleur
                        )
                    }
                }
                // Poids de tirage : plus il est élevé, plus l'événement sort souvent.
                IconButton(onClick = { onChangerPoids(entree.id, entree.poids - 1) }, enabled = entree.poids > 1, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Baisser le poids", modifier = Modifier.size(18.dp))
                }
                Text("×${entree.poids}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                IconButton(onClick = { onChangerPoids(entree.id, entree.poids + 1) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Augmenter le poids", modifier = Modifier.size(18.dp))
                }
                // Croix rouge plutôt qu'un lien barré, peu lisible : retire l'entrée de la table
                // (l'événement reste dans la bibliothèque, voir le rappel sous la liste).
                IconButton(onClick = { onRetirer(entree.id) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer de la table", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (entrees.isNotEmpty()) {
            Text(
                "✕ retire l'événement de cette table, sans le supprimer de la bibliothèque.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onAjouter, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bibliothèque")
            }
            Button(onClick = onCreer, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Créer")
            }
        }
    }
}

@Composable
private fun TirageLootSection(
    entrees: List<EntreeLoot>,
    onTirer: (Int) -> Unit,
) {
    var nombre by remember { mutableStateOf("1") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.filter(Char::isDigit) },
                label = { Text("Nombre d'objets") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp),
                singleLine = true
            )
            Spacer(Modifier.width(8.dp))
            Button(
                enabled = entrees.isNotEmpty(),
                onClick = { onTirer((nombre.toIntOrNull() ?: 1).coerceIn(1, entrees.size.coerceAtLeast(1))) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tirer du loot")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntreesLootSection(
    entrees: List<EntreeLoot>,
    worldId: String?,
    onEntreesChanged: (List<EntreeLoot>) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var poids by remember { mutableStateOf("1") }
    var quantiteMin by remember { mutableStateOf("1") }
    var quantiteMax by remember { mutableStateOf("1") }

    LaunchedEffect(query, worldId) {
        results = if (query.length < 2) emptyList() else {
            SrdRepository.loadEquipmentList(context, worldId)
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { it.name }
                .take(6)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entrees.forEachIndexed { index, entree ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val quantiteTexte = if (entree.quantiteMax > entree.quantiteMin) {
                    "${entree.quantiteMin}-${entree.quantiteMax}"
                } else {
                    "${entree.quantiteMin}"
                }
                Text(
                    "${entree.equipementNom} (poids ${entree.poids}, qté $quantiteTexte)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onEntreesChanged(entrees.filterIndexed { i, _ -> i != index }) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer")
                }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Rechercher un objet (équipement SRD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = poids,
                onValueChange = { poids = it.filter(Char::isDigit) },
                label = { Text("Poids") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = quantiteMin,
                onValueChange = { quantiteMin = it.filter(Char::isDigit) },
                label = { Text("Qté min") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = quantiteMax,
                onValueChange = { quantiteMax = it.filter(Char::isDigit) },
                label = { Text("Qté max") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
        results.forEach { name ->
            Text(
                name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val min = quantiteMin.toIntOrNull() ?: 1
                        val max = (quantiteMax.toIntOrNull() ?: 1).coerceAtLeast(min)
                        onEntreesChanged(
                            entrees + EntreeLoot(
                                equipementNom = name,
                                poids = poids.toIntOrNull() ?: 1,
                                quantiteMin = min,
                                quantiteMax = max
                            )
                        )
                        query = ""
                        results = emptyList()
                        poids = "1"
                        quantiteMin = "1"
                        quantiteMax = "1"
                    }
                    .padding(vertical = 4.dp)
            )
        }
    }
}
