package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.rememberImageCampagne
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioReaderContent

data class MjTool(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

// Couleur unique appliquée à toutes les icônes d'outils du tableau de bord ("icônes
// classiques, toutes de la même couleur") au lieu d'une couleur par outil.
private val ToolIconColor = Color.White

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MjHomeScreen(
    currentWorld: WorldState?,
    onCreateCharacter: () -> Unit,
    onViewCharacters: () -> Unit,
    onOpenScenarioEditor: (String?) -> Unit,
    onOpenScenarios: () -> Unit = {},
    onOpenCampaigns: () -> Unit,
    onOpenCampaign: (String) -> Unit = {},
    onOpenGroups: () -> Unit,
    onOpenBoutiques: () -> Unit,
    onOpenEnvironnements: () -> Unit = {},
    onOpenTableAleatoire: () -> Unit = {},
    onOpenEpreuves: () -> Unit = {},
    onOpenEvenements: () -> Unit = {},
    onOpenCombatActions: () -> Unit = {},
    onOpenMusic: () -> Unit,
    onOpenClock: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {

    // "Bibliothèque" et "Connexion/Partie" ne sont pas repris ici : déjà accessibles
    // depuis la barre du bas globale (AppBottomBar), un outil dédié ici ferait doublon.
    val tools = listOf(
        MjTool("characters", "FICHES", Icons.Default.People),
        MjTool("scenarios", "SCÉNARIOS", Icons.AutoMirrored.Filled.MenuBook),
        MjTool("campaigns", "CAMPAGNES", Icons.Default.Checklist),
        MjTool("groups", "GROUPES", Icons.Default.Group),
        MjTool("boutiques", "BOUTIQUES", Icons.Default.Storefront),
        MjTool("environnements", "ENVIRONNEMENTS", Icons.Default.Terrain),
        MjTool("table_aleatoire", "TABLE ALÉATOIRE", Icons.Default.Casino),
        MjTool("epreuves", "ÉPREUVES", Icons.Default.Hiking),
        MjTool("evenements", "ÉVÉNEMENTS", Icons.Default.AutoStories),
        MjTool("combat_actions", "ACTIONS DE COMBAT", Icons.Default.Shield),
        MjTool("music", "MUSIQUE", Icons.Default.MusicNote),
        MjTool("time", "HORLOGE", Icons.Default.Schedule),
        MjTool("import", "IMPORT", Icons.Default.UploadFile),
    )

    // rememberSaveable : restauré au retour d'un autre écran (lien, combat, fiche...) — avec un
    // simple remember, revenir en arrière ramenait sur l'accueil au lieu du scénario en lecture.
    var showReader by rememberSaveable { mutableStateOf(false) }

    // Retour système sur la première scène : revient au tableau de bord au lieu de quitter l'app
    // (le lecteur, composé après, intercepte lui-même le retour tant qu'il y a une scène précédente).
    androidx.activity.compose.BackHandler(enabled = showReader) { showReader = false }

    // Limités au monde courant : sans ce filtre, le scénario sélectionné dans un autre
    // univers restait ouvrable depuis le tableau de bord après un changement de monde.
    val allScenarios by GameState.mjScenarios.collectAsState()
    val allCampaigns by GameState.mjCampaigns.collectAsState()
    val mjScenarios = allScenarios.filter { it.worldId == (currentWorld?.id ?: "") }
    val mjCampaigns = allCampaigns.filter { it.worldId == (currentWorld?.id ?: "") }
    // Sélection de campagne/scénario : état global (persisté par monde), pas
    // local à cet écran — pour que le tiroir MJ, partagé par tous les écrans
    // (voir MjDrawer), retrouve toujours la même sélection quel que soit
    // l'écran d'où on l'ouvre.
    val selectedCampaignId by GameState.currentCampaignId.collectAsState()
    val lastScenarioId by GameState.lastScenarioId.collectAsState()

    val selectedCampaign = selectedCampaignId?.let { id -> mjCampaigns.firstOrNull { it.id == id } }
    val visibleScenarios = if (selectedCampaign != null) {
        mjScenarios.filter { it.id in selectedCampaign.scenarioIds }
    } else {
        mjScenarios.filter { com.jc2.jdrcompagnon.ui.PorteeCampagne.scenarioVisible(it) }
    }

    LaunchedEffect(mjCampaigns, selectedCampaignId) {
        // La campagne sélectionnée a été supprimée entre-temps : retombe sur "Toutes".
        if (selectedCampaignId != null && mjCampaigns.none { it.id == selectedCampaignId }) {
            GameState.setCurrentCampaignId(null)
        }
    }

    LaunchedEffect(visibleScenarios, lastScenarioId) {
        if (lastScenarioId == null || visibleScenarios.none { it.id == lastScenarioId }) {
            GameState.setLastScenarioId(visibleScenarios.firstOrNull()?.id)
        }
    }

    val selectedScenario = lastScenarioId?.let { id -> mjScenarios.firstOrNull { it.id == id } }

    LaunchedEffect(lastScenarioId) {
        if (lastScenarioId == null) showReader = false
    }

    val homeResetRequested by GameState.homeResetRequested.collectAsState()
    LaunchedEffect(homeResetRequested) {
        if (homeResetRequested) {
            showReader = false
            GameState.consumeHomeResetRequest()
        }
    }

    fun handleToolClick(toolId: String) {
        when (toolId) {
            "characters" -> onViewCharacters()
            "scenarios" -> onOpenScenarios()
            "campaigns" -> onOpenCampaigns()
            "groups" -> onOpenGroups()
            "boutiques" -> onOpenBoutiques()
            "environnements" -> onOpenEnvironnements()
            "table_aleatoire" -> onOpenTableAleatoire()
            "epreuves" -> onOpenEpreuves()
            "evenements" -> onOpenEvenements()
            "combat_actions" -> onOpenCombatActions()
            "music" -> onOpenMusic()
            "time" -> onOpenClock()
            "import" -> onOpenImport()
        }
    }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (showReader && selectedScenario != null) selectedScenario.title else "MAÎTRE DU JEU",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                            // Heure de scénario qui défile sous le titre du scénario en lecture ;
                            // un clic la met en pause / la relance.
                            if (showReader && selectedScenario != null) {
                                com.jc2.jdrcompagnon.ui.components.ScenarioClockTicker()
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Ouvrir le menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            if (showReader && selectedScenario != null) {
                ScenarioReaderContent(
                    scenario = selectedScenario,
                    onOpenInternalLink = onOpenInternalLink,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- Carte "campagne en cours" : remplace la carte scénario quand une campagne
                    // est sélectionnée ; ouvre sa page (carte de la zone, scénarios, fiche de suivi).
                    if (selectedCampaign != null) {
                        CampagneEnCoursCard(
                            campaign = selectedCampaign,
                            worldName = currentWorld?.name,
                            scenarioEnCours = selectedScenario?.takeIf { it.id in selectedCampaign.scenarioIds },
                            onOpenCampaign = { onOpenCampaign(selectedCampaign.id) },
                            onResumeScenario = { showReader = true },
                        )
                    } else if (selectedScenario != null) {
                    // --- Carte "scénario en cours" (équivalent réel de "prochaine partie") ---
                    // Carte classique Material, translucide (75% d'opacité), sans image de fond.
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        ) {
                            Column(modifier = Modifier.padding(28.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SCÉNARIO EN COURS",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = selectedScenario.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Casino, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentWorld?.name ?: "Monde inconnu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                    if (selectedScenario.scenes.size > 1) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${selectedScenario.scenes.size} scènes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showReader = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("Reprendre la lecture")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        ) {
                            Column(modifier = Modifier.padding(28.dp)) {
                                Text(
                                    text = "Aucun scénario sélectionné",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sélectionnez une campagne ou un scénario depuis le menu.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MES OUTILS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    tools.chunked(2).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowTools.forEach { tool ->
                                DashboardToolCard(
                                    tool = tool,
                                    modifier = Modifier.weight(1f),
                                    onClick = { handleToolClick(tool.id) }
                                )
                            }
                            if (rowTools.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
}

/**
 * Carte de l'accueil MJ pour la campagne sélectionnée : son image occupe tout le fond de la carte
 * (assombrie pour la lisibilité) et les infos s'affichent par-dessus, en passant à la ligne quand
 * elles ne tiennent pas sur une seule.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CampagneEnCoursCard(
    campaign: GameState.MjCampaign,
    worldName: String?,
    scenarioEnCours: GameState.MjScenario?,
    onOpenCampaign: () -> Unit,
    onResumeScenario: () -> Unit,
) {
    val image = rememberImageCampagne(campaign)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        onClick = onOpenCampaign,
    ) {
        Box {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = "Image de la campagne",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                // Voile sombre dégradé : texte blanc lisible quelle que soit l'image.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )
            }
            Column(modifier = Modifier.padding(28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CAMPAGNE EN COURS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = campaign.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Infos en « puces » qui passent à la ligne au lieu de s'écraser.
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val nb = campaign.scenarioIds.size
                    CampagneInfo(Icons.Default.Casino, worldName ?: "Monde inconnu")
                    CampagneInfo(Icons.AutoMirrored.Filled.MenuBook, "$nb scénario${if (nb > 1) "s" else ""}")
                    if (campaign.quests.isNotEmpty()) {
                        val enCours = campaign.quests.count { it.status == com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.EN_COURS }
                        CampagneInfo(Icons.Default.Flag, "$enCours quête${if (enCours > 1) "s" else ""} en cours")
                    }
                    if (campaign.pnjIds.isNotEmpty()) {
                        CampagneInfo(Icons.Default.Person, "${campaign.pnjIds.size} PNJ")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenCampaign,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Ouvrir la campagne")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                // Raccourci vers le scénario en lecture, pour ne pas perdre "Reprendre la lecture".
                if (scenarioEnCours != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onResumeScenario,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            "Reprendre : ${com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios.titreNumerote(scenarioEnCours)}",
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** Une info de la carte de campagne : icône + texte (qui peut lui-même passer à la ligne). */
@Composable
private fun CampagneInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, texte: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(texte, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

@Composable
private fun DashboardToolCard(
    tool: MjTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Carte classique Material, translucide (75% d'opacité), sans image de fond : nom et
    // icône uniquement (l'icône garde une couleur unique pour tous les outils, cf. ToolIconColor).
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(tool.icon, contentDescription = null, tint = ToolIconColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tool.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}
