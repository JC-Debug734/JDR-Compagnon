package com.jc2.jdrcompagnon.feature_environnement.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.feature_environnement.domain.model.CapaciteEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DifficulteRelative
import com.jc2.jdrcompagnon.feature_environnement.domain.model.DureeEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EchelleEpreuve
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import com.jc2.jdrcompagnon.feature_environnement.domain.model.GraviteDegats
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeCapacite
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TypeEpreuve
import com.jc2.jdrcompagnon.feature_environnement.presentation.EpreuveSession
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import kotlin.math.roundToInt

/**
 * Carte d'une épreuve d'environnement : repliée elle montre le résumé, dépliée (toucher la carte)
 * tout son contenu — pulsions, compétences, capacités, adversaires.
 */
@Composable
internal fun EpreuveCard(
    epreuve: EpreuveEnvironnementale,
    onLancer: () -> Unit,
    onModifier: (() -> Unit)?,
    onSupprimer: (() -> Unit)?
) {
    var deplie by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().clickable { deplie = !deplie }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(epreuve.nom, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${epreuve.type.label} · ${epreuve.duree.label} · Menace ${epreuve.menaceMax} · ${epreuve.capacites.size} capacité(s)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (deplie) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (deplie) "Replier" else "Voir le contenu"
                )
            }
            if (deplie) {
                Spacer(Modifier.height(8.dp))
                EpreuveContenu(epreuve)
            } else if (epreuve.description.isNotBlank()) {
                Text(epreuve.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onLancer) { Text("Lancer") }
                if (onModifier != null) TextButton(onClick = onModifier) { Text("Modifier") }
                Spacer(Modifier.weight(1f))
                if (onSupprimer != null) {
                    IconButton(onClick = onSupprimer) { Icon(Icons.Default.Delete, contentDescription = "Supprimer") }
                }
            }
        }
    }
}

/**
 * Contenu complet d'une épreuve, en valeurs relatives (les DD/dégâts chiffrés dépendent du groupe
 * et ne sont connus qu'au lancement).
 */
@Composable
internal fun EpreuveContenu(epreuve: EpreuveEnvironnementale) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (epreuve.description.isNotBlank()) Text(epreuve.description, style = MaterialTheme.typography.bodyMedium)
        if (epreuve.pulsions.isNotEmpty()) LigneDetail("Pulsions", epreuve.pulsions.joinToString(", "))
        if (epreuve.competences.isNotEmpty()) LigneDetail("Compétences utiles", epreuve.competences.joinToString(", "))
        TypeCapacite.entries.forEach { type ->
            val capacites = epreuve.capacites.filter { it.type == type }
            if (capacites.isNotEmpty()) {
                Text("${type.label}s", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                capacites.forEach { capacite ->
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(capacite.nom, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        EchelleEpreuve.libelleRelatif(capacite)?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (capacite.description.isNotBlank()) Text(capacite.description, style = MaterialTheme.typography.bodySmall)
                        capacite.question?.let { Text("« $it »", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic) }
                    }
                }
            }
        }
        if (epreuve.adversaires.isNotEmpty()) LigneDetail("Adversaires possibles", epreuve.adversaires.joinToString(", "))
    }
}

