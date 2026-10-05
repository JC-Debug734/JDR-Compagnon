package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.widget.Toast
import android.content.Context
import android.graphics.BitmapFactory
import android.view.KeyEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jc2.jdrcompagnon.di.EnvironmentDependencies
import com.jc2.jdrcompagnon.feature_combat.ui.LancerCombatDialog
import com.jc2.jdrcompagnon.di.TableAleatoireDependencies
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironmentImageStore
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_evenement.ui.EventResultDialog
import com.jc2.jdrcompagnon.di.EvenementDependencies
import com.jc2.jdrcompagnon.feature_evenement.ui.EvenementsLiesSection
import com.jc2.jdrcompagnon.feature_table_aleatoire.ui.LootResultDialog
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.SendItemToCharacterDialog
import com.jc2.jdrcompagnon.ui.components.SendItemToGroupDialog
import com.jc2.jdrcompagnon.ui.MusicManager
import com.jc2.jdrcompagnon.ui.LectureScenarioState
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.library.MonsterStatBlock
import com.jc2.jdrcompagnon.ui.screens.mj.library.aUnBlocDeStats
import com.mikepenz.markdown.m3.Markdown
import kotlinx.coroutines.launch

/**
 * Écran plein écran de lecture d'un scénario MJ (route Route.ScenarioReader).
 * Ne fait que poser un Scaffold + bouton retour autour de [ScenarioReaderContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioReaderScreen(
    scenarioId: String,
    onBack: () -> Unit,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    onOpenMenu: () -> Unit = {},
) {
    val mjScenarios by GameState.mjScenarios.collectAsState()
    val scenario = remember(scenarioId, mjScenarios) {
        mjScenarios.firstOrNull { it.id == scenarioId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = scenario?.title ?: "",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        // Heure de scénario qui défile ; un clic la met en pause / la relance.
                        com.jc2.jdrcompagnon.ui.components.ScenarioClockTicker()
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        ScenarioReaderContent(
            scenario = scenario,
            onOpenInternalLink = onOpenInternalLink,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * Contenu de lecture d'un scénario, scène par scène : réutilisable en plein
 * écran (ScenarioReaderScreen) ou intégré directement dans un autre écran
 * (MjHomeScreen) sans navigation — c'est cette dernière utilisation qui
 * justifie l'extraction hors de ScenarioReaderScreen.
 *
 * - Affiche le scénario scène par scène avec animation de transition.
 * - Joue automatiquement la musique associée à la scène via MusicManager.
 * - Navigation via les touches fléchées gauche/droite (clavier / télécommande).
 * - Navigation via les gestes swipe horizontal sur écran tactile.
 * - Rendu des balises couleur, liens internes cliquables, markdown simple.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioReaderContent(
    scenario: GameState.MjScenario?,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val scenes = scenario?.scenes?.ifEmpty {
        listOf(GameState.MjScene(title = scenario.title, markdownContent = scenario.markdownContent))
    } ?: listOf()

    // Scène en cours tenue par GameState (persistée) : on la retrouve au retour d'un autre écran
    // ou au redémarrage, et le menu latéral MJ peut afficher les profils de cette scène.
    var currentIndex by rememberSaveable(scenario?.id) {
        mutableIntStateOf(
            scenario?.id?.let { GameState.currentSceneIndex(it) }?.coerceIn(0, (scenes.size - 1).coerceAtLeast(0)) ?: 0
        )
    }
    val currentScene = scenes.getOrNull(currentIndex)

    // Zones préparées des cartes du scénario (images {image:...} de toutes ses scènes) : une
    // icône œil à côté de leur description les révèle sur la page table.
    val versionExploration by com.jc2.jdrcompagnon.feature_exploration.ExplorationSession.version.collectAsState()
    val cartesScenario = remember(scenario?.id, scenes) {
        val imageRegex = Regex("""\{image:\s*([^}\n]+?)\s*\}""")
        (scenes.flatMap { s -> imageRegex.findAll(s.markdownContent).map { it.groupValues[1] }.toList() } +
            listOfNotNull(scenario?.lieuImageFileName))
            .distinct()
            .map { "scenario-image:$it" to ScenarioImageStore.fichier(context, it) }
    }
    val zonesCarte = remember(cartesScenario, versionExploration) {
        com.jc2.jdrcompagnon.feature_exploration.ExplorationSession.zonesDesCartes(context, cartesScenario)
    }
    fun revelerZone(z: com.jc2.jdrcompagnon.feature_exploration.ExplorationSession.ZoneDeCarte) {
        com.jc2.jdrcompagnon.feature_exploration.ExplorationSession.revelerZoneDeCarte(
            context, z.cle, scenario?.title?.let { "Carte — $it" } ?: "Carte du scénario", z.imageFile, z.zone.id
        )
        Toast.makeText(context, "« ${z.zone.nom} » révélée sur la table", Toast.LENGTH_SHORT).show()
    }

    // Mode lecture (LectureScenarioState) : seul moment où le temps des tables aléatoires est
    // compté. L'ouverture du scénario remet le compteur à zéro ; un retour sur ce même lecteur
    // après un passage par un autre écran (état sauvegardé) reprend simplement le comptage.
    var lectureOuverte by rememberSaveable(scenario?.id) { mutableStateOf(false) }
    DisposableEffect(scenario?.id) {
        val id = scenario?.id
        if (id != null) {
            if (!lectureOuverte || LectureScenarioState.state.value.scenarioId != id) {
                LectureScenarioState.ouvrir(id)
                lectureOuverte = true
            } else {
                LectureScenarioState.reprendre()
            }
        }
        onDispose { if (id != null) LectureScenarioState.suspendre() }
    }

    var direction by remember(scenario?.id) { mutableIntStateOf(1) }
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    var showScenePanel by remember { mutableStateOf(false) }
    var showLinksPanel by remember { mutableStateOf(false) }
    val isMusicPlaying by MusicManager.isPlaying.collectAsState()
    val currentTrackName by MusicManager.currentTrack.collectAsState()

    // Événement de scène (#event:[Nom PNJ]) : briefing plein écran MJ, cf. PnjBriefingOverlay.
    var briefingCharacter by remember { mutableStateOf<Character?>(null) }
    // Tout autre lien (monstre, PNJ, équipement, sort, règle) : un même petit menu d'actions,
    // avec "Voir le détail" toujours présent (petite fenêtre par-dessus le texte, cf.
    // InternalLinkDetailDialog), et "Envoyer à un personnage/groupe" seulement pour l'équipement.
    var linkMenuItem by remember { mutableStateOf<Pair<String, String>?>(null) }
    var linkDetailItem by remember { mutableStateOf<Pair<String, String>?>(null) }
    var lootCharacterPickerItem by remember { mutableStateOf<String?>(null) }
    var lootGroupPickerItem by remember { mutableStateOf<String?>(null) }
    // Butin d'un monstre / possessions d'un PNJ cité dans la scène (type, nom).
    var butinLien by remember { mutableStateOf<Pair<String, String>?>(null) }
    // Trésor écrit dans la scène (#butin:[450 pc; Longue-vue (100 po)]) : à donner aux joueurs.
    var tresorLien by remember { mutableStateOf<String?>(null) }
    // « Ajouter au groupe » d'un lien (type, nom) : voir AjouterAuGroupeDialog.
    var ajoutGroupeItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Résultat d'un événement tiré (table de la scène) ou lancé par un lien #evenement:.
    var resultatEvenement by remember { mutableStateOf<Evenement?>(null) }
    var afficherResultatEvenement by remember { mutableStateOf(false) }

    // Lien #epreuve:[Nom] : épreuve de l'outil ÉPREUVES démarrée puis écran de résolution.
    val epreuveScope = rememberCoroutineScope()

    // Lien #combat:[Gobelin x3, Loup x2] : dialogue de préparation puis écran de suivi du combat.
    var combatALancer by remember { mutableStateOf<String?>(null) }

    fun handleLinkClick(type: String, name: String) {
        when (type) {
            "event" -> {
                briefingCharacter = GameState.characters.value.firstOrNull {
                    it.type == "PNJ" && it.name.equals(name, ignoreCase = true)
                }
            }
            "epreuve" -> epreuveScope.launch {
                val epreuve = com.jc2.jdrcompagnon.di.EpreuveDependencies.trouverEpreuve(scenario?.worldId ?: GameState.currentWorldId(), name)
                if (epreuve != null) {
                    // Une autre épreuve en cours est remplacée ; la même reprend où elle en était.
                    val active = com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession.etat.value
                    if (active?.epreuve?.id != epreuve.id) {
                        com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveOutilSession.demarrer(epreuve)
                    }
                    onOpenInternalLink("epreuve", epreuve.nom)
                } else {
                    Toast.makeText(context, "Épreuve \"$name\" introuvable dans l'outil ÉPREUVES", Toast.LENGTH_SHORT).show()
                }
            }
            // Lien #evenement:[Titre] : événement de la bibliothèque, affiché avec ses effets.
            "evenement" -> epreuveScope.launch {
                val monde = scenario?.worldId?.takeIf { it.isNotBlank() } ?: GameState.currentWorldId() ?: "donjon_et_dragon"
                val evenement = EvenementDependencies.trouverEvenement(monde, name, GameState.currentCampaignId.value)
                if (evenement != null) {
                    resultatEvenement = evenement
                    afficherResultatEvenement = true
                } else {
                    Toast.makeText(context, "Événement \"$name\" introuvable dans la bibliothèque", Toast.LENGTH_SHORT).show()
                }
            }
            "combat" -> combatALancer = name
            "butin" -> tresorLien = name
            // Règle : aucune autre action possible, on ouvre directement sa fiche (petite
            // fenêtre de détail) sans passer par le menu "Que faire ?".
            "rule" -> linkDetailItem = type to name
            else -> linkMenuItem = type to name
        }
    }

    // Environnement rattaché à la scène courante (feature_environnement), utilisé pour le
    // bandeau d'ambiance et comme source de la musique de la scène (voir currentTrackId
    // ci-dessous — le choix manuel de musique par scène a été retiré de l'éditeur, la musique
    // est désormais entièrement pilotée par l'environnement).
    var currentEnvironment by remember { mutableStateOf<Environnement?>(null) }
    LaunchedEffect(currentScene?.environmentId) {
        currentEnvironment = currentScene?.environmentId?.let { id ->
            EnvironmentDependencies.repository.getEnvironnementParId(id)
        }
    }

    // Page d'affichage table : nom du scénario et image de la scène en cours (image de son
    // environnement, sinon celle du lieu du scénario).
    LaunchedEffect(scenario?.id, scenario?.title, currentScene?.title, currentEnvironment?.imageFileName) {
        val sc = scenario ?: return@LaunchedEffect
        val image = currentEnvironment?.imageFileName
            ?.let { com.jc2.jdrcompagnon.feature_environnement.data.EnvironmentImageStore.fichier(context, it) }
            ?.takeIf { it.exists() }
            ?: sc.lieuImageFileName?.let { ScenarioImageStore.fichier(context, it) }?.takeIf { it.exists() }
        NetworkSessionManager.updateTableScene(
            com.jc2.jdrcompagnon.network.MjWebServer.SceneInfo(sc.title, currentScene?.title, image)
        )
    }

    // Piste effective de la scène courante : celle propre à la scène (ancien contenu importé
    // depuis un .md) sinon celle de son environnement.
    val currentTrackId = currentScene?.musicTrackId ?: currentEnvironment?.musicTrackId

    // Tables aléatoires (feature_table_aleatoire) exploitables depuis la lecture : la table
    // d'événements de la scène, avec repli sur celle du scénario entier si absente (voir
    // MjScenario.tableEvenementsId), et la table de loot propre à la scène. Entre les deux, les
    // tables liées à l'environnement de la scène (Environnement.tablesAleatoiresIds) : la
    // première de chaque type sert de repli quand la scène n'en désigne aucune.
    var tableEvenements by remember { mutableStateOf<TableAleatoire?>(null) }
    var tableLoot by remember { mutableStateOf<TableAleatoire?>(null) }
    LaunchedEffect(currentScene?.tableAleatoireId, currentScene?.tableLootId, currentEnvironment, scenario?.tableEvenementsId) {
        val repository = TableAleatoireDependencies.repository
        val tablesEnvironnement = currentEnvironment?.tablesAleatoiresIds.orEmpty().mapNotNull { repository.getTable(it) }
        tableEvenements = currentScene?.tableAleatoireId?.let { repository.getTable(it) }
            ?: tablesEnvironnement.firstOrNull { it.type == TypeTable.EVENEMENTS }
            ?: scenario?.tableEvenementsId?.let { repository.getTable(it) }
        tableLoot = currentScene?.tableLootId?.let { repository.getTable(it) }
            ?: tablesEnvironnement.firstOrNull { it.type == TypeTable.LOOT }
    }
    // Boutique associée à la scène (services et tarifs consultables depuis la barre d'outils).
    val boutiques by remember {
        com.jc2.jdrcompagnon.di.BoutiqueDependencies.repository.observerToutesLesBoutiques()
    }.collectAsState(initial = emptyList())
    val boutiqueScene = currentScene?.boutiqueId?.let { id -> boutiques.find { it.id == id } }
    var afficherBoutique by remember { mutableStateOf(false) }
    var afficherEvenementsScene by remember { mutableStateOf(false) }
    var resultatLoot by remember { mutableStateOf<Pair<String, List<ResultatLoot>>?>(null) }
    val tirageScope = rememberCoroutineScope()

    fun tirerEvenement() {
        val table = tableEvenements ?: return
        val (tableMiseAJour, resultat) = TableAleatoireDependencies.tirage.tirerEvenement(
            table, null, LectureScenarioState.minutesEcouleesMaintenant()
        )
        tableEvenements = tableMiseAJour
        if (resultat != null) tirageScope.launch { TableAleatoireDependencies.repository.sauvegarder(tableMiseAJour) }
        resultatEvenement = resultat
        afficherResultatEvenement = true
    }

    fun tirerLoot() {
        val table = tableLoot ?: return
        val (tableMiseAJour, resultats) = TableAleatoireDependencies.tirage.tirerLoot(
            table, 3, LectureScenarioState.minutesEcouleesMaintenant()
        )
        tableLoot = tableMiseAJour
        if (resultats.isNotEmpty()) tirageScope.launch { TableAleatoireDependencies.repository.sauvegarder(tableMiseAJour) }
        resultatLoot = "Loot — ${table.nom}" to resultats
    }

    // Coupure manuelle de la musique par le MJ (bouton son) : tenue par MusicManager et non par
    // un remember local, qui était perdu à chaque sortie/retour sur la lecture (combat, lien...)
    // ou remis à zéro au changement de scène — la musique se relançait alors toute seule. Elle
    // reste coupée jusqu'à ce que le MJ la relance lui-même.
    val musicManuallyStopped by MusicManager.mutedByUser.collectAsState()

    LaunchedEffect(currentTrackId, musicManuallyStopped) {
        if (musicManuallyStopped) return@LaunchedEffect
        currentTrackId?.let { playSceneMusic(context, it) }
    }

    fun goToScene(newIndex: Int) {
        if (newIndex in scenes.indices && newIndex != currentIndex) {
            direction = if (newIndex > currentIndex) 1 else -1
            currentIndex = newIndex
            scenario?.id?.let { GameState.setCurrentSceneIndex(it, newIndex) }
            // Avertit les joueurs dont la décision manque encore sur une proposition de groupe en
            // cours : dernier appel avant perte de la récompense (cf. PlayerNetworkOverlay).
            NetworkSessionManager.warnPendingProposalsOnSceneChange()
        }
    }

    // Retour système : scène précédente ; sur la première scène, on laisse passer le retour à
    // l'appelant (accueil MJ ou pile de navigation).
    androidx.activity.compose.BackHandler(enabled = currentIndex > 0) {
        goToScene(currentIndex - 1)
    }

    if (scenario == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Scénario introuvable", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(modifier = modifier) {
        // En-tête : titre du scénario, scène courante, action musique. Fond plein (comme le
        // contenu) : sans ça, l'en-tête se retrouvait directement sur le fond d'écran global de
        // l'app, illisible.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Le titre du scénario est déjà affiché par l'écran englobant (barre du haut de
                // ScenarioReaderScreen, ou de MjHomeScreen en lecture intégrée) : pas de doublon ici.
                currentScene?.title?.let {
                    Text(
                        text = "Scène ${currentIndex + 1}/${scenes.size} — $it",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    // Environnement de la scène, toujours indiqué sous le titre.
                    SceneEnvironmentLine(currentEnvironment)
                }
            }
            // Liste des scènes, profils/liens et validation : déplacés dans la barre d'outils
            // de la scène ci-dessous, avec les autres icônes.
        }

        // Barre d'outils de la scène : toutes les icônes côte à côte (environnement, tables
        // aléatoires) plutôt qu'un bandeau par élément, et le son toujours visible à droite.
        // Appui long sur une icône : son nom ; son : appui = musique on/off, double appui =
        // réglage du volume (musique + effets météo).
        val currentTrackDisplayName = currentTrackId?.let { id ->
            availableLoopTracks.firstOrNull { it.id.equals(id, ignoreCase = true) }?.displayName ?: id
        }
        val sceneMusicPlaying = currentTrackId != null && isMusicPlaying &&
            currentTrackName == currentTrackDisplayName && !musicManuallyStopped
        SceneToolsBar(
            tableEvenementsNom = tableEvenements?.nom,
            tableLootNom = tableLoot?.nom,
            onTirerEvenement = ::tirerEvenement,
            onTirerLoot = ::tirerLoot,
            boutiqueNom = boutiqueScene?.nom,
            onOpenBoutique = { afficherBoutique = true },
            nombreEvenements = currentScene?.evenementIds.orEmpty().size + currentEnvironment?.evenementIds.orEmpty().size,
            onOpenEvenements = { afficherEvenementsScene = true },
            musicPlaying = sceneMusicPlaying,
            musicTrackName = currentTrackDisplayName,
            onOpenScenes = if (scenes.size > 1) ({ showScenePanel = true }) else null,
            onOpenLinks = { showLinksPanel = true },
            sceneValidated = currentScene?.validated,
            onToggleValidated = {
                currentScene?.let { scene -> GameState.setSceneValidated(scenario.id, scene.id, !scene.validated, context) }
            },
            onToggleMusic = {
                val trackId = currentTrackId
                when {
                    trackId == null -> Toast.makeText(context, "Aucune musique pour cette scène (double appui : volume)", Toast.LENGTH_SHORT).show()
                    sceneMusicPlaying -> MusicManager.stopByUser()
                    else -> playSceneMusic(context, trackId)
                }
            },
        )

        val activeLootOffers by NetworkSessionManager.activeLootOffers.collectAsState()
        activeLootOffers.forEach { (offerId, offer) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Offre en cours : ${offer.itemName} (${offer.respondedClientIds.size}/${offer.pendingClientIds.size} réponses)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { NetworkSessionManager.closeLootOffer(offerId) }) {
                    Text("Fermer", color = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.isFocused }
                .onKeyEvent { keyEvent ->
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT -> {
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                goToScene(currentIndex - 1)
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT -> {
                            if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                goToScene(currentIndex + 1)
                            }
                            true
                        }
                        else -> false
                    }
                }
                .pointerInput(scenes.size, currentIndex) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        val threshold = 80f
                        when {
                            dragAmount < -threshold -> goToScene(currentIndex + 1)
                            dragAmount > threshold -> goToScene(currentIndex - 1)
                        }
                    }
                }
                // Fond opaque à 75% (et non transparent), même couleur que l'en-tête et le
                // pied de page : le fond d'écran global de l'app passait à travers et rendait
                // le texte du scénario illisible. Texte du contenu en blanc (cf.
                // PlainScenarioRenderer) pour rester lisible sur ce fond.
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
        ) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            if (currentScene == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune scène à afficher", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                AnimatedContent(
                    targetState = currentIndex,
                    transitionSpec = {
                        val enter = slideInHorizontally(initialOffsetX = { fullWidth -> direction * fullWidth })
                        val exit = slideOutHorizontally(targetOffsetX = { fullWidth -> -direction * fullWidth })
                        enter togetherWith exit
                    },
                    label = "scene_transition"
                ) { index ->
                    val scene = scenes[index]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (index == 0 && (scenario.lieuNom.isNotBlank() || scenario.lieuImageFileName != null)) {
                            LieuBanner(scenario = scenario, context = context)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        // Introduction MJ du scénario (aperçu du lieu, niveau, objectifs...), écrite
                        // avant la première scène du fichier : en tête de la première page, ses
                        // titres repliables comme ceux d'une scène.
                        if (index == 0 && scenario.description.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                contentColor = Color.White,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                PlainScenarioRenderer(
                                    content = scenario.description,
                                    onLinkClick = ::handleLinkClick,
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    collapseKey = "${scenario.id}_apercu",
                                    zonesCarte = zonesCarte,
                                    onRevelerZone = ::revelerZone,
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        // Les liens de cette scène (monstre, PNJ, équipement...) ne sont pas
                        // réaffichés ici en dessous du texte : ils restent accessibles cliquables
                        // directement dans le texte (PlainScenarioRenderer), et la liste complète
                        // du scénario reste disponible via le bouton en haut ("Profils et liens").
                        PlainScenarioRenderer(
                            content = scene.markdownContent,
                            onLinkClick = ::handleLinkClick,
                            modifier = Modifier.fillMaxWidth(),
                            collapseKey = "${scenario.id}_${scene.id}",
                            zonesCarte = zonesCarte,
                            onRevelerZone = ::revelerZone,
                        )
                    }
                }
            }
        }

        if (scenes.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { goToScene(currentIndex - 1) },
                    enabled = currentIndex > 0,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Scène précédente")
                }
                Text("${currentIndex + 1} / ${scenes.size}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                IconButton(
                    onClick = { goToScene(currentIndex + 1) },
                    enabled = currentIndex < scenes.size - 1,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.38f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Scène suivante")
                }
            }
        }
    }

    if (showScenePanel) {
        ModalBottomSheet(onDismissRequest = { showScenePanel = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Scènes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                scenes.forEachIndexed { index, scene ->
                    ListItem(
                        headlineContent = { Text(scene.title) },
                        leadingContent = if (index == currentIndex) {
                            @Composable { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        trailingContent = {
                            IconButton(onClick = {
                                GameState.setSceneValidated(scenario.id, scene.id, !scene.validated, context)
                            }) {
                                Icon(
                                    imageVector = if (scene.validated) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                    contentDescription = if (scene.validated) "Scène validée — retirer la validation" else "Marquer comme validée",
                                    tint = if (scene.validated) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            goToScene(index)
                            showScenePanel = false
                        }
                    )
                }
            }
        }
    }

    if (showLinksPanel) {
        val allLinks = remember(scenario.id, scenes) {
            scenes.flatMap { extractInternalLinks(it.markdownContent) }.distinct()
        }
        ModalBottomSheet(onDismissRequest = { showLinksPanel = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Profils et liens du scénario", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (allLinks.isEmpty()) {
                    Text(
                        "Aucun lien (monstre, PNJ, équipement...) dans ce scénario pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    allLinks.forEach { (type, name) ->
                        ListItem(
                            headlineContent = { Text(name) },
                            supportingContent = {
                                Text(
                                    when (type) {
                                        "monster" -> "Monstre"
                                        "pnj", "npc" -> "PNJ"
                                        "equipment" -> "Équipement"
                                        "spell" -> "Sort"
                                        "rule" -> "Règle"
                                        "event" -> "Discussion avec un PNJ"
                                        "epreuve" -> "Épreuve environnementale"
                                        "evenement" -> "Événement"
                                        "combat" -> "Combat"
                                        "butin" -> "Trésor à donner"
                                        else -> type
                                    }
                                )
                            },
                            leadingContent = {
                                Text(
                                    when (type) {
                                        "monster" -> "🐲"
                                        "pnj", "npc" -> "👤"
                                        "equipment" -> "⚔️"
                                        "spell" -> "✨"
                                        "rule" -> "📜"
                                        "event" -> "💬"
                                        "epreuve" -> "⛰️"
                                        "evenement" -> "🎲"
                                        "combat" -> "⚔️"
                                        "butin" -> "💰"
                                        else -> "🔗"
                                    },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            modifier = Modifier.clickable {
                                showLinksPanel = false
                                handleLinkClick(type, name)
                            }
                        )
                    }
                }
            }
        }
    }

    // Événements de la bibliothèque liés à la scène et à son environnement : consultation et
    // tirage (tap = résultat avec ses effets), l'édition se fait dans l'éditeur de scénario.
    if (afficherEvenementsScene) {
        val mondeEvenements = scenario?.worldId?.takeIf { it.isNotBlank() } ?: GameState.currentWorldId() ?: "donjon_et_dragon"
        AlertDialog(
            onDismissRequest = { afficherEvenementsScene = false },
            title = { Text("Événements") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    currentScene?.takeIf { it.evenementIds.isNotEmpty() }?.let { scene ->
                        EvenementsLiesSection(
                            worldId = mondeEvenements,
                            campagneId = null,
                            evenementIds = scene.evenementIds,
                            onChanger = {},
                            readOnly = true,
                            titre = "Scène"
                        )
                    }
                    currentEnvironment?.takeIf { it.evenementIds.isNotEmpty() }?.let { env ->
                        EvenementsLiesSection(
                            worldId = mondeEvenements,
                            campagneId = null,
                            evenementIds = env.evenementIds,
                            onChanger = {},
                            readOnly = true,
                            titre = env.nom
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { afficherEvenementsScene = false }) { Text("Fermer") } }
        )
    }

    if (afficherResultatEvenement) {
        EventResultDialog(
            evenement = resultatEvenement,
            worldId = scenario?.worldId,
            onDismiss = { afficherResultatEvenement = false }
        )
    }

    if (afficherBoutique) {
        boutiqueScene?.let { boutique ->
            com.jc2.jdrcompagnon.feature_boutique.ui.BoutiqueApercuDialog(boutique = boutique, onDismiss = { afficherBoutique = false })
        }
    }

    resultatLoot?.let { (titreDialog, resultats) ->
        LootResultDialog(
            titre = titreDialog,
            resultats = resultats,
            worldId = scenario?.worldId,
            onDismiss = { resultatLoot = null }
        )
    }

    briefingCharacter?.let { character ->
        // Fiche de discussion de la scène (null = tout est libre) et réputation du PNJ envers le
        // groupe sélectionné, qui sert à suggérer l'attitude si le scénario ne la fixe pas.
        val discussion = currentScene?.discussions?.firstOrNull { it.pnjName.equals(character.name, ignoreCase = true) }
        val groupeId = GameState.currentGroupId.collectAsState().value
        PnjBriefingOverlay(
            character = character,
            discussion = discussion,
            reputationGroupe = groupeId?.let { character.groupReputations[it] },
            onDismiss = { briefingCharacter = null }
        )
    }


    combatALancer?.let { composition ->
        LancerCombatDialog(
            composition = composition,
            worldId = scenario.worldId,
            onDismiss = { combatALancer = null },
            onLance = {
                combatALancer = null
                onOpenInternalLink("combat", composition)
            }
        )
    }

    linkMenuItem?.let { (type, name) ->
        val typeLabel = when (type) {
            "monster" -> "Monstre"
            "pnj", "npc" -> "PNJ"
            "equipment" -> "Équipement"
            "spell" -> "Sort"
            "rule" -> "Règle"
            else -> null
        }
        AlertDialog(
            onDismissRequest = { linkMenuItem = null },
            title = { Text(name, fontWeight = FontWeight.Bold) },
            text = { Text(typeLabel?.let { "$it — que faire ?" } ?: "Que faire ?") },
            confirmButton = {},
            dismissButton = {
                Column(horizontalAlignment = Alignment.End) {
                    if (type == "pnj" || type == "npc") {
                        // Conversation improvisée : briefing plein écran (comportement, personnalité,
                        // aide de jeu sociale), même si le PNJ n'a pas de fiche.
                        TextButton(onClick = {
                            linkMenuItem = null
                            briefingCharacter = pnjPourConversation(name, scenario.worldId)
                        }) { Text("💬 Lancer une conversation") }
                    }
                    if (type in setOf("monster", "pnj", "npc")) {
                        TextButton(onClick = {
                            linkMenuItem = null
                            butinLien = type to name
                        }) { Text(if (type == "monster") "💰 Butin" else "💰 Possessions / échange") }
                    }
                    TextButton(onClick = {
                        linkMenuItem = null
                        if (type == "pnj" || type == "npc") {
                            // La fiche de personnage a besoin de tout l'écran, pas d'une petite
                            // fenêtre : seule exception à l'affichage compact ci-dessous.
                            onOpenInternalLink(type, name)
                        } else {
                            linkDetailItem = type to name
                        }
                    }) { Text("Voir le détail") }
                    // Objet : proposé au groupe (écran de réclamation des joueurs) ; PNJ, monstre,
                    // animal : profil supplémentaire du groupe (voir AjoutAuGroupe).
                    if (type in setOf("equipment", "monster", "pnj", "npc")) {
                        TextButton(onClick = {
                            linkMenuItem = null
                            ajoutGroupeItem = type to name
                        }) { Text("Ajouter au groupe") }
                    }
                    if (type == "equipment") {
                        TextButton(onClick = {
                            linkMenuItem = null
                            lootCharacterPickerItem = name
                        }) { Text("Envoyer à un personnage") }
                        TextButton(onClick = {
                            linkMenuItem = null
                            lootGroupPickerItem = name
                        }) { Text("Envoyer à un groupe") }
                    }
                }
            }
        )
    }

    linkDetailItem?.let { (type, name) ->
        InternalLinkDetailDialog(
            type = type,
            name = name,
            worldId = scenario?.worldId,
            onDismiss = { linkDetailItem = null }
        )
    }

    lootCharacterPickerItem?.let { itemName ->
        SendItemToCharacterDialog(itemName = itemName, worldId = scenario.worldId, onDismiss = { lootCharacterPickerItem = null })
    }

    lootGroupPickerItem?.let { itemName ->
        SendItemToGroupDialog(itemName = itemName, worldId = scenario.worldId, onDismiss = { lootGroupPickerItem = null })
    }

    butinLien?.let { (type, name) ->
        if (type == "monster") {
            com.jc2.jdrcompagnon.feature_butin.ui.ButinMonstreDialog(
                nom = name,
                cle = "scenario:${scenario.id}:${currentScene?.title}:$name",
                worldId = scenario.worldId,
                onDismiss = { butinLien = null },
            )
        } else {
            // Mémorisé : un PNJ sans fiche est recréé à chaque appel (nouvel id, nouveau butin).
            val pnj = remember(name) { pnjPourConversation(name, scenario.worldId) }
            com.jc2.jdrcompagnon.feature_butin.ui.ButinPersonnageDialog(
                character = pnj,
                onDismiss = { butinLien = null },
            )
        }
    }

    tresorLien?.let { contenu ->
        com.jc2.jdrcompagnon.feature_butin.ui.ButinScenarioDialog(
            contenu = contenu,
            cle = "tresor:${scenario.id}:${currentScene?.id}:$contenu",
            worldId = scenario.worldId,
            onDismiss = { tresorLien = null },
        )
    }

    ajoutGroupeItem?.let { (type, name) ->
        AjouterAuGroupeDialog(type = type, nom = name, worldId = scenario.worldId, onDismiss = { ajoutGroupeItem = null })
    }
}

/**
 * Bandeau affiché en tête de la première page du lecteur : image et nom du lieu associé au
 * scénario (configurés dans ScenarioEditorScreen), pour planter le décor avant la première scène.
 */
