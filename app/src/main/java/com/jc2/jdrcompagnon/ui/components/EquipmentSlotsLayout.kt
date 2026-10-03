@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.stackCount
import com.jc2.jdrcompagnon.ui.BACK_WEAPON_SLOTS
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Harmonisation
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Focaliseurs
import com.jc2.jdrcompagnon.ui.screens.joueur.slotColor
import com.jc2.jdrcompagnon.ui.screens.joueur.slotIcon
import com.jc2.jdrcompagnon.ui.screens.joueur.itemIcon
import com.jc2.jdrcompagnon.ui.screens.joueur.slotLabel
import com.jc2.jdrcompagnon.ui.screens.joueur.equipmentWeightLabel
import com.jc2.jdrcompagnon.ui.screens.joueur.equipmentHandsRequired
import com.jc2.jdrcompagnon.ui.screens.joueur.HandsBadge

private typealias EquipmentItemSRD = com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem

/**
 * Gestion complète de l'équipement d'un personnage, embarquée directement
 * dans l'onglet "Équipement" de la fiche (plus d'écran séparé) :
 *
 * - Sac à dos en haut avec objets sélectionnables.
 * - Silhouette humaine stylisée avec emplacements.
 * - Sélection au clic + placement au clic (PAS de glisser-déposer : ne fonctionne pas
 *   fiablement sur les grandes listes ou petits écrans) — on touche un objet pour le
 *   sélectionner, puis un emplacement pour l'y placer ; la barre de sélection propose
 *   aussi de le ranger dans le sac ou de le jeter.
 * - Double-clic sur un objet → détails.
 * - Le sac à dos équipé propose 3 emplacements extérieurs (sac de couchage, corde...),
 *   affichés juste à côté de sa case, uniquement quand on clique dessus.
 * - Règles : 1 armure max (TORSO), bouclier = OFF_HAND, 2 mains = MAIN+OFF.
 */
