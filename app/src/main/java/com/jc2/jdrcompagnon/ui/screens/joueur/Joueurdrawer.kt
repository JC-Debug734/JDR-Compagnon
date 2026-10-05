package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.PlayerConnectionState
import com.jc2.jdrcompagnon.network.SessionRole
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.launch

/**
 * Menu latéral partagé côté Joueur, utilisé de la même façon sur les 3
 * écrans concernés (accueil joueur, sélection de personnage, fiche de
 * personnage) pour garder une navigation cohérente.
 *
 * Deux choix : "Choisir un personnage" et "Configuration". Ce dernier
 * n'ouvre pour l'instant qu'une boîte de dialogue permettant de changer
 * le pseudo du joueur (GameState.playerName) ; d'autres réglages
 * pourront y être ajoutés plus tard.
 *
 * Palette forcée sur ForcedDarkPalette (la même que la barre de
 * navigation du bas) plutôt que sur MaterialTheme brut ou SheetTheme,
 * pour que le tiroir soit visuellement identique à cette barre.
 */
@Composable
fun JoueurDrawer(
    currentWorld: WorldState? = null,
    onOpenAccueil: () -> Unit,
    onChooseCharacter: () -> Unit,
    onOpenSettings: () -> Unit = {},
    // Écran de campagne, limité aux villes et à la carte côté joueur (pas la
    // fiche de suivi MJ ni les événements aléatoires) : n'apparaît que si une
    // campagne est sélectionnée par le MJ.
    onOpenVilles: (campagneId: String) -> Unit = {},
    onOpenCarte: (campagneId: String) -> Unit = {},
    onOpenRepos: () -> Unit = {},
    onOpenQuetes: () -> Unit = {},
    onOpenGroupe: () -> Unit = {},
    onOpenCombatActions: () -> Unit = {},
    onOpenProposalStatus: () -> Unit = {},
    onClose: () -> Unit,
    // true une fois qu'un personnage réseau a été réservé/accepté : le joueur
    // y reste assigné pour la session, donc plus d'entrée pour en changer.
    characterLocked: Boolean = false,
) {
    val selectedCharacterId by GameState.selectedCharacterId.collectAsState()
    val mjGroups by GameState.mjGroups.collectAsState()
    val mjCampaigns by GameState.mjCampaigns.collectAsState()
    val selectedCampaignId by GameState.currentCampaignId.collectAsState()
    val currentGroup = remember(selectedCharacterId, mjGroups, currentWorld?.id) {
        GameState.groupForCharacter(selectedCharacterId, currentWorld?.id)
    }
    val selectedCampaign = selectedCampaignId?.let { id ->
        mjCampaigns.firstOrNull { it.id == id && it.worldId == (currentWorld?.id ?: "") }
    }
    val context = LocalContext.current
    val networkRole by NetworkSessionManager.role.collectAsState()
    val playerConnectionState by NetworkSessionManager.playerState.collectAsState()
    // Connecté (ou en cours de connexion/reconnexion) à une partie hébergée par un MJ : le
    // rôle ne doit pas pouvoir changer sous le pied de cette connexion (perte de la session
    // réseau, personnage revendiqué laissé orphelin côté hôte).
    val isConnectedToGame = networkRole == SessionRole.PLAYER && playerConnectionState != PlayerConnectionState.DISCONNECTED
    var showBlockedRoleChangeDialog by remember { mutableStateOf(false) }
    val activeProposalState by NetworkSessionManager.activeProposalState.collectAsState()
    val networkGroup by NetworkSessionManager.networkGroup.collectAsState()

    ModalDrawerSheet(
        drawerContainerColor = ForcedDarkPalette.Surface,
        drawerContentColor = ForcedDarkPalette.Content,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Header with role icon (clickable to change role) and settings ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Role icon (clickable to return to role selection, sauf si connecté à une partie)
                IconButton(
                    onClick = {
                        if (isConnectedToGame) {
                            showBlockedRoleChangeDialog = true
                        } else {
                            onClose()
                            GameState.requestRoleChange()
                        }
                    },
                    modifier = Modifier.size(70.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_joueur),
                        contentDescription = "Joueur — changer de rôle",
                        modifier = Modifier.size(70.dp),
                        alpha = if (isConnectedToGame) 0.5f else 1f,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Joueur",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ForcedDarkPalette.AccentGold,
                    modifier = Modifier.weight(1f),
                )

                IconButton(
                    onClick = {
                        onClose()
                        onOpenSettings()
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Réglages",
                        tint = ForcedDarkPalette.AccentGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            HorizontalDivider(color = ForcedDarkPalette.Indicator)

            ScenarioClockRow()

            // Connecté à une partie : campagne choisie par le MJ (ses cartes et lieux révélés sont
            // recopiés sur cet appareil, voir CarteSyncReseau) ; sinon, la campagne locale.
            val networkCampagne by NetworkSessionManager.networkCampagne.collectAsState()
            val campagneId = networkCampagne?.first ?: selectedCampaign?.id
            Text(
                text = "Campagne : ${networkCampagne?.second ?: selectedCampaign?.title ?: "Aucune"}",
                style = MaterialTheme.typography.labelSmall,
                color = ForcedDarkPalette.Content,
            )
            // Côté joueur, l'écran de campagne se limite à la carte (pas la fiche de suivi MJ,
            // ni les lieux/événements réservés au MJ) ; la ville du groupe s'affiche dessous.
            if (campagneId != null) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Map, null, tint = ForcedDarkPalette.Content) },
                    label = { Text("Carte", color = ForcedDarkPalette.Content) },
                    selected = false,
                    onClick = {
                        onClose()
                        onOpenCarte(campagneId)
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = ForcedDarkPalette.Surface,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )
                VilleDuGroupe(
                    campagneId = campagneId,
                    lieu = networkGroup?.details?.location ?: currentGroup?.location.orEmpty(),
                )
                ServicesDuLieu(
                    campagneId = campagneId,
                    lieu = networkGroup?.details?.location ?: currentGroup?.location.orEmpty(),
                    enReseau = networkCampagne != null,
                    characterId = selectedCharacterId,
                    connecte = isConnectedToGame,
                )
            }

            // Journal de quêtes : quêtes rendues visibles par le MJ (en cours et terminées).
            val networkQuests by NetworkSessionManager.networkQuests.collectAsState()
            run {
                val enCours = networkQuests?.count { it.status == "EN_COURS" }
                    ?: selectedCampaign?.quests?.count { it.visibleToPlayers && it.status.name == "EN_COURS" } ?: 0
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Flag, null, tint = ForcedDarkPalette.Content) },
                    label = { Text("Quêtes" + if (enCours > 0) " ($enCours en cours)" else "", color = ForcedDarkPalette.Content) },
                    selected = false,
                    onClick = {
                        onClose()
                        onOpenQuetes()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = ForcedDarkPalette.Surface,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )
            }

            HorizontalDivider(color = ForcedDarkPalette.Indicator)

            // Connecté à une partie : le groupe vient du MJ (le joueur n'a pas ses groupes en local).
            val groupName = networkGroup?.name ?: currentGroup?.name
            Text(
                text = "Groupe : ${groupName ?: "Non assigné"}",
                style = MaterialTheme.typography.labelSmall,
                color = ForcedDarkPalette.Content,
            )
            networkGroup?.members?.forEach { member ->
                Text(
                    text = "• ${member.name} — ${member.characterClass} niv. ${member.level}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (member.id == selectedCharacterId) FontWeight.Bold else FontWeight.Normal,
                    color = ForcedDarkPalette.Content,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            // Détail du groupe (lieu, montures, véhicules, inventaire, réputation...), synchronisé par le MJ.
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Group, null, tint = ForcedDarkPalette.Content) },
                label = { Text("Groupe", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = {
                    onClose()
                    onOpenGroupe()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content
                )
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Bedtime, null, tint = ForcedDarkPalette.Content) },
                label = { Text("Repos", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = {
                    onClose()
                    onOpenRepos()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content
                )
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Shield, null, tint = ForcedDarkPalette.Content) },
                label = { Text("Actions de Combat", color = ForcedDarkPalette.Content) },
                selected = false,
                onClick = {
                    onClose()
                    onOpenCombatActions()
                },
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = ForcedDarkPalette.Surface,
                    unselectedIconColor = ForcedDarkPalette.Content,
                    unselectedTextColor = ForcedDarkPalette.Content
                )
            )

            if (activeProposalState.isNotEmpty()) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.HowToVote, null, tint = ForcedDarkPalette.Content) },
                    label = { Text("Proposition en cours", color = ForcedDarkPalette.Content) },
                    selected = false,
                    onClick = {
                        onClose()
                        onOpenProposalStatus()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = ForcedDarkPalette.Surface,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )

                HorizontalDivider(color = ForcedDarkPalette.Indicator)
            }

            // Choose character (masqué une fois assigné à un personnage réseau)
            if (!characterLocked) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Group, null, tint = ForcedDarkPalette.Content) },
                    label = { Text("Choisir un personnage", color = ForcedDarkPalette.Content) },
                    selected = false,
                    onClick = {
                        onClose()
                        onChooseCharacter()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = ForcedDarkPalette.Surface,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )

                HorizontalDivider(color = ForcedDarkPalette.Indicator)
            }
        }
    }

    if (showBlockedRoleChangeDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedRoleChangeDialog = false },
            title = { Text("Connecté à une partie") },
            text = { Text("Vous êtes connecté à une partie hébergée par le MJ. Déconnectez-vous avant de changer de rôle.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        NetworkSessionManager.disconnect(context)
                        showBlockedRoleChangeDialog = false
                        onClose()
                        GameState.requestRoleChange()
                    }
                ) { Text("Se déconnecter et changer") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockedRoleChangeDialog = false }) { Text("Annuler") }
            }
        )
    }
}

