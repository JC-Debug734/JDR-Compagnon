package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.CompositionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneCompositionCombat
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.domain.model.rencontrableDans
import com.jc2.jdrcompagnon.feature_environnement.domain.model.terrainsEffectifs
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.extractInternalLinks

/**
 * Outil "Combat" de l'éditeur de scénario : compose un combat (avec quantités) à partir des
 * monstres, PNJ et créatures présents dans le scénario, et renvoie le texte du lien, ex.
 * `Gobelin x3, Loup x2`, à insérer sous la forme `#combat:[...]`.
 *
 * @param scenarioContent contenu markdown de tout le scénario : seuls les noms liés par
 * `#monster:` / `#pnj:` / `#npc:` (et ceux déjà utilisés dans un `#combat:`) sont proposés.
 * @param environnement environnement sélectionné pour la scène : son bestiaire (monstres
 * suggérés d'après leur habitat + ajoutés à la main) est proposé en plus.
 */
@Composable
fun ComposerCombatDialog(
    scenarioContent: String,
    environnement: Environnement? = null,
    // Monstres attachés à la campagne dont fait partie le scénario.
    bestiaireCampagne: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onInserer: (composition: String) -> Unit,
) {
    val context = LocalContext.current
    val worldId = GameState.currentWorldId()
    var recherche by remember { mutableStateOf("") }
    var lignes by remember { mutableStateOf(listOf<LigneCompositionCombat>()) }
    val duScenario = remember(scenarioContent) { adversairesDuScenario(scenarioContent) }
    var deLEnvironnement by remember(environnement) { mutableStateOf(environnement?.monstresIds.orEmpty()) }

    LaunchedEffect(environnement, worldId) {
        val env = environnement ?: return@LaunchedEffect
        val suggeres = SrdRepository.loadMonsters(context, worldId)
            .filter { it.rencontrableDans(env.terrainsEffectifs) }
            .map { it.name }
        deLEnvironnement = (env.monstresIds + suggeres).distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
    }
    // Un monstre déjà cité dans le scénario n'est pas répété dans la section environnement.
    val envSansDoublon = deLEnvironnement.filter { nom -> duScenario.none { it.equals(nom, ignoreCase = true) } }
    // Idem pour le bestiaire de la campagne (après scénario et environnement).
    val campagneSansDoublon = bestiaireCampagne.filter { nom ->
        (duScenario + envSansDoublon).none { it.equals(nom, ignoreCase = true) }
    }
    val monstres = duScenario + envSansDoublon + campagneSansDoublon

    fun changerQuantite(nom: String, delta: Int) {
        val existante = lignes.firstOrNull { it.monstreNom == nom }
        lignes = if (existante == null) {
            if (delta > 0) lignes + LigneCompositionCombat(nom, delta) else lignes
        } else {
            lignes.mapNotNull { l ->
                if (l.monstreNom != nom) l
                else (l.quantite + delta).takeIf { it > 0 }?.let { l.copy(quantite = it.coerceAtMost(50)) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚔ Insérer un combat", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (lignes.isEmpty()) {
                    Text(
                        "Choisissez un ou plusieurs adversaires du scénario ci-dessous.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    lignes.forEach { ligne ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ligne.monstreNom, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { changerQuantite(ligne.monstreNom, -1) }) { Icon(Icons.Default.Remove, contentDescription = "Un de moins") }
                            Text("${ligne.quantite}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { changerQuantite(ligne.monstreNom, 1) }) { Icon(Icons.Default.Add, contentDescription = "Un de plus") }
                        }
                    }
                }
                HorizontalDivider()
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher un adversaire") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (monstres.isEmpty()) {
                    Text(
                        "Aucun monstre, PNJ ou créature dans ce scénario ni dans l'environnement de la scène. " +
                            "Liez-les d'abord dans les scènes (outil lien : #monster:, #pnj:) ou choisissez " +
                            "un environnement pour la scène.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    fun correspond(nom: String) = recherche.isBlank() || nom.contains(recherche.trim(), ignoreCase = true)
                    val scenarioFiltres = duScenario.filter(::correspond)
                    val envFiltres = envSansDoublon.filter(::correspond)
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        if (scenarioFiltres.isNotEmpty()) {
                            item { SectionAdversaires("Dans le scénario") }
                            items(scenarioFiltres) { nom -> LigneAdversaire(nom) { changerQuantite(nom, 1) } }
                        }
                        if (envFiltres.isNotEmpty()) {
                            item { SectionAdversaires("Environnement : ${environnement?.nom.orEmpty()}") }
                            items(envFiltres) { nom -> LigneAdversaire(nom) { changerQuantite(nom, 1) } }
                        }
                        val campagneFiltres = campagneSansDoublon.filter(::correspond)
                        if (campagneFiltres.isNotEmpty()) {
                            item { SectionAdversaires("Bestiaire de la campagne") }
                            items(campagneFiltres) { nom -> LigneAdversaire(nom) { changerQuantite(nom, 1) } }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = lignes.isNotEmpty(), onClick = { onInserer(CompositionCombat.formater(lignes)) }) {
                Text("Insérer")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun SectionAdversaires(titre: String) {
    Text(
        titre,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun LigneAdversaire(nom: String, onAjouter: () -> Unit) {
    TextButton(onClick = onAjouter, modifier = Modifier.fillMaxWidth()) {
        Text(nom, modifier = Modifier.weight(1f))
        Icon(Icons.Default.Add, contentDescription = "Ajouter")
    }
}

/** Monstres, PNJ et créatures cités dans le scénario (liens internes), sans doublon, triés. */
internal fun adversairesDuScenario(scenarioContent: String): List<String> =
    extractInternalLinks(scenarioContent)
        .flatMap { (type, nom) ->
            when (type) {
                "monster", "pnj", "npc" -> listOf(nom)
                "combat" -> CompositionCombat.parser(nom).map { it.monstreNom }
                else -> emptyList()
            }
        }
        // Un lien sans crochets en fin de phrase (#monster:gobelin.) embarque la ponctuation.
        .map { it.trim().trimEnd('.', ',', ';', ':', '!', '?', ')').trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .sortedBy { it.lowercase() }
