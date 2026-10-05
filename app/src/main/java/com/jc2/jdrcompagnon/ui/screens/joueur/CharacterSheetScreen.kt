@file:OptIn(ExperimentalFoundationApi::class)

package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.CharacterProgression
import com.jc2.jdrcompagnon.ui.calculateProficiencyBonus
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.components.PnjPortraits
import com.jc2.jdrcompagnon.ui.components.rememberCharacterPortraitPainter
import com.jc2.jdrcompagnon.ui.components.PnjReputationEditor
import com.jc2.jdrcompagnon.ui.components.EquipmentManagementContent
import com.jc2.jdrcompagnon.ui.components.SheetBorder
import com.jc2.jdrcompagnon.ui.components.SheetCard
import com.jc2.jdrcompagnon.ui.components.SheetSurface
import com.jc2.jdrcompagnon.ui.components.SheetSurfaceLight
import com.jc2.jdrcompagnon.ui.components.SheetTextPrimary
import com.jc2.jdrcompagnon.ui.components.SheetTextSecondary
import com.jc2.jdrcompagnon.ui.components.SortApercuCarte
import com.jc2.jdrcompagnon.ui.components.FiltreSortsBarre
import com.jc2.jdrcompagnon.ui.components.rememberFiltreSorts
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.markdownEnLigne
import com.jc2.jdrcompagnon.ui.components.SrdLibraryPickerDialog
import com.jc2.jdrcompagnon.ui.components.SrdPickerEntry
import com.jc2.jdrcompagnon.ui.components.sheetTextFieldColors
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmesPersonnage
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ObjetsACharges
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Harmonisation
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Consommables
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.DureeEffet
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.AptitudeClasse
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Caracteristique
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ChoixBaliseCarte
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ContexteChoix
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Don
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.DonParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.LangueParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.construireChoix
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EffetBalise
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EmplacementsDeSortParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Espece
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EspeceParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.choixEspece
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.effetsEspece
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.traitsEspecePourFiche
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.TOUTES_COMPETENCES
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.TableMd
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.parseCompetencesClasse
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.valeurNiveau
import com.jc2.jdrcompagnon.ui.screens.joueur.savingThrows
import com.jc2.jdrcompagnon.ui.screens.joueur.skillList
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.mikepenz.markdown.m3.Markdown
import com.jc2.jdrcompagnon.ui.screens.joueur.character.CapaciteFiche
import com.jc2.jdrcompagnon.ui.screens.joueur.character.CapaciteDescription
import com.jc2.jdrcompagnon.ui.screens.joueur.character.CouleurChoix
import com.jc2.jdrcompagnon.ui.screens.joueur.character.parserCapacitesFiche
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterSheetScreen(
    character: Character,
    isMjMode: Boolean = false,
    onBack: () -> Unit,
    onEdit: ((Character) -> Unit)? = null,
    // Distinct de onBack : "Choisir un personnage" doit toujours rouvrir la
    // liste de sélection (même item, même comportement que sur JoueurHomeScreen
    // et CharacterSelectionScreen), pas juste dépiler l'écran courant — sinon
    // le même libellé fait des choses différentes selon d'où on arrive ici.
    onChooseCharacter: () -> Unit = onBack,
    onOpenMenu: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val allCharacters by GameState.characters.collectAsState()
    val currentCharacter = allCharacters.find { it.id == character.id } ?: character
    // Fiche de PNJ : pas d'onglet Sorts, ses capacités (et sorts éventuels) sont dans Combat,
    // son briefing (comportement / intentions / objectif) dans Notes.
    val isPnj = currentCharacter.type == "PNJ"
    val tabs = buildList {
        // PNJ : vue simplifiée façon bloc de stats du bestiaire, ouverte par défaut.
        if (isPnj) add(SheetTab.FICHE)
        add(SheetTab.APERCU)
        add(SheetTab.COMBAT)
        add(SheetTab.EQUIPEMENT)
        if (!isPnj) add(SheetTab.SORTS)
        add(SheetTab.NOTES)
    }
    val tabTitles = tabs.map { it.titre }
    val tabIcons = tabs.map { it.icone }
    if (selectedTab >= tabs.size) selectedTab = 0

    if (showHistoryDialog) {
        com.jc2.jdrcompagnon.ui.screens.joueur.character.CharacterHistoryDialog(
            characterName = currentCharacter.name,
            characterId = currentCharacter.id,
            onDismiss = { showHistoryDialog = false }
        )
    }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onDoubleClick = { showHistoryDialog = true }
                            )
                        ) {
                            Text(
                                text = currentCharacter.name,
                                // Nom long : police réduite et retour à la ligne plutôt
                                // qu'un nom coupé sur une seule ligne.
                                style = (if (currentCharacter.name.length > 16) MaterialTheme.typography.titleMedium
                                    else MaterialTheme.typography.titleLarge).copy(fontWeight = FontWeight.Bold),
                                color = SheetTextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${currentCharacter.race} ${libelleClasses(currentCharacter)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SheetTextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = SheetTextPrimary)
                        }
                    },
                    actions = {
                        if (isMjMode && onEdit != null) {
                            IconButton(onClick = { onEdit(currentCharacter) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = SheetTextPrimary)
                            }
                        }
                        if (isMjMode) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = SheetTextPrimary,
                        navigationIconContentColor = SheetTextPrimary,
                        actionIconContentColor = SheetTextPrimary
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Carte d'en-tête commune, présente sur tous les onglets sauf la vue simplifiée
                // du PNJ (son bloc de stats reprend déjà ces informations).
                if (tabs[selectedTab] != SheetTab.FICHE) CharacterStatsHeader(currentCharacter, isMjMode)

                // Tab selector, sous la carte d'en-tête — icônes uniquement, toujours visible
                // quel que soit l'onglet sélectionné (y compris Sorts).
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        tabIcons.forEachIndexed { index, icon ->
                            val selected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { selectedTab = index }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = tabTitles[index],
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                when (tabs[selectedTab]) {
                    SheetTab.FICHE -> PnjFicheSimplifiee(currentCharacter, isMjMode)
                    SheetTab.APERCU -> OverviewTab(currentCharacter, isMjMode)
                    SheetTab.COMBAT -> CombatTab(currentCharacter, isMjMode)
                    SheetTab.EQUIPEMENT -> EquipmentTab(currentCharacter, isMjMode)
                    SheetTab.SORTS -> SpellsTab(currentCharacter, isMjMode)
                    SheetTab.NOTES -> NotesTab(currentCharacter, isMjMode)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmer la suppression") },
            text = { Text("Supprimer ${currentCharacter.name} ? Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GameState.removeCharacter(character.id)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Supprimer") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }
}

/** Onglets de la fiche (le PNJ n'a pas l'onglet Sorts, cf. CharacterSheetScreen). */
private enum class SheetTab(val titre: String, val icone: androidx.compose.ui.graphics.vector.ImageVector) {
    FICHE("Fiche simplifiée", Icons.Default.Badge),
    APERCU("Aperçu", Icons.Default.Person),
    COMBAT("Combat", Icons.Default.Shield),
    EQUIPEMENT("Équipement", Icons.Default.Backpack),
    SORTS("Sorts", Icons.AutoMirrored.Filled.MenuBook),
    NOTES("Notes", Icons.AutoMirrored.Filled.Article),
}

/**
 * Barre de niveau affichée sous le nom du personnage (remplace
 * l'ancien menu "Monter de niveau").
 */
@Composable
private fun LevelBar(level: Int, experience: Int) {
    val progress = CharacterProgression.progressToNextLevel(level, experience)
    val nextThreshold = CharacterProgression.xpForLevel((level + 1).coerceAtMost(20))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, SheetBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            trackColor = SheetSurfaceLight
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NIV. $level",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                color = SheetTextPrimary
            )
            Text(
                text = if (level >= 20) "$experience XP (max)" else "$experience / $nextThreshold XP",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = SheetTextPrimary
            )
        }
    }
}

/**
 * Vue simplifiée d'un PNJ : bloc de stats façon bestiaire, puis (MJ) son comportement, ses
 * possessions à échanger et une conversation improvisée. Les autres onglets restent la fiche
 * complète, pour l'éditer en détail.
 */
@Composable
private fun PnjFicheSimplifiee(character: Character, isMjMode: Boolean) {
    var afficherButin by remember { mutableStateOf(false) }
    var conversation by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        com.jc2.jdrcompagnon.ui.screens.mj.library.PnjStatBlock(
            character,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        )
        if (isMjMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { conversation = true }, modifier = Modifier.weight(1f)) {
                    Text("💬 Conversation", color = SheetTextPrimary)
                }
                OutlinedButton(onClick = { afficherButin = true }, modifier = Modifier.weight(1f)) {
                    Text("💰 Possessions", color = SheetTextPrimary)
                }
            }
            com.jc2.jdrcompagnon.ui.screens.mj.scenario.InfosComportementalesPnj(character)
        }
    }
    if (afficherButin) {
        com.jc2.jdrcompagnon.feature_butin.ui.ButinPersonnageDialog(character, onDismiss = { afficherButin = false })
    }
    if (conversation) {
        val groupeId by GameState.currentGroupId.collectAsState()
        com.jc2.jdrcompagnon.ui.screens.mj.scenario.PnjBriefingOverlay(
            character = character,
            reputationGroupe = groupeId?.let { character.groupReputations[it] },
            onDismiss = { conversation = false }
        )
    }
}

@Composable
private fun OverviewTab(character: Character, isMjMode: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Réputation du PNJ (factions + groupes), réservée au MJ : modifiable directement ici,
        // sans passer par l'écran d'édition.
        if (isMjMode && character.type == "PNJ") {
            SheetCard {
                PnjReputationEditor(
                    character = character,
                    onUpdate = { GameState.updateCharacter(it) },
                    textColor = SheetTextPrimary,
                    sectionTitle = { titre ->
                        Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                    }
                )
            }
        }

        SheetCard {
            Text(
                "Caractéristiques",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            AbilitiesGrid(character, isMjMode)
        }

        SavingThrowsOverviewCard(character)

        SheetCard {
            Text("Compétences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            skillList.forEach { skill ->
                ProficiencyRow(
                    label = "$skill (${skillAbilityAbbreviation(skill)})",
                    level = GameState.skillProficiencyLevel(character, skill),
                    modifier = GameState.abilityModifierForSkill(skill, character),
                    proficiencyBonus = character.proficiencyBonus,
                    onClick = if (isMjMode) {
                        { GameState.cycleSkillProficiency(character.id, skill) }
                    } else null
                )
            }
        }
    }
}

/**
 * Bandeau d'en-tête façon fiche de personnage : armure, initiative,
 * portrait, points de vie avec barre de progression.
 */