@Composable
fun EquipmentManagementContent(character: Character, isMjMode: Boolean = false, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allCharacters by GameState.characters.collectAsState()
    val liveCharacter = allCharacters.find { it.id == character.id } ?: character
    var currentCharacter by remember { mutableStateOf(liveCharacter) }
    var showLibraryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(liveCharacter) {
        currentCharacter = liveCharacter
    }

    // ArmorRules.weightInPounds lit un cache d'équipement rempli par
    // SrdRepository.loadEquipmentList — sans ce préchargement, le cache reste vide tant
    // qu'aucun picker SRD n'a été ouvert cette session et tous les poids valent 0.
    // equipmentLoaded ne sert qu'à déclencher une recomposition une fois le cache prêt.
    var equipmentLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(currentCharacter.worldId) {
        val bibliotheque = SrdRepository.loadEquipmentList(context, currentCharacter.worldId.ifBlank { "donjon_et_dragon" })
        // Noms au pluriel de l'équipement de départ (« Dagues ») ramenés au nom de la
        // bibliothèque, sans quoi l'arme n'est pas reconnue en combat.
        GameState.normaliserNomsObjets(currentCharacter.id, bibliotheque.map { it.name })
        equipmentLoaded = true
    }

    var selectedItemDetail by remember { mutableStateOf<String?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<String?>(null) }
    var backpackPanelExpanded by remember { mutableStateOf(false) }
    var quiverPanelExpanded by remember { mutableStateOf(false) }
    // Grimoire (Magicien) : s'ouvre comme le sac à dos, en touchant sa puce ou son emplacement.
    var grimoirePanelExpanded by remember { mutableStateOf(false) }

    // Sélection courante pour le placement au clic (remplace le glisser-déposer) : on
    // sélectionne un objet — dans le sac général (selectedFromSlot == null) ou déjà équipé
    // (selectedFromSlot == son emplacement) —, puis on clique sur un emplacement pour l'y
    // placer, sur "Ranger dans le sac" pour le déséquiper, ou sur "Jeter" pour le supprimer.
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var selectedFromSlot by remember { mutableStateOf<EquipmentSlot?>(null) }
    // Second toucher sur un objet du sac déjà sélectionné : TOUS ses exemplaires sont
    // sélectionnés (déplacement groupé vers une arme de dos empilable ou le carquois).
    var selectAll by remember { mutableStateOf(false) }

    fun clearSelection() {
        selectedItem = null
        selectedFromSlot = null
        selectAll = false
    }

    fun refresh() {
        currentCharacter = GameState.characters.value.find { it.id == currentCharacter.id } ?: currentCharacter
    }

    fun selectFromBackpack(item: String) {
        val dejaSelectionne = selectedItem == item && selectedFromSlot == null
        val exemplaires = currentCharacter.backpackItems.count { it == item }
        when {
            // 1er toucher : un exemplaire ; 2e : tous (s'il y en a plusieurs) ; 3e : désélection.
            dejaSelectionne && !selectAll && exemplaires > 1 -> selectAll = true
            dejaSelectionne -> clearSelection()
            else -> {
                selectedItem = item
                selectedFromSlot = null
                selectAll = false
            }
        }
    }

    /** Nombre d'exemplaires concernés par la sélection courante (depuis le sac). */
    fun selectedCount(): Int {
        val item = selectedItem ?: return 0
        return if (selectAll && selectedFromSlot == null) currentCharacter.backpackItems.count { it == item } else 1
    }

    fun selectFromSlot(slot: EquipmentSlot, item: String) {
        if (selectedItem == item && selectedFromSlot == slot) {
            clearSelection()
        } else {
            selectedItem = item
            selectedFromSlot = slot
        }
    }

    fun placeOnSlot(target: EquipmentSlot) {
        val item = selectedItem ?: return
        if (!ArmorRules.itemAllowedInSlot(item, target)) return
        val originSlot = selectedFromSlot
        // Une pile (javelines...) se déplace entière.
        val originCount = originSlot?.let { currentCharacter.stackCount(it) } ?: 1
        if (originSlot != null && originSlot != target) {
            // Déjà équipé ailleurs : on le range d'abord (il repasse dans le sac), pour
            // que equipInSlot (qui exige l'objet dans le sac) puisse ensuite l'y remettre.
            GameState.unequipFromSlot(currentCharacter.id, item, originSlot)
        }
        val accepted = GameState.equipInSlot(currentCharacter.id, item, target)
        // Objet à harmonisation équipé sans harmonisation : porté, mais magie inactive.
        if (accepted && originSlot == null) {
            SrdRepository.findEquipmentItemCached(item)
                ?.takeIf { it.name.equals(item, ignoreCase = true) && !Harmonisation.estActif(currentCharacter, it) }
                ?.let {
                    android.widget.Toast.makeText(
                        context,
                        "$item est équipé, mais ${Harmonisation.libelle(it).lowercase()} : ses propriétés magiques " +
                            "restent inactives. Harmonisez-vous pendant un repos court (écran Repos).",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
        }
        if (accepted && originSlot == null && target in BACK_WEAPON_SLOTS && ArmorRules.isStackableWeapon(item)) {
            // Sélection groupée depuis le sac : tous les exemplaires rejoignent la pile.
            repeat(selectedCount() - 1) { GameState.equipInSlot(currentCharacter.id, item, target) }
        }
        if (originSlot != null && originSlot != target) {
            // Refusé par le nouvel emplacement (mauvais type) : on rééquipe là où il était.
            // Accepté : le reste de la pile suit, s'il peut s'empiler sur la cible.
            val destination = if (accepted) target else originSlot
            if (!accepted) GameState.equipInSlot(currentCharacter.id, item, originSlot)
            if (destination in BACK_WEAPON_SLOTS && ArmorRules.isStackableWeapon(item)) {
                repeat(originCount - 1) { GameState.equipInSlot(currentCharacter.id, item, destination) }
            }
        }
        refresh()
        clearSelection()
    }

    fun returnSelectedToBackpack() {
        val item = selectedItem ?: return
        if (selectedFromSlot != null) {
            GameState.unequipFromSlot(currentCharacter.id, item, selectedFromSlot)
            refresh()
        }
        clearSelection()
    }

    fun onSlotTapped(slot: EquipmentSlot) {
        val occupant = currentCharacter.equippedSlots[slot]
        val quiverOnBack = slot == EquipmentSlot.BACK && occupant != null && ArmorRules.isQuiverItem(occupant)
        val selection = selectedItem
        when {
            // Flèches du sac touchées puis carquois : on les range dedans (autant que la
            // place le permet) au lieu de remplacer le carquois.
            quiverOnBack && selection != null && selectedFromSlot == null && ArmorRules.isArrowItem(selection) -> {
                GameState.putArrowsInQuiver(currentCharacter.id, selection, selectedCount())
                refresh()
                clearSelection()
                quiverPanelExpanded = true
            }
            // Objet équipé sélectionné puis case du sac à dos : il est rangé dedans (et le sac
            // s'ouvre pour le montrer), au lieu de tenter de l'équiper comme sac à dos.
            slot == EquipmentSlot.BACKPACK && selection != null && selectedFromSlot != null && !ArmorRules.isBackpackItem(selection) -> {
                returnSelectedToBackpack()
                backpackPanelExpanded = true
            }
            selection != null -> placeOnSlot(slot)
            // Le carquois s'ouvre comme le sac à dos (contenu + flèches à ranger).
            quiverOnBack -> quiverPanelExpanded = !quiverPanelExpanded
            occupant != null && ArmorRules.isGrimoireItem(occupant) -> grimoirePanelExpanded = !grimoirePanelExpanded
            // Le sac à dos ouvre/ferme son panneau (objets du sac + emplacements
            // extérieurs) qu'il soit déjà équipé ou non : sans backpack équipé, c'est
            // là qu'on voit et sélectionne les objets du sac général pour en équiper un.
            slot == EquipmentSlot.BACKPACK -> backpackPanelExpanded = !backpackPanelExpanded
            occupant != null -> selectFromSlot(slot, occupant)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // key() force le recalcul du poids une fois le cache SRD chargé : ces cartes sont
        // sinon "skippées" par Compose (mêmes currentCharacter/isMjMode en entrée) et
        // resteraient figées sur les poids à 0 lus avant que le cache soit prêt.
        key(equipmentLoaded) {
            // Infos charge / poids
            WeightSummaryCard(currentCharacter, isMjMode)

            // La bourse d'or, séparée de la charge : c'est un objet à part.
            PurseCard(currentCharacter, isMjMode)
        }

        // Toujours présente et de hauteur fixe : avant, la barre n'apparaissait qu'à la
        // sélection et poussait toute la silhouette vers le bas (l'écran "sautait").
        SelectionBar(
            itemName = selectedItem?.let { item ->
                val dispo = currentCharacter.backpackItems.count { it == item }
                when {
                    selectedFromSlot != null -> item
                    selectAll -> "$item ×$dispo (tous)"
                    dispo > 1 -> "$item (1/$dispo, retouchez pour tous)"
                    else -> item
                }
            },
            canReturnToBackpack = selectedFromSlot != null,
            onReturnToBackpack = { returnSelectedToBackpack() },
            onDelete = { pendingDeleteItem = selectedItem },
            onCancel = { clearSelection() }
        )

        // Silhouette avec emplacements — le sac à dos (objets non équipés + emplacements
        // extérieurs) n'apparaît que si on clique sur sa case (cf. onSlotTapped),
        // key(equipmentLoaded) force le recalcul du poids une fois le cache SRD chargé.
        key(equipmentLoaded) {
            SilhouetteSlots(
                character = currentCharacter,
                selectedItem = selectedItem,
                selectedFromSlot = selectedFromSlot,
                hasSelection = selectedItem != null,
                backpackPanelExpanded = backpackPanelExpanded,
                quiverPanelExpanded = quiverPanelExpanded,
                grimoirePanelExpanded = grimoirePanelExpanded,
                onSlotClick = { onSlotTapped(it) },
                onItemDoubleClick = { selectedItemDetail = it },
                // Le grimoire s'ouvre au toucher (comme le sac à dos) ; appui long pour le sélectionner.
                onBackpackItemClick = { item ->
                    if (ArmorRules.isGrimoireItem(item) && selectedItem == null) grimoirePanelExpanded = !grimoirePanelExpanded
                    else selectFromBackpack(item)
                },
                onBackpackItemLongClick = { selectFromBackpack(it) },
                onSlotLongClick = { slot -> currentCharacter.equippedSlots[slot]?.let { selectFromSlot(slot, it) } },
                onEquippedQuiverClick = { selectFromSlot(EquipmentSlot.BACK, it) },
                onPutArrows = { itemName, count ->
                    GameState.putArrowsInQuiver(currentCharacter.id, itemName, count)
                    refresh()
                },
                onTakeArrows = { itemName, count ->
                    GameState.takeArrowsFromQuiver(currentCharacter.id, itemName, count)
                    refresh()
                },
                onExteriorTransfer = { itemName, index ->
                    GameState.equipBackpackExteriorSlot(currentCharacter.id, itemName, index)
                    refresh()
                },
                onExteriorRemove = { index ->
                    GameState.unequipBackpackExteriorSlot(currentCharacter.id, index)
                    refresh()
                },
                // Ajout d'objet depuis la bibliothèque SRD (MJ) : simple « + » en haut à droite.
                onAddItem = if (isMjMode) ({ showLibraryPicker = true }) else null,
                onReturnSelectedToBackpack = { returnSelectedToBackpack() }
            )
        }
    }

    if (showLibraryPicker) {
        SrdLibraryPickerDialog(
            title = "Choisir un objet",
            onDismiss = { showLibraryPicker = false },
            onSelect = { itemName -> GameState.addItemToBackpack(currentCharacter.id, itemName) },
            search = { query ->
                val items = SrdRepository.loadEquipmentList(context, currentCharacter.worldId.ifBlank { "donjon_et_dragon" })
                val filtered = if (query.isBlank()) items else items.filter { it.name.contains(query, ignoreCase = true) }
                filtered.map { SrdPickerEntry(name = it.name) }
            }
        )
    }

    selectedItemDetail?.let { item ->
        EquipmentItemDetailDialog(
            itemName = item,
            character = currentCharacter,
            onDismiss = { selectedItemDetail = null }
        )
    }

    pendingDeleteItem?.let { item ->
        val nombre = selectedCount()
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (nombre > 1) "Jeter ces objets ?" else "Jeter cet objet ?") },
            text = {
                Text(
                    if (nombre > 1) "Les $nombre « $item » seront définitivement retirés de l'inventaire."
                    else "« $item » sera définitivement retiré de l'inventaire."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (nombre > 1) GameState.removeFromBackpack(currentCharacter.id, item, nombre)
                    else GameState.removeItemCompletely(currentCharacter.id, item, selectedFromSlot)
                    refresh()
                    clearSelection()
                    pendingDeleteItem = null
                }) { Text("Jeter", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) { Text("Annuler") }
            }
        )
    }
}

