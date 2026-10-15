package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Conditions d'un personnage : texte libre séparé par des virgules (Character.condition). */
internal fun conditionsDe(character: Character): List<String> =
    character.condition.split(",").map { it.trim() }.filter { it.isNotEmpty() }

private enum class ActionMj { ETATS, EXPERIENCE, DON_OBJET, POINTS_DE_VIE }

/** Couleur de la jauge de PV : vert, orange sous la moitié, rouge sous le quart. */
internal fun couleurPv(pv: Int, pvMax: Int): androidx.compose.ui.graphics.Color {
    val ratio = if (pvMax > 0) pv.toFloat() / pvMax else 0f
    return when {
        ratio > 0.5f -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
        ratio > 0.25f -> androidx.compose.ui.graphics.Color(0xFFFFA726)
        else -> androidx.compose.ui.graphics.Color(0xFFEF5350)
    }
}

/** « 12 / 20 PV (+5) », « À terre (0 / 20 PV) ». */
internal fun textePv(c: Character): String =
    (if (c.currentHitPoints <= 0) "À terre · " else "") +
        "${c.currentHitPoints} / ${c.maxHitPoints} PV" + if (c.temporaryHitPoints > 0) " (+${c.temporaryHitPoints} temp.)" else ""

/**
 * Ajustements rapides MJ depuis le tiroir, sans passer par la fiche complète : quatre cases —
 * Inspiration (un appui la donne ou la retire), États, Expérience et Don d'objet (chacune ouvre
 * sa fenêtre). Les changements passent par GameState (source unique de vérité, persistée) : si
 * ce personnage est réclamé par un joueur connecté, NetworkSessionManager les relaie
 * automatiquement à ce client dès que GameState.characters change (voir son init).
 */
