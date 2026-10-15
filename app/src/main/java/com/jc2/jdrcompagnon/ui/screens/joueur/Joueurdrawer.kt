package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.drawWithContent
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
        // Liseré doré sur le flanc droit du menu, comme le menu du MJ et la barre du bas.
        modifier = Modifier.drawWithContent {
            drawContent()
            val x = size.width - 1.dp.toPx()
            drawLine(
                color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore,
                start = androidx.compose.ui.geometry.Offset(x, 16.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(x, size.height - 16.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        },
        drawerContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
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

            HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)

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
                        unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )
                VilleDuGroupe(
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
                // Fenêtre par-dessus l'écran en cours, comme la liste des lieux de la ville.
                var quetesOuvertes by remember { mutableStateOf(false) }
                if (quetesOuvertes) {
                    com.jc2.jdrcompagnon.feature_quete.ui.PlayerQuestsDialog(onDismiss = { quetesOuvertes = false })
                }
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Flag, null, tint = ForcedDarkPalette.Content) },
                    label = { Text("Quêtes" + if (enCours > 0) " ($enCours en cours)" else "", color = ForcedDarkPalette.Content) },
                    selected = false,
                    onClick = { quetesOuvertes = true },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )
            }

            HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)

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
                    unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
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
                    unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
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
                    unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
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
                        unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )

                HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)
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
                        unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
                        unselectedIconColor = ForcedDarkPalette.Content,
                        unselectedTextColor = ForcedDarkPalette.Content
                    )
                )

                HorizontalDivider(thickness = 1.dp, color = com.jc2.jdrcompagnon.ui.components.IconesAsset.LisereDore)
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

/** Lieu d'une ville où le joueur peut se rendre : lieu notable, boutique, ou les deux (même nom). */
private data class LieuVisitable(
    // null = entrée de la ville ; "boutique:<id>" pour une boutique sans lieu notable à son nom.
    val id: String?,
    val nom: String,
    val description: String,
    val boutique: com.jc2.jdrcompagnon.network.BoutiqueJoueurData?,
    // Lieu notable (avec sa fonction : temple, panneau de quêtes...), null = entrée ou boutique seule.
    val notable: com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable? = null,
)

/**
 * Ville où se trouve le groupe (localisation du groupe rapprochée, par son nom, d'une ville de la
 * campagne) : entrée du menu à son nom, qui ouvre la liste des lieux à visiter — lieux notables et
 * boutiques — avec, pour chacun, les autres joueurs qui s'y trouvent. Le joueur arrive toujours par
 * l'entrée de la ville (VisiteVille) ; choisir un lieu l'y emmène (position envoyée au MJ, qui la
 * diffuse aux autres) et ouvre son écran : particularité, commerce et services (CommerceLieuDialog).
 *
 * En partie réseau, boutiques, stock et services viennent du MJ (CarteSyncReseau.boutiquesReseau) ;
 * sinon, de la base locale.
 */
