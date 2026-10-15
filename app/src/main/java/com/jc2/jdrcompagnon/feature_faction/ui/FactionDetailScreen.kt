package com.jc2.jdrcompagnon.feature_faction.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_faction.data.FactionsStore
import com.jc2.jdrcompagnon.feature_faction.domain.model.AttitudeFaction
import com.jc2.jdrcompagnon.feature_faction.domain.model.Faction
import com.jc2.jdrcompagnon.feature_faction.domain.model.FeteFaction
import com.jc2.jdrcompagnon.feature_faction.domain.model.MembreFaction
import com.jc2.jdrcompagnon.feature_faction.domain.model.ProjetFaction
import com.jc2.jdrcompagnon.feature_faction.domain.model.RelationFaction
import com.jc2.jdrcompagnon.feature_faction.domain.model.TypeFaction
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.feature_preparation.data.CarnetMj
import com.jc2.jdrcompagnon.feature_preparation.ui.ChoixPnjDialog
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private const val OWNER_KEY = "fiche-faction"

/**
 * Fiche d'une faction, modifiable directement (tout est enregistré à la saisie) : identité,
 * intérêts, membres, fêtes, relations avec les autres factions, projets en cours (horloges),
 * réputation des groupes, secrets et notes du MJ.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FactionDetailScreen(factionId: String, onBack: () -> Unit, onOpenPnj: (String) -> Unit, onOpenFaction: (String) -> Unit) {
    val toutes by FactionsStore.factions.collectAsState()
    val f = toutes.firstOrNull { it.id == factionId } ?: return
    fun maj(t: (Faction) -> Faction) = FactionsStore.maj(f.id, t)
    var renommer by remember { mutableStateOf(false) }
    var suppression by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${f.type.emoji} ${f.nom}", fontWeight = FontWeight.Bold, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } },
                actions = {
                    IconButton(onClick = { renommer = true }) { Icon(Icons.Default.Edit, contentDescription = "Renommer", tint = Color.White) }
                    IconButton(onClick = { suppression = true }) { Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (f.devise.isNotBlank()) {
                Text("« ${f.devise} »", fontStyle = FontStyle.Italic, color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.titleMedium)
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Identité") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TypeFaction.entries.forEach { t ->
                        FilterChip(selected = f.type == t, onClick = { maj { it.copy(type = t) } }, label = { Text("${t.emoji} ${t.label}") }, colors = chipCouleurs())
                    }
                }
                Champ("Devise", f.devise) { v -> maj { it.copy(devise = v) } }
                Champ("Ce qu'on en sait (public)", f.description, lignes = 3) { v -> maj { it.copy(description = v) } }
                Champ("Symbole et couleurs", f.symbole) { v -> maj { it.copy(symbole = v) } }
                Champ("Siège / quartier général", f.siege) { v -> maj { it.copy(siege = v) } }
                Champ("Territoires et lieux d'influence", f.territoires, lignes = 2) { v -> maj { it.copy(territoires = v) } }
                CampagnesFaction(f) { ids -> maj { it.copy(campagneIds = ids) } }
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Intérêts et objectifs") {
                Champ("Ce qu'elle veut", f.interets, lignes = 3) { v -> maj { it.copy(interets = v) } }
                Champ("Comment elle agit (méthodes)", f.methodes, lignes = 2) { v -> maj { it.copy(methodes = v) } }
                Champ("Ressources (argent, troupes, savoirs, appuis)", f.ressources, lignes = 2) { v -> maj { it.copy(ressources = v) } }
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Membres", count = f.membres.size) {
                MembresFaction(f, onOpenPnj, ::maj)
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Fêtes et célébrations", count = f.fetes.size) {
                FetesFaction(f, ::maj)
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Relations", count = f.relations.size) {
                RelationsFaction(f, toutes.filter { it.worldId == f.worldId && it.id != f.id }, onOpenFaction, ::maj)
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Projets en cours", count = f.projets.count { it.avancement < it.total }) {
                ProjetsFaction(f, ::maj)
            }

            ReputationGroupes(f)

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Secrets (MJ)") {
                Champ("Ce que les joueurs ignorent", f.secrets, lignes = 3) { v -> maj { it.copy(secrets = v) } }
                Text(
                    "Les secrets à faire découvrir peuvent aussi aller dans « Secrets et indices » du carnet de préparation, reliés aux membres.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FactionHint,
                )
            }

            CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Notes") {
                Champ("Notes", f.notes, lignes = 3) { v -> maj { it.copy(notes = v) } }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (renommer) {
        var nom by remember { mutableStateOf(f.nom) }
        AlertDialog(
            onDismissRequest = { renommer = false },
            title = { Text("Renommer la faction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = nom, onValueChange = { nom = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("La réputation des groupes et des PNJ envers elle suit le nouveau nom.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(enabled = nom.isNotBlank(), onClick = { renommerFaction(f, nom.trim()); renommer = false }) { Text("Renommer") }
            },
            dismissButton = { TextButton(onClick = { renommer = false }) { Text("Annuler") } },
        )
    }
    if (suppression) {
        AlertDialog(
            onDismissRequest = { suppression = false },
            title = { Text("Supprimer ${f.nom} ?") },
            text = { Text("La fiche et ses relations avec les autres factions sont supprimées. Les PNJ et la réputation des groupes ne sont pas touchés.") },
            confirmButton = {
                TextButton(onClick = { suppression = false; FactionsStore.supprimer(f.id); onBack() }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { suppression = false }) { Text("Annuler") } },
        )
    }
}

/** Renomme la faction et reporte le nom dans les réputations (groupes et PNJ) qui la citaient. */
private fun renommerFaction(f: Faction, nouveau: String) {
    val ancien = f.nom
    FactionsStore.maj(f.id) { it.copy(nom = nouveau) }
    if (ancien.equals(nouveau, ignoreCase = false)) return
    GameState.mjGroups.value.filter { g -> g.worldId == f.worldId && g.reputations.any { it.factionName.equals(ancien, ignoreCase = true) } }
        .forEach { g ->
            GameState.updateMjGroup(g.copy(reputations = g.reputations.map { if (it.factionName.equals(ancien, ignoreCase = true)) it.copy(factionName = nouveau) else it }))
        }
    GameState.characters.value.filter { c -> c.factionReputations.any { it.factionName.equals(ancien, ignoreCase = true) } }
        .forEach { c ->
            GameState.updateCharacter(c.copy(factionReputations = c.factionReputations.map { if (it.factionName.equals(ancien, ignoreCase = true)) it.copy(factionName = nouveau) else it }))
        }
}

