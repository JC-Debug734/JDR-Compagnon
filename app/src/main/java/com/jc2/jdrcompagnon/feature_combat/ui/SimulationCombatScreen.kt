package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import com.jc2.jdrcompagnon.ui.GameState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ActionsCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeEnMain
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueHeros
import com.jc2.jdrcompagnon.feature_combat.domain.model.CampLigne
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.DeclarationHeros
import com.jc2.jdrcompagnon.feature_combat.domain.model.Distance
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatSimulation
import com.jc2.jdrcompagnon.feature_combat.domain.model.FrappeHeros
import com.jc2.jdrcompagnon.feature_combat.domain.model.HerosSimule
import com.jc2.jdrcompagnon.feature_combat.domain.model.IssueSimulation
import com.jc2.jdrcompagnon.feature_combat.domain.model.LigneSimulation
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.SimulateurCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.SortSimule
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.network.CombatJoueurData
import com.jc2.jdrcompagnon.network.CombattantJoueurData
import com.jc2.jdrcompagnon.network.etatSante
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.Consommables
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import com.jc2.jdrcompagnon.feature_group.domain.MonsterXpMultiplier
import com.jc2.jdrcompagnon.feature_group.domain.XpThresholdTable
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.monsterChallenge
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un type de monstre choisi pour la simulation, avec sa quantité. */
private data class MonstreChoisi(val nom: String, val quantite: Int)

private val CouleurMonstre = Color(0xFFFF8A7A)

/**
 * Simulation d'un combat du personnage contre des monstres (outil Actions de combat, côté MJ).
 * Rien n'est enregistré : la fiche du personnage et un éventuel combat en cours ne sont pas
 * touchés. À chaque tour du personnage, le MJ a exactement l'écran d'un joueur en partie
 * (actions, cartes d'armes, vue tactique, déplacement, puis attaques / sort / objet et cible) ;
 * l'action est ensuite résolue automatiquement, et les monstres jouent seuls.
 */
@Composable
fun SimulationCombatScreen(
    personnageId: String,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) = TexteBlancCombat {
    val personnages by GameState.characters.collectAsState()
    val personnage = personnages.firstOrNull { it.id == personnageId }
    val arsenal = rememberArsenal(personnage)
    if (personnage == null || arsenal == null) {
        Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)) {
            EnTeteSimulation(personnage?.name ?: "", onBack)
            Text(if (personnage == null) "Personnage introuvable." else "Chargement des capacités…")
        }
        return@TexteBlancCombat
    }
    SimulationCombat(personnage, arsenal, onBack, onOpenMenu)
}

/**
 * Écran de navigation (comme la lecture d'un scénario) : la barre du bas de l'appli reste
 * visible. Retour Android pendant un combat : retour à la configuration.
 */
