package com.jc2.jdrcompagnon.feature_group.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_carte.presentation.appliquerReputationAuGroupe
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupAsset
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupItem
import com.jc2.jdrcompagnon.feature_group.domain.model.Mount
import com.jc2.jdrcompagnon.feature_group.domain.model.MountKind
import com.jc2.jdrcompagnon.feature_group.domain.model.ReputationScale
import com.jc2.jdrcompagnon.feature_group.domain.model.Transport
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val Hint = Color.White.copy(alpha = 0.8f)

/** Lieu affiché d'un élément du groupe : le sien, sinon celui du groupe. */
internal fun lieuDe(location: String, group: GameState.MjGroup): String =
    location.ifBlank { group.location.ifBlank { "Avec le groupe" } }

/** Boîte de dialogue de saisie d'un lieu (voir LocationField). */
@Composable
internal fun LocationDialog(title: String, initial: String, hint: String? = null, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var lieu by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LocationField(value = lieu, onValueChange = { lieu = it })
                hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(lieu.trim()) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun SectionHeader(title: String, onAdd: (() -> Unit)? = null, addLabel: String = "Ajouter") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { SubsectionTitle(title) }
        if (onAdd != null) {
            TextButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text(addLabel)
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
}

private fun MutableList<String>.ajouterSi(condition: Boolean, texte: String) { if (condition) add(texte) }

/** Champ numérique commun aux boîtes de dialogue ci-dessous. */
@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit, allowNegative: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filterIndexed { i, c -> c.isDigit() || (allowNegative && c == '-' && i == 0) }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

// --- Lieu du groupe et trésor commun ---

@Composable
internal fun GroupLocationSubsection(group: GameState.MjGroup) {
    var editLieu by remember { mutableStateOf(false) }
    var editOr by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubsectionTitle("Position et trésor")
        Row(Modifier.fillMaxWidth().clickable { editLieu = true }, verticalAlignment = Alignment.CenterVertically) {
            Text("📍 Le groupe est à : ", color = Hint)
            Text(group.location.ifBlank { "lieu non précisé" }, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = "Changer le lieu", tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.fillMaxWidth().clickable { editOr = true }, verticalAlignment = Alignment.CenterVertically) {
            Text("💰 Trésor commun : ", color = Hint)
            Text("${group.gold} po", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = "Modifier le trésor", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
    if (editLieu) {
        LocationDialog(
            title = "Où se trouve le groupe ?",
            initial = group.location,
            hint = "Les membres, montures et objets sans lieu propre sont réputés être ici.",
            onDismiss = { editLieu = false },
            onConfirm = { GameState.updateMjGroup(group.copy(location = it)); editLieu = false },
        )
    }
    if (editOr) {
        var delta by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { editOr = false },
            title = { Text("Trésor commun : ${group.gold} po") },
            text = { NumberField("Montant (po)", delta, { delta = it }) },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        delta.toIntOrNull()?.let { GameState.updateMjGroup(group.copy(gold = (group.gold - it).coerceAtLeast(0))) }
                        editOr = false
                    }) { Text("Retirer") }
                    TextButton(onClick = {
                        delta.toIntOrNull()?.let { GameState.updateMjGroup(group.copy(gold = group.gold + it)) }
                        editOr = false
                    }) { Text("Ajouter") }
                }
            },
            dismissButton = { TextButton(onClick = { editOr = false }) { Text("Annuler") } },
        )
    }
}

// --- Répartition par lieu : où se trouve quoi ---

