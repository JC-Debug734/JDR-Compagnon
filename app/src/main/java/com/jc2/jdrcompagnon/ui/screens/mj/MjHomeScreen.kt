package com.jc2.jdrcompagnon.ui.screens.mj

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioReaderContent

data class MjTool(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MjHomeScreen(
    currentWorld: WorldState?,
    onCreateCharacter: () -> Unit,
    onViewCharacters: () -> Unit,
    onOpenScenarioEditor: (String?) -> Unit,
    onOpenCampaigns: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenBoutiques: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenInternalLink: (type: String, name: String) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {

    val context = LocalContext.current
    val couleurTexteParchemin = Color(0xFF3E2723)
    val parcheminBitmap = remember {
        runCatching {
            context.assets.open("dnd/theme/th_parchemin.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }
    val caseBitmap = remember {
        runCatching {
            context.assets.open("dnd/theme/th_case.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }

    // "Bibliothèque" et "Connexion/Partie" ne sont pas repris ici : déjà accessibles
    // depuis la barre du bas globale (AppBottomBar), un outil dédié ici ferait doublon.
    val tools = listOf(
        MjTool("characters", "FICHES", "Voir et modifier", Icons.Default.People, MaterialTheme.colorScheme.secondary),
        MjTool("campaigns", "CAMPAGNES", "Suivi d'objectifs", Icons.Default.Checklist, MaterialTheme.colorScheme.tertiary),
        MjTool("groups", "GROUPES", "Gérer les groupes", Icons.Default.Group, MaterialTheme.colorScheme.tertiary),
        MjTool("music", "MUSIQUE", "Ambiance sonore", Icons.Default.MusicNote, MaterialTheme.colorScheme.primary),
    )

    var showReader by remember { mutableStateOf(false) }

    val mjScenarios by GameState.mjScenarios.collectAsState()
    val mjCampaigns by GameState.mjCampaigns.collectAsState()
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
        mjScenarios
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

    fun handleToolClick(toolId: String) {
        when (toolId) {
            "characters" -> onViewCharacters()
            "campaigns" -> onOpenCampaigns()
            "groups" -> onOpenGroups()
            "music" -> onOpenMusic()
        }
    }

    Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Box(contentAlignment = Alignment.Center) {
                            if (parcheminBitmap != null) {
                                Image(
                                    bitmap = parcheminBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                            }
                            Text(
                                text = if (showReader && selectedScenario != null) selectedScenario.title else "MAÎTRE DU JEU",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                                color = if (parcheminBitmap != null) couleurTexteParchemin else Color.Unspecified,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                // Texte à sa taille d'origine ; seul le fond parchemin (padding autour) est agrandi de 20%.
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        if (showReader) {
                            IconButton(onClick = { showReader = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour au tableau de bord")
                            }
                        } else {
                            IconButton(onClick = onOpenMenu) {
                                Icon(Icons.Default.Menu, contentDescription = "Ouvrir le menu")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
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

                    // --- Carte "scénario en cours" (équivalent réel de "prochaine partie") ---
                    // Fond th_case dessiné derrière la Surface, rendue transparente quand
                    // l'asset est chargé, même principe que th_cadre sur DashboardToolCard.
                    // Case agrandie de 20% (260dp -> 312dp).
                    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 312.dp)) {
                        if (caseBitmap != null) {
                            Image(
                                bitmap = caseBitmap,
                                contentDescription = null,
                                modifier = Modifier.matchParentSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    if (selectedScenario != null) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            color = if (caseBitmap != null) Color.Transparent else MaterialTheme.colorScheme.surface,
                            border = if (caseBitmap != null) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        ) {
                            Column(modifier = Modifier.padding(28.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SCÉNARIO EN COURS",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Black,
                                        color = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = selectedScenario.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Casino, contentDescription = null, tint = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = currentWorld?.name ?: "Monde inconnu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (selectedScenario.scenes.size > 1) {
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${selectedScenario.scenes.size} scènes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurfaceVariant
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
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            color = if (caseBitmap != null) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(modifier = Modifier.padding(28.dp)) {
                                Text(
                                    text = "Aucun scénario sélectionné",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Choisissez un scénario existant ou créez-en un nouveau.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (caseBitmap != null) couleurTexteParchemin else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = onOpenMenu) {
                                        Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Charger un scénario")
                                    }
                                    OutlinedButton(onClick = { onOpenScenarioEditor(null) }) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Nouveau")
                                    }
                                }
                            }
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

@Composable
private fun DashboardToolCard(
    tool: MjTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Cadre décoratif fourni en asset (pas en drawable, même convention que ic_acceuil dans
    // AppBottomBar) ; repli silencieux sur la Surface nue si le fichier est absent, pour ne
    // jamais casser le tableau de bord.
    val context = LocalContext.current
    val cadreBitmap = remember {
        runCatching {
            context.assets.open("dnd/theme/th_cadre.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }
    Box(modifier = modifier.fillMaxWidth()) {
        // th_cadre.png est un fond de case complet (pas juste une bordure creuse) : il doit
        // être dessiné EN DESSOUS du contenu, pas par-dessus, sinon il masque le texte.
        if (cadreBitmap != null) {
            Image(
                bitmap = cadreBitmap,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = if (cadreBitmap != null) Color.Transparent else MaterialTheme.colorScheme.surface,
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(tool.color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tool.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (cadreBitmap != null) Color.White else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (cadreBitmap != null) Color.White.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}