/**
 * Barre affichée quand un objet est sélectionné (clic sur une puce du sac ou sur un
 * emplacement équipé) : rappelle l'objet en cours de placement et propose les actions qui
 * n'ont pas de cible dédiée sur laquelle cliquer (le ranger, le jeter, annuler).
 */
@Composable
private fun SelectionBar(
    itemName: String?,
    canReturnToBackpack: Boolean,
    onReturnToBackpack: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val hasSelection = itemName != null
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (hasSelection) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, if (hasSelection) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (hasSelection) "Sélectionné : $itemName" else "Touchez un objet pour le sélectionner",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasSelection) FontWeight.Bold else FontWeight.Normal,
                color = if (hasSelection) SheetTextPrimary else SheetTextPrimary.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (hasSelection) {
                if (canReturnToBackpack) {
                    IconButton(onClick = onReturnToBackpack) {
                        Icon(Icons.Default.Backpack, contentDescription = "Ranger dans le sac", tint = SheetTextPrimary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Jeter", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Annuler la sélection", tint = SheetTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun WeightSummaryCard(character: Character, isMjMode: Boolean) {
    val totalWeight = GameState.totalEquipmentWeight(character)
    val maxCarry = GameState.maxCarryWeight(character)
    val isOverloaded = totalWeight > maxCarry
    var showDetail by remember { mutableStateOf(false) }

    if (showDetail) {
        ChargeDetailDialog(character = character, totalWeight = totalWeight, maxCarry = maxCarry, isOverloaded = isOverloaded, onDismiss = { showDetail = false })
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetail = true },
        shape = RoundedCornerShape(16.dp),
        color = if (isOverloaded) MaterialTheme.colorScheme.error.copy(alpha = 0.15f) else SheetSurface,
        border = BorderStroke(1.dp, if (isOverloaded) MaterialTheme.colorScheme.error else SheetBorder)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Charge", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                    Text(
                        text = "${String.format(java.util.Locale.FRANCE, "%.1f", totalWeight)} / ${String.format(java.util.Locale.FRANCE, "%.1f", maxCarry)} kg",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverloaded) MaterialTheme.colorScheme.error else SheetTextPrimary
                    )
                }
                LinearProgressIndicator(
                    progress = { (totalWeight / maxCarry.coerceAtLeast(0.1)).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOverloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = SheetBorder
                )
                if (isOverloaded) {
                    Text("Surcharge ! Force × 7,5 dépassé.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Détail du calcul de charge, sur le même principe que EquipmentItemDetailDialog. */
@Composable
private fun ChargeDetailDialog(character: Character, totalWeight: Double, maxCarry: Double, isOverloaded: Boolean, onDismiss: () -> Unit) {
    val objetsPeses = (character.backpackItems + character.equippedItems + character.backpackExteriorSlots.values + character.quiverContents + character.grimoireContents)
        .mapNotNull { nom -> equipmentWeightLabel(nom)?.let { nom to it } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Détail de la charge") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Charge max = ${character.strength} (FOR) × 7,5 = ${String.format(java.util.Locale.FRANCE, "%.1f", maxCarry)} kg",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Poids total porté : ${String.format(java.util.Locale.FRANCE, "%.1f", totalWeight)} kg",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isOverloaded) {
                    Text("Surcharge ! Force × 7,5 dépassé.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (objetsPeses.isNotEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp)
                    Text("Composition :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        objetsPeses.forEach { (nom, poids) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(nom, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(poids, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

/**
 * La bourse d'or, séparée de la carte "Charge" : c'est un objet à part (comme le sac à
 * dos), cliquable pour voir/gérer son contenu (le montant d'or) — sans rapport avec les
 * emplacements de sacoche (Bourse 1/2) de la silhouette, qui portent un objet physique.
 */
@Composable
private fun PurseCard(character: Character, isMjMode: Boolean) {
    var showDetail by remember { mutableStateOf(false) }
    val purseWeight = GameState.purseWeight(character)
    val purseCapacity = GameState.PURSE_CAPACITY_KG
    val purseOverloaded = purseWeight > purseCapacity

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { showDetail = true },
        shape = RoundedCornerShape(16.dp),
        color = SheetSurface,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFD4AF37))
                    Text("Bourse d'or", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                }
                Text(
                    text = "${character.gold} po",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (purseWeight / purseCapacity).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (purseOverloaded) MaterialTheme.colorScheme.error else Color(0xFFD4AF37),
                trackColor = SheetBorder
            )
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            icon = { Icon(Icons.Default.Payments, contentDescription = null, tint = Color(0xFFD4AF37)) },
            title = { Text("Bourse d'or") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${character.gold} po",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4AF37)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Charge", style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary)
                        Text(
                            text = "${String.format(java.util.Locale.FRANCE, "%.2f", purseWeight)} / ${String.format(java.util.Locale.FRANCE, "%.2f", purseCapacity)} kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (purseOverloaded) MaterialTheme.colorScheme.error else SheetTextSecondary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (purseWeight / purseCapacity).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = if (purseOverloaded) MaterialTheme.colorScheme.error else Color(0xFFD4AF37),
                        trackColor = SheetBorder
                    )
                    if (purseOverloaded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("La bourse déborde !", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Équivalence en pièces", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        listOf(
                            "Pièces de platine (pp)" to character.gold / 10,
                            "Pièces d'or (po)" to character.gold,
                            "Pièces d'argent (pa)" to character.gold * 10,
                            "Pièces de cuivre (pc)" to character.gold * 100
                        ).forEach { (label, quantite) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                                Text(quantite.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                            }
                        }
                    }
                    if (isMjMode) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { GameState.addGold(character.id, -10) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Retirer 10 po")
                            }
                            Text("10 po", style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { GameState.addGold(character.id, 10) }) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter 10 po")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetail = false }) { Text("Fermer") }
            }
        )
    }
}

@Composable
private fun SilhouetteSlots(
    character: Character,
    selectedItem: String?,
    selectedFromSlot: EquipmentSlot?,
    hasSelection: Boolean,
    backpackPanelExpanded: Boolean,
    quiverPanelExpanded: Boolean,
    grimoirePanelExpanded: Boolean,
    onSlotClick: (EquipmentSlot) -> Unit,
    onItemDoubleClick: (String) -> Unit,
    onBackpackItemClick: (String) -> Unit,
    onBackpackItemLongClick: (String) -> Unit,
    // Appui long sur un emplacement occupé : sélectionne l'objet porté (sac à dos, carquois…),
    // dont le simple toucher ouvre le contenu plutôt que de le sélectionner.
    onSlotLongClick: (EquipmentSlot) -> Unit,
    onEquippedQuiverClick: (String) -> Unit,
    onPutArrows: (itemName: String, count: Int) -> Unit,
    onTakeArrows: (itemName: String, count: Int) -> Unit,
    onExteriorTransfer: (itemName: String, index: Int) -> Unit,
    onExteriorRemove: (index: Int) -> Unit,
    onAddItem: (() -> Unit)? = null,
    onReturnSelectedToBackpack: () -> Unit = {}
) {
    @Composable
    fun slot(slot: EquipmentSlot, label: String, modifier: Modifier = Modifier) {
        SlotBox(
            slot = slot,
            item = character.equippedSlots[slot],
            label = label,
            modifier = modifier,
            isSelected = selectedFromSlot == slot,
            // Seuls les emplacements qui acceptent l'objet sélectionné sont mis en évidence.
            isPlaceable = hasSelection && selectedItem != null && ArmorRules.itemAllowedInSlot(selectedItem, slot),
            onClick = { onSlotClick(slot) },
            onLongClick = { onSlotLongClick(slot) },
            onDoubleClick = onItemDoubleClick,
            mention = character.equippedSlots[slot]?.let { Focaliseurs.type(character, it) }?.let { Focaliseurs.libelle(it) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(24.dp))
            .background(SheetSurface)
            .border(1.dp, SheetBorder, RoundedCornerShape(24.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // TORSO (large)
            slot(EquipmentSlot.TORSO, "Torse", Modifier.width(180.dp))

            // MAIN_HAND + OFF_HAND
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                slot(EquipmentSlot.MAIN_HAND, "Main princ.")
                slot(EquipmentSlot.OFF_HAND, "Main sec.")
            }

            // CLOTHING + BACKPACK
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                slot(EquipmentSlot.CLOTHING, "Vêtements")
                slot(EquipmentSlot.BACKPACK, "Sac à dos")
            }

            // Le sac à dos (objets non équipés + emplacements extérieurs) n'apparaît que
            // si on clique sur sa case ci-dessus (cf. onSlotClick, EquipmentManagementContent)
            // — qu'un sac à dos soit déjà équipé ou non : sans ça, aucun moyen de
            // sélectionner un objet du sac général pour en équiper un la première fois.
            if (backpackPanelExpanded) {
                BackpackPanel(
                    character = character,
                    selectedItem = selectedItem,
                    selectedFromSlot = selectedFromSlot,
                    onItemClick = onBackpackItemClick,
                    onItemLongClick = onBackpackItemLongClick,
                    grimoireOuvert = grimoirePanelExpanded,
                    onItemDoubleClick = onItemDoubleClick,
                    onTransfer = onExteriorTransfer,
                    onRemove = onExteriorRemove,
                    objetARanger = selectedItem?.takeIf { selectedFromSlot != null },
                    onRanger = onReturnSelectedToBackpack
                )
                // Grimoire rangé dans le sac : son contenu s'ouvre juste sous le sac.
                character.backpackItems.firstOrNull { ArmorRules.isGrimoireItem(it) }
                    ?.takeIf { grimoirePanelExpanded }
                    ?.let { GrimoirePanel(character, it, onItemDoubleClick) }
            }

            // BACK + ACCESSORY
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                slot(EquipmentSlot.BACK, "Dos")
                slot(EquipmentSlot.ACCESSORY, "Accessoire")
            }

            // Carquois porté dans le dos : s'ouvre comme le sac à dos (cf. onSlotTapped).
            val quiverName = character.equippedSlots[EquipmentSlot.BACK]?.takeIf { ArmorRules.isQuiverItem(it) }
            if (quiverName != null && quiverPanelExpanded) {
                QuiverPanel(
                    character = character,
                    quiverName = quiverName,
                    isQuiverSelected = selectedItem == quiverName && selectedFromSlot == EquipmentSlot.BACK,
                    onQuiverClick = { onEquippedQuiverClick(quiverName) },
                    onItemDoubleClick = onItemDoubleClick,
                    onPut = onPutArrows,
                    onTake = onTakeArrows
                )
            }

            // Emplacements utilitaires (ex-bourses de ceinture + accessoires de dos) : un
            // seul grand cadre vert, cases en icônes seules (nom via la barre de sélection
            // ou le double-clic).
            SlotGroup(
                title = "Utilitaire",
                color = EquipmentSlot.BELT_POUCH_1.slotColor(),
                slots = listOf(
                    EquipmentSlot.BELT_POUCH_1, EquipmentSlot.BELT_POUCH_2,
                    EquipmentSlot.BACK_ACCESSORY_1, EquipmentSlot.BACK_ACCESSORY_2,
                    EquipmentSlot.BACK_ACCESSORY_3, EquipmentSlot.BACK_ACCESSORY_4
                ),
                character = character,
                selectedItem = selectedItem,
                selectedFromSlot = selectedFromSlot,
                onSlotClick = onSlotClick,
                onSlotLongClick = onSlotLongClick,
                onItemDoubleClick = onItemDoubleClick
            )
            // Grimoire porté sur un emplacement utilitaire : son contenu s'ouvre sous le cadre.
            character.equippedSlots.values.firstOrNull { ArmorRules.isGrimoireItem(it) }
                ?.takeIf { grimoirePanelExpanded && character.backpackItems.none { b -> ArmorRules.isGrimoireItem(b) } }
                ?.let { GrimoirePanel(character, it, onItemDoubleClick) }

            // Armes portées dans le dos (3), même présentation ; les armes de jet
            // (javelines, fléchettes) s'y empilent (badge ×N).
            SlotGroup(
                title = "Armes de dos",
                color = EquipmentSlot.BACK_WEAPON_1.slotColor(),
                slots = listOf(EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3),
                character = character,
                selectedItem = selectedItem,
                selectedFromSlot = selectedFromSlot,
                onSlotClick = onSlotClick,
                onSlotLongClick = onSlotLongClick,
                onItemDoubleClick = onItemDoubleClick
            )
        }

        if (onAddItem != null) {
            IconButton(
                onClick = onAddItem,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un objet depuis la bibliothèque", tint = SheetTextPrimary)
            }
        }
    }
}

/**
 * Contenu du carquois équipé (EquipmentSlot.BACK), sur le modèle de BackpackPanel : le
 * carquois lui-même (sélectionnable pour le ranger/jeter), ses flèches (limite
 * ArmorRules.QUIVER_CAPACITY) et les flèches du sac général qu'on peut y ranger.
 */
@Composable
private fun QuiverPanel(
    character: Character,
    quiverName: String,
    isQuiverSelected: Boolean,
    onQuiverClick: () -> Unit,
    onItemDoubleClick: (String) -> Unit,
    onPut: (itemName: String, count: Int) -> Unit,
    onTake: (itemName: String, count: Int) -> Unit
) {
    val capacity = ArmorRules.QUIVER_CAPACITY
    val count = character.quiverContents.size
    val full = count >= capacity
    val contenu = character.quiverContents.groupingBy { it }.eachCount()
    val aRanger = character.backpackItems.filter { ArmorRules.isArrowItem(it) }.groupingBy { it }.eachCount()
    val color = EquipmentSlot.BACK.slotColor()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SheetSurfaceLight,
        border = BorderStroke(1.dp, color)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(itemIcon(quiverName), null, tint = color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Carquois", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                }
                Text(
                    text = "$count / $capacity flèches",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (full) MaterialTheme.colorScheme.error else SheetTextSecondary
                )
            }
            LinearProgressIndicator(
                progress = { (count.toFloat() / capacity).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = SheetBorder
            )

            HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
            Text("Carquois équipé :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
            SelectableItemChip(
                item = quiverName,
                isEquipped = true,
                isSelected = isQuiverSelected,
                onClick = onQuiverClick,
                onDoubleClick = { onItemDoubleClick(quiverName) }
            )

            HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
            Text("Dans le carquois :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
            if (contenu.isEmpty()) {
                Text("Le carquois est vide.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
            }
            contenu.forEach { (nom, n) ->
                ArrowStackRow(
                    itemName = nom,
                    count = n,
                    onDoubleClick = { onItemDoubleClick(nom) },
                    actions = {
                        TextButton(onClick = { onTake(nom, 1) }) { Text("Sortir 1") }
                        TextButton(onClick = { onTake(nom, n) }) { Text("Tout") }
                    }
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
            Text("Flèches dans le sac :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
            if (aRanger.isEmpty()) {
                Text("Aucune flèche à ranger.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
            }
            aRanger.forEach { (nom, n) ->
                ArrowStackRow(
                    itemName = nom,
                    count = n,
                    onDoubleClick = { onItemDoubleClick(nom) },
                    actions = {
                        TextButton(onClick = { onPut(nom, 1) }, enabled = !full) { Text("Ranger 1") }
                        TextButton(onClick = { onPut(nom, n) }, enabled = !full) { Text("Remplir") }
                    }
                )
            }
            if (full && aRanger.isNotEmpty()) {
                Text("Le carquois est plein.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ArrowStackRow(
    itemName: String,
    count: Int,
    onDoubleClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SheetSurface)
            .combinedClickable(onClick = {}, onDoubleClick = onDoubleClick)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(itemIcon(itemName), null, tint = SheetTextPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$itemName ×$count",
            style = MaterialTheme.typography.bodyMedium,
            color = SheetTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

/**
 * Panneau du sac à dos, affiché juste à côté de sa case (cf. SilhouetteSlots) uniquement
 * quand on clique dessus (EquipmentManagementContent) — qu'un sac à dos soit déjà équipé ou
 * non. Regroupe ce qui était avant deux endroits séparés :
 * - le sac à dos lui-même (s'il est équipé, sélectionnable comme n'importe quel objet
 *   équipé pour le ranger/jeter via la barre de sélection) et ses 3 emplacements
 *   EXTÉRIEURS (sac de couchage, corde...), avec transfert en un clic depuis le sac général ;
 * - le sac général (objets non équipés), pour qu'on puisse en sélectionner un et
 *   l'équiper (y compris le tout premier sac à dos, avant qu'aucun ne soit équipé).
 */
@Composable
private fun BackpackPanel(
    character: Character,
    selectedItem: String?,
    selectedFromSlot: EquipmentSlot?,
    onItemClick: (String) -> Unit,
    onItemLongClick: (String) -> Unit,
    grimoireOuvert: Boolean,
    onItemDoubleClick: (String) -> Unit,
    onTransfer: (itemName: String, index: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    // Objet équipé actuellement sélectionné : une zone du panneau permet de le ranger dans le sac.
    objetARanger: String? = null,
    onRanger: () -> Unit = {}
) {
    val backpackName = character.equippedSlots[EquipmentSlot.BACKPACK]
    val weight = GameState.backpackWeight(character)
    val capacity = GameState.BACKPACK_CAPACITY_KG
    val overloaded = weight > capacity
    val eligiblesExterieur = character.backpackItems.distinct().filter { ArmorRules.isBackExteriorItem(it) }

    // Sélection locale pour placer un objet sur un emplacement extérieur : on touche
    // l'objet, puis l'emplacement — même principe que le reste de l'écran (sac général /
    // silhouette), pas de bouton "Transférer"/"Retirer" séparé. Touche l'emplacement à
    // nouveau (occupé, rien sélectionné) pour le vider directement.
    var selectedExteriorItem by remember(character.id) { mutableStateOf<String?>(null) }

    fun onExteriorSlotTapped(index: Int) {
        val chosen = selectedExteriorItem
        val occupant = character.backpackExteriorSlots[index]
        when {
            chosen != null -> {
                onTransfer(chosen, index)
                selectedExteriorItem = null
            }
            occupant != null -> onRemove(index)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SheetSurfaceLight,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backpack, null, tint = SheetTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sac à dos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                }
                Text(
                    text = "${String.format(java.util.Locale.FRANCE, "%.1f", weight)} / ${String.format(java.util.Locale.FRANCE, "%.1f", capacity)} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overloaded) MaterialTheme.colorScheme.error else SheetTextSecondary
                )
            }
            LinearProgressIndicator(
                progress = { (weight / capacity).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = if (overloaded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = SheetBorder
            )
            if (overloaded) {
                Text("Le sac déborde !", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            // Objet équipé sélectionné (arme, grimoire, potion...) : un toucher ici le range
            // dans le sac, sans passer par la barre de sélection.
            if (objetARanger != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onRanger() },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MoveToInbox, contentDescription = null, tint = SheetTextPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Toucher ici pour ranger « $objetARanger » dans le sac",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SheetTextPrimary
                        )
                    }
                }
            }

            if (backpackName != null) {
                HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                Text(
                    "Emplacements extérieurs (sur le sac, pas à l'intérieur) :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = SheetTextSecondary
                )
                (1..3).forEach { index ->
                    val occupant = character.backpackExteriorSlots[index]
                    val isTarget = selectedExteriorItem != null && occupant == null
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExteriorSlotTapped(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else SheetSurface,
                        border = BorderStroke(
                            if (isTarget) 1.5.dp else 1.dp,
                            if (isTarget) MaterialTheme.colorScheme.primary else SheetBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = occupant ?: "Emplacement extérieur $index (vide)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (occupant != null) SheetTextPrimary else SheetTextSecondary
                            )
                        }
                    }
                }
                if (eligiblesExterieur.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        eligiblesExterieur.forEach { itemName ->
                            SelectableItemChip(
                                item = itemName,
                                isEquipped = false,
                                isSelected = selectedExteriorItem == itemName,
                                onClick = {
                                    selectedExteriorItem = if (selectedExteriorItem == itemName) null else itemName
                                },
                                onDoubleClick = { onItemDoubleClick(itemName) }
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Aucun sac à dos équipé.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
            Text("Objets non équipés :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
            if (character.backpackItems.isEmpty()) {
                Text("Le sac est vide.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
            } else {
                // FlowRow : chaque puce garde sa largeur naturelle et passe à la ligne
                // suivante d'elle-même, au lieu de lignes fixes de 3 qui écrasaient les
                // puces voisines quand un nom d'objet était long.
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Objets strictement identiques empilés en une seule puce ("Rations (1
                    // jour) ×10") plutôt qu'une puce par unité — purement un regroupement
                    // d'affichage : `backpackItems` reste une liste à plat (pas d'ID unique
                    // par objet), et chaque interaction (sélection, double-clic, jeter)
                    // continue d'agir sur une seule occurrence.
                    val comptes = character.backpackItems.groupingBy { it }.eachCount()
                    character.backpackItems.distinct().forEach { item ->
                        SelectableItemChip(
                            item = item,
                            quantite = comptes[item] ?: 1,
                            isEquipped = false,
                            isSelected = item == selectedItem && selectedFromSlot == null,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                            ouvert = grimoireOuvert.takeIf { ArmorRules.isGrimoireItem(item) },
                            mention = Focaliseurs.type(character, item)?.let { Focaliseurs.libelle(it) },
                            onDoubleClick = { onItemDoubleClick(item) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Grand cadre coloré regroupant des emplacements compacts en icônes seules (Utilitaire,
 * Armes de dos). L'objet porté n'est pas nommé ici : son nom s'affiche dans la barre de
 * sélection au toucher, et ses détails au double-clic.
 */
@Composable
private fun SlotGroup(
    title: String,
    color: Color,
    slots: List<EquipmentSlot>,
    character: Character,
    selectedItem: String?,
    selectedFromSlot: EquipmentSlot?,
    onSlotClick: (EquipmentSlot) -> Unit,
    onSlotLongClick: (EquipmentSlot) -> Unit,
    onItemDoubleClick: (String) -> Unit
) {
    // Toucher le titre ouvre la vue détaillée (comme le sac à dos) : nom, quantité et poids
    // de chaque objet porté.
    var detailExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.5.dp, color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { detailExpanded = !detailExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (detailExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (detailExpanded) "Masquer le détail" else "Voir le détail",
                    tint = SheetTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                slots.forEach { slot ->
                    IconSlot(
                        slot = slot,
                        item = character.equippedSlots[slot],
                        count = character.stackCount(slot),
                        color = color,
                        isSelected = selectedFromSlot == slot,
                        isPlaceable = selectedItem != null && ArmorRules.itemAllowedInSlot(selectedItem, slot),
                        onClick = { onSlotClick(slot) },
                        onLongClick = { onSlotLongClick(slot) },
                        onDoubleClick = onItemDoubleClick
                    )
                }
            }

            if (detailExpanded) {
                HorizontalDivider(thickness = 0.5.dp, color = color.copy(alpha = 0.6f))
                slots.forEachIndexed { index, slot ->
                    val item = character.equippedSlots[slot]
                    val count = character.stackCount(slot)
                    val isSelected = selectedFromSlot == slot
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else SheetSurface)
                            .combinedClickable(
                                onClick = { onSlotClick(slot) },
                                onDoubleClick = item?.let { { onItemDoubleClick(it) } }
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            item?.let { itemIcon(it) } ?: slot.slotIcon(),
                            contentDescription = null,
                            tint = item?.let { couleurObjet(it, SheetTextPrimary) } ?: color,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                item == null -> "Emplacement ${index + 1} (vide)"
                                count > 1 -> "$item ×$count"
                                else -> item
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item != null) SheetTextPrimary else SheetTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        val poids = item?.let { equipmentWeightLabel(it) }
                        if (poids != null) {
                            Text(
                                text = if (count > 1) "$count × $poids" else poids,
                                style = MaterialTheme.typography.labelSmall,
                                color = SheetTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Case compacte (icône seule) d'un SlotGroup, avec badge ×N pour une pile. */
@Composable
private fun IconSlot(
    slot: EquipmentSlot,
    item: String?,
    count: Int,
    color: Color,
    isSelected: Boolean,
    isPlaceable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: (String) -> Unit
) {
    val hasItem = item != null
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isPlaceable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        hasItem -> color
        else -> SheetBorder
    }
    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                hasItem -> color.copy(alpha = 0.35f)
                isPlaceable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else -> SheetSurface
            },
            border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
            modifier = Modifier
                .size(52.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = item?.let { { onLongClick() } },
                    onDoubleClick = item?.let { { onDoubleClick(it) } }
                )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box {
                    Icon(
                        imageVector = item?.let { itemIcon(it) } ?: slot.slotIcon(),
                        contentDescription = item ?: slot.slotLabel(),
                        tint = item?.let { couleurObjet(it, SheetTextPrimary) } ?: color.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                    val hands = remember(item) { item?.let { equipmentHandsRequired(it) } }
                    if (hands != null) HandsBadge(hands, Modifier.align(Alignment.BottomEnd))
                }
            }
        }
        if (count > 1) {
            Text(
                text = "×$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun SlotBox(
    slot: EquipmentSlot,
    item: String?,
    label: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isPlaceable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDoubleClick: (String) -> Unit,
    mention: String? = null
) {
    // Emplacement occupé : icône de l'objet lui-même (épée, arc...), sinon celle de l'emplacement.
    val icon = remember(item) { item?.let { itemIcon(it) } ?: slot.slotIcon() }
    val hasItem = item != null

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        hasItem -> SheetTextSecondary
        isPlaceable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> SheetBorder
    }
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        hasItem -> SheetSurfaceLight
        isPlaceable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> SheetSurface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = item?.let { { onLongClick() } },
                onDoubleClick = item?.let { { onDoubleClick(it) } }
            )
    ) {
        val weightLabel = remember(item) { item?.let { equipmentWeightLabel(it) } }
        val hands = remember(item) { item?.let { equipmentHandsRequired(it) } }

        Column(
            modifier = Modifier
                .widthIn(min = 120.dp)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icône toujours visible dans une pastille de couleur (celle de l'emplacement) :
            // garantit un contraste constant que le thème/monde actif soit clair ou sombre,
            // au lieu d'un simple changement de teinte qui pouvait la rendre peu visible une
            // fois l'emplacement occupé.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(slot.slotColor().copy(alpha = if (hasItem) 0.28f else 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = slot.slotColor(),
                    modifier = Modifier.size(20.dp)
                )
                // Mains requises par l'arme, en bas à droite de son icône.
                if (hands != null) HandsBadge(hands, Modifier.align(Alignment.BottomEnd))
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (hasItem) {
                // Contour de couleur autour du nom = emplacement où l'objet peut être équipé
                Box(
                    modifier = Modifier
                        .border(1.dp, slot.slotColor(), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item ?: label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = SheetTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp)
                    )
                }
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    color = SheetTextSecondary,
                    modifier = Modifier.widthIn(min = 80.dp, max = 140.dp)
                )
            }
            if (hasItem) {
                if (weightLabel != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = weightLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = SheetTextSecondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                if (mention != null) {
                    Text(mention, style = MaterialTheme.typography.labelSmall, color = CouleurGrimoire, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun SelectableItemChip(
    item: String,
    isEquipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    quantite: Int = 1,
    onLongClick: (() -> Unit)? = null,
    // Contenant ouvrable (grimoire) : true/false = ouvert/fermé, null = objet ordinaire.
    ouvert: Boolean? = null,
    // Propriété affichée sous le nom (ex. « Focaliseur arcanique »).
    mention: String? = null
) {
    val slot = remember(item) { ArmorRules.slotForItem(item) }
    val weightLabel = remember(item) { equipmentWeightLabel(item) }
    val hands = remember(item) { equipmentHandsRequired(item) }
    // Couleur de l'emplacement de l'objet (violet pour le grimoire).
    val couleur = couleurObjet(item, slot.slotColor())

    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        isEquipped -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else -> SheetSurfaceLight
    }
    val contentColor = SheetTextPrimary

    Surface(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick
        ),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        // Le contour de la puce prend la couleur de l'emplacement où l'objet peut être
        // équipé (arme, armure, sac...), pour l'associer visuellement au nom entier.
        border = BorderStroke(if (isSelected) 2.dp else if (isEquipped) 1.5.dp else 1.dp, couleur)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isEquipped) {
                Icon(Icons.Default.Check, null, tint = contentColor, modifier = Modifier.size(16.dp))
            }
            Box {
                Icon(itemIcon(item), null, tint = couleur, modifier = Modifier.size(18.dp))
                // Mains requises (armes), en bas à droite de l'icône.
                if (hands != null) HandsBadge(hands, Modifier.align(Alignment.BottomEnd))
            }

            Column {
                Text(
                    text = if (quantite > 1) "$item ×$quantite" else item,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp)
                )
                if (weightLabel != null || mention != null) {
                    Text(
                        text = listOfNotNull(weightLabel, mention).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mention != null) CouleurGrimoire else SheetTextSecondary
                    )
                }
            }
            // Contenant (grimoire) : flèche d'ouverture, comme un inventaire.
            if (ouvert != null) {
                Icon(
                    if (ouvert) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (ouvert) "Fermer" else "Ouvrir",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentItemDetailDialog(
    itemName: String,
    character: Character,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var itemDetails by remember { mutableStateOf<EquipmentItemSRD?>(null) }

    LaunchedEffect(itemName) {
        itemDetails = SrdRepository.getEquipmentByName(context, itemName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(itemName) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (itemDetails != null) {
                    val item = itemDetails!!
                    if (item.category.isNotBlank()) Text("Catégorie : ${item.category}")
                    if (item.cost.isNotBlank()) Text("Coût : ${item.cost}")
                    if (item.weight.isNotBlank()) Text("Poids : ${item.weight}")
                    if (item.damage.isNotBlank()) Text("Dégâts : ${item.damage}")
                    if (item.properties.isNotBlank()) Text("Propriétés : ${item.properties}")
                    if (item.ac.isNotBlank()) Text("CA : ${item.ac}")
                    // Propriété de focaliseur (bâton du Magicien, grimoire d'un Magicien...).
                    Focaliseurs.type(character, itemName)?.let { type ->
                        Text(
                            "Propriété : ${Focaliseurs.libelle(type)} — remplace les composantes matérielles sans coût indiqué quand le personnage lance ses sorts.",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (item.harmonisation) {
                        val harmonise = Harmonisation.estHarmonise(character, item.name)
                        Text(
                            Harmonisation.libelle(item) + if (harmonise) " — harmonisé" else " — non harmonisé (propriétés magiques inactives)",
                            fontWeight = FontWeight.Bold,
                            color = if (harmonise) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    // Description + champs libres (ex. "Contenu : ..." pour un paquetage) —
                    // absents jusqu'ici de ce dialogue, alors qu'EquipmentParser les conserve
                    // tous dans rawMarkdown (cf. Explorateur, Cambrioleur...).
                    if (item.rawMarkdown.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.rawMarkdown.trim())
                    }
                } else {
                    Text("Aucune fiche SRD trouvée pour cet objet.")
                    Focaliseurs.type(character, itemName)?.let { type ->
                        Text("Propriété : ${Focaliseurs.libelle(type)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val slot = ArmorRules.slotForItem(itemName)
                Text(
                    text = "Slot : ${slot.slotLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

/** Teinte d'icône d'un objet : violet pour le grimoire, sinon [defaut]. */
private fun couleurObjet(item: String, defaut: Color): Color =
    if (ArmorRules.isGrimoireItem(item)) CouleurGrimoire else defaut