@Composable
private fun VilleDuGroupe(
    campagneId: String,
    lieu: String,
    enReseau: Boolean,
    characterId: String?,
    connecte: Boolean,
) {
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
        androidx.compose.runtime.LaunchedEffect(Unit) {
            com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.quitter(context)
            NetworkSessionManager.signalerPositionVille(null)
            characterId?.let { GameState.setLieuVille(it, "") }
        }
        return
    }
    val lieuxNotables by remember(ville.id) {
        com.jc2.jdrcompagnon.di.CarteDependencies.repository.observerLieuxNotables(ville.id)
    }.collectAsState(initial = emptyList())
    val boutiquesReseau by com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.boutiquesReseau.collectAsState()
    val boutiquesLocales by remember {
        com.jc2.jdrcompagnon.di.BoutiqueDependencies.repository.observerToutesLesBoutiques()
    }.collectAsState(initial = emptyList())
    val boutiques = if (enReseau) {
        boutiquesReseau.filter { it.villeId == ville.id }
    } else {
        com.jc2.jdrcompagnon.feature_carte.data.CarteSyncReseau.boutiquesJoueur(listOf(ville), boutiquesLocales)
    }
    // Un lieu notable au nom d'une boutique est cette boutique ; les autres boutiques sont des lieux à part.
    val lieux = buildList {
        add(LieuVisitable(null, com.jc2.jdrcompagnon.feature_carte.ui.ENTREE_DE_LA_VILLE, ville.description, null))
        // Lieux cachés par le MJ : jamais proposés (en réseau, ils ne sont même pas transmis).
        lieuxNotables.filter { it.visibleJoueurs }.forEach { l ->
            add(LieuVisitable(l.id, l.nom, l.description, boutiques.firstOrNull { it.nom.trim().equals(l.nom.trim(), ignoreCase = true) }, l))
        }
        boutiques.filter { b -> none { it.boutique?.id == b.id } }.forEach { b ->
            add(LieuVisitable("boutique:${b.id}", b.nom, "", b))
        }
    }

    var lieuActuelId by remember(ville.id) {
        com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.arriver(context, ville.id)
        mutableStateOf(com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.lieuActuel(context, ville.id))
    }
    val lieuActuel = lieux.firstOrNull { it.id == lieuActuelId } ?: lieux.first()
    androidx.compose.runtime.LaunchedEffect(ville.id, lieuActuel.id, lieuActuel.nom, characterId) {
        // Sur la fiche aussi : le MJ voit où est le personnage, sur le même téléphone comme en réseau.
        characterId?.let { GameState.setLieuVille(it, "${ville.nom} · ${lieuActuel.nom}") }
        NetworkSessionManager.signalerPositionVille(
            com.jc2.jdrcompagnon.network.PositionVilleData(villeId = ville.id, lieuId = lieuActuel.id, lieuNom = lieuActuel.nom)
        )
    }

    // Autres joueurs dans cette ville (pas soi-même, reconnu à son pseudo ou à son personnage).
    val positions by NetworkSessionManager.positionsVille.collectAsState()
    val monPseudo by NetworkSessionManager.nomJoueurReseau.collectAsState()
    val characters by GameState.characters.collectAsState()
    val personnage = characters.firstOrNull { it.id == characterId }
    val autres = positions.filter { p ->
        p.villeId == ville.id &&
            !(monPseudo != null && p.joueur == monPseudo) &&
            !(personnage != null && p.personnage == personnage.name)
    }
    fun presentsA(l: LieuVisitable) = autres.filter { it.lieuId == l.id }

    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var ouvert by remember { mutableStateOf(false) }
    var lieuOuvert by remember { mutableStateOf<LieuVisitable?>(null) }
    var carteOuverte by remember { mutableStateOf(false) }
    // Carte de la ville : son image, envoyée par le MJ (VilleImageStore, synchronisée en partie réseau).
    val versionImageVille by com.jc2.jdrcompagnon.feature_carte.data.VilleImageStore.version.collectAsState()
    val fichierCarteVille = remember(ville.id, versionImageVille) {
        com.jc2.jdrcompagnon.feature_carte.data.VilleImageStore.fichier(context, ville.id).takeIf { it.exists() }
    }

    NavigationDrawerItem(
        icon = { Icon(Icons.Default.LocationCity, null, tint = ForcedDarkPalette.AccentGold) },
        label = {
            Column {
                Text(ville.nom, color = ForcedDarkPalette.Content, fontWeight = FontWeight.Bold)
                Text(
                    "Vous êtes : ${lieuActuel.nom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ForcedDarkPalette.Content,
                )
                autres.forEach { p ->
                    Text(
                        "${com.jc2.jdrcompagnon.feature_boutique.ui.nomPresent(p)} → ${p.lieuNom.ifBlank { com.jc2.jdrcompagnon.feature_carte.ui.ENTREE_DE_LA_VILLE }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForcedDarkPalette.Content,
                    )
                }
            }
        },
        selected = false,
        onClick = { ouvert = true },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedContainerColor = com.jc2.jdrcompagnon.ui.components.IconesAsset.FondIcones,
            unselectedIconColor = ForcedDarkPalette.Content,
            unselectedTextColor = ForcedDarkPalette.Content
        )
    )

    if (ouvert) {
        AlertDialog(
            onDismissRequest = { ouvert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ville.nom, modifier = Modifier.weight(1f))
                    if (fichierCarteVille != null) {
                        androidx.compose.material3.IconButton(onClick = { carteOuverte = true }) {
                            Icon(Icons.Default.Map, contentDescription = "Carte de la ville", tint = ForcedDarkPalette.AccentGold)
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Où allez-vous ?", style = MaterialTheme.typography.labelMedium)
                    lieux.forEach { l ->
                        LieuVilleRow(
                            nom = (l.notable?.fonction?.takeIf { it != com.jc2.jdrcompagnon.feature_carte.domain.model.FonctionLieu.AUCUNE }?.let { "${it.emoji} " } ?: "") + l.nom,
                            description = l.description.ifBlank { l.boutique?.let { b -> "${b.type} · ${b.marchand}" }.orEmpty() },
                            ici = l.id == lieuActuel.id,
                            commerce = l.boutique != null,
                            image = l.boutique?.image,
                            presents = presentsA(l).map { com.jc2.jdrcompagnon.feature_boutique.ui.nomPresent(it) },
                        ) {
                            com.jc2.jdrcompagnon.feature_carte.data.VisiteVille.aller(context, ville.id, l.id)
                            lieuActuelId = l.id
                            // L'entrée de la ville n'a d'écran que si elle a une description à lire.
                            if (l.id != null || l.description.isNotBlank()) lieuOuvert = l
                        }
                    }
                    if (lieux.size == 1) {
                        Text("Le MJ n'a pas encore indiqué d'autre lieu à visiter.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { ouvert = false }) { Text("Fermer") } },
        )
    }

    if (carteOuverte && fichierCarteVille != null) {
        CarteVilleDialog(ville.nom, fichierCarteVille) { carteOuverte = false }
    }

    lieuOuvert?.let { l ->
        // Données à jour (stock, caisse, services) : la boutique est relue à chaque diffusion.
        val boutique = l.boutique?.let { b -> boutiques.firstOrNull { it.id == b.id } }
        com.jc2.jdrcompagnon.feature_boutique.ui.CommerceLieuDialog(
            lieu = l.nom,
            description = l.description,
            boutique = boutique,
            personnage = personnage,
            presents = presentsA(l),
            onUtiliserService = { b, service ->
                val id = personnage?.id ?: return@CommerceLieuDialog
                // Paiement sur la fiche (relayée au MJ en partie réseau), puis place décomptée.
                GameState.payer(id, service.prixEnPc)
                if (connecte) {
                    NetworkSessionManager.signalerServiceUtilise(id, b.id, service.nom)
                } else {
                    scope.launch {
                        com.jc2.jdrcompagnon.feature_boutique.domain.usecase.UtiliserServiceUseCase.decompter(b.id, service.nom)
                    }
                }
            },
            onDismiss = { lieuOuvert = null },
            fonction = l.notable?.takeIf { it.fonction != com.jc2.jdrcompagnon.feature_carte.domain.model.FonctionLieu.AUCUNE }?.let { n ->
                { com.jc2.jdrcompagnon.feature_carte.ui.FonctionLieuJoueur(n, personnage, campagneId) }
            },
        )
    }
}

/**
 * Lieu à visiter d'une ville dans la liste du menu joueur ; [ici] = lieu actuel du joueur,
 * [commerce] = on peut y acheter ou vendre, [presents] = autres joueurs qui s'y trouvent.
 */
@Composable
private fun LieuVilleRow(
    nom: String,
    description: String,
    ici: Boolean,
    commerce: Boolean = false,
    // Enseigne de la boutique du lieu (affichée à la place de l'icône générique quand elle est disponible).
    image: String? = null,
    presents: List<String> = emptyList(),
    onAller: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = onAller,
        shape = MaterialTheme.shapes.medium,
        color = if (ici) ForcedDarkPalette.AccentGold.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            val teinte = if (ici) ForcedDarkPalette.AccentGold else MaterialTheme.colorScheme.onSurface
            if (commerce) {
                com.jc2.jdrcompagnon.feature_boutique.ui.EnseigneBoutique(image, Modifier.size(36.dp), tint = teinte)
            } else {
                Icon(Icons.Default.Place, contentDescription = null, tint = teinte, modifier = Modifier.size(36.dp).padding(8.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nom, fontWeight = FontWeight.Bold)
                if (description.isNotBlank()) {
                    Text(description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                }
                if (presents.isNotEmpty()) {
                    Text(
                        "Ici : ${presents.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForcedDarkPalette.AccentGold,
                    )
                }
            }
            if (ici) Text("Vous êtes ici", style = MaterialTheme.typography.labelSmall, color = ForcedDarkPalette.AccentGold)
        }
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
/** Carte (image) de la ville en plein écran, agrandissable en pinçant et déplaçable au doigt. */
@Composable
private fun CarteVilleDialog(nomVille: String, fichier: java.io.File, onDismiss: () -> Unit) {
    val image = com.jc2.jdrcompagnon.ui.rememberImageFichier(fichier, maxDimension = 2400)
    var echelle by remember { mutableStateOf(1f) }
    var decalage by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.Black) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, deplacement, zoom, _ ->
                            echelle = (echelle * zoom).coerceIn(1f, 6f)
                            decalage = if (echelle == 1f) androidx.compose.ui.geometry.Offset.Zero else decalage + deplacement
                        }
                    },
            ) {
                if (image != null) {
                    androidx.compose.foundation.Image(
                        bitmap = image,
                        contentDescription = "Carte de $nomVille",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = echelle
                                scaleY = echelle
                                translationX = decalage.x
                                translationY = decalage.y
                            },
                    )
                } else {
                    Text("Chargement de la carte…", color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.align(Alignment.Center))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopStart).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(nomVille, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    androidx.compose.material3.IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer la carte", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }
        }
    }
}
