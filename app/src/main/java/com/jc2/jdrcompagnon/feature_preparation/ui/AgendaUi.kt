package com.jc2.jdrcompagnon.feature_preparation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.BoutiqueDependencies
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.di.EnvironmentDependencies
import com.jc2.jdrcompagnon.di.EpreuveDependencies
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.domain.model.EntreeAgenda
import com.jc2.jdrcompagnon.feature_preparation.domain.model.TypeAgenda
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.launch

/** Ouverture des éléments de l'agenda dans l'appli (fournie par la navigation). */
data class NavigationAgenda(
    val ouvrirScenario: (String) -> Unit,
    val ouvrirPnj: (String) -> Unit,
    val ouvrirEnvironnement: (String) -> Unit,
    val ouvrirVille: (campagneId: String, villeId: String) -> Unit,
    val ouvrirBoutique: (String) -> Unit,
    // Combat préparé par LancerCombatDialog : écran du combat en cours.
    val ouvrirCombat: () -> Unit,
)

/**
 * Agenda de la séance : entrées dans l'ordre prévu, cochées une fois jouées. Un appui ouvre
 * l'élément (scénario, fiche PNJ, ville, boutique, environnement) ou le lance (combat, épreuve).
 * [modifiable] : flèches pour réordonner, suppression, bouton d'ajout ; sinon lecture et cases seules.
 */
