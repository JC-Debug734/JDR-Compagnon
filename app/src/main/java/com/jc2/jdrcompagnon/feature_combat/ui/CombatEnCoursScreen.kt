package com.jc2.jdrcompagnon.feature_combat.ui

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.feature_combat.presentation.DeclarationAction
import com.jc2.jdrcompagnon.feature_combat.presentation.JetCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilIA
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.mikepenz.markdown.m3.Markdown
import java.util.UUID
import kotlinx.coroutines.launch

/**
 * Suivi d'un combat lancé depuis un lien #combat: de scénario (voir LancerCombatDialog) :
 * ordre d'initiative, tour actif, PV, conditions et journal. L'état vit dans [CombatSession],
 * l'écran peut donc être quitté et repris sans rien perdre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombatEnCoursScreen(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) = TexteBlancCombat { CombatEnCoursScreenInterne(onBack, onOpenMenu) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CombatEnCoursScreenInterne(
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val etat by CombatSession.etat.collectAsState()
    var confirmerFin by remember { mutableStateOf(false) }
    var afficherAjout by remember { mutableStateOf(false) }

    Scaffold(
        // La barre du bas de l'appli (AppBottomBar) inclut la barre de navigation Android en plus
        // des 80 dp réservés par le NavHost : sans ce décalage, la barre "Tour suivant" et le
        // bas de la liste passaient dessous.
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = Color.Transparent,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("COMBAT") },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) { Icon(Icons.Default.Menu, contentDescription = "Menu") }
                    },
                )
                Row(modifier = Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                    Spacer(Modifier.weight(1f))
                    if (etat != null) {
                        TextButton(onClick = { confirmerFin = true }) { Text("Terminer le combat") }
                    }
                }
            }
        },
        floatingActionButton = {
            if (etat != null) {
                FloatingActionButton(onClick = { afficherAjout = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un combattant")
                }
            }
        },
        bottomBar = {
            etat?.takeIf { it.demarre }?.let { courant ->
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (courant.phase == PhaseCombat.DECLARATION) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Round ${courant.round} — déclarations", fontWeight = FontWeight.Bold)
                                Text(
                                    "${courant.attendus.size - courant.enAttente.size}/${courant.attendus.size} actions choisies",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(onClick = { CombatSession.passerEnResolution() }) { Text("Résolution") }
                        } else {
                            OutlinedButton(onClick = { CombatSession.tourPrecedent() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = null)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Round ${courant.round}", fontWeight = FontWeight.Bold)
                                Text(
                                    courant.actif?.nom ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            val dernier = courant.combattants.drop(courant.tourIndex + 1).none { !(it.estMonstre && it.horsCombat) }
                            Button(onClick = { CombatSession.tourSuivant() }) {
                                Text(if (dernier) "Round suivant" else "Suivant")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.SkipNext, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val courant = etat
        if (courant == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Aucun combat en cours.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Lancez-en un depuis un lien #combat: d'un scénario.", style = MaterialTheme.typography.bodySmall)
            }
            return@Scaffold
        }
        CombatContenu(etat = courant, modifier = Modifier.fillMaxSize().padding(padding))
    }

    var butinFin by remember { mutableStateOf<Combattant?>(null) }
    butinFin?.let { c -> ButinCombattantDialog(combattant = c, onDismiss = { butinFin = null }) }

    if (confirmerFin) {
        val courant = etat
        val vaincus = courant?.combattants?.filter { it.estMonstre && it.horsCombat }.orEmpty()
        AlertDialog(
            onDismissRequest = { confirmerFin = false },
            title = { Text("Terminer le combat ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (courant != null && courant.monstresRestants > 0) "Il reste ${courant.monstresRestants} adversaire(s) debout. Les PV des personnages sont déjà reportés sur leurs fiches."
                        else "Les PV des personnages sont déjà reportés sur leurs fiches."
                    )
                    // Dernière occasion de fouiller les vaincus avant que le combat ne disparaisse.
                    if (vaincus.isNotEmpty()) {
                        Text("Butin des vaincus", fontWeight = FontWeight.Bold)
                        vaincus.forEach { v ->
                            TextButton(onClick = { butinFin = v }) { Text("💰 ${v.nom}") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmerFin = false
                    CombatSession.fermer()
                    onBack()
                }) { Text("Terminer") }
            },
            dismissButton = { TextButton(onClick = { confirmerFin = false }) { Text("Annuler") } }
        )
    }

    if (afficherAjout) {
        AjouterCombattantDialog(onDismiss = { afficherAjout = false })
    }
}

@Composable
private fun CombatContenu(etat: CombatEnCours, modifier: Modifier) {
    var cibleDegats by remember { mutableStateOf<Combattant?>(null) }
    var cibleConditions by remember { mutableStateOf<String?>(null) }
    var cibleInitiative by remember { mutableStateOf<Combattant?>(null) }
    var ficheMonstre by remember { mutableStateOf<String?>(null) }
    var journalOuvert by remember { mutableStateOf(false) }
    var declarationPour by remember { mutableStateOf<String?>(null) }
    var resolutionPour by remember { mutableStateOf<String?>(null) }
    var profilPour by remember { mutableStateOf<String?>(null) }
    var positionsPour by remember { mutableStateOf<String?>(null) }
    var butinPour by remember { mutableStateOf<Combattant?>(null) }
    var reglagesPour by remember { mutableStateOf<String?>(null) }

    // PJ tenus par un joueur connecté : ceux-là déclarent depuis leur appareil.
    val clients by NetworkSessionManager.connectedClients.collectAsState()
    val reservations by NetworkSessionManager.claimedCharacters.collectAsState()
    val envoyes by NetworkSessionManager.sentCharacterByClient.collectAsState()
    val personnagesConnectes = remember(clients, reservations, envoyes) {
        NetworkSessionManager.connectedCharacters().map { it.id }.toSet()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (etat.demarre) {
            item {
                Text(etat.titre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${etat.combattants.count { !it.estMonstre }} personnage(s) · ${etat.monstresRestants} adversaire(s) debout",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Préparation : deux boutons, le réglage de chaque combattant se fait en appuyant sur sa carte.
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { CombatSession.lancerInitiatives(seulementMonstres = false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Lancer l'initiative")
                    }
                    Button(
                        onClick = { CombatSession.commencer() },
                        enabled = etat.combattants.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Démarrer le combat") }
                }
            }
        }
        if (etat.phase == PhaseCombat.DECLARATION) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Round ${etat.round} — déclaration des actions", fontWeight = FontWeight.Bold)
                        val attente = etat.enAttente
                        Text(
                            if (attente.isEmpty()) "Tout le monde a choisi."
                            else "En attente : " + attente.joinToString { c ->
                                c.nom + if (c.characterId != null && c.characterId !in personnagesConnectes) " (à déclarer par vous)" else ""
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "La résolution démarre seule quand tout le monde a choisi. Les monstres ont déjà décidé (modifiable).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        // En préparation, chaque camp est regroupé ; ensuite, l'ordre d'initiative prime.
        val groupes = if (etat.demarre) listOf<Pair<String?, List<Combattant>>>(null to etat.combattants)
        else listOf(
            "Personnages" to etat.combattants.filter { !it.estMonstre },
            "Adversaires" to etat.combattants.filter { it.estMonstre },
        ).filter { it.second.isNotEmpty() }
        groupes.forEach { (titreGroupe, membres) ->
        if (titreGroupe != null) {
            item(key = "groupe-$titreGroupe") {
                Text(
                    "$titreGroupe (${membres.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (titreGroupe == "Adversaires") Color(0xFFE57373) else Color(0xFF64B5F6),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        items(membres, key = { it.id }) { combattant ->
            CarteCombattant(
                onReglages = if (!etat.demarre) {
                    { reglagesPour = combattant.id }
                } else null,
                combattant = combattant,
                actif = etat.actif?.id == combattant.id,
                phase = etat.phase,
                declaration = etat.declarations[combattant.id],
                jets = etat.jetsDuRound.filter { it.combattantId == combattant.id },
                joueurConnecte = combattant.characterId != null && combattant.characterId in personnagesConnectes,
                onDegats = { cibleDegats = combattant },
                onInitiative = { cibleInitiative = combattant },
                onConditions = { cibleConditions = combattant.id },
                onFiche = combattant.monstreNom?.let { nom -> { ficheMonstre = nom } },
                onRetirer = { CombatSession.retirer(combattant.id) },
                onDeclarer = { declarationPour = combattant.id },
                onResoudre = { resolutionPour = combattant.id },
                onRedecider = { CombatSession.redeciderMonstre(combattant.id) },
                onProfil = { profilPour = combattant.id },
                positions = resumePositions(etat, combattant),
                deplacement = etat.declarations[combattant.id]?.deplacements?.takeIf { it.isNotEmpty() }?.let { deps ->
                    deps.entries.joinToString { (id, d) ->
                        "${d.label.lowercase()} de ${etat.combattants.firstOrNull { it.id == id }?.nom ?: "?"}"
                    }.let { "Finit à " + it + if (etat.declarations[combattant.id]?.desengage == true) " (désengagé)" else "" }
                },
                onPositions = { positionsPour = combattant.id },
                onImpossible = { CombatSession.annulerAction(combattant.id) },
                combattantsEnJeu = etat.combattants,
                relances = etat.relances[combattant.id] ?: 0,
                onButin = if (combattant.estMonstre && combattant.horsCombat) {
                    { butinPour = combattant }
                } else null
            )
        }
        }
        item {
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth().clickable { journalOuvert = !journalOuvert }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Journal (${etat.journal.size})", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (journalOuvert) "Masquer" else "Afficher", color = MaterialTheme.colorScheme.primary)
            }
        }
        if (journalOuvert) {
            items(etat.journal.reversed()) { ligne ->
                Text("• $ligne", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    cibleDegats?.let { cible ->
        DegatsDialog(combattant = cible, onDismiss = { cibleDegats = null })
    }
    cibleInitiative?.let { cible ->
        InitiativeDialog(combattant = cible, onDismiss = { cibleInitiative = null })
    }
    cibleConditions?.let { id ->
        etat.combattants.firstOrNull { it.id == id }?.let { cible ->
            ConditionsDialog(combattant = cible, onDismiss = { cibleConditions = null })
        } ?: run { cibleConditions = null }
    }
    butinPour?.let { c ->
        ButinCombattantDialog(combattant = c, onDismiss = { butinPour = null })
    }
    ficheMonstre?.let { nom ->
        FicheMonstreDialog(nom = nom, onDismiss = { ficheMonstre = null })
    }
    declarationPour?.let { id ->
        etat.combattants.firstOrNull { it.id == id }?.let { c ->
            DeclarationMjDialog(combattant = c, etat = etat, onDismiss = { declarationPour = null })
        } ?: run { declarationPour = null }
    }
    resolutionPour?.let { id ->
        val c = etat.combattants.firstOrNull { it.id == id }
        val d = etat.declarations[id]
        if (c != null && d?.attaque != null) {
            ResolutionMonstreDialog(monstre = c, declaration = d, etat = etat, onDismiss = { resolutionPour = null })
        } else {
            resolutionPour = null
        }
    }
    positionsPour?.let { id ->
        etat.combattants.firstOrNull { it.id == id }?.let { c ->
            PositionsDialog(monstre = c, etat = etat, onDismiss = { positionsPour = null })
        } ?: run { positionsPour = null }
    }
    profilPour?.let { id ->
        etat.combattants.firstOrNull { it.id == id }?.let { c ->
            ComportementIADialog(combattant = c, onDismiss = { profilPour = null })
        } ?: run { profilPour = null }
    }
    reglagesPour?.let { id ->
        etat.combattants.firstOrNull { it.id == id }?.let { c ->
            val joueurConnecte = c.characterId != null && c.characterId in personnagesConnectes
            ReglagesCombattantDialog(
                combattant = c,
                resumePositions = resumePositions(etat, c),
                joueurConnecte = joueurConnecte,
                onInitiative = { cibleInitiative = c },
                onPositions = { positionsPour = c.id },
                onProfil = { profilPour = c.id },
                onDegats = { cibleDegats = c },
                onConditions = { cibleConditions = c.id },
                onFiche = c.monstreNom?.let { nom -> { ficheMonstre = nom } },
                onRetirer = { CombatSession.retirer(c.id) },
                onDismiss = { reglagesPour = null }
            )
        } ?: run { reglagesPour = null }
    }
}

/** Préparation : tous les réglages d'un combattant en un appui sur sa carte. */
@Composable
private fun ReglagesCombattantDialog(
    combattant: Combattant,
    resumePositions: String?,
    joueurConnecte: Boolean,
    onInitiative: () -> Unit,
    onPositions: () -> Unit,
    onProfil: () -> Unit,
    onDegats: () -> Unit,
    onConditions: () -> Unit,
    onFiche: (() -> Unit)?,
    onRetirer: () -> Unit,
    onDismiss: () -> Unit,
) {
    @Composable
    fun Ligne(libelle: String, valeur: String? = null, action: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss(); action() }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(libelle, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (valeur != null) {
                Text(valeur, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(combattant.nom, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Ligne("🎲 Initiative", combattant.initiative?.toString() ?: "–", onInitiative)
                Ligne("📍 Position", resumePositions ?: "Courte par défaut", onPositions)
                if (!joueurConnecte) {
                    val pilote = if (combattant.piloteParIa) (combattant.profilIA ?: ProfilIA.BRUTE).label else "MJ"
                    Ligne("🧠 Comportement", pilote, onProfil)
                }
                Ligne("❤️ PV", "${combattant.pv}/${combattant.pvMax}", onDegats)
                Ligne("Conditions", combattant.conditions.size.takeIf { it > 0 }?.toString(), onConditions)
                if (onFiche != null) Ligne("Voir la fiche", action = onFiche)
                Ligne("Retirer du combat", action = onRetirer)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarteCombattant(
    combattant: Combattant,
    actif: Boolean,
    phase: PhaseCombat,
    declaration: DeclarationAction?,
    jets: List<JetCombat>,
    joueurConnecte: Boolean,
    onDegats: () -> Unit,
    onInitiative: () -> Unit,
    onConditions: () -> Unit,
    onFiche: (() -> Unit)?,
    onRetirer: () -> Unit,
    onDeclarer: () -> Unit,
    onResoudre: () -> Unit,
    onRedecider: () -> Unit,
    onProfil: () -> Unit,
    positions: String?,
    deplacement: String?,
    onPositions: () -> Unit,
    onImpossible: () -> Unit,
    combattantsEnJeu: List<Combattant> = emptyList(),
    relances: Int = 0,
    // Adversaire vaincu : fouiller le corps (butin à distribuer aux joueurs).
    onButin: (() -> Unit)? = null,
    // Préparation : appui sur la carte = fenêtre de réglages du combattant.
    onReglages: (() -> Unit)? = null,
) {
    val couleurCamp = if (combattant.estMonstre) Color(0xFFE57373) else Color(0xFF64B5F6)
    var menuOuvert by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onReglages != null) Modifier.clickable(onClick = onReglages) else Modifier)
            .then(if (actif) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (combattant.horsCombat) 0.45f else 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (actif) 6.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Initiative : appui pour la saisir/corriger.
                Surface(
                    onClick = onInitiative,
                    shape = MaterialTheme.shapes.small,
                    color = couleurCamp.copy(alpha = 0.2f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(combattant.initiative?.toString() ?: "–", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (actif) {
                            Text("▶ ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            combattant.nom,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (combattant.horsCombat) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = "CA", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" ${combattant.ca}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "   PV ${combattant.pv}/${combattant.pvMax}" +
                                (if (combattant.pvTemporaires > 0) " (+${combattant.pvTemporaires})" else "") +
                                (if (combattant.horsCombat) if (combattant.estMonstre) " — vaincu" else " — à terre, jets contre la mort" else ""),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                TextButton(onClick = onDegats) { Text("PV ±") }
                Box {
                    IconButton(onClick = { menuOuvert = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Actions") }
                    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                        DropdownMenuItem(text = { Text("Conditions") }, onClick = { menuOuvert = false; onConditions() })
                        DropdownMenuItem(text = { Text("Position") }, onClick = { menuOuvert = false; onPositions() })
                        // Un PJ tenu par un joueur connecté décide lui-même : pas d'IA à régler.
                        if (!joueurConnecte) {
                            val pilote = when {
                                combattant.piloteParIa -> (combattant.profilIA ?: ProfilIA.BRUTE).label +
                                    if (combattant.reglesIA.isNotEmpty()) " +${combattant.reglesIA.size}" else ""
                                else -> "MJ"
                            }
                            DropdownMenuItem(
                                text = { Text("Comportement IA : $pilote") },
                                onClick = { menuOuvert = false; onProfil() }
                            )
                        }
                        if (onFiche != null) {
                            DropdownMenuItem(text = { Text("Voir la fiche") }, onClick = { menuOuvert = false; onFiche() })
                        }
                        DropdownMenuItem(text = { Text("Retirer du combat") }, onClick = { menuOuvert = false; onRetirer() })
                    }
                }
            }
            val ratio = if (combattant.pvMax > 0) combattant.pv.toFloat() / combattant.pvMax else 0f
            LinearProgressIndicator(
                progress = { ratio.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp),
                color = when {
                    ratio > 0.5f -> Color(0xFF66BB6A)
                    ratio > 0.25f -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                },
            )
            if (combattant.conditions.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    combattant.conditions.sortedBy { it.ordinal }.forEach { condition ->
                        Surface(
                            onClick = { CombatSession.basculerCondition(combattant.id, condition) },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(condition.label, style = MaterialTheme.typography.labelSmall)
                                Icon(Icons.Default.Close, contentDescription = "Retirer ${condition.label}", modifier = Modifier.size(12.dp).padding(start = 2.dp))
                            }
                        }
                    }
                }
                // Ce que ces états empêchent ou imposent, sans aller lire le livre États.
                Etats.bloquant(combattant.conditions)?.let {
                    Text("⛔ ${it.label} : aucune action possible", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                Etats.immobilisant(combattant.conditions)?.let {
                    Text("⚓ ${it.label} : vitesse 0", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (combattant.piloteParIa) {
                // Comportement IA visible d'emblée sur la carte (MJ seulement), appui pour le changer :
                // son nom seul, le détail est dans la fenêtre du comportement.
                val profil = combattant.profilIA ?: ProfilIA.BRUTE
                Text(
                    "🧠 IA : ${profil.label}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable(onClick = onProfil)
                )
            }
            if (onButin != null) {
                TextButton(onClick = onButin, modifier = Modifier.padding(top = 4.dp)) {
                    Text("💰 Fouiller le corps (butin)")
                }
            }
            if (!combattant.horsCombat) {
                // Appui : placer ce combattant par rapport à chacun de ses adversaires.
                Text(
                    "📍 " + (positions ?: "Placer par rapport aux adversaires"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable(onClick = onPositions)
                )
            }
            if (phase != PhaseCombat.PREPARATION && !(combattant.estMonstre && combattant.horsCombat)) {
                BlocDeclaration(
                    combattants = combattantsEnJeu,
                    relances = relances,
                    deplacement = deplacement,
                    onImpossible = onImpossible,
                    combattant = combattant,
                    actif = actif,
                    phase = phase,
                    declaration = declaration,
                    jets = jets,
                    joueurConnecte = joueurConnecte,
                    onDeclarer = onDeclarer,
                    onResoudre = onResoudre,
                    onRedecider = onRedecider,
                )
            }
        }
    }
}

@Composable
private fun DegatsDialog(combattant: Combattant, onDismiss: () -> Unit) {
    var saisie by remember { mutableStateOf("") }
    val montant = saisie.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(combattant.nom, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PV actuels : ${combattant.pv}/${combattant.pvMax}")
                OutlinedTextField(
                    value = saisie,
                    onValueChange = { saisie = it.filter(Char::isDigit).take(4) },
                    label = { Text("Montant") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row {
                TextButton(enabled = montant != null, onClick = {
                    CombatSession.appliquerPv(combattant.id, -(montant ?: 0)); onDismiss()
                }) { Text("Soigner", color = Color(0xFF66BB6A)) }
                TextButton(enabled = montant != null, onClick = {
                    CombatSession.appliquerPv(combattant.id, montant ?: 0); onDismiss()
                }) { Text("Dégâts", color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
private fun InitiativeDialog(combattant: Combattant, onDismiss: () -> Unit) {
    var saisie by remember { mutableStateOf(combattant.initiative?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Initiative — ${combattant.nom}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bonus d'initiative : ${signe(combattant.bonusInitiative)}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = saisie,
                    onValueChange = { v -> saisie = v.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(3) },
                    label = { Text("Résultat du jet") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = saisie.toIntOrNull() != null, onClick = {
                saisie.toIntOrNull()?.let { CombatSession.definirInitiative(combattant.id, it) }
                onDismiss()
            }) { Text("Valider") }
        },
        dismissButton = {
            TextButton(onClick = {
                CombatSession.definirInitiative(combattant.id, kotlin.random.Random.nextInt(1, 21) + combattant.bonusInitiative)
                onDismiss()
            }) { Text("Lancer le d20") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConditionsDialog(combattant: Combattant, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conditions — ${combattant.nom}") },
        text = {
            // Combattant relu dans la session : les puces suivent chaque bascule.
            val etat by CombatSession.etat.collectAsState()
            val actuel = etat?.combattants?.firstOrNull { it.id == combattant.id } ?: combattant
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Etats.choisissables.forEach { condition ->
                        FilterChip(
                            selected = condition in actuel.conditions,
                            onClick = { CombatSession.basculerCondition(actuel.id, condition) },
                            label = { Text(condition.label) }
                        )
                    }
                }
                ResumeEffetsEtats(actuel.conditions)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

/**
 * Butin d'un adversaire vaincu : monstre du bestiaire (butin généré d'après sa fiche) ou
 * personnage ennemi (ses possessions réelles). Même butin à chaque réouverture pour ce combattant.
 */
@Composable
private fun ButinCombattantDialog(combattant: Combattant, onDismiss: () -> Unit) {
    val personnage = combattant.characterId?.let { id -> GameState.characters.value.firstOrNull { it.id == id } }
    if (personnage != null) {
        com.jc2.jdrcompagnon.feature_butin.ui.ButinPersonnageDialog(personnage, cle = "combat:${combattant.id}", onDismiss = onDismiss)
    } else {
        com.jc2.jdrcompagnon.feature_butin.ui.ButinMonstreDialog(
            nom = combattant.monstreNom ?: combattant.nom,
            cle = "combat:${combattant.id}",
            worldId = GameState.currentWorldId(),
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun FicheMonstreDialog(nom: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var contenu by remember(nom) { mutableStateOf<String?>(null) }
    var chargement by remember(nom) { mutableStateOf(true) }
    LaunchedEffect(nom) {
        contenu = SrdRepository.getMonsterByName(context, nom, GameState.currentWorldId())?.rawMarkdown
        chargement = false
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nom, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Fermer") }
                }
                Box(modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    when {
                        chargement -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        contenu.isNullOrBlank() -> Text("Fiche introuvable pour « $nom ».", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else -> Markdown(content = contenu!!)
                    }
                }
            }
        }
    }
}

/** Ajout en cours de combat : renfort de monstres du bestiaire, ou personnage (PJ/PNJ). */
@Composable
private fun AjouterCombattantDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val worldId = GameState.currentWorldId()
    var onglet by remember { mutableIntStateOf(0) }
    var recherche by remember { mutableStateOf("") }
    var quantite by remember { mutableIntStateOf(1) }
    var monstres by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(worldId) {
        monstres = SrdRepository.loadMonsters(context, worldId).map { it.name }
    }
    val personnages by GameState.characters.collectAsState()
    val combat by CombatSession.etat.collectAsState()
    val dejaEnCombat = combat?.combattants?.mapNotNull { it.characterId }?.toSet().orEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter au combat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TabRow(selectedTabIndex = onglet) {
                    Tab(selected = onglet == 0, onClick = { onglet = 0 }, text = { Text("Monstre") })
                    Tab(selected = onglet == 1, onClick = { onglet = 1 }, text = { Text("Personnage") })
                }
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    label = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (onglet == 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Quantité", modifier = Modifier.weight(1f))
                        IconButton(onClick = { quantite = (quantite - 1).coerceAtLeast(1) }) { Icon(Icons.Default.Remove, contentDescription = "Moins") }
                        Text("$quantite", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { quantite = (quantite + 1).coerceAtMost(20) }) { Icon(Icons.Default.Add, contentDescription = "Plus") }
                    }
                    val filtres = monstres.filter { recherche.isBlank() || it.contains(recherche.trim(), ignoreCase = true) }.take(50)
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(filtres) { nom ->
                            TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                val existants = combat?.combattants?.count { it.monstreNom == nom } ?: 0
                                // onDismiss après l'ajout : fermer le dialogue annulerait `scope`.
                                scope.launch {
                                    ajouterMonstres(context, nom, quantite, existants, worldId)
                                    onDismiss()
                                }
                            }) { Text(nom) }
                        }
                    }
                } else {
                    val filtres = personnages
                        .filter { it.id !in dejaEnCombat && it.type != "Monstre" }
                        .filter { worldId == null || it.worldId.isBlank() || it.worldId == worldId }
                        .filter { recherche.isBlank() || it.name.contains(recherche.trim(), ignoreCase = true) }
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(filtres) { perso ->
                            if (perso.type == "PJ") {
                                TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    CombatSession.ajouter(listOf(CombatSession.combattantDepuisPersonnage(perso)))
                                    onDismiss()
                                }) { Text("${perso.name} (${perso.type})") }
                            } else {
                                // PNJ : allié des joueurs (piloté par le MJ, IA possible ensuite) ou
                                // ennemi (piloté par l'IA avec les attaques de ses armes).
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text("${perso.name} (${perso.type})", modifier = Modifier.weight(1f))
                                    TextButton(onClick = {
                                        scope.launch {
                                            // Allié : piloté par l'IA seulement si un profil est choisi sur sa fiche.
                                            val base = CombatSession.combattantDepuisPersonnage(perso)
                                            val combattant = if (perso.profilIA == null) base else {
                                                val profil = profilPersonnage(context, perso)
                                                base.copy(attaques = attaquesDuPersonnage(context, perso), profilIA = profil.profil, raisonProfil = profil.raison)
                                            }
                                            CombatSession.ajouter(listOf(combattant))
                                            onDismiss()
                                        }
                                    }) { Text("Allié") }
                                    TextButton(onClick = {
                                        scope.launch {
                                            val attaques = attaquesDuPersonnage(context, perso)
                                            val profil = profilPersonnage(context, perso)
                                            CombatSession.ajouter(
                                                listOf(
                                                    CombatSession.combattantDepuisPersonnage(perso, ennemi = true)
                                                        .copy(attaques = attaques, profilIA = profil.profil, raisonProfil = profil.raison)
                                                )
                                            )
                                            onDismiss()
                                        }
                                    }) { Text("Ennemi", color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

private suspend fun ajouterMonstres(context: Context, nom: String, quantite: Int, existants: Int, worldId: String?) {
    val fiche = SrdRepository.getMonsterByName(context, nom, worldId)
    val profil = fiche?.let { ProfilCombatMonstre.depuisFiche(it.rawMarkdown) } ?: ProfilCombatMonstre(10, 10, null, 0)
    val numeroter = quantite > 1 || existants > 0
    CombatSession.ajouter((1..quantite).map { i ->
        Combattant(
            id = UUID.randomUUID().toString(),
            nom = if (numeroter) "$nom ${existants + i}" else nom,
            estMonstre = true,
            monstreNom = nom,
            ca = profil.ca,
            pvMax = profil.pvMoyens,
            pv = profil.pvMoyens,
            bonusInitiative = profil.bonusInitiative,
            attaques = profil.actions.attaques,
            nbAttaquesMultiples = profil.actions.nbAttaquesMultiples,
            profilIA = profil.comportement.profil,
            raisonProfil = profil.comportement.raison,
            vitesse = profil.vitesse
        )
    })
}
