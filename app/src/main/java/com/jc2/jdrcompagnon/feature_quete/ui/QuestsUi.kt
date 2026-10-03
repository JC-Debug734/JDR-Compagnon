package com.jc2.jdrcompagnon.feature_quete.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_group.ui.LocationField
import com.jc2.jdrcompagnon.feature_group.ui.rememberLieuxConnus
import com.jc2.jdrcompagnon.feature_quete.domain.ValiderQueteUseCase
import com.jc2.jdrcompagnon.feature_quete.domain.model.Quest
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestReward
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestRewardType
import com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val HintColor = Color.White.copy(alpha = 0.8f)

/**
 * Quêtes d'une campagne côté MJ (éditeur et page de campagne) : en cours d'abord, puis terminées
 * et échouées. Chaque modification est enregistrée immédiatement (GameState.upsertQuest), comme
 * les villes ou les cartes : la validation distribue des récompenses, elle ne peut pas attendre le
 * bouton « Enregistrer » de l'éditeur.
 */
@Composable
fun ColumnScope.CampaignQuestsContent(campaign: GameState.MjCampaign) {
    var editing by remember { mutableStateOf<Quest?>(null) }
    var creating by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf<Quest?>(null) }
    val characters by GameState.characters.collectAsState()

    if (campaign.quests.isEmpty()) {
        Text(
            "Aucune quête. Créez-en une : description pour les joueurs, lieu, récompenses (XP, or, équipement, réputation, PNJ, monture, animal...).",
            style = MaterialTheme.typography.bodyMedium,
            color = HintColor,
        )
    }
    QuestStatus.entries.forEach { status ->
        val quetes = campaign.quests.filter { it.status == status }
        if (quetes.isEmpty()) return@forEach
        Text(
            "${status.label} — ${quetes.size}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ForcedDarkPalette.AccentGold,
            modifier = Modifier.padding(top = 4.dp),
        )
        quetes.forEach { quest ->
            QuestRow(
                quest = quest,
                giverName = quest.giverCharacterId?.let { id -> characters.firstOrNull { it.id == id }?.name },
                onClick = { editing = quest },
                onValidate = { validating = quest },
            )
        }
    }
    OutlinedButton(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Nouvelle quête")
    }

    if (creating || editing != null) {
        QuestEditorDialog(
            campaign = campaign,
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onValidate = { quest -> creating = false; editing = null; validating = quest },
        )
    }
    validating?.let { quest ->
        ValidateQuestDialog(campaign = campaign, quest = quest, onDismiss = { validating = null })
    }
}

@Composable
private fun QuestRow(quest: Quest, giverName: String?, onClick: () -> Unit, onValidate: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            when (quest.status) {
                QuestStatus.EN_COURS -> Icons.Default.Flag
                QuestStatus.EN_ATTENTE -> Icons.Default.HourglassEmpty
                QuestStatus.TERMINEE -> Icons.Default.CheckCircle
                QuestStatus.ECHOUEE -> Icons.Default.Close
            },
            contentDescription = quest.status.label,
            tint = if (quest.status == QuestStatus.ECHOUEE) MaterialTheme.colorScheme.error else ForcedDarkPalette.AccentGold,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                quest.title.ifBlank { "(sans titre)" },
                color = if (quest.status == QuestStatus.EN_COURS) Color.White else HintColor,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = listOfNotNull(
                quest.location.ifBlank { null }?.let { "📍 $it" },
                giverName?.let { "par $it" },
                quest.rewards.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.resume },
            ).joinToString(" · ")
            if (detail.isNotBlank()) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = HintColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(
            if (quest.visibleToPlayers) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (quest.visibleToPlayers) "Visible des joueurs" else "Cachée aux joueurs",
            tint = if (quest.visibleToPlayers) Color.White else HintColor.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp),
        )
        if (quest.status == QuestStatus.EN_COURS) {
            IconButton(onClick = onValidate) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Valider la quête", tint = ForcedDarkPalette.AccentGold)
            }
        }
    }
}