@Composable
private fun SimulationCombat(
    personnage: Character,
    arsenal: ArsenalJoueur,
    onDismiss: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monde = personnage.worldId.ifBlank { "donjon_et_dragon" }

    val choisis = remember { mutableStateListOf<MonstreChoisi>() }
    var distanceDepart by remember { mutableStateOf(Distance.COURTE) }
    var pvPleins by remember { mutableStateOf(true) }
    var afficherSelecteur by remember { mutableStateOf(false) }
    var preparation by remember { mutableStateOf(false) }
    // Adversaire automatique (difficulté moyenne pour le niveau du personnage).
    var rechercheAuto by remember { mutableStateOf(false) }
    var explicationAuto by remember { mutableStateOf<String?>(null) }

    var simulateur by remember { mutableStateOf<SimulateurCombat?>(null) }
    var etat by remember { mutableStateOf<EtatSimulation?>(null) }
    // Début, dans le journal, de ce qui s'est passé depuis la dernière action du personnage.
    var debutTour by remember { mutableIntStateOf(0) }
    // Dernière composition lancée, pour « Rejouer » à l'identique (nouveaux jets).
    var derniersMonstres by remember { mutableStateOf<List<Combattant>>(emptyList()) }

    var consommables by remember { mutableStateOf<List<EquipmentItem>>(emptyList()) }
    LaunchedEffect(personnage.id) { consommables = Consommables.equipesDuPersonnage(context, personnage) }

    val heros = remember(personnage, arsenal, pvPleins) { herosDepuisPersonnage(personnage, arsenal, pvPleins) }

    fun lancer(monstres: List<Combattant>) {
        val sim = SimulateurCombat(heros)
        simulateur = sim
        derniersMonstres = monstres
        etat = sim.demarrer(monstres, distanceDepart)
        debutTour = 0
    }

    fun jouer(nouvel: (EtatSimulation) -> EtatSimulation) {
        val courant = etat ?: return
        debutTour = courant.journal.size
        etat = nouvel(courant)
    }

    BackHandler(enabled = etat != null) { etat = null; simulateur = null }
    val courant = etat
    val sim = simulateur
    when {
        courant == null || sim == null -> Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EnTeteSimulation(personnage.name, onDismiss)
            Configuration(
                heros = heros,
                choisis = choisis,
                distanceDepart = distanceDepart,
                onDistance = { distanceDepart = it },
                pvPleins = pvPleins,
                onPvPleins = { pvPleins = it },
                pvActuels = personnage.currentHitPoints,
                preparation = preparation,
                onAjouter = { afficherSelecteur = true; explicationAuto = null },
                niveau = personnage.level,
                rechercheAuto = rechercheAuto,
                explicationAuto = explicationAuto,
                onAuto = {
                    rechercheAuto = true
                    scope.launch {
                        val rencontre = withContext(Dispatchers.IO) { rencontreMoyenne(context, monde, personnage.level) }
                        rechercheAuto = false
                        if (rencontre != null) {
                            choisis.clear()
                            choisis += rencontre.choix
                            explicationAuto = rencontre.explication
                        } else {
                            explicationAuto = "Aucun monstre du bestiaire ne correspond à ce niveau."
                        }
                    }
                },
                onLancer = {
                    preparation = true
                    scope.launch {
                        val monstres = withContext(Dispatchers.IO) { creerMonstres(context, monde, choisis.toList()) }
                        preparation = false
                        if (monstres.isNotEmpty()) lancer(monstres)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        courant.issue != null -> Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EnTeteSimulation(personnage.name, onDismiss)
            Resultat(
                etat = courant,
                onRejouer = { lancer(derniersMonstres.map { it.copy(pv = it.pvMax, capacitesUtilisees = emptyMap(), conditions = emptySet()) }) },
                onNouvelle = { etat = null; simulateur = null },
                modifier = Modifier.weight(1f)
            )
        }
        else -> TourDuPersonnage(
            etat = courant,
            recents = courant.journal.drop(debutTour),
            personnage = personnage,
            arsenal = arsenal,
            consommables = consommables,
            onDeclarer = { declaration -> jouer { sim.jouerHeros(it, declaration) } },
            onAutomatique = { sim.actionAutomatique(courant)?.let { d -> jouer { sim.jouerHeros(it, d) } } },
            onJusquALaFin = { jouer { sim.simulerJusquALaFin(it) } },
            onArreter = { etat = null; simulateur = null },
            onOpenMenu = onOpenMenu,
        )
    }

    if (afficherSelecteur) {
        SelecteurMonstre(
            monde = monde,
            onDismiss = { afficherSelecteur = false },
            onChoisir = { nom ->
                val index = choisis.indexOfFirst { it.nom == nom }
                if (index >= 0) choisis[index] = choisis[index].copy(quantite = choisis[index].quantite + 1)
                else choisis += MonstreChoisi(nom, 1)
                afficherSelecteur = false
            }
        )
    }
}

@Composable
private fun EnTeteSimulation(nom: String, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Simulation de combat", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("$nom contre des monstres — rien n'est enregistré", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }
        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
    }
}

/** Case translucide titrée, pour séparer lisiblement chaque partie de la simulation. */
@Composable
private fun CaseSimulation(
    titre: String?,
    modifier: Modifier = Modifier,
    contenu: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            titre?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold) }
            contenu()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Configuration(
    heros: HerosSimule,
    choisis: MutableList<MonstreChoisi>,
    distanceDepart: Distance,
    onDistance: (Distance) -> Unit,
    pvPleins: Boolean,
    onPvPleins: (Boolean) -> Unit,
    pvActuels: Int,
    preparation: Boolean,
    onAjouter: () -> Unit,
    onLancer: () -> Unit,
    niveau: Int,
    rechercheAuto: Boolean,
    explicationAuto: String?,
    onAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CaseSimulation("PV de départ") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = pvPleins, onClick = { onPvPleins(true) }, label = { Text("Pleins (${heros.pvMax})") })
                FilterChip(selected = !pvPleins, onClick = { onPvPleins(false) }, label = { Text("Actuels ($pvActuels)") })
            }
        }
        CaseSimulation("Distance de départ des monstres") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Distance.entries.forEach { d ->
                    FilterChip(selected = distanceDepart == d, onClick = { onDistance(d) }, label = { Text(d.court) })
                }
            }
        }
        CaseSimulation("Monstres") {
            if (choisis.isEmpty()) {
                Text("Ajoutez un ou plusieurs monstres du bestiaire.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            }
            choisis.toList().forEachIndexed { index, m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(m.nom, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (m.quantite > 1) choisis[index] = m.copy(quantite = m.quantite - 1) else choisis.removeAt(index)
                    }) { Icon(Icons.Default.Remove, contentDescription = "Un de moins") }
                    Text("×${m.quantite}")
                    IconButton(onClick = { choisis[index] = m.copy(quantite = (m.quantite + 1).coerceAtMost(20)) }) {
                        Icon(Icons.Default.Add, contentDescription = "Un de plus")
                    }
                }
            }
            // Adversaire tiré au hasard dans le bestiaire, à la mesure du personnage (un nouvel appui en propose un autre).
            Button(onClick = onAuto, enabled = !rechercheAuto, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    if (rechercheAuto) "Recherche…" else "Adversaire auto — niveau $niveau, difficulté moyenne",
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            explicationAuto?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold)
            }
            OutlinedButton(onClick = onAjouter, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Ajouter un monstre", modifier = Modifier.padding(start = 6.dp))
            }
        }
        Button(
            onClick = onLancer,
            enabled = choisis.isNotEmpty() && !preparation && heros.attaques.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.SportsMartialArts, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(if (preparation) "Préparation…" else "Lancer la simulation", modifier = Modifier.padding(start = 6.dp))
        }
        if (heros.attaques.isEmpty()) {
            Text("Ce personnage n'a aucune attaque utilisable.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * Tour du personnage : l'écran Actions de combat d'un joueur en partie (mêmes actions, cartes
 * d'armes, vue tactique et déplacement), puis l'assistant de déclaration (attaques, sort, objet,
 * cible). En tête, l'état de la simulation et ce que les monstres viennent de faire.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TourDuPersonnage(
    etat: EtatSimulation,
    recents: List<LigneSimulation>,
    personnage: Character,
    arsenal: ArsenalJoueur,
    consommables: List<EquipmentItem>,
    onDeclarer: (DeclarationHeros) -> Unit,
    onAutomatique: () -> Unit,
    onJusquALaFin: () -> Unit,
    onArreter: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val combat = remember(etat) { etat.versJoueur() }
    val moi = combat.ordre.first { it.id == etat.heros.id }
    // Munitions et armes lancées restantes dans la simulation (la fiche n'est pas touchée).
    val arsenalSim = remember(arsenal, etat.stocks) { arsenal.avecStocks(etat.stocks) }
    var actionChoisie by remember(etat.round, etat.journal.size) { mutableStateOf<String?>(null) }
    var armePreselectionnee by remember(etat.round, etat.journal.size) { mutableStateOf<String?>(null) }
    var deplacement by remember(etat.round, etat.journal.size) { mutableStateOf<DeplacementJoueur?>(null) }
    var journalComplet by remember { mutableStateOf(false) }

    // Action touchée : l'assistant (détail puis cible) remplace l'écran des actions.
    val actionEnCours = actionChoisie?.let { id -> ActionsCombat.toutes.firstOrNull { it.id == id } }
    if (actionEnCours != null) {
        AssistantDeclaration(
            combat = combat,
            moi = moi,
            action = actionEnCours,
            arsenal = arsenalSim,
            personnage = personnage,
            consommables = consommables,
            initiale = null,
            armeInitiale = armePreselectionnee,
            onValider = { d ->
                actionChoisie = null
                onDeclarer(versDeclarationHeros(d, deplacement, arsenalSim, consommables))
            },
            onFermer = { actionChoisie = null },
            emplacementsRestants = etat.emplacements,
        )
        return
    }

    CombatActionsScreen(
        onBack = onArreter,
        onOpenMenu = onOpenMenu,
        titre = "Simulation",
        entete = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Emplacements restants : carte des ressources, sous cet en-tête.
                    Text(
                        "${etat.heros.nom} · ${etat.heros.pv}/${etat.heros.pvMax} PV · CA ${etat.heros.ca}",
                        fontWeight = FontWeight.Bold,
                        color = ForcedDarkPalette.AccentGold
                    )
                    LinearProgressIndicator(
                        progress = { if (etat.heros.pvMax > 0) etat.heros.pv / etat.heros.pvMax.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ForcedDarkPalette.AccentGold
                    )
                    // Ce qui s'est passé depuis la dernière action du personnage (monstres compris).
                    recents.takeLast(8).forEach { LigneJournal(it) }
                }
            }
        },
        sousTitre = "Round ${etat.round}",
        actionHaut = {
            // Commandes de la simulation, repliées dans un petit menu.
            var menuOuvert by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuOuvert = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Commandes de la simulation", tint = ForcedDarkPalette.AccentGold)
                }
                DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                    DropdownMenuItem(
                        text = { Text("Tour auto") },
                        leadingIcon = { Icon(Icons.Default.SmartToy, contentDescription = null) },
                        onClick = { menuOuvert = false; onAutomatique() }
                    )
                    DropdownMenuItem(
                        text = { Text("Jusqu'à la fin") },
                        leadingIcon = { Icon(Icons.Default.FastForward, contentDescription = null) },
                        onClick = { menuOuvert = false; onJusquALaFin() }
                    )
                    DropdownMenuItem(
                        text = { Text("Journal complet") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        onClick = { menuOuvert = false; journalComplet = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Arrêter", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOuvert = false; onArreter() }
                    )
                }
            }
        },
        onChoisirAction = { actionChoisie = it.id; armePreselectionnee = null },
        personnageId = personnage.id,
        onChoisirArme = { arme ->
            armePreselectionnee = arme.nom
            actionChoisie = "attaquer"
        },
        tactique = { TableauTactique(combat, moi.id) },
        sectionDeplacement = {
            CarteDeplacement(
                combat = combat,
                moiId = moi.id,
                deplacement = deplacement,
                desengage = false,
                onChange = { deplacement = it },
            )
        },
        emplacementsSimulation = etat.emplacements,
        arsenalSimulation = arsenalSim,
    )

    if (journalComplet) {
        AlertDialog(
            onDismissRequest = { journalComplet = false },
            title = { Text("Journal de la simulation") },
            text = {
                val etatListe = rememberLazyListState(initialFirstVisibleItemIndex = (etat.journal.size - 1).coerceAtLeast(0))
                LazyColumn(state = etatListe, modifier = Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(etat.journal) { LigneJournal(it) }
                }
            },
            confirmButton = { TextButton(onClick = { journalComplet = false }) { Text("Fermer") } }
        )
    }
}

