package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import com.jc2.jdrcompagnon.ui.components.CollapsibleSectionCard
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueDetailUiState
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueDetailViewModel

/** Texte secondaire (descriptions, prix, listes vides) : blanc légèrement atténué. */
private val TexteSecondaire = Color.White.copy(alpha = 0.8f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutiqueDetailScreen(
    viewModel: BoutiqueDetailViewModel,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var afficherAjoutEmploye by remember { mutableStateOf(false) }
    var employeEnEdition by remember { mutableStateOf<Int?>(null) }
    // -1 : nouveau service ; sinon index du service paramétré ; null : fermé.
    var serviceEnEdition by remember { mutableStateOf<Int?>(null) }
    var afficherAjoutArticle by remember { mutableStateOf(false) }
    var afficherApprovisionnement by remember { mutableStateOf(false) }
    var afficherConfirmationVisite by remember { mutableStateOf(false) }
    var equipementsDisponibles by remember { mutableStateOf<List<EquipementReference>>(emptyList()) }

    val titre = (uiState as? BoutiqueDetailUiState.Success)?.boutique?.nom?.uppercase() ?: "BOUTIQUE"

    // Chargé une seule fois par boutique affichée, réutilisé par les deux dialogs
    // (ajout manuel et réapprovisionnement filtré) plutôt que rechargé à chaque ouverture.
    LaunchedEffect((uiState as? BoutiqueDetailUiState.Success)?.boutique?.id) {
        if (uiState is BoutiqueDetailUiState.Success) {
            equipementsDisponibles = viewModel.equipementsDisponibles()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(titre, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { padding ->
        when (val state = uiState) {
            is BoutiqueDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            is BoutiqueDetailUiState.Introuvable -> Box(Modifier.fillMaxSize().padding(padding)) {
                Text("Boutique introuvable", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            is BoutiqueDetailUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                Text("Erreur : ${state.message}", color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            is BoutiqueDetailUiState.Success -> {
                val boutique = state.boutique

                val ownerKey = "boutique:${boutique.id}"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CollapsibleSectionCard(ownerKey, "Marchand") {
                        Text(boutique.marchand.nom, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${boutique.type.label} · ${boutique.standing.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TexteSecondaire
                        )
                        if (boutique.marchand.trait.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    boutique.marchand.trait,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TexteSecondaire,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.onRegenererTraitMarchand(boutique) }) {
                                    Icon(Icons.Default.Casino, contentDescription = "Régénérer le caractère du marchand")
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Paid, contentDescription = null)
                            Text(
                                "Argent disponible : ${boutique.argentDisponibleEnPo} po",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                            IconButton(onClick = { afficherConfirmationVisite = true }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Nouvelle visite")
                            }
                        }
                    }

                    CollapsibleSectionCard(
                        ownerKey,
                        "Employés",
                        headerActions = {
                            IconButton(onClick = { afficherAjoutEmploye = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter un employé")
                            }
                        }
                    ) {
                    if (boutique.employes.isEmpty()) {
                        Text("Aucun employé", color = TexteSecondaire)
                    }
                    boutique.employes.forEachIndexed { index, employe ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            // Appui sur l'employé : le paramétrer (nom, rôle, caractère).
                            Column(modifier = Modifier.weight(1f).clickable { employeEnEdition = index }) {
                                Text("${employe.nom} — ${employe.role.label}")
                                if (employe.trait.isNotBlank()) {
                                    Text(
                                        employe.trait,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TexteSecondaire
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.onSupprimerEmploye(boutique, index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Retirer ${employe.nom}", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    }

                    if (boutique.type == TypeBoutique.MARCHAND) {
                        CollapsibleSectionCard(
                            ownerKey,
                            "Inventaire",
                            headerActions = {
                                IconButton(onClick = { afficherApprovisionnement = true }) {
                                    Icon(Icons.Default.Sync, contentDescription = "Réapprovisionner")
                                }
                                IconButton(onClick = { afficherAjoutArticle = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Ajouter un article précis")
                                }
                            }
                        ) {
                        if (boutique.inventaire.isEmpty()) {
                            Text("Aucun article", color = TexteSecondaire)
                        }
                        boutique.inventaire.forEachIndexed { index, article ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(article.equipement.nom)
                                    Text(
                                        "${article.prixApplique} po · x${article.quantiteStock}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TexteSecondaire
                                    )
                                }
                                IconButton(onClick = { viewModel.onToggleArticlePermanent(boutique, index) }) {
                                    Icon(
                                        if (article.toujoursDisponible) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = if (article.toujoursDisponible)
                                            "Retirer ${article.equipement.nom} des articles toujours disponibles"
                                        else
                                            "Marquer ${article.equipement.nom} comme toujours disponible"
                                    )
                                }
                                IconButton(onClick = { viewModel.onSupprimerArticle(boutique, index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Retirer ${article.equipement.nom}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        }
                    }

                    // Services : toute boutique peut en proposer (un marchand peut réparer,
                    // livrer, graver...), ajoutés et paramétrés par le MJ.
                    CollapsibleSectionCard(
                        ownerKey,
                        "Services",
                        headerActions = {
                            IconButton(onClick = { serviceEnEdition = -1 }) {
                                Icon(Icons.Default.Add, contentDescription = "Ajouter un service")
                            }
                        }
                    ) {
                        if (boutique.services.isEmpty()) {
                            Text("Aucun service", color = TexteSecondaire)
                        }
                        boutique.services.forEachIndexed { index, service ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(
                                    checked = service.actif,
                                    onCheckedChange = { viewModel.onToggleServiceActif(boutique, index) }
                                )
                                // Appui sur le service : le paramétrer (tarif, places, description).
                                Column(modifier = Modifier.weight(1f).clickable { serviceEnEdition = index }.padding(vertical = 4.dp)) {
                                    val couleur = if (service.actif) TexteSecondaire else Color.White.copy(alpha = 0.4f)
                                    Text(
                                        if (service.actif) service.nom else "${service.nom} (désactivé)",
                                        color = if (service.actif) Color.Unspecified else couleur
                                    )
                                    Text(service.description, style = MaterialTheme.typography.bodySmall, color = couleur)
                                    Text(
                                        if (service.quantiteDisponible != null)
                                            "${service.prixEnPo} po · ${service.quantiteDisponible} places"
                                        else
                                            "${service.prixEnPo} po",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = couleur
                                    )
                                }
                                IconButton(onClick = { viewModel.onSupprimerService(boutique, index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Retirer ${service.nom}", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                employeEnEdition?.let { index ->
                    boutique.employes.getOrNull(index)?.let { employe ->
                        AjouterEmployeDialog(
                            initial = employe,
                            onGenererNom = viewModel::genererNomEmployeAleatoire,
                            onGenererTrait = viewModel::genererTraitAleatoire,
                            onDismiss = { employeEnEdition = null },
                            onConfirmer = { nom, role, trait ->
                                viewModel.onModifierEmploye(boutique, index, nom, role, trait)
                                employeEnEdition = null
                            }
                        )
                    }
                }

                serviceEnEdition?.let { index ->
                    ServiceFormDialog(
                        initial = boutique.services.getOrNull(index),
                        onDismiss = { serviceEnEdition = null },
                        onConfirmer = { service ->
                            if (index < 0) viewModel.onAjouterService(boutique, service)
                            else viewModel.onModifierService(boutique, index, service)
                            serviceEnEdition = null
                        }
                    )
                }

                if (afficherAjoutEmploye) {
                    AjouterEmployeDialog(
                        onGenererNom = viewModel::genererNomEmployeAleatoire,
                        onGenererTrait = viewModel::genererTraitAleatoire,
                        onDismiss = { afficherAjoutEmploye = false },
                        onConfirmer = { nom, role, trait ->
                            viewModel.onAjouterEmploye(boutique, nom, role, trait)
                            afficherAjoutEmploye = false
                        }
                    )
                }

                if (afficherAjoutArticle) {
                    AjouterArticleDialog(
                        equipementsDisponibles = equipementsDisponibles,
                        prixSuggere = { equipement -> viewModel.prixSuggere(equipement.coutBaseEnPo, boutique) },
                        onDismiss = { afficherAjoutArticle = false },
                        onConfirmer = { equipement, prix, quantite, toujoursDisponible ->
                            viewModel.onAjouterArticle(boutique, equipement, prix, quantite, toujoursDisponible)
                            afficherAjoutArticle = false
                        }
                    )
                }

                if (afficherApprovisionnement) {
                    ApprovisionnerBoutiqueDialog(
                        equipementsDisponibles = equipementsDisponibles,
                        budgetSuggereEnPo = viewModel.budgetSuggere(boutique),
                        onDismiss = { afficherApprovisionnement = false },
                        onConfirmer = { filtres, budget ->
                            viewModel.onApprovisionner(boutique, filtres, budget)
                            afficherApprovisionnement = false
                        }
                    )
                }

                if (afficherConfirmationVisite) {
                    AlertDialog(
                        onDismissRequest = { afficherConfirmationVisite = false },
                        title = { Text("Nouvelle visite") },
                        text = {
                            Text(
                                if (boutique.type == TypeBoutique.MARCHAND)
                                    "Le stock va être renouvelé (les articles marqués « toujours disponibles » sont conservés) et l'argent disponible va être réinitialisé. Continuer ?"
                                else
                                    "Les services générés vont être renouvelés (ceux que vous avez ajoutés ou paramétrés sont conservés) et l'argent disponible va être réinitialisé. Continuer ?"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.onNouvelleVisite(boutique)
                                afficherConfirmationVisite = false
                            }) { Text("Confirmer") }
                        },
                        dismissButton = {
                            TextButton(onClick = { afficherConfirmationVisite = false }) { Text("Annuler") }
                        }
                    )
                }
            }
        }
    }
}