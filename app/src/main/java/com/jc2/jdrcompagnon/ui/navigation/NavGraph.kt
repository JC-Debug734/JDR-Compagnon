package com.jc2.jdrcompagnon.ui.navigation

import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.network.SessionRole
import com.jc2.jdrcompagnon.ui.AppRole
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.AppBottomBar
import com.jc2.jdrcompagnon.ui.components.DiceOverlay
import com.jc2.jdrcompagnon.ui.components.HostNetworkOverlay
import com.jc2.jdrcompagnon.ui.components.PlayerNetworkOverlay
import com.jc2.jdrcompagnon.ui.components.JoueurDrawer
import com.jc2.jdrcompagnon.ui.components.MjDrawer
import com.jc2.jdrcompagnon.ui.screens.RoleSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.SettingsScreen
import com.jc2.jdrcompagnon.ui.screens.ProposalStatusScreen
import kotlinx.coroutines.launch
import com.jc2.jdrcompagnon.ui.tools.LanToolsScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterSheetScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.JoueurHomeScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterCreationScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterEditScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.RestScreen
import com.jc2.jdrcompagnon.ui.components.ScenarioClockScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MjHomeScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MjCharacterCreationScreen
import com.jc2.jdrcompagnon.ui.screens.mj.ScenariosScreen
import com.jc2.jdrcompagnon.ui.screens.mj.CampaignListScreen
import com.jc2.jdrcompagnon.ui.screens.mj.CampaignEditorScreen
import com.jc2.jdrcompagnon.ui.screens.mj.CampaignOverviewScreen
import com.jc2.jdrcompagnon.feature_group.ui.GroupsScreen
import com.jc2.jdrcompagnon.feature_group.ui.GroupDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MusicScreen
import com.jc2.jdrcompagnon.ui.screens.WorldSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.BestiaryDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.EquipmentDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.LibraryScreen
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioEditorScreen
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioReaderScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.SpellDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.SrdSectionDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.di.BoutiqueDetailViewModelFactory
import com.jc2.jdrcompagnon.di.BoutiqueViewModelFactory
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueDetailViewModel
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueViewModel
import com.jc2.jdrcompagnon.feature_boutique.ui.BoutiqueDetailScreen
import com.jc2.jdrcompagnon.feature_boutique.ui.BoutiqueListScreen
import com.jc2.jdrcompagnon.di.EnvironmentDetailViewModelFactory
import com.jc2.jdrcompagnon.di.EnvironmentViewModelFactory
import com.jc2.jdrcompagnon.feature_environnement.presentation.EnvironmentDetailViewModel
import com.jc2.jdrcompagnon.feature_environnement.presentation.EnvironmentViewModel
import com.jc2.jdrcompagnon.feature_environnement.ui.EnvironmentDetailScreen
import com.jc2.jdrcompagnon.feature_environnement.ui.EnvironmentListScreen
import com.jc2.jdrcompagnon.di.TableAleatoireDetailViewModelFactory
import com.jc2.jdrcompagnon.di.TableAleatoireListViewModelFactory
import com.jc2.jdrcompagnon.feature_table_aleatoire.presentation.TableAleatoireDetailViewModel
import com.jc2.jdrcompagnon.feature_table_aleatoire.presentation.TableAleatoireListViewModel
import com.jc2.jdrcompagnon.feature_table_aleatoire.ui.TableAleatoireDetailScreen
import com.jc2.jdrcompagnon.feature_table_aleatoire.ui.TableAleatoireListScreen
import com.jc2.jdrcompagnon.feature_combat.ui.CombatActionsScreen
import com.jc2.jdrcompagnon.feature_combat.ui.CombatEnCoursScreen
import com.jc2.jdrcompagnon.feature_combat.ui.CombatJoueurScreen
import com.jc2.jdrcompagnon.feature_combat.presentation.PhaseCombat
import com.jc2.jdrcompagnon.feature_carte.ui.CarteCampagneScreen
import com.jc2.jdrcompagnon.feature_carte.ui.EvenementsAleatoiresScreen
import com.jc2.jdrcompagnon.feature_carte.ui.VilleDetailScreen
import com.jc2.jdrcompagnon.feature_carte.ui.VillesListScreen
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Route "maison" courante selon le rôle actif : point d'ancrage utilisé pour que les
 * écrans persistants (bibliothèque, musique, campagnes, groupes) retrouvent leur état
 * (onglet, scroll, position dans un livre...) quand on les quitte puis qu'on y revient,
 * quel que soit l'écran par lequel on est passé entre-temps.
 */
private fun currentHomeRoute(appRole: AppRole?): String = when (appRole) {
    AppRole.MJ -> Route.MjHome.path
    AppRole.JOUEUR -> {
        val lastCharacterId = GameState.selectedCharacterId.value
        val hasLastCharacter = lastCharacterId != null &&
                GameState.characters.value.any { it.id == lastCharacterId }
        if (hasLastCharacter) "${Route.CharacterSheet.path}/$lastCharacterId?isMj=false" else Route.JoueurHome.path
    }
    null -> Route.RoleSelection.path
}

