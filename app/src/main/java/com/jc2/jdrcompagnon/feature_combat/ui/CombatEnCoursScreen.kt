package com.jc2.jdrcompagnon.feature_combat.ui

import android.content.Context
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.ui.draw.alpha
import com.jc2.jdrcompagnon.network.versData
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
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
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SportsMartialArts
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
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
import com.jc2.jdrcompagnon.feature_combat.domain.model.Terrain
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.presentation.PreparationBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatEnCours
import com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession
import com.jc2.jdrcompagnon.network.versJoueur
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
    val sonsActifs = com.jc2.jdrcompagnon.feature_combat.presentation.ReglagesCombatStore.reglages.collectAsState().value.sons
    var confirmerFin by remember { mutableStateOf(false) }
    var afficherAjout by remember { mutableStateOf(false) }
    // Vue tactique du MJ : position de chacun (au contact de qui) et comportement IA.
    var vueTactique by remember { mutableStateOf(false) }

    // Personnages (PJ, PNJ) pilotés par l'IA : leurs attaques sont tirées de leurs armes équipées,
    // une fois par combattant, puis l'IA redécide avec ses vraies armes.
    val context = LocalContext.current
    remember { PreferencesCombat.init(context) }
    val attaquesChargees = remember { mutableSetOf<String>() }
    val aEquiper = etat?.combattants.orEmpty()
        .filter { it.characterId != null && it.piloteParIa && it.attaques.isEmpty() && it.id !in attaquesChargees }
        .map { it.id to it.characterId!! }
    LaunchedEffect(aEquiper) {
        aEquiper.forEach { (combattantId, characterId) ->
            attaquesChargees += combattantId
            val perso = GameState.characters.value.firstOrNull { it.id == characterId } ?: return@forEach
            val attaques = attaquesDuPersonnage(context, perso)
            if (attaques.isEmpty()) return@forEach
            CombatSession.definirAttaques(combattantId, attaques)
            CombatSession.redeciderMonstre(combattantId)
        }
    }
    // Musique de combat / de boss (réglages), et retour à la piste d'avant quand le combat se ferme.
    LaunchedEffect(etat?.id, etat?.phase, etat?.boss?.isNotEmpty()) { MusiqueCombat.suivre(context, etat) }

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
                    actions = {
                        if (etat != null) {
                            IconButton(onClick = { SonsCombat.basculer() }) {
                                Icon(
                                    if (sonsActifs) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = if (sonsActifs) "Couper les sons" else "Activer les sons",
                                )
                            }
                            IconButton(onClick = { vueTactique = !vueTactique }) {
                                Icon(
                                    if (vueTactique) Icons.Default.SportsMartialArts else Icons.Default.Radar,
                                    contentDescription = if (vueTactique) "Vue combat" else "Vue tactique",
                                    tint = if (vueTactique) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                )
                            }
                            IconButton(onClick = { afficherAjout = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter un combattant")
                            }
                        }
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
        bottomBar = {
            etat?.takeIf { it.demarre }?.let { courant ->
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { CombatSession.tourPrecedent() }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Tour précédent")
                        }
                        // Annonce à lire, jet contre la mort à noter : on ne passe pas au suivant avant.
                        val blocage = CombatSession.blocageTourSuivant(courant)
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Round ${courant.round}", fontWeight = FontWeight.Bold)
                            Text(
                                blocage ?: courant.actif?.let { "Tour de ${it.nom}" } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (blocage != null) ForcedDarkPalette.AccentGold else LocalContentColor.current,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { CombatSession.tourSuivant() },
                            enabled = blocage == null,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        ) {
                            Text(if (courant.dernierDuRound) "Round suivant" else "Tour suivant", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.SkipNext, contentDescription = null)
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
        if (vueTactique) {
            TableauTactique(
                combat = remember(courant) { courant.versJoueur(null, pourMj = true) },
                moiId = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            // Sons du récit et bannières (nouveau round, critique, victoire) par-dessus l'écran.
            val lignes = remember(courant.recit) { courant.recit.map { it.versData() } }
            EffetsRecit(lignes = lignes, actifId = courant.actif?.id, modifier = Modifier.fillMaxSize().padding(padding)) {
                CombatContenu(etat = courant, modifier = Modifier.fillMaxSize())
            }
        }
    }

    var butinFin by remember { mutableStateOf<Combattant?>(null) }
    // Butin envoyé au groupe à la fin du combat (victoire, ou « Terminer » avec des vaincus).
    var bilanFin by remember { mutableStateOf<com.jc2.jdrcompagnon.feature_combat.presentation.FinDeCombat.Bilan?>(null) }
    var finManuelle by remember { mutableStateOf(false) }
    // Boss vaincus : récapitulatif (coup final, dégâts de chacun) affiché avec le butin.
    var bossFin by remember { mutableStateOf<List<com.jc2.jdrcompagnon.network.BossJoueurData>>(emptyList()) }
    val scopeFin = rememberCoroutineScope()
    butinFin?.let { c -> ButinCombattantDialog(combattant = c, onDismiss = { butinFin = null }) }

    if (confirmerFin) {
        val courant = etat
        val vaincus = courant?.combattants?.filter { it.estMonstre && it.horsCombat && !it.inerte }.orEmpty()
        AlertDialog(
            onDismissRequest = { confirmerFin = false },
            title = { Text("Terminer le combat ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (courant != null && courant.monstresRestants > 0) "Il reste ${courant.monstresRestants} adversaire(s) debout. Les PV des personnages sont déjà reportés sur leurs fiches."
                        else "Les PV des personnages sont déjà reportés sur leurs fiches."
                    )
                    // Fouille possible avant la fin ; le reste du butin des vaincus est proposé au groupe.
                    if (vaincus.isNotEmpty()) {
                        Text("Butin des vaincus", fontWeight = FontWeight.Bold)
                        Text(
                            "En terminant, le butin restant est proposé à tous les joueurs du groupe (« Je le veux »).",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        vaincus.forEach { v ->
                            TextButton(onClick = { butinFin = v }) { Text("💰 ${v.nom}") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmerFin = false
                    if (courant == null || vaincus.isEmpty()) {
                        CombatSession.fermer()
                        MusiqueCombat.terminer()
                        onBack()
                    } else {
                        scopeFin.launch {
                            finManuelle = true
                            bossFin = courant.versJoueur(null, pourMj = true).boss.filter { it.vaincu }
                            bilanFin =com.jc2.jdrcompagnon.feature_combat.presentation.FinDeCombat.distribuerButin(context, courant)
                            CombatSession.fermer()
                            MusiqueCombat.terminer()
                        }
                    }
                }) { Text("Terminer") }
            },
            dismissButton = { TextButton(onClick = { confirmerFin = false }) { Text("Annuler") } }
        )
    }

    // Tous les ennemis vaincus : le butin part au groupe, puis le combat se ferme tout seul.
    LaunchedEffect(etat?.id, etat?.termine) {
        val fini = etat?.takeIf { it.termine } ?: return@LaunchedEffect
        confirmerFin = false
        finManuelle = false
        bossFin = fini.versJoueur(null, pourMj = true).boss.filter { it.vaincu }
        bilanFin =com.jc2.jdrcompagnon.feature_combat.presentation.FinDeCombat.distribuerButin(context, fini)
        CombatSession.fermer()
        MusiqueCombat.terminer()
    }
    bilanFin?.let { bilan ->
        AlertDialog(
            onDismissRequest = { bilanFin = null; onBack() },
            title = { Text(if (finManuelle) "Combat terminé" else "Victoire !") },
            text = {
                Column(modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    bossFin.forEach { BandeauBoss(it, nbAnnonces = 1) }
                    Text(if (finManuelle) "Le butin des vaincus est partagé avec le groupe." else "Tous les ennemis sont vaincus : le combat est terminé.")
                    when {
                        bilan.groupe == null -> Text(
                            "Aucun groupe sélectionné : le butin n'a pas été distribué (fouillez les vaincus depuis un nouveau combat ou le bestiaire).",
                            color = MaterialTheme.colorScheme.error
                        )
                        bilan.vide -> Text("Les vaincus n'avaient rien de valeur.")
                        else -> {
                            Text("Butin envoyé au groupe « ${bilan.groupe} » :", fontWeight = FontWeight.Bold)
                            if (bilan.orPo > 0) Text("💰 ${bilan.orPo} po versées au trésor commun")
                            bilan.objets.groupingBy { it }.eachCount().forEach { (nom, n) ->
                                Text("• " + if (n > 1) "$nom ×$n" else nom)
                            }
                            if (bilan.objets.isNotEmpty()) Text(
                                if (bilan.inventaireCommun) "Aucun joueur du groupe n'est connecté : les objets sont rangés dans l'inventaire commun du groupe."
                                else "Chaque joueur du groupe reçoit les objets : à eux de dire qui prend quoi (« Je le veux »).",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { bilanFin = null; onBack() }) { Text("OK") } }
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
    var declarationPour by remember { mutableStateOf<String?>(null) }
    var resolutionPour by remember { mutableStateOf<String?>(null) }
    var profilPour by remember { mutableStateOf<String?>(null) }
    var positionsPour by remember { mutableStateOf<String?>(null) }
    var butinPour by remember { mutableStateOf<Combattant?>(null) }
    var reglagesPour by remember { mutableStateOf<String?>(null) }
    var actionsPour by remember { mutableStateOf<String?>(null) }
    // Combattants dépliés dans l'ordre du round (carte complète au lieu de la ligne résumée).
    var deplies by remember { mutableStateOf<Set<String>>(emptySet()) }
    val recitComplet by PreferencesCombat.recitComplet.collectAsState()
    val journalTechnique by PreferencesCombat.journalTechnique.collectAsState()

    // PJ tenus par un joueur connecté : ceux-là choisissent leur action depuis leur appareil.
    val clients by NetworkSessionManager.connectedClients.collectAsState()
    val reservations by NetworkSessionManager.claimedCharacters.collectAsState()
    val envoyes by NetworkSessionManager.sentCharacterByClient.collectAsState()
    val personnagesConnectes = remember(clients, reservations, envoyes) {
        NetworkSessionManager.connectedCharacters().map { it.id }.toSet()
    }
    fun connecte(c: Combattant) = c.characterId != null && c.characterId in personnagesConnectes

    val carte: @Composable (Combattant) -> Unit = { combattant ->
        CarteCombattant(
            // Préparation : réglages ; en combat, appui sur un monstre = ses actions autorisées.
            onReglages = when {
                !etat.demarre -> {
                    { reglagesPour = combattant.id }
                }
                combattant.estMonstre && combattant.attaques.isNotEmpty() -> {
                    { actionsPour = combattant.id }
                }
                else -> null
            },
            onActions = if (combattant.estMonstre && combattant.attaques.isNotEmpty()) {
                { actionsPour = combattant.id }
            } else null,
            combattant = combattant,
            actif = etat.actif?.id == combattant.id,
            phase = etat.phase,
            declaration = etat.declarations[combattant.id],
            jets = etat.jetsDuRound.filter { it.combattantId == combattant.id },
            joueurConnecte = connecte(combattant),
            onDegats = { cibleDegats = combattant },
            onInitiative = { cibleInitiative = combattant },
            onConditions = { cibleConditions = combattant.id },
            onFiche = combattant.monstreNom?.let { nom -> { ficheMonstre = nom } },
            onRetirer = { CombatSession.retirer(combattant.id) },
            onDeclarer = { declarationPour = combattant.id },
            onResoudre = { resolutionPour = combattant.id },
            onRedecider = { CombatSession.redeciderMonstre(combattant.id) },
            onProfil = { profilPour = combattant.id },
            deplacement = etat.declarations[combattant.id]?.deplacements?.takeIf { it.isNotEmpty() }?.let { deps ->
                deps.entries.joinToString { (id, d) ->
                    "${d.label.lowercase()} de ${etat.combattants.firstOrNull { it.id == id }?.nom ?: "?"}"
                }.let { "Finit à " + it + if (etat.declarations[combattant.id]?.desengage == true) " (désengagé)" else "" }
            },
            onPositions = { positionsPour = combattant.id },
            onImpossible = { CombatSession.annulerAction(combattant.id) },
            combattantsEnJeu = etat.combattants,
            relances = etat.relances[combattant.id] ?: 0,
            onButin = if (combattant.estMonstre && combattant.horsCombat && !combattant.inerte) {
                { butinPour = combattant }
            } else null
        )
    }

    val vueDetaillee by PreferencesCombat.vueDetaillee.collectAsState()
    val annonceALire = etat.boss.values.any { b -> b.annonces.any { it.aLire && !it.lue } }
    Column(modifier = modifier) {
    // Frise d'initiative toujours visible en haut, hors de la liste qui défile.
    if (etat.demarre) {
        Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) { FriseInitiative(etat) }
    }
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mise en scène du boss à lire aux joueurs : en tête, jusqu'à ce que le MJ la marque lue.
        if (annonceALire) {
            item(key = "a-lire") { ALireAVoixHaute(etat) }
        }
        if (!etat.demarre) {
            // Préparation : placement de départ sur le terrain, puis deux boutons ; le réglage de
            // chaque combattant se fait en appuyant sur sa carte.
            item(key = "terrain-prep") { TerrainMj(etat) }
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
                Text(
                    "Chacun jouera à son tour, dans l'ordre d'initiative. Les joueurs sans l'appli annoncent leur action, vous la saisissez (ou ils passent par la page web de la table).",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            listOf(
                "Personnages" to etat.combattants.filter { !it.estMonstre },
                "Adversaires" to etat.combattants.filter { it.estMonstre },
            ).filter { it.second.isNotEmpty() }.forEach { (titreGroupe, membres) ->
                item(key = "groupe-$titreGroupe") {
                    Text(
                        "$titreGroupe (${membres.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (titreGroupe == "Adversaires") Color(0xFFE57373) else Color(0xFF64B5F6),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(membres, key = { it.id }) { carte(it) }
            }
        } else {
            // Une seule étape à la fois : l'annonce à lire d'abord, puis le tour en cours.
            if (!annonceALire) etat.actif?.let { actif ->
                item(key = "tour-${actif.id}") {
                    TourEnCours(
                        etat = etat,
                        actif = actif,
                        joueurConnecte = connecte(actif),
                        carte = { carte(actif) },
                        onAutreAction = { declarationPour = actif.id },
                    )
                }
            }
            item(key = "terrain") { TerrainMj(etat) }
            item(key = "details") {
                TextButton(onClick = { PreferencesCombat.basculerVueDetaillee() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (vueDetaillee) "▲ Masquer les détails" else "▼ Boss, récit et ordre du round")
                }
            }
            if (!vueDetaillee) return@LazyColumn
            // Boss : bandeau de la table et commandes du MJ (actions légendaires, repaire, phases).
            if (etat.boss.isNotEmpty()) {
                val vueTable = etat.versJoueur(null, pourMj = true).boss
                items(etat.boss.keys.toList(), key = { "boss-$it" }) { id ->
                    PanneauBossMj(etat, id, vueTable.firstOrNull { it.combattantId == id })
                }
            }
            item(key = "recit") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📜 Récit du combat", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { PreferencesCombat.basculerRecitComplet() }) {
                                Text(if (recitComplet) "Réduire" else "Tout voir")
                            }
                        }
                        Text(
                            "📢 en jaune : à lire à voix haute aux joueurs. Le reste est facultatif, pour raconter l'action si vous le souhaitez. " +
                                "Les joueurs ne voient que les faits (qui attaque qui, touché, dégâts).",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        val lignes = remember(etat.recit) { etat.recit.map { it.versData() } }
                        FilRecit(lignes, nombre = if (recitComplet) 200 else 6)
                        TextButton(onClick = { PreferencesCombat.basculerJournalTechnique() }) {
                            Text(if (journalTechnique) "Masquer le détail des dés" else "Détail des dés (${etat.journal.size})")
                        }
                        if (journalTechnique) {
                            etat.journal.takeLast(if (recitComplet) 300 else 15).reversed().forEach { ligne ->
                                Text("• $ligne", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
            // Ordre du round à partir du suivant ; ceux qui ont déjà joué ensuite.
            val apres = etat.combattants.drop(etat.tourIndex + 1)
            val avant = etat.combattants.take(etat.tourIndex)
            listOf("À venir ce round" to apres, "Ont déjà joué" to avant).filter { it.second.isNotEmpty() }.forEach { (titre, liste) ->
                item(key = "titre-$titre") {
                    Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                items(liste, key = { it.id }) { c ->
                    if (c.id in deplies) {
                        Column {
                            carte(c)
                            TextButton(onClick = { deplies = deplies - c.id }) { Text("Replier") }
                        }
                    } else {
                        LigneCombattant(
                            combattant = c,
                            declaration = etat.declarations[c.id],
                            prochain = etat.prochain?.id == c.id,
                            onClick = { deplies = deplies + c.id },
                        )
                    }
                }
            }
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
            ReglagesCombattantDialog(
                combattant = c,
                resumePositions = resumePositions(etat, c),
                joueurConnecte = connecte(c),
                onInitiative = { cibleInitiative = c },
                onPositions = { positionsPour = c.id },
                onProfil = { profilPour = c.id },
                onDegats = { cibleDegats = c },
                onConditions = { cibleConditions = c.id },
                onFiche = c.monstreNom?.let { nom -> { ficheMonstre = nom } },
                onActions = if (c.estMonstre && c.attaques.isNotEmpty()) {
                    { actionsPour = c.id }
                } else null,
                onRetirer = { CombatSession.retirer(c.id) },
                onDismiss = { reglagesPour = null }
            )
        } ?: run { reglagesPour = null }
    }
    actionsPour?.let { id ->
        etat.combattants.firstOrNull { it.id == id }?.let { c ->
            ActionsAutoriseesDialog(combattant = c, onDismiss = { actionsPour = null })
        } ?: run { actionsPour = null }
    }
}

/**
 * Textes de mise en scène d'un boss (entrée en scène, changement de phase, attaque qui se
 * prépare…) : le MJ les lit à voix haute, puis appuie sur « C'est lu ». Les joueurs et l'écran de
 * la table n'en voient jamais le texte, seulement le fait (« Cryovain : phase 2 »).
 */
@Composable
private fun ALireAVoixHaute(etat: CombatEnCours) {
    val aLire = etat.boss.flatMap { (id, b) -> b.annonces.withIndex().filter { it.value.aLire && !it.value.lue }.map { id to it.value } }
    val indices = etat.boss.flatMap { (_, b) -> b.annonces.withIndex().filter { it.value.aLire && !it.value.lue }.map { it.index } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, ForcedDarkPalette.AccentGold, MaterialTheme.shapes.large)
            .background(ForcedDarkPalette.AccentGold.copy(alpha = 0.14f), MaterialTheme.shapes.large)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("📢 À lire à voix haute aux joueurs", fontWeight = FontWeight.ExtraBold, color = ForcedDarkPalette.AccentGold)
        aLire.forEachIndexed { n, (bossId, annonce) ->
            Text(
                annonce.texte,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = Color.White,
            )
            // Action de repaire : résolue par l'appli, jets et dégâts déjà appliqués.
            if (annonce.resultat.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.35f), MaterialTheme.shapes.medium).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("🎲 Résolu par l'appli (déjà appliqué) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                    annonce.resultat.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Color.White) }
                    if (annonce.avant.isNotEmpty() && !annonce.annulee) {
                        var modifierDegats by remember(bossId, indices[n]) { mutableStateOf(false) }
                        Row {
                            TextButton(onClick = { modifierDegats = true }) { Text("Modifier les dégâts") }
                            TextButton(onClick = { CombatSession.annulerAnnonce(bossId, indices[n]) }) { Text("Ne pas appliquer") }
                        }
                        if (modifierDegats) DegatsAnnonceDialog(etat, bossId, indices[n], annonce, onDismiss = { modifierDegats = false })
                    }
                }
            }
        }
        Button(
            onClick = { aLire.map { it.first }.distinct().forEach { CombatSession.marquerAnnoncesLues(it) } },
            modifier = Modifier.align(Alignment.End),
        ) { Text("✓ C'est lu") }
    }
}

/**
 * Dégâts d'une action de repaire ou légendaire choisis par le MJ, pour chaque combattant touché :
 * pré-remplis avec ceux réellement subis (PV d'avant l'action moins PV actuels).
 */
@Composable
private fun DegatsAnnonceDialog(
    etat: CombatEnCours,
    bossId: String,
    index: Int,
    annonce: com.jc2.jdrcompagnon.feature_combat.domain.model.AnnonceBoss,
    onDismiss: () -> Unit,
) {
    val saisies = remember(annonce) {
        androidx.compose.runtime.mutableStateMapOf<String, String>().apply {
            annonce.avant.forEach { avant ->
                val actuel = etat.combattants.firstOrNull { it.id == avant.id }
                val subis = actuel?.let { (avant.pv + avant.pvTemporaires) - (it.pv + it.pvTemporaires) }?.coerceAtLeast(0) ?: 0
                put(avant.id, subis.toString())
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dégâts à appliquer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                annonce.avant.forEach { avant ->
                    OutlinedTextField(
                        value = saisies[avant.id].orEmpty(),
                        onValueChange = { v -> saisies[avant.id] = v.filter(Char::isDigit).take(3) },
                        label = { Text("${avant.nom} (PV avant : ${avant.pv}/${avant.pvMax})") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                CombatSession.ajusterDegatsAnnonce(bossId, index, saisies.mapValues { it.value.toIntOrNull() ?: 0 })
                onDismiss()
            }) { Text("Appliquer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

/** Qui décide pour [c] : son joueur (appli ou table), l'IA ou le MJ. */
private fun pilotage(c: Combattant, joueurConnecte: Boolean): String = when {
    joueurConnecte -> "📱 Joué sur l'appli"
    c.piloteParIa -> "🤖 IA · ${c.comportement.nom}"
    !c.estMonstre -> "🎲 Joué à la table"
    else -> "🎭 Mené par le MJ"
}

/**
 * Le tour en cours, au centre de l'écran : qui joue, qui le pilote, sa carte (action, jets,
 * boutons pour appliquer), et pour un personnage joué à la table, la saisie de son action.
 */
@Composable
private fun TourEnCours(
    etat: CombatEnCours,
    actif: Combattant,
    joueurConnecte: Boolean,
    carte: @Composable () -> Unit,
    onAutreAction: () -> Unit,
) {
    val couleurCamp = if (actif.estMonstre) Color(0xFFE57373) else Color(0xFF64B5F6)
    // Halo doré qui pulse doucement : on voit tout de suite qui joue.
    val pulsation = rememberInfiniteTransition(label = "tour")
    val alphaHalo by pulsation.animateFloat(
        initialValue = 0.35f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "halo",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, ForcedDarkPalette.AccentGold.copy(alpha = alphaHalo), MaterialTheme.shapes.large)
            .background(couleurCamp.copy(alpha = 0.10f), MaterialTheme.shapes.large)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    (if (actif.estMonstre) "Tour de " else "À toi, ") + actif.nom + if (actif.estMonstre) "" else " !",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ForcedDarkPalette.AccentGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Round ${etat.round} · ${pilotage(actif, joueurConnecte)}", style = MaterialTheme.typography.bodySmall)
            }
            etat.prochain?.let { p ->
                Column(horizontalAlignment = Alignment.End) {
                    Text("Ensuite", style = MaterialTheme.typography.labelSmall)
                    Text(p.nom, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        carte()
        val declaration = etat.declarations[actif.id]
        // Personnage à 0 PV : le MJ note le d20 lancé à la table, ou le fait lancer par l'appli.
        val jetMortEnAttente = CombatSession.jetContreLaMortEnAttente(etat, actif)
        if (jetMortEnAttente) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💀 Jet de sauvegarde contre la mort", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "${actif.nom} lance un d20 : 10 ou plus = réussite, 1 = deux échecs, 20 = se relève avec 1 PV. (${actif.succesMort} ✓ / ${actif.echecsMort} ✗)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                    )
                    var saisie by remember(actif.id, etat.round) { mutableStateOf("") }
                    val valeur = saisie.toIntOrNull()?.takeIf { it in 1..20 }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = saisie,
                            onValueChange = { saisie = it.filter(Char::isDigit).take(2) },
                            label = { Text("d20") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.width(90.dp),
                        )
                        Button(onClick = { CombatSession.noterJetContreLaMort(actif.id, valeur) }, enabled = valeur != null) { Text("Noter") }
                        TextButton(onClick = { CombatSession.noterJetContreLaMort(actif.id, null) }) { Text("Lancer pour lui") }
                    }
                }
            }
        }
        // Joué à la table : le panneau reste ouvert tout son tour (plusieurs attaques possibles).
        val aSaisir = !joueurConnecte && !actif.piloteParIa && !actif.horsCombat && !actif.inerte &&
            Etats.peutAgir(actif.conditions)
        if (aSaisir) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SaisieActionTable(actif, etat, onAutreAction)
                }
            }
        }
        Text(
            when {
                jetMortEnAttente -> "Notez le jet contre la mort pour passer au tour suivant."
                declaration?.degatsAppliques == true || (declaration != null && declaration.attaque == null && declaration.attaquesJoueur.isEmpty()) ->
                    "Action jouée : passez au tour suivant (en bas)."
                declaration != null -> "Appliquez le résultat, puis passez au tour suivant."
                joueurConnecte -> "Le joueur choisit son action sur son appareil…"
                aSaisir -> "Saisissez ce que le joueur annonce."
                else -> ""
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

/**
 * Frise d'initiative : tous les combattants dans l'ordre, le combattant actif mis en avant, ceux
 * qui ont déjà joué ce round estompés, les vaincus barrés. Elle suit le tour en cours.
 */
@Composable
private fun FriseInitiative(etat: CombatEnCours) {
    val liste = rememberLazyListState()
    LaunchedEffect(etat.tourIndex, etat.round) {
        liste.animateScrollToItem((etat.tourIndex - 1).coerceAtLeast(0))
    }
    LazyRow(state = liste, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(etat.combattants, key = { _, c -> "frise-${c.id}" }) { i, c ->
            val actif = etat.actif?.id == c.id
            val dejaJoue = i < etat.tourIndex
            val couleurCamp = if (c.estMonstre) Color(0xFFE57373) else Color(0xFF64B5F6)
            val ratio = if (c.pvMax > 0) (c.pv.toFloat() / c.pvMax).coerceIn(0f, 1f) else 0f
            Column(
                modifier = Modifier
                    .width(if (actif) 96.dp else 74.dp)
                    .alpha(if (c.horsCombat) 0.35f else if (dejaJoue) 0.6f else 1f)
                    .background(
                        if (actif) ForcedDarkPalette.AccentGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        MaterialTheme.shapes.small,
                    )
                    .border(if (actif) 2.dp else 1.dp, if (actif) ForcedDarkPalette.AccentGold else couleurCamp.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    (c.initiative?.toString() ?: "–") + when {
                        c.horsCombat && c.estMonstre -> " ☠"
                        c.horsCombat -> " ⚠"
                        dejaJoue -> " ✓"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    (if (c.bossId != null) "👑" else "") + c.nom,
                    style = if (actif) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = if (actif) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier.fillMaxWidth().padding(top = 3.dp).height(4.dp),
                    color = when {
                        ratio > 0.5f -> Color(0xFF66BB6A)
                        ratio > 0.25f -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    },
                )
                if (c.conditions.isNotEmpty()) {
                    Text(
                        c.conditions.sortedBy { it.ordinal }.joinToString("") { iconeEtat(it) },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Icône courte d'un état, pour les vues compactes (frise, lignes résumées). */
internal fun iconeEtat(c: ConditionCombat): String = when (c) {
    ConditionCombat.A_TERRE -> "⬇"
    ConditionCombat.AGRIPPE -> "✊"
    ConditionCombat.AVEUGLE -> "🙈"
    ConditionCombat.CHARME -> "💗"
    ConditionCombat.EFFRAYE -> "😱"
    ConditionCombat.EMPOISONNE -> "🤢"
    ConditionCombat.ENTRAVE -> "⛓"
    ConditionCombat.ETOURDI -> "💫"
    ConditionCombat.INCAPABLE_D_AGIR -> "⛔"
    ConditionCombat.INCONSCIENT -> "💤"
    ConditionCombat.INVISIBLE -> "👻"
    ConditionCombat.PARALYSE -> "🧊"
    ConditionCombat.PETRIFIE -> "🗿"
    ConditionCombat.SOURD -> "🔇"
    ConditionCombat.CONCENTRATION -> "🔮"
}

/** Combattant hors de son tour, en une ligne : initiative, nom, santé, états, action du round. */
@Composable
private fun LigneCombattant(combattant: Combattant, declaration: DeclarationAction?, prochain: Boolean, onClick: () -> Unit) {
    val couleurCamp = if (combattant.estMonstre) Color(0xFFE57373) else Color(0xFF64B5F6)
    val ratio = if (combattant.pvMax > 0) (combattant.pv.toFloat() / combattant.pvMax).coerceIn(0f, 1f) else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (combattant.horsCombat) 0.5f else 1f)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), MaterialTheme.shapes.small)
            .border(if (prochain) 1.dp else 0.dp, if (prochain) ForcedDarkPalette.AccentGold.copy(alpha = 0.7f) else Color.Transparent, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).background(couleurCamp.copy(alpha = 0.25f), MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) { Text(combattant.initiative?.toString() ?: "–", fontWeight = FontWeight.Bold) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (combattant.bossId != null) "👑 " else if (combattant.inerte) "🎯 " else "") + combattant.nom +
                        combattant.conditions.sortedBy { it.ordinal }.joinToString("") { " " + iconeEtat(it) },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (prochain) Text("  · ensuite", style = MaterialTheme.typography.labelSmall, color = ForcedDarkPalette.AccentGold)
            }
            Text(
                "PV ${combattant.pv}/${combattant.pvMax}" +
                    (if (combattant.horsCombat) if (combattant.estMonstre) " — vaincu" else if (combattant.mort) " — mort" else " — à terre" else "") +
                    (declaration?.let { " · " + it.libelle + (it.cibleNom?.let { n -> " → $n" } ?: "") } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp),
                color = when {
                    ratio > 0.5f -> Color(0xFF66BB6A)
                    ratio > 0.25f -> Color(0xFFFFA726)
                    else -> Color(0xFFEF5350)
                },
            )
        }
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
    onActions: (() -> Unit)?,
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
                    val pilote = when {
                        combattant.joueParJoueur -> "Son joueur"
                        combattant.piloteParIa -> combattant.comportement.nom
                        else -> "MJ"
                    }
                    Ligne("🧠 Comportement", pilote, onProfil)
                }
                Ligne("❤️ PV", "${combattant.pv}/${combattant.pvMax}", onDegats)
                if (onActions != null) Ligne("⚔️ Actions autorisées", resumeActionsAutorisees(combattant), onActions)
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
    deplacement: String?,
    onPositions: () -> Unit,
    onImpossible: () -> Unit,
    combattantsEnJeu: List<Combattant> = emptyList(),
    relances: Int = 0,
    // Adversaire vaincu : fouiller le corps (butin à distribuer aux joueurs).
    onButin: (() -> Unit)? = null,
    // Préparation : appui sur la carte = fenêtre de réglages du combattant.
    onReglages: (() -> Unit)? = null,
    // Monstre : choix des actions de la fiche qu'il peut réaliser.
    onActions: (() -> Unit)? = null,
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
                            (if (combattant.bossId != null) "👑 " else if (combattant.inerte) "🎯 " else "") + combattant.nom,
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
                                (if (combattant.horsCombat) if (combattant.estMonstre) " — vaincu" else if (combattant.mort) " — mort" else if (combattant.stable) " — stabilisé" else " — à terre (jets contre la mort : ${combattant.succesMort} ✓ / ${combattant.echecsMort} ✗)" else "") +
                                (combattant.typeCreature?.let { " · $it" } ?: "") +
                                (if (com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession.concentre(combattant)) " · concentration" else ""),
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
                                combattant.joueParJoueur -> "son joueur"
                                combattant.piloteParIa -> combattant.comportement.nom +
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
            // Comportement IA et positions : affichés dans la vue tactique (bouton en haut de
            // l'écran), réglables depuis le menu ⋮ de la carte.
            if (onActions != null && !combattant.horsCombat) {
                Text(
                    "⚔️ Actions : " + resumeActionsAutorisees(combattant),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clickable(onClick = onActions)
                )
            }
            if (onButin != null) {
                TextButton(onClick = onButin, modifier = Modifier.padding(top = 4.dp)) {
                    Text("💰 Fouiller le corps (butin)")
                }
            }
            if (phase != PhaseCombat.PREPARATION && !combattant.inerte && !(combattant.estMonstre && combattant.horsCombat)) {
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

/** « toutes (4) » ou « 2/4 ». */
private fun resumeActionsAutorisees(combattant: Combattant): String {
    val total = combattant.attaques.size
    val autorisees = combattant.attaquesAutorisees.size
    return if (autorisees == total) "toutes ($total)" else "$autorisees/$total"
}

/**
 * Le MJ coche les actions de la fiche que le monstre peut réaliser : l'IA ne choisit que parmi
 * celles-ci, et seules elles sont proposées à la déclaration manuelle.
 */
@Composable
private fun ActionsAutoriseesDialog(combattant: Combattant, onDismiss: () -> Unit) {
    var desactivees by remember(combattant.id) { mutableStateOf(combattant.actionsDesactivees) }
    val noms = combattant.attaques.map { it.nom }.toSet()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actions — ${combattant.nom}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "Cochez les actions que ce monstre peut réaliser. L'IA ne choisira que parmi elles.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    TextButton(onClick = { desactivees = emptySet() }) { Text("Tout cocher") }
                    TextButton(onClick = { desactivees = noms }) { Text("Tout décocher") }
                }
                combattant.attaques.forEach { a ->
                    val autorisee = a.nom !in desactivees
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { desactivees = if (autorisee) desactivees + a.nom else desactivees - a.nom }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autorisee,
                            onCheckedChange = { coche -> desactivees = if (coche) desactivees - a.nom else desactivees + a.nom }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.nom, fontWeight = FontWeight.Bold)
                            Text(a.resume, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                CombatSession.definirActionsDesactivees(combattant.id, desactivees)
                onDismiss()
            }) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
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
                // Avantage éventuel (Assassin, Champion, Barbare niv. 7) compris.
                CombatSession.definirInitiative(combattant.id, CombatSession.jetInitiative(combattant))
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
                    Tab(selected = onglet == 2, onClick = { onglet = 2 }, text = { Text("Boss") })
                }
                if (onglet == 2) {
                    val nbPersonnages = combat?.combattants?.count { !it.estMonstre } ?: 0
                    Text(
                        "PV et actions légendaires ajustés à $nbPersonnages personnage(s) (prévu pour 4).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    BossCatalogue.tous.value.filter { recherche.isBlank() || it.nom.contains(recherche.trim(), ignoreCase = true) }.forEach { def ->
                        TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                            scope.launch {
                                CombatSession.ajouterBoss(PreparationBoss.preparer(context, def, worldId, nbPersonnages.coerceAtLeast(1)))
                                onDismiss()
                            }
                        }) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("👑 ${def.nom}", fontWeight = FontWeight.Bold)
                                Text("${def.titre} · ${def.monstreBase} · ${def.phases.size} phases", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    return@Column
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
                                    // Allié comme ennemi : piloté par l'IA selon son comportement (fiche,
                                    // sinon grille par classe), avec les attaques de ses armes.
                                    TextButton(onClick = {
                                        scope.launch {
                                            val attaques = attaquesDuPersonnage(context, perso)
                                            CombatSession.ajouter(listOf(CombatSession.combattantDepuisPersonnage(perso).copy(attaques = attaques)))
                                            onDismiss()
                                        }
                                    }) { Text("Allié") }
                                    TextButton(onClick = {
                                        scope.launch {
                                            val attaques = attaquesDuPersonnage(context, perso)
                                            CombatSession.ajouter(listOf(CombatSession.combattantDepuisPersonnage(perso, ennemi = true).copy(attaques = attaques)))
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
        profil.versCombattant(if (numeroter) "$nom ${existants + i}" else nom, nom)
    })
}

/**
 * Terrain du combat côté MJ : toucher un pion puis une zone l'y place (ajustement de la scène,
 * sans attaque d'opportunité). Repliable, l'état est mémorisé.
 */
@Composable
private fun TerrainMj(etat: CombatEnCours) {
    var choisi by remember { mutableStateOf<String?>(null) }
    val pions = etat.combattants.filter { !it.inerte }.map { c ->
        PionTerrain(c.id, c.nom, c.estMonstre, Terrain.zoneDe(c), c.horsCombat, actif = c.id == etat.actif?.id)
    }
    com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard("combat_mj", "🗺 Terrain") {
        Text(
            if (choisi == null) "Touchez un combattant, puis la zone où le placer. Même zone qu'un adversaire : au contact."
            else "Touchez la zone où placer ${pions.firstOrNull { it.id == choisi }?.nom}.",
            style = MaterialTheme.typography.bodySmall,
            color = if (choisi == null) Color.White.copy(alpha = 0.8f) else ForcedDarkPalette.AccentGold,
        )
        TerrainCombat(
            pions = pions,
            pionChoisi = choisi,
            onPion = { p -> choisi = if (choisi == p.id) null else p.id },
            onZone = { z -> choisi?.let { CombatSession.placer(it, z) }; choisi = null },
        )
    }
}
