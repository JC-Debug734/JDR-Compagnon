package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortPret
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.decomposerFormule
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.network.CombatJoueurData
import com.jc2.jdrcompagnon.network.CombattantJoueurData
import com.jc2.jdrcompagnon.network.DeclarationJoueurData
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Consommables
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

private val FondCarte @Composable get() = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)

/**
 * Écran de combat du joueur (partie en réseau). Pendant la déclaration, il choisit son action
 * (le joueur y est amené automatiquement, cf. NavGraph) ; pendant la résolution, il suit l'ordre
 * d'initiative et envoie ses jets au MJ — valeur de ses vrais dés, ou tirage par l'appli.
 * Les consommables équipés (potions...) sont proposés comme actions ; l'objet est utilisé pour de
 * bon quand vient le tour du joueur (soins et effet à durée appliqués à sa fiche, synchronisée
 * avec le MJ).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatJoueurScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) = TexteBlancCombat { CombatJoueurScreenInterne(onBack, onOpenMenu) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombatJoueurScreenInterne(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val combat by NetworkSessionManager.combatEnCours.collectAsState()
    val courantCombat = combat
    // Réouverture du choix : clé par round pour repartir fermé à chaque nouveau round.
    var enModification by rememberSaveable(courantCombat?.id, courantCombat?.round) { mutableStateOf(false) }
    var actionChoisie by rememberSaveable(courantCombat?.id, courantCombat?.round) { mutableStateOf<String?>(null) }
    var armePreselectionnee by rememberSaveable(courantCombat?.id, courantCombat?.round) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val personnages by GameState.characters.collectAsState()
    val monPersonnageId by NetworkSessionManager.claimedCharacterId.collectAsState()
    val monPersonnage = personnages.firstOrNull { it.id == monPersonnageId }
    var consommables by remember { mutableStateOf<List<EquipmentItem>>(emptyList()) }
    LaunchedEffect(monPersonnage?.equippedSlots, monPersonnage?.equippedItems, monPersonnage?.backpackExteriorSlots) {
        consommables = monPersonnage?.let { Consommables.equipesDuPersonnage(context, it) }.orEmpty()
    }
    // Armes en main, capacités d'attaque et sorts prêts, calculés depuis la fiche.
    val arsenal = rememberArsenal(monPersonnage)

    // Déplacement du round, réglé sur l'écran des actions (repris d'une déclaration précédente).
    var deplacement by remember(courantCombat?.id, courantCombat?.round) {
        mutableStateOf(
            courantCombat?.maDeclaration?.let { d ->
                val visee = d.distanceVisee?.let { runCatching { Distance.valueOf(it) }.getOrNull() }
                if (d.deplacementCibleId != null && visee != null) DeplacementJoueur(d.deplacementCibleId, visee) else null
            }
        )
    }

    // Déclaration : le joueur choisit sur l'écran Actions de Combat (bascule Tactique en haut à
    // droite, déplacement en bas) ; toucher une action ouvre l'assistant (détail puis cible).
    val moiDeclaration = courantCombat?.ordre?.firstOrNull { it.id == courantCombat.monCombattantId }
    // États du personnage (libellés reçus du MJ) : Neutralisé & co. empêchent de déclarer, vitesse 0 de bouger.
    val mesEtats = moiDeclaration?.conditions.orEmpty().mapNotNull(Etats::reconnaitre).toSet()
    val immobilise = Etats.immobilisant(mesEtats)
    if (courantCombat != null && moiDeclaration != null && courantCombat.phase == PhaseCombat.DECLARATION.name &&
        Etats.peutAgir(mesEtats) && (courantCombat.maDeclaration == null || enModification)
    ) {
        // Action touchée : l'assistant (détail puis cible) remplace l'écran des actions.
        val actionEnCours = actionChoisie?.let { id -> ActionsCombat.toutes.firstOrNull { it.id == id } }
        if (actionEnCours != null) {
            AssistantDeclaration(
                combat = courantCombat,
                moi = moiDeclaration,
                action = actionEnCours,
                arsenal = arsenal,
                personnage = monPersonnage,
                consommables = consommables,
                initiale = courantCombat.maDeclaration?.takeIf { it.actionId == actionEnCours.id },
                armeInitiale = armePreselectionnee,
                onValider = { d ->
                    NetworkSessionManager.declarerActionCombat(
                        d.libelle, d.actionId, d.cible?.id,
                        // Sort de zone : toutes les cibles nommées (« Gobelin 1, Gobelin 2 »).
                        d.ciblesZone.takeIf { it.size > 1 }?.joinToString { it.nom } ?: d.cible?.nom,
                        d.detail,
                        deplacement?.cibleId, deplacement?.visee?.name, d.attaques,
                        ciblesZone = d.ciblesZone.map { it.id },
                        zone = d.zone,
                    )
                    enModification = false
                    actionChoisie = null
                },
                onFermer = { actionChoisie = null },
            )
            return
        }
        CombatActionsScreen(
            onBack = onBack,
            onOpenMenu = onOpenMenu,
            titre = "Round ${courantCombat.round} — votre action",
            onChoisirAction = { actionChoisie = it.id; armePreselectionnee = null },
            personnageId = monPersonnage?.id,
            // Carte d'une arme : action Attaquer avec cette arme déjà choisie.
            onChoisirArme = { arme ->
                armePreselectionnee = arme.nom
                actionChoisie = "attaquer"
            },
            tactique = { TableauTactique(courantCombat, moiDeclaration.id) },
            sectionDeplacement = {
                if (immobilise != null) {
                    CarteInfo("${immobilise.label} : votre vitesse est de 0, vous ne pouvez pas vous déplacer ce round.")
                } else {
                    CarteDeplacement(
                        combat = courantCombat,
                        moiId = moiDeclaration.id,
                        deplacement = deplacement,
                        desengage = courantCombat.maDeclaration?.actionId == "disengage",
                        onChange = { deplacement = it },
                    )
                }
            },
        )
        return
    }

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = Color.Transparent,
        contentColor = Color.White,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("COMBAT", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
                IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = Color.White)
                }
            }
        },
    ) { padding ->
        val courant = combat
        if (courant == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Aucun combat en cours.", color = Color.White)
                Text("Le MJ vous amènera ici au début du prochain combat.", style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
            return@Scaffold
        }
        ContenuCombatJoueur(
            combat = courant,
            mesEtats = mesEtats,
            modifier = Modifier.fillMaxSize().padding(padding),
            monPersonnage = monPersonnage,
            consommables = consommables,
            arsenal = arsenal,
            onChangerAction = { enModification = true },
        )
    }
}

@Composable
private fun ContenuCombatJoueur(
    combat: CombatJoueurData,
    mesEtats: Set<ConditionCombat>,
    modifier: Modifier,
    monPersonnage: Character?,
    consommables: List<EquipmentItem>,
    arsenal: ArsenalJoueur?,
    onChangerAction: () -> Unit,
) {
    val phase = runCatching { PhaseCombat.valueOf(combat.phase) }.getOrDefault(PhaseCombat.PREPARATION)
    val moi = combat.ordre.firstOrNull { it.id == combat.monCombattantId }
    val actif = combat.ordre.firstOrNull { it.id == combat.actifId }
    val sortAnnonce = combat.maDeclaration?.libelle?.let { sortRegex.find(it) }
    val sortPretAnnonce = sortAnnonce?.let { m -> arsenal?.sorts?.firstOrNull { it.nom == m.groupValues[1] } }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(combat.titre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                if (combat.round > 0) "Round ${combat.round} · ${phase.label}" else phase.label,
                style = MaterialTheme.typography.bodyMedium,
                color = ForcedDarkPalette.AccentGold,
            )
        }

        // États subis (Paralysé, Agrippé…) et ce qu'ils imposent, rappelés au joueur.
        if (moi != null && mesEtats.isNotEmpty()) item {
            Card(colors = CardDefaults.cardColors(containerColor = FondCarte, contentColor = Color.White)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "États : " + mesEtats.sortedBy { it.ordinal }.joinToString { it.label },
                        fontWeight = FontWeight.Bold,
                        color = ForcedDarkPalette.AccentGold,
                    )
                    ResumeEffetsEtats(mesEtats, couleur = Color.White)
                }
            }
        }

        when {
            moi == null -> item {
                CarteInfo("Votre personnage ne participe pas à ce combat.")
            }
            phase == PhaseCombat.PREPARATION -> item {
                CarteInfo("Le MJ prépare le combat (initiative). Vous choisirez votre action dès qu'il le lancera.")
            }
            phase == PhaseCombat.DECLARATION -> item {
                Card(colors = CardDefaults.cardColors(containerColor = FondCarte, contentColor = Color.White)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Votre action", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                        combat.maDeclaration?.let { TexteDeclaration(it) }
                            ?: Text("Aucune action : vous ne pouvez pas agir ce round.", color = Color.White)
                        LinearProgressIndicator(
                            progress = { if (combat.nbAttendus > 0) combat.nbDeclares.toFloat() / combat.nbAttendus else 0f },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        )
                        Text(
                            "${combat.nbDeclares}/${combat.nbAttendus} combattants ont choisi. Le MJ résoudra ensuite les actions dans l'ordre d'initiative.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        // Neutralisé : l'« Aucune action » imposée par le MJ ne se change pas.
                        if (Etats.peutAgir(mesEtats)) TextButton(onClick = onChangerAction) { Text("Changer d'action") }
                    }
                }
            }
            else -> {
                item {
                    val monTour = actif?.id == moi.id
                    Card(
                        modifier = if (monTour) Modifier.border(2.dp, ForcedDarkPalette.AccentGold, MaterialTheme.shapes.medium) else Modifier,
                        colors = CardDefaults.cardColors(containerColor = FondCarte, contentColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Seul le tour du joueur est signalé : l'ordre du tour est suivi à la table.
                            if (monTour) {
                                Text(
                                    "C'est votre tour ! Le MJ décrit la scène, lancez vos dés quand il vous le demande.",
                                    fontWeight = FontWeight.Bold,
                                    color = ForcedDarkPalette.AccentGold,
                                )
                            }
                            combat.maDeclaration?.let {
                                Text("Votre action :", style = MaterialTheme.typography.bodySmall)
                                TexteDeclaration(it, sansResume = true)
                            }
                            val objetAnnonce = combat.maDeclaration?.detail
                                ?.takeIf { it.startsWith(PREFIXE_OBJET) }
                                ?.removePrefix(PREFIXE_OBJET)?.substringBefore(" — ")
                                ?.let { nom -> consommables.firstOrNull { it.name == nom } }
                            if (monTour && objetAnnonce != null && monPersonnage != null) {
                                UtiliserObjetAnnonce(combat.id, combat.round, objetAnnonce) {
                                    Consommables.utiliser(monPersonnage, objetAnnonce)
                                }
                            }
                            val niveauEmplacement = sortAnnonce?.groupValues?.get(2)?.toIntOrNull()
                            if (monTour && niveauEmplacement != null && monPersonnage != null) {
                                DepenserEmplacement(combat.id, combat.round, niveauEmplacement, monPersonnage, arsenal)
                            }
                            // Sort de prédilection lancé sans emplacement : une fois par repos court ou long.
                            val sansEmplacement = sortAnnonce?.groupValues?.get(3)?.isNotEmpty() == true
                            if (monTour && sansEmplacement && monPersonnage != null &&
                                sortPretAnnonce?.lancementGratuit == ArsenalPersonnage.SORT_PREDILECTION
                            ) {
                                UtiliserPredilection(combat.id, combat.round, sortPretAnnonce, monPersonnage)
                            }
                        }
                    }
                }
                item {
                    val d = combat.maDeclaration
                    // Attaques déclarées dans l'ordre (déclaration d'avant l'Attaque supplémentaire : une seule).
                    val attaques = d?.attaques?.ifEmpty { null }
                        ?: listOfNotNull(d?.libelle?.takeIf { it.startsWith("Attaquer : ") }?.removePrefix("Attaquer : "))
                    val sauvagerie = monPersonnage?.let(ArsenalPersonnage::aSauvagerieMartiale) == true
                    PanneauDes(
                        etapes = etapesAction(arsenal, attaques, sortPretAnnonce, sauvagerie),
                        suivantes = optionsSuivantes(arsenal, sauvagerie),
                        cleRound = "${combat.id}-${combat.round}",
                        jetsLances = combat.jetsLances.ifEmpty { combat.typesLances.associateWith { 1 } },
                        relances = combat.relances,
                        onUtilisation = { option ->
                            val a = option.arme
                            if (monPersonnage != null && a != null) {
                                when {
                                    a.munition != null -> GameState.consommerMunition(monPersonnage.id, a.munition)
                                    option.lancer && a.slot != null -> GameState.lancerArme(monPersonnage.id, a.nom, a.slot)
                                }
                            }
                        },
                    )
                }
                // Actions bonus de classe (Paladin : Imposition des mains, Châtiment divin), pendant son tour.
                if (monPersonnage != null && actif?.id == moi.id && aActionsBonusClasse(monPersonnage)) item {
                    // Au contact : soi-même, les alliés (même à terre) et les ennemis au contact.
                    val cibles = listOf(CibleContact(moi.id, moi.nom + " (vous)")) + combat.ordre
                        .filter { it.id != moi.id && (!it.estMonstre || !it.horsCombat) && (it.distance == null || it.distance == Distance.CONTACT.name) }
                        .map { CibleContact(it.id, it.nom) }
                    ActionsBonusPaladin(
                        personnage = monPersonnage,
                        cibles = cibles,
                        emplacementsRestants = arsenal?.restants(monPersonnage).orEmpty(),
                        onImposition = { cible, pv, poison ->
                            val ok = if (poison) {
                                NetworkSessionManager.envoyerJetCombat(
                                    "Imposition des mains", "retire l'état Empoisonné de ${cible.nom}", emptyList(), 0, false, cibleId = cible.id,
                                )
                            } else {
                                NetworkSessionManager.envoyerJetCombat(
                                    "Soins (Imposition des mains)", "Imposition des mains → ${cible.nom}", listOf(pv), 0, false, cibleId = cible.id,
                                )
                            }
                            if (!ok) null
                            else if (poison) "Imposition des mains envoyée au MJ : Empoisonné retiré de ${cible.nom}."
                            else "Imposition des mains envoyée au MJ : ${cible.nom} récupère $pv PV."
                        },
                        onChatiment = { des, formule ->
                            val ok = NetworkSessionManager.envoyerJetCombat("Dégâts (Châtiment divin)", formule, des, 0, false)
                            if (ok) "Châtiment divin : ${des.sum()} dégâts radiants (${des.joinToString("+")}) envoyés au MJ." else null
                        },
                    )
                }
                // Vue tactique : distance et santé des ennemis et des alliés.
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = FondCarte, contentColor = Color.White)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Tactique", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                            Spacer(Modifier.height(6.dp))
                            TableauTactique(combat, moi.id)
                        }
                    }
                }
            }
        }
    }
}

/** « Dépenser l'emplacement » du sort annoncé, une seule fois par round ; la fiche part chez le MJ. */
@Composable
private fun DepenserEmplacement(combatId: String, round: Int, niveau: Int, personnage: Character, arsenal: ArsenalJoueur?) {
    var fait by rememberSaveable(combatId, round, niveau) { mutableStateOf(false) }
    val max = arsenal?.emplacements?.classiques?.get(niveau)
    val utilises = personnage.spellSlotsUsed[niveau] ?: 0
    if (fait) {
        Text("Emplacement de niveau $niveau dépensé.", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
    } else {
        Button(
            enabled = max == null || utilises < max,
            onClick = {
                GameState.setSpellSlotUsed(personnage.id, niveau, utilises + 1)
                fait = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Dépenser l'emplacement niv. $niveau" + (max?.let { " (reste ${(it - utilises).coerceAtLeast(0)})" } ?: "")) }
    }
}

/** « Lancer sans emplacement » d'un sort de prédilection annoncé, une seule fois par round. */
@Composable
private fun UtiliserPredilection(combatId: String, round: Int, sort: SortPret, personnage: Character) {
    var fait by rememberSaveable(combatId, round, sort.nom) { mutableStateOf(false) }
    when {
        fait -> Text("Sort de prédilection utilisé (récupéré au prochain repos).", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
        !sort.gratuitDisponible -> Text(
            "Prédilection déjà utilisée depuis le dernier repos : dépensez un emplacement (changez d'action).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        else -> Button(
            onClick = {
                GameState.utiliserSortPredilection(personnage.id, sort.nom)
                fait = true
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Lancer ${sort.nom} sans emplacement (prédilection)") }
    }
}

/** Bouton « Utiliser maintenant » de l'objet annoncé, une seule fois par round. */
@Composable
private fun UtiliserObjetAnnonce(combatId: String, round: Int, objet: EquipmentItem, utiliser: () -> String) {
    var resultat by rememberSaveable(combatId, round, objet.name) { mutableStateOf<String?>(null) }
    val fait = resultat
    if (fait == null) {
        Button(onClick = { resultat = utiliser() }, modifier = Modifier.fillMaxWidth()) {
            Text("Utiliser " + objet.name + " maintenant")
        }
    } else {
        Text(fait, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
    }
}

@Composable
private fun CarteInfo(texte: String) {
    Card(colors = CardDefaults.cardColors(containerColor = FondCarte, contentColor = Color.White)) {
        Text(texte, modifier = Modifier.padding(14.dp))
    }
}

@Composable
private fun TexteDeclaration(d: DeclarationJoueurData, sansResume: Boolean = false) {
    Text(d.libelle + (d.cibleNom?.let { " → $it" } ?: ""), style = MaterialTheme.typography.titleSmall, color = Color.White)
    d.distanceVisee?.let { v -> runCatching { Distance.valueOf(v) }.getOrNull() }?.let {
        Text("Déplacement : finir ${it.label.lowercase()}", style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
    // [sansResume] : le résumé calculé d'une arme ou d'un sort est déjà dans le panneau de dés,
    // seule la précision écrite par le joueur (après « — ») est gardée.
    val calcule = d.libelle.startsWith("Attaquer : ") || d.libelle.startsWith("Lancer : ")
    d.detail
        ?.let { if (sansResume && calcule) it.substringAfter(" — ", "") else it }
        ?.takeIf { it.isNotBlank() }
        ?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White) }
}

@Composable
private fun LigneOrdre(c: CombattantJoueurData, actif: Boolean, estMoi: Boolean) {
    val couleurCamp = if (c.estMonstre) Color(0xFFE57373) else Color(0xFF64B5F6)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (c.horsCombat) 0.4f else 0.75f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().then(
            if (actif) Modifier.border(2.dp, ForcedDarkPalette.AccentGold, MaterialTheme.shapes.medium) else Modifier
        ),
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.small, color = couleurCamp.copy(alpha = 0.25f), modifier = Modifier.size(38.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(c.initiative?.toString() ?: "–", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    (if (actif) "▶ " else "") + c.nom + if (estMoi) " (vous)" else "",
                    fontWeight = FontWeight.Bold,
                    color = if (actif) ForcedDarkPalette.AccentGold else Color.White,
                )
                val sante = if (c.pv != null && c.pvMax != null) "PV ${c.pv}/${c.pvMax}" else c.etat
                val distance = c.distance?.let { d -> runCatching { Distance.valueOf(d).label.lowercase() }.getOrNull() }
                Text(
                    (listOfNotNull(sante, distance) + c.conditions).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Dernier jet envoyé au MJ : [d20s] lancés (vide sans d20), [total] du toucher ou du jet, et pour
 * une attaque les [desDegats] avec leur total [degats].
 */
private data class JetEnvoye(
    val libelle: String,
    val total: Int,
    val degats: Int?,
    val note: String? = null,
    val d20s: List<Int> = emptyList(),
    val desDegats: List<Int> = emptyList(),
    // Annonce sans dé (empoignade, bousculade) : le texte est dans [note].
    val annonce: Boolean = false,
)

/** Lancer du d20 : un dé, ou deux en gardant le meilleur (avantage) ou le pire (désavantage). */
private enum class ModeD20(val label: String) { NORMAL("Normal"), AVANTAGE("Avantage"), DESAVANTAGE("Désavantage") }

private fun signeFormule(b: Int) = when {
    b > 0 -> " + $b"
    b < 0 -> " - ${-b}"
    else -> ""
}

/**
 * Jets du joueur. Les jets de l'action déclarée ([etapes]) s'enchaînent d'eux-mêmes — une attaque
 * après l'autre avec Attaque supplémentaire — puis, l'action résolue, le joueur passe à une autre
 * action disponible ([suivantes] : action bonus, Fougue, sauvegarde…). Les dés sont imposés :
 * toujours un d20 pour toucher (deux avec avantage ou désavantage), lancé en même temps que les
 * dégâts. Le tout part d'un seul envoi ; le MJ décide de l'appliquer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PanneauDes(
    etapes: List<OptionJet>,
    suivantes: List<OptionJet>,
    cleRound: String = "",
    // Jets déjà envoyés ce round, par type (confirmés par le MJ).
    jetsLances: Map<String, Int> = emptyMap(),
    relances: Int = 0,
    // Jet envoyé : dépense la munition de l'arme, ou retire l'exemplaire lancé, de la fiche.
    onUtilisation: (OptionJet) -> Unit = {},
) {
    // Jets envoyés depuis cet écran ce round, en attendant la confirmation du MJ.
    val envoyesLocaux = remember(cleRound, relances) { mutableStateMapOf<String, Int>() }
    fun envoyes(libelle: String) = maxOf(jetsLances[libelle] ?: 0, envoyesLocaux[libelle] ?: 0)
    // Jets autorisés par type : autant de « Attaque » que d'attaques déclarées, sinon un seul.
    fun autorises(libelle: String) = etapes.count { it.libelle == libelle }.coerceAtLeast(1)
    // Prochaine étape de l'action : la première dont le jet n'est pas encore parti.
    val indexEtape = run {
        val vus = mutableMapOf<String, Int>()
        etapes.indexOfFirst { o ->
            val n = (vus[o.libelle] ?: 0) + 1
            vus[o.libelle] = n
            envoyes(o.libelle) < n
        }.let { if (it < 0) etapes.size else it }
    }
    val toutes = (etapes + suivantes).distinctBy { it.nom }
    // Jet choisi dans le menu (action suivante, autre jet) ; null = étape en cours de l'action.
    var choixNom by rememberSaveable(cleRound) { mutableStateOf<String?>(null) }
    val option = choixNom?.let { n -> toutes.firstOrNull { it.nom == n } } ?: etapes.getOrNull(indexEtape)
    var menuOuvert by remember { mutableStateOf(false) }
    var dernier by remember(cleRound) { mutableStateOf<JetEnvoye?>(null) }
    var erreur by remember { mutableStateOf<String?>(null) }

    Card(colors = CardDefaults.cardColors(containerColor = FondCarte, contentColor = Color.White)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // En-tête : jet en cours (« Attaque 1/2 »), ou action terminée ; menu des autres jets.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val rang = option?.let { o -> etapes.indexOf(o).takeIf { it >= 0 && choixNom == null } }
                    Text(
                        when {
                            option == null -> "Action terminée"
                            rang != null && etapes.size > 1 -> "Lancer les dés — ${etapes[rang].libelle} ${rang + 1}/${etapes.size}"
                            else -> "Lancer les dés"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = ForcedDarkPalette.AccentGold,
                    )
                    Text(
                        option?.let { it.nom.substringBefore(" (") + if (it.lancer) " (lancer)" else "" }
                            ?: if (etapes.isEmpty()) "Aucun jet pour cette action" else "✓ Tous vos jets sont partis au MJ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                if (toutes.isNotEmpty()) Box {
                    TextButton(onClick = { menuOuvert = true }) {
                        Text("Autres jets")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                        toutes.filter { it.nom != option?.nom }.forEach { o ->
                            DropdownMenuItem(text = { Text(o.nom) }, onClick = { choixNom = o.nom; menuOuvert = false })
                        }
                    }
                }
            }

            if (option == null) {
                // Action résolue : on passe à une autre action si le personnage en a une.
                val disponibles = suivantes.filter { it.genre != GenreJet.D20 && envoyes(it.libelle) < 1 }
                if (disponibles.isNotEmpty()) {
                    Text("Action suivante disponible :", style = MaterialTheme.typography.labelMedium)
                    disponibles.forEach { o ->
                        OutlinedButton(onClick = { choixNom = o.nom }, modifier = Modifier.fillMaxWidth()) {
                            Text(o.nom, color = Color.White)
                        }
                    }
                } else if (etapes.isNotEmpty()) {
                    Text("Le MJ applique vos résultats. Un jet de sauvegarde ou de compétence reste dans « Autres jets ».", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                JetEnCours(
                    option = option,
                    cleRound = cleRound,
                    bloque = relances <= 0 && envoyes(option.libelle) >= autorises(option.libelle),
                    relanceAccordee = relances > 0 && envoyes(option.libelle) >= autorises(option.libelle),
                    onEnvoye = { jet ->
                        onUtilisation(option)
                        envoyesLocaux[option.libelle] = envoyes(option.libelle) + 1
                        dernier = jet
                        erreur = null
                        // Étape suivante de l'action (attaque suivante) ou action suivante.
                        choixNom = null
                    },
                    onErreur = { erreur = it },
                )
            }
            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            // Résultat du dernier jet envoyé (toucher et dés de dégâts) : c'est le MJ qui décide de l'appliquer.
            dernier?.let { j -> ResultatEnvoye(j) }
        }
    }
}

/** Dés d'un jet : d20 imposé (avantage/désavantage au choix) et dégâts lancés en même temps. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JetEnCours(
    option: OptionJet,
    cleRound: String,
    bloque: Boolean,
    relanceAccordee: Boolean,
    onEnvoye: (JetEnvoye) -> Unit,
    onErreur: (String) -> Unit,
) {
    var mode by rememberSaveable(cleRound) { mutableStateOf(ModeD20.NORMAL) }
    var bonusTexte by rememberSaveable(option.nom) { mutableStateOf(option.bonus.toString()) }
    var saisieD20 by rememberSaveable(option.nom) { mutableStateOf("") }
    var saisieDegats by rememberSaveable(option.nom) { mutableStateOf("") }

    // Empoignade, bousculade : rien à lancer, la cible fait un jet de sauvegarde demandé par le MJ.
    if (option.genre == GenreJet.ANNONCE) {
        Text(option.annonce.orEmpty(), style = MaterialTheme.typography.titleSmall)
        Button(enabled = !bloque, onClick = {
            val ok = NetworkSessionManager.envoyerJetCombat(option.libelle, option.annonce.orEmpty(), emptyList(), 0, false)
            if (ok) onEnvoye(JetEnvoye(option.nom, 0, null, note = option.annonce, annonce = true)) else onErreur("Envoi impossible : connexion au MJ perdue.")
        }, modifier = Modifier.fillMaxWidth()) { Text("Annoncer au MJ") }
        if (bloque) Text("Déjà annoncé ce round.", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
        return
    }

    val attaque = option.genre == GenreJet.ATTAQUE
    val avecD20 = attaque || option.genre == GenreJet.D20
    val bonus = if (option.genre == GenreJet.D20) bonusTexte.toIntOrNull() ?: 0 else option.bonus
    // (nombre de dés, faces, bonus) des dégâts ou des soins.
    val des = option.formule?.let { decomposerFormule(it) }
    val d20Manuel = saisieD20.toIntOrNull()?.takeIf { it in 1..20 }
    // 20 naturel sur une attaque : les dés de dégâts sont doublés.
    val nbDesManuel = des?.let { if (attaque && d20Manuel == 20) it.first * 2 else it.first }
    val bornesDegats = des?.let { nbDesManuel!!..(nbDesManuel * it.second) }
    val degatsManuel = saisieDegats.toIntOrNull()?.takeIf { v -> bornesDegats?.contains(v) == true }
    val manuelPret = when (option.genre) {
        GenreJet.D20 -> d20Manuel != null
        GenreJet.ATTAQUE -> d20Manuel != null && (des == null || degatsManuel != null)
        else -> degatsManuel != null
    }
    // Don Sauvagerie martiale (une fois par tour) : dés de dégâts de l'arme lancés deux fois.
    var sauvagerieActive by rememberSaveable(cleRound) { mutableStateOf(true) }
    val avecSauvagerie = option.sauvagerie && attaque && des != null && sauvagerieActive

    fun formuleDegats(critique: Boolean): String? = des?.let { (nb, faces, b) ->
        "${if (critique) nb * 2 else nb}d$faces" + signeFormule(b) + (option.typeDegats?.let { " $it" } ?: "") +
            if (critique) " (critique)" else ""
    }

    // [d20s] : dés lancés (deux avec avantage/désavantage, ou le seul retenu en saisie manuelle).
    // [noteDegats] : précision ajoutée à la formule des dégâts (ex. jet écarté par Sauvagerie martiale).
    fun envoyer(d20s: List<Int>, degats: List<Int>, manuel: Boolean, noteDegats: String? = null) {
        if (bloque || option.interdit) return
        var jet: JetEnvoye? = null
        val ok = if (avecD20) {
            val garde = when (mode) {
                ModeD20.NORMAL -> d20s.first()
                ModeD20.AVANTAGE -> d20s.max()
                ModeD20.DESAVANTAGE -> d20s.min()
            }
            val suffixe = if (mode == ModeD20.NORMAL) "" else " (${mode.label.lowercase()}" +
                (if (d20s.size > 1) " : ${d20s.joinToString(" / ")}" else "") + ")"
            val totalDegats = if (attaque && des != null) (degats.sum() + des.third).coerceAtLeast(0) else null
            NetworkSessionManager.envoyerJetCombat(
                option.libelle, "1d20" + signeFormule(bonus) + suffixe, listOf(garde), bonus, manuel,
                formuleDegats = if (attaque) formuleDegats(garde == 20)?.let { f -> noteDegats?.let { "$f · $it" } ?: f } else null,
                desDegats = degats,
                bonusDegats = des?.third ?: 0,
            ).also { if (it) jet = JetEnvoye(option.libelle, garde + bonus, totalDegats, noteDegats, d20s, if (attaque) degats else emptyList()) }
        } else {
            val b = des?.third ?: 0
            NetworkSessionManager.envoyerJetCombat(option.libelle, formuleDegats(false).orEmpty(), degats, b, manuel)
                .also { if (it) jet = JetEnvoye(option.libelle, degats.sum() + b, null, desDegats = degats) }
        }
        val envoye = jet
        if (ok && envoye != null) {
            saisieD20 = ""
            saisieDegats = ""
            onEnvoye(envoye)
        } else {
            onErreur("Envoi impossible : connexion au MJ perdue.")
        }
    }

    fun lancerAvecAppli() {
        val d20s = List(if (avecD20 && mode != ModeD20.NORMAL) 2 else 1) { (1..20).random() }
        val garde = if (mode == ModeD20.DESAVANTAGE) d20s.min() else d20s.max()
        val critique = attaque && garde == 20
        fun lancerDegats() = des?.takeIf { !avecD20 || attaque }
            ?.let { (nb, faces, _) -> List(if (critique) nb * 2 else nb) { (1..faces).random() } }
            .orEmpty()
        val premier = lancerDegats()
        if (avecSauvagerie && premier.isNotEmpty()) {
            // Sauvagerie martiale : deux jets de dégâts, le meilleur est gardé.
            val second = lancerDegats()
            val (garde2, ecarte) = if (second.sum() > premier.sum()) second to premier else premier to second
            sauvagerieActive = false
            envoyer(d20s, garde2, manuel = false, noteDegats = "Sauvagerie martiale : ${garde2.sum()} gardé, ${ecarte.sum()} écarté")
        } else {
            envoyer(d20s, premier, manuel = false)
        }
    }

    // Le d20 est imposé : seul l'avantage ou le désavantage se choisit.
    if (avecD20) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeD20.entries.forEach { m ->
                FilterChip(selected = m == mode, onClick = { mode = m }, label = { Text(m.label) })
            }
        }
    }
    if (option.genre == GenreJet.D20) {
        OutlinedTextField(
            value = bonusTexte,
            onValueChange = { v -> bonusTexte = v.filterIndexed { i, c -> c.isDigit() || (i == 0 && (c == '-' || c == '+')) }.take(4) },
            label = { Text("Bonus / malus") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    // Ce qui part en un seul lancer : le d20 pour toucher et les dés de dégâts.
    val lignes = listOfNotNull(
        if (avecD20) (if (attaque) "Toucher " else "Jet ") +
            (if (mode == ModeD20.NORMAL) "1d20" else "2d20 (${mode.label.lowercase()})") + signeFormule(bonus) else null,
        formuleDegats(false)?.takeIf { !avecD20 || attaque }?.let {
            (if (option.genre == GenreJet.SOINS) "Soins " else "Dégâts ") + it
        },
    )
    Text(lignes.joinToString("  ·  "), style = MaterialTheme.typography.titleSmall)
    if (attaque && des != null) {
        Text("Dés de dégâts doublés sur un 20 naturel.", style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
    if (option.sauvagerie && attaque && des != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = sauvagerieActive, onCheckedChange = { sauvagerieActive = it })
            Column {
                Text("Sauvagerie martiale", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                Text(
                    "Dés de dégâts de l'arme lancés deux fois, le meilleur résultat est gardé (une fois par tour). " +
                        "Avec vos vrais dés : lancez-les deux fois et saisissez le meilleur total.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                )
            }
        }
    }

    option.arme?.let { a ->
        val etat = when {
            option.interdit -> "Plus de ${a.munition} : attaque impossible."
            a.munition != null -> "Chaque attaque dépense 1 munition (${a.munitionsRestantes} ${a.munition})."
            option.lancer -> "L'arme lancée quitte votre équipement" + (if (a.quantite > 1) " (${a.quantite} portées)." else ".")
            else -> null
        }
        etat?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = if (option.interdit) MaterialTheme.colorScheme.error else ForcedDarkPalette.AccentGold)
        }
    }
    Button(enabled = !bloque && !option.interdit && (avecD20 || des != null), onClick = { lancerAvecAppli() }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Casino, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (attaque && des != null) "Lancer toucher + dégâts" else "Lancer avec l'appli")
    }
    // Vrais dés : le d20 retenu et/ou la somme des dés de dégâts, sans les bonus.
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (avecD20) {
            OutlinedTextField(
                value = saisieD20,
                onValueChange = { saisieD20 = it.filter(Char::isDigit).take(2) },
                label = { Text("d20") },
                singleLine = true,
                isError = saisieD20.isNotEmpty() && d20Manuel == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        if (bornesDegats != null && (!avecD20 || attaque)) {
            OutlinedTextField(
                value = saisieDegats,
                onValueChange = { saisieDegats = it.filter(Char::isDigit).take(3) },
                label = { Text((if (option.genre == GenreJet.SOINS) "Soins" else "Dégâts") + " (${bornesDegats.first}–${bornesDegats.last})") },
                singleLine = true,
                isError = saisieDegats.isNotEmpty() && degatsManuel == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedButton(enabled = manuelPret && !bloque && !option.interdit, onClick = {
            val note = if (avecSauvagerie && degatsManuel != null) "Sauvagerie martiale : meilleur de deux jets" else null
            if (note != null) sauvagerieActive = false
            envoyer(listOfNotNull(d20Manuel), listOfNotNull(degatsManuel), manuel = true, noteDegats = note)
        }) {
            Icon(Icons.Default.Send, contentDescription = "Envoyer mes vrais dés")
        }
    }
    Text(
        "Vrais dés : saisissez le d20 retenu" + (if (attaque && des != null) " et la somme des dés de dégâts" else "") +
            ", sans les bonus : l'appli les ajoute.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White,
    )
    if (bloque) {
        Text(
            "Jet « ${option.libelle} » déjà lancé ce round. Demandez au MJ s'il vous en accorde un autre.",
            style = MaterialTheme.typography.bodySmall,
            color = ForcedDarkPalette.AccentGold,
        )
    } else if (relanceAccordee) {
        Text("Le MJ vous accorde un jet supplémentaire.", style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
    }
}

/** Dernier jet envoyé : toucher et dés de dégâts, ou annonce (empoignade, bousculade). */
@Composable
private fun ResultatEnvoye(j: JetEnvoye) {
    Surface(shape = MaterialTheme.shapes.small, color = ForcedDarkPalette.AccentGold.copy(alpha = 0.15f), contentColor = Color.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val estAttaque = j.libelle.startsWith("Attaque")
            if (j.d20s.isNotEmpty()) {
                Text(
                    (if (estAttaque) "🎲 Toucher " else "🎲 ${j.libelle} ") + j.total +
                        "  (d20 : ${j.d20s.joinToString(" / ")})" + if (j.d20s.contains(20) && estAttaque) " — 20 naturel !" else "",
                    fontWeight = FontWeight.Bold,
                    color = ForcedDarkPalette.AccentGold,
                )
            }
            val montant = if (j.annonce) null else j.degats ?: j.total.takeIf { j.d20s.isEmpty() }
            if (montant != null) {
                Text(
                    (if (j.libelle.startsWith("Soins")) "🎲 Soins " else "🎲 Dégâts ") + montant +
                        (j.desDegats.takeIf { it.isNotEmpty() }?.let { "  (dés : ${it.joinToString("+")})" } ?: ""),
                    fontWeight = FontWeight.Bold,
                    color = ForcedDarkPalette.AccentGold,
                )
            }
            j.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Envoyé au MJ : c'est lui qui décide d'appliquer le résultat.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
