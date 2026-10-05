package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Consommables
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeEnMain
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.CategorieActionCombat
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val IconesActions: Map<String, ImageVector> = mapOf(
    "attaquer" to Icons.Default.SportsMartialArts,
    "magie" to Icons.Default.AutoFixHigh,
    "dash" to Icons.Default.DirectionsRun,
    "disengage" to Icons.Default.DirectionsRun,
    "dodge" to Icons.Default.Shield,
    "hide" to Icons.Default.VisibilityOff,
    "help" to Icons.Default.Groups,
    "ready" to Icons.Default.Pause,
    "search" to Icons.Default.Search,
    "study" to Icons.Default.MenuBook,
    "influence" to Icons.Default.RecordVoiceOver,
    "utilize" to Icons.Default.Build,
    "improvise" to Icons.Default.Handyman,
)

/**
 * Outil de référence rapide pendant un combat. Offensive & Magie et Mouvement & Posture — les
 * décisions du tour en cours — restent en permanence au centre de l'écran ; Tactique &
 * Compétences et Environnement & Équipement — les approches alternatives, moins fréquentes —
 * sont repliées dans un bottom sheet permanent qu'on tire vers le haut au besoin, plutôt que
 * d'occuper l'écran en continu. Accessible aussi bien MJ que Joueur (même écran, pas de donnée
 * propre à un rôle) — voir le tiroir latéral des deux côtés. Les consommables équipés du
 * personnage sélectionné (potions...) y sont utilisables directement (voir Consommables).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CombatActionsScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
    titre: String = "Actions de Combat",
    // Combat en réseau (joueur) : bandeau en tête, et un appui sur une action la choisit au lieu
    // d'afficher sa description.
    entete: (@Composable () -> Unit)? = null,
    onChoisirAction: ((ActionCombat) -> Unit)? = null,
    // Combat en réseau : personnage du joueur (sinon le personnage sélectionné), et choix d'une
    // arme depuis sa carte (action Attaquer avec cette arme).
    personnageId: String? = null,
    onChoisirArme: ((ArmeEnMain) -> Unit)? = null,
    // Combat en réseau : vue tactique (distance et santé de chacun), ouverte par le bouton en haut
    // à droite à la place des actions, et réglage du déplacement du round.
    tactique: (@Composable () -> Unit)? = null,
    sectionDeplacement: (@Composable () -> Unit)? = null,
    // Ligne sous le titre (ex. « Round 3 ») et bouton supplémentaire en haut à droite (menu).
    sousTitre: String? = null,
    actionHaut: (@Composable () -> Unit)? = null,
    // MJ, hors combat en réseau : ouvre l'écran de simulation de combat du personnage affiché.
    onSimuler: ((String) -> Unit)? = null,
    // Simulation : emplacements restants de la simulation (carte des ressources).
    emplacementsSimulation: Map<Int, Int>? = null,
    // Simulation : arsenal avec les munitions / armes lancées restantes de la simulation.
    arsenalSimulation: ArsenalJoueur? = null,
) {
    var actionDetail by remember { mutableStateOf<ActionCombat?>(null) }
    var vueTactique by remember { mutableStateOf(false) }
    val choisir: (ActionCombat) -> Unit = { a -> if (onChoisirAction != null) onChoisirAction(a) else actionDetail = a }
    val context = LocalContext.current
    val personnages by GameState.characters.collectAsState()
    val selectionId by GameState.selectedCharacterId.collectAsState()
    // Hors combat en réseau, on choisit de qui afficher les capacités (côté MJ, aucun
    // personnage n'est « sélectionné ») : PJ du monde courant, le sélectionné par défaut.
    val mondeCourant = GameState.currentWorldId()
    val candidats = personnages.filter { it.type == "PJ" && (mondeCourant == null || it.worldId.isBlank() || it.worldId == mondeCourant) }
    var personnageChoisiId by remember { mutableStateOf<String?>(null) }
    val personnage = personnages.firstOrNull { it.id == personnageId }
        ?: candidats.firstOrNull { it.id == personnageChoisiId }
        ?: candidats.firstOrNull { it.id == selectionId }
        ?: personnages.firstOrNull { it.id == selectionId }
        ?: candidats.firstOrNull()
    // Armes équipées et capacités d'attaque, présentées en cartes (une par capacité de combat).
    val arsenalFiche = rememberArsenal(personnage)
    val arsenal = arsenalSimulation ?: arsenalFiche
    var armeDetailNom by remember { mutableStateOf<String?>(null) }
    var armeDetailDerniere by remember { mutableStateOf<ArmeEnMain?>(null) }
    var consommables by remember { mutableStateOf<List<EquipmentItem>>(emptyList()) }
    var messageObjet by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(personnage?.equippedSlots, personnage?.equippedItems, personnage?.backpackExteriorSlots) {
        consommables = personnage?.let { Consommables.equipesDuPersonnage(context, it) }.orEmpty()
    }
    val estMj = GameState.appRole.collectAsState().value == com.jc2.jdrcompagnon.ui.AppRole.MJ
    val sheetState: SheetState = rememberStandardBottomSheetState()
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    BottomSheetScaffold(
        // La barre du bas de l'appli (AppBottomBar) inclut la barre de navigation Android en plus
        // des 80 dp réservés par le NavHost : sans ce décalage, le volet du bas passait dessous.
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        scaffoldState = scaffoldState,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(titre, fontWeight = FontWeight.Bold)
                        sousTitre?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = ForcedDarkPalette.AccentGold) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // MJ, hors combat en réseau : simuler un combat du personnage affiché contre des monstres.
                    if (estMj && onSimuler != null && onChoisirAction == null && personnage != null && arsenal != null) {
                        IconButton(onClick = { onSimuler(personnage.id) }) {
                            Icon(Icons.Default.Science, contentDescription = "Simuler un combat de ${personnage.name}")
                        }
                    }
                    if (tactique != null) {
                        FilterChip(
                            selected = vueTactique,
                            onClick = { vueTactique = !vueTactique },
                            label = { Text(if (vueTactique) "Actions" else "Tactique") },
                            leadingIcon = {
                                Icon(if (vueTactique) Icons.Default.SportsMartialArts else Icons.Default.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    actionHaut?.invoke()
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        sheetPeekHeight = 76.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = Color.White,
        containerColor = Color.Transparent,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Partie toujours visible (hauteur = sheetPeekHeight) : sert de poignée pour
                // indiquer qu'il y a plus de contenu à tirer vers le haut.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Approches alternatives",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = ForcedDarkPalette.AccentGold
                        )
                        Text(
                            text = "Tactique & Compétences · Environnement & Équipement",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Tirer vers le haut", tint = Color.White)
                }

                // 3. Tactique & Compétences : liste extensible, une ligne par action.
                CategorieCard(CategorieActionCombat.TACTIQUE_COMPETENCES) {
                    Column {
                        ActionsCombat.parCategorie(CategorieActionCombat.TACTIQUE_COMPETENCES).forEach { action ->
                            ListItem(
                                headlineContent = { Text(action.nom, color = Color.White) },
                                leadingContent = {
                                    IconesActions[action.id]?.let { Icon(it, contentDescription = null, tint = Color.White) }
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { choisir(action) }
                            )
                        }
                    }
                }

                // 4. Environnement & Équipement : interactions avec le monde matériel.
                CategorieCard(CategorieActionCombat.ENVIRONNEMENT_EQUIPEMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionsCombat.parCategorie(CategorieActionCombat.ENVIRONNEMENT_EQUIPEMENT).forEach { action ->
                            OutlinedButton(onClick = { choisir(action) }, modifier = Modifier.weight(1f)) {
                                IconesActions[action.id]?.let {
                                    Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(action.nom)
                            }
                        }
                    }
                    // Consommables équipés du personnage sélectionné : action Utiliser.
                    if (personnage != null && consommables.isNotEmpty()) {
                        Text("Objets équipés de " + personnage.name, style = MaterialTheme.typography.labelMedium, color = Color.White)
                        consommables.forEach { objet ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(objet.name, color = Color.White)
                                    Text(Consommables.resume(objet), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                }
                                OutlinedButton(onClick = {
                                    // Déclaration de combat : l'objet est annoncé (action Utiliser), pas consommé tout de suite.
                                    if (onChoisirAction != null) ActionsCombat.toutes.firstOrNull { it.id == "utilize" }?.let(onChoisirAction)
                                    else messageObjet = Consommables.utiliser(personnage, objet)
                                }) { Text("Utiliser") }
                            }
                        }
                    }
                    messageObjet?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) { innerPadding ->
        if (vueTactique && tactique != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp)
            ) { tactique() }
            return@BottomSheetScaffold
        }
        // Offensive & Magie et Mouvement & Posture : les décisions du tour, toujours visibles,
        // centrées verticalement dans l'espace laissé par le bottom sheet replié.
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Centré quand tout tient à l'écran, défilable sinon (carte des actions bonus du Paladin).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                entete?.let {
                    it()
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Emplacements de sort et ressources de classe, visibles pendant le choix de l'action.
                if (personnage != null && arsenal != null) {
                    CarteRessourcesCombat(personnage, arsenal, emplacementsSimulation)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Rappel des règles : seulement hors combat en réseau (outil de référence).
                if (onChoisirAction == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Text(
                            text = ActionsCombat.RAPPEL_ACTION_PRINCIPALE,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                CategorieCard(CategorieActionCombat.OFFENSIVE_MAGIE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        ActionsCombat.parCategorie(CategorieActionCombat.OFFENSIVE_MAGIE).forEach { action ->
                            BoutonActionPrincipale(action = action, onClick = { choisir(action) }, modifier = Modifier.weight(1f))
                        }
                    }
                    // Chaque capacité de combat en carte : armes équipées (munitions, lancer) et
                    // capacités d'attaque de classe.
                    if (personnageId == null && candidats.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidats.forEach { c ->
                                FilterChip(
                                    selected = c.id == personnage?.id,
                                    onClick = { personnageChoisiId = c.id },
                                    label = { Text(c.name) },
                                )
                            }
                        }
                    }
                    if (personnage != null && arsenal == null) {
                        Text("Chargement des capacités de ${personnage.name}…", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    if (personnage != null && arsenal != null) {
                        Text("Capacités de combat de ${personnage.name}", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        CartesCapacitesCombat(arsenal) { arme ->
                            if (onChoisirArme != null) {
                                if (!arme.sansMunition) onChoisirArme(arme)
                                else messageObjet = "Plus de ${arme.munition} : impossible d'attaquer avec ${arme.nom}."
                            } else {
                                armeDetailNom = arme.nom
                                armeDetailDerniere = arme
                            }
                        }
                        if (onChoisirArme != null) messageObjet?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions bonus de classe (Paladin) du personnage sélectionné, hors combat en réseau :
                // effet appliqué directement aux fiches (en réseau, elles sont sur l'écran de combat).
                if (personnage != null && onChoisirAction == null && aActionsBonusClasse(personnage)) {
                    ActionsBonusPaladin(
                        personnage = personnage,
                        cibles = listOf(CibleContact(personnage.id, personnage.name + " (vous)")) +
                            personnages.filter { it.id != personnage.id && it.worldId == personnage.worldId }.map { CibleContact(it.id, it.name) },
                        emplacementsRestants = arsenal?.restants(personnage).orEmpty(),
                        onImposition = { cible, pv, poison ->
                            GameState.soignerOuGuerirPoison(cible.id, pv, poison)
                            if (poison) "Empoisonné retiré de ${cible.nom}." else "${cible.nom} récupère $pv PV."
                        },
                        onChatiment = { des, formule -> "Châtiment divin : ${des.sum()} dégâts radiants (${des.joinToString("+")}) — $formule." },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                CategorieCard(CategorieActionCombat.MOUVEMENT_POSTURE) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionsCombat.parCategorie(CategorieActionCombat.MOUVEMENT_POSTURE).forEach { action ->
                            AssistChip(
                                onClick = { choisir(action) },
                                label = { Text(action.nom) },
                                leadingIcon = {
                                    IconesActions[action.id]?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = Color.White,
                                    leadingIconContentColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Déplacement du round, choisi ici et non dans le détail de l'action.
                sectionDeplacement?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    it()
                }
            }
        }
    }

    armeDetailNom?.let { nom ->
        // Relue dans l'arsenal à jour (munitions restantes), sinon dernière version connue
        // (dernière javeline lancée : l'arme n'est plus équipée).
        val arme = arsenal?.armes?.firstOrNull { it.nom == nom } ?: armeDetailDerniere
        if (arsenal != null && arme != null) {
            DetailArmeDialog(arsenal, arme, personnage) { armeDetailNom = null }
        }
    }

    actionDetail?.let { action ->
        AlertDialog(
            onDismissRequest = { actionDetail = null },
            title = { Text(action.nom, fontWeight = FontWeight.Bold) },
            text = { Text(action.description) },
            confirmButton = {
                TextButton(onClick = { actionDetail = null }) { Text("Fermer") }
            }
        )
    }
}

@Composable
private fun CategorieCard(categorie: CategorieActionCombat, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = categorie.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = ForcedDarkPalette.AccentGold
            )
            content()
        }
    }
}

@Composable
private fun BoutonActionPrincipale(action: ActionCombat, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = ForcedDarkPalette.AccentGold)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconesActions[action.id]?.let {
                Icon(it, contentDescription = null, tint = ForcedDarkPalette.Background, modifier = Modifier.size(32.dp))
            }
            Text(
                text = action.nom,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ForcedDarkPalette.Background
            )
        }
    }
}