@Composable
internal fun WhereIsWhatSubsection(group: GameState.MjGroup, members: List<Character>) {
    val entrees = buildList {
        members.forEach { add(lieuDe(it.location, group) to "${if (it.type == "PJ") "🧙" else "👤"} ${it.name}") }
        group.mounts.forEach { add(lieuDe(it.location, group) to "${if (it.kind == MountKind.MONTURE) "🐎" else "🐾"} ${it.name}") }
        group.transports.forEach { add(lieuDe(it.location, group) to "🛒 ${it.name}") }
        group.inventory.forEach { add(lieuDe(it.location, group) to "🎒 ${it.name}" + if (it.quantity > 1) " ×${it.quantity}" else "") }
        group.assets.forEach { add(lieuDe(it.location, group) to "🏠 ${it.name}") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubsectionTitle("Où se trouve quoi")
        if (entrees.isEmpty()) EmptyHint("Rien à localiser pour l'instant.")
        entrees.groupBy({ it.first }, { it.second })
            .toSortedMap(compareBy { it.lowercase() })
            .forEach { (lieu, elements) ->
                Text("📍 $lieu", color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                Text(elements.joinToString("  ·  "), color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
    }
}

// --- PNJ de la campagne sélectionnée (hors groupe), par lieu ---

@Composable
internal fun CampaignPnjsSubsection(group: GameState.MjGroup, onOpenCharacter: (Character) -> Unit) {
    val campaigns by GameState.mjCampaigns.collectAsState()
    val campaignId by GameState.currentCampaignId.collectAsState()
    val characters by GameState.characters.collectAsState()
    val campaign = campaigns.firstOrNull { it.id == campaignId && it.worldId == group.worldId }
    val pnjs = campaign?.let { c -> characters.filter { it.id in c.pnjIds && it.id !in group.memberIds } }.orEmpty()
    var locating by remember { mutableStateOf<Character?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubsectionTitle("PNJ de la campagne — ${pnjs.size}")
        when {
            campaign == null -> EmptyHint("Sélectionnez une campagne pour voir ses PNJ.")
            pnjs.isEmpty() -> EmptyHint("Aucun PNJ affecté à « ${campaign.title} » (éditeur de campagne).")
        }
        pnjs.groupBy { it.location.ifBlank { "Lieu non précisé" } }
            .toSortedMap(compareBy<String> { it == "Lieu non précisé" }.thenBy { it.lowercase() })
            .forEach { (lieu, liste) ->
                Text("📍 $lieu", color = ForcedDarkPalette.AccentGold, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                liste.forEach { pnj ->
                    Row(Modifier.fillMaxWidth().clickable { onOpenCharacter(pnj) }, verticalAlignment = Alignment.CenterVertically) {
                        val attitude = pnj.groupReputations[group.id]?.let { " — ${ReputationScale.labelFor(it)}" } ?: ""
                        Text(pnj.name + attitude, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { locating = pnj }) {
                            Icon(Icons.Default.Edit, contentDescription = "Changer le lieu", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
    }
    locating?.let { pnj ->
        LocationDialog(
            title = "Où se trouve ${pnj.name} ?",
            initial = pnj.location,
            onDismiss = { locating = null },
            onConfirm = { GameState.setCharacterLocation(pnj.id, it); locating = null },
        )
    }
}

// --- Montures et animaux ---

@Composable
internal fun MountsSubsection(group: GameState.MjGroup, members: List<Character>, onOpenBestiaryDetail: (String) -> Unit) {
    var editing by remember { mutableStateOf<Mount?>(null) }
    var creating by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Montures et animaux — ${group.mounts.size}", onAdd = { creating = true })
        if (group.mounts.isEmpty()) EmptyHint("Aucune monture ni animal.")
        group.mounts.forEach { mount ->
            val cavalier = mount.riderCharacterId?.let { id -> members.firstOrNull { it.id == id } ?: GameState.characters.value.firstOrNull { it.id == id } }
            val attelage = mount.transportId?.let { id -> group.transports.firstOrNull { it.id == id } }
            Row(Modifier.fillMaxWidth().clickable { editing = mount }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${if (mount.kind == MountKind.MONTURE) "🐎" else "🐾"} ${mount.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    val details = mutableListOf<String>()
                    details.ajouterSi(mount.species.isNotBlank(), mount.species)
                    details.ajouterSi(mount.kind == MountKind.MONTURE, "VIT ${mount.speed} m")
                    details.ajouterSi(cavalier != null, if (mount.kind == MountKind.MONTURE) "montée par ${cavalier?.name}" else "compagnon de ${cavalier?.name}")
                    details.ajouterSi(attelage != null, "attelée à ${attelage?.name}")
                    details += "📍 ${lieuDe(mount.location, group)}"
                    Text(details.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = Hint)
                }
                if (mount.species.isNotBlank()) {
                    TextButton(onClick = { onOpenBestiaryDetail(mount.species) }) { Text("Profil") }
                }
            }
        }
    }
    if (creating || editing != null) {
        MountDialog(
            group = group,
            members = members,
            initial = editing,
            onDismiss = { creating = false; editing = null },
        )
    }
}

@Composable
private fun MountDialog(group: GameState.MjGroup, members: List<Character>, initial: Mount?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var species by remember { mutableStateOf(initial?.species ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: MountKind.MONTURE) }
    var speed by remember { mutableStateOf((initial?.speed ?: 18).toString()) }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var riderId by remember { mutableStateOf(initial?.riderCharacterId) }
    var transportId by remember { mutableStateOf(initial?.transportId) }
    val cavaliers = members.filter { it.type == "PJ" || it.type == "PNJ" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle monture / animal" else initial.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MountKind.entries.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label) }) }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = species, onValueChange = { species = it }, label = { Text("Espèce (ex. Cheval de trait)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (kind == MountKind.MONTURE) NumberField("Vitesse (m)", speed, { speed = it })
                LocationField(value = location, onValueChange = { location = it }, modifier = Modifier.fillMaxWidth())

                Text(if (kind == MountKind.MONTURE) "Cavalier (prend la vitesse de la monture)" else "Maître", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().clickable { riderId = null }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = riderId == null, onClick = { riderId = null })
                    Text("Personne")
                }
                cavaliers.forEach { c ->
                    Row(Modifier.fillMaxWidth().clickable { riderId = c.id; transportId = null }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = riderId == c.id, onClick = { riderId = c.id; transportId = null })
                        Text("${c.name} (${c.type})")
                    }
                }
                if (kind == MountKind.MONTURE && group.transports.isNotEmpty()) {
                    Text("Attelée à", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth().clickable { transportId = null }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = transportId == null, onClick = { transportId = null })
                        Text("Aucun véhicule")
                    }
                    group.transports.forEach { t ->
                        Row(Modifier.fillMaxWidth().clickable { transportId = t.id; riderId = null }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = transportId == t.id, onClick = { transportId = t.id; riderId = null })
                            Text(t.name)
                        }
                    }
                }
                if (initial != null) {
                    TextButton(onClick = { GameState.removeMount(group.id, initial.id); onDismiss() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Retirer du groupe", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val base = (initial ?: Mount(name = "")).copy(
                        name = name.trim(),
                        species = species.trim(),
                        kind = kind,
                        speed = speed.toIntOrNull()?.takeIf { it > 0 } ?: 18,
                        location = location.trim(),
                    )
                    if (initial == null) {
                        GameState.updateMjGroup(group.copy(mounts = group.mounts + base))
                    } else {
                        GameState.updateMount(group.id, base)
                    }
                    // Cavalier puis attelage : chacun gère la vitesse et l'exclusivité (voir GameState).
                    GameState.assignMountRider(group.id, base.id, if (kind == MountKind.MONTURE && transportId != null) null else riderId)
                    if (kind == MountKind.MONTURE) GameState.harnessMount(group.id, base.id, transportId)
                    onDismiss()
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// --- Véhicules ---

@Composable
internal fun TransportsSubsection(group: GameState.MjGroup, onOpenEquipmentDetail: (String) -> Unit) {
    var editing by remember { mutableStateOf<Transport?>(null) }
    var creating by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionHeader("Véhicules — ${group.transports.size}", onAdd = { creating = true })
        if (group.transports.isEmpty()) EmptyHint("Aucun véhicule.")
        group.transports.forEach { transport ->
            val attelees = group.mounts.filter { it.transportId == transport.id }
            val vitesse = transport.vitesse(group.mounts)
            Row(Modifier.fillMaxWidth().clickable { editing = transport }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🛒 ${transport.name}", color = Color.White, fontWeight = FontWeight.Bold)
                    val details = mutableListOf<String>()
                    details.ajouterSi(transport.type.isNotBlank(), transport.type)
                    details.ajouterSi(transport.capacity > 0, "capacité ${transport.capacity}")
                    details += "📍 ${lieuDe(transport.location, group)}"
                    Text(details.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = Hint)
                    Text(
                        when {
                            vitesse == null && transport.needsMount -> "⚠ Immobile : attelez une monture"
                            vitesse == null -> "Vitesse non renseignée"
                            attelees.isNotEmpty() -> "Tiré par ${attelees.joinToString { it.name }} — VIT $vitesse m"
                            else -> "VIT $vitesse m"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vitesse == null) MaterialTheme.colorScheme.error else Color.White,
                    )
                }
                if (transport.type.isNotBlank()) {
                    TextButton(onClick = { onOpenEquipmentDetail(transport.type) }) { Text("Fiche") }
                }
            }
        }
    }
    if (creating || editing != null) {
        TransportDialog(group = group, initial = editing, onDismiss = { creating = false; editing = null })
    }
}

@Composable
private fun TransportDialog(group: GameState.MjGroup, initial: Transport?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Chariot") }
    var capacity by remember { mutableStateOf(initial?.capacity?.takeIf { it > 0 }?.toString() ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var needsMount by remember { mutableStateOf(initial?.needsMount ?: true) }
    var ownSpeed by remember { mutableStateOf(initial?.ownSpeed?.takeIf { it > 0 }?.toString() ?: "") }
    val transportId = remember { initial?.id ?: java.util.UUID.randomUUID().toString() }
    var harnessed by remember { mutableStateOf(group.mounts.filter { it.transportId == transportId }.map { it.id }.toSet()) }
    val montures = group.mounts.filter { it.kind == MountKind.MONTURE }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouveau véhicule" else initial.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (Chariot, Charrette, Barque...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                NumberField("Capacité (passagers)", capacity, { capacity = it })
                LocationField(value = location, onValueChange = { location = it }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Doit être tiré par une monture", modifier = Modifier.weight(1f))
                    Switch(checked = needsMount, onCheckedChange = { needsMount = it })
                }
                if (needsMount) {
                    Text("Montures attelées", style = MaterialTheme.typography.labelLarge)
                    if (montures.isEmpty()) Text("Aucune monture dans le groupe.", style = MaterialTheme.typography.bodySmall)
                    montures.forEach { m ->
                        val ailleurs = m.transportId != null && m.transportId != transportId
                        Row(
                            Modifier.fillMaxWidth().clickable { harnessed = if (m.id in harnessed) harnessed - m.id else harnessed + m.id },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = m.id in harnessed, onCheckedChange = { harnessed = if (it) harnessed + m.id else harnessed - m.id })
                            Text(
                                "${m.name} (VIT ${m.speed} m)" + when {
                                    ailleurs -> " — attelée ailleurs"
                                    m.riderCharacterId != null -> " — montée"
                                    else -> ""
                                }
                            )
                        }
                    }
                } else {
                    NumberField("Vitesse propre (m)", ownSpeed, { ownSpeed = it })
                }
                if (initial != null) {
                    TextButton(onClick = { GameState.removeTransport(group.id, initial.id); onDismiss() }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Retirer du groupe", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    val transport = (initial ?: Transport(id = transportId, name = "")).copy(
                        name = name.trim(),
                        type = type.trim(),
                        capacity = capacity.toIntOrNull() ?: 0,
                        location = location.trim(),
                        needsMount = needsMount,
                        ownSpeed = ownSpeed.toIntOrNull() ?: 0,
                    )
                    val transports = if (initial == null) group.transports + transport else group.transports.map { if (it.id == transport.id) transport else it }
                    GameState.updateMjGroup(group.copy(transports = transports))
                    // Attelage : les montures cochées sont attelées (leur cavalier descend), les autres dételées.
                    montures.forEach { m ->
                        val veut = needsMount && m.id in harnessed
                        if (veut && m.transportId != transport.id) GameState.harnessMount(group.id, m.id, transport.id)
                        if (!veut && m.transportId == transport.id) GameState.harnessMount(group.id, m.id, null)
                    }
                    onDismiss()
                },
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// --- Réputation ---

@Composable
internal fun ReputationSubsection(group: GameState.MjGroup) {
    var adding by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Réputation", onAdd = { adding = true }, addLabel = "Faction")
        if (group.reputations.isEmpty()) EmptyHint("Aucune réputation enregistrée pour l'instant.")
        group.reputations.sortedByDescending { it.score }.forEach { reputation ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(reputation.factionName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${ReputationScale.labelFor(reputation.score)} (${reputation.score})", color = Hint, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { appliquerReputationAuGroupe(group.id, reputation.factionName, -5) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Baisser", tint = Color.White)
                }
                IconButton(onClick = { appliquerReputationAuGroupe(group.id, reputation.factionName, 5) }) {
                    Icon(Icons.Default.Add, contentDescription = "Augmenter", tint = Color.White)
                }
                IconButton(onClick = {
                    GameState.updateMjGroup(group.copy(reputations = group.reputations.filterNot { it.factionId == reputation.factionId }))
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (adding) {
        var faction by remember { mutableStateOf("") }
        var score by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("Réputation auprès d'une faction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = faction, onValueChange = { faction = it }, label = { Text("Faction") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    NumberField("Score (-100 à 100)", score, { score = it }, allowNegative = true)
                }
            },
            confirmButton = {
                TextButton(enabled = faction.isNotBlank(), onClick = {
                    appliquerReputationAuGroupe(group.id, faction.trim(), score.toIntOrNull() ?: 0)
                    adding = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Annuler") } },
        )
    }
}

// --- Inventaire commun ---

@Composable
internal fun InventorySubsection(group: GameState.MjGroup, members: List<Character>, onOpenEquipmentDetail: (String) -> Unit) {
    var editing by remember { mutableStateOf<GroupItem?>(null) }
    var creating by remember { mutableStateOf(false) }
    var giving by remember { mutableStateOf<GroupItem?>(null) }
    val pj = members.filter { it.type == "PJ" || it.type == "PNJ" }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Inventaire du groupe — ${group.inventory.sumOf { it.quantity }}", onAdd = { creating = true })
        if (group.inventory.isEmpty()) EmptyHint("Inventaire vide. Les équipements gagnés en quête arrivent ici.")
        group.inventory.forEach { item ->
            Row(Modifier.fillMaxWidth().clickable { editing = item }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name + if (item.quantity > 1) " ×${item.quantity}" else "",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onOpenEquipmentDetail(item.name) },
                    )
                    Text("📍 ${lieuDe(item.location, group)}", style = MaterialTheme.typography.bodySmall, color = Hint)
                }
                if (pj.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { giving = item }) {
                            Icon(Icons.Default.Send, contentDescription = "Donner à un membre", tint = Color.White)
                        }
                        DropdownMenu(expanded = giving?.id == item.id, onDismissRequest = { giving = null }) {
                            pj.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("Donner à ${c.name}") },
                                    onClick = {
                                        GameState.addNamedItemToBackpack(c.id, item.name)
                                        val reste = item.quantity - 1
                                        GameState.updateMjGroup(
                                            group.copy(
                                                inventory = if (reste > 0) group.inventory.map { if (it.id == item.id) it.copy(quantity = reste) else it }
                                                else group.inventory.filterNot { it.id == item.id }
                                            )
                                        )
                                        giving = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        val initial = editing
        var name by remember { mutableStateOf(initial?.name ?: "") }
        var quantity by remember { mutableStateOf((initial?.quantity ?: 1).toString()) }
        var location by remember { mutableStateOf(initial?.location ?: "") }
        val close = { creating = false; editing = null }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(if (initial == null) "Nouvel objet" else initial.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Objet") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    NumberField("Quantité", quantity, { quantity = it })
                    LocationField(value = location, onValueChange = { location = it }, label = "Entreposé à (vide = porté)", modifier = Modifier.fillMaxWidth())
                    if (initial != null) {
                        TextButton(onClick = {
                            GameState.updateMjGroup(group.copy(inventory = group.inventory.filterNot { it.id == initial.id }))
                            close()
                        }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val item = (initial ?: GroupItem(name = "")).copy(name = name.trim(), quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1, location = location.trim())
                    GameState.updateMjGroup(
                        group.copy(inventory = if (initial == null) group.inventory + item else group.inventory.map { if (it.id == item.id) item else it })
                    )
                    close()
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = close) { Text("Annuler") } },
        )
    }
}

// --- Biens ---

@Composable
internal fun AssetsSubsection(group: GameState.MjGroup) {
    var editing by remember { mutableStateOf<GroupAsset?>(null) }
    var creating by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader("Biens — ${group.assets.size}", onAdd = { creating = true })
        if (group.assets.isEmpty()) EmptyHint("Aucun bien (maison, taverne, terrain...).")
        group.assets.forEach { asset ->
            Column(Modifier.fillMaxWidth().clickable { editing = asset }) {
                Text("🏠 ${asset.name}", color = Color.White, fontWeight = FontWeight.Bold)
                val details = listOfNotNull(asset.description.ifBlank { null }, "📍 ${lieuDe(asset.location, group)}").joinToString(" • ")
                Text(details, style = MaterialTheme.typography.bodySmall, color = Hint)
            }
        }
    }
    if (creating || editing != null) {
        val initial = editing
        var name by remember { mutableStateOf(initial?.name ?: "") }
        var description by remember { mutableStateOf(initial?.description ?: "") }
        var location by remember { mutableStateOf(initial?.location ?: "") }
        val close = { creating = false; editing = null }
        AlertDialog(
            onDismissRequest = close,
            title = { Text(if (initial == null) "Nouveau bien" else initial.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    LocationField(value = location, onValueChange = { location = it }, modifier = Modifier.fillMaxWidth())
                    if (initial != null) {
                        TextButton(onClick = {
                            GameState.updateMjGroup(group.copy(assets = group.assets.filterNot { it.id == initial.id }))
                            close()
                        }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val asset = (initial ?: GroupAsset(name = "")).copy(name = name.trim(), description = description.trim(), location = location.trim())
                    GameState.updateMjGroup(
                        group.copy(assets = if (initial == null) group.assets + asset else group.assets.map { if (it.id == asset.id) asset else it })
                    )
                    close()
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = close) { Text("Annuler") } },
        )
    }
}
