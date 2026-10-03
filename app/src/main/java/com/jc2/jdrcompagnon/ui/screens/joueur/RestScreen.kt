package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.AppRole
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Harmonisation
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ObjetsACharges
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.EmplacementsDeSortParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.TableMd
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * Écran de repos : repos court (dépense de dés de vie pour récupérer des PV) et
 * repos long (PV et dés de vie restaurés, avance l'horloge de scénario de 8h).
 *
 * Règles D&D 5e simplifiées :
 * - Repos court (~1h) : le joueur choisit combien de dés de vie dépenser (jusqu'au
 *   nombre restant), chaque dé relancé (1..face du dé) + modificateur de
 *   Constitution (minimum 1 par dé) est ajouté aux PV actuels.
 * - Repos long (8h) : PV actuels remis au maximum, la moitié des dés de vie du
 *   personnage (arrondi au supérieur, minimum 1) sont récupérés. Réduit l'épuisement
 *   de 1 si le personnage a mangé le jour du repos.
 * - Faim (règle "Nourriture et eau" du SRD) : un personnage tient 3 + modificateur de
 *   Constitution jours (minimum 1) sans manger ; au-delà, il subit un niveau
 *   d'épuisement par jour supplémentaire (GameState.syncHunger, appelé à l'ouverture
 *   de cet écran, rattrape les jours écoulés depuis le dernier repas).
 * - Objets à charges (ObjetsACharges) : listés avec leurs charges restantes ; le repos long
 *   relance leur formule de recharge (ex. 1d6 + 4 pour le bâton des chants d'oiseaux).
 * - Sorts préparés (Clerc, Druide, Magicien, Paladin) : choisis ici parmi les sorts connus
 *   (SpellPreparationCard), ils alimentent l'action Magie de l'écran de combat.
 * - Harmonisation (Harmonisation) : un repos court peut être consacré à s'harmoniser avec UN
 *   objet, ou à rompre une harmonisation (ShortRestCard) ; HarmonisationCard fait le point.
 * - Magicien : Restauration magique (emplacements récupérés à la fin d'un repos court, une fois
 *   par repos long) et Mémorisation de sort (niv. 5, un sort préparé échangé), dans ShortRestCard.
 */
/** Demande d'ouverture de l'écran de repos depuis n'importe où (halte d'un voyage) : traitée par NavGraph. */
object ReposNavigation {
    private val _demande = kotlinx.coroutines.flow.MutableStateFlow(false)
    val demande: kotlinx.coroutines.flow.StateFlow<Boolean> = _demande

    fun ouvrir() {
        _demande.value = true
    }

    fun consommer() {
        _demande.value = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val characters by GameState.characters.collectAsState()
    val selectedCharacterId by GameState.selectedCharacterId.collectAsState()
    val character = characters.firstOrNull { it.id == selectedCharacterId }
    val appRole by GameState.appRole.collectAsState()
    val isMj = appRole == AppRole.MJ

    LaunchedEffect(character?.id) {
        character?.let { GameState.syncHunger(it.id) }
    }

    // Objets à harmonisation portés par le personnage (cartes Harmonisation et Repos court).
    val context = LocalContext.current
    var aHarmoniser by remember { mutableStateOf<List<EquipmentItem>>(emptyList()) }
    val portes = character?.let { ObjetsACharges.objetsPortes(it) }.orEmpty()
    LaunchedEffect(character?.id, character?.worldId, portes) {
        val c = character ?: return@LaunchedEffect
        val noms = portes.toSet()
        aHarmoniser = SrdRepository.loadEquipmentList(context, c.worldId.ifBlank { GameState.currentWorldId() ?: "donjon_et_dragon" })
            .filter { it.harmonisation && it.name in noms }
            .distinctBy { it.name }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("REPOS") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                },
            )
        }
    ) { padding ->
        if (character == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Sélectionnez d'abord un personnage.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") }
                Text(character.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            HpCard(character)
            HitDiceCard(character)
            HungerCard(character)
            ExhaustionCard(character, isMj = isMj)
            ChargedItemsCard(character)
            HarmonisationCard(character, aHarmoniser, isMj = isMj)
            ShortRestCard(character, aHarmoniser)
            LongRestCard(character)
            WeaponMasteryCard(character)
            SpellPreparationCard(character)
        }
    }
}