@Composable
internal fun CharacterStatsHeader(character: Character, isMjMode: Boolean = false) {
    val init = GameState.abilityModifier(character.dexterity)
    val passivePerception = 10 + GameState.abilityModifierForSkill("Perception", character)
    val hitDieFaces = hitDieForClass(character.characterClass)
    val hitDiceRemaining = (character.level - character.hitDiceUsed).coerceIn(0, character.level)
    val hpFraction = if (character.maxHitPoints > 0) {
        character.currentHitPoints.toFloat() / character.maxHitPoints.toFloat()
    } else 0f
    var showPortraitPicker by remember { mutableStateOf(false) }
    var showHpDialog by remember { mutableStateOf(false) }
    var showHitDiceDialog by remember { mutableStateOf(false) }
    var showConditionDialog by remember { mutableStateOf(false) }
    var showXpDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSizeDialog by remember { mutableStateOf(false) }
    var showProficiencyDetail by remember { mutableStateOf(false) }
    var showInitiativeDetail by remember { mutableStateOf(false) }
    var showPassivePerceptionDetail by remember { mutableStateOf(false) }
    var showLevelTableDialog by remember { mutableStateOf(false) }
    var showLevelUpDialog by remember { mutableStateOf(false) }
    // L'XP n'entraîne plus le niveau automatiquement (cf. GameState.appliquerMonteeDeNiveau) :
    // dès que l'XP accumulée dépasse le seuil du niveau suivant, un bandeau "Monter de
    // niveau" apparaît sous la barre et ouvre LevelUpDialog au clic.
    val peutMonterDeNiveau = character.level < 20 &&
        CharacterProgression.levelForXp(character.experience) > character.level
    // TODO: `character.heroicInspiration` doit être ajouté au data class Character
    // (Boolean, défaut false) et un GameState.setHeroicInspiration(id, value)
    // doit être créé sur le même modèle que GameState.setCharacterPortrait.
    val hasInspiration = character.heroicInspiration

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = SheetSurface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colonne gauche : Maîtrise, Inspiration
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBadge(
                        value = "+${character.proficiencyBonus}",
                        label = "MAÎTRISE",
                        boxModifier = Modifier.size(44.dp),
                        onDoubleClick = { showProficiencyDetail = true }
                    )
                    InspirationBadge(
                        active = hasInspiration,
                        onToggle = { GameState.setHeroicInspiration(character.id, !hasInspiration) }
                    )
                }

                // Portrait agrandi, centré
                Box(
                    modifier = Modifier.width(120.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(156.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, PortraitFrameGold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SheetBorder)
                            .border(3.dp, PortraitFrameGold, RoundedCornerShape(8.dp))
                            // Le portrait se choisit désormais à la création du personnage
                            // (EtapePortrait) ; côté joueur, la fiche ne permet plus de le
                            // modifier après coup — seul le MJ le peut encore (ex. PNJ).
                            .let { if (isMjMode) it.clickable { showPortraitPicker = true } else it },
                        contentAlignment = Alignment.Center
                    ) {
                        val portraitPainter = rememberCharacterPortraitPainter(character.portrait)
                        if (portraitPainter != null) {
                            Image(
                                painter = portraitPainter,
                                contentDescription = "Portrait de ${character.name}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = SheetTextPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                // Colonne droite : PV puis Dés de vie
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .let { if (isMjMode) it.clickable { showHpDialog = true } else it }
                            .padding(4.dp)
                    ) {
                        Text(
                            "PV",
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${character.currentHitPoints}/${character.maxHitPoints}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { hpFraction.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = SheetBorder
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .let { if (isMjMode) it.clickable { showHitDiceDialog = true } else it }
                            .padding(4.dp)
                    ) {
                        Text(
                            "DÉ DE VIE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "D$hitDieFaces $hitDiceRemaining/${character.level}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { showConditionDialog = true }
                            .padding(4.dp)
                    ) {
                        Text(
                            "CONDITION",
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            character.condition.ifBlank { "Aucune" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (isMjMode) showXpDialog = true },
                        onDoubleClick = { showLevelTableDialog = true }
                    ),
                contentAlignment = Alignment.Center
            ) {
                LevelBar(level = character.level, experience = character.experience)
            }

            if (peutMonterDeNiveau) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLevelUpDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        "MONTER DE NIVEAU !",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 cases sous la barre d'XP : Initiative, Vitesse, Taille, Perception passive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatBadge(
                    value = if (init >= 0) "+$init" else init.toString(),
                    label = "INITIATIVE",
                    modifier = Modifier.weight(1f),
                    onDoubleClick = { showInitiativeDetail = true }
                )
                StatBadge(
                    value = "${character.speed}m",
                    label = "VITESSE",
                    modifier = Modifier.weight(1f),
                    onClick = if (isMjMode) { { showSpeedDialog = true } } else null
                )
                StatBadge(
                    value = character.size.ifBlank { sizeForRace(character.race) },
                    label = "TAILLE",
                    modifier = Modifier.weight(1f),
                    onClick = if (isMjMode) { { showSizeDialog = true } } else null
                )
                StatBadge(
                    value = passivePerception.toString(),
                    label = "PERC. PASSIVE",
                    modifier = Modifier.weight(1f),
                    onDoubleClick = { showPassivePerceptionDetail = true }
                )
            }
        }
    }

    if (showPortraitPicker) {
        PortraitPickerDialog(
            currentPortrait = character.portrait,
            pnj = character.type == "PNJ",
            onSelect = { portraitId ->
                GameState.setCharacterPortrait(character.id, portraitId)
                showPortraitPicker = false
            },
            onDismiss = { showPortraitPicker = false }
        )
    }

    if (showHpDialog) {
        HpEditDialog(
            currentHp = character.currentHitPoints,
            maxHp = character.maxHitPoints,
            onConfirm = { current, max ->
                GameState.setCharacterHp(character.id, current, max)
                showHpDialog = false
            },
            onDismiss = { showHpDialog = false }
        )
    }

    if (showHitDiceDialog) {
        HitDiceEditDialog(
            hitDieFaces = hitDieFaces,
            level = character.level,
            hitDiceRemaining = hitDiceRemaining,
            onConfirm = { remaining ->
                GameState.setHitDiceUsed(character.id, character.level - remaining)
                showHitDiceDialog = false
            },
            onDismiss = { showHitDiceDialog = false }
        )
    }

    if (showConditionDialog) {
        ConditionEditDialog(
            currentCondition = character.condition,
            onConfirm = { value ->
                GameState.setCondition(character.id, value)
                showConditionDialog = false
            },
            onDismiss = { showConditionDialog = false }
        )
    }

    if (showXpDialog) {
        XpEditDialog(
            currentXp = character.experience,
            onConfirm = { newXp ->
                GameState.setExperience(character.id, newXp)
                showXpDialog = false
            },
            onDismiss = { showXpDialog = false }
        )
    }

    if (showSpeedDialog) {
        SpeedEditDialog(
            currentSpeed = character.speed,
            onConfirm = { newSpeed ->
                GameState.setCharacterSpeed(character.id, newSpeed)
                showSpeedDialog = false
            },
            onDismiss = { showSpeedDialog = false }
        )
    }

    if (showSizeDialog) {
        SizeEditDialog(
            currentSize = character.size.ifBlank { sizeForRace(character.race) },
            onConfirm = { newSize ->
                GameState.setCharacterSize(character.id, newSize)
                showSizeDialog = false
            },
            onDismiss = { showSizeDialog = false }
        )
    }

    if (showProficiencyDetail) {
        StatDetailDialog(
            title = "Bonus de maîtrise",
            lignes = listOf(
                "Le bonus de maîtrise dépend du niveau du personnage.",
                "Niveau ${character.level} → +${character.proficiencyBonus}",
                "S'ajoute aux jets pour lesquels le personnage est maîtrisé (compétences, jets de sauvegarde, armes maîtrisées...)."
            ),
            onDismiss = { showProficiencyDetail = false }
        )
    }

    if (showInitiativeDetail) {
        StatDetailDialog(
            title = "Initiative",
            lignes = listOfNotNull(
                "Initiative = modificateur de Dextérité = ${GameState.abilityModifier(character.dexterity)} (DEX ${character.dexterity})",
                if (character.initiativeBonus != 0) "Bonus supplémentaire : ${if (character.initiativeBonus >= 0) "+" else ""}${character.initiativeBonus}" else null
            ),
            onDismiss = { showInitiativeDetail = false }
        )
    }

    if (showPassivePerceptionDetail) {
        val percMod = GameState.abilityModifierForSkill("Perception", character)
        StatDetailDialog(
            title = "Perception passive",
            lignes = listOf(
                "Perception passive = 10 + modificateur de Perception",
                "10 + $percMod = $passivePerception",
                if (character.skillProficiencies.contains("Perception")) "Inclut le bonus de maîtrise (+${character.proficiencyBonus})." else "Non maîtrisé en Perception."
            ),
            onDismiss = { showPassivePerceptionDetail = false }
        )
    }

    if (showLevelTableDialog) {
        LevelProgressionTableDialog(currentLevel = character.level, onDismiss = { showLevelTableDialog = false })
    }

    if (showLevelUpDialog) {
        LevelUpDialog(character = character, onDismiss = { showLevelUpDialog = false })
    }
}

/** Tableau de progression (niveau → XP requise), avec le niveau actuel mis en évidence. */
@Composable
private fun LevelProgressionTableDialog(currentLevel: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tableau de montée de niveau") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Niveau", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
                    Text("XP requise", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
                    Text("Maîtrise", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
                }
                HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                for (niveau in 1..20) {
                    val estActuel = niveau == currentLevel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            niveau.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (estActuel) FontWeight.Bold else FontWeight.Normal,
                            color = if (estActuel) MaterialTheme.colorScheme.primary else SheetTextPrimary
                        )
                        Text(
                            "${CharacterProgression.xpForLevel(niveau)} XP",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (estActuel) FontWeight.Bold else FontWeight.Normal,
                            color = if (estActuel) MaterialTheme.colorScheme.primary else SheetTextPrimary
                        )
                        Text(
                            "+${calculateProficiencyBonus(niveau)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (estActuel) FontWeight.Bold else FontWeight.Normal,
                            color = if (estActuel) MaterialTheme.colorScheme.primary else SheetTextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

/**
 * Une "piste" de niveau possible lors d'une montée de niveau : soit continuer la classe
 * principale ou une classe secondaire déjà en cours (`estNouvelle = false`), soit
 * multiclasser dans une classe pas encore prise (`estNouvelle = true`, uniquement
 * proposée si ses prérequis — et ceux de toutes les classes déjà en cours — sont
 * remplis, cf. Classe.remplitPrerequisMulticlasse).
 */
private data class PisteNiveau(val classe: Classe, val niveauActuel: Int, val estNouvelle: Boolean, val estPrincipale: Boolean)

/**
 * Dialogue de montée de niveau (niveau total du personnage + 1) : points de vie gagnés
 * (valeur fixe par classe ou dé lancé, au choix), piste de classe à faire progresser
 * (classe principale, classe secondaire déjà prise, ou multiclassage vers une nouvelle
 * classe dont les prérequis SRD sont remplis), aptitudes atteintes à ce niveau dans
 * cette classe (lues depuis classes_srd521.md via ClasseParser — voir AptitudeClasse.type
 * dans Srdcreationparsers.kt), choix de la sous-classe (SRD + sous-classes importées) quand
 * l'aptitude "choix-sousclasse" est atteinte, puis aptitudes et sorts de cette sous-classe, et
 * répartition des points d'Amélioration de caractéristique quand l'aptitude
 * "choix-generique" est atteinte (+2 à une caractéristique, ou +1 à deux, plafonné à 20).
 * Si la classe principale n'est pas reconnue dans le SRD (classe personnalisée, monde
 * Naheulbeuk...), seuls les points de vie sont proposés — le dialogue reste utilisable
 * dans tous les cas, sans piste ni aptitude.
 */
@Composable
private fun LevelUpDialog(character: Character, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var classesSrd by remember(character.worldId) { mutableStateOf<List<Classe>>(emptyList()) }
    var chargementTermine by remember(character.worldId) { mutableStateOf(false) }
    // Espèce du personnage (balises de especes_srd521.md) : gains et choix propres au nouveau niveau.
    var especeSrd by remember(character.worldId, character.race) { mutableStateOf<Espece?>(null) }
    // Options des choix balisés (ex. instrument du multiclassage Barde : liste du fichier équipement).
    var contexteChoix by remember(character.worldId) { mutableStateOf(ContexteChoix()) }
    // Sorts du SRD et table des emplacements : choix des sorts d'un nouvel emplacement de préparation.
    var sortsSrd by remember(character.worldId) { mutableStateOf<List<SrdEntry>>(emptyList()) }
    var tableEmplacementsSrd by remember(character.worldId) { mutableStateOf<TableMd?>(null) }
    // Dons du SRD : choix d'un don par une aptitude (Faveur épique, balise « don-categorie: »).
    var donsSrd by remember(character.worldId) { mutableStateOf<List<Don>>(emptyList()) }
    LaunchedEffect(character.worldId) {
        val monde = character.worldId.ifBlank { "donjon_et_dragon" }
        val classesMd = SrdRepository.loadClasses(context, monde).joinToString("\n\n") { it.rawMarkdown }
        classesSrd = ClasseParser.parse(classesMd)
        tableEmplacementsSrd = EmplacementsDeSortParser.parse(classesMd)
        sortsSrd = SrdRepository.loadSpells(context, monde)
        donsSrd = DonParser.depuisEntrees(SrdRepository.loadDons(context, monde).map { it.name to it.rawMarkdown })
        especeSrd = EspeceParser.parse(SrdRepository.loadEspeces(context, monde).joinToString("\n\n") { it.rawMarkdown })
            .firstOrNull { it.nom.equals(character.race.trim(), ignoreCase = true) }
        contexteChoix = ContexteChoix(
            equipements = SrdRepository.loadEquipmentList(context, monde).map { it.name to it.category },
            langues = LangueParser.parse(SrdRepository.loadLangues(context, monde)).map { it.nom },
        )
        chargementTermine = true
    }

    fun valeurCaracteristique(car: Caracteristique): Int = when (car) {
        Caracteristique.FORCE -> character.strength
        Caracteristique.DEXTERITE -> character.dexterity
        Caracteristique.CONSTITUTION -> character.constitution
        Caracteristique.INTELLIGENCE -> character.intelligence
        Caracteristique.SAGESSE -> character.wisdom
        Caracteristique.CHARISME -> character.charisma
    }

    val classePrincipale = classesSrd.firstOrNull { it.nom.equals(character.characterClass, ignoreCase = true) }
    val niveauPrincipalActuel = character.level - character.classesSecondaires.sumOf { it.niveau }

    val pistesExistantes = buildList {
        classePrincipale?.let { add(PisteNiveau(it, niveauPrincipalActuel, estNouvelle = false, estPrincipale = true)) }
        character.classesSecondaires.forEach { nc ->
            classesSrd.firstOrNull { it.nom.equals(nc.classe, ignoreCase = true) }
                ?.let { add(PisteNiveau(it, nc.niveau, estNouvelle = false, estPrincipale = false)) }
        }
    }
    // Multiclasser vers une classe pas encore prise exige de remplir le prérequis de
    // TOUTES les classes déjà en cours, en plus de celui de la nouvelle classe (règle SRD).
    val remplitTousPrerequisActuels = pistesExistantes.all { it.classe.remplitPrerequisMulticlasse(::valeurCaracteristique) }
    // Le multiclassage n'est proposé que si la classe principale est elle-même reconnue
    // dans le SRD : impossible sinon de vérifier son propre prérequis (règle SRD : il faut
    // remplir le prérequis de toutes les classes déjà en cours pour en prendre une nouvelle).
    val nouvellesPistes = if (classePrincipale != null && remplitTousPrerequisActuels) {
        classesSrd.filter { c -> pistesExistantes.none { it.classe.nom == c.nom } && c.remplitPrerequisMulticlasse(::valeurCaracteristique) }
            .map { PisteNiveau(it, 0, estNouvelle = true, estPrincipale = false) }
    } else emptyList()
    val pistesDisponibles = pistesExistantes + nouvellesPistes

    var pisteChoisieNom by remember { mutableStateOf<String?>(null) }
    val pisteActive = pistesDisponibles.firstOrNull { it.classe.nom == pisteChoisieNom } ?: pistesDisponibles.firstOrNull()

    val nouveauNiveauTotal = character.level + 1
    val nouveauNiveauDansClasse = (pisteActive?.niveauActuel ?: character.level) + 1
    val hitDieFaces = pisteActive?.classe?.deDeVie?.removePrefix("d")?.toIntOrNull() ?: hitDieForClass(character.characterClass)
    val modConstitution = GameState.abilityModifier(character.constitution)
    val pvFixe = (hitDieFaces / 2 + 1 + modConstitution).coerceAtLeast(1)

    var lanceLeDe by remember { mutableStateOf(false) }
    var jetDe by remember(pisteActive, hitDieFaces) { mutableStateOf<Int?>(null) }
    val pvGagnes = if (lanceLeDe) ((jetDe ?: (hitDieFaces / 2 + 1)) + modConstitution).coerceAtLeast(1) else pvFixe

    // Multiclassage : premier niveau dans une classe pas encore prise → gains RÉDUITS par
    // rapport à une classe de départ (cf. Classe.multiclassage, section "Multiclassage" de
    // classes_srd521.md), pas les Traits de base complets. Les aptitudes de niveau 1
    // elles-mêmes restent identiques (le SRD le précise), déjà couvertes par aptitudesGagnees.
    val infosMulticlasse = pisteActive?.classe?.multiclassage?.takeIf { pisteActive.estNouvelle }
    val optionsCompetenceMulticlasse = infosMulticlasse?.competence?.let { texte ->
        if (texte.contains("parmi", ignoreCase = true)) {
            parseCompetencesClasse(pisteActive!!.classe.maitrisesCompetence).optionsFixes ?: TOUTES_COMPETENCES
        } else TOUTES_COMPETENCES
    }?.filterNot { it in character.skillExpertise } // déjà maîtrisée : proposée, elle passera en Expertise
    var competenceMulticlasseChoisie by remember(pisteActive) { mutableStateOf<String?>(null) }
    // Choix balisés propres au multiclassage (section "Multiclassage", ex. instrument du Barde).
    val choixMulticlasse = if (infosMulticlasse != null) {
        pisteActive?.classe?.let { c ->
            c.balisesMulticlasse.filter { it.containsKey("choix") }
                .mapNotNull { b -> construireChoix(b, "Multiclassage ${c.nom}", "Maîtrise d'outils du multiclassage : ${infosMulticlasse.outils.orEmpty()}", contexteChoix) }
        }.orEmpty()
    } else emptyList()
    var choixMulticlasseFaits by remember(pisteActive) { mutableStateOf(mapOf<String, List<String>>()) }
    val choixMulticlasseComplet = choixMulticlasse.all { c -> choixMulticlasseFaits[c.id].orEmpty().size == c.nombre }
    val outilsMulticlasse = choixMulticlasse.filter { it.effet == EffetBalise.OUTILS }
        .flatMap { choixMulticlasseFaits[it.id].orEmpty() }
        .takeIf { it.isNotEmpty() }?.joinToString(", ")
    val choixCompetenceMulticlasseComplet = optionsCompetenceMulticlasse == null || competenceMulticlasseChoisie != null

    // Sous-classe : choisie parmi celles de la classe (SRD + importées) quand l'aptitude
    // "choix-sousclasse" est atteinte, ou plus tard si elle n'a jamais été choisie. Sur la
    // classe principale, elle se fixe dans le champ dédié `subclass` ; sur une classe
    // secondaire, `subclass` reste réservé à la classe principale, la sous-classe secondaire
    // n'est donc que mentionnée dans classFeatures ("Sous-classe (Classe) : Nom").
    val sousClassesDispo = pisteActive?.classe?.sousClasses.orEmpty()
    val niveauChoixSousClasse = pisteActive?.classe?.aptitudes.orEmpty()
        .firstOrNull { it.type == "choix-sousclasse" }?.niveaux?.minOrNull()
    val sousClasseActuelleNom = pisteActive?.let { piste ->
        if (piste.estPrincipale) character.subclass.ifBlank { null }
        else Regex("""Sous-classe \(${Regex.escape(piste.classe.nom)}\) : (.+)""").find(character.classFeatures)?.groupValues?.get(1)?.trim()
    }
    val doitChoisirSousClasse = sousClasseActuelleNom == null && sousClassesDispo.isNotEmpty() &&
        niveauChoixSousClasse != null && nouveauNiveauDansClasse >= niveauChoixSousClasse
    var sousClasseChoisieNom by remember(pisteActive) { mutableStateOf(sousClassesDispo.singleOrNull()?.nom) }
    // Choix en deux temps : on retient d'abord la sous-classe (résumé du style de jeu), puis
    // « Suivant » affiche ses aptitudes et leurs choix (compétences, sorts...).
    var sousClasseValidee by remember(pisteActive) { mutableStateOf(false) }
    val etapeChoixSousClasse = doitChoisirSousClasse && !sousClasseValidee
    val sousClasseRetenue = if (doitChoisirSousClasse) sousClassesDispo.firstOrNull { it.nom == sousClasseChoisieNom }
    else sousClassesDispo.firstOrNull { it.nom.equals(sousClasseActuelleNom, ignoreCase = true) }
    val choixSousClasseComplet = !doitChoisirSousClasse || (sousClasseRetenue != null && sousClasseValidee)
    // Sous-classe dont les aptitudes s'appliquent : seulement une fois validée.
    val sousClasseAppliquee = sousClasseRetenue?.takeIf { !etapeChoixSousClasse }
    // Aptitudes de sous-classe de ce niveau ; au choix de la sous-classe, aussi celles des
    // niveaux déjà dépassés (sous-classe choisie en retard).
    val aptitudesSousClasse = sousClasseAppliquee?.aptitudes.orEmpty().filter { apt ->
        if (doitChoisirSousClasse) apt.niveaux.any { it <= nouveauNiveauDansClasse } else nouveauNiveauDansClasse in apt.niveaux
    }.map { it.copy(nom = "${it.nom} (${sousClasseAppliquee?.nom})") }
    // Sorts toujours préparés de la sous-classe accessibles au nouveau niveau, pas encore connus.
    val sortsSousClasseGagnes = sousClasseAppliquee?.sortsJusquAu(nouveauNiveauDansClasse).orEmpty()
        .filter { s -> !ArsenalPersonnage.sortConnu(character, s) }

    val aptitudesGagnees = pisteActive?.classe?.aptitudes.orEmpty()
        .filter { nouveauNiveauDansClasse in it.niveaux && !(it.type == "choix-sousclasse" && sousClassesDispo.isNotEmpty()) } +
        aptitudesSousClasse
    val aptitudeAsi = aptitudesGagnees.firstOrNull { it.type == "choix-generique" }

    val abilities = listOf(
        "FOR" to character.strength, "DEX" to character.dexterity, "CON" to character.constitution,
        "INT" to character.intelligence, "SAG" to character.wisdom, "CHA" to character.charisma
    )
    var pointsAssignes by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf(mapOf<String, Int>()) }
    val totalAssigne = pointsAssignes.values.sum()
    val choixAsiComplet = aptitudeAsi == null || totalAssigne == 2

    // Aptitudes à choix intégré gagnées à ce niveau (ex. Impacts bénis du Clerc au niveau 7 :
    // Impact divin ou Incantation puissante) : une option retenue par aptitude, obligatoire
    // pour confirmer — cf. AptitudeClasse.type == "choix-effet" et ses [AptitudeClasse.options].
    val aptitudesChoixEffet = aptitudesGagnees.filter { it.type == "choix-effet" }
    var optionsChoisiesEffet by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf(mapOf<String, String>()) }
    val choixEffetComplet = aptitudesChoixEffet.all { apt -> optionsChoisiesEffet.containsKey(apt.id ?: apt.nom) }

    // Aptitudes "Expertise"/"Fin explorateur" (type: choix-expertise-N) : choix de N
    // compétences déjà maîtrisées à passer en Expertise (bonus doublé), cf.
    // GameState.skillExpertise. Le nombre N est lu dans le suffixe du type.
    val aptitudesChoixExpertise = aptitudesGagnees.filter { it.type.startsWith("choix-expertise") }
    var expertiseChoisies by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf(mapOf<String, Set<String>>()) }
    val competencesDejaExpertes = character.skillExpertise.toSet()
    // Compétences maîtrisées éligibles, restreintes le cas échéant par une balise
    // "competences: A, B" de l'aptitude (ex. Érudition du Magicien).
    fun eligiblesExpertise(apt: AptitudeClasse): List<String> {
        val restriction = apt.balises.firstNotNullOfOrNull { it["competences"] }
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        return character.skillProficiencies.filter { s ->
            s !in competencesDejaExpertes && (restriction == null || restriction.any { it.equals(s, ignoreCase = true) })
        }
    }
    val choixExpertiseComplet = aptitudesChoixExpertise.all { apt ->
        val cle = apt.id ?: apt.nom
        val count = apt.type.substringAfterLast("-").toIntOrNull() ?: 1
        val eligibles = eligiblesExpertise(apt).size
        val requis = count.coerceAtMost(eligibles)
        expertiseChoisies[cle].orEmpty().size >= requis
    }

    // Nouvel emplacement de sort préparé (colonne « Sorts préparés » de la table de progression,
    // ex. Paladin 2 → 3 au niveau 2) : un sort de la classe par emplacement gagné, d'un niveau
    // ne dépassant pas le plus haut emplacement de sort disponible au nouveau niveau.
    val classeSorts = pisteActive?.classe
    val nouveauxEmplacementsPrepares = classeSorts?.takeIf { it.typeIncantation != "aucun" }?.let { c ->
        fun prepares(n: Int) = c.tableProgression?.valeurNiveau(n.toString(), "Sorts préparés")?.toIntOrNull() ?: 0
        (prepares(nouveauNiveauDansClasse) - prepares(nouveauNiveauDansClasse - 1)).coerceAtLeast(0)
    } ?: 0
    // Classe à grimoire (Magicien, balise « grimoire-depart / grimoire-par-niveau » de son aptitude
    // Sorts) : les sorts appris vont au grimoire, indépendamment des sorts préparés.
    val baliseGrimoire = classeSorts?.aptitudes.orEmpty().flatMap { it.balises }.firstOrNull { it.containsKey("grimoire-par-niveau") }
    val nombreNouveauxSorts = baliseGrimoire?.let { b ->
        (if (nouveauNiveauDansClasse == 1) b["grimoire-depart"] else b["grimoire-par-niveau"])?.trim()?.toIntOrNull()
    } ?: nouveauxEmplacementsPrepares
    val niveauMaxNouveauSort = classeSorts?.let { c ->
        when (c.typeIncantation) {
            "complet" -> tableEmplacementsSrd?.plusHautNiveauAvecEmplacement(nouveauNiveauDansClasse) ?: 0
            "demi" -> tableEmplacementsSrd?.plusHautNiveauAvecEmplacement((nouveauNiveauDansClasse + 1) / 2) ?: 0
            "pacte" -> c.tableProgression?.valeurNiveau(nouveauNiveauDansClasse.toString(), "Niveau des emplacements")?.toIntOrNull() ?: 0
            else -> 0
        }
    } ?: 0
    val sortsCandidats = remember(sortsSrd, classeSorts, niveauMaxNouveauSort, character.spells, sortsSousClasseGagnes) {
        if (classeSorts == null || nombreNouveauxSorts == 0) emptyList()
        else sortsSrd.filter { s ->
            val niveauSort = ArsenalPersonnage.niveauDepuisLibelle(s.niveauSort)
            // Seulement des sorts pas encore acquis (ni gagnés d'office à ce niveau).
            s.enseignePar(classeSorts.nom) && niveauSort in 1..niveauMaxNouveauSort &&
                !ArsenalPersonnage.sortConnu(character, s.name) &&
                sortsSousClasseGagnes.none { ArsenalPersonnage.memeSort(it, s.name) } &&
                !ArsenalPersonnage.memeSort(s.name, ArsenalPersonnage.SORT_CHATIMENT_DIVIN)
        }.distinctBy { it.name.lowercase() }
            .sortedWith(compareBy({ ArsenalPersonnage.niveauDepuisLibelle(it.niveauSort) }, { it.name }))
    }
    // Option d'aptitude qui donne des sorts mineurs d'une autre liste (Paladin, Style de combat →
    // Combattant béni : « deux sorts mineurs de Clerc ») : (aptitude, nombre, liste de sorts).
    val sortsMineursOption = aptitudesChoixEffet.firstNotNullOfOrNull { apt ->
        val option = apt.options.firstOrNull { it.nom == optionsChoisiesEffet[apt.id ?: apt.nom] } ?: return@firstNotNullOfOrNull null
        REGEX_SORTS_MINEURS_OPTION.find(option.description)?.let { m ->
            val nombre = m.groupValues[1].toIntOrNull() ?: NOMBRES_EN_LETTRES[m.groupValues[1].lowercase()] ?: return@let null
            Triple(apt, nombre, m.groupValues[2])
        }
    }
    val mineursCandidats = remember(sortsSrd, sortsMineursOption, character.spells) {
        sortsMineursOption?.let { (_, _, liste) ->
            sortsSrd.filter { s ->
                ArsenalPersonnage.niveauDepuisLibelle(s.niveauSort) == 0 && s.enseignePar(liste) &&
                    !ArsenalPersonnage.sortConnu(character, s.name)
            }.distinctBy { it.name.lowercase() }.sortedBy { it.name }
        }.orEmpty()
    }
    val mineursRequis = (sortsMineursOption?.second ?: 0).coerceAtMost(mineursCandidats.size)
    var mineursChoisis by remember(sortsMineursOption) { mutableStateOf(setOf<String>()) }
    val choixMineursComplet = mineursChoisis.size == mineursRequis
    val sortsRequis = nombreNouveauxSorts.coerceAtMost(sortsCandidats.size)
    var nouveauxSortsChoisis by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf(setOf<String>()) }
    val choixSortsComplet = nouveauxSortsChoisis.size == sortsRequis
    val filtreNouveauxSorts = rememberFiltreSorts(pisteActive, nouveauNiveauDansClasse)
    val filtreMineurs = rememberFiltreSorts(sortsMineursOption)

    // Sorts d'une école offerts par une aptitude (ex. Savant en évocation de l'Évocateur) :
    // balise "choix: N; effet: sorts; ecole: X; niveau-max: M", sorts de la classe qui progresse.
    val choixSortsEcole = aptitudesGagnees.flatMap { apt ->
        apt.balises.filter { it.containsKey("choix") && it["effet"] == EffetBalise.SORTS && it.containsKey("ecole") }.map { apt to it }
    }
    var sortsEcoleChoisis by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf(mapOf<String, Set<String>>()) }
    val candidatsSortsEcole = remember(sortsSrd, choixSortsEcole, character.spells, nouveauxSortsChoisis) {
        choixSortsEcole.associate { (apt, b) ->
            val niveauMax = b["niveau-max"]?.toIntOrNull() ?: 9
            (b["id"] ?: apt.nom) to sortsSrd.filter { s ->
                val niveauSort = ArsenalPersonnage.niveauDepuisLibelle(s.niveauSort)
                (classeSorts == null || s.enseignePar(classeSorts.nom)) && niveauSort in 1..niveauMax &&
                    s.category.equals(b["ecole"], ignoreCase = true) &&
                    !ArsenalPersonnage.sortConnu(character, s.name) && s.name !in nouveauxSortsChoisis
            }.distinctBy { it.name.lowercase() }
                .sortedWith(compareBy({ ArsenalPersonnage.niveauDepuisLibelle(it.niveauSort) }, { it.name }))
        }
    }
    fun requisSortsEcole(cle: String, b: Map<String, String>): Int =
        (b["choix"]?.toIntOrNull() ?: 1).coerceAtMost(candidatsSortsEcole[cle].orEmpty().size)
    val choixSortsEcoleComplet = choixSortsEcole.all { (apt, b) ->
        val cle = b["id"] ?: apt.nom
        sortsEcoleChoisis[cle].orEmpty().size == requisSortsEcole(cle, b)
    }
    val filtreSortsEcole = rememberFiltreSorts(pisteActive, nouveauNiveauDansClasse)

    // Sorts lançables sans emplacement (balise « sorts-speciaux: MODE; niveaux: 1, 2[; incantation: Action] »,
    // ex. Maîtrise des sorts, Sorts de prédilection du Magicien) : un sort du grimoire par niveau listé.
    data class EmplacementSortSpecial(val cle: String, val apt: AptitudeClasse, val mode: String, val niveau: Int, val incantation: String?)
    val emplacementsSpeciaux = aptitudesGagnees.flatMap { apt ->
        apt.balises.filter { it.containsKey("sorts-speciaux") }.flatMap { b ->
            b["niveaux"].orEmpty().split(",").mapIndexedNotNull { i, n ->
                n.trim().toIntOrNull()?.let { EmplacementSortSpecial("${b["id"] ?: apt.nom}#$i", apt, b["sorts-speciaux"]!!.trim(), it, b["incantation"]?.trim()) }
            }
        }
    }
    var sortsSpeciauxChoisis by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf(mapOf<String, String>()) }
    // Grimoire de la classe qui progresse (sorts déjà appris + ceux choisis à ce niveau).
    val grimoire = character.spells.filter { s -> (character.spellClasses[s] ?: character.characterClass).equals(classeSorts?.nom, ignoreCase = true) } +
        nouveauxSortsChoisis
    fun candidatsSpeciaux(e: EmplacementSortSpecial): List<SrdEntry> = sortsSrd.filter { s ->
        grimoire.any { ArsenalPersonnage.memeSort(it, s.name) } &&
            ArsenalPersonnage.niveauDepuisLibelle(s.niveauSort) == e.niveau &&
            (e.incantation == null || s.tempsIncantation.startsWith(e.incantation, ignoreCase = true)) &&
            character.sortsSpeciaux.keys.none { ArsenalPersonnage.memeSort(it, s.name) } &&
            sortsSpeciauxChoisis.none { (cle, nom) -> cle != e.cle && nom == s.name }
    }.distinctBy { it.name.lowercase() }.sortedBy { it.name }
    val choixSpeciauxComplet = emplacementsSpeciaux.all { e -> sortsSpeciauxChoisis.containsKey(e.cle) || candidatsSpeciaux(e).isEmpty() }

    // Don choisi par une aptitude (Faveur épique : balise « don-categorie: Faveur épique »), avec son
    // augmentation de caractéristique (« Intelligence, Sagesse ou Charisme +1 (max 30) »).
    val aptitudeDon = aptitudesGagnees.firstNotNullOfOrNull { apt ->
        apt.balises.firstNotNullOfOrNull { it["don-categorie"] }?.let { apt to it.trim() }
    }
    val donsCandidats = aptitudeDon?.let { (_, categorie) ->
        donsSrd.filter { d -> d.categorie.equals(categorie, ignoreCase = true) && !character.feats.contains(d.nom, ignoreCase = true) }
    }.orEmpty()
    var donChoisiNom by remember(pisteActive, nouveauNiveauDansClasse) { mutableStateOf<String?>(null) }
    val donChoisi = donsCandidats.firstOrNull { it.nom == donChoisiNom }
    val augmentationDon = donChoisi?.let { augmentationDeDon(it) }
    var caracDon by remember(donChoisiNom) { mutableStateOf<String?>(null) }
    val choixDonComplet = aptitudeDon == null || donsCandidats.isEmpty() ||
        (donChoisi != null && (augmentationDon == null || augmentationDon.options.isEmpty() || caracDon != null))

    // Autres choix balisés des aptitudes gagnées (ex. Maîtrises supplémentaires du Collège du
    // Savoir : « choix: 3; effet: competences »), cf. construireChoix.
    val choixAptitudes = aptitudesGagnees.flatMap { apt ->
        apt.balises.filter { it.containsKey("choix") && !it.containsKey("ecole") }
            .mapNotNull { b -> construireChoix(b, apt.nom, apt.description, contexteChoix)?.let { apt to it } }
    }
    var choixAptitudesFaits by remember(pisteActive, nouveauNiveauDansClasse, sousClasseAppliquee) { mutableStateOf(mapOf<String, List<String>>()) }
    val choixAptitudesComplet = choixAptitudes.all { (_, c) -> choixAptitudesFaits[c.id].orEmpty().size == c.nombre }
    val competencesAptitudes = choixAptitudes.filter { it.second.effet == EffetBalise.COMPETENCES }
        .flatMap { (_, c) -> choixAptitudesFaits[c.id].orEmpty() }

    // Espèce : traits qui s'activent au nouveau niveau de personnage (balise "niveau:"), choix
    // balisés de ce niveau, et gains déduits des choix déjà faits (ex. sort de lignage au
    // niveau 3). Le niveau d'espèce suit le niveau TOTAL du personnage, pas celui de la classe.
    val especeCourante = especeSrd
    val traitsEspeceNiveau = especeCourante?.traits.orEmpty().filter { !it.base && it.niveau == nouveauNiveauTotal }
    val choixEspeceNiveau = especeCourante?.let { choixEspece(it, emptyList()) }.orEmpty().filter { it.niveau == nouveauNiveauTotal }
    var choixEspeceFaits by remember(especeCourante, nouveauNiveauTotal) { mutableStateOf(mapOf<String, List<String>>()) }
    val choixEspeceComplet = choixEspeceNiveau.all { c -> choixEspeceFaits[c.id].orEmpty().size == c.nombre }
    val effetsAvant = especeCourante?.let { effetsEspece(it, character.speciesChoices, character.level) }
    val effetsApres = especeCourante?.let { effetsEspece(it, character.speciesChoices + choixEspeceFaits, nouveauNiveauTotal) }
    val sortsEspeceGagnes = effetsApres?.sorts.orEmpty() - effetsAvant?.sorts.orEmpty().toSet() - character.spells.toSet()
    val resistancesEspeceGagnees = effetsApres?.resistances.orEmpty() - character.damageResistances.toSet()
    val competencesEspeceGagnees = effetsApres?.competences.orEmpty() - character.skillProficiencies.toSet()
    val pvEspece = effetsApres?.pvParNiveau ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Niveau $nouveauNiveauTotal !") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!chargementTermine) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    return@Column
                }

                if (pistesExistantes.size > 1 || nouvellesPistes.isNotEmpty()) {
                    Text("Classe à faire progresser :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    // Classes déjà en cours : sélectionnables directement.
                    pistesExistantes.forEach { piste ->
                        val selectionnee = piste == pisteActive
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pisteChoisieNom = piste.classe.nom },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectionnee, onClick = { pisteChoisieNom = piste.classe.nom })
                            Text(
                                piste.classe.nom + if (piste.estPrincipale) {
                                    " (niveau ${piste.niveauActuel + 1})"
                                } else {
                                    " (secondaire, niveau ${piste.niveauActuel + 1})"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    // Nouvelles classes (multiclassage) : regroupées derrière une seule option
                    // "Multiclassage" au lieu de lister toutes les classes accessibles en vrac —
                    // cliquer dessus déplie la liste des classes disponibles pour choisir.
                    if (nouvellesPistes.isNotEmpty()) {
                        val multiclasseSelectionnee = pisteActive?.estNouvelle == true
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!multiclasseSelectionnee) pisteChoisieNom = nouvellesPistes.first().classe.nom
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = multiclasseSelectionnee,
                                onClick = { pisteChoisieNom = nouvellesPistes.first().classe.nom }
                            )
                            Text("Multiclassage", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        if (multiclasseSelectionnee) {
                            Column(modifier = Modifier.padding(start = 32.dp)) {
                                nouvellesPistes.forEach { piste ->
                                    val selectionnee = piste == pisteActive
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { pisteChoisieNom = piste.classe.nom },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(selected = selectionnee, onClick = { pisteChoisieNom = piste.classe.nom })
                                        Text(piste.classe.nom + " (multiclasse, niveau 1)", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                }

                Text("Points de vie", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PvModeButton(
                        label = "Fixe (+$pvFixe)",
                        selectionne = !lanceLeDe,
                        onClick = { lanceLeDe = false }
                    )
                    PvModeButton(
                        label = "Dé (D$hitDieFaces)",
                        selectionne = lanceLeDe,
                        onClick = { lanceLeDe = true; if (jetDe == null) jetDe = (1..hitDieFaces).random() }
                    )
                }
                if (lanceLeDe) {
                    // Un seul jet autorisé par montée de niveau (pas de relance) : jetDe est
                    // tiré une fois au passage en mode "Dé" (ci-dessus) et ne change plus tant
                    // que le dialogue reste ouvert pour ce niveau.
                    Text("Jet : ${jetDe ?: "—"} + mod. Constitution $modConstitution = +$pvGagnes", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        "D$hitDieFaces/2 + 1 + mod. Constitution $modConstitution = +$pvGagnes",
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetTextSecondary
                    )
                }
                if (pvEspece > 0) {
                    Text(
                        "+$pvEspece PV d'espèce (${especeCourante?.nom})",
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetTextSecondary
                    )
                }
                Text(
                    "Nouveau maximum : ${character.maxHitPoints + pvGagnes + pvEspece}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary
                )

                // Espèce : rappel des traits de ce niveau, gains automatiques et choix à faire.
                if (traitsEspeceNiveau.isNotEmpty() || choixEspeceNiveau.isNotEmpty() || sortsEspeceGagnes.isNotEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text("Espèce : ${especeCourante?.nom}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    traitsEspeceNiveau.forEach { t ->
                        Text("${t.nom} — disponible dès ce niveau", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(t.description, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                    }
                    if (sortsEspeceGagnes.isNotEmpty()) {
                        Text(
                            "Sort(s) appris grâce à votre espèce : ${sortsEspeceGagnes.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    choixEspeceNiveau.forEach { c ->
                        ChoixBaliseCarte(
                            choix = c,
                            selection = choixEspeceFaits[c.id].orEmpty(),
                            onSelectionChange = { choixEspeceFaits = choixEspeceFaits + (c.id to it) },
                            // Compétence déjà maîtrisée : la reprendre la passe en Expertise ; déjà experte : rien à gagner.
                            indisponibles = if (c.effet == EffetBalise.COMPETENCES) character.skillExpertise.toSet() else emptySet(),
                            versExpertise = if (c.effet == EffetBalise.COMPETENCES) character.skillProficiencies.toSet() else emptySet(),
                        )
                    }
                }

                if (infosMulticlasse != null) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "Multiclassage : ${pisteActive?.classe?.nom}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val maitrisesAccordees = listOfNotNull(
                        infosMulticlasse.armures?.let { "Armures : $it" },
                        infosMulticlasse.armes?.let { "Armes : $it" },
                        infosMulticlasse.outils?.let { "Outils : $it" }
                    )
                    Text(
                        if (maitrisesAccordees.isEmpty()) "Aucune maîtrise d'armure/arme/outil supplémentaire."
                        else maitrisesAccordees.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetTextSecondary
                    )
                    if (pisteActive?.classe?.estLanceurDeSorts == true) {
                        Text(
                            "Emplacements de sort : soumis aux règles de multiclassage (non calculés automatiquement ici).",
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetTextSecondary
                        )
                    }
                    choixMulticlasse.forEach { c ->
                        ChoixBaliseCarte(
                            choix = c,
                            selection = choixMulticlasseFaits[c.id].orEmpty(),
                            onSelectionChange = { choixMulticlasseFaits = choixMulticlasseFaits + (c.id to it) },
                        )
                    }
                    if (optionsCompetenceMulticlasse != null) {
                        Text(
                            "${infosMulticlasse.competence} :",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                optionsCompetenceMulticlasse.forEach { competence ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { competenceMulticlasseChoisie = competence },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = competenceMulticlasseChoisie == competence,
                                            onClick = { competenceMulticlasseChoisie = competence }
                                        )
                                        Text(
                                            if (competence in character.skillProficiencies) "$competence (déjà maîtrisée → Expertise)" else competence,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (etapeChoixSousClasse) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "Sous-classe de ${pisteActive?.classe?.nom} — choisissez-en une :",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    sousClassesDispo.forEach { sc ->
                        val selectionnee = sc.nom == sousClasseChoisieNom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { sousClasseChoisieNom = sc.nom },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectionnee, onClick = { sousClasseChoisieNom = sc.nom })
                            Column {
                                Text(sc.nom, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                // Seulement le résumé du style de jeu ; les aptitudes viennent à l'étape suivante.
                                val resume = sc.description.ifBlank { sc.aptitudes.joinToString(", ") { it.nom } }
                                if (resume.isNotBlank()) {
                                    Text(
                                        markdownEnLigne(resume.lineSequence().map { it.trim().removePrefix("- ") }.filter { it.isNotBlank() }.joinToString("\n")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SheetTextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Touchez « Suivant » pour voir les aptitudes et faire les choix de la sous-classe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetTextSecondary
                    )
                } else if (doitChoisirSousClasse && sousClasseRetenue != null) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Sous-classe : ${sousClasseRetenue.nom}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { sousClasseValidee = false }) { Text("Changer") }
                    }
                }

                if (aptitudesGagnees.isEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "Aucune nouvelle aptitude de classe répertoriée à ce niveau.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetTextSecondary
                    )
                } else {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    aptitudesGagnees.forEach { apt ->
                        Column {
                            Text(apt.nom, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            if (apt.description.isNotBlank() && apt.type == "automatique") {
                                Text(apt.description, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                            }
                            if (apt.type == "choix-effet") {
                                val cle = apt.id ?: apt.nom
                                if (apt.description.isNotBlank()) {
                                    Text(apt.description, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                                }
                                apt.options.forEach { option ->
                                    val selectionnee = optionsChoisiesEffet[cle] == option.nom
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { optionsChoisiesEffet = optionsChoisiesEffet + (cle to option.nom) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selectionnee,
                                            onClick = { optionsChoisiesEffet = optionsChoisiesEffet + (cle to option.nom) }
                                        )
                                        Column {
                                            Text(option.nom, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text(option.description, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                                        }
                                    }
                                }
                            }
                            if (apt.type.startsWith("choix-expertise")) {
                                val cle = apt.id ?: apt.nom
                                val count = apt.type.substringAfterLast("-").toIntOrNull() ?: 1
                                val eligibles = eligiblesExpertise(apt)
                                val choisies = expertiseChoisies[cle].orEmpty()
                                val requis = count.coerceAtMost(eligibles.size)
                                if (apt.description.isNotBlank()) {
                                    Text(apt.description, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                                }
                                Text(
                                    "Choisissez $requis compétence${if (requis > 1) "s" else ""} maîtrisée${if (requis > 1) "s" else ""} en Expertise (${choisies.size}/$requis) :",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (eligibles.isEmpty()) {
                                    Text(
                                        "Aucune compétence maîtrisée disponible pour l'Expertise.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SheetTextSecondary
                                    )
                                } else {
                                    eligibles.forEach { skill ->
                                        val selectionnee = skill in choisies
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val nouvelles = when {
                                                        selectionnee -> choisies - skill
                                                        choisies.size < requis -> choisies + skill
                                                        else -> choisies
                                                    }
                                                    expertiseChoisies = expertiseChoisies + (cle to nouvelles)
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectionnee,
                                                onCheckedChange = { coche ->
                                                    val nouvelles = when {
                                                        coche && choisies.size < requis -> choisies + skill
                                                        !coche -> choisies - skill
                                                        else -> choisies
                                                    }
                                                    expertiseChoisies = expertiseChoisies + (cle to nouvelles)
                                                }
                                            )
                                            Text(skill, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (choixAptitudes.isNotEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    choixAptitudes.forEach { (_, c) ->
                        ChoixBaliseCarte(
                            choix = c,
                            selection = choixAptitudesFaits[c.id].orEmpty(),
                            onSelectionChange = { choixAptitudesFaits = choixAptitudesFaits + (c.id to it) },
                            indisponibles = if (c.effet == EffetBalise.COMPETENCES) character.skillExpertise.toSet() else emptySet(),
                            versExpertise = if (c.effet == EffetBalise.COMPETENCES) {
                                (character.skillProficiencies + listOfNotNull(competenceMulticlasseChoisie) + competencesEspeceGagnees).toSet()
                            } else emptySet(),
                        )
                    }
                }

                if (sortsSousClasseGagnes.isNotEmpty()) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "Sorts toujours préparés (${sousClasseRetenue?.nom}) : ${sortsSousClasseGagnes.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                choixSortsEcole.forEach { (apt, b) ->
                    val cle = b["id"] ?: apt.nom
                    val requis = requisSortsEcole(cle, b)
                    val choisis = sortsEcoleChoisis[cle].orEmpty()
                    val candidats = candidatsSortsEcole[cle].orEmpty()
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "${apt.nom} — sorts ${b["ecole"].orEmpty()} de niveau ${b["niveau-max"] ?: "9"} au plus : " +
                            "choisissez-en $requis (${choisis.size}/$requis)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (candidats.isEmpty()) {
                        Text("Aucun sort disponible dans la bibliothèque.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                    } else {
                        FiltreSortsBarre(filtreSortsEcole, candidats)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtreSortsEcole.appliquer(candidats).forEach { sort ->
                            val coche = sort.name in choisis
                            SortApercuCarte(
                                sort = sort,
                                selectionne = coche,
                                selectionPossible = choisis.size < requis,
                                onSelection = {
                                    val nouveaux = when {
                                        coche -> choisis - sort.name
                                        choisis.size < requis -> choisis + sort.name
                                        else -> choisis
                                    }
                                    sortsEcoleChoisis = sortsEcoleChoisis + (cle to nouveaux)
                                }
                            )
                        }
                    }
                }

                if (aptitudeAsi != null) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "${aptitudeAsi.nom} — répartissez 2 points ($totalAssigne/2), max +2 sur une " +
                            "caractéristique ou +1 sur deux, plafonné à 20.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    abilities.forEach { (abrev, valeurActuelle) ->
                        val assigne = pointsAssignes[abrev] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "$abrev ${abilitySaveName(abrev)} : $valeurActuelle" +
                                    if (assigne > 0) " → ${valeurActuelle + assigne}" else "",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row {
                                TextButton(
                                    onClick = { pointsAssignes = pointsAssignes + (abrev to (assigne - 1)) },
                                    enabled = assigne > 0
                                ) { Text("−") }
                                TextButton(
                                    onClick = { pointsAssignes = pointsAssignes + (abrev to (assigne + 1)) },
                                    enabled = totalAssigne < 2 && assigne < 2 && valeurActuelle + assigne < 20
                                ) { Text("+") }
                            }
                        }
                    }
                }

                sortsMineursOption?.let { (apt, _, liste) ->
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "${apt.nom} — sorts mineurs de $liste : choisissez-en $mineursRequis (${mineursChoisis.size}/$mineursRequis)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    FiltreSortsBarre(filtreMineurs, mineursCandidats)
                    // Même carte que l'onglet Sorts de la fiche : aperçu rapide, détail dépliable.
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtreMineurs.appliquer(mineursCandidats).forEach { sort ->
                            val coche = sort.name in mineursChoisis
                            SortApercuCarte(
                                sort = sort,
                                selectionne = coche,
                                selectionPossible = mineursChoisis.size < mineursRequis,
                                onSelection = {
                                    mineursChoisis = when {
                                        coche -> mineursChoisis - sort.name
                                        mineursChoisis.size < mineursRequis -> mineursChoisis + sort.name
                                        else -> mineursChoisis
                                    }
                                }
                            )
                        }
                    }
                }

                if (nombreNouveauxSorts > 0) {
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        (if (baliseGrimoire != null) "Nouveaux sorts du grimoire" else "Nouveau sort préparé") +
                            " (${classeSorts?.nom}) — choisissez $sortsRequis sort${if (sortsRequis > 1) "s" else ""} " +
                            "de niveau $niveauMaxNouveauSort au plus (${nouveauxSortsChoisis.size}/$sortsRequis) :",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (baliseGrimoire != null) {
                        Text(
                            "Les $nouveauxEmplacementsPrepares premier(s) choisi(s) sont préparés d'office (nouveaux emplacements de " +
                                "préparation) ; les autres restent dans le grimoire. Changez votre préparation dans l'écran Repos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetTextSecondary
                        )
                    }
                    if (sortsCandidats.isEmpty()) {
                        Text(
                            "Aucun nouveau sort disponible dans la bibliothèque pour cette classe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetTextSecondary
                        )
                    } else {
                        FiltreSortsBarre(filtreNouveauxSorts, sortsCandidats)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        filtreNouveauxSorts.appliquer(sortsCandidats)
                            .filter { s -> sortsEcoleChoisis.values.none { s.name in it } }
                            .forEach { sort ->
                            val coche = sort.name in nouveauxSortsChoisis
                            SortApercuCarte(
                                sort = sort,
                                selectionne = coche,
                                selectionPossible = nouveauxSortsChoisis.size < sortsRequis,
                                onSelection = {
                                    nouveauxSortsChoisis = when {
                                        coche -> nouveauxSortsChoisis - sort.name
                                        nouveauxSortsChoisis.size < sortsRequis -> nouveauxSortsChoisis + sort.name
                                        else -> nouveauxSortsChoisis
                                    }
                                }
                            )
                        }
                    }
                }

                emplacementsSpeciaux.forEach { e ->
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text(
                        "${e.apt.nom} — sort du ${if (e.niveau == 1) "1er" else "${e.niveau}e"} niveau de votre grimoire" +
                            (e.incantation?.let { " (incantation : ${it.lowercase()})" } ?: "") + " : " +
                            when (e.mode) {
                                ArsenalPersonnage.SORT_A_VOLONTE -> "lançable à volonté sans emplacement."
                                ArsenalPersonnage.SORT_PREDILECTION -> "lançable une fois sans emplacement par repos court ou long."
                                else -> "lançable sans emplacement."
                            },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    val candidats = candidatsSpeciaux(e)
                    if (candidats.isEmpty()) {
                        Text("Aucun sort éligible dans le grimoire.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                    }
                    candidats.forEach { sort ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { sortsSpeciauxChoisis = sortsSpeciauxChoisis + (e.cle to sort.name) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sortsSpeciauxChoisis[e.cle] == sort.name,
                                onClick = { sortsSpeciauxChoisis = sortsSpeciauxChoisis + (e.cle to sort.name) }
                            )
                            Text(sort.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                aptitudeDon?.let { (apt, categorie) ->
                    HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                    Text("${apt.nom} — choisissez un don ($categorie) :", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (donsCandidats.isEmpty()) {
                        Text("Aucun don de cette catégorie disponible dans la bibliothèque.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                    }
                    donsCandidats.forEach { don ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { donChoisiNom = don.nom },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = donChoisiNom == don.nom, onClick = { donChoisiNom = don.nom })
                            Text(don.nom, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    donChoisi?.let { don ->
                        Markdown(content = don.description)
                        augmentationDon?.takeIf { it.options.isNotEmpty() }?.let { aug ->
                            Text(
                                "Augmentation : +${aug.gain} à une caractéristique (max ${aug.max})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                aug.options.forEach { abrev ->
                                    val valeur = abilities.first { it.first == abrev }.second
                                    FilterChip(
                                        selected = caracDon == abrev,
                                        enabled = valeur < aug.max,
                                        onClick = { caracDon = abrev },
                                        label = { Text(abrev) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (etapeChoixSousClasse) {
                TextButton(enabled = sousClasseRetenue != null, onClick = { sousClasseValidee = true }) { Text("Suivant") }
                return@AlertDialog
            }
            TextButton(
                enabled = chargementTermine && choixAsiComplet && choixCompetenceMulticlasseComplet && choixEffetComplet &&
                    choixExpertiseComplet && choixEspeceComplet && choixMulticlasseComplet && choixSortsComplet && choixMineursComplet && choixSousClasseComplet && choixSortsEcoleComplet && choixAptitudesComplet &&
                    choixSpeciauxComplet && choixDonComplet,
                onClick = {
                    especeCourante?.let { espece ->
                        // Seuls les traits concernés par un choix de ce niveau sont réécrits sur la fiche
                        // (valeur choisie + raison) ; les autres y figurent déjà depuis la création.
                        val traitsChoisis = espece.copy(traits = espece.traits.filter { t -> choixEspeceNiveau.any { it.nomTrait == t.nom } })
                        GameState.appliquerGainsEspece(
                            characterId = character.id,
                            sorts = sortsEspeceGagnes,
                            resistances = resistancesEspeceGagnees,
                            competences = competencesEspeceGagnees,
                            choix = choixEspeceFaits,
                            traitsTexte = if (choixEspeceFaits.isEmpty()) "" else traitsEspecePourFiche(traitsChoisis, choixEspeceFaits),
                        )
                    }
                    val aptitudesTexte = aptitudesGagnees.joinToString("\n\n") { apt ->
                        val prefixeClasse = pisteActive?.classe?.nom?.takeIf { pisteActive.estNouvelle || !pisteActive.estPrincipale }
                        val suffixeNiveau = " (Niveau $nouveauNiveauDansClasse${prefixeClasse?.let { " $it" } ?: ""})"
                        if (apt.type == "choix-effet") {
                            val choix = optionsChoisiesEffet[apt.id ?: apt.nom]
                            val option = choix?.let { nom -> apt.options.firstOrNull { it.nom == nom } }
                            val mineurs = mineursChoisis.takeIf { sortsMineursOption?.first == apt && it.isNotEmpty() }
                                ?.let { "\nSorts mineurs choisis : ${it.joinToString(", ")}" }.orEmpty()
                            "${apt.nom}$suffixeNiveau — ${choix.orEmpty()} : ${option?.description.orEmpty()}$mineurs"
                        } else if (apt.type == "choix-generique" && pointsAssignes.any { it.value > 0 }) {
                            // Amélioration de caractéristique : les valeurs retenues figurent dans le
                            // titre (en vert sur la carte « Capacités de classe »).
                            val gains = pointsAssignes.filterValues { it > 0 }.entries.joinToString(", ") { (abrev, gain) ->
                                "${abilitySaveName(abrev)} +$gain"
                            }
                            val details = pointsAssignes.filterValues { it > 0 }.entries.joinToString("\n") { (abrev, gain) ->
                                val avant = abilities.first { it.first == abrev }.second
                                "${abilitySaveName(abrev)} : $avant → ${(avant + gain).coerceAtMost(20)}"
                            }
                            "${apt.nom}$suffixeNiveau — $gains : " +
                                listOf(details, apt.description).filter { it.isNotBlank() }.joinToString("\n")
                        } else if (apt.type.startsWith("choix-expertise")) {
                            val choisies = expertiseChoisies[apt.id ?: apt.nom].orEmpty()
                            "${apt.nom}$suffixeNiveau — Expertise : ${choisies.joinToString(", ").ifBlank { "aucune compétence éligible" }}"
                        } else {
                            val sortsChoisis = choixAptitudes.filter { it.first == apt }.flatMap { (_, c) -> choixAptitudesFaits[c.id].orEmpty() } +
                                choixSortsEcole.filter { it.first == apt }
                                .flatMap { (_, b) -> sortsEcoleChoisis[b["id"] ?: apt.nom].orEmpty() } +
                                emplacementsSpeciaux.filter { it.apt == apt }.mapNotNull { sortsSpeciauxChoisis[it.cle] } +
                                listOfNotNull(donChoisi?.nom?.takeIf { aptitudeDon?.first == apt })
                            val titre = "${apt.nom}$suffixeNiveau" +
                                if (sortsChoisis.isNotEmpty()) " — ${sortsChoisis.joinToString(", ")}" else ""
                            if (apt.description.isNotBlank()) "$titre : ${apt.description}" else titre
                        }
                    }.let { texte ->
                        if (doitChoisirSousClasse && pisteActive?.estPrincipale != true && sousClasseRetenue != null) {
                            listOf(texte, "Sous-classe (${pisteActive?.classe?.nom}) : ${sousClasseRetenue.nom}")
                                .filter { it.isNotBlank() }.joinToString("\n\n")
                        } else texte
                    }.let { texte ->
                        if (infosMulticlasse != null) {
                            val maitrises = listOfNotNull(
                                infosMulticlasse.armures?.let { "Armures : $it" },
                                infosMulticlasse.armes?.let { "Armes : $it" },
                                infosMulticlasse.outils?.let { "Outils : ${outilsMulticlasse?.let { choisis -> "$choisis (choix : « $it »)" } ?: it}" }
                            ).joinToString(" · ").ifBlank { "aucune maîtrise supplémentaire" }
                            listOf(texte, "Multiclassage (${pisteActive?.classe?.nom}) : $maitrises")
                                .filter { it.isNotBlank() }.joinToString("\n\n")
                        } else texte
                    }
                    GameState.appliquerMonteeDeNiveau(
                        characterId = character.id,
                        pvGagnes = pvGagnes + pvEspece,
                        aptitudesTexte = aptitudesTexte,
                        sousClasse = sousClasseRetenue?.nom?.takeIf { doitChoisirSousClasse && pisteActive?.estPrincipale == true },
                        ameliorationCaracteristiques = pointsAssignes.filterValues { it != 0 }.toMutableMap().apply {
                            // Augmentation du don choisi (Faveur épique : +1, max 30).
                            val aug = augmentationDon
                            val carac = caracDon
                            if (aug != null && carac != null) merge(carac, aug.gain, Int::plus)
                        },
                        plafondCaracteristiques = augmentationDon?.takeIf { caracDon != null }?.max ?: 20,
                        donTexte = donChoisi?.let { don ->
                            val augmentation = augmentationDon?.let { aug -> caracDon?.let { " — ${abilitySaveName(it)} +${aug.gain}" } }.orEmpty()
                            "${don.nom} (${aptitudeDon?.second.orEmpty()}, niveau $nouveauNiveauTotal)$augmentation : ${don.description}"
                        }.orEmpty(),
                        nouveauxSortsSpeciaux = emplacementsSpeciaux.mapNotNull { e -> sortsSpeciauxChoisis[e.cle]?.let { it to e.mode } }.toMap(),
                        // Classe à grimoire : seuls les nouveaux emplacements de préparation sont remplis d'office.
                        sortsAPreparer = if (baliseGrimoire != null) {
                            nouveauxSortsChoisis.take(nouveauxEmplacementsPrepares) + mineursChoisis + sortsSousClasseGagnes
                        } else null,
                        classeMulticlasseCible = pisteActive?.classe?.nom?.takeIf { pisteActive.estNouvelle || !pisteActive.estPrincipale },
                        nouvelleCompetence = competenceMulticlasseChoisie,
                        nouvellesCompetences = competencesAptitudes,
                        nouvellesExpertises = expertiseChoisies.values.flatten(),
                        // Sorts mineurs d'une option (Combattant béni) : sorts de la classe qui progresse.
                        nouveauxSorts = nouveauxSortsChoisis.toList() + mineursChoisis + sortsSousClasseGagnes + sortsEcoleChoisis.values.flatten(),
                        classeNouveauxSorts = classeSorts?.nom,
                        sourcesNouveauxSorts = buildMap {
                            val classeNiveau = "${classeSorts?.nom ?: character.characterClass} niv. $nouveauNiveauDansClasse"
                            nouveauxSortsChoisis.forEach {
                                put(it, "$classeNiveau — " + if (baliseGrimoire != null) "copié dans le grimoire" else "nouveau sort préparé")
                            }
                            sortsMineursOption?.first?.let { apt -> mineursChoisis.forEach { put(it, "$classeNiveau — ${apt.nom}") } }
                            sortsSousClasseGagnes.forEach { put(it, "Sous-classe : ${sousClasseRetenue?.nom.orEmpty()} (toujours préparé)") }
                            choixSortsEcole.forEach { (apt, b) -> sortsEcoleChoisis[b["id"] ?: apt.nom].orEmpty().forEach { put(it, "$classeNiveau — ${apt.nom}") } }
                        },
                    )
                    onDismiss()
                }
            ) { Text("Confirmer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Plus tard") } }
    )
}

/** Petit bouton bascule "sélectionné/non" pour le choix du mode de points de vie (LevelUpDialog). */
@Composable
private fun PvModeButton(label: String, selectionne: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selectionne) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = if (selectionne) MaterialTheme.colorScheme.onPrimary else SheetTextPrimary
        )
    }
}

@Composable
private fun HpEditDialog(
    currentHp: Int,
    maxHp: Int,
    onConfirm: (current: Int, max: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var currentText by remember { mutableStateOf(currentHp.toString()) }
    var maxText by remember { mutableStateOf(maxHp.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les points de vie") },
        text = {
            Column {
                OutlinedTextField(
                    value = maxText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) maxText = input },
                    label = { Text("PV maximum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) currentText = input },
                    label = { Text("PV actuels") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newMax = maxText.toIntOrNull() ?: maxHp
                val newCurrent = currentText.toIntOrNull() ?: currentHp
                onConfirm(newCurrent, newMax)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun HitDiceEditDialog(
    hitDieFaces: Int,
    level: Int,
    hitDiceRemaining: Int,
    onConfirm: (remaining: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var remainingText by remember { mutableStateOf(hitDiceRemaining.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier les dés de vie (D$hitDieFaces)") },
        text = {
            OutlinedTextField(
                value = remainingText,
                onValueChange = { input -> if (input.all { it.isDigit() }) remainingText = input },
                label = { Text("Dés restants (sur $level)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val newRemaining = (remainingText.toIntOrNull() ?: hitDiceRemaining).coerceIn(0, level)
                onConfirm(newRemaining)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * États du personnage : plusieurs à la fois, cochés parmi ceux des règles (catalogue Etats, le
 * même que le combat et le menu MJ), plus des mentions libres séparées par des virgules.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionEditDialog(
    currentCondition: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val etatsInitiaux = remember { Etats.lire(currentCondition) }
    var etats by remember { mutableStateOf(etatsInitiaux) }
    var autresTexte by remember { mutableStateOf(Etats.autres(currentCondition).joinToString(", ")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("États") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etats.choisissables.forEach { etat ->
                        FilterChip(
                            selected = etat in etats,
                            onClick = { etats = if (etat in etats) etats - etat else etats + etat },
                            label = { Text(etat.label) }
                        )
                    }
                }
                com.jc2.jdrcompagnon.feature_combat.ui.ResumeEffetsEtats(etats)
                OutlinedTextField(
                    value = autresTexte,
                    onValueChange = { autresTexte = it },
                    label = { Text("Autres (séparés par des virgules)") },
                    placeholder = { Text("Ex : Maudit, Ivre...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            // Un état tapé à la main (« paralysée ») est reconnu et rangé avec les autres.
            TextButton(onClick = { onConfirm(Etats.ecrire(autresTexte, etats + Etats.lire(autresTexte))) }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun XpEditDialog(
    currentXp: Int,
    onConfirm: (newXp: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var xpText by remember { mutableStateOf(currentXp.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier l'expérience") },
        text = {
            OutlinedTextField(
                value = xpText,
                onValueChange = { input -> if (input.all { it.isDigit() }) xpText = input },
                label = { Text("XP total") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(xpText.toIntOrNull() ?: currentXp)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun SpeedEditDialog(
    currentSpeed: Int,
    onConfirm: (newSpeed: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var speedText by remember { mutableStateOf(currentSpeed.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la vitesse") },
        text = {
            OutlinedTextField(
                value = speedText,
                onValueChange = { input -> if (input.all { it.isDigit() }) speedText = input },
                label = { Text("Vitesse (mètres)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(speedText.toIntOrNull() ?: currentSpeed)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

private val characterSizeOptions = listOf("Très Petite", "Petite", "Moyenne", "Grande", "Très Grande", "Gigantesque")

@Composable
private fun SizeEditDialog(
    currentSize: String,
    onConfirm: (newSize: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(currentSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier la taille") },
        text = {
            Column {
                characterSizeOptions.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = option }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selected == option) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .border(1.dp, SheetTextSecondary, CircleShape)
                        )
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/** Teinte dorée du cadre de portrait — cohérente avec la couleur de l'or (Bourse). */
private val PortraitFrameGold = Color(0xFFD4AF37)

/**
 * Portrait disponible localement (drawable ajouté au projet). Internal (pas private) :
 * réutilisé par EtapePortrait dans le wizard de création de personnage.
 */
internal data class PortraitOption(val id: String, val label: String, val resId: Int)

internal val characterPortraitOptions = listOf(
    PortraitOption("drakeide_f", "Drakéide (F)", R.drawable.av_drakeide_f),
    PortraitOption("drakeide_m", "Drakéide (H)", R.drawable.av_drakeide_m),
    PortraitOption("elfe_f", "Elfe (F)", R.drawable.av_elfe_f),
    PortraitOption("elfe_m", "Elfe (H)", R.drawable.av_elfe_m),
    PortraitOption("gnome_f", "Gnome (F)", R.drawable.av_gnome_f),
    PortraitOption("gnome_m", "Gnome (H)", R.drawable.av_gnome_m),
    PortraitOption("goliath_f", "Goliath (F)", R.drawable.av_goliath_f),
    PortraitOption("goliath_m", "Goliath (H)", R.drawable.av_goliath_m),
    PortraitOption("halfling_f", "Halfelin (F)", R.drawable.av_halfling_f),
    PortraitOption("halfling_m", "Halfelin (H)", R.drawable.av_halfling_m),
    PortraitOption("humain_f", "Humain (F)", R.drawable.av_humain_f),
    PortraitOption("humain_h", "Humain (H)", R.drawable.av_humain_h),
    PortraitOption("nain_f", "Nain (F)", R.drawable.av_nain_f),
    PortraitOption("nain_h", "Nain (H)", R.drawable.av_nain_h),
    PortraitOption("nain2_h", "Nain (H) 2", R.drawable.av_nain2_h),
    PortraitOption("orc_f", "Orc (F)", R.drawable.av_orc_f),
    PortraitOption("orc_m", "Orc (H)", R.drawable.av_orc_m),
    PortraitOption("tieffelin_f", "Tieffelin (F)", R.drawable.av_tieffelin_f),
    PortraitOption("tieffelin_h", "Tieffelin (H)", R.drawable.av_tieffelin_h),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortraitPickerDialog(
    currentPortrait: String,
    // PNJ : seuls les portraits du dossier assets/dnd/PNJ sont proposés (voir PnjPortraits).
    pnj: Boolean = false,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // (id, libellé) de chaque portrait proposé ; l'image est résolue par rememberCharacterPortraitPainter.
    val options = remember(pnj) {
        if (pnj) PnjPortraits.ids(context).map { it to PnjPortraits.label(it) }
        else characterPortraitOptions.map { it.id to it.label }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir un portrait") },
        text = {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (optionId, optionLabel) ->
                    val isSelected = optionId == currentPortrait
                    val painter = rememberCharacterPortraitPainter(optionId) ?: return@forEach
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(optionId) }
                            .padding(8.dp)
                    ) {
                        Image(
                            painter = painter,
                            contentDescription = optionLabel,
                            modifier = Modifier
                                .width(56.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else SheetBorder,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(optionLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

/**
 * Badge cliquable pour l'Inspiration Héroïque — s'allume en doré quand
 * elle est disponible, à toggle au tap (dépensée après usage en jeu).
 */
@Composable
private fun InspirationBadge(active: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) PortraitFrameGold.copy(alpha = 0.25f) else SheetSurfaceLight)
                .border(
                    1.dp,
                    if (active) PortraitFrameGold else SheetTextSecondary.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Inspiration héroïque",
                tint = if (active) PortraitFrameGold else SheetTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        StatBadgeLabel("INSPIRATION")
    }
}

/**
 * Libellé de classe(s) affiché sous le nom du personnage : "Classe (Sous-classe)" pour un
 * personnage mono-classe (comportement inchangé), ou notation multiclassage standard
 * "ClasseA N / ClasseB M / ..." (cf. Character.classesSecondaires) dès qu'il a des niveaux
 * dans une autre classe — sinon le multiclassage n'apparaissait nulle part sur la fiche.
 */
private fun libelleClasses(character: Character): String {
    if (character.classesSecondaires.isEmpty()) {
        return character.characterClass + character.subclass.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
    }
    val niveauPrincipal = character.level - character.classesSecondaires.sumOf { it.niveau }
    val classePrincipale = character.characterClass + " $niveauPrincipal" +
        character.subclass.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
    val secondaires = character.classesSecondaires.map { "${it.classe} ${it.niveau}" }
    return (listOf(classePrincipale) + secondaires).joinToString(" / ")
}

/**
 * Type de dé de vie associé à une classe D&D 5e (noms français du SRD).
 */
internal fun hitDieForClass(characterClass: String): Int = when (characterClass.trim().lowercase(Locale.FRANCE)) {
    "barbare" -> 12
    "guerrier", "paladin", "rôdeur", "rodeur" -> 10
    "barde", "clerc", "druide", "moine", "roublard", "occultiste" -> 8
    "ensorceleur", "magicien" -> 6
    else -> 8
}

/**
 * Catégorie de taille D&D 5e associée à une race (noms français du SRD).
 */
private fun sizeForRace(race: String): String = when (race.trim().lowercase(Locale.FRANCE)) {
    "nain", "halfelin", "gnome" -> "Petite"
    else -> "Moyenne"
}

@Composable
private fun StatBadge(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    boxModifier: Modifier = Modifier.fillMaxWidth().height(30.dp),
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = boxModifier
                .clip(RoundedCornerShape(8.dp))
                .background(SheetSurfaceLight)
                .border(1.dp, SheetTextSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .let {
                    if (onClick != null || onDoubleClick != null) {
                        it.combinedClickable(onClick = { onClick?.invoke() }, onDoubleClick = onDoubleClick)
                    } else it
                }
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AutoShrinkText(
                value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, lineHeight = 16.sp),
                color = SheetTextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        StatBadgeLabel(label)
    }
}

@Composable
private fun StatBadgeLabel(label: String) {
    AutoShrinkText(
        label,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
        color = SheetTextSecondary
    )
}

/**
 * Texte sur une ligne qui réduit sa taille de police jusqu'à tenir dans la largeur
 * disponible (Compose 1.7 n'a pas encore TextAutoSize).
 */
@Composable
private fun AutoShrinkText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 8.sp
) {
    var fontSize by remember(text, style) { mutableStateOf(style.fontSize) }
    var ready by remember(text, style) { mutableStateOf(false) }
    Text(
        text,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        style = style.copy(fontSize = fontSize, lineHeight = (fontSize.value * 1.2f).sp),
        color = color,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize.value > minFontSize.value) {
                fontSize = (fontSize.value * 0.9f).coerceAtLeast(minFontSize.value).sp
            } else {
                ready = true
            }
        }
    )
}

/**
 * Jets de sauvegarde en aperçu (lecture seule) — la version modifiable
 * complète reste dans l'onglet "Compétences".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SavingThrowsOverviewCard(character: Character) {
    SheetCard {
        Text(
            "Jets de sauvegarde",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = SheetTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 2
        ) {
            savingThrows.forEach { save ->
                val proficient = character.savingThrowProficiencies.contains(save)
                val total = GameState.abilityModifierForSave(save, character) +
                        if (proficient) character.proficiencyBonus else 0
                Row(
                    modifier = Modifier.width(150.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (proficient) MaterialTheme.colorScheme.error else Color.Transparent)
                                .border(1.dp, SheetTextSecondary, CircleShape)
                        )
                        Text(
                            save.uppercase(Locale.FRANCE),
                            style = MaterialTheme.typography.labelMedium,
                            color = SheetTextPrimary
                        )
                    }
                    Text(
                        text = if (total >= 0) "+$total" else total.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = SheetTextPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilitiesGrid(character: Character, isMjMode: Boolean = false) {
    val abilities = listOf(
        "FOR" to character.strength,
        "DEX" to character.dexterity,
        "CON" to character.constitution,
        "INT" to character.intelligence,
        "SAG" to character.wisdom,
        "CHA" to character.charisma
    )
    var editingAbility by remember { mutableStateOf<String?>(null) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        maxItemsInEachRow = 3
    ) {
        var detailAbility by remember { mutableStateOf<String?>(null) }

        abilities.forEach { (label, value) ->
            val proficient = character.savingThrowProficiencies.contains(abilitySaveName(label))
            AbilityCard(
                label,
                value,
                proficient,
                onClick = if (isMjMode) { { editingAbility = label } } else null,
                onDoubleClick = { detailAbility = label },
                ameliorations = ameliorationsCaracteristique(character, label)
            )
        }

        val abilityDetail = detailAbility
        if (abilityDetail != null) {
            val value = abilities.first { it.first == abilityDetail }.second
            val modifierValue = GameState.abilityModifier(value)
            // Améliorations choisies aux montées de niveau (« … (Niveau 4) — Force +1, … » dans classFeatures).
            val nomComplet = abilitySaveName(abilityDetail)
            val ameliorations = ameliorationsCaracteristique(character, abilityDetail)
                .map { (source, gain) -> "$source : +$gain" }
            StatDetailDialog(
                title = nomComplet.ifBlank { abilityDetail },
                lignes = listOf(
                    "$abilityDetail $value → modificateur ${if (modifierValue >= 0) "+$modifierValue" else modifierValue.toString()}",
                    "Formule : (valeur − 10) ÷ 2, arrondi à l'inférieur"
                ) + ameliorations,
                onDismiss = { detailAbility = null }
            )
        }
    }

    val abilityBeingEdited = editingAbility
    if (abilityBeingEdited != null) {
        val currentValue = abilities.first { it.first == abilityBeingEdited }.second
        val saveName = abilitySaveName(abilityBeingEdited)
        AbilityEditDialog(
            label = abilityBeingEdited,
            currentValue = currentValue,
            proficient = character.savingThrowProficiencies.contains(saveName),
            onConfirm = { newValue, newProficient ->
                GameState.setAbilityScore(character.id, abilityBeingEdited, newValue)
                if (newProficient != character.savingThrowProficiencies.contains(saveName)) {
                    GameState.toggleSavingThrowProficiency(character.id, saveName)
                }
                editingAbility = null
            },
            onDismiss = { editingAbility = null }
        )
    }
}

@Composable
private fun AbilityEditDialog(
    label: String,
    currentValue: Int,
    proficient: Boolean,
    onConfirm: (Int, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var valueText by remember { mutableStateOf(currentValue.toString()) }
    var proficientChecked by remember { mutableStateOf(proficient) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifier $label") },
        text = {
            Column {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) valueText = input },
                    label = { Text("Valeur") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { proficientChecked = !proficientChecked }
                ) {
                    Checkbox(checked = proficientChecked, onCheckedChange = { proficientChecked = it })
                    Text("Maîtrise le jet de sauvegarde")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newValue = valueText.toIntOrNull() ?: currentValue
                onConfirm(newValue, proficientChecked)
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

/**
 * Nom complet du jet de sauvegarde associé à l'abréviation d'une
 * caractéristique (FOR/DEX/CON/INT/SAG/CHA), pour vérifier la maîtrise.
 */
/** Augmentation de caractéristique d'un don : abréviations possibles (FOR…CHA), gain et plafond. */
private data class AugmentationDon(val options: List<String>, val gain: Int, val max: Int)

private val ABREVIATIONS_CARACTERISTIQUES = listOf("FOR", "DEX", "CON", "INT", "SAG", "CHA")

/**
 * Lit la ligne « **Augmentation de caractéristique** … » d'un don de dons_srd521.md
 * (« Intelligence, Sagesse ou Charisme +1 (max 30) », « 1 caractéristique au choix (max 30) »).
 * null si le don n'en donne pas (« - »).
 */
private fun augmentationDeDon(don: Don): AugmentationDon? {
    val ligne = Regex("""\*\*Augmentation de caractéristique\*\*\s*(.+)""").find(don.description)?.groupValues?.get(1)?.trim()
        ?.takeIf { it != "-" } ?: return null
    val max = Regex("""\(max (\d+)\)""").find(ligne)?.groupValues?.get(1)?.toIntOrNull() ?: 20
    val gain = Regex("""\+(\d+)""").find(ligne)?.groupValues?.get(1)?.toIntOrNull() ?: 1
    val options = if (ligne.contains("au choix", ignoreCase = true)) ABREVIATIONS_CARACTERISTIQUES
    else ABREVIATIONS_CARACTERISTIQUES.filter { ligne.contains(abilitySaveName(it), ignoreCase = true) }
    return AugmentationDon(options, gain, max)
}

private fun abilitySaveName(label: String): String = when (label) {
    "FOR" -> "Force"
    "DEX" -> "Dextérité"
    "CON" -> "Constitution"
    "INT" -> "Intelligence"
    "SAG" -> "Sagesse"
    "CHA" -> "Charisme"
    else -> ""
}

@Composable
private fun AbilityCard(
    label: String,
    value: Int,
    proficient: Boolean = false,
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    // Améliorations de caractéristique des montées de niveau (source, points), déjà incluses
    // dans [value] : affichées au verso de la carte, qu'un appui retourne (sans [onClick]).
    ameliorations: List<Pair<String, Int>> = emptyList()
) {
    val modifierValue = GameState.abilityModifier(value)
    val gainNiveaux = ameliorations.sumOf { it.second }
    var retournee by rememberSaveable(label) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (retournee) 180f else 0f, animationSpec = tween(400), label = "retournement-$label")
    val versoVisible = rotation > 90f
    Box(
        modifier = Modifier
            .width(100.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick?.invoke() ?: run { retournee = !retournee } },
                    onDoubleClick = onDoubleClick
                ),
            shape = RoundedCornerShape(16.dp),
            color = SheetSurfaceLight.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, if (gainNiveaux > 0 && !versoVisible) CouleurChoix.copy(alpha = 0.6f) else SheetBorder)
        ) {
            Box {
                // Recto : toujours composé (il fixe la taille de la carte), masqué une fois retourné.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (versoVisible) 0f else 1f)
                        .padding(vertical = 14.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SheetTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (modifierValue >= 0) "+$modifierValue" else modifierValue.toString(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = SheetTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SheetBorder,
                        border = BorderStroke(1.dp, SheetTextSecondary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            value.toString(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary
                        )
                    }
                }
                // Verso : valeur de base et améliorations gagnées aux montées de niveau.
                if (versoVisible) {
                    Column(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { rotationY = 180f }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SheetTextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Base ${value - gainNiveaux}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = SheetTextPrimary
                        )
                        if (ameliorations.isEmpty()) {
                            Text(
                                "Aucune amélioration",
                                style = MaterialTheme.typography.labelSmall,
                                color = SheetTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            ameliorations.forEach { (source, gain) ->
                                val niveau = Regex("""Niveau (\d+)""").find(source)?.groupValues?.get(1)
                                Text(
                                    "+$gain" + (niveau?.let { " (niv. $it)" } ?: ""),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CouleurChoix
                                )
                            }
                        }
                    }
                }
            }
        }

        if (proficient && !versoVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(20.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                border = BorderStroke(1.dp, SheetTextPrimary.copy(alpha = 0.6f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "M",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

/**
 * Améliorations de la caractéristique [abreviation] (FOR, DEX…) choisies aux montées de niveau,
 * relues dans les capacités de classe (« Amélioration de caractéristique (Niveau 4) — Force +1,
 * Constitution +1 : … », cf. LevelUpDialog) : (source, points gagnés).
 */
/** « deux sorts mineurs de Clerc » dans la description d'une option d'aptitude : (nombre, liste). */
private val REGEX_SORTS_MINEURS_OPTION = Regex("""(\d+|une?|deux|trois|quatre)\s+sorts?\s+mineurs?\s+(?:de|du)\s+(\p{L}+)""", RegexOption.IGNORE_CASE)
private val NOMBRES_EN_LETTRES = mapOf("un" to 1, "une" to 1, "deux" to 2, "trois" to 3, "quatre" to 4)

private fun ameliorationsCaracteristique(character: Character, abreviation: String): List<Pair<String, Int>> {
    val nom = abilitySaveName(abreviation).ifBlank { return emptyList() }
    val regexGain = Regex("""(?<![\p{L}])${Regex.escape(nom)} \+(\d)""")
    return character.classFeatures.lineSequence().mapNotNull { ligne ->
        Regex("""^(.+?\(Niveau \d+[^)]*\)) — (.+?) : """).find(ligne)?.let { m ->
            regexGain.find(m.groupValues[2])?.let { m.groupValues[1] to it.groupValues[1].toInt() }
        }
    }.toList()
}

/** Popup générique "détail du calcul" déclenchée par double-clic, sur le même principe que
 * la charge/les équipements : quelques lignes de texte expliquant comment une valeur affichée
 * a été obtenue. */
@Composable
private fun StatDetailDialog(title: String, lignes: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lignes.forEach { ligne ->
                    Text(ligne, style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
    }
}

@Composable
private fun CombatTab(character: Character, isMjMode: Boolean) {
    CombatTabBase(character)
    Spacer(modifier = Modifier.height(12.dp))
    EffetsEnCoursCard(character)
    ArmesEtPouvoirsCard(character, isMjMode)
    // PNJ : tout ce qu'il sait faire est réuni ici (pas d'onglet Sorts pour lui).
    if (character.type == "PNJ") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CapacitesSection("Capacités", character.classFeatures)
            CapacitesSection("Traits", character.traits)
            CapacitesSection("Dons", character.feats)
            PnjSortsCard(character, isMjMode)
            if (isMjMode) com.jc2.jdrcompagnon.feature_combat.ui.ComportementIaPnjCard(character)
        }
    }
}

/**
 * Armes équipées (attaque et dégâts, bonus magique compris — voir ArmesPersonnage) et pouvoirs
 * des objets à charges équipés (voir ObjetsACharges) : bouton d'utilisation qui dépense une
 * charge, et ajustement manuel des charges côté MJ. Masquée si rien n'est équipé.
 */
@Composable
private fun ArmesEtPouvoirsCard(character: Character, isMjMode: Boolean) {
    val context = LocalContext.current
    val equipes = remember(character.equippedSlots, character.equippedItems, character.backpackExteriorSlots) {
        (character.equippedSlots.values + character.equippedItems + character.backpackExteriorSlots.values + character.quiverContents).distinct()
    }
    var bibliotheque by remember { mutableStateOf<List<com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem>>(emptyList()) }
    LaunchedEffect(character.worldId) {
        bibliotheque = SrdRepository.loadEquipmentList(
            context, character.worldId.ifBlank { GameState.currentWorldId() ?: "donjon_et_dragon" }
        )
    }
    val objetsEquipes = remember(equipes, bibliotheque) {
        // Nom de la bibliothèque, au singulier près (« Dagues » → « Dague »).
        val noms = bibliotheque.map { it.name }
        equipes.mapNotNull { nom ->
            val canon = ArmorRules.nomCanonique(nom, noms)
            bibliotheque.firstOrNull { it.name.equals(canon, ignoreCase = true) }
        }.distinctBy { it.name }
    }
    val armes = objetsEquipes.filter { it.damage.isNotBlank() }
    val attaques = remember(armes, character.strength, character.dexterity, character.proficiencyBonus) {
        if (armes.isEmpty()) emptyList() else ArmesPersonnage.attaques(
            nomsArmes = armes.map { it.name },
            armes = armes.map {
                ArmeSrd(
                    nom = it.name,
                    degats = it.damage,
                    proprietes = it.properties,
                    aDistance = it.properties.contains("Munitions", ignoreCase = true) ||
                        it.rawMarkdown.contains("**Portée** Distance"),
                )
            },
            force = character.strength,
            dexterite = character.dexterity,
            maitrise = character.proficiencyBonus,
        )
    }
    val objetsACharges = objetsEquipes.filter { it.charges != null }
    val consommables = objetsEquipes.filter { it.consommable }
    var message by remember(character.id) { mutableStateOf<String?>(null) }

    if (attaques.isEmpty() && objetsACharges.isEmpty() && consommables.isEmpty()) return

    SheetCard {
        Text("Armes et pouvoirs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
        attaques.forEach { attaque ->
            val arme = armes.firstOrNull { it.name == attaque.nom }
            Spacer(modifier = Modifier.height(8.dp))
            Text(attaque.nom, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Text(
                "Attaque ${(attaque.bonusToucher ?: 0).let { if (it >= 0) "+$it" else "$it" }} · " +
                    "Dégâts ${attaque.formuleDegats ?: "—"}${attaque.typeDegats?.let { " $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = SheetTextPrimary,
            )
            arme?.properties?.takeIf { it.isNotBlank() && it != "-" }?.let {
                Text("Propriétés : $it", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
            }
            if (arme != null && !Harmonisation.estActif(character, arme)) {
                Text(
                    "${Harmonisation.libelle(arme)} : non harmonisée, ses propriétés magiques ne s'appliquent pas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        objetsACharges.forEach { objet ->
            val max = objet.charges ?: 0
            val restantes = ObjetsACharges.restantes(character, objet)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SheetBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                objet.pouvoir.substringBefore(':').trim().ifBlank { "Pouvoir" } + " — ${objet.name}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary,
            )
            if (':' in objet.pouvoir) {
                Text(objet.pouvoir.substringAfter(':').trim(), style = MaterialTheme.typography.bodySmall, color = SheetTextPrimary)
            }
            val actif = Harmonisation.estActif(character, objet)
            if (!actif) {
                Text(
                    "${Harmonisation.libelle(objet)} : non harmonisé, pouvoir inactif (harmonisation au repos court).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (isMjMode) {
                    TextButton(
                        onClick = { GameState.setItemCharges(character.id, objet.name, restantes - 1) },
                        enabled = restantes > 0,
                    ) { Text("−") }
                }
                Text(
                    "Charges : $restantes / $max",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SheetTextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (isMjMode) {
                    TextButton(
                        onClick = { GameState.setItemCharges(character.id, objet.name, (restantes + 1).coerceAtMost(max)) },
                        enabled = restantes < max,
                    ) { Text("+") }
                }
                Button(
                    onClick = { message = ObjetsACharges.utiliser(character, objet) },
                    enabled = restantes > 0 && actif,
                ) { Text("Utiliser") }
            }
            if (objet.recharge.isNotBlank()) {
                Text(
                    "Récupère ${objet.recharge} charge(s) à chaque repos long." +
                        (objet.destruction?.let { " Dernière charge : d20, détruit sur $it." } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary,
                )
            }
        }
        if (consommables.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SheetBorder)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Consommables équipés", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            consommables.forEach { objet ->
                val nombre = (character.equippedSlots.values + character.equippedItems + character.backpackExteriorSlots.values + character.quiverContents)
                    .count { it == objet.name }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(objet.name + if (nombre > 1) " x$nombre" else "", style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
                        Text(Consommables.resume(objet), style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                    }
                    Button(onClick = { message = Consommables.utiliser(character, objet) }) { Text("Utiliser") }
                }
            }
        }
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Effets à durée en cours (potion bue, sort lancé) avec leur temps restant d'après l'horloge de
 * scénario — qui ne s'écoule, pendant un combat, que de 6 s par round. Masquée s'il n'y en a pas.
 */
@Composable
private fun EffetsEnCoursCard(character: Character) {
    if (character.effetsActifs.isEmpty()) return
    val horloge by ScenarioClockState.state.collectAsState()
    val maintenant = ScenarioClockState.totalSeconds(horloge)
    SheetCard {
        Text("Effets en cours", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
        character.effetsActifs.sortedBy { it.finSecondes }.forEach { effet ->
            val restant = effet.finSecondes - maintenant
            val rounds = (restant + ScenarioClockState.SECONDES_PAR_ROUND - 1) / ScenarioClockState.SECONDES_PAR_ROUND
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        effet.nom + if (effet.concentration) " (concentration)" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SheetTextPrimary,
                    )
                    Text(
                        "Reste " + DureeEffet.libelle(restant) + if (restant <= 60) " ($rounds round(s))" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = SheetTextSecondary,
                    )
                    if (effet.source != "Sort" && effet.source != effet.nom) {
                        Text(effet.source, style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                    }
                }
                IconButton(onClick = { GameState.retirerEffet(character.id, effet.id) }) {
                    Icon(Icons.Default.Close, contentDescription = "Mettre fin à l'effet", tint = SheetTextSecondary)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

/**
 * Sorts d'un PNJ dans son onglet Combat : DD et bonus d'attaque, liste des sorts (dépliables)
 * et ajout depuis toute la bibliothèque côté MJ. N'apparaît pas si le PNJ n'a aucun sort et
 * qu'on n'est pas en mode MJ.
 */
@Composable
private fun PnjSortsCard(character: Character, isMjMode: Boolean) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var expandedSpell by remember { mutableStateOf<String?>(null) }
    if (character.spells.isEmpty() && !isMjMode) return

    SheetCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sorts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            if (isMjMode) {
                IconButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un sort", tint = SheetTextPrimary)
                }
            }
        }
        val saveDC = GameState.spellSaveDC(character)
        val attackBonus = GameState.spellAttackBonus(character)
        if (saveDC != null && attackBonus != null) {
            Text(
                "DD de sauvegarde $saveDC · attaque ${if (attackBonus >= 0) "+$attackBonus" else "$attackBonus"}",
                style = MaterialTheme.typography.bodySmall,
                color = SheetTextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (character.spells.isEmpty()) {
            Text("Aucun sort", style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            character.spells.forEach { spellName ->
                SpellRow(
                    spellName = spellName,
                    worldId = character.worldId,
                    isMjMode = isMjMode,
                    expanded = expandedSpell == spellName,
                    onClick = { expandedSpell = if (expandedSpell == spellName) null else spellName },
                    onDelete = { GameState.removeSpellFromCharacter(character.id, spellName) },
                    onLancer = { sort -> Consommables.lancerSort(character, sort) },
                )
            }
        }
    }

    if (showPicker) {
        SrdLibraryPickerDialog(
            title = "Choisir un sort",
            onDismiss = { showPicker = false },
            onSelect = { spellName -> GameState.addSpellToCharacter(character.id, spellName, character.characterClass) },
            search = { query ->
                SrdRepository.searchSpells(context, query, character.worldId.ifBlank { "donjon_et_dragon" })
                    .map { SrdPickerEntry(name = it.name, subtitle = it.category) }
            }
        )
    }
}

/** Types de dégâts du SRD 5.2, proposés pour les résistances et vulnérabilités. */
private val damageTypes = listOf(
    "Acide", "Contondant", "Feu", "Force", "Foudre", "Froid", "Nécrotique",
    "Perforant", "Poison", "Psychique", "Radiant", "Tonnerre", "Tranchant",
)

/** Résistances (dégâts ÷ 2) et vulnérabilités (dégâts × 2), modifiables via le crayon. */
@Composable
private fun ResistancesVulnerabilitesCard(character: Character) {
    var showEdit by remember { mutableStateOf(false) }
    SheetCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Résistances et vulnérabilités",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SheetTextPrimary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showEdit = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Modifier les résistances et vulnérabilités", tint = SheetTextPrimary)
            }
        }
        Text("🛡 Résistances (dégâts ÷ 2)", style = MaterialTheme.typography.labelLarge, color = SheetTextSecondary)
        Text(
            character.damageResistances.joinToString(", ").ifEmpty { "Aucune" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (character.damageResistances.isEmpty()) SheetTextSecondary else SheetTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("⚠ Vulnérabilités (dégâts × 2)", style = MaterialTheme.typography.labelLarge, color = SheetTextSecondary)
        Text(
            character.damageVulnerabilities.joinToString(", ").ifEmpty { "Aucune" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (character.damageVulnerabilities.isEmpty()) SheetTextSecondary else SheetTextPrimary
        )
    }

    if (showEdit) {
        var resistances by remember { mutableStateOf(character.damageResistances.toSet()) }
        var vulnerabilites by remember { mutableStateOf(character.damageVulnerabilities.toSet()) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Résistances et vulnérabilités") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Résistances", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    DamageTypeChips(resistances) { type ->
                        resistances = if (type in resistances) resistances - type else resistances + type
                        vulnerabilites = vulnerabilites - type
                    }
                    Text("Vulnérabilités", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    DamageTypeChips(vulnerabilites) { type ->
                        vulnerabilites = if (type in vulnerabilites) vulnerabilites - type else vulnerabilites + type
                        resistances = resistances - type
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    GameState.updateCharacter(
                        character.copy(
                            damageResistances = damageTypes.filter { it in resistances },
                            damageVulnerabilities = damageTypes.filter { it in vulnerabilites },
                        )
                    )
                    showEdit = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Annuler") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DamageTypeChips(selected: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        damageTypes.forEach { type ->
            androidx.compose.material3.FilterChip(
                selected = type in selected,
                onClick = { onToggle(type) },
                label = { Text(type) },
            )
        }
    }
}

@Composable
private fun CombatTabBase(character: Character) {
    val acBreakdown = GameState.armorClassBreakdown(character)
    val init = GameState.abilityModifier(character.dexterity)
    var showAcDetail by remember { mutableStateOf(false) }
    var showInitDetail by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (character.weaponArmorTraining.isNotBlank()) {
            SheetCard {
                Text("Maîtrises de combat", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(character.weaponArmorTraining, style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
                // Bottes d'arme choisies (écran Repos), avec la botte de chaque arme.
                val bottes = ArsenalPersonnage.bottesActives(character)
                if (ArsenalPersonnage.nombreBottes(character) > 0 && !bottes.isNullOrEmpty()) {
                    val context = LocalContext.current
                    var bottesParArme by remember(character.worldId) { mutableStateOf<Map<String, String>>(emptyMap()) }
                    LaunchedEffect(character.worldId) {
                        bottesParArme = SrdRepository.loadEquipmentList(context, character.worldId.ifBlank { "donjon_et_dragon" })
                            .filter { it.damage.isNotBlank() }
                            .mapNotNull { e -> ArmeSrd.depuisFiche(e.name, e.damage, e.properties, e.rawMarkdown).botte?.let { e.name to it } }
                            .toMap()
                    }
                    Text(
                        "Bottes d'arme : " + bottes.joinToString(", ") { arme ->
                            arme + (bottesParArme.entries.firstOrNull { it.key.equals(arme, ignoreCase = true) }?.value?.let { " ($it)" } ?: "")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SheetTextPrimary
                    )
                }
            }
        }
        ResistancesVulnerabilitesCard(character)
        SheetCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { showAcDetail = true })
                ) {
                    Icon(Icons.Default.Shield, null, tint = SheetTextSecondary)
                    Text("Classe d'armure", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { showInitDetail = true })
                ) {
                    Text(
                        "INITIATIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SheetTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (init >= 0) "+$init" else init.toString(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SheetTextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = acBreakdown.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = SheetTextSecondary
            )
            if (acBreakdown.hasArmor || acBreakdown.hasShield) {
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (acBreakdown.hasArmor) {
                        AssistChip(
                            onClick = {},
                            label = { Text(acBreakdown.armorName ?: "Armure") },
                            leadingIcon = { Icon(Icons.Default.Shield, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SheetSurfaceLight,
                                labelColor = SheetTextPrimary,
                                leadingIconContentColor = SheetTextSecondary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = SheetBorder
                            )
                        )
                    }
                    if (acBreakdown.hasShield) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Bouclier +2") },
                            leadingIcon = { Icon(Icons.Default.Shield, null, Modifier.size(16.dp)) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SheetSurfaceLight,
                                labelColor = SheetTextPrimary,
                                leadingIconContentColor = SheetTextSecondary
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = SheetBorder
                            )
                        )
                    }
                }
            }
        }
    }

    if (showAcDetail) {
        StatDetailDialog(title = "Classe d'armure", lignes = listOf(acBreakdown.detail), onDismiss = { showAcDetail = false })
    }
    if (showInitDetail) {
        StatDetailDialog(
            title = "Initiative",
            lignes = listOfNotNull(
                "Initiative = modificateur de Dextérité = $init (DEX ${character.dexterity})",
                if (character.initiativeBonus != 0) "Bonus supplémentaire : ${if (character.initiativeBonus >= 0) "+" else ""}${character.initiativeBonus}" else null
            ),
            onDismiss = { showInitDetail = false }
        )
    }
}

@Composable
private fun ProficiencyRow(
    label: String,
    level: ProficiencyLevel,
    modifier: Int,
    proficiencyBonus: Int,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, labelText) = when (level) {
        ProficiencyLevel.NONE -> SheetSurfaceLight to "—"
        ProficiencyLevel.PROFICIENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) to "M"
        ProficiencyLevel.EXPERTISE -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f) to "E"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SheetBorder,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(labelText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                }
            }
            Text(label, style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
        }
        val total = modifier + when (level) {
            ProficiencyLevel.NONE -> 0
            ProficiencyLevel.PROFICIENT -> proficiencyBonus
            ProficiencyLevel.EXPERTISE -> proficiencyBonus * 2
        }
        Text(
            text = if (total >= 0) "+$total" else "$total",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = SheetTextPrimary
        )
    }
}

@Composable
private fun EquipmentTab(character: Character, isMjMode: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // L'offre de butin de groupe est maintenant affichée par PlayerNetworkOverlay (ui/components),
        // monté une fois au niveau du NavGraph — visible quel que soit l'écran, pas seulement l'inventaire.
        EquipmentManagementContent(character, isMjMode)
    }
}

/**
 * Carte affichée par l'overlay réseau global quand le MJ propose un objet au groupe (loot de
 * groupe) : le joueur clique "Je le veux" ou "Passer", la carte disparaît alors localement — le
 * MJ suit les réponses de son côté (voir ScenarioReaderScreen) et ferme l'offre pour tout le
 * monde quand tous ont répondu.
 */
@Composable
fun LootOfferCard(itemName: String, onWantIt: () -> Unit, onPass: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Objet proposé par le MJ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(itemName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onWantIt, modifier = Modifier.weight(1f)) { Text("Je le veux") }
                OutlinedButton(onClick = onPass, modifier = Modifier.weight(1f)) { Text("Passer") }
            }
        }
    }
}

/**
 * Emplacements de sort d'un personnage, à partir de ses classes (principale + secondaires,
 * cf. Character.classesSecondaires) et de la table SRD ("## Emplacements de sort (lanceurs
 * complets)" de classes_srd521.md, lue par EmplacementsDeSortParser).
 */
internal data class EmplacementsDeSortResultat(
    // Niveau de sort (1-9) -> nombre maximum d'emplacements, seulement les niveaux > 0.
    val classiques: Map<Int, Int>,
    // (nombre, niveau des emplacements) de Magie de pacte (Occultiste), null si absent.
    val pacte: Pair<Int, Int>?
)

/**
 * Calcule les emplacements de sort d'un personnage : somme des niveaux des classes
 * "complet" + moitié arrondie au supérieur des classes "demi" → niveau de lanceur
 * effectif, cherché dans la table partagée. L'Occultiste ("pacte") a sa propre table
 * Magie de pacte, lue directement dans sa table de progression (indépendante de la
 * table partagée, et non cumulée avec elle — règle SRD, cf. progression.md).
 */
internal fun calculerEmplacementsDeSort(
    character: Character,
    classesSrd: List<Classe>,
    tableEmplacements: TableMd?
): EmplacementsDeSortResultat {
    val niveauPrincipal = character.level - character.classesSecondaires.sumOf { it.niveau }
    val pistes = buildList {
        classesSrd.firstOrNull { it.nom.equals(character.characterClass, ignoreCase = true) }
            ?.let { add(it to niveauPrincipal) }
        character.classesSecondaires.forEach { nc ->
            classesSrd.firstOrNull { it.nom.equals(nc.classe, ignoreCase = true) }?.let { add(it to nc.niveau) }
        }
    }

    val niveauEffectif = pistes.sumOf { (classe, niveau) ->
        when (classe.typeIncantation) {
            "complet" -> niveau
            // Demi-lanceur (Paladin, Rôdeur) : moitié arrondie au supérieur (règles 2024), soit
            // 2 emplacements du 1er niveau dès le niveau 1.
            "demi" -> (niveau + 1) / 2
            else -> 0
        }
    }
    val suffixes = mapOf(1 to "1er", 2 to "2e", 3 to "3e", 4 to "4e", 5 to "5e", 6 to "6e", 7 to "7e", 8 to "8e", 9 to "9e")
    val classiques = if (niveauEffectif > 0 && tableEmplacements != null) {
        suffixes.mapNotNull { (niveauSort, colonne) ->
            tableEmplacements.valeurNiveau(niveauEffectif.toString(), colonne)
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
                ?.let { niveauSort to it }
        }.toMap()
    } else emptyMap()

    val pacte = pistes.firstOrNull { (classe, _) -> classe.typeIncantation == "pacte" }?.let { (classe, niveau) ->
        val table = classe.tableProgression ?: return@let null
        val nombre = table.valeurNiveau(niveau.toString(), "Emplacements de sort")?.toIntOrNull()
        val niveauEmplacements = table.valeurNiveau(niveau.toString(), "Niveau des emplacements")?.toIntOrNull()
        if (nombre != null && niveauEmplacements != null && nombre > 0) nombre to niveauEmplacements else null
    }

    return EmplacementsDeSortResultat(classiques, pacte)
}

/**
 * Une classe de lanceur de sorts du personnage (principale ou secondaire, cf.
 * Character.classesSecondaires) : caractéristique d'incantation, niveau de sort maximum
 * castable et sorts mineurs/préparés max **propres à cette seule classe** — un personnage
 * multiclassé détermine ses sorts connus/préparés par classe individuellement, comme s'il
 * était mono-classe dans chacune (règle SRD, § "Incantation" de progression.md), plutôt
 * que de piocher dans une liste combinée. Absente de la liste = pas encore lanceur dans
 * cette classe à ce niveau (ex. demi-lanceur au niveau 1).
 */
internal data class PisteIncantation(
    val classe: Classe,
    val niveauDansClasse: Int,
    val ability: String,
    val maxNiveauSort: Int,
    val sortsMineursMax: Int?,
    val sortsPreparesMax: Int?
)

internal val SUFFIXES_NIVEAU_SORT = listOf(
    1 to "1er", 2 to "2e", 3 to "3e", 4 to "4e", 5 to "5e", 6 to "6e", 7 to "7e", 8 to "8e", 9 to "9e"
)

/** Plus haut niveau de sort avec au moins un emplacement, à un niveau de lanceur donné de la table partagée. */
internal fun TableMd.plusHautNiveauAvecEmplacement(niveauLanceur: Int): Int =
    SUFFIXES_NIVEAU_SORT.lastOrNull { (_, colonne) -> (valeurNiveau(niveauLanceur.toString(), colonne)?.toIntOrNull() ?: 0) > 0 }
        ?.first ?: 0

/**
 * Sorts toujours préparés par la sous-classe de la classe principale (ex. table des sorts du
 * Serment des Anciens), jusqu'au niveau actuel dans cette classe. Vide si la sous-classe
 * n'est pas choisie ou n'est pas reconnue dans [classesSrd].
 */
internal fun sortsDeSousClasse(character: Character, classesSrd: List<Classe>): List<String> {
    if (character.subclass.isBlank()) return emptyList()
    val classe = classesSrd.firstOrNull { it.nom.equals(character.characterClass, ignoreCase = true) } ?: return emptyList()
    val sousClasse = classe.sousClasses.firstOrNull { it.nom.equals(character.subclass.trim(), ignoreCase = true) } ?: return emptyList()
    return sousClasse.sortsJusquAu(character.level - character.classesSecondaires.sumOf { it.niveau })
}

internal fun calculerPistesIncantation(
    character: Character,
    classesSrd: List<Classe>,
    tableEmplacements: TableMd?
): List<PisteIncantation> {
    val niveauPrincipal = character.level - character.classesSecondaires.sumOf { it.niveau }
    val pistesBrutes = buildList {
        classesSrd.firstOrNull { it.nom.equals(character.characterClass, ignoreCase = true) }
            ?.let { add(it to niveauPrincipal) }
        character.classesSecondaires.forEach { nc ->
            classesSrd.firstOrNull { it.nom.equals(nc.classe, ignoreCase = true) }?.let { add(it to nc.niveau) }
        }
    }
    return pistesBrutes.mapNotNull { (classe, niveau) ->
        // Caractéristique d'incantation de la classe (Paladin : Charisme), pas sa première
        // caractéristique principale (« Force et Charisme » donnait Force).
        val ability = spellcastingAbilityByClass.entries.firstOrNull { it.key.equals(classe.nom, ignoreCase = true) }?.value
            ?: classe.caracteristiquePrincipale.firstOrNull()?.label ?: return@mapNotNull null
        val maxNiveauSort = when (classe.typeIncantation) {
            "complet" -> tableEmplacements?.plusHautNiveauAvecEmplacement(niveau) ?: 0
            "demi" -> tableEmplacements?.plusHautNiveauAvecEmplacement((niveau + 1) / 2) ?: 0
            "pacte" -> classe.tableProgression?.valeurNiveau(niveau.toString(), "Niveau des emplacements")?.toIntOrNull() ?: 0
            else -> return@mapNotNull null
        }
        if (maxNiveauSort <= 0) return@mapNotNull null
        PisteIncantation(
            classe = classe,
            niveauDansClasse = niveau,
            ability = ability,
            maxNiveauSort = maxNiveauSort,
            sortsMineursMax = classe.tableProgression?.valeurNiveau(niveau.toString(), "Sorts mineurs")?.toIntOrNull(),
            sortsPreparesMax = classe.tableProgression?.valeurNiveau(niveau.toString(), "Sorts préparés")?.toIntOrNull()
        )
    }
}

/**
 * Onglet Sorts, embarqué directement dans la fiche (plus d'écran séparé) : caractéristique
 * d'incantation calculée depuis la classe, puis liste des sorts connus — cliquer sur un sort
 * déplie son détail directement sous la ligne (pas de nouvel écran).
 */
@Composable
private fun SpellsTab(character: Character, isMjMode: Boolean) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    var showChoixClassePourAjout by remember { mutableStateOf(false) }
    var classeChoisiePourAjout by remember { mutableStateOf<String?>(null) }
    var expandedSpell by remember { mutableStateOf<String?>(null) }

    var classesSrd by remember(character.worldId) { mutableStateOf<List<Classe>>(emptyList()) }
    var tableEmplacements by remember(character.worldId) { mutableStateOf<TableMd?>(null) }
    LaunchedEffect(character.worldId) {
        val rawMarkdown = SrdRepository.loadClasses(context, character.worldId.ifBlank { "donjon_et_dragon" })
            .joinToString("\n\n") { it.rawMarkdown }
        classesSrd = ClasseParser.parse(rawMarkdown)
        tableEmplacements = EmplacementsDeSortParser.parse(rawMarkdown)
    }
    val emplacements = remember(character, classesSrd, tableEmplacements) {
        calculerEmplacementsDeSort(character, classesSrd, tableEmplacements)
    }
    // Une piste par classe de lanceur du personnage (principale + secondaires) : sorts
    // connus/préparés déterminés PAR CLASSE (règle SRD), pas dans une liste combinée — cf.
    // PisteIncantation. Pour un personnage mono-classe, il n'y en a qu'une, et l'affichage
    // reste identique à avant (une seule carte, pas de sélection de classe à l'ajout).
    val pistesIncantation = remember(character, classesSrd, tableEmplacements) {
        calculerPistesIncantation(character, classesSrd, tableEmplacements)
    }
    // Sorts toujours préparés par une aptitude de classe (Châtiment de paladin : Châtiment divin)
    // ou par la sous-classe de la classe principale (table de sorts du Serment...), affichés
    // avec les autres mais hors du compte des sorts préparés.
    val sortsToujoursPrepares = remember(character, classesSrd) {
        ArsenalPersonnage.sortsToujoursPreparesClasse(character) + sortsDeSousClasse(character, classesSrd)
    }
    val sortsPreparesAffiches = character.spells.filterNot { s -> sortsToujoursPrepares.any { it.equals(s, ignoreCase = true) } }
    // Niveau de chaque sort du SRD : les sorts mineurs ne comptent pas dans les sorts préparés.
    var niveauxSorts by remember(character.worldId) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(character.worldId) {
        niveauxSorts = SrdRepository.loadSpells(context, character.worldId.ifBlank { "donjon_et_dragon" })
            .associate { it.name.lowercase() to ArsenalPersonnage.niveauDepuisLibelle(it.niveauSort) }
    }
    // Enregistré sur la fiche (Paladin passé niveau 2, ou personnage d'avant cette règle) pour
    // figurer aussi côté MJ, en combat et dans la synchronisation réseau.
    LaunchedEffect(character.id, sortsToujoursPrepares, character.spells) {
        sortsToujoursPrepares.filter { s -> character.spells.none { it.equals(s, ignoreCase = true) } }
            .forEach { s ->
                val classe = if (s.equals(ArsenalPersonnage.SORT_CHATIMENT_DIVIN, ignoreCase = true)) "Paladin" else character.characterClass
                GameState.addSpellToCharacter(character.id, s, classe)
            }
    }
    // Règle expliquée au double-appui sur une valeur de la carte d'incantation : (titre, lignes).
    var regleIncantation by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    regleIncantation?.let { (titre, lignes) -> StatDetailDialog(titre, lignes) { regleIncantation = null } }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Repli synchrone (pas besoin d'attendre le chargement de classesSrd) pour les
        // mondes/classes hors SRD D&D (ex. Naheulbeuk) ou pendant le court instant avant que
        // pistesIncantation ne soit calculée : évite qu'un lanceur de sorts connu perde
        // temporairement sa carte "Caractéristique d'incantation".
        // Emplacements de sort communs : un pool PARTAGÉ entre toutes les classes "complet"/
        // "demi" du personnage (règle SRD), donc rattaché à une seule carte — celle du
        // fallback ou de l'unique piste dans le cas courant (mono-classe), sinon sa propre
        // carte séparée après les cartes par classe (vrai multiclassage à 2+ lanceurs).
        @Composable
        fun blocEmplacements() {
            if (emplacements.classiques.isNotEmpty() || emplacements.pacte != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
                Spacer(modifier = Modifier.height(8.dp))
                // Tous les niveaux de sort que la classe atteindra, même pas encore débloqués :
                // 9 pour un lanceur complet (Magicien, Clerc...), 5 pour un demi-lanceur
                // (Paladin, Rôdeur), aucun emplacement classique pour la seule Magie de pacte.
                val niveauMaxClasse = pistesIncantation.maxOfOrNull { piste ->
                    when (piste.classe.typeIncantation) {
                        "complet" -> 9
                        "demi" -> 5
                        else -> 0
                    }
                } ?: 0
                val niveauMax = maxOf(niveauMaxClasse, emplacements.classiques.keys.maxOrNull() ?: 0)
                (1..niveauMax).chunked(5).forEach { ligne ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ligne.forEach { niveauSort ->
                            val max = emplacements.classiques[niveauSort] ?: 0
                            val utilises = (character.spellSlotsUsed[niveauSort] ?: 0).coerceAtMost(max)
                            CaseEmplacement(
                                titre = "Niv. $niveauSort",
                                disponibles = max - utilises,
                                max = max,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Dernière ligne incomplète (niveaux 6 à 9) : cases de même largeur.
                        repeat(5 - ligne.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                emplacements.pacte?.let { (max, niveauSort) ->
                    val utilises = character.pactSlotsUsed.coerceAtMost(max)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CaseEmplacement(
                            titre = "Pacte niv. $niveauSort",
                            disponibles = max - utilises,
                            max = max,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        if (pistesIncantation.isEmpty()) {
            val ability = GameState.spellcastingAbility(character)
            if (ability != null) {
                val modifier = GameState.spellcastingModifier(character) ?: 0
                val saveDC = GameState.spellSaveDC(character) ?: 0
                val attackBonus = GameState.spellAttackBonus(character) ?: 0
                SheetCard {
                    Text("Caractéristique d'incantation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpellStatColumn("Caractéristique", ability, Modifier.weight(1f))
                        SpellStatColumn("Modificateur", if (modifier >= 0) "+$modifier" else "$modifier", Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpellStatColumn("DD de sauvegarde", "$saveDC", Modifier.weight(1f))
                        SpellStatColumn("Bonus d'attaque", if (attackBonus >= 0) "+$attackBonus" else "$attackBonus", Modifier.weight(1f))
                    }
                    // Classe non reconnue dans le SRD (chargement en cours, monde hors D&D...) :
                    // pas de table pour calculer les emplacements, seule la caractéristique
                    // d'incantation générique reste disponible ici.
                }
            }
        }
        pistesIncantation.forEach { piste ->
            val modifier = GameState.abilityModifierForSave(piste.ability, character)
            val saveDC = 8 + character.proficiencyBonus + modifier
            val attackBonus = character.proficiencyBonus + modifier
            SheetCard {
                Text(
                    "Caractéristique d'incantation : ${piste.ability}" + if (pistesIncantation.size > 1) " (${piste.classe.nom})" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SheetTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                val prepares = sortsPreparesAffiches.count {
                    (character.spellClasses[it] ?: character.characterClass).equals(piste.classe.nom, ignoreCase = true) &&
                        niveauxSorts[it.lowercase()] != 0
                }
                val signeMod = if (modifier >= 0) "+$modifier" else "$modifier"
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpellStatColumn("Modificateur\nd'incantation", signeMod, Modifier.weight(1f)) {
                        regleIncantation = "Modificateur d'incantation" to listOf(
                            "${piste.ability} → modificateur $signeMod ((valeur − 10) ÷ 2, arrondi à l'inférieur).",
                            "Le ${piste.classe.nom} lance ses sorts grâce à son ${piste.ability} : ce modificateur entre dans le DD de sauvegarde et le bonus d'attaque de ses sorts, et dans les effets qui l'indiquent (ex. PV temporaires d'Héroïsme).",
                            "Augmenter le ${piste.ability} rend donc tous ses sorts plus efficaces."
                        )
                    }
                    SpellStatColumn("DD de\nsauvegarde", "$saveDC", Modifier.weight(1f)) {
                        regleIncantation = "DD de sauvegarde des sorts" to listOf(
                            "8 + bonus de maîtrise (+${character.proficiencyBonus}) + modificateur d'incantation ($signeMod) = $saveDC.",
                            "Quand un de vos sorts impose un jet de sauvegarde (ex. Constitution pour Châtiment de fournaise), la cible doit obtenir au moins $saveDC pour y résister.",
                            "Plus il est élevé, plus vos sorts ont de chances de faire effet."
                        )
                    }
                    SpellStatColumn("Bonus\nd'attaque", if (attackBonus >= 0) "+$attackBonus" else "$attackBonus", Modifier.weight(1f)) {
                        regleIncantation = "Bonus d'attaque des sorts" to listOf(
                            "Bonus de maîtrise (+${character.proficiencyBonus}) + modificateur d'incantation ($signeMod) = ${if (attackBonus >= 0) "+$attackBonus" else "$attackBonus"}.",
                            "Pour un sort qui demande un jet d'attaque, lancez 1d20 + ce bonus : il touche si le total atteint la CA de la cible."
                        )
                    }
                    if (piste.sortsPreparesMax != null) {
                        SpellStatColumn("Sorts\npréparés", "$prepares/${piste.sortsPreparesMax}", Modifier.weight(1f)) {
                            regleIncantation = "Sorts préparés" to listOf(
                                "Vous avez $prepares sort(s) préparé(s) sur ${piste.sortsPreparesMax} possible(s) au niveau ${piste.niveauDansClasse}.",
                                "Seuls les sorts préparés peuvent être lancés. Chaque nouveau niveau qui augmente ce nombre vous fait choisir un nouveau sort, d'un niveau au plus égal à votre plus haut emplacement.",
                                "Les sorts toujours préparés (ex. Châtiment divin du Paladin) ne comptent pas dans ce total. Les sorts mineurs non plus.",
                                "Lancer un sort de niveau 1 ou plus dépense un emplacement de ce niveau ou d'un niveau supérieur ; ils reviennent au repos long."
                            )
                        }
                    }
                }
                piste.sortsMineursMax?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Sorts mineurs : $it (toujours prêts, sans emplacement)", style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary)
                }
                // Pool d'emplacements affiché ici seulement en mono-classe (ou fallback) : en
                // vrai multiclassage (2+ pistes), il est partagé entre elles et reçoit sa
                // propre carte juste après (cf. plus bas), pour ne pas le dupliquer par classe.
                if (pistesIncantation.size <= 1) blocEmplacements()
                if (piste.classe.nom.equals("Paladin", ignoreCase = true)) RessourcesPaladin(character, isMjMode)
                if (piste.classe.nom.equals("Magicien", ignoreCase = true)) RessourcesMagicien(character)
            }
        }
        if (pistesIncantation.size > 1 && (emplacements.classiques.isNotEmpty() || emplacements.pacte != null)) {
            SheetCard { blocEmplacements() }
        }

        SheetCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sorts", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                // Réservé au MJ : le joueur apprend ses sorts à la création et aux montées de
                // niveau. Filtré sur la classe choisie (champ "Classes :" de sorts_srd521.md).
                // Personnage multiclassé (plusieurs pistes) : demande d'abord pour quelle
                // classe le sort est appris, les sorts restant compartimentés par classe.
                if (isMjMode) IconButton(onClick = {
                    if (pistesIncantation.size > 1) showChoixClassePourAjout = true
                    else {
                        classeChoisiePourAjout = pistesIncantation.firstOrNull()?.classe?.nom ?: character.characterClass
                        showPicker = true
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Voir/ajouter un sort de la classe", tint = SheetTextPrimary)
                }
            }
            if (character.spells.isEmpty() && sortsToujoursPrepares.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Aucun sort enregistré", style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                // Groupé par classe seulement pour un personnage multiclassé avec plusieurs
                // pistes de lanceur : la vue mono-classe habituelle reste une simple liste.
                val groupes = if (pistesIncantation.size > 1) {
                    (sortsPreparesAffiches + sortsToujoursPrepares).groupBy { s ->
                        character.spellClasses[s] ?: if (s in sortsToujoursPrepares) "Paladin" else character.characterClass
                    }
                } else {
                    mapOf(character.characterClass to sortsPreparesAffiches + sortsToujoursPrepares)
                }
                // D'où vient le sort : enregistré à son acquisition (création, montée de niveau,
                // grimoire...), sinon déduit pour les personnages créés avant ce suivi.
                fun origineSort(s: String): String = character.spellSources[s] ?: when {
                    s.equals(ArsenalPersonnage.SORT_CHATIMENT_DIVIN, ignoreCase = true) && s in sortsToujoursPrepares ->
                        "Châtiment de paladin (toujours préparé)"
                    s in character.sortsSpeciaux -> "Aptitude de classe (toujours préparé)"
                    s in sortsToujoursPrepares -> "Sous-classe : ${character.subclass} (toujours préparé)"
                    character.traits.contains(s, ignoreCase = true) -> "Espèce : ${character.race}"
                    character.feats.contains(s, ignoreCase = true) -> "Don"
                    character.classFeatures.contains(s, ignoreCase = true) -> "Aptitude de classe"
                    else -> character.spellClasses[s] ?: character.characterClass
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    groupes.forEach { (classeSort, sortsGroupe) ->
                        val piste = pistesIncantation.firstOrNull { it.classe.nom.equals(classeSort, ignoreCase = true) }
                            ?: pistesIncantation.singleOrNull()
                        // Sorts mineurs d'un côté, sorts de niveau 1+ préparés de l'autre, chacun
                        // avec son maximum (colonnes de la table de progression de la classe).
                        val (mineurs, prepares) = sortsGroupe.partition { niveauxSorts[it.lowercase()] == 0 }
                        val preparesComptes = prepares.count { it !in sortsToujoursPrepares }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (groupes.size > 1) {
                                Text(classeSort, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
                            }
                            listOf(
                                "Sorts mineurs" + (piste?.sortsMineursMax?.let { " (${mineurs.size}/$it)" } ?: "") to mineurs,
                                "Sorts préparés" + (piste?.sortsPreparesMax?.let { " ($preparesComptes/$it)" } ?: "") to prepares,
                            ).filter { it.second.isNotEmpty() }.forEach { (titre, sorts) ->
                            Text(titre, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
                            // Favoris en tête de liste.
                            sorts.sortedByDescending { it in character.favoriteSpells }.forEach { spellName ->
                                SpellRow(
                                    spellName = spellName,
                                    worldId = character.worldId,
                                    isMjMode = isMjMode,
                                    expanded = expandedSpell == spellName,
                                    onClick = {
                                        expandedSpell = if (expandedSpell == spellName) null else spellName
                                    },
                                    onDelete = { GameState.removeSpellFromCharacter(character.id, spellName) },
                                    onLancer = { sort -> Consommables.lancerSort(character, sort) },
                                    toujoursPrepare = spellName in sortsToujoursPrepares,
                                    emplacementsRestants = emplacements.classiques.mapValues { (niveau, max) ->
                                        (max - (character.spellSlotsUsed[niveau] ?: 0)).coerceAtLeast(0)
                                    },
                                    onDepenserEmplacement = { niveau ->
                                        GameState.setSpellSlotUsed(character.id, niveau, (character.spellSlotsUsed[niveau] ?: 0) + 1)
                                    },
                                    origine = origineSort(spellName),
                                    rituelAutorise = ArsenalPersonnage.rituelAutorise(character, spellName),
                                    badgeSpecial = character.sortsSpeciaux[spellName]?.let { mode ->
                                        ArsenalPersonnage.libelleSortSpecial(mode)?.let { libelle ->
                                            if (mode == ArsenalPersonnage.SORT_PREDILECTION && spellName in character.sortsPredilectionUtilises) "$libelle (utilisé)"
                                            else libelle
                                        }
                                    },
                                    favori = spellName in character.favoriteSpells,
                                    onToggleFavori = { GameState.toggleFavoriteSpell(character.id, spellName) },
                                )
                            }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showChoixClassePourAjout) {
        AlertDialog(
            onDismissRequest = { showChoixClassePourAjout = false },
            title = { Text("Apprendre ce sort via quelle classe ?") },
            text = {
                Column {
                    pistesIncantation.forEach { piste ->
                        TextButton(
                            onClick = {
                                classeChoisiePourAjout = piste.classe.nom
                                showChoixClassePourAjout = false
                                showPicker = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${piste.classe.nom} (jusqu'au niveau ${piste.maxNiveauSort})", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showChoixClassePourAjout = false }) { Text("Annuler") } }
        )
    }

    if (showPicker) {
        val piste = pistesIncantation.firstOrNull { it.classe.nom.equals(classeChoisiePourAjout, ignoreCase = true) }
        // Niveau de sort le plus haut effectivement castable POUR CETTE CLASSE (sorts
        // connus/préparés déterminés par classe individuellement, cf. PisteIncantation) —
        // repli sur le total combiné si la classe n'est pas reconnue dans le SRD (MJ, monde
        // hors D&D...), pour ne jamais bloquer l'ajout.
        val niveauMaxCastable = piste?.maxNiveauSort ?: maxOf(
            emplacements.classiques.keys.maxOrNull() ?: 0,
            emplacements.pacte?.second ?: 0
        )
        val classePourRecherche = classeChoisiePourAjout ?: character.characterClass
        SrdLibraryPickerDialog(
            title = if (isMjMode) "Choisir un sort" else "Sorts de $classePourRecherche",
            onDismiss = { showPicker = false },
            onSelect = { spellName -> GameState.addSpellToCharacter(character.id, spellName, classePourRecherche, source = if (isMjMode) "Ajouté par le MJ" else null) },
            search = { query ->
                // Jamais un sort déjà acquis.
                val tousLesSorts = SrdRepository.searchSpells(context, query, character.worldId.ifBlank { "donjon_et_dragon" })
                    .filterNot { ArsenalPersonnage.sortConnu(character, it.name) }
                val filtres = if (isMjMode) tousLesSorts else tousLesSorts.filter { entry ->
                    entry.enseignePar(classePourRecherche) && entry.niveauSort() <= niveauMaxCastable
                }
                filtres.map { SrdPickerEntry(name = it.name, subtitle = it.category) }
            }
        )
    }
}

/**
 * Case compacte d'un pool de ressource (emplacements d'un niveau, Puissance curative…) :
 * disponibles/max, en lecture seule (dépensés en combat ou au lancement d'un sort, restaurés au
 * repos). Grisée si max = 0.
 */
@Composable
private fun CaseEmplacement(
    titre: String,
    disponibles: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val actif = max > 0
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = SheetSurfaceLight.copy(alpha = if (actif) 0.75f else 0.3f),
        border = BorderStroke(1.dp, if (actif && disponibles > 0) CouleurChoix.copy(alpha = 0.6f) else SheetBorder)
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(titre, style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary, maxLines = 1)
            Text(
                if (actif) "$disponibles/$max" else "—",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (actif) SheetTextPrimary else SheetTextSecondary
            )
        }
    }
}

/**
 * Ressources propres au Paladin, dans sa carte d'incantation : Puissance curative d'Imposition
 * des mains (5 × niveau de Paladin) et lancement gratuit de Châtiment divin (niveau 2+), toutes
 * deux récupérées au repos long.
 */
@Composable
private fun RessourcesPaladin(character: Character, isMjMode: Boolean) {
    val reserve = ArsenalPersonnage.reservePuissanceCurative(character)
    if (reserve <= 0) return
    val utilises = character.layOnHandsUsed.coerceAtMost(reserve)
    Spacer(modifier = Modifier.height(4.dp))
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CaseEmplacement(
            titre = "Puissance curative",
            disponibles = reserve - utilises,
            max = reserve,
            modifier = Modifier.weight(1f)
        )
        if (ArsenalPersonnage.aChatimentDePaladin(character)) {
            CaseEmplacement(
                titre = "Châtiment gratuit",
                disponibles = if (character.divineSmiteFreeUsed) 0 else 1,
                max = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Ressources propres au Magicien, dans sa carte d'incantation : Restauration magique (une fois
 * par repos long, budget = moitié du niveau de Magicien arrondie au supérieur).
 */
@Composable
private fun RessourcesMagicien(character: Character) {
    val budget = ArsenalPersonnage.budgetRestaurationMagique(character)
    if (budget <= 0) return
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CaseEmplacement(
            titre = "Restauration magique ($budget niv.)",
            disponibles = if (character.arcaneRecoveryUsed) 0 else 1,
            max = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

private val REGEX_CLASSES_SORT =Regex("""\*\*Classes\s*:\*\*\s*(.+)""")
private val REGEX_NIVEAU_SORT = Regex("""—\s*(\d+)[a-zé]*\s*niveau""", RegexOption.IGNORE_CASE)

/** Le sort figure-t-il dans la liste de sorts de [classe] (champ "**Classes :**" de sorts_srd521.md) ? */
private fun SrdEntry.enseignePar(classe: String): Boolean =
    REGEX_CLASSES_SORT.find(rawMarkdown)?.groupValues?.get(1).orEmpty()
        .split(",")
        .map { it.trim() }
        .any { it.equals(classe, ignoreCase = true) }

/** Niveau du sort (0 pour un sort mineur), lu sur la première ligne "*École — Niveau*". */
private fun SrdEntry.niveauSort(): Int =
    REGEX_NIVEAU_SORT.find(rawMarkdown)?.groupValues?.get(1)?.toIntOrNull() ?: 0

@Composable
private fun SpellStatColumn(label: String, value: String, modifier: Modifier = Modifier, onDoubleClick: (() -> Unit)? = null) {
    Column(
        modifier = modifier.then(if (onDoubleClick != null) Modifier.combinedClickable(onClick = {}, onDoubleClick = onDoubleClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SheetTextSecondary,
            textAlign = TextAlign.Center,
            // Libellés sur 2 lignes exactement : les valeurs restent alignées.
            minLines = 2,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SheetTextPrimary)
    }
}

/**
 * Ligne d'un sort connu : cliquer déplie son détail (école + description, relus depuis
 * la bibliothèque SRD) directement sous la ligne, sans naviguer vers un autre écran.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpellRow(
    spellName: String,
    worldId: String,
    isMjMode: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    // Sort à durée : l'ajoute aux effets en cours de la fiche (voir Consommables.lancerSort).
    onLancer: ((SrdEntry) -> String?)? = null,
    // Emplacements restants par niveau de sort : un sort de niveau N se lance en dépensant un
    // emplacement de niveau N ou supérieur ([onDepenserEmplacement]). null = pas de suivi (PNJ).
    emplacementsRestants: Map<Int, Int>? = null,
    onDepenserEmplacement: ((Int) -> Unit)? = null,
    // Sort toujours préparé par une aptitude (Châtiment divin du Paladin) : badge doré, non retirable.
    toujoursPrepare: Boolean = false,
    // D'où vient le sort (classe et niveau, espèce, don, grimoire...), affiché sous la carte.
    origine: String? = null,
    // Sort rituel que le personnage peut lancer en rituel (préparé, ou grimoire du Magicien —
    // Savoir rituel, cf. ArsenalPersonnage.rituelAutorise) : +10 min, sans emplacement.
    rituelAutorise: Boolean = false,
    // Lancement sans emplacement (« À volonté », « Prédilection ») : badge doré.
    badgeSpecial: String? = null,
    // Sort favori (étoile) : listé en premier quand il faut choisir un sort, en combat notamment.
    favori: Boolean = false,
    onToggleFavori: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var detail by remember(spellName) { mutableStateOf<SrdEntry?>(null) }
    var messageLancer by remember(spellName) { mutableStateOf<String?>(null) }
    var niveauEmplacementChoisi by remember(spellName) { mutableStateOf<Int?>(null) }
    var loading by remember(spellName) { mutableStateOf(false) }

    // Chargé dès l'affichage de la carte (pas seulement au dépli) : le niveau et le type du
    // sort doivent être visibles directement sur la carte, sans avoir à cliquer dessus.
    LaunchedEffect(spellName) {
        if (detail == null) {
            loading = true
            detail = SrdRepository.getSpellByName(context, spellName, worldId.ifBlank { "donjon_et_dragon" })
            loading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SheetSurfaceLight.copy(alpha = 0.75f)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spellName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (toujoursPrepare) CouleurToujoursPrepare else SheetTextPrimary,
                    )
                    val rituel = detail?.let { ArsenalPersonnage.estRituel(it.tempsIncantation) } == true
                    if (toujoursPrepare || rituel || detail?.niveauSort?.isNotBlank() == true || detail?.typeSort?.isNotBlank() == true) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (toujoursPrepare) SpellBadge(text = "Toujours préparé", color = CouleurToujoursPrepare)
                            if (rituel) SpellBadge(text = "Rituel", color = MaterialTheme.colorScheme.secondary)
                            badgeSpecial?.let { SpellBadge(text = it, color = CouleurToujoursPrepare) }
                            detail?.niveauSort?.takeIf { it.isNotBlank() }?.let { niveau ->
                                SpellBadge(text = niveau, color = SheetTextSecondary)
                            }
                            detail?.typeSort?.takeIf { it.isNotBlank() }?.let { type ->
                                SpellBadge(
                                    text = type,
                                    color = when (type) {
                                        "Attaque" -> MaterialTheme.colorScheme.error
                                        "Soutien" -> MaterialTheme.colorScheme.tertiary
                                        else -> SheetTextSecondary
                                    }
                                )
                            }
                        }
                    }
                    // Temps d'incantation et portée, visibles sans déplier le sort.
                    val infosIncantation = listOfNotNull(
                        detail?.tempsIncantation?.takeIf { it.isNotBlank() }?.let { "Incantation : $it" },
                        detail?.portee?.takeIf { it.isNotBlank() }?.let { "Portée : $it" },
                    )
                    if (infosIncantation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            infosIncantation.joinToString("  •  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextSecondary,
                        )
                    }
                }
                if (onToggleFavori != null) {
                    IconButton(onClick = onToggleFavori) {
                        Icon(
                            if (favori) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (favori) "Retirer des favoris" else "Ajouter aux favoris",
                            tint = if (favori) CouleurToujoursPrepare else SheetTextSecondary
                        )
                    }
                }
                if (isMjMode && !toujoursPrepare) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Retirer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    // Provenance du sort, visible seulement dans le détail.
                    origine?.let {
                        Text(
                            "Obtenu par : $it",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CouleurChoix,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    when {
                        loading -> Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                        detail == null -> Text(
                            "Détail introuvable dans la bibliothèque SRD",
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetTextSecondary
                        )
                        else -> {
                            detail!!.category.ifBlank { null }?.let { school ->
                                Text(
                                    "École de magie : $school",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Markdown(content = detail!!.rawMarkdown)
                            val sort = detail!!
                            val duree = remember(sort.name) { DureeEffet.depuisSort(sort.rawMarkdown) }
                            val niveauSort = ArsenalPersonnage.niveauDepuisLibelle(sort.niveauSort)
                            if (niveauSort > 0 && emplacementsRestants != null && onDepenserEmplacement != null) {
                                // Sort de niveau 1+ : dépense un emplacement de son niveau ou d'un niveau supérieur.
                                val possibles = emplacementsRestants.keys.filter { it >= niveauSort }.sorted()
                                val choisi = niveauEmplacementChoisi?.takeIf { (emplacementsRestants[it] ?: 0) > 0 }
                                    ?: possibles.firstOrNull { (emplacementsRestants[it] ?: 0) > 0 }
                                Text(
                                    "Emplacement utilisé (niveau $niveauSort ou supérieur)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SheetTextPrimary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                if (possibles.isEmpty()) {
                                    Text("Aucun emplacement de ce niveau.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    possibles.forEach { n ->
                                        val reste = emplacementsRestants[n] ?: 0
                                        FilterChip(
                                            selected = choisi == n,
                                            enabled = reste > 0,
                                            onClick = { niveauEmplacementChoisi = n },
                                            label = { Text("Niv. $n ($reste)") }
                                        )
                                    }
                                }
                                Button(
                                    enabled = choisi != null,
                                    onClick = {
                                        choisi?.let(onDepenserEmplacement)
                                        val effet = if (duree != null) onLancer?.invoke(sort) else null
                                        messageLancer = "${sort.name} lancé (emplacement niv. $choisi dépensé)." + (effet?.let { " $it" } ?: "")
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) { Text("Lancer le sort" + (choisi?.let { " (emplacement niv. $it)" } ?: "")) }
                                messageLancer?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            } else if (onLancer != null && duree != null) {
                                Button(onClick = { messageLancer = onLancer(sort) }, modifier = Modifier.padding(top = 8.dp)) {
                                    Text("Lancer le sort (" + duree.libelle + ")")
                                }
                                messageLancer?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (rituelAutorise && ArsenalPersonnage.estRituel(sort.tempsIncantation)) {
                                val minutes = ArsenalPersonnage.dureeRituelMinutes(sort.tempsIncantation)
                                OutlinedButton(
                                    onClick = {
                                        ScenarioClockState.advanceManually(minutes.toLong())
                                        val effet = if (duree != null) onLancer?.invoke(sort) else null
                                        messageLancer = "${sort.name} lancé en rituel ($minutes min, aucun emplacement dépensé)." +
                                            (effet?.let { " $it" } ?: "")
                                    },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) { Text("Lancer en rituel ($minutes min, sans emplacement)") }
                                // Message déjà affiché par le bloc d'emplacements ou de sort à durée ci-dessus.
                                val dejaAffiche = (niveauSort > 0 && emplacementsRestants != null && onDepenserEmplacement != null) ||
                                    (onLancer != null && duree != null)
                                if (!dejaAffiche) messageLancer?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Doré des sorts toujours préparés (hors limite de sorts préparés). */
private val CouleurToujoursPrepare = Color(0xFFFFD778)

/** Petite étiquette (niveau, type) affichée sous le nom d'un sort dans [SpellRow]. */
@Composable
private fun SpellBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun NotesTab(character: Character, isMjMode: Boolean) {
    var expandedAppearance by remember { mutableStateOf(false) }

    val isPnj = character.type == "PNJ"
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // PNJ : briefing utilisé par les discussions de scénario (confidentiel, côté MJ).
        if (isPnj) {
            SheetCard {
                Text("Briefing du PNJ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Text(
                    "Affiché au MJ quand il ouvre une discussion avec ce PNJ ; jamais envoyé aux joueurs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = character.comportement,
                    onValueChange = { GameState.updateCharacter(character.copy(comportement = it)) },
                    label = { Text("Comportement") },
                    readOnly = !isMjMode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sheetTextFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = character.intentions,
                    onValueChange = { GameState.updateCharacter(character.copy(intentions = it)) },
                    label = { Text("Intentions") },
                    readOnly = !isMjMode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sheetTextFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = character.objectif,
                    onValueChange = { GameState.updateCharacter(character.copy(objectif = it)) },
                    label = { Text("Objectif") },
                    readOnly = !isMjMode,
                    modifier = Modifier.fillMaxWidth(),
                    colors = sheetTextFieldColors()
                )
            }
        }

        SheetCard {
            Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = character.notes,
                onValueChange = { GameState.updateCharacterNotes(character.id, it) },
                label = { Text("Notes du personnage") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                colors = sheetTextFieldColors()
            )
        }

        // Pour un PNJ, capacités, traits et dons sont dans l'onglet Combat.
        if (!isPnj) {
            CapacitesSection("Capacités de classe", character.classFeatures)
            CapacitesSection("Traits d'espèce", character.traits)
            CapacitesSection("Dons", character.feats)
        }
        ExpandableSection("Apparence", character.appearance, expandedAppearance) { expandedAppearance = !expandedAppearance }

        SheetCard {
            Text("Histoire et personnalité", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "Alignement", value = character.alignment.ifBlank { "—" })
            DetailRow(label = "Historique", value = character.background.ifBlank { "—" })
        }

        SheetCard {
            Text("Langues connues", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                character.languages.joinToString(", ").ifBlank { "—" },
                style = MaterialTheme.typography.bodyMedium,
                color = SheetTextPrimary
            )
        }

        if (character.proficiencies.isNotBlank()) {
            SheetCard {
                Text("Maîtrises", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                // Anciennes fiches : « Outils (Soldat) : Boîte de jeux (cartes) (choix : « … ») » → « Boîte de jeux (cartes) ».
                val maitrises = character.proficiencies.lines()
                    .map { it.substringAfter(" : ").replace(Regex("""\s*\(choix : «[^»]*»\)"""), "").trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
                Text(maitrises, style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
            }
        }
    }
}

/**
 * Section "une capacité par ligne" (comme les sorts connus, cf. SpellRow) : le titre seul
 * est affiché, cliquer déplie sa description juste en dessous. Découpage et rendu dans
 * CapacitesFiche.kt : une description SRD contenant des lignes vides (paragraphes, "###",
 * tables) reste dans sa capacité, et les choix faits apparaissent en vert.
 */
@Composable
private fun CapacitesSection(titre: String, texte: String) {
    val capacites = remember(texte) { parserCapacitesFiche(texte) }
    var expandedNom by remember { mutableStateOf<String?>(null) }
    if (capacites.isNotEmpty()) {
        SheetCard {
            Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                capacites.forEach { capacite ->
                    CapaciteRow(
                        capacite = capacite,
                        expanded = expandedNom == capacite.titre,
                        onClick = { expandedNom = if (expandedNom == capacite.titre) null else capacite.titre }
                    )
                }
            }
        }
    }
}

@Composable
private fun CapaciteRow(capacite: CapaciteFiche, expanded: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SheetSurfaceLight.copy(alpha = 0.75f)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = capacite.description.isNotBlank(), onClick = onClick)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Le nom en blanc, les valeurs choisies (après " — ") en vert.
                val nom = capacite.titre.substringBefore(" — ")
                val valeurs = capacite.titre.substringAfter(" — ", "")
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append(nom)
                        if (valeurs.isNotBlank()) {
                            append(" — ")
                            pushStyle(androidx.compose.ui.text.SpanStyle(color = CouleurChoix))
                            append(valeurs)
                            pop()
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SheetTextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (capacite.description.isNotBlank()) {
                    Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
                }
            }
            if (expanded && capacite.description.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    CapaciteDescription(capacite.description, capacite.choix)
                }
            }
        }
    }
}
@Composable
private fun ExpandableSection(title: String, content: String, expanded: Boolean, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SheetSurface.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, SheetBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary)
                Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodyMedium, color = SheetTextSecondary)
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = content.ifBlank { "—" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (content.isBlank()) SheetTextSecondary else SheetTextPrimary
                    )
                }
            }
        }
    }
}