/**
 * Ville où se trouve le groupe (localisation du groupe rapprochée, par son nom, d'une ville de la
 * campagne) : entrée du menu à son nom, qui ouvre la liste des lieux à visiter. Le joueur arrive
 * toujours par l'entrée de la ville (VisiteVille), puis choisit où il va.
 */
@Composable
private fun VilleDuGroupe(campagneId: String, lieu: String) {
    val context = LocalContext.current
    // null tant que la base n'a pas répondu : ne pas prendre ce premier instant pour un départ.
    val points by remember(campagneId) {
        com.jc2.jdrcompagnon.di.CarteDependencies.repository.observerPoints(campagneId)
    }.collectAsState(initial = null)
    val charges = points ?: return
    val ville = charges.firstOrNull {
        it.type == com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret.VILLE &&
            lieu.isNotBlank() && it.nom.trim().equals(lieu.trim(), ignoreCase = true)
    }
    if (ville == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) { com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.quitter(context) }
        return
    }
    val lieux by remember(ville.id) {
        com.jc2.jdrcompagnon.di.CarteDependencies.repository.observerLieuxNotables(ville.id)
    }.collectAsState(initial = emptyList())
    var lieuActuelId by remember(ville.id) {
        com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.arriver(context, ville.id)
        mutableStateOf(com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.lieuActuel(context, ville.id))
    }
    val lieuActuel = lieux.firstOrNull { it.id == lieuActuelId }
    var ouvert by remember { mutableStateOf(false) }

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.LocationCity, null, tint = ForcedDarkPalette.AccentGold) },
        label = {
            Column {
                Text(ville.nom, color = ForcedDarkPalette.Content, fontWeight = FontWeight.Bold)
                Text(
                    "Vous êtes : ${lieuActuel?.nom ?: com.jc2.jdrcompagnon.feature_carte.ui.ENTREE_DE_LA_VILLE}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                )
            }
        },
        selected = false,
        onClick = { ouvert = true },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = ForcedDarkPalette.Surface,
            unselectedIconColor = ForcedDarkPalette.Content,
            unselectedTextColor = ForcedDarkPalette.Content
        )
    )

    if (ouvert) {
        AlertDialog(
            onDismissRequest = { ouvert = false },
            title = { Text(ville.nom) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Où allez-vous ?", style = MaterialTheme.typography.labelMedium)
                    LieuVilleRow(
                        nom = com.jc2.jdrcompagnon.feature_carte.ui.ENTREE_DE_LA_VILLE,
                        description = ville.description,
                        ici = lieuActuelId == null || lieuActuel == null,
                    ) {
                        com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.aller(context, ville.id, null)
                        lieuActuelId = null
                    }
                    lieux.forEach { l ->
                        LieuVilleRow(nom = l.nom, description = l.description, ici = l.id == lieuActuel?.id) {
                            com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.aller(context, ville.id, l.id)
                            lieuActuelId = l.id
                        }
                    }
                    if (lieux.isEmpty()) {
                        Text("Le MJ n'a pas encore indiqué d'autre lieu à visiter.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { ouvert = false }) { Text("Fermer") } },
        )
    }
}

/** Lieu à visiter d'une ville dans la liste du menu joueur ; [ici] = lieu actuel du joueur. */
@Composable
private fun LieuVilleRow(nom: String, description: String, ici: Boolean, onAller: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onAller,
        shape = MaterialTheme.shapes.medium,
        color = if (ici) ForcedDarkPalette.AccentGold.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = if (ici) ForcedDarkPalette.AccentGold else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nom, fontWeight = FontWeight.Bold)
                if (description.isNotBlank()) {
                    Text(description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
            }
            if (ici) Text("Vous êtes ici", style = MaterialTheme.typography.labelSmall, color = ForcedDarkPalette.AccentGold)
        }
    }
}