@Composable
private fun Resultat(
    etat: EtatSimulation,
    onRejouer: () -> Unit,
    onNouvelle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val issue = etat.issue ?: return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CaseSimulation(null) {
            Text(
                issue.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (issue == IssueSimulation.VICTOIRE) Color(0xFF81C784) else CouleurMonstre
            )
            Text("${etat.round} round(s) · ${etat.heros.nom} ${etat.heros.pv}/${etat.heros.pvMax} PV", style = MaterialTheme.typography.bodyMedium)
        }
        val etatListe = rememberLazyListState(initialFirstVisibleItemIndex = (etat.journal.size - 1).coerceAtLeast(0))
        CaseSimulation("Journal", modifier = Modifier.weight(1f)) {
            LazyColumn(state = etatListe, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                items(etat.journal) { LigneJournal(it) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRejouer, modifier = Modifier.weight(1f)) { Text("Rejouer") }
            OutlinedButton(onClick = onNouvelle, modifier = Modifier.weight(1f)) { Text("Autres monstres") }
        }
    }
}

@Composable
private fun LigneJournal(ligne: LigneSimulation) {
    Text(
        ligne.texte,
        style = MaterialTheme.typography.bodySmall,
        color = when (ligne.camp) {
            CampLigne.HEROS -> ForcedDarkPalette.AccentGold
            CampLigne.MONSTRE -> CouleurMonstre
            CampLigne.INFO -> Color.White.copy(alpha = 0.75f)
        },
        fontWeight = if (ligne.camp == CampLigne.INFO) FontWeight.Bold else FontWeight.Normal
    )
}