@Composable
private fun LieuBanner(scenario: GameState.MjScenario, context: Context) {
    val bitmap = scenario.lieuImageFileName?.let { fileName ->
        remember(fileName) {
            runCatching {
                val file = ScenarioImageStore.fichier(context, fileName)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() else null
            }.getOrNull()
        }
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = scenario.lieuNom.ifBlank { "Lieu du scénario" },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (scenario.lieuNom.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomStart)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = scenario.lieuNom,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else if (scenario.lieuNom.isNotBlank()) {
            Text(
                text = scenario.lieuNom,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun playSceneMusic(context: Context, trackId: String) {
    val track = availableLoopTracks.firstOrNull { it.id.equals(trackId, ignoreCase = true) }
    if (track != null) {
        MusicManager.play(context, track)
    }
}

/**
 * "Voir le détail" d'un lien (monstre, équipement, sort, règle) : une petite fenêtre par-dessus
 * le texte du scénario plutôt qu'une navigation plein écran, pour ne pas faire perdre au MJ sa
 * place dans la lecture. Le PNJ (type pnj/npc) n'utilise pas cette fenêtre : sa fiche complète a
 * besoin de tout l'écran (voir l'appelant, ScenarioReaderContent.linkMenuItem).
 */
@Composable
internal fun InternalLinkDetailDialog(
    type: String,
    name: String,
    worldId: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var loading by remember(type, name) { mutableStateOf(true) }
    var content by remember(type, name) { mutableStateOf<String?>(null) }
    // Monstre : fiche gardée pour afficher son image (MonsterImage) au-dessus du texte.
    var monster by remember(type, name) { mutableStateOf<com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry?>(null) }

    LaunchedEffect(type, name, worldId) {
        loading = true
        content = when (type) {
            "monster" -> SrdRepository.getMonsterByName(context, name, worldId)?.also { monster = it }?.rawMarkdown
            "spell" -> SrdRepository.getSpellByName(context, name, worldId)?.rawMarkdown
            "equipment" -> SrdRepository.getEquipmentByName(context, name, worldId)?.rawMarkdown
            "rule" -> SrdRepository.getRuleEntryByName(context, name, worldId)?.rawMarkdown
            else -> null
        }
        loading = false
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when {
                        loading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                        content.isNullOrBlank() -> Text(
                            "Détail introuvable pour « $name ».",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> Column {
                            monster?.let {
                                com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterImage(it, modifier = Modifier.padding(bottom = 8.dp))
                            }
                            val fiche = monster
                            if (fiche != null && fiche.aUnBlocDeStats()) {
                                MonsterStatBlock(fiche)
                            } else {
                                Markdown(content = content!!)
                            }
                        }
                    }
                }
            }
        }
    }
}