@Composable
private fun HpCard(character: Character) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Points de vie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            val ratio = if (character.maxHitPoints > 0) {
                (character.currentHitPoints.toFloat() / character.maxHitPoints).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(8.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "${character.currentHitPoints} / ${character.maxHitPoints}" +
                    if (character.temporaryHitPoints > 0) " (+${character.temporaryHitPoints} temporaires)" else "",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HitDiceCard(character: Character) {
    val dieFaces = hitDieForClass(character.characterClass)
    val remaining = (character.level - character.hitDiceUsed).coerceIn(0, character.level)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Dés de vie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("D$dieFaces · $remaining / ${character.level} disponibles", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HungerCard(character: Character) {
    val scenarioMinutes by ScenarioClockState.state.collectAsState()
    val currentDay = ScenarioClockState.dayIndex(scenarioMinutes.scenarioMinutes)
    val daysSinceLastMeal = (currentDay - character.lastMealDay).coerceAtLeast(0)
    val tolerance = GameState.foodToleranceDays(character)
    val isStarving = daysSinceLastMeal > tolerance
    val hasRation = GameState.hasRation(character)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = if (isStarving) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Faim", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            val statusText = when {
                daysSinceLastMeal == 0L -> "A mangé aujourd'hui."
                daysSinceLastMeal <= tolerance -> "$daysSinceLastMeal jour(s) sans manger (tient $tolerance jour(s) sans effet)."
                else -> "$daysSinceLastMeal jour(s) sans manger — au-delà de la limite de $tolerance jour(s), un niveau d'épuisement est subi chaque jour."
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isStarving) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { GameState.eatRation(character.id) }, enabled = hasRation) {
                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Manger une ration")
            }
            if (!hasRation) {
                Text(
                    "Aucune ration dans l'inventaire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun exhaustionEffectLabel(level: Int): String = when (level) {
    1 -> "Désavantage sur les tests de caractéristique."
    2 -> "Vitesse réduite de moitié."
    3 -> "Désavantage aux jets d'attaque et de sauvegarde."
    4 -> "Points de vie maximum réduits de moitié."
    5 -> "Vitesse réduite à 0."
    6 -> "Décès."
    else -> "Aucun effet."
}

@Composable
private fun ExhaustionCard(character: Character, isMj: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (character.exhaustionLevel > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Épuisement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Niveau ${character.exhaustionLevel} / 6",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (character.exhaustionLevel > 0) {
                Spacer(Modifier.height(4.dp))
                (1..character.exhaustionLevel).forEach { level ->
                    Text(
                        "• Niveau $level : ${exhaustionEffectLabel(level)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isMj) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { GameState.setExhaustionLevel(character.id, character.exhaustionLevel - 1) },
                        enabled = character.exhaustionLevel > 0,
                    ) { Text("−") }
                    Text("Ajuster (MJ)", modifier = Modifier.padding(horizontal = 12.dp))
                    TextButton(
                        onClick = { GameState.setExhaustionLevel(character.id, character.exhaustionLevel + 1) },
                        enabled = character.exhaustionLevel < 6,
                    ) { Text("+") }
                }
            }
        }
    }
}

/** Harmonisation choisie pour un repos court : s'harmoniser avec [objet], ou rompre le lien. */
private data class ActionHarmonisation(val objet: String, val rompre: Boolean)

@Composable
private fun ShortRestCard(character: Character, aHarmoniser: List<EquipmentItem>) {
    val dieFaces = hitDieForClass(character.characterClass)
    val remaining = (character.level - character.hitDiceUsed).coerceIn(0, character.level)
    var diceToSpend by remember(character.id) { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    // Un seul objet par repos court (règle d'harmonisation).
    var harmonisation by remember(character.id) { mutableStateOf<ActionHarmonisation?>(null) }
    val options = aHarmoniser.filter { !Harmonisation.estHarmonise(character, it.name) }
        .map { ActionHarmonisation(it.name, rompre = false) to Harmonisation.verifier(character, it) } +
        character.attunedItems.map { ActionHarmonisation(it, rompre = true) to null }
    // Magicien — Restauration magique : emplacements à récupérer à la fin de ce repos court
    // (niveau de sort → nombre), somme des niveaux bornée au budget.
    val budgetRestauration = ArsenalPersonnage.budgetRestaurationMagique(character)
    var restauration by remember(character.id) { mutableStateOf(mapOf<Int, Int>()) }
    // Magicien niv. 5 — Mémorisation de sort : (sort préparé retiré, sort du grimoire préparé à sa place).
    val memorisationPossible = ArsenalPersonnage.niveauMagicien(character) >= ArsenalPersonnage.NIVEAU_MEMORISATION_DE_SORT
    var memorisation by remember(character.id) { mutableStateOf<Pair<String?, String?>>(null to null) }
    val echange = memorisation.takeIf { (retire, ajoute) -> retire != null && ajoute != null }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Repos court (≈1h)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Dépensez des dés de vie pour récupérer des PV : 1D$dieFaces + modificateur de Constitution par dé.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (diceToSpend > 0) diceToSpend-- }, enabled = diceToSpend > 0) { Text("−") }
                Text("$diceToSpend dé(s)", modifier = Modifier.padding(horizontal = 12.dp))
                TextButton(onClick = { if (diceToSpend < remaining) diceToSpend++ }, enabled = diceToSpend < remaining) { Text("+") }
            }
            if (options.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Harmonisation (un seul objet par repos court)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                listOf<Pair<ActionHarmonisation?, String?>>(null to null).plus(options).forEach { (action, blocage) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = harmonisation == action,
                            enabled = blocage == null,
                            onClick = { harmonisation = action },
                        )
                        Column {
                            Text(
                                when {
                                    action == null -> "Aucune"
                                    action.rompre -> "Rompre l'harmonisation : ${action.objet}"
                                    else -> "S'harmoniser : ${action.objet}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            blocage?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            if (budgetRestauration > 0) {
                Spacer(Modifier.height(8.dp))
                RestaurationMagiqueSection(character, budgetRestauration, restauration) { restauration = it }
            }
            if (memorisationPossible) {
                Spacer(Modifier.height(8.dp))
                MemorisationDeSortSection(character, memorisation) { memorisation = it }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val conMod = GameState.abilityModifier(character.constitution)
                    var healed = 0
                    repeat(diceToSpend) {
                        val roll = (1..dieFaces).random()
                        healed += (roll + conMod).coerceAtLeast(1)
                        GameState.spendHitDie(character.id)
                    }
                    if (diceToSpend > 0) GameState.updateCharacterHp(character.id, character.currentHitPoints + healed)
                    // Magie de pacte (Occultiste) récupère au repos court, contrairement aux
                    // emplacements de sort classiques (repos long uniquement, cf. LongRestCard).
                    GameState.resetPactSlots(character.id)
                    // Sorts de prédilection (Magicien niv. 20) : récupérés au repos court comme au repos long.
                    GameState.recupererSortsPredilection(character.id)
                    val lienHarmonisation = harmonisation?.let { action ->
                        if (action.rompre) {
                            GameState.rompreHarmonisation(character.id, action.objet)
                            " Harmonisation rompue avec ${action.objet}."
                        } else {
                            GameState.harmoniser(character.id, action.objet)
                            " ${character.name} est désormais harmonisé avec ${action.objet}."
                        }
                    }.orEmpty()
                    val texteRestauration = restauration.takeIf { it.isNotEmpty() }?.let { recuperes ->
                        GameState.restaurationMagique(character.id, recuperes)
                        " Restauration magique : " + recuperes.toSortedMap().entries.joinToString(", ") { (n, k) -> "$k emplacement(s) niv. $n" } + " récupéré(s)."
                    }.orEmpty()
                    val texteMemorisation = echange?.let { (retire, ajoute) ->
                        val prepares = character.preparedSpells.orEmpty()
                        GameState.setPreparedSpells(character.id, prepares.filterNot { it == retire } + ajoute!!)
                        " Mémorisation de sort : $ajoute préparé à la place de $retire."
                    }.orEmpty()
                    ScenarioClockState.advanceManually(60L)
                    result = "Repos court : +$healed PV récupérés ($diceToSpend dé(s) dépensé(s))." + lienHarmonisation +
                        texteRestauration + texteMemorisation
                    diceToSpend = 0
                    harmonisation = null
                    restauration = emptyMap()
                    memorisation = null to null
                },
                enabled = diceToSpend > 0 || harmonisation != null || restauration.isNotEmpty() || echange != null,
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Prendre un repos court")
            }
            result?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Magicien — Restauration magique (une fois par repos long) : à la fin du repos court, récupère
 * des emplacements dépensés dont la somme des niveaux ne dépasse pas [budget], aucun du 6e niveau
 * ou plus. [choix] = niveau de sort → nombre d'emplacements à récupérer.
 */
@Composable
private fun RestaurationMagiqueSection(character: Character, budget: Int, choix: Map<Int, Int>, onChoix: (Map<Int, Int>) -> Unit) {
    val cout = choix.entries.sumOf { (niveau, n) -> niveau * n }
    Text("Restauration magique (Magicien)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    if (character.arcaneRecoveryUsed) {
        Text(
            "Déjà utilisée depuis le dernier repos long.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Text(
        "Récupérez des emplacements dépensés à la fin de ce repos court : somme des niveaux ≤ $budget, " +
            "aucun du 6e niveau ou plus. Une fois par repos long. Niveaux choisis : $cout / $budget.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val depenses = (1..minOf(budget, ArsenalPersonnage.NIVEAU_MAX_RESTAURATION_MAGIQUE))
        .associateWith { character.spellSlotsUsed[it] ?: 0 }
        .filterValues { it > 0 }
    if (depenses.isEmpty()) {
        Text("Aucun emplacement récupérable n'a été dépensé.", style = MaterialTheme.typography.bodySmall)
        return
    }
    depenses.forEach { (niveau, utilises) ->
        val n = choix[niveau] ?: 0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Niv. $niveau ($utilises dépensé(s))", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(
                onClick = { onChoix((choix + (niveau to n - 1)).filterValues { it > 0 }) },
                enabled = n > 0,
            ) { Text("−") }
            Text("$n", modifier = Modifier.padding(horizontal = 8.dp))
            TextButton(
                onClick = { onChoix(choix + (niveau to n + 1)) },
                enabled = n < utilises && cout + niveau <= budget,
            ) { Text("+") }
        }
    }
}

/**
 * Magicien niv. 5 — Mémorisation de sort : à la fin du repos court, un sort préparé du Magicien
 * est remplacé par un autre sort de niveau 1+ de son grimoire. [choix] = (retiré, ajouté).
 */
@Composable
private fun MemorisationDeSortSection(character: Character, choix: Pair<String?, String?>, onChoix: (Pair<String?, String?>) -> Unit) {
    val context = LocalContext.current
    val monde = character.worldId.ifBlank { "donjon_et_dragon" }
    var niveaux by remember(monde) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    LaunchedEffect(monde) {
        niveaux = SrdRepository.loadSpells(context, monde).associate { it.name.lowercase() to ArsenalPersonnage.niveauDepuisLibelle(it.niveauSort) }
    }
    Text("Mémorisation de sort (Magicien)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    val prepares = character.preparedSpells
    if (prepares == null) {
        Text(
            "Enregistrez d'abord votre préparation (carte « Sorts préparés ») pour pouvoir échanger un sort.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    fun duGrimoire(s: String) = (character.spellClasses[s] ?: character.characterClass).equals("Magicien", ignoreCase = true) &&
        (niveaux[s.lowercase()] ?: 1) >= 1
    val retirables = prepares.filter(::duGrimoire).sorted()
    val ajoutables = character.spells.distinct().filter { s -> duGrimoire(s) && prepares.none { it == s } }
        .sortedWith(compareBy({ niveaux[it.lowercase()] ?: 1 }, { it }))
    Text(
        "Remplacez un sort préparé par un autre sort de votre grimoire à la fin de ce repos court.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (retirables.isEmpty() || ajoutables.isEmpty()) {
        Text("Aucun échange possible (grimoire entièrement préparé ou aucun sort préparé).", style = MaterialTheme.typography.bodySmall)
        return
    }
    fun libelle(s: String) = "$s (niv. ${niveaux[s.lowercase()] ?: "?"})"
    ChoixDeroulant("Retirer", choix.first?.let(::libelle), retirables.map { it to libelle(it) }) { onChoix(it to choix.second) }
    ChoixDeroulant("Préparer", choix.second?.let(::libelle), ajoutables.map { it to libelle(it) }) { onChoix(choix.first to it) }
    if (choix.first != null || choix.second != null) {
        TextButton(onClick = { onChoix(null to null) }) { Text("Annuler l'échange") }
    }
}

/** Bouton qui ouvre un menu déroulant d'[options] (valeur → libellé). */
@Composable
private fun ChoixDeroulant(titre: String, choisi: String?, options: List<Pair<String, String>>, onChoix: (String) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$titre :", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        Box {
            TextButton(onClick = { ouvert = true }) { Text(choisi ?: "Choisir…") }
            DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                options.forEach { (valeur, libelle) ->
                    DropdownMenuItem(text = { Text(libelle) }, onClick = { onChoix(valeur); ouvert = false })
                }
            }
        }
    }
}

/**
 * Objets harmonisés (n / maximum) et objets à harmonisation portés sans l'être. Un prérequis
 * perdu (ex. plus magicien) rompt l'harmonisation ; le MJ peut aussi la rompre directement (mort
 * du personnage, objet éloigné de plus de 30 m pendant 24 h, harmonisation par un tiers).
 */
@Composable
private fun HarmonisationCard(character: Character, aHarmoniser: List<EquipmentItem>, isMj: Boolean) {
    if (aHarmoniser.isEmpty() && character.attunedItems.isEmpty()) return
    val parNom = aHarmoniser.associateBy { it.name }
    LaunchedEffect(character.attunedItems, aHarmoniser) {
        character.attunedItems
            .filter { nom -> parNom[nom]?.let { Harmonisation.prerequisRempli(character, it) } == false }
            .forEach { GameState.rompreHarmonisation(character.id, it) }
    }
    val maximum = Harmonisation.maximum(character)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Harmonisation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "${character.attunedItems.size} / $maximum objet(s) harmonisé(s)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Un objet à harmonisation n'agit que pour un porteur harmonisé. S'harmoniser (ou rompre le lien) " +
                    "demande un repos court consacré à l'objet, un seul par repos court. L'harmonisation prend fin si " +
                    "l'objet quitte l'inventaire ou si un prérequis n'est plus rempli.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            character.attunedItems.forEach { nom ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("• $nom", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (isMj) {
                        TextButton(onClick = { GameState.rompreHarmonisation(character.id, nom) }) { Text("Rompre (MJ)") }
                    }
                }
            }
            aHarmoniser.filter { !Harmonisation.estHarmonise(character, it.name) }.forEach { item ->
                Text(
                    "○ ${item.name} — ${Harmonisation.libelle(item).lowercase()}, non harmonisé" +
                        if (Harmonisation.prerequisRempli(character, item) == null) " (prérequis à vérifier par le MJ)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isMj && character.attunedItems.isNotEmpty()) {
                Text(
                    "MJ : rompez l'harmonisation à la mort du personnage, si l'objet reste à plus de 30 m pendant 24 h " +
                        "ou si une autre créature s'y harmonise.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Objets à charges portés par le personnage : charges restantes et recharge au repos long. */
@Composable
private fun ChargedItemsCard(character: Character) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<EquipmentItem>>(emptyList()) }
    val portes = ObjetsACharges.objetsPortes(character)
    LaunchedEffect(character.id, portes) {
        items = ObjetsACharges.objetsDuPersonnage(context, character)
    }
    if (items.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Objets à charges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items.forEach { item ->
                val max = item.charges ?: 0
                val restantes = ObjetsACharges.restantes(character, item)
                Spacer(Modifier.height(8.dp))
                Text("${item.name} · $restantes / $max charges", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { if (max > 0) restantes.toFloat() / max else 0f },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                )
                if (item.recharge.isNotBlank()) {
                    Text(
                        "Repos long : récupère ${item.recharge} charge(s)." +
                            if (item.destruction != null) " Dernière charge : d20, détruit sur ${item.destruction}." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Repos long d'un personnage (voir LongRestCard) : PV, dés de vie, emplacements de sort,
 * épuisement si le personnage a mangé, objets à charges. [avancerHorloge] faux quand le MJ a
 * déjà avancé l'horloge pour tout le groupe (halte sur un trajet). [onResultat] reçoit le résumé,
 * puis à nouveau le résumé complété des recharges d'objets.
 */
fun effectuerReposLong(
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    character: Character,
    avancerHorloge: Boolean = true,
    onResultat: (String) -> Unit = {},
) {
    val currentDay = ScenarioClockState.dayIndex(ScenarioClockState.state.value.scenarioMinutes)
    val ateToday = character.lastMealDay >= currentDay
    val recovered = ceil(character.level / 2.0).toInt().coerceAtLeast(1)
    GameState.updateCharacterHp(character.id, character.maxHitPoints)
    GameState.recoverHitDice(character.id, recovered)
    GameState.resetSpellSlots(character.id)
    if (ateToday && character.exhaustionLevel > 0) {
        GameState.setExhaustionLevel(character.id, character.exhaustionLevel - 1)
    }
    if (avancerHorloge) ScenarioClockState.advanceManually(8L * 60)
    val resume = "Repos long effectué : PV au maximum, $recovered dé(s) de vie récupéré(s), emplacements de sort restaurés." +
        (if (ArsenalPersonnage.reservePuissanceCurative(character) > 0) " Puissance curative restaurée." else "") +
        (if (ArsenalPersonnage.aChatimentDePaladin(character)) " Châtiment divin gratuit de nouveau disponible." else "") +
        (if (ArsenalPersonnage.niveauMagicien(character) > 0) " Restauration magique de nouveau disponible." else "") +
        (if (character.sortsSpeciaux.containsValue(ArsenalPersonnage.SORT_PREDILECTION)) " Sorts de prédilection de nouveau disponibles." else "") +
        (if (ArsenalPersonnage.nombreBottes(character) > 0) " Vous pouvez changer vos bottes d'arme dans l'écran Repos." else "") +
        (if (ateToday && character.exhaustionLevel > 0) " Épuisement réduit de 1." else "") +
        if (ArsenalPersonnage.classesQuiPreparent.any { it.equals(character.characterClass, ignoreCase = true) })
            " Vous pouvez changer vos sorts préparés dans l'écran Repos." else ""
    onResultat(resume)
    // Objets à charges : chacun relance sa formule de recharge.
    scope.launch {
        val recharges = ObjetsACharges.rechargerAuReposLong(context, character)
        if (recharges.isNotEmpty()) onResultat(resume + "\n" + recharges.joinToString("\n"))
    }
}

@Composable
private fun LongRestCard(character: Character) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirm by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Repos long (8h)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Restaure tous les PV, la moitié des dés de vie (arrondi au supérieur), les emplacements de sort et les " +
                    "ressources de classe (Puissance curative, Châtiment divin gratuit…). Réduit l'épuisement de 1 " +
                    "si le personnage a mangé aujourd'hui. Recharge les objets à charges. Avance l'horloge de scénario de 8h.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Nightlight, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Prendre un repos long")
            }
            result?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Repos long") },
            text = { Text("${character.name} se repose 8 heures. PV et dés de vie récupérés, l'horloge de scénario avance de 8h.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        effectuerReposLong(context, scope, character) { result = it }
                        showConfirm = false
                    }
                ) { Text("Confirmer") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Annuler") } }
        )
    }
}

/**
 * Bottes d'arme (Maîtrise des armes : Paladin, Guerrier, Barbare, Rôdeur, Roublard) : le
 * personnage choisit les armes dont il utilise la botte, modifiables à chaque repos long. Le
 * Paladin commence avec l'épée longue et la javeline (cf. ArsenalPersonnage.bottesParDefaut).
 */
@Composable
private fun WeaponMasteryCard(character: Character) {
    val nombre = ArsenalPersonnage.nombreBottes(character)
    if (nombre <= 0) return
    val context = LocalContext.current
    val monde = character.worldId.ifBlank { "donjon_et_dragon" }
    var armes by remember(monde) { mutableStateOf<List<ArmeSrd>>(emptyList()) }
    LaunchedEffect(monde) {
        armes = SrdRepository.loadEquipmentList(context, monde)
            .filter { it.damage.isNotBlank() }
            .map { ArmeSrd.depuisFiche(it.name, it.damage, it.properties, it.rawMarkdown) }
            .filter { it.botte != null }
    }
    val maitrisees = armes.filter { ArsenalPersonnage.maitrise(character, it) }.sortedBy { it.nom }
    val actives = ArsenalPersonnage.bottesActives(character)
    var selection by remember(character.id, character.weaponMasteries, actives) { mutableStateOf(actives.orEmpty().toSet()) }
    var message by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsMartialArts, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bottes d'arme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Choisissez $nombre armes maîtrisées dont vous utilisez la botte. Modifiable à la fin de chaque repos long.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${selection.size} / $nombre : " + selection.sorted().joinToString(", ").ifBlank { "aucune" },
                fontWeight = FontWeight.Bold
            )
            if (armes.isEmpty()) {
                Text("Chargement des armes…", style = MaterialTheme.typography.bodySmall)
            }
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                maitrisees.forEach { arme ->
                    val coche = selection.any { it.equals(arme.nom, ignoreCase = true) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = coche,
                            enabled = coche || selection.size < nombre,
                            onCheckedChange = { c ->
                                selection = if (c) selection + arme.nom else selection.filterNot { it.equals(arme.nom, ignoreCase = true) }.toSet()
                            }
                        )
                        Text("${arme.nom} — botte ${arme.botte}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            TextButton(
                enabled = selection.isNotEmpty() && selection.size <= nombre,
                onClick = {
                    GameState.setWeaponMasteries(character.id, selection.toList())
                    message = "Bottes enregistrées : ${selection.sorted().joinToString(", ")}."
                }
            ) { Text("Enregistrer les bottes") }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

/**
 * Préparation des sorts (Clerc, Druide, Magicien, Paladin) : après un repos long, le personnage
 * choisit parmi ses sorts connus ceux qu'il a prêts, dans la limite « Sorts préparés » de sa
 * classe. Les sorts mineurs sont toujours prêts. Les autres lanceurs (Barde, Ensorceleur,
 * Occultiste, Rôdeur) ont tous leurs sorts connus prêts : pas de carte pour eux.
 */
@Composable
private fun SpellPreparationCard(character: Character) {
    val context = LocalContext.current
    val monde = character.worldId.ifBlank { "donjon_et_dragon" }
    var pistes by remember(monde) { mutableStateOf<List<PisteIncantation>?>(null) }
    var niveaux by remember(monde) { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var classesSrd by remember(monde) { mutableStateOf<List<Classe>>(emptyList()) }
    var table by remember(monde) { mutableStateOf<TableMd?>(null) }
    LaunchedEffect(monde) {
        val md = SrdRepository.loadClasses(context, monde).joinToString("\n\n") { it.rawMarkdown }
        classesSrd = ClasseParser.parse(md)
        table = EmplacementsDeSortParser.parse(md)
        niveaux = SrdRepository.loadSpells(context, monde).associate { it.name to ArsenalPersonnage.niveauDepuisLibelle(it.niveauSort) }
    }
    LaunchedEffect(character.level, character.characterClass, character.classesSecondaires, classesSrd, table) {
        if (classesSrd.isNotEmpty()) pistes = calculerPistesIncantation(character, classesSrd, table)
    }
    val aPreparer = pistes.orEmpty().filter { p -> ArsenalPersonnage.classesQuiPreparent.any { it.equals(p.classe.nom, ignoreCase = true) } }
    if (aPreparer.isEmpty()) return

    // Toujours préparés (Châtiment divin du Paladin, sorts de sous-classe) : hors de la limite, jamais à cocher.
    val toujoursPrepares = sortsDeSousClasse(character, classesSrd) + ArsenalPersonnage.SORT_CHATIMENT_DIVIN +
        ArsenalPersonnage.sortsToujoursPreparesClasse(character)

    // Sorts de niveau 1+ connus pour chaque classe qui prépare.
    fun sortsDe(piste: PisteIncantation) = character.spells.distinct().filter { s ->
        (character.spellClasses[s] ?: character.characterClass).equals(piste.classe.nom, ignoreCase = true) &&
            (niveaux[s] ?: 1) in 1..piste.maxNiveauSort &&
            toujoursPrepares.none { it.equals(s, ignoreCase = true) }
    }.sortedWith(compareBy({ niveaux[it] ?: 1 }, { it }))
    val candidats = aPreparer.flatMap(::sortsDe).toSet()
    var selection by remember(character.id, character.preparedSpells, candidats) {
        mutableStateOf((character.preparedSpells?.toSet() ?: candidats).intersect(candidats))
    }
    var message by remember { mutableStateOf<String?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sorts préparés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "À la fin d'un repos long, choisissez les sorts prêts à être lancés. Seuls ceux-ci apparaissent dans " +
                    "l'action Magie de l'écran de combat. Les sorts mineurs sont toujours prêts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (character.preparedSpells == null) {
                Text(
                    "Aucune préparation enregistrée : pour l'instant, tous vos sorts connus comptent comme préparés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            aPreparer.forEach { piste ->
                val sorts = sortsDe(piste)
                val max = piste.sortsPreparesMax
                val choisis = sorts.count { it in selection }
                Spacer(Modifier.height(4.dp))
                Text(
                    (if (aPreparer.size > 1) "${piste.classe.nom} — " else "") + "$choisis" + (max?.let { " / $it" } ?: "") + " préparé(s)",
                    fontWeight = FontWeight.Bold,
                    color = if (max != null && choisis > max) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                if (sorts.isEmpty()) {
                    Text("Aucun sort de niveau 1 ou plus connu : ajoutez-en depuis l'onglet Sorts de la fiche.", style = MaterialTheme.typography.bodySmall)
                }
                sorts.forEach { sort ->
                    val coche = sort in selection
                    val complet = max != null && choisis >= max && !coche
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = coche,
                            enabled = !complet,
                            onCheckedChange = { selection = if (it) selection + sort else selection - sort }
                        )
                        Text("$sort (niv. ${niveaux[sort] ?: "?"})", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            TextButton(
                enabled = aPreparer.none { p -> p.sortsPreparesMax?.let { max -> sortsDe(p).count { it in selection } > max } == true },
                onClick = {
                    GameState.setPreparedSpells(character.id, selection.toList())
                    message = "${selection.size} sort(s) préparé(s)."
                }
            ) { Text("Enregistrer la préparation") }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        }
    }
}