/**
 * Services disponibles là où se trouve le groupe : la localisation du groupe est rapprochée (par
 * son nom) d'une ville de la campagne, dont les boutiques proposent leurs services. En partie
 * réseau, boutiques et services viennent du MJ (CarteSyncReseau.boutiquesReseau) ; sinon, de la
 * base locale. Rien n'est affiché si le groupe n'est pas dans une ville ayant des services.
 */
@Composable
private fun ServicesDuLieu(
    campagneId: String,
    lieu: String,
    enReseau: Boolean,
    characterId: String?,
    connecte: Boolean,
) {
    val points by remember(campagneId) {
        com.jc2.jdrcompagnon.di.CarteDependencies.repository.observerPoints(campagneId)
    }.collectAsState(initial = emptyList())
    val ville = points.firstOrNull {
        it.type == com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret.VILLE &&
            lieu.isNotBlank() && it.nom.trim().equals(lieu.trim(), ignoreCase = true)
    } ?: return
    val boutiquesReseau by com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.boutiquesReseau.collectAsState()
    val boutiquesLocales by remember {
        com.jc2.jdrcompagnon.di.BoutiqueDependencies.repository.observerToutesLesBoutiques()
    }.collectAsState(initial = emptyList())
    val boutiques = if (enReseau) {
        boutiquesReseau.filter { it.villeId == ville.id }
    } else {
        com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.boutiquesJoueur(listOf(ville), boutiquesLocales)
    }.filter { it.services.isNotEmpty() }
    if (boutiques.isEmpty()) return

    val characters by GameState.characters.collectAsState()
    val personnage = characters.firstOrNull { it.id == characterId }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var ouvert by remember { mutableStateOf(false) }

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Storefront, null, tint = ForcedDarkPalette.Content) },
        label = {
            Text(
                "Services — ${ville.nom} (${boutiques.sumOf { it.services.size }})",
                color = ForcedDarkPalette.Content,
            )
        },
        selected = false,
        onClick = { ouvert = true },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = ForcedDarkPalette.Surface,
            unselectedIconColor = ForcedDarkPalette.Content,
            unselectedTextColor = ForcedDarkPalette.Content
        )
    )

    if (ouvert) {
        com.jc2.jdrcompagnon.feature_boutique.ui.ServicesLieuDialog(
            lieu = ville.nom,
            boutiques = boutiques,
            personnage = personnage,
            onUtiliser = { boutique, service ->
                val id = personnage?.id ?: return@ServicesLieuDialog
                // Paiement sur la fiche (relayée au MJ en partie réseau), puis place décomptée.
                GameState.addGold(id, -service.prixEnPo)
                if (connecte) {
                    NetworkSessionManager.signalerServiceUtilise(id, boutique.id, service.nom)
                } else {
                    scope.launch {
                        com.jc2.jdrcompagnon.feature_boutique.domain.usecase.UtiliserServiceUseCase.decompter(boutique.id, service.nom)
                    }
                }
            },
            onDismiss = { ouvert = false },
        )
    }
}

/**
 * Boîte de dialogue de configuration côté joueur. Pour l'instant, permet
 * uniquement de changer le pseudo (GameState.playerName) ; d'autres
 * réglages viendront s'y ajouter par la suite.
 */
@Composable
private fun PlayerSettingsDialog(onDismiss: () -> Unit) {
    val currentPlayerName by GameState.playerName.collectAsState()
    var pseudo by remember(currentPlayerName) { mutableStateOf(currentPlayerName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuration") },
        text = {
            OutlinedTextField(
                value = pseudo,
                onValueChange = { pseudo = it },
                label = { Text("Pseudo") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pseudo.isNotBlank()) {
                        GameState.setPlayerName(pseudo.trim())
                    }
                    onDismiss()
                },
                enabled = pseudo.isNotBlank()
            ) { Text("Enregistrer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}