@Composable
fun AgendaContent(worldId: String, agenda: List<EntreeAgenda>, navigation: NavigationAgenda, modifiable: Boolean) {
    val scope = rememberCoroutineScope()
    var combatALancer by remember { mutableStateOf<String?>(null) }
    var epreuveADemarrer by remember { mutableStateOf<Epreuve?>(null) }
    var epreuveOuverte by remember { mutableStateOf(false) }
    var noteEditee by remember { mutableStateOf<EntreeAgenda?>(null) }
    var ajout by remember { mutableStateOf(false) }

    fun ouvrir(e: EntreeAgenda) {
        val id = e.refId
        when (e.type) {
            TypeAgenda.SCENARIO -> id?.let(navigation.ouvrirScenario)
            TypeAgenda.PNJ -> id?.let(navigation.ouvrirPnj)
            TypeAgenda.ENVIRONNEMENT -> id?.let(navigation.ouvrirEnvironnement)
            TypeAgenda.BOUTIQUE -> id?.let(navigation.ouvrirBoutique)
            TypeAgenda.LIEU -> if (id != null && e.detail.isNotBlank()) navigation.ouvrirVille(e.detail, id)
            TypeAgenda.COMBAT -> combatALancer = e.libelle
            // Boss : lancé par son nom actuel (renommé depuis l'ajout à l'agenda), comme dans l'outil BOSS.
            TypeAgenda.BOSS -> combatALancer = com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue.parId(id)?.nom ?: e.libelle
            TypeAgenda.EPREUVE -> scope.launch {
                val epreuve = EpreuveDependencies.epreuvesDuMonde(worldId).firstOrNull { it.id == id } ?: return@launch
                // La même épreuve déjà en cours reprend où elle en était.
                if (EpreuveOutilSession.etat.value?.epreuve?.id == epreuve.id) epreuveOuverte = true else epreuveADemarrer = epreuve
            }
            TypeAgenda.NOTE -> if (modifiable) noteEditee = e
        }
    }

    if (agenda.isEmpty()) {
        Text(
            "Ajoutez ce qui pourrait se jouer : scénarios, combats, épreuves, lieux, boutiques, PNJ...",
            style = MaterialTheme.typography.bodyMedium,
            color = PrepHintColor,
        )
    }
    agenda.forEachIndexed { index, e ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable { ouvrir(e) }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = e.fait,
                onCheckedChange = { CarnetMj.basculerFait(worldId, e.id) },
                colors = CheckboxDefaults.colors(checkedColor = ForcedDarkPalette.AccentGold, uncheckedColor = Color.White),
            )
            Text(e.type.emoji, modifier = Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    e.libelle,
                    color = if (e.fait) PrepHintColor else Color.White,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (e.fait) TextDecoration.LineThrough else null,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(e.type.label, color = PrepHintColor, style = MaterialTheme.typography.bodySmall)
            }
            if (modifiable) {
                IconButton(onClick = { CarnetMj.deplacerAgenda(worldId, e.id, -1) }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Monter", tint = if (index > 0) Color.White else PrepHintColor.copy(alpha = 0.3f))
                }
                IconButton(onClick = { CarnetMj.deplacerAgenda(worldId, e.id, 1) }, enabled = index < agenda.lastIndex, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Descendre", tint = if (index < agenda.lastIndex) Color.White else PrepHintColor.copy(alpha = 0.3f))
                }
                IconButton(onClick = { CarnetMj.supprimerAgenda(worldId, e.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Retirer de l'agenda", tint = PrepHintColor)
                }
            }
        }
    }
    if (modifiable) {
        OutlinedButton(onClick = { ajout = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ajouter à l'agenda", color = Color.White)
        }
    }

    if (ajout) AjoutAgenda(worldId, agenda, onFini = { ajout = false })
    noteEditee?.let { note ->
        TexteDialog(
            titre = "Note de l'agenda",
            initial = note.libelle,
            onDismiss = { noteEditee = null },
            onValider = { CarnetMj.modifierAgenda(worldId, note.copy(libelle = it)); noteEditee = null },
        )
    }
    combatALancer?.let { composition ->
        com.jc2.jdrcompagnon.feature_combat.ui.LancerCombatDialog(
            composition = composition,
            worldId = worldId,
            onDismiss = { combatALancer = null },
            onLance = { combatALancer = null; navigation.ouvrirCombat() },
        )
    }
    if (epreuveOuverte) {
        com.jc2.jdrcompagnon.feature_epreuve.ui.EpreuveResolutionDialog(onDismiss = { epreuveOuverte = false })
    }
    com.jc2.jdrcompagnon.feature_epreuve.ui.DemandeDifficulteEpreuve(
        demande = epreuveADemarrer,
        onLancee = { epreuveADemarrer = null; epreuveOuverte = true },
        onDismiss = { epreuveADemarrer = null },
    )
}

/** Élément proposé dans un choix de l'agenda (id de l'appli, nom, précision, détail stocké). */
private data class ElementChoix(val id: String, val libelle: String, val precision: String = "", val detail: String = "")

/** Ajout à l'agenda : choix du type, puis de ses éléments (ou saisie pour un combat ou une note). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AjoutAgenda(worldId: String, agenda: List<EntreeAgenda>, onFini: () -> Unit) {
    var type by remember { mutableStateOf<TypeAgenda?>(null) }
    val context = LocalContext.current
    val dejaChoisis: (TypeAgenda) -> List<String> = { t -> agenda.filter { it.type == t }.mapNotNull { it.refId } }

    when (type) {
        null -> AlertDialog(
            onDismissRequest = onFini,
            title = { Text("Ajouter à l'agenda") },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeAgenda.entries.forEach { t ->
                        OutlinedButton(onClick = { type = t }) { Text("${t.emoji} ${t.label}") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onFini) { Text("Annuler") } },
        )

        TypeAgenda.SCENARIO -> {
            val scenarios = rememberScenariosMonde(worldId)
            ChoixScenariosDialog(worldId, dejaChoisis(TypeAgenda.SCENARIO), onDismiss = onFini, onValider = { ids ->
                CarnetMj.definirAgendaType(worldId, TypeAgenda.SCENARIO, ids.mapNotNull { id ->
                    scenarios.firstOrNull { it.id == id }?.let { EntreeAgenda(type = TypeAgenda.SCENARIO, libelle = it.title, refId = it.id) }
                })
                onFini()
            })
        }

        TypeAgenda.PNJ -> {
            val pnjs = rememberPnjsMonde(worldId)
            ChoixPnjDialog(pnjs, dejaChoisis(TypeAgenda.PNJ), onDismiss = onFini, onValider = { ids ->
                CarnetMj.definirAgendaType(worldId, TypeAgenda.PNJ, ids.mapNotNull { id ->
                    pnjs.firstOrNull { it.id == id }?.let { EntreeAgenda(type = TypeAgenda.PNJ, libelle = it.name, refId = it.id) }
                })
                onFini()
            })
        }

        TypeAgenda.BOSS -> {
            val boss by com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue.tous.collectAsState()
            ChoixElements(
                worldId, TypeAgenda.BOSS, "Combats de boss",
                boss.sortedBy { it.nom.lowercase() }.map { ElementChoix(it.id, it.nom, it.monstreBase) },
                dejaChoisis(TypeAgenda.BOSS), onFini,
            )
        }

        TypeAgenda.EPREUVE -> {
            val elements by produceState(emptyList<ElementChoix>(), worldId) {
                value = EpreuveDependencies.epreuvesDuMonde(worldId).sortedBy { it.nom.lowercase() }
                    .map { ElementChoix(it.id, it.nom, it.difficulte?.libelle.orEmpty()) }
            }
            ChoixElements(worldId, TypeAgenda.EPREUVE, "Épreuves", elements, dejaChoisis(TypeAgenda.EPREUVE), onFini)
        }

        TypeAgenda.ENVIRONNEMENT -> {
            val environnements by remember(worldId) { EnvironmentDependencies.repository.observerEnvironnements(worldId) }.collectAsState(initial = emptyList())
            ChoixElements(
                worldId, TypeAgenda.ENVIRONNEMENT, "Environnements",
                environnements.sortedBy { it.nom.lowercase() }.map { ElementChoix(it.id, it.nom) },
                dejaChoisis(TypeAgenda.ENVIRONNEMENT), onFini,
            )
        }

        TypeAgenda.BOUTIQUE -> {
            val campagnes = rememberCampagnesMonde(worldId)
            val boutiques by remember { BoutiqueDependencies.repository.observerToutesLesBoutiques() }.collectAsState(initial = emptyList())
            val villes by remember { CarteDependencies.repository.observerToutesLesVilles() }.collectAsState(initial = emptyList())
            val idsCampagnes = campagnes.map { it.id }.toSet()
            ChoixElements(
                worldId, TypeAgenda.BOUTIQUE, "Boutiques",
                boutiques.filter { it.campagneId == null || it.campagneId in idsCampagnes }.sortedBy { it.nom.lowercase() }.map { b ->
                    ElementChoix(b.id, b.nom, villes.firstOrNull { it.id == b.villeId }?.nom.orEmpty())
                },
                dejaChoisis(TypeAgenda.BOUTIQUE), onFini,
            )
        }

        TypeAgenda.LIEU -> {
            val campagnes = rememberCampagnesMonde(worldId)
            val villes by remember { CarteDependencies.repository.observerToutesLesVilles() }.collectAsState(initial = emptyList())
            ChoixElements(
                worldId, TypeAgenda.LIEU, "Lieux",
                villes.filter { v -> campagnes.any { it.id == v.campagneId } }.sortedBy { it.nom.lowercase() }.map { v ->
                    ElementChoix(v.id, v.nom, campagnes.firstOrNull { it.id == v.campagneId }?.title.orEmpty(), detail = v.campagneId)
                },
                dejaChoisis(TypeAgenda.LIEU), onFini,
                libre = "Autre lieu (saisie libre)",
            )
        }

        TypeAgenda.COMBAT -> {
            val monstres by produceState(emptyList<String>(), worldId) {
                value = runCatching { SrdRepository.loadMonsters(context, worldId.ifBlank { null }).map { it.name } }.getOrDefault(emptyList())
            }
            CombatDialog(monstres, onDismiss = onFini, onValider = { composition ->
                CarnetMj.ajouterAgenda(worldId, listOf(EntreeAgenda(type = TypeAgenda.COMBAT, libelle = composition)))
                onFini()
            })
        }

        TypeAgenda.NOTE -> TexteDialog(titre = "Note de l'agenda", initial = "", onDismiss = onFini, onValider = {
            CarnetMj.ajouterAgenda(worldId, listOf(EntreeAgenda(type = TypeAgenda.NOTE, libelle = it)))
            onFini()
        })
    }
}

/** Choix multiple avec recherche ; [libre] non null : champ pour un élément saisi librement. */
@Composable
private fun ChoixElements(
    worldId: String,
    type: TypeAgenda,
    titre: String,
    elements: List<ElementChoix>,
    selection: List<String>,
    onFini: () -> Unit,
    libre: String? = null,
) {
    var choix by remember { mutableStateOf(selection.toSet()) }
    var recherche by remember { mutableStateOf("") }
    var texteLibre by remember { mutableStateOf("") }
    val q = recherche.trim().lowercase()
    val affiches = elements.filter { q.isEmpty() || it.libelle.lowercase().contains(q) || it.precision.lowercase().contains(q) }
    AlertDialog(
        onDismissRequest = onFini,
        title = { Text(titre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = recherche, onValueChange = { recherche = it }, label = { Text("Rechercher") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    if (affiches.isEmpty()) Text("Aucun élément.", style = MaterialTheme.typography.bodySmall)
                    affiches.forEach { el ->
                        Row(
                            Modifier.fillMaxWidth().clickable { choix = if (el.id in choix) choix - el.id else choix + el.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = el.id in choix, onCheckedChange = { choix = if (it) choix + el.id else choix - el.id })
                            Column {
                                Text(el.libelle, fontWeight = FontWeight.Bold)
                                if (el.precision.isNotBlank()) Text(el.precision, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                if (libre != null) {
                    OutlinedTextField(value = texteLibre, onValueChange = { texteLibre = it }, label = { Text(libre) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                CarnetMj.definirAgendaType(worldId, type, elements.filter { it.id in choix }.map {
                    EntreeAgenda(type = type, libelle = it.libelle, refId = it.id, detail = it.detail)
                })
                texteLibre.trim().takeIf { it.isNotBlank() }?.let {
                    CarnetMj.ajouterAgenda(worldId, listOf(EntreeAgenda(type = type, libelle = it)))
                }
                onFini()
            }) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onFini) { Text("Annuler") } },
    )
}

/**
 * Composition d'un combat, au format des liens `#combat:` (« Gobelin x3, Loup x2 ») : un appui
 * sur un monstre trouvé l'ajoute, ou augmente sa quantité s'il y est déjà.
 */
@Composable
private fun CombatDialog(monstres: List<String>, onDismiss: () -> Unit, onValider: (String) -> Unit) {
    var composition by remember { mutableStateOf("") }
    var recherche by remember { mutableStateOf("") }
    val q = recherche.trim()
    val trouves = if (q.length < 2) emptyList() else monstres.filter { it.contains(q, ignoreCase = true) }.take(8)

    fun ajouter(nom: String) {
        val parties = composition.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        val i = parties.indexOfFirst { it.substringBeforeLast(" x").trim().equals(nom, ignoreCase = true) }
        if (i >= 0) {
            val n = parties[i].substringAfterLast(" x", "1").trim().toIntOrNull() ?: 1
            parties[i] = "$nom x${n + 1}"
        } else parties += "$nom x1"
        composition = parties.joinToString(", ")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Combat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = composition,
                    onValueChange = { composition = it },
                    label = { Text("Adversaires (ex. Gobelin x3, Loup x2)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(value = recherche, onValueChange = { recherche = it }, label = { Text("Chercher un monstre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                trouves.forEach { nom ->
                    Text(
                        "+ $nom",
                        color = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.fillMaxWidth().clickable { ajouter(nom) }.padding(vertical = 6.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = composition.isNotBlank(), onClick = { onValider(composition.trim()) }) { Text("Ajouter") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Saisie d'un texte (note de l'agenda, note rapide). */
@Composable
internal fun TexteDialog(titre: String, initial: String, onDismiss: () -> Unit, onValider: (String) -> Unit) {
    var texte by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre) },
        text = { OutlinedTextField(value = texte, onValueChange = { texte = it }, minLines = 3, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(enabled = texte.isNotBlank(), onClick = { onValider(texte.trim()) }) { Text("Enregistrer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