/**
 * La simulation vue comme un combat en réseau par le joueur (même structure que
 * CombatEnCours.versJoueur), pour réutiliser ses écrans : vue tactique, déplacement, cible.
 */
private fun EtatSimulation.versJoueur(): CombatJoueurData {
    val parId = (monstres + heros).associateBy { it.id }
    return CombatJoueurData(
        id = "simulation",
        titre = "Simulation",
        round = round,
        phase = PhaseCombat.DECLARATION.name,
        ordre = ordre.mapNotNull { parId[it] }.map { c ->
            CombattantJoueurData(
                id = c.id,
                nom = c.nom,
                estMonstre = c.estMonstre,
                initiative = c.initiative,
                etat = etatSante(c.pv, c.pvMax, c.estMonstre),
                horsCombat = c.horsCombat,
                pv = if (c.estMonstre) null else c.pv,
                pvMax = if (c.estMonstre) null else c.pvMax,
                conditions = c.conditions.map { it.label },
                distance = if (c.estMonstre) distance(c.id).name else null,
            )
        },
        actifId = heros.id,
        nbDeclares = 0,
        nbAttendus = 1,
        monCombattantId = heros.id,
    )
}

/** Déclaration du joueur (assistant) traduite pour le simulateur : frappes, sort, objet, déplacement. */
private fun versDeclarationHeros(
    d: DeclarationChoisie,
    deplacement: DeplacementJoueur?,
    arsenal: ArsenalJoueur,
    consommables: List<EquipmentItem>,
): DeclarationHeros {
    val armes = arsenal.armes + listOfNotNull(arsenal.mainsNues)
    val frappes = d.attaques.mapNotNull { nom ->
        when (nom) {
            ArsenalPersonnage.EMPOIGNADE -> FrappeHeros.Empoignade(arsenal.ddMainsNues)
            ArsenalPersonnage.BOUSCULADE -> FrappeHeros.Bousculade(arsenal.ddMainsNues)
            else -> armes.firstOrNull { it.nom == nom }?.let { FrappeHeros.Arme(it.versAttaqueHeros()) }
        }
    }
    val sortTrouve = sortRegex.find(d.libelle)
    val sort = sortTrouve?.let { m -> arsenal.sorts.firstOrNull { it.nom == m.groupValues[1] } }?.let { s ->
        SortSimule(
            s.nom, s.niveau, s.jet, s.bonusAttaque, s.dd, s.sauvegarde, s.formuleDegats, s.soin, s.demiDegatsSiEchec,
            sansEmplacement = sortTrouve?.groupValues?.get(3)?.isNotEmpty() == true && s.gratuitDisponible,
        )
    }
    val objetNom = d.detail.takeIf { it.startsWith(PREFIXE_OBJET) }?.removePrefix(PREFIXE_OBJET)?.substringBefore(" — ")
    val objet = consommables.firstOrNull { it.name == objetNom }
    return DeclarationHeros(
        libelle = d.libelle,
        actionId = d.actionId,
        cibleId = d.cible?.takeIf { it.estMonstre }?.id,
        ciblesZone = d.ciblesZone.filter { it.estMonstre }.map { it.id },
        frappes = frappes,
        sort = sort,
        niveauEmplacement = sortTrouve?.groupValues?.get(2)?.toIntOrNull(),
        objetNom = objetNom,
        soinObjet = objet?.let { Consommables.formuleSoin(it) },
        deplacementCibleId = deplacement?.cibleId,
        distanceVisee = deplacement?.visee,
    )
}