@Composable
private fun LigneDetail(titre: String, valeur: String) {
    Text("$titre : $valeur", style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun Stepper(label: String, valeur: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        IconButton(onClick = { onChange((valeur - 1).coerceAtLeast(min)) }, enabled = valeur > min) {
            Icon(Icons.Default.Remove, contentDescription = "Diminuer $label")
        }
        Text("$valeur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = { onChange((valeur + 1).coerceAtMost(max)) }, enabled = valeur < max) {
            Icon(Icons.Default.Add, contentDescription = "Augmenter $label")
        }
    }
}

/**
 * Réglage du dosage avant lancement : niveau et nombre de joueurs calculés depuis les
 * personnages connectés quand une partie réseau est hébergée, sinon saisis par le MJ.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LancerEpreuveDialog(
    epreuve: EpreuveEnvironnementale,
    onDismiss: () -> Unit,
    onLancee: () -> Unit
) {
    val connectes = remember { NetworkSessionManager.connectedCharacters() }
    val niveauAutoCalcule = connectes.takeIf { it.isNotEmpty() }?.map { it.level }?.average()?.roundToInt()
    var manuel by remember { mutableStateOf(niveauAutoCalcule == null) }
    var niveau by remember { mutableIntStateOf(niveauAutoCalcule ?: 1) }
    var nbJoueurs by remember { mutableIntStateOf(connectes.size.takeIf { it > 0 } ?: 4) }
    var duree by remember { mutableStateOf(epreuve.duree) }
    val dejaEnCours = EpreuveSession.etat.value?.let { it.issue == null } == true

    val niveauEffectif = if (manuel || niveauAutoCalcule == null) niveau else niveauAutoCalcule
    val nbEffectif = if (manuel || niveauAutoCalcule == null) nbJoueurs else connectes.size
    val tier = EchelleEpreuve.tier(niveauEffectif)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lancer : ${epreuve.nom}") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                var contenuVisible by remember { mutableStateOf(false) }
                TextButton(onClick = { contenuVisible = !contenuVisible }) {
                    Text(if (contenuVisible) "Masquer le contenu" else "Voir le contenu de l'épreuve")
                }
                if (contenuVisible) {
                    EpreuveContenu(epreuve)
                    Spacer(Modifier.height(8.dp))
                }
                if (niveauAutoCalcule != null) {
                    Text(
                        "${connectes.size} personnage(s) connecté(s) : " +
                            connectes.joinToString { "${it.name} (niv. ${it.level})" },
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Réglage manuel", modifier = Modifier.weight(1f))
                        Switch(checked = manuel, onCheckedChange = { manuel = it })
                    }
                } else {
                    Text("Aucun joueur connecté : choisissez le niveau et la taille du groupe.", style = MaterialTheme.typography.bodySmall)
                }
                if (manuel || niveauAutoCalcule == null) {
                    Stepper("Niveau moyen", niveau, 1, 20) { niveau = it }
                    Stepper("Joueurs", nbJoueurs, 1, 10) { nbJoueurs = it }
                }
                Spacer(Modifier.height(8.dp))
                Text("Durée", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DureeEpreuve.entries.forEach { d ->
                        FilterChip(selected = duree == d, onClick = { duree = d }, label = { Text(d.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Tier $tier (${EchelleEpreuve.niveauxDuTier(tier)}) · niveau $niveauEffectif · $nbEffectif joueur(s)", style = MaterialTheme.typography.labelLarge)
                        Text("Progrès ${EchelleEpreuve.progresMax(duree, nbEffectif)} · Menace ${epreuve.menaceMax}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            DifficulteRelative.entries.joinToString(" · ") { "${it.label} DD ${EchelleEpreuve.dd(tier, it)}" },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (dejaEnCours) {
                    Spacer(Modifier.height(8.dp))
                    Text("Une épreuve est déjà en cours : elle sera remplacée.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                EpreuveSession.demarrer(
                    epreuve = epreuve,
                    niveau = niveauEffectif,
                    nbJoueurs = nbEffectif,
                    niveauAuto = !manuel && niveauAutoCalcule != null,
                    duree = duree
                )
                onLancee()
            }) { Text("Lancer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

private fun String.enListe(): List<String> = split(",").map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Création ([initiale] null) ou modification d'une épreuve (valeurs relatives uniquement, dosées
 * au lancement).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreerEpreuveDialog(
    onDismiss: () -> Unit,
    onConfirmer: (EpreuveEnvironnementale) -> Unit,
    initiale: EpreuveEnvironnementale? = null
) {
    var nom by remember { mutableStateOf(initiale?.nom.orEmpty()) }
    var type by remember { mutableStateOf(initiale?.type ?: TypeEpreuve.TRAVERSEE) }
    var description by remember { mutableStateOf(initiale?.description.orEmpty()) }
    var pulsions by remember { mutableStateOf(initiale?.pulsions.orEmpty().joinToString(", ")) }
    var competences by remember { mutableStateOf(initiale?.competences.orEmpty().joinToString(", ")) }
    var adversaires by remember { mutableStateOf(initiale?.adversaires.orEmpty().joinToString(", ")) }
    var duree by remember { mutableStateOf(initiale?.duree ?: DureeEpreuve.STANDARD) }
    var menaceMax by remember { mutableIntStateOf(initiale?.menaceMax ?: 4) }
    val capacites = remember { mutableStateListOf<CapaciteEpreuve>().apply { addAll(initiale?.capacites.orEmpty()) } }
    var ajoutCapacite by remember { mutableStateOf(false) }
    // Index de la capacité en cours de modification (null = aucune).
    var capaciteEditee by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large) {
            Column(modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                Text(if (initiale == null) "Nouvelle épreuve" else "Modifier l'épreuve", style = MaterialTheme.typography.titleLarge)
                if (initiale != null) {
                    Text(
                        "Renommer l'épreuve casse les liens #epreuve: des scénarios qui utilisent l'ancien nom.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeEpreuve.entries.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) }) }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = pulsions, onValueChange = { pulsions = it }, label = { Text("Pulsions (séparées par des virgules)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = competences, onValueChange = { competences = it }, label = { Text("Compétences utiles (virgules)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = adversaires, onValueChange = { adversaires = it }, label = { Text("Adversaires possibles (virgules)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Durée (longueur du Progrès)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DureeEpreuve.entries.forEach { d -> FilterChip(selected = duree == d, onClick = { duree = d }, label = { Text(d.label) }) }
                }
                Stepper("Menace", menaceMax, 2, 8) { menaceMax = it }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Capacités", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { ajoutCapacite = true }) { Icon(Icons.Default.Add, contentDescription = "Ajouter une capacité") }
                }
                if (capacites.isEmpty()) {
                    Text("Aucune capacité", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                capacites.forEachIndexed { index, capacite ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { capaciteEditee = index }) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("[${capacite.type.label}] ${capacite.nom}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            EchelleEpreuve.libelleRelatif(capacite)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                            if (capacite.description.isNotBlank()) Text(capacite.description, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { capaciteEditee = index }) { Icon(Icons.Default.Edit, contentDescription = "Modifier") }
                        IconButton(onClick = { capacites.removeAt(index) }) { Icon(Icons.Default.Delete, contentDescription = "Retirer") }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Annuler") }
                    Button(
                        enabled = nom.isNotBlank(),
                        onClick = {
                            onConfirmer(
                                EpreuveEnvironnementale(
                                    nom = nom.trim(),
                                    type = type,
                                    description = description.trim(),
                                    pulsions = pulsions.enListe(),
                                    competences = competences.enListe(),
                                    duree = duree,
                                    menaceMax = menaceMax,
                                    capacites = capacites.toList(),
                                    adversaires = adversaires.enListe()
                                )
                            )
                        }
                    ) { Text(if (initiale == null) "Créer" else "Enregistrer") }
                }
            }
        }
    }

    if (ajoutCapacite) {
        AjouterCapaciteDialog(
            onDismiss = { ajoutCapacite = false },
            onConfirmer = { capacites.add(it); ajoutCapacite = false }
        )
    }

    capaciteEditee?.let { index ->
        AjouterCapaciteDialog(
            initiale = capacites[index],
            onDismiss = { capaciteEditee = null },
            onConfirmer = { capacites[index] = it; capaciteEditee = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AjouterCapaciteDialog(
    onDismiss: () -> Unit,
    onConfirmer: (CapaciteEpreuve) -> Unit,
    initiale: CapaciteEpreuve? = null
) {
    var nom by remember { mutableStateOf(initiale?.nom.orEmpty()) }
    var type by remember { mutableStateOf(initiale?.type ?: TypeCapacite.REACTION) }
    var description by remember { mutableStateOf(initiale?.description.orEmpty()) }
    var sauvegarde by remember { mutableStateOf(initiale?.sauvegarde) }
    var difficulte by remember { mutableStateOf(initiale?.difficulte) }
    var gravite by remember { mutableStateOf(initiale?.degats) }
    var typeDegats by remember { mutableStateOf(initiale?.typeDegats.orEmpty()) }
    var question by remember { mutableStateOf(initiale?.question.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initiale == null) "Nouvelle capacité" else "Modifier la capacité") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = nom, onValueChange = { nom = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeCapacite.entries.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label) }) }
                }
                Text(
                    when (type) {
                        TypeCapacite.PASSIVE -> "Toujours active."
                        TypeCapacite.REACTION -> "Se déclenche sur un échec lié."
                        TypeCapacite.ACTION -> "Jouée par le MJ après un échec."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic
                )
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Effet") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Jet de sauvegarde (optionnel)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("FOR", "DEX", "CON", "INT", "SAG", "CHA").forEach { carac ->
                        FilterChip(selected = sauvegarde == carac, onClick = { sauvegarde = if (sauvegarde == carac) null else carac }, label = { Text(carac) })
                    }
                }
                Text("Difficulté (optionnel)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DifficulteRelative.entries.forEach { d ->
                        FilterChip(selected = difficulte == d, onClick = { difficulte = if (difficulte == d) null else d }, label = { Text(d.label) })
                    }
                }
                Text("Dégâts (optionnel)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GraviteDegats.entries.forEach { g ->
                        FilterChip(selected = gravite == g, onClick = { gravite = if (gravite == g) null else g }, label = { Text(g.label) })
                    }
                }
                if (gravite != null) {
                    OutlinedTextField(value = typeDegats, onValueChange = { typeDegats = it }, label = { Text("Type de dégâts (feu, contondant…)") }, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = question, onValueChange = { question = it }, label = { Text("Question au groupe (optionnel)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                enabled = nom.isNotBlank(),
                onClick = {
                    onConfirmer(
                        CapaciteEpreuve(
                            nom = nom.trim(),
                            type = type,
                            description = description.trim(),
                            sauvegarde = sauvegarde,
                            difficulte = difficulte,
                            degats = gravite,
                            typeDegats = typeDegats.trim().takeIf { gravite != null && it.isNotEmpty() },
                            question = question.trim().takeIf { it.isNotEmpty() }
                        )
                    )
                }
            ) { Text(if (initiale == null) "Ajouter" else "Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
