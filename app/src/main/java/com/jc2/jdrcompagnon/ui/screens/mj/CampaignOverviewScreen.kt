package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.di.CarteDependencies
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ChapitresScenarios
import com.jc2.jdrcompagnon.feature_carte.data.CarteImageStore
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.ui.envoyerCarteALaTable
import com.jc2.jdrcompagnon.feature_quete.ui.CampaignQuestsContent
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import com.jc2.jdrcompagnon.ui.rememberImageCampagne
import com.jc2.jdrcompagnon.ui.rememberImageFichier
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Page de la campagne en cours, ouverte depuis la carte "Campagne en cours" de l'accueil MJ :
 * image de la campagne, carte(s) de la zone, scénarios et bestiaire attachés, et quêtes (créées,
 * modifiées et validées directement ici, sans passer par l'éditeur).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignOverviewScreen(
    campaignId: String,
    onBack: () -> Unit,
    onEditCampaign: (String) -> Unit,
    onOpenCarte: (campagneId: String, carteId: String?) -> Unit,
    onOpenScenario: (String) -> Unit,
    onOpenMonster: (String) -> Unit,
) {
    val campaigns by GameState.mjCampaigns.collectAsState()
    val scenarios by GameState.mjScenarios.collectAsState()
    val lastScenarioId by GameState.lastScenarioId.collectAsState()
    val campaign = campaigns.firstOrNull { it.id == campaignId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        campaign?.title ?: "Campagne",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    if (campaign != null) {
                        IconButton(onClick = { onEditCampaign(campaign.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Éditer la campagne")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (campaign == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Campagne introuvable.", color = Color.White)
            }
            return@Scaffold
        }
        val ownerKey = "campagne-vue:${campaign.id}"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            rememberImageCampagne(campaign)?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = "Image de la campagne",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }

            CarteDeLaZoneSection(ownerKey, campaign.id, onOpenCarte)

            val attachesBruts = campaign.scenarioIds.mapNotNull { id -> scenarios.firstOrNull { it.id == id } }
            // Ordre de jeu : par chapitre puis par numéro (ordre de la campagne sinon).
            val attaches = ChapitresScenarios.trier(attachesBruts)
            val avecChapitres = ChapitresScenarios.aDesChapitres(attaches)
            CollapsibleSectionCard(ownerKey = ownerKey, title = "Scénarios") {
                if (attaches.isEmpty()) {
                    Text("Aucun scénario attaché à cette campagne.", style = MaterialTheme.typography.bodyMedium, color = OverviewHintColor)
                } else {
                    attaches.forEachIndexed { index, scenario ->
                        val chapitre = ChapitresScenarios.libelleChapitre(scenario.chapitreNumero, scenario.chapitreTitre)
                        val chapitrePrecedent = attaches.getOrNull(index - 1)?.let { ChapitresScenarios.libelleChapitre(it.chapitreNumero, it.chapitreTitre) }
                        if (avecChapitres && (index == 0 || chapitre != chapitrePrecedent)) {
                            Text(
                                chapitre ?: "Sans chapitre",
                                color = ForcedDarkPalette.AccentGold,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp, bottom = 2.dp),
                            )
                        }
                        val enCours = scenario.id == lastScenarioId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenScenario(scenario.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = ForcedDarkPalette.AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ChapitresScenarios.titreNumerote(scenario), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val detail = listOfNotNull(
                                    "En cours".takeIf { enCours },
                                    scenario.scenes.size.takeIf { it > 1 }?.let { "$it scènes" },
                                    scenario.lieuNom.ifBlank { null },
                                ).joinToString(" · ")
                                if (detail.isNotBlank()) {
                                    Text(detail, style = MaterialTheme.typography.bodySmall, color = OverviewHintColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Lire", tint = Color.White)
                        }
                        if (index < attaches.lastIndex) HorizontalDivider()
                    }
                }
            }

            CollapsibleSectionCard(
                ownerKey = ownerKey,
                title = "Bestiaire",
                headerActions = {
                    if (campaign.monsterIds.isNotEmpty()) {
                        Text("${campaign.monsterIds.size}", color = OverviewHintColor, style = MaterialTheme.typography.labelLarge)
                    }
                }
            ) {
                if (campaign.monsterIds.isEmpty()) {
                    Text(
                        "Aucun monstre attaché. Ajoutez-en depuis l'éditeur de campagne.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OverviewHintColor
                    )
                } else {
                    campaign.monsterIds.sortedBy { it.lowercase() }.forEach { nom ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onOpenMonster(nom) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
                            Text(nom, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ChevronRight, contentDescription = "Ouvrir", tint = OverviewHintColor)
                        }
                    }
                }
            }

            val total = campaign.quests.size
            val terminees = campaign.quests.count { it.status == com.jc2.jdrcompagnon.feature_quete.domain.model.QuestStatus.TERMINEE }
            CollapsibleSectionCard(
                ownerKey = ownerKey,
                title = "Quêtes",
                headerActions = {
                    if (total > 0) {
                        Text("$terminees/$total", color = OverviewHintColor, style = MaterialTheme.typography.labelLarge)
                    }
                }
            ) {
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { terminees.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
                CampaignQuestsContent(campaign)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/** Carte(s) géographique(s) de la campagne : aperçu de chacune, un toucher ouvre la carte. */
@Composable
private fun CarteDeLaZoneSection(
    ownerKey: String,
    campagneId: String,
    onOpenCarte: (campagneId: String, carteId: String?) -> Unit,
) {
    val cartes by remember(campagneId) { CarteDependencies.repository.observerCartes(campagneId) }
        .collectAsState(initial = emptyList())
    CollapsibleSectionCard(ownerKey = ownerKey, title = if (cartes.size > 1) "Cartes de la zone" else "Carte de la zone") {
        if (cartes.isEmpty()) {
            Text(
                "Aucune carte. Créez-en une depuis l'éditeur de campagne.",
                style = MaterialTheme.typography.bodyMedium,
                color = OverviewHintColor
            )
        } else {
            cartes.forEach { carte -> ApercuCarte(carte) { onOpenCarte(campagneId, carte.id) } }
        }
    }
}

@Composable
private fun ApercuCarte(carte: CarteCampagne, onOpen: () -> Unit) {
    val context = LocalContext.current
    val image = rememberImageFichier(carte.imageFileName?.let { CarteImageStore.fichier(context, it) }, maxDimension = 1200)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onOpen)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = "Carte ${carte.nom}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Map, contentDescription = null, tint = ForcedDarkPalette.AccentGold, modifier = Modifier.size(20.dp))
            Text(carte.nom.ifBlank { "Carte" }, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (carte.imageFileName != null) {
                IconButton(onClick = { envoyerCarteALaTable(context, carte) }) {
                    Icon(Icons.Default.Send, contentDescription = "Envoyer à l'écran de table", tint = Color.White)
                }
            }
            Text("Ouvrir", color = OverviewHintColor, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val OverviewHintColor = Color.White.copy(alpha = 0.8f)
