package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportement
import com.jc2.jdrcompagnon.feature_combat.domain.model.Comportements
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneGrille
import com.jc2.jdrcompagnon.feature_combat.domain.model.StrategieCible
import com.jc2.jdrcompagnon.feature_combat.domain.model.StyleCombat
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Outil COMPORTEMENT : comment se battent les combattants pilotés par l'IA.
 * - Grille de comportement : comportement par défaut d'un PJ ou PNJ selon sa classe, quand aucun
 *   joueur ne l'incarne en combat (une fiche peut aussi imposer le sien).
 * - Comportements : les intégrés (Brute, Tireur…, modifiables et réinitialisables) et ceux créés
 *   par le MJ (« + » à droite du titre).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComportementsScreen(onOpenMenu: () -> Unit = {}) {
    val tous by Comportements.tous.collectAsState()
    val grille by Comportements.grille.collectAsState()
    // null = éditeur fermé ; id absent de la liste = création.
    var enEdition by remember { mutableStateOf<Comportement?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Comportement", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
                actions = {
                    IconButton(onClick = {
                        enEdition = Comportement(id = UUID.randomUUID().toString(), nom = "Nouveau comportement")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Nouveau comportement", tint = ForcedDarkPalette.AccentGold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { CarteGrille(grille, tous) }
            item {
                Text(
                    "Comportements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
            items(tous, key = { it.id }) { c ->
                CarteComportementOutil(
                    comportement = c,
                    classes = grille.filter { it.comportementId == c.id }.map { it.classe },
                    onClick = { enEdition = c },
                )
            }
        }
    }

    enEdition?.let { c ->
        EditeurComportement(
            initial = c,
            existe = tous.any { it.id == c.id },
            onEnregistrer = { Comportements.enregistrer(it); enEdition = null },
            onSupprimer = { Comportements.supprimerOuReinitialiser(c.id); enEdition = null },
            onDismiss = { enEdition = null },
        )
    }
}

@Composable
private fun CarteTranslucide(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

/** Grille de comportement : une ligne par classe, plus « Autre classe » pour les classes non citées. */
@Composable
private fun CarteGrille(grille: List<LigneGrille>, tous: List<Comportement>) {
    var nouvelleClasse by remember { mutableStateOf("") }
    CarteTranslucide {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grille de comportement", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold, modifier = Modifier.weight(1f))
            TextButton(onClick = { Comportements.reinitialiserGrille() }) { Text("Réinitialiser") }
        }
        Text(
            "Comportement en combat d'un PJ ou PNJ selon sa classe, quand aucun joueur ne l'incarne. " +
                "Le choix fait sur la fiche d'un personnage reste prioritaire.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
        )
        grille.forEachIndexed { index, ligne ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ligne.classe, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                ChoixComportement(
                    actuel = tous.firstOrNull { it.id == ligne.comportementId },
                    tous = tous,
                    onChoix = { c -> Comportements.definirGrille(grille.toMutableList().apply { this[index] = ligne.copy(comportementId = c.id) }) },
                )
                if (ligne.classe != Comportements.CLASSE_PAR_DEFAUT) {
                    IconButton(onClick = { Comportements.definirGrille(grille.filterIndexed { i, _ -> i != index }) }) {
                        Icon(Icons.Default.Close, contentDescription = "Retirer ${ligne.classe}", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = nouvelleClasse,
                onValueChange = { nouvelleClasse = it },
                label = { Text("Ajouter une classe") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                enabled = nouvelleClasse.isNotBlank() && grille.none { it.classe.equals(nouvelleClasse.trim(), ignoreCase = true) },
                onClick = {
                    // Ajoutée avant « Autre classe », avec le comportement de celle-ci.
                    val defaut = grille.firstOrNull { it.classe == Comportements.CLASSE_PAR_DEFAUT }
                    val ligne = LigneGrille(nouvelleClasse.trim(), defaut?.comportementId ?: tous.first().id)
                    val autres = grille.filter { it.classe != Comportements.CLASSE_PAR_DEFAUT }
                    Comportements.definirGrille(autres + ligne + listOfNotNull(defaut))
                    nouvelleClasse = ""
                },
            ) { Icon(Icons.Default.Add, contentDescription = "Ajouter la classe", tint = ForcedDarkPalette.AccentGold) }
        }
    }
}

/** Menu déroulant de choix d'un comportement. */
@Composable
private fun ChoixComportement(actuel: Comportement?, tous: List<Comportement>, onChoix: (Comportement) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { ouvert = true }) {
            Text(actuel?.nom ?: "—", color = Color.White)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
        }
        DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            tous.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.nom, fontWeight = if (c.id == actuel?.id) FontWeight.Bold else null) },
                    onClick = { onChoix(c); ouvert = false },
                )
            }
        }
    }
}