@Composable
fun JdrNavGraph() {
    val navController = rememberNavController()

    // Liens internes ouverts hors d'un écran (ex. profil ou combat d'un événement, voir
    // DemandesLienInterne).
    val demandeLienInterne by DemandesLienInterne.demande.collectAsState()
    LaunchedEffect(demandeLienInterne) {
        demandeLienInterne?.let { (type, nom) ->
            DemandesLienInterne.consommer()
            navigateToInternalLink(navController, type, nom)
        }
    }
    val currentWorld by GameState.currentWorld.collectAsState()
    val appRole by GameState.appRole.collectAsState()
    val playerName by GameState.playerName.collectAsState()
    val roleChangeRequested by GameState.roleChangeRequested.collectAsState()
    val networkRole by NetworkSessionManager.role.collectAsState()
    val claimedCharacterId by NetworkSessionManager.claimedCharacterId.collectAsState()
    // Une fois un personnage réservé/accepté côté réseau, le joueur y est
    // assigné pour la durée de la session : plus de retour à la liste de
    // sélection tant qu'il reste connecté avec ce rôle.
    val isLockedToNetworkCharacter = networkRole == SessionRole.PLAYER && claimedCharacterId != null

    LaunchedEffect(claimedCharacterId) {
        val id = claimedCharacterId
        if (id != null && networkRole == SessionRole.PLAYER) {
            GameState.selectCharacter(id)
            navController.navigate("${Route.CharacterSheet.path}/$id?isMj=false") {
                popUpTo(Route.JoueurHome.path) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    // Combat en réseau : le joueur est amené sur son écran de combat à chaque ouverture d'une
    // phase de déclaration (début du combat, puis chaque nouveau round) pour choisir son action.
    val combatJoueur by NetworkSessionManager.combatEnCours.collectAsState()
    val cleDeclaration = combatJoueur
        ?.takeIf { it.phase == PhaseCombat.DECLARATION.name && it.monCombattantId != null }
        ?.let { "${it.id}-${it.round}" }
    LaunchedEffect(cleDeclaration) {
        if (cleDeclaration != null && networkRole == SessionRole.PLAYER) {
            navController.navigate(Route.CombatJoueur.path) { launchSingleTop = true }
        }
    }
    // Tiroir latéral global : câblé une seule fois ici (comme AppBottomBar et
    // DiceOverlay) plutôt que réimplémenté par chaque écran, pour que le menu
    // (hamburger, pas de flèche retour) et son contenu MJ/Joueur soient
    // strictement identiques quel que soit l'écran d'où on l'ouvre.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()

    LaunchedEffect(roleChangeRequested) {
        if (roleChangeRequested) {
            // Seul chemin de retour vers le choix de rôle : on vide toute la
            // pile pour repartir sur une base propre.
            navController.navigate(Route.RoleSelection.path) {
                // popUpTo(0) ne correspond à aucune destination réelle en Navigation
                // Compose (les ids sont dérivés d'un hash de route) : le pop était un
                // no-op silencieux qui laissait les anciens écrans (dont un éventuel
                // écran de connexion réseau) empilés sous RoleSelectionScreen, prêts à
                // ressurgir au retour arrière. On vide la pile jusqu'à la racine du graphe.
                popUpTo(navController.graph.id) { inclusive = true }
            }
            GameState.consumeRoleChangeRequest()
        }
    }

    // Effets à durée (potions, sorts) : retirés des fiches dès que l'horloge de scénario dépasse
    // leur fin. Collecte directe du flux (pas de collectAsState) pour ne pas recomposer tout le
    // graphe à chaque seconde.
    LaunchedEffect(Unit) {
        com.jc2.jdrcompagnon.ui.ScenarioClockState.state.collect { horloge ->
            GameState.purgerEffetsExpires(com.jc2.jdrcompagnon.ui.ScenarioClockState.totalSeconds(horloge))
        }
    }

    // Côté MJ : les PV et états d'une fiche changés hors du combat (potion bue sur l'appareil d'un joueur,
    // fiche modifiée) sont reportés sur le combattant correspondant.
    LaunchedEffect(Unit) {
        GameState.characters.collect { personnages ->
            com.jc2.jdrcompagnon.feature_combat.presentation.CombatSession.synchroniserFiches(personnages)
        }
    }

    // Livres propres à une campagne (PorteeCampagne) : changer de campagne, ou modifier la liste
    // des livres d'une campagne, change le contenu personnalisé visible — caches SRD vidés.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.flow.combine(GameState.currentCampaignId, GameState.mjCampaigns) { courante, campagnes ->
            courante to campagnes.map { it.id to it.livreIds }
        }
            .distinctUntilChanged()
            .collect { com.jc2.jdrcompagnon.ui.PorteeCampagne.livresChanges() }
    }

    // Anciens boutons d'import (Bibliothèque, Scénarios, Campagnes, Musique) : ouvrent l'outil IMPORT.
    val importDemande by com.jc2.jdrcompagnon.feature_import.ImportNavigation.demande.collectAsState()
    LaunchedEffect(importDemande) {
        if (importDemande) {
            navController.navigate(Route.Import.path) { launchSingleTop = true }
            com.jc2.jdrcompagnon.feature_import.ImportNavigation.consommer()
        }
    }

    // Halte d'un voyage : le joueur ouvre l'écran de repos pour choisir son repos.
    val reposDemande by com.jc2.jdrcompagnon.ui.screens.joueur.ReposNavigation.demande.collectAsState()
    LaunchedEffect(reposDemande) {
        if (reposDemande) {
            navController.navigate(Route.Repos.path) { launchSingleTop = true }
            com.jc2.jdrcompagnon.ui.screens.joueur.ReposNavigation.consommer()
        }
    }

    // Composé seulement à l'ouverture du tiroir : enregistré en dernier, il passe devant les
    // BackHandler des écrans (lecteur de scénario, accueil MJ) tant que le tiroir est ouvert.
    if (drawerState.isOpen) {
        BackHandler {
            drawerScope.launch { drawerState.close() }
        }
    }

    // Menu combat / dialogue / fiche des profils d'une scène (menu latéral MJ, lecteur).
    com.jc2.jdrcompagnon.ui.screens.mj.scenario.SceneProfileActionsHost(
        onOpenInternalLink = { type, name -> navigateToInternalLink(navController, type, name) }
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            when (appRole) {
                AppRole.MJ -> MjDrawer(
                    currentWorld = currentWorld,
                    onOpenAccueil = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.MjHome.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenBoutiques = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Boutiques.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenEnvironnements = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Environnements.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenTableAleatoire = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.TablesAleatoires.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCombatActions = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.CombatActions.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Settings.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenProposalStatus = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.ProposalStatus.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenEtat = { nom ->
                        navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "etat").replace("{entryName}", android.net.Uri.encode(nom)))
                    },
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                )
                AppRole.JOUEUR -> JoueurDrawer(
                    currentWorld = currentWorld,
                    onOpenAccueil = {
                        drawerScope.launch { drawerState.close() }
                        val homeRoute = currentHomeRoute(appRole)
                        navController.navigate(homeRoute) {
                            launchSingleTop = true
                        }
                    },
                    onChooseCharacter = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.CharacterSelection.path + "?isMj=false") {
                            launchSingleTop = true
                        }
                    },
                    onOpenSettings = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Settings.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenVilles = { campagneId ->
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Villes.path.replace("{campagneId}", campagneId))
                    },
                    onOpenCarte = { campagneId ->
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.CarteCampagne.creer(campagneId))
                    },
                    onOpenRepos = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Repos.path)
                    },
                    onOpenQuetes = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.PlayerQuests.path) { launchSingleTop = true }
                    },
                    onOpenGroupe = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.PlayerGroup.path) { launchSingleTop = true }
                    },
                    onOpenCombatActions = {
                        drawerScope.launch { drawerState.close() }
                        // Pendant un combat en réseau, l'entrée combat mène à l'écran du combat en cours.
                        val route = if (combatJoueur != null) Route.CombatJoueur.path else Route.CombatActions.path
                        navController.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenProposalStatus = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.ProposalStatus.path) {
                            launchSingleTop = true
                        }
                    },
                    onClose = { drawerScope.launch { drawerState.close() } },
                    characterLocked = isLockedToNetworkCharacter,
                )
                null -> Unit // Pas de rôle choisi : pas de tiroir pertinent (choix de rôle, sélection de monde).
            }
        }
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Route.RoleSelection.path,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
        ) {
            composable(Route.RoleSelection.path) {
                RoleSelectionScreen(
                    currentWorld = currentWorld,
                    playerName = playerName,
                    onNameConfirmed = { name -> GameState.setPlayerName(name) },
                    onGenerateRandomName = { GameState.assignRandomPlayerName() },
                    onSelectMj = {
                        GameState.setAppRole(AppRole.MJ)
                        navController.navigate(Route.MjHome.path) {
                            // Retire l'écran de choix de rôle de la pile : le retour
                            // arrière système ne doit jamais pouvoir y ramener.
                            popUpTo(Route.RoleSelection.path) { inclusive = true }
                        }
                    },
                    onSelectJoueur = {
                        GameState.setAppRole(AppRole.JOUEUR)
                        val lastCharacterId = GameState.selectedCharacterId.value
                        val hasLastCharacter = lastCharacterId != null &&
                                GameState.characters.value.any { it.id == lastCharacterId }
                        val destination = if (hasLastCharacter) {
                            "${Route.CharacterSheet.path}/$lastCharacterId?isMj=false"
                        } else {
                            Route.JoueurHome.path
                        }
                        navController.navigate(destination) {
                            popUpTo(Route.RoleSelection.path) { inclusive = true }
                        }
                    },
                    onSelectContext = { navController.navigate(Route.WorldSelection.path) },
                    onSelectSettings = { navController.navigate(Route.Settings.path) }
                )
            }

            composable(Route.LanTools.path) {
                LanToolsScreen()
            }

            composable(Route.LanJoin.path) {
                LanToolsScreen(
                    startInJoinMode = true,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(Route.MjHome.path) {
                MjHomeScreen(
                    currentWorld = currentWorld,
                    onCreateCharacter = { navController.navigate(Route.MjCharacterCreation.path) },
                    onViewCharacters = { navController.navigate(Route.CharacterSelection.path + "?isMj=true") },
                    onOpenScenarioEditor = { scenarioId ->
                        navController.navigate(Route.ScenarioEditor.path.replace("{scenarioId}", scenarioId ?: "new"))
                    },
                    onOpenScenarios = {
                        navController.navigate(Route.Scenarios.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCampaigns = {
                        navController.navigate(Route.Campaigns.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCampaign = { campaignId ->
                        navController.navigate(Route.CampaignOverview.path.replace("{campaignId}", campaignId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenGroups = {
                        navController.navigate(Route.Groups.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenBoutiques = {
                        navController.navigate(Route.Boutiques.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenEnvironnements = {
                        navController.navigate(Route.Environnements.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCombatActions = {
                        navController.navigate(Route.CombatActions.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenTableAleatoire = {
                        navController.navigate(Route.TablesAleatoires.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenEpreuves = {
                        navController.navigate(Route.Epreuves.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenEvenements = {
                        navController.navigate(Route.BibliothequeEvenements.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenMusic = {
                        navController.navigate(Route.Music.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenClock = {
                        navController.navigate(Route.Horloge.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenImport = {
                        navController.navigate(Route.Import.path) {
                            launchSingleTop = true
                        }
                    },
                    onOpenInternalLink = { type, name ->
                        navigateToInternalLink(navController, type, name)
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.LanHost.path + "?groupId={groupId}&campaignTitle={campaignTitle}",
                arguments = listOf(
                    navArgument("groupId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("campaignTitle") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId")
                val campaignTitle = backStackEntry.arguments?.getString("campaignTitle")
                LanToolsScreen(
                    startInHostMode = true,
                    groupId = groupId,
                    campaignTitle = campaignTitle,
                    onExit = { navController.popBackStack() }
                )
            }

            composable(
                route = Route.MjCharacterCreation.path + "?type={type}",
                arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "PJ" })
            ) { backStackEntry ->
                MjCharacterCreationScreen(
                    currentWorld = currentWorld,
                    initialType = backStackEntry.arguments?.getString("type") ?: "PJ",
                    onCharacterCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.JoueurHome.path) {
                JoueurHomeScreen(
                    currentWorld = currentWorld,
                    onChooseCharacter = { navController.navigate(Route.CharacterSelection.path + "?isMj=false") },
                    onCreateCharacter = { navController.navigate(Route.CharacterCreation.path) },
                    onBack = { navController.popBackStack() },
                    onViewCharacter = { character ->
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}") {
                            launchSingleTop = true
                        }
                    },
                    onOpenMusic = {
                        navController.navigate(Route.Music.path) {
                            launchSingleTop = true
                        }
                    },
                    onSelectWorld = { navController.navigate(Route.WorldSelection.path) },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.CharacterSelection.path + "?isMj={isMj}",
                arguments = listOf(navArgument("isMj") { type = NavType.BoolType; defaultValue = false })
            ) { backStackEntry ->
                val isMj = backStackEntry.arguments?.getBoolean("isMj") ?: false
                val currentWorld by GameState.currentWorld.collectAsState()
                CharacterSelectionScreen(
                    currentWorld = currentWorld,
                    onViewCharacter = { character ->
                        // Ne mémoriser "le personnage à reprendre" que côté Joueur : en
                        // mode MJ, consulter une fiche (PJ, PNJ, boss...) ne doit pas
                        // écraser le dernier personnage réellement joué, sinon un passage
                        // en rôle Joueur juste après rouvre la mauvaise fiche.
                        if (!isMj) GameState.selectCharacter(character.id)
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}?isMj=$isMj") {
                            launchSingleTop = true
                        }
                    },
                    onCreateQuick = { type -> navController.navigate(Route.MjCharacterCreation.path + "?type=$type") },
                    onCreateWizard = { navController.navigate(Route.CharacterCreation.path + "?isMj=true") },
                    // PNJ créé depuis un profil : ouvrir directement sa fiche pour l'ajuster.
                    onPnjCreated = { pnj ->
                        navController.navigate("${Route.CharacterSheet.path}/${pnj.id}?isMj=true") {
                            launchSingleTop = true
                        }
                    },
                    onImportCharacter = { importe ->
                        val nouveauPersonnage = importe.character.copy(
                            id = java.util.UUID.randomUUID().toString(),
                            worldId = currentWorld?.id ?: "donjon_et_dragon",
                            createdBy = if (isMj) "MJ" else "Joueur"
                        )
                        GameState.addCharacter(nouveauPersonnage, historiqueImporte = importe.historique)
                    },
                    isMjMode = isMj,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = "${Route.CharacterSheet.path}/{characterId}?isMj={isMj}",
                arguments = listOf(
                    navArgument("characterId") { type = NavType.StringType },
                    navArgument("isMj") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                val isMj = backStackEntry.arguments?.getBoolean("isMj") ?: false
                val characters by GameState.characters.collectAsState()
                val character = characters.firstOrNull { it.id == characterId }
                character?.let {
                    CharacterSheetScreen(
                        character = it,
                        isMjMode = isMj,
                        onBack = { navController.popBackStack() },
                        onEdit = { character ->
                            navController.navigate("${Route.CharacterEdit.path}/${character.id}")
                        },
                        onChooseCharacter = {
                            navController.navigate(Route.CharacterSelection.path + "?isMj=false")
                        },
                        onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    )
                }
            }

            composable(
                route = "${Route.CharacterEdit.path}/{characterId}",
                arguments = listOf(navArgument("characterId") { type = NavType.StringType })
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                val characters by GameState.characters.collectAsState()
                val character = characters.firstOrNull { it.id == characterId }
                character?.let {
                    CharacterEditScreen(
                        character = it,
                        onSave = { updated ->
                            GameState.updateCharacter(updated)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() },
                        onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    )
                }
            }

            composable(
                route = Route.CharacterCreation.path + "?isMj={isMj}",
                arguments = listOf(navArgument("isMj") { type = NavType.BoolType; defaultValue = false })
            ) { backStackEntry ->
                val isMj = backStackEntry.arguments?.getBoolean("isMj") ?: false
                CharacterCreationScreen(
                    currentWorld = currentWorld,
                    isMjMode = isMj,
                    onCharacterCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = "${Route.WorldSelection.path}/{nextDestination}",
                arguments = listOf(navArgument("nextDestination") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val nextDestination = backStackEntry.arguments?.getString("nextDestination") ?: ""
                WorldSelectionScreen(
                    onWorldSelected = { id, label, description, _, _, _ ->
                        GameState.selectWorld(
                            com.jc2.jdrcompagnon.ui.WorldState(
                                id = id,
                                name = label,
                                description = description,
                            )
                        )
                        if (nextDestination.isNotBlank()) {
                            navController.navigate(nextDestination)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onCustomWorldSelected = { world ->
                        GameState.selectWorld(world)
                        if (nextDestination.isNotBlank()) {
                            navController.navigate(nextDestination)
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.WorldSelection.path) {
                WorldSelectionScreen(
                    onWorldSelected = { id, label, description, _, _, _ ->
                        GameState.selectWorld(
                            com.jc2.jdrcompagnon.ui.WorldState(
                                id = id,
                                name = label,
                                description = description,
                            )
                        )
                        navController.popBackStack()
                    },
                    onCustomWorldSelected = { world ->
                        GameState.selectWorld(world)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Scenarios.path) {
                ScenariosScreen(
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenScenarioEditor = { scenarioId ->
                        navController.navigate(Route.ScenarioEditor.path.replace("{scenarioId}", scenarioId ?: "new"))
                    },
                    onOpenScenarioReader = { scenarioId ->
                        navController.navigate(Route.ScenarioReader.path.replace("{scenarioId}", scenarioId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.ScenarioEditor.path,
                arguments = listOf(navArgument("scenarioId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val scenarioId = backStackEntry.arguments?.getString("scenarioId")
                ScenarioEditorScreen(
                    scenarioId = if (scenarioId == "new") null else scenarioId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    onOpenInternalLink = { type, name ->
                        navigateToInternalLink(navController, type, name)
                    },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.ScenarioReader.path,
                arguments = listOf(navArgument("scenarioId") { type = NavType.StringType })
            ) { backStackEntry ->
                val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: ""
                ScenarioReaderScreen(
                    scenarioId = scenarioId,
                    onBack = { navController.popBackStack() },
                    onOpenInternalLink = { type, name ->
                        navigateToInternalLink(navController, type, name)
                    },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.Library.path,
                arguments = listOf(navArgument("initialTab") { type = NavType.StringType; defaultValue = "monsters" })
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments?.getString("initialTab") ?: "monsters"
                LibraryScreen(
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    initialTab = initialTab,
                    onMonsterClick = { name: String -> navController.navigate(Route.BestiaryDetail.path.replace("{monsterName}", name)) },
                    onSpellClick = { name: String -> navController.navigate(Route.SpellDetail.path.replace("{spellName}", name)) },
                    onEquipmentClick = { item: EquipmentItem -> navController.navigate(Route.EquipmentDetail.path.replace("{equipmentName}", item.name)) },
                    onClasseClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "classe").replace("{entryName}", name)) },
                    onEspeceClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "espece").replace("{entryName}", name)) },
                    onHistoriqueClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "historique").replace("{entryName}", name)) },
                    onDonClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "don").replace("{entryName}", name)) },
                    onArmeArmureMagiqueClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "arme_magique").replace("{entryName}", name)) },
                    // Noms de règles encodés : certains titres du SRD contiennent "/" ou "?".
                    onRuleClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "regle").replace("{entryName}", android.net.Uri.encode(name))) },
                    onGlossaryClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "glossaire").replace("{entryName}", android.net.Uri.encode(name))) },
                    onEtatClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "etat").replace("{entryName}", android.net.Uri.encode(name))) },
                )
            }

            composable(
                route = Route.SrdSectionDetail.path,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("entryName") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val kind = backStackEntry.arguments?.getString("kind") ?: ""
                val entryName = backStackEntry.arguments?.getString("entryName")
                SrdSectionDetailScreen(
                    kind = kind,
                    entryName = entryName,
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.BestiaryDetail.path,
                arguments = listOf(navArgument("monsterName") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val monsterName = backStackEntry.arguments?.getString("monsterName")
                BestiaryDetailScreen(
                    monsterName = monsterName,
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.SpellDetail.path,
                arguments = listOf(navArgument("spellName") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val spellName = backStackEntry.arguments?.getString("spellName")
                SpellDetailScreen(
                    spellName = spellName,
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.EquipmentDetail.path,
                arguments = listOf(navArgument("equipmentName") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val equipmentName = backStackEntry.arguments?.getString("equipmentName")
                EquipmentDetailScreen(
                    equipmentName = equipmentName,
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Music.path) {
                MusicScreen(
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Import.path) {
                com.jc2.jdrcompagnon.feature_import.ImportScreen(
                    currentWorld = currentWorld,
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Groups.path) {
                GroupsScreen(
                    currentWorld = currentWorld,
                    onOpenGroupDetail = { groupId ->
                        navController.navigate(Route.GroupDetail.path.replace("{groupId}", groupId))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.GroupDetail.path,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupDetailScreen(
                    groupId = groupId,
                    onOpenBestiaryDetail = { monsterName ->
                        navController.navigate(Route.BestiaryDetail.path.replace("{monsterName}", monsterName))
                    },
                    onOpenEquipmentDetail = { equipmentName ->
                        navController.navigate(Route.EquipmentDetail.path.replace("{equipmentName}", equipmentName))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenCharacter = { character ->
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}?isMj=true") {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Route.Boutiques.path) {
                val boutiqueViewModel: BoutiqueViewModel = viewModel(factory = BoutiqueViewModelFactory())
                BoutiqueListScreen(
                    viewModel = boutiqueViewModel,
                    onBoutiqueClick = { boutiqueId ->
                        navController.navigate(Route.BoutiqueDetail.path.replace("{boutiqueId}", boutiqueId))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.BoutiqueDetail.path,
                arguments = listOf(navArgument("boutiqueId") { type = NavType.StringType })
            ) { backStackEntry ->
                val boutiqueId = backStackEntry.arguments?.getString("boutiqueId") ?: return@composable
                val boutiqueDetailViewModel: BoutiqueDetailViewModel =
                    viewModel(factory = BoutiqueDetailViewModelFactory(boutiqueId))
                BoutiqueDetailScreen(
                    viewModel = boutiqueDetailViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Environnements.path) {
                val environmentViewModel: EnvironmentViewModel = viewModel(factory = EnvironmentViewModelFactory())
                EnvironmentListScreen(
                    viewModel = environmentViewModel,
                    worldId = currentWorld?.id ?: "donjon_et_dragon",
                    onOpenDetail = { environnementId ->
                        navController.navigate(Route.EnvironnementDetail.path.replace("{environnementId}", environnementId))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.EnvironnementDetail.path,
                arguments = listOf(navArgument("environnementId") { type = NavType.StringType })
            ) { backStackEntry ->
                val environnementId = backStackEntry.arguments?.getString("environnementId") ?: return@composable
                val environmentDetailViewModel: EnvironmentDetailViewModel =
                    viewModel(factory = EnvironmentDetailViewModelFactory(environnementId))
                EnvironmentDetailScreen(
                    viewModel = environmentDetailViewModel,
                    worldId = currentWorld?.id,
                    onOpenBestiaryDetail = { monsterName ->
                        navController.navigate(Route.BestiaryDetail.path.replace("{monsterName}", monsterName))
                    },
                    onOpenEquipmentDetail = { equipmentName ->
                        navController.navigate(Route.EquipmentDetail.path.replace("{equipmentName}", equipmentName))
                    },
                    onOpenTableAleatoire = { tableId ->
                        navController.navigate(Route.TableAleatoireDetail.path.replace("{tableId}", tableId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Route.TablesAleatoires.path) {
                val tableAleatoireViewModel: TableAleatoireListViewModel =
                    viewModel(factory = TableAleatoireListViewModelFactory())
                TableAleatoireListScreen(
                    viewModel = tableAleatoireViewModel,
                    worldId = currentWorld?.id ?: "donjon_et_dragon",
                    onOpenDetail = { tableId ->
                        navController.navigate(Route.TableAleatoireDetail.path.replace("{tableId}", tableId))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.TableAleatoireDetail.path,
                arguments = listOf(navArgument("tableId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tableId = backStackEntry.arguments?.getString("tableId") ?: return@composable
                val tableAleatoireDetailViewModel: TableAleatoireDetailViewModel =
                    viewModel(factory = TableAleatoireDetailViewModelFactory(tableId))
                TableAleatoireDetailScreen(
                    viewModel = tableAleatoireDetailViewModel,
                    worldId = currentWorld?.id,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Route.BibliothequeEvenements.path) {
                val evenementListViewModel: com.jc2.jdrcompagnon.feature_evenement.presentation.EvenementListViewModel =
                    viewModel(factory = com.jc2.jdrcompagnon.di.EvenementListViewModelFactory())
                com.jc2.jdrcompagnon.feature_evenement.ui.EvenementListScreen(
                    viewModel = evenementListViewModel,
                    worldId = currentWorld?.id ?: "donjon_et_dragon",
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.CombatActions.path) {
                CombatActionsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    onSimuler = { personnageId ->
                        navController.navigate(Route.SimulationCombat.path.replace("{personnageId}", personnageId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(
                route = Route.SimulationCombat.path,
                arguments = listOf(navArgument("personnageId") { type = NavType.StringType })
            ) { backStackEntry ->
                com.jc2.jdrcompagnon.feature_combat.ui.SimulationCombatScreen(
                    personnageId = backStackEntry.arguments?.getString("personnageId") ?: "",
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.CombatEnCours.path) {
                CombatEnCoursScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.CombatJoueur.path) {
                CombatJoueurScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Campaigns.path) {
                CampaignListScreen(
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenCampaignEditor = { campaignId ->
                        navController.navigate(Route.CampaignEditor.path.replace("{campaignId}", campaignId ?: "new"))
                    },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.CampaignOverview.path,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId") ?: return@composable
                CampaignOverviewScreen(
                    campaignId = campaignId,
                    onBack = { navController.popBackStack() },
                    onEditCampaign = { id -> navController.navigate(Route.CampaignEditor.path.replace("{campaignId}", id)) },
                    onOpenCarte = { id, carteId -> navController.navigate(Route.CarteCampagne.creer(id, carteId)) },
                    onOpenScenario = { scenarioId ->
                        // Devient le scénario en cours (repris ensuite depuis la page de campagne).
                        GameState.setLastScenarioId(scenarioId)
                        navController.navigate(Route.ScenarioReader.path.replace("{scenarioId}", scenarioId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenMonster = { name -> navController.navigate(Route.BestiaryDetail.path.replace("{monsterName}", name)) },
                )
            }

            composable(
                route = Route.CampaignEditor.path,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId")
                CampaignEditorScreen(
                    campaignId = if (campaignId == "new") null else campaignId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    onOpenCarte = { id, carteId -> navController.navigate(Route.CarteCampagne.creer(id, carteId)) },
                    onOpenEvenements = { id -> navController.navigate(Route.Evenements.path.replace("{campagneId}", id)) },
                    onOpenVilles = { id -> navController.navigate(Route.Villes.path.replace("{campagneId}", id)) },
                    onOpenVilleDetail = { id, villeId ->
                        navController.navigate(Route.VilleDetail.path.replace("{campagneId}", id).replace("{villeId}", villeId))
                    },
                    onOpenBestiaryDetail = { monsterName ->
                        navController.navigate(Route.BestiaryDetail.path.replace("{monsterName}", monsterName))
                    },
                )
            }

            composable(
                route = Route.CarteCampagne.path,
                arguments = listOf(
                    navArgument("campagneId") { type = NavType.StringType },
                    navArgument("carteId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val campagneId = backStackEntry.arguments?.getString("campagneId") ?: return@composable
                CarteCampagneScreen(
                    campagneId = campagneId,
                    carteId = backStackEntry.arguments?.getString("carteId"),
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    readOnly = appRole != AppRole.MJ,
                    onOuvrirCarte = { autreCarteId ->
                        navController.navigate(Route.CarteCampagne.creer(campagneId, autreCarteId)) {
                            popUpTo(Route.CarteCampagne.path) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = Route.Evenements.path,
                arguments = listOf(navArgument("campagneId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campagneId = backStackEntry.arguments?.getString("campagneId") ?: return@composable
                EvenementsAleatoiresScreen(
                    campagneId = campagneId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Epreuves.path) {
                val epreuveListViewModel: com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveListViewModel =
                    viewModel(factory = com.jc2.jdrcompagnon.di.EpreuveListViewModelFactory())
                com.jc2.jdrcompagnon.feature_epreuve.ui.EpreuveListScreen(
                    viewModel = epreuveListViewModel,
                    worldId = currentWorld?.id ?: "donjon_et_dragon",
                    onOuvrirResolution = {
                        navController.navigate(Route.EpreuveResolution.path) { launchSingleTop = true }
                    },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.EpreuveResolution.path) {
                com.jc2.jdrcompagnon.feature_epreuve.ui.EpreuveResolutionScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(
                route = Route.Villes.path,
                arguments = listOf(navArgument("campagneId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campagneId = backStackEntry.arguments?.getString("campagneId") ?: return@composable
                VillesListScreen(
                    campagneId = campagneId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    readOnly = appRole != AppRole.MJ,
                    onOpenVille = { villeId ->
                        navController.navigate(
                            Route.VilleDetail.path.replace("{campagneId}", campagneId).replace("{villeId}", villeId)
                        )
                    },
                )
            }

            composable(
                route = Route.VilleDetail.path,
                arguments = listOf(
                    navArgument("campagneId") { type = NavType.StringType },
                    navArgument("villeId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val campagneId = backStackEntry.arguments?.getString("campagneId") ?: return@composable
                val villeId = backStackEntry.arguments?.getString("villeId") ?: return@composable
                VilleDetailScreen(
                    campagneId = campagneId,
                    villeId = villeId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    readOnly = appRole != AppRole.MJ,
                    onOpenBoutique = { boutiqueId ->
                        navController.navigate(Route.BoutiqueDetail.path.replace("{boutiqueId}", boutiqueId))
                    },
                )
            }

            composable(Route.Repos.path) {
                RestScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Horloge.path) {
                ScenarioClockScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.Settings.path) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Route.PlayerGroup.path) {
                com.jc2.jdrcompagnon.feature_group.ui.PlayerGroupScreen(
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.PlayerQuests.path) {
                com.jc2.jdrcompagnon.feature_quete.ui.PlayerQuestsScreen(
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

            composable(Route.ProposalStatus.path) {
                ProposalStatusScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }
        }

        // Overlay du dé accessible depuis n'importe quel écran
        DiceOverlay(currentWorld = currentWorld)

        // Messages réseau (MJ → joueur) en attente d'une réaction, accessibles depuis n'importe
        // quel écran (briefing PNJ, offre de butin, proposition de groupe, conflit de version...)
        PlayerNetworkOverlay()
        // Pendant MJ : propositions de fiche envoyées par les joueurs.
        HostNetworkOverlay()
        // MJ : déroulé d'un voyage parti (événement de voyage puis repos, à chaque étape).
        if (appRole == AppRole.MJ) com.jc2.jdrcompagnon.feature_carte.ui.VoyageMjOverlay()
        com.jc2.jdrcompagnon.ui.components.HostSocialRollOverlay()

        // Barre de menu globale accessible depuis n'importe quel écran
        // (sauf sur l'écran de sélection de rôle qui affiche ses propres boutons)
        // currentBackStackEntryAsState() (pas currentBackStackEntry brut, qui n'est pas
        // observable en Compose) : sinon la barre ne réapparaissait qu'au hasard d'une
        // recomposition provoquée par autre chose (ex. ouvrir le menu latéral), jamais tout
        // de suite après une navigation.
        val currentRoute by navController.currentBackStackEntryAsState()
        if (appRole != null && currentRoute?.destination?.route != Route.RoleSelection.path) {
            AppBottomBar(
                onNavigateConnection = {
                    // Sans popUpTo/launchSingleTop, chaque tap (y compris depuis l'écran
                    // de connexion lui-même) empilait un nouvel écran LanHost/LanJoin par
                    // dessus le précédent : la navigation semblait "bloquée" dessus car le
                    // retour arrière ne faisait que dépiler des doublons.
                    val destination = when (appRole) {
                        AppRole.MJ -> Route.LanHost.path
                        AppRole.JOUEUR -> Route.LanJoin.path
                        null -> Route.LanTools.path
                    }
                    navController.navigate(destination) {
                        launchSingleTop = true
                    }
                },
                onNavigateLibrary = {
                    navController.navigate(Route.Library.path.replace("{initialTab}", "monsters")) {
                        launchSingleTop = true
                    }
                },
                onHomeTap = {
                    val homeRoute = currentHomeRoute(appRole)
                    // popUpTo(homeRoute) ne fonctionnait pas de façon fiable depuis certains
                    // écrans profonds (fiche de personnage, sélection de personnage...) : quand
                    // homeRoute n'était plus exactement au sommet attendu de la pile, le tap sur
                    // "Accueil" ne déclenchait rien. popUpTo(0) vide toute la pile de navigation
                    // avant de repartir sur l'accueil, ce qui garantit que le bouton fonctionne
                    // depuis n'importe quel écran.
                    navController.navigate(homeRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                    // Si on est déjà sur l'écran d'accueil (ex : en mode lecture de scénario
                    // côté MJ), le navigate() ci-dessus ne déclenche rien : ce flag permet à
                    // l'écran de sortir quand même du mode lecture.
                    GameState.requestHomeReset()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Écran d'exploration (brouillard de la page table), par-dessus tout le reste, barre du
        // bas comprise, depuis n'importe quel écran qui l'ouvre.
        com.jc2.jdrcompagnon.feature_exploration.ExplorationOverlay()
    }
    }
}

private fun navigateToInternalLink(navController: androidx.navigation.NavController, type: String, name: String) {
    if (type == "pnj" || type == "npc") {
        val character = GameState.characters.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
        val route = if (character != null) {
            "${Route.CharacterSheet.path}/${character.id}?isMj=true"
        } else {
            // Repli : le personnage n'a pas été trouvé (nom modifié/supprimé) —
            // on ouvre la liste plutôt que de ne rien faire.
            Route.CharacterSelection.path + "?isMj=true"
        }
        navController.navigate(route) { launchSingleTop = true }
        return
    }
    val route = when (type) {
        "monster" -> Route.BestiaryDetail.path.replace("{monsterName}", name)
        "equipment" -> Route.EquipmentDetail.path.replace("{equipmentName}", name)
        "spell" -> Route.SpellDetail.path.replace("{spellName}", name)
        // Lien #rule: → directement la fiche de la règle (repli sur les grandes sections pour
        // les anciens liens, voir SrdRepository.getRuleEntryByName).
        "rule" -> Route.SrdSectionDetail.path.replace("{kind}", "regle").replace("{entryName}", android.net.Uri.encode(name))
        // Lien de scénario #epreuve: → l'épreuve a déjà été démarrée par le lecteur (session de
        // l'outil ÉPREUVES), il ne reste qu'à ouvrir l'écran de résolution.
        "epreuve" -> Route.EpreuveResolution.path
        // Lien #combat: → même principe : le combat a été préparé par LancerCombatDialog.
        "combat" -> Route.CombatEnCours.path
        else -> null
    }
    route?.let { navController.navigate(it) { launchSingleTop = true } }
}