@Composable
private fun Champ(label: String, valeur: String, lignes: Int = 1, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = lignes == 1,
        minLines = lignes,
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = ForcedDarkPalette.AccentGold,
            unfocusedLabelColor = FactionHint,
            focusedBorderColor = ForcedDarkPalette.AccentGold,
            unfocusedBorderColor = FactionHint.copy(alpha = 0.5f),
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CampagnesFaction(f: Faction, onChange: (List<String>) -> Unit) {
    val campagnes by GameState.mjCampaigns.collectAsState()
    val duMonde = campagnes.filter { it.worldId == f.worldId }
    if (duMonde.isEmpty()) return
    Text("Campagnes où elle intervient", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        duMonde.forEach { c ->
            val choisie = c.id in f.campagneIds
            FilterChip(selected = choisie, onClick = { onChange(if (choisie) f.campagneIds - c.id else f.campagneIds + c.id) }, label = { Text(c.title) }, colors = chipCouleurs())
        }
    }
}

// --- Membres ---

@Composable
private fun MembresFaction(f: Faction, onOpenPnj: (String) -> Unit, maj: ((Faction) -> Faction) -> Unit) {
    val characters by GameState.characters.collectAsState()
    val pnjs = remember(characters, f.worldId) { CarnetMj.pnjsDuMonde(f.worldId, characters) }
    var edition by remember { mutableStateOf<MembreFaction?>(null) }
    var choixPnj by remember { mutableStateOf(false) }
    var libre by remember { mutableStateOf(false) }

    if (f.membres.isEmpty()) Text("Chef, lieutenants, agents, sympathisants...", color = FactionHint)
    f.membres.sortedByDescending { it.chef }.forEach { m ->
        Row(Modifier.fillMaxWidth().clickable { edition = m }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (m.chef) Icon(Icons.Default.Star, contentDescription = "Chef", tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(18.dp))
            else Text(if (m.pnjId != null) "👤" else "•", color = Color.White)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(m.nom, color = Color.White, fontWeight = FontWeight.Bold)
                val detail = listOf(m.rang, m.note).filter { it.isNotBlank() }.joinToString(" · ")
                if (detail.isNotBlank()) Text(detail, color = FactionHint, style = MaterialTheme.typography.bodySmall)
            }
            if (m.pnjId != null) TextButton(onClick = { onOpenPnj(m.pnjId) }) { Text("Fiche", color = ForcedDarkPalette.AccentGold) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { choixPnj = true }, modifier = Modifier.weight(1f)) { Text("Ajouter des PNJ", color = Color.White) }
        OutlinedButton(onClick = { libre = true }, modifier = Modifier.weight(1f)) { Text("Autre membre", color = Color.White) }
    }

    if (choixPnj) {
        val deja = f.membres.mapNotNull { it.pnjId }
        ChoixPnjDialog(pnjs, deja, onDismiss = { choixPnj = false }, onValider = { ids ->
            maj { fac ->
                val gardes = fac.membres.filter { it.pnjId == null || it.pnjId in ids }
                val nouveaux = ids.filter { id -> gardes.none { it.pnjId == id } }
                    .mapNotNull { id -> pnjs.firstOrNull { it.id == id } }
                    .map { MembreFaction(nom = it.name, pnjId = it.id) }
                fac.copy(membres = gardes + nouveaux)
            }
            choixPnj = false
        })
    }
    if (libre || edition != null) {
        MembreDialog(
            initial = edition ?: MembreFaction(nom = ""),
            nouveau = edition == null,
            onDismiss = { libre = false; edition = null },
            onValider = { m ->
                maj { fac ->
                    // Un seul chef : nommer un membre chef retire ce rôle aux autres.
                    val autres = fac.membres.filterNot { it.id == m.id }.map { if (m.chef) it.copy(chef = false) else it }
                    val liste = if (fac.membres.any { it.id == m.id }) fac.membres.map { if (it.id == m.id) m else autres.first { a -> a.id == it.id } }
                    else autres + m
                    fac.copy(membres = liste)
                }
                libre = false; edition = null
            },
            onSupprimer = { m -> maj { fac -> fac.copy(membres = fac.membres.filterNot { it.id == m.id }) }; edition = null },
        )
    }
}

@Composable
private fun MembreDialog(initial: MembreFaction, nouveau: Boolean, onDismiss: () -> Unit, onValider: (MembreFaction) -> Unit, onSupprimer: (MembreFaction) -> Unit) {
    var m by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (nouveau) "Nouveau membre" else m.nom.ifBlank { "Membre" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (m.pnjId == null) OutlinedTextField(value = m.nom, onValueChange = { m = m.copy(nom = it) }, label = { Text("Nom *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = m.rang, onValueChange = { m = m.copy(rang = it) }, label = { Text("Rang / rôle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = m.note, onValueChange = { m = m.copy(note = it) }, label = { Text("Note (loyauté, ambition...)") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chef de la faction", modifier = Modifier.weight(1f))
                    Switch(checked = m.chef, onCheckedChange = { m = m.copy(chef = it) })
                }
            }
        },
        confirmButton = { TextButton(enabled = m.nom.isNotBlank(), onClick = { onValider(m.copy(nom = m.nom.trim())) }) { Text("Enregistrer") } },
        dismissButton = {
            Row {
                if (!nouveau) TextButton(onClick = { onSupprimer(m) }) { Text("Retirer", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Annuler") }
            }
        },
    )
}

// --- Fêtes ---

@Composable
private fun FetesFaction(f: Faction, maj: ((Faction) -> Faction) -> Unit) {
    var edition by remember { mutableStateOf<FeteFaction?>(null) }
    var creation by remember { mutableStateOf(false) }
    if (f.fetes.isEmpty()) Text("Fêtes, rites, commémorations : autant d'occasions de scènes.", color = FactionHint)
    f.fetes.forEach { fete ->
        Column(Modifier.fillMaxWidth().clickable { edition = fete }.padding(vertical = 6.dp)) {
            Text("🎉 ${fete.nom}" + (fete.quand.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""), color = Color.White, fontWeight = FontWeight.Bold)
            if (fete.description.isNotBlank()) Text(fete.description, color = FactionHint, style = MaterialTheme.typography.bodySmall)
        }
    }
    OutlinedButton(onClick = { creation = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Ajouter une fête", color = Color.White)
    }
    if (creation || edition != null) {
        var fete by remember { mutableStateOf(edition ?: FeteFaction(nom = "")) }
        val nouvelle = edition == null
        AlertDialog(
            onDismissRequest = { creation = false; edition = null },
            title = { Text(if (nouvelle) "Nouvelle fête" else "Modifier la fête") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fete.nom, onValueChange = { fete = fete.copy(nom = it) }, label = { Text("Nom *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fete.quand, onValueChange = { fete = fete.copy(quand = it) }, label = { Text("Quand (date, saison...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = fete.description, onValueChange = { fete = fete.copy(description = it) }, label = { Text("Traditions, déroulement") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = fete.nom.isNotBlank(), onClick = {
                    maj { fac -> fac.copy(fetes = if (nouvelle) fac.fetes + fete else fac.fetes.map { if (it.id == fete.id) fete else it }) }
                    creation = false; edition = null
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                Row {
                    if (!nouvelle) TextButton(onClick = { maj { fac -> fac.copy(fetes = fac.fetes.filterNot { it.id == fete.id }) }; edition = null }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { creation = false; edition = null }) { Text("Annuler") }
                }
            },
        )
    }
}

// --- Relations ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationsFaction(f: Faction, autres: List<Faction>, onOpenFaction: (String) -> Unit, maj: ((Faction) -> Faction) -> Unit) {
    var edition by remember { mutableStateOf<RelationFaction?>(null) }
    var creation by remember { mutableStateOf(false) }
    if (autres.isEmpty()) {
        Text("Créez d'autres factions dans ce monde pour décrire leurs alliances et rivalités.", color = FactionHint)
        return
    }
    if (f.relations.isEmpty()) Text("Alliances, rivalités, guerres ouvertes...", color = FactionHint)
    f.relations.forEach { r ->
        val autre = autres.firstOrNull { it.id == r.factionId } ?: return@forEach
        Row(Modifier.fillMaxWidth().clickable { edition = r }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(autre.type.emoji)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(autre.nom, color = Color.White, fontWeight = FontWeight.Bold)
                Text(listOf(r.attitude.label, r.note).filter { it.isNotBlank() }.joinToString(" · "), color = couleurAttitude(r.attitude), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { onOpenFaction(autre.id) }) { Text("Voir", color = ForcedDarkPalette.AccentGold) }
        }
    }
    val disponibles = autres.filter { a -> f.relations.none { it.factionId == a.id } }
    if (disponibles.isNotEmpty()) {
        OutlinedButton(onClick = { creation = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Ajouter une relation", color = Color.White)
        }
    }
    if (creation || edition != null) {
        val nouvelle = edition == null
        var r by remember { mutableStateOf(edition ?: RelationFaction(factionId = disponibles.first().id)) }
        AlertDialog(
            onDismissRequest = { creation = false; edition = null },
            title = { Text(if (nouvelle) "Nouvelle relation" else autres.firstOrNull { it.id == r.factionId }?.nom.orEmpty()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (nouvelle) {
                        Text("Faction", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            disponibles.forEach { a -> FilterChip(selected = r.factionId == a.id, onClick = { r = r.copy(factionId = a.id) }, label = { Text(a.nom) }) }
                        }
                    }
                    Text("Attitude", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AttitudeFaction.entries.forEach { a -> FilterChip(selected = r.attitude == a, onClick = { r = r.copy(attitude = a) }, label = { Text(a.label) }) }
                    }
                    OutlinedTextField(value = r.note, onValueChange = { r = r.copy(note = it) }, label = { Text("Pourquoi, depuis quand") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    maj { fac -> fac.copy(relations = if (nouvelle) fac.relations + r else fac.relations.map { if (it.factionId == r.factionId) r else it }) }
                    creation = false; edition = null
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                Row {
                    if (!nouvelle) TextButton(onClick = { maj { fac -> fac.copy(relations = fac.relations.filterNot { it.factionId == r.factionId }) }; edition = null }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { creation = false; edition = null }) { Text("Annuler") }
                }
            },
        )
    }
}

private fun couleurAttitude(a: AttitudeFaction): Color = when (a) {
    AttitudeFaction.ALLIEE -> Color(0xFF7CD992)
    AttitudeFaction.AMICALE -> Color(0xFFB5E3A1)
    AttitudeFaction.NEUTRE -> FactionHint
    AttitudeFaction.RIVALE -> Color(0xFFFFAA5C)
    AttitudeFaction.ENNEMIE -> Color(0xFFFF8A7A)
}

// --- Projets (horloges) ---

@Composable
private fun ProjetsFaction(f: Faction, maj: ((Faction) -> Faction) -> Unit) {
    var edition by remember { mutableStateOf<ProjetFaction?>(null) }
    var creation by remember { mutableStateOf(false) }
    fun avancer(p: ProjetFaction, case: Int) = maj { fac ->
        // Appui sur la dernière case remplie : on la vide ; sinon on remplit jusqu'à elle.
        val n = if (case + 1 == p.avancement) case else case + 1
        fac.copy(projets = fac.projets.map { if (it.id == p.id) it.copy(avancement = n) else it })
    }
    if (f.projets.isEmpty()) Text("Ce que la faction prépare en coulisses : chaque case remplie la rapproche du but.", color = FactionHint)
    f.projets.forEach { p ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.nom, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { edition = p })
                Text("${p.avancement}/${p.total}", color = if (p.avancement >= p.total) MaterialTheme.colorScheme.error else FactionHint)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                repeat(p.total) { i ->
                    Box(
                        Modifier
                            .size(24.dp)
                            .background(if (i < p.avancement) ForcedDarkPalette.AccentGold else Color.Transparent, RoundedCornerShape(4.dp))
                            .border(1.dp, ForcedDarkPalette.AccentGold, RoundedCornerShape(4.dp))
                            .clickable { avancer(p, i) }
                    )
                }
            }
            if (p.consequence.isNotBlank()) {
                Text(
                    (if (p.avancement >= p.total) "⚠ Accompli : " else "Quand c'est plein : ") + p.consequence,
                    color = if (p.avancement >= p.total) MaterialTheme.colorScheme.error else FactionHint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    OutlinedButton(onClick = { creation = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Ajouter un projet", color = Color.White)
    }
    if (creation || edition != null) {
        val nouveau = edition == null
        var p by remember { mutableStateOf(edition ?: ProjetFaction(nom = "")) }
        AlertDialog(
            onDismissRequest = { creation = false; edition = null },
            title = { Text(if (nouveau) "Nouveau projet" else "Modifier le projet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = p.nom, onValueChange = { p = p.copy(nom = it) }, label = { Text("Projet *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Nombre de cases", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(4, 6, 8, 10).forEach { n -> FilterChip(selected = p.total == n, onClick = { p = p.copy(total = n, avancement = p.avancement.coerceAtMost(n)) }, label = { Text("$n") }) }
                    }
                    OutlinedTextField(value = p.consequence, onValueChange = { p = p.copy(consequence = it) }, label = { Text("Ce qui arrive quand c'est accompli") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(enabled = p.nom.isNotBlank(), onClick = {
                    maj { fac -> fac.copy(projets = if (nouveau) fac.projets + p else fac.projets.map { if (it.id == p.id) p else it }) }
                    creation = false; edition = null
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                Row {
                    if (!nouveau) TextButton(onClick = { maj { fac -> fac.copy(projets = fac.projets.filterNot { it.id == p.id }) }; edition = null }) {
                        Text("Supprimer", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { creation = false; edition = null }) { Text("Annuler") }
                }
            },
        )
    }
}

// --- Réputation ---

/** Réputation des groupes du monde auprès de la faction (lue dans l'outil GROUPES, par nom). */
@Composable
private fun ReputationGroupes(f: Faction) {
    val groupes by GameState.mjGroups.collectAsState()
    val lignes = groupes.filter { it.worldId == f.worldId }.mapNotNull { g ->
        g.reputations.firstOrNull { it.factionName.equals(f.nom, ignoreCase = true) }?.let { g to it.score }
    }
    CollapsibleSectionCard(ownerKey = OWNER_KEY, title = "Réputation des groupes", count = lignes.size) {
        if (lignes.isEmpty()) {
            Text("Aucun groupe n'a encore de réputation auprès d'elle (à régler dans l'outil Groupes, sous ce nom).", color = FactionHint)
        }
        lignes.forEach { (g, score) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(g.name, color = Color.White, modifier = Modifier.weight(1f))
                Text("${if (score > 0) "+" else ""}$score · ${ReputationScale.labelFor(score)}", color = ForcedDarkPalette.AccentGold)
            }
        }
    }
}