/** Résumé d'un comportement : cible, style, repli, capacités spéciales. */
internal fun resumeComportement(c: Comportement): String = buildList {
    if (c.imprevisible) add("Change chaque round au hasard")
    else {
        add("Vise ${c.cible.label}")
        add(c.style.label.substringBefore(" :"))
        add(c.seuilFuite?.let { "se replie sous ${(it * 100).roundToInt()} % PV" } ?: "ne fuit jamais")
        if (c.speciauxEnPremier) add("capacités spéciales d'abord")
    }
}.joinToString(" · ")

@Composable
private fun CarteComportementOutil(comportement: Comportement, classes: List<String>, onClick: () -> Unit) {
    val integre = comportement.profilIntegre
    val modifie = integre != null && comportement != Comportement.depuis(integre)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comportement.nom, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    when {
                        integre == null -> "Créé par le MJ"
                        modifie -> "Intégré · modifié"
                        else -> "Intégré"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ForcedDarkPalette.AccentGold,
                )
            }
            if (comportement.description.isNotBlank()) {
                Text(comportement.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
            Text(resumeComportement(comportement), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            if (classes.isNotEmpty()) {
                Text("Grille : ${classes.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Création ou modification d'un comportement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditeurComportement(
    initial: Comportement,
    existe: Boolean,
    onEnregistrer: (Comportement) -> Unit,
    onSupprimer: () -> Unit,
    onDismiss: () -> Unit,
) {
    var c by remember(initial.id) { mutableStateOf(initial) }
    val integre = initial.profilIntegre
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existe) "Modifier le comportement" else "Nouveau comportement", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = c.nom, onValueChange = { c = c.copy(nom = it) }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = c.description, onValueChange = { c = c.copy(description = it) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Imprévisible", style = MaterialTheme.typography.labelLarge)
                        Text("Change chaque round de comportement, tiré au hasard.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = c.imprevisible, onCheckedChange = { c = c.copy(imprevisible = it) })
                }
                if (!c.imprevisible) {
                    Text("Cible privilégiée", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StrategieCible.entries.forEach { s ->
                            FilterChip(selected = c.cible == s, onClick = { c = c.copy(cible = s) }, label = { Text(s.label) })
                        }
                    }
                    Text("Manière de se battre", style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StyleCombat.entries.forEach { s ->
                            FilterChip(selected = c.style == s, onClick = { c = c.copy(style = s) }, label = { Text(s.label) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Capacités spéciales d'abord", style = MaterialTheme.typography.labelLarge)
                            Text("Souffle, sorts, capacités à recharge dès qu'elles touchent quelqu'un.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = c.speciauxEnPremier, onCheckedChange = { c = c.copy(speciauxEnPremier = it) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Se replie quand il est blessé", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Switch(checked = c.seuilFuite != null, onCheckedChange = { c = c.copy(seuilFuite = if (it) 0.25f else null) })
                    }
                    c.seuilFuite?.let { seuil ->
                        Text("Sous ${(seuil * 100).roundToInt()} % de ses PV", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = seuil,
                            onValueChange = { v -> c = c.copy(seuilFuite = (v * 20).roundToInt() / 20f) },
                            valueRange = 0.05f..0.75f,
                        )
                    }
                }
                Text(resumeComportement(c), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                if (existe && (integre == null || c != Comportement.depuis(integre) || initial != Comportement.depuis(integre))) {
                    TextButton(onClick = onSupprimer) {
                        Text(
                            if (integre == null) "Supprimer ce comportement" else "Rétablir les réglages d'origine",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = c.nom.isNotBlank(), onClick = { onEnregistrer(c.copy(nom = c.nom.trim())) }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