/** Création / modification d'une quête, enregistrée à la confirmation. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuestEditorDialog(
    campaign: GameState.MjCampaign,
    initial: Quest?,
    onDismiss: () -> Unit,
    onValidate: (Quest) -> Unit,
) {
    val characters by GameState.characters.collectAsState()
    val pnjs = remember(characters, campaign.worldId) {
        characters.filter { it.type != "PJ" && (it.worldId == campaign.worldId || it.worldId.isBlank()) }.sortedBy { it.name.lowercase() }
    }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var mjNotes by remember { mutableStateOf(initial?.mjNotes ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var giverId by remember { mutableStateOf(initial?.giverCharacterId) }
    var status by remember { mutableStateOf(initial?.status ?: QuestStatus.EN_COURS) }
    var visible by remember { mutableStateOf(initial?.visibleToPlayers ?: false) }
    var rewardsVisible by remember { mutableStateOf(initial?.rewardsVisibleToPlayers ?: false) }
    var rewards by remember { mutableStateOf(initial?.rewards ?: emptyList()) }
    var editingReward by remember { mutableStateOf<QuestReward?>(null) }
    var addingReward by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val dejaRecompensee = initial?.rewardedGroupId != null

    fun build(): Quest = (initial ?: Quest(title = "")).copy(
        title = title.trim(),
        description = description.trim(),
        mjNotes = mjNotes.trim(),
        location = location.trim(),
        giverCharacterId = giverId,
        status = status,
        visibleToPlayers = visible,
        rewardsVisibleToPlayers = rewardsVisible,
        rewards = rewards,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle quête" else "Modifier la quête") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descriptif (vu par les joueurs)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = mjNotes,
                    onValueChange = { mjNotes = it },
                    label = { Text("Notes du MJ (secrètes)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                LocationField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Lieu de la quête",
                    lieux = rememberLieuxConnus(campaign.id),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Donneur de quête
                var giverMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = giverMenu, onExpandedChange = { giverMenu = it }) {
                    OutlinedTextField(
                        value = giverId?.let { id -> pnjs.firstOrNull { it.id == id }?.name } ?: "Aucun",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Confiée par (PNJ)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(giverMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = giverMenu, onDismissRequest = { giverMenu = false }) {
                        DropdownMenuItem(text = { Text("Aucun") }, onClick = { giverId = null; giverMenu = false })
                        pnjs.forEach { pnj ->
                            DropdownMenuItem(
                                text = { Text(pnj.name + (pnj.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")) },
                                onClick = { giverId = pnj.id; giverMenu = false },
                            )
                        }
                    }
                }

                Text("Statut", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestStatus.entries.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) })
                    }
                }
                if (status == QuestStatus.TERMINEE && !dejaRecompensee && rewards.isNotEmpty()) {
                    Text(
                        "Pour distribuer les récompenses, utilisez plutôt « Valider et récompenser ».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Visible des joueurs", modifier = Modifier.weight(1f))
                    Switch(checked = visible, onCheckedChange = { visible = it })
                }
                if (visible) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Récompenses annoncées aux joueurs", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = rewardsVisible, onCheckedChange = { rewardsVisible = it })
                    }
                }

                HorizontalDivider()
                Text("Récompenses", style = MaterialTheme.typography.labelLarge)
                if (rewards.isEmpty()) {
                    Text("Aucune récompense.", style = MaterialTheme.typography.bodySmall)
                }
                rewards.forEach { reward ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !dejaRecompensee) { editingReward = reward },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("• ${reward.resume}", modifier = Modifier.weight(1f))
                        if (!dejaRecompensee) {
                            IconButton(onClick = { rewards = rewards.filterNot { it.id == reward.id } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (dejaRecompensee) {
                    Text("Récompenses déjà distribuées :", style = MaterialTheme.typography.labelMedium)
                    Text(initial?.rewardSummary.orEmpty(), style = MaterialTheme.typography.bodySmall)
                } else {
                    TextButton(onClick = { addingReward = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Ajouter une récompense")
                    }
                }

                if (initial != null) {
                    HorizontalDivider()
                    TextButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Supprimer la quête", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (!dejaRecompensee && status == QuestStatus.EN_COURS && initial != null) {
                    TextButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            val quest = build()
                            GameState.upsertQuest(campaign.id, quest)
                            onValidate(quest)
                        },
                    ) { Text("Valider…") }
                }
                TextButton(
                    enabled = title.isNotBlank(),
                    onClick = {
                        GameState.upsertQuest(campaign.id, build())
                        onDismiss()
                    },
                ) { Text("Enregistrer") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )

    if (addingReward || editingReward != null) {
        RewardEditorDialog(
            initial = editingReward,
            worldId = campaign.worldId,
            pnjs = pnjs,
            onDismiss = { addingReward = false; editingReward = null },
            onSave = { reward ->
                rewards = if (rewards.any { it.id == reward.id }) rewards.map { if (it.id == reward.id) reward else it } else rewards + reward
                addingReward = false
                editingReward = null
            },
        )
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Supprimer la quête") },
            text = { Text("Supprimer « ${initial.title} » ? Les récompenses déjà distribuées ne sont pas reprises.") },
            confirmButton = {
                TextButton(onClick = {
                    GameState.removeQuest(campaign.id, initial.id)
                    confirmDelete = false
                    onDismiss()
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } },
        )
    }
}

/** Saisie d'une récompense : les champs demandés dépendent du type (voir QuestReward). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RewardEditorDialog(
    initial: QuestReward?,
    worldId: String,
    pnjs: List<com.jc2.jdrcompagnon.ui.Character>,
    onDismiss: () -> Unit,
    onSave: (QuestReward) -> Unit,
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initial?.type ?: QuestRewardType.XP) }
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.takeIf { it != 0 }?.toString() ?: "") }
    var detail by remember { mutableStateOf(initial?.detail ?: "") }
    var characterId by remember { mutableStateOf(initial?.characterId) }
    val groups by GameState.mjGroups.collectAsState()
    val factions = remember(groups) { groups.flatMap { g -> g.reputations.map { it.factionName } }.distinct().sorted() }

    // Suggestions d'équipement (SRD du monde) pendant la saisie du nom.
    var equipements by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(worldId) {
        equipements = runCatching { SrdRepository.loadEquipmentList(context, worldId.ifBlank { null }).map { it.name } }.getOrDefault(emptyList())
    }

    val amount = amountText.toIntOrNull() ?: 0
    val valide = when (type) {
        QuestRewardType.XP, QuestRewardType.OR -> amount > 0
        QuestRewardType.REPUTATION -> label.isNotBlank() && amount != 0
        QuestRewardType.PNJ -> characterId != null
        else -> label.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nouvelle récompense" else "Modifier la récompense") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var typeMenu by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }) {
                    OutlinedTextField(
                        value = type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        QuestRewardType.entries.forEach { t ->
                            DropdownMenuItem(text = { Text(t.label) }, onClick = { type = t; typeMenu = false })
                        }
                    }
                }

                @Composable
                fun champNombre(libelle: String, negatifAutorise: Boolean = false) = OutlinedTextField(
                    value = amountText,
                    onValueChange = { v -> amountText = v.filterIndexed { i, c -> c.isDigit() || (negatifAutorise && c == '-' && i == 0) } },
                    label = { Text(libelle) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                @Composable
                fun champTexte(libelle: String, valeur: String, onChange: (String) -> Unit, suggestions: List<String> = emptyList()) {
                    OutlinedTextField(value = valeur, onValueChange = onChange, label = { Text(libelle) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (valeur.length >= 2) {
                        suggestions.filter { it.contains(valeur.trim(), ignoreCase = true) && !it.equals(valeur.trim(), ignoreCase = true) }
                            .take(5)
                            .forEach { s ->
                                Text(
                                    s,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth().clickable { onChange(s) }.padding(vertical = 4.dp),
                                )
                            }
                    }
                }

                when (type) {
                    QuestRewardType.XP -> {
                        champNombre("Points d'expérience (total)")
                        Text("Répartis équitablement entre les joueurs du groupe.", style = MaterialTheme.typography.bodySmall)
                    }
                    QuestRewardType.OR -> {
                        champNombre("Pièces d'or (total)")
                        Text("Réparties entre les joueurs ; le reste va au trésor du groupe.", style = MaterialTheme.typography.bodySmall)
                    }
                    QuestRewardType.EQUIPEMENT -> {
                        champTexte("Objet", label, { label = it }, equipements)
                        champNombre("Quantité")
                    }
                    QuestRewardType.REPUTATION -> {
                        champTexte("Faction", label, { label = it }, factions)
                        champNombre("Variation (ex. 10 ou -10)", negatifAutorise = true)
                    }
                    QuestRewardType.PNJ -> {
                        if (pnjs.isEmpty()) Text("Aucune fiche PNJ dans ce monde.")
                        pnjs.forEach { pnj ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { characterId = pnj.id; label = pnj.name },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = characterId == pnj.id, onClick = { characterId = pnj.id; label = pnj.name })
                                Text(pnj.name + (pnj.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""))
                            }
                        }
                    }
                    QuestRewardType.MONTURE, QuestRewardType.ANIMAL -> {
                        champTexte("Nom", label, { label = it })
                        champTexte("Espèce (ex. Cheval de selle)", detail, { detail = it })
                        if (type == QuestRewardType.MONTURE) champNombre("Vitesse (m, défaut 18)")
                    }
                    QuestRewardType.VEHICULE -> {
                        champTexte("Nom", label, { label = it })
                        champTexte("Type (ex. Chariot)", detail, { detail = it })
                    }
                    QuestRewardType.BIEN -> {
                        champTexte("Nom (ex. Taverne du Pont)", label, { label = it })
                        champTexte("Description", detail, { detail = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valide,
                onClick = {
                    onSave(
                        (initial ?: QuestReward(type = type)).copy(
                            type = type,
                            label = label.trim(),
                            amount = when (type) {
                                QuestRewardType.EQUIPEMENT -> amount.coerceAtLeast(1)
                                else -> amount
                            },
                            detail = detail.trim(),
                            characterId = characterId.takeIf { type == QuestRewardType.PNJ },
                        )
                    )
                },
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Choix du groupe récompensé, aperçu de la répartition, puis distribution. */
@Composable
fun ValidateQuestDialog(campaign: GameState.MjCampaign, quest: Quest, onDismiss: () -> Unit) {
    val groups by GameState.mjGroups.collectAsState()
    val characters by GameState.characters.collectAsState()
    val currentGroupId by GameState.currentGroupId.collectAsState()
    val worldGroups = groups.filter { it.worldId == campaign.worldId }
    var groupId by remember { mutableStateOf(currentGroupId?.takeIf { id -> worldGroups.any { it.id == id } } ?: worldGroups.firstOrNull()?.id) }
    var resultat by remember { mutableStateOf<String?>(null) }

    val done = resultat
    if (done != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Quête validée") },
            text = { Text(done) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Valider « ${quest.title} »") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (worldGroups.isEmpty()) {
                    Text("Aucun groupe dans ce monde : créez-en un dans l'outil Groupes.")
                    return@Column
                }
                Text("Groupe récompensé", style = MaterialTheme.typography.labelLarge)
                worldGroups.forEach { g ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { groupId = g.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = groupId == g.id, onClick = { groupId = g.id })
                        Text(g.name)
                    }
                }
                val group = worldGroups.firstOrNull { it.id == groupId }
                if (group != null) {
                    val rep = ValiderQueteUseCase.repartition(quest, group, characters)
                    HorizontalDivider()
                    Text("Répartition", style = MaterialTheme.typography.labelLarge)
                    if (rep.parts == 0) {
                        Text(
                            "Aucun joueur dans ce groupe : l'or ira au trésor, l'XP ne sera pas distribuée.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text("${rep.parts} joueur(s) : " + (rep.pj.map { it.name } + rep.joueursSansFiche.map { "${it.name} (sans fiche)" }).joinToString())
                        if (rep.xpTotal > 0) Text("• ${rep.xpParJoueur} XP chacun")
                        if (rep.orTotal > 0) Text("• ${rep.orParJoueur} po chacun" + (rep.orAuTresor.takeIf { it > 0 }?.let { ", $it po au trésor" } ?: ""))
                    }
                    quest.rewards.filter { it.type != QuestRewardType.XP && it.type != QuestRewardType.OR }.forEach {
                        Text("• ${it.resume}" + if (it.type == QuestRewardType.REPUTATION) "" else " → groupe")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = groupId != null,
                onClick = { groupId?.let { id -> resultat = ValiderQueteUseCase.valider(campaign.id, quest, id) ?: "Quête déjà récompensée." } },
            ) { Text("Valider et récompenser") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