@Composable
internal fun PersonnageMjDialog(
    character: Character,
    joueur: String?,
    lieu: String?,
    onDismiss: () -> Unit,
    onOpenEtat: (String) -> Unit,
) {
    var action by remember(character.id) { mutableStateOf<ActionMj?>(null) }
    // Conditions avec la fatigue (épuisement) et son niveau.
    val conditions = conditionsDe(character) + listOfNotNull(com.jc2.jdrcompagnon.ui.screens.joueur.character.libelleEpuisement(character.exhaustionLevel))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(character.name + (joueur?.let { " ($it)" } ?: ""))
                lieu?.let { Text("📍 $it", style = MaterialTheme.typography.bodySmall) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (joueur != null) {
                    Text(
                        "Connecté : les changements sont envoyés immédiatement au joueur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForcedDarkPalette.AccentGold,
                    )
                }
                if (conditions.isNotEmpty()) {
                    Text("États : ${conditions.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                // Points de vie : la case entière, sur toute la largeur, ouvre l'ajustement.
                CaseMj(
                    titre = "❤ Points de vie",
                    detail = textePv(character),
                    actif = character.currentHitPoints * 2 <= character.maxHitPoints,
                    modifier = Modifier.fillMaxWidth(),
                ) { action = ActionMj.POINTS_DE_VIE }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CaseMj(
                        titre = "Inspiration",
                        detail = if (character.heroicInspiration) "★ Oui" else "☆ Non",
                        actif = character.heroicInspiration,
                        modifier = Modifier.weight(1f),
                    ) { GameState.setHeroicInspiration(character.id, !character.heroicInspiration) }
                    CaseMj(
                        titre = "États",
                        detail = if (conditions.isEmpty()) "Aucun" else conditions.size.toString(),
                        actif = conditions.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { action = ActionMj.ETATS }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CaseMj(
                        titre = "Expérience",
                        detail = "Niv. ${character.level} · ${character.experience} PX",
                        modifier = Modifier.weight(1f),
                    ) { action = ActionMj.EXPERIENCE }
                    CaseMj(
                        titre = "Don d'objet",
                        detail = "Rechercher",
                        modifier = Modifier.weight(1f),
                    ) { action = ActionMj.DON_OBJET }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )

    when (action) {
        ActionMj.ETATS -> EtatsPersonnageDialog(character, onOpenEtat = onOpenEtat, onDismiss = { action = null })
        ActionMj.EXPERIENCE -> ExperiencePersonnageDialog(character, onDismiss = { action = null })
        ActionMj.DON_OBJET -> DonObjetDialog(character, connecte = joueur != null, onDismiss = { action = null })
        ActionMj.POINTS_DE_VIE -> PointsDeViePersonnageDialog(character, onDismiss = { action = null })
        null -> Unit
    }
}

/**
 * Points de vie d'un personnage ajustés par le MJ : boutons rapides (−5, −1, +1, +5), dégâts ou
 * soins d'un montant saisi, retour au maximum, PV temporaires. Un combat en cours reprend ces PV
 * (CombatSession.synchroniserFiches), un joueur connecté les reçoit aussitôt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PointsDeViePersonnageDialog(character: Character, onDismiss: () -> Unit) {
    val personnages by GameState.characters.collectAsState()
    val perso = personnages.firstOrNull { it.id == character.id } ?: character
    var montant by remember(character.id) { mutableStateOf("") }
    var temporaires by remember(character.id) { mutableStateOf(perso.temporaryHitPoints.takeIf { it > 0 }?.toString().orEmpty()) }
    val n = montant.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Points de vie — ${perso.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(textePv(perso), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = couleurPv(perso.currentHitPoints, perso.maxHitPoints))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { if (perso.maxHitPoints > 0) (perso.currentHitPoints.toFloat() / perso.maxHitPoints).coerceIn(0f, 1f) else 0f },
                    color = couleurPv(perso.currentHitPoints, perso.maxHitPoints),
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-5, -1, 1, 5).forEach { d ->
                        OutlinedButton(onClick = { GameState.ajusterPv(perso.id, d) }) { Text(if (d > 0) "+$d" else "$d") }
                    }
                }
                OutlinedTextField(
                    value = montant,
                    onValueChange = { v -> montant = v.filter(Char::isDigit).take(4) },
                    label = { Text("Montant") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = n != null,
                        onClick = { GameState.ajusterPv(perso.id, -(n ?: 0)); montant = "" },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFC62828)),
                        modifier = Modifier.weight(1f),
                    ) { Text("Dégâts") }
                    Button(
                        enabled = n != null,
                        onClick = { GameState.ajusterPv(perso.id, n ?: 0); montant = "" },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f),
                    ) { Text("Soins") }
                }
                TextButton(
                    enabled = perso.currentHitPoints < perso.maxHitPoints,
                    onClick = { GameState.updateCharacterHp(perso.id, perso.maxHitPoints) },
                ) { Text("Remettre au maximum (${perso.maxHitPoints} PV)") }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = temporaires,
                        onValueChange = { v -> temporaires = v.filter(Char::isDigit).take(3) },
                        label = { Text("PV temporaires") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { GameState.definirPvTemporaires(perso.id, temporaires.toIntOrNull() ?: 0) }) { Text("Fixer") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/** Case cliquable de la fenêtre MJ d'un personnage : titre, petite valeur dessous. */
@Composable
private fun CaseMj(titre: String, detail: String, modifier: Modifier = Modifier, actif: Boolean = false, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (actif) ForcedDarkPalette.AccentGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.heightIn(min = 76.dp).clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

/** Gestion des états (catalogue Etats, le même qu'en combat et dans le livre États) : ajouter ou retirer. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EtatsPersonnageDialog(character: Character, onOpenEtat: (String) -> Unit, onDismiss: () -> Unit) {
    // Relu à chaque changement : la fiche affichée suit GameState.
    val characters by GameState.characters.collectAsState()
    val perso = characters.firstOrNull { it.id == character.id } ?: character
    var autreCondition by rememberSaveable(perso.id) { mutableStateOf("") }
    val conditions = conditionsDe(perso)
    val etats = Etats.lire(perso.condition)
    val autres = Etats.autres(perso.condition)

    fun basculerEtat(etat: ConditionCombat) {
        GameState.setCondition(perso.id, Etats.ecrire(perso.condition, if (etat in etats) etats - etat else etats + etat))
    }

    fun retirerAutre(libre: String) {
        // Retirer « Affamé » = le personnage a mangé : compteur de faim remis à zéro.
        if (libre.equals(GameState.CONDITION_FAIM, ignoreCase = true)) return GameState.nourrir(perso.id)
        GameState.setCondition(perso.id, Etats.ecrire(autres.filterNot { it == libre }.joinToString(", "), etats))
    }

    /** Un état saisi à la main (« paralysée ») est reconnu et coché ; sinon il reste une mention libre. */
    fun ajouterLibre(saisie: String) {
        val libelle = saisie.trim().takeIf { it.isNotEmpty() } ?: return
        val reconnu = Etats.reconnaitre(libelle)
        when {
            reconnu != null -> if (reconnu !in etats) basculerEtat(reconnu)
            autres.none { it.equals(libelle, ignoreCase = true) } ->
                GameState.setCondition(perso.id, Etats.ecrire((autres + libelle).joinToString(", "), etats))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("États — ${perso.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("États subis (appui = retirer)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (conditions.isEmpty() && perso.exhaustionLevel == 0) {
                    Text("Aucun état.", style = MaterialTheme.typography.bodySmall)
                } else if (conditions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        etats.sortedBy { it.ordinal }.forEach { etat ->
                            InputChip(
                                selected = true,
                                onClick = { basculerEtat(etat) },
                                label = { Text(etat.label) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Retirer ${etat.label}", modifier = Modifier.size(16.dp)) },
                            )
                        }
                        autres.forEach { libre ->
                            InputChip(
                                selected = true,
                                onClick = { retirerAutre(libre) },
                                label = { Text(libre) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Retirer $libre", modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }
                    // Ce que les états imposent (en combat : actions bloquées, vitesse 0…) et leur règle.
                    com.jc2.jdrcompagnon.feature_combat.ui.ResumeEffetsEtats(etats)
                    if (etats.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            etats.sortedBy { it.ordinal }.forEach { etat ->
                                TextButton(onClick = { onDismiss(); onOpenEtat(etat.label) }) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Règle : ${etat.label}")
                                }
                            }
                        }
                    }
                    TextButton(onClick = { GameState.setCondition(perso.id, "") }) { Text("Tout retirer") }
                }

                // Fatigue (épuisement, 0 à 6) : un état à niveaux, réglé directement.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fatigue (épuisement) : ${perso.exhaustionLevel} / 6", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(com.jc2.jdrcompagnon.ui.screens.joueur.character.effetEpuisement(perso.exhaustionLevel), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { GameState.setExhaustionLevel(perso.id, perso.exhaustionLevel - 1) }, enabled = perso.exhaustionLevel > 0) { Text("−") }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(onClick = { GameState.setExhaustionLevel(perso.id, perso.exhaustionLevel + 1) }, enabled = perso.exhaustionLevel < 6) { Text("+") }
                }

                HorizontalDivider()
                Text("Ajouter un état", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etats.choisissables.filter { it !in etats }.forEach { etat ->
                        AssistChip(
                            onClick = { basculerEtat(etat) },
                            label = { Text(etat.label) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                    // Faim : le dernier repas est reculé pour que le calcul au repos garde la condition.
                    if (!GameState.estAffame(perso)) {
                        AssistChip(
                            onClick = { GameState.rendreAffame(perso.id) },
                            label = { Text(GameState.CONDITION_FAIM) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = autreCondition,
                        onValueChange = { autreCondition = it.replace(",", "") },
                        label = { Text("Autre état (texte libre)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    TextButton(
                        onClick = {
                            ajouterLibre(autreCondition)
                            autreCondition = ""
                        },
                        enabled = autreCondition.isNotBlank(),
                    ) { Text("Ajouter") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/**
 * Expérience : « +1 niveau » donne les PX qui manquent pour atteindre le niveau suivant (le joueur
 * applique ensuite la montée de niveau sur sa fiche), ou un montant de PX à ajouter / retirer.
 */
@Composable
private fun ExperiencePersonnageDialog(character: Character, onDismiss: () -> Unit) {
    val characters by GameState.characters.collectAsState()
    val perso = characters.firstOrNull { it.id == character.id } ?: character
    var montant by rememberSaveable(perso.id) { mutableStateOf("") }
    // Niveau atteint par les PX actuels (peut dépasser le niveau de la fiche si la montée n'est pas encore faite).
    val niveauPx = com.jc2.jdrcompagnon.ui.CharacterProgression.levelForXp(perso.experience)
    val niveauVise = (maxOf(perso.level, niveauPx) + 1).coerceAtMost(20)
    val manquant = (com.jc2.jdrcompagnon.ui.CharacterProgression.xpForLevel(niveauVise) - perso.experience).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Expérience — ${perso.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${perso.experience} PX · niveau ${perso.level}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (niveauPx > perso.level) {
                    Text("Montée au niveau $niveauPx en attente sur la fiche.", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
                }
                Button(
                    onClick = { GameState.addExperience(perso.id, manquant) },
                    enabled = perso.level < 20 && manquant > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+1 niveau (niveau $niveauVise : +$manquant PX)") }
                HorizontalDivider()
                OutlinedTextField(
                    value = montant,
                    onValueChange = { montant = it.filter { c -> c.isDigit() } },
                    label = { Text("Montant de PX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { montant.toIntOrNull()?.let { GameState.addExperience(perso.id, it) }; montant = "" },
                        enabled = montant.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Ajouter") }
                    OutlinedButton(
                        onClick = { montant.toIntOrNull()?.let { GameState.addExperience(perso.id, -it) }; montant = "" },
                        enabled = montant.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("Retirer") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )
}

/**
 * Don d'objet : recherche dans l'équipement de la bibliothèque du monde (armes et armures
 * magiques comprises) ; un objet introuvable peut être donné sous le nom saisi. Personnage
 * connecté : l'objet lui est livré par le réseau ; sinon, il entre directement dans son sac.
 */
@Composable
private fun DonObjetDialog(character: Character, connecte: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val monde = character.worldId.ifBlank { GameState.currentWorld.value?.id ?: "donjon_et_dragon" }
    val catalogue by produceState(initialValue = emptyList<Pair<String, String>>(), monde) {
        value = runCatching {
            (SrdRepository.loadEquipmentList(context, monde).map { it.name to it.category } +
                SrdRepository.loadArmesArmuresMagiques(context, monde).map { it.name to it.category })
                .distinctBy { it.first.lowercase() }
                .sortedBy { it.first.lowercase() }
        }.getOrDefault(emptyList())
    }
    var recherche by rememberSaveable(character.id) { mutableStateOf("") }
    var aConfirmer by remember { mutableStateOf<String?>(null) }
    var dernierDon by remember { mutableStateOf<String?>(null) }
    val claimed by NetworkSessionManager.claimedCharacters.collectAsState()
    val resultats = remember(catalogue, recherche) {
        val q = recherche.trim()
        if (q.isEmpty()) catalogue.take(50) else catalogue.filter { it.first.contains(q, ignoreCase = true) }.take(80)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Donner un objet à ${character.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher un objet") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                dernierDon?.let { Text("✔ « $it » donné.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                if (recherche.isNotBlank() && resultats.none { it.first.equals(recherche.trim(), ignoreCase = true) }) {
                    TextButton(onClick = { aConfirmer = recherche.trim() }) { Text("Donner « ${recherche.trim()} » (nom libre)") }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(resultats, key = { it.first }) { (nom, categorie) ->
                        ListItem(
                            headlineContent = { Text(nom) },
                            supportingContent = categorie.takeIf { it.isNotBlank() }?.let { @Composable { Text(it) } },
                            modifier = Modifier.clickable { aConfirmer = nom },
                        )
                    }
                }
                if (catalogue.isEmpty()) {
                    Text("Bibliothèque d'équipement indisponible : saisissez le nom de l'objet.", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )

    aConfirmer?.let { libelle ->
        AlertDialog(
            onDismissRequest = { aConfirmer = null },
            title = { Text("Donner l'objet ?") },
            text = { Text("« $libelle » à ${character.name}" + if (connecte) " (envoyé au joueur connecté)." else " (ajouté à son sac).") },
            confirmButton = {
                TextButton(onClick = {
                    val clientId = claimed[character.id]
                    // « 3x Potion de soins » : trois objets réels (voir objetsDuLibelle).
                    objetsDuLibelle(libelle).forEach { objet ->
                        if (clientId != null) NetworkSessionManager.sendItemToClient(clientId, objet)
                        else GameState.addItemToBackpack(character.id, objet)
                    }
                    dernierDon = libelle
                    aConfirmer = null
                }) { Text("Donner") }
            },
            dismissButton = { TextButton(onClick = { aConfirmer = null }) { Text("Annuler") } },
        )
    }
}