@Composable
private fun SelecteurMonstre(monde: String, onDismiss: () -> Unit, onChoisir: (String) -> Unit) {
    val context = LocalContext.current
    var recherche by remember { mutableStateOf("") }
    var noms by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(recherche, monde) {
        noms = withContext(Dispatchers.IO) {
            SrdRepository.loadMonsters(context, monde).map { it.name }
                .filter { recherche.isBlank() || it.contains(recherche.trim(), ignoreCase = true) }
                .take(60)
        }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter un monstre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher dans le bestiaire") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(noms) { nom ->
                        Text(nom, modifier = Modifier.fillMaxWidth().clickable { onChoisir(nom) }.padding(vertical = 10.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * Arme de la fiche telle que la simulation la joue : une arme à munitions dépense ses munitions
 * (carquois, sac), une arme de lancer équipée ses exemplaires (pile de javelines).
 */
private fun ArmeEnMain.versAttaqueHeros(): AttaqueHeros {
    val lancee = lancer && slot != null
    return AttaqueHeros(
        nom = nom,
        bonusToucher = bonusToucher,
        formuleDegats = formuleDegats,
        aDistance = aDistance,
        lancer = lancee,
        stock = when {
            munition != null -> munitionsRestantes
            lancee -> quantite
            else -> null
        },
    )
}

/** Arsenal affiché pendant la simulation : munitions et exemplaires restants de la simulation. */
private fun ArsenalJoueur.avecStocks(stocks: Map<String, Int>): ArsenalJoueur =
    if (stocks.isEmpty()) this else copy(
        armes = armes.mapNotNull { a ->
            val reste = stocks[a.nom] ?: return@mapNotNull a
            when {
                a.munition != null -> a.copy(munitionsRestantes = reste)
                // Dernier exemplaire lancé : l'arme n'est plus en main.
                reste <= 0 -> null
                else -> a.copy(quantite = reste)
            }
        }
    )

/** Le personnage tel que la simulation le voit : armes de l'arsenal, mains nues, sauvegardes. */
private fun herosDepuisPersonnage(personnage: Character, arsenal: ArsenalJoueur, pvPleins: Boolean): HerosSimule {
    val armes = arsenal.armes.filter { !it.sansMunition } + listOfNotNull(arsenal.mainsNues)
    val maitrise = personnage.proficiencyBonus
    fun sauvegarde(nom: String, valeur: Int): Pair<String, Int> {
        val maitrisee = personnage.savingThrowProficiencies.any { it.equals(nom, ignoreCase = true) }
        return nom.lowercase() to Math.floorDiv(valeur - 10, 2) + if (maitrisee) maitrise else 0
    }
    return HerosSimule(
        nom = personnage.name,
        ca = personnage.armorClass,
        pvMax = personnage.maxHitPoints.coerceAtLeast(1),
        pvDepart = if (pvPleins) personnage.maxHitPoints else personnage.currentHitPoints,
        bonusInitiative = personnage.initiativeBonus,
        attaques = armes.distinctBy { it.nom }.map { it.versAttaqueHeros() },
        nbAttaques = arsenal.nbAttaques,
        emplacements = arsenal.restants(personnage),
        vitesse = com.jc2.jdrcompagnon.feature_combat.domain.model.vitesseEnMetres(personnage.speed),
        bonusSauvegardes = mapOf(
            sauvegarde("Force", personnage.strength),
            sauvegarde("Dextérité", personnage.dexterity),
            sauvegarde("Constitution", personnage.constitution),
            sauvegarde("Intelligence", personnage.intelligence),
            sauvegarde("Sagesse", personnage.wisdom),
            sauvegarde("Charisme", personnage.charisma),
        ),
    )
}

/** Rencontre proposée automatiquement : monstres choisis et explication du calcul. */
private data class RencontreAuto(val choix: List<MonstreChoisi>, val explication: String)

/**
 * Rencontre de difficulté moyenne pour un personnage seul de niveau [niveau] (barème D&D 5e) :
 * budget = seuil « moyenne » du niveau ; PX des monstres × multiplicateur de nombre (un
 * personnage seul compte comme un petit groupe : ×1,5 dès un monstre). Tirée au hasard parmi les
 * compositions (1 à 4 monstres identiques) qui remplissent le mieux le budget sans le dépasser,
 * en ne gardant que des monstres qui ont des attaques utilisables par la simulation.
 */
private suspend fun rencontreMoyenne(context: android.content.Context, monde: String, niveau: Int): RencontreAuto? {
    val budget = XpThresholdTable.thresholdFor(niveau, EncounterDifficulty.MOYENNE)
    val monstres = SrdRepository.loadMonsters(context, monde).mapNotNull { entree ->
        val (fp, px) = monsterChallenge(entree) ?: return@mapNotNull null
        val xp = px.filter(Char::isDigit).toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
        Triple(entree, fp, xp)
    }
    val compositions = monstres.flatMap { (entree, fp, xp) ->
        (1..4).map { n -> Triple(entree to fp, n, (xp * n * MonsterXpMultiplier.multiplierFor(n, 1)).toInt()) }
    }.filter { it.third <= budget }
    if (compositions.isEmpty()) return null
    val utilisable = { c: Triple<Pair<SrdEntry, String>, Int, Int> ->
        ProfilCombatMonstre.depuisFiche(c.first.first.rawMarkdown).actions.attaques.isNotEmpty()
    }
    // Du plus proche du budget au plus large, on garde la première tranche qui offre un choix.
    val retenue = listOf(0.85, 0.7, 0.5, 0.0).firstNotNullOfOrNull { part ->
        compositions.filter { it.third >= budget * part }.shuffled().take(40).filter(utilisable).ifEmpty { null }
    }?.random() ?: return null
    val (monstre, n, ajuste) = retenue
    val (entree, fp) = monstre
    return RencontreAuto(
        choix = listOf(MonstreChoisi(entree.name, n)),
        explication = "Niveau $niveau, difficulté moyenne : budget $budget PX. " +
            "${entree.name} (FP $fp) ×$n = $ajuste PX ajustés.",
    )
}

/** Monstres du bestiaire instanciés pour la simulation (PV moyens, actions de la fiche, profil d'IA). */
private suspend fun creerMonstres(context: android.content.Context, monde: String, choisis: List<MonstreChoisi>): List<Combattant> =
    choisis.flatMap { choix ->
        val fiche = SrdRepository.getMonsterByName(context, choix.nom, monde) ?: return@flatMap emptyList()
        val profil = ProfilCombatMonstre.depuisFiche(fiche.rawMarkdown)
        (1..choix.quantite).map { numero ->
            Combattant(
                id = UUID.randomUUID().toString(),
                nom = if (choix.quantite > 1) "${fiche.name} $numero" else fiche.name,
                estMonstre = true,
                monstreNom = fiche.name,
                ca = profil.ca,
                pvMax = profil.pvMoyens,
                pv = profil.pvMoyens,
                bonusInitiative = profil.bonusInitiative,
                attaques = profil.actions.attaques,
                nbAttaquesMultiples = profil.actions.nbAttaquesMultiples,
                profilIA = profil.comportement.profil,
                raisonProfil = profil.comportement.raison,
                vitesse = profil.vitesse,
            )
        }
    }
