package com.jc2.jdrcompagnon.ui.navigation

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
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jc2.jdrcompagnon.ui.AppRole
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.AppBottomBar
import com.jc2.jdrcompagnon.ui.components.DiceOverlay
import com.jc2.jdrcompagnon.ui.components.JoueurDrawer
import com.jc2.jdrcompagnon.ui.components.MjDrawer
import com.jc2.jdrcompagnon.ui.screens.RoleSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.SettingsScreen
import kotlinx.coroutines.launch
import com.jc2.jdrcompagnon.ui.tools.LanToolsScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterSheetScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.JoueurHomeScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterCreationScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterEditScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.SpellManagementScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MjHomeScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MjCharacterCreationScreen
import com.jc2.jdrcompagnon.ui.screens.mj.ScenariosScreen
import com.jc2.jdrcompagnon.ui.screens.mj.CampaignListScreen
import com.jc2.jdrcompagnon.ui.screens.mj.CampaignEditorScreen
import com.jc2.jdrcompagnon.feature_group.ui.GroupsScreen
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
fun JdrNavGraph(overrideStartDestination: String = Route.RoleSelection.path) {
    val navController = rememberNavController()
    val currentWorld by GameState.currentWorld.collectAsState()
    val appRole by GameState.appRole.collectAsState()
    val playerName by GameState.playerName.collectAsState()
    val roleChangeRequested by GameState.roleChangeRequested.collectAsState()
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

    BackHandler(enabled = drawerState.isOpen) {
        drawerScope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            when (appRole) {
                AppRole.MJ -> MjDrawer(
                    currentWorld = currentWorld,
                    onOpenAccueil = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.MjHome.path) {
                            popUpTo(Route.MjHome.path) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenCampaigns = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Campaigns.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenScenarioEditor = { scenarioId ->
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.ScenarioEditor.path.replace("{scenarioId}", scenarioId ?: "new"))
                    },
                    onOpenBoutiques = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Boutiques.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenSettings = {
                        drawerScope.launch { drawerState.close() }
                        navController.navigate(Route.Settings.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCloseDrawer = { drawerScope.launch { drawerState.close() } },
                )
                AppRole.JOUEUR -> JoueurDrawer(
                    onOpenAccueil = {
                        drawerScope.launch { drawerState.close() }
                        val homeRoute = currentHomeRoute(appRole)
                        navController.navigate(homeRoute) {
                            popUpTo(homeRoute) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onClose = { drawerScope.launch { drawerState.close() } },
                )
                null -> Unit // Pas de rôle choisi : pas de tiroir pertinent (choix de rôle, sélection de monde).
            }
        }
    ) {
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = overrideStartDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
        ) {
            composable(Route.FirstLaunchWorldSelection.path) {
                WorldSelectionScreen(
                    onWorldSelected = { id, label, description, _, _, _ ->
                        GameState.selectWorld(
                            com.jc2.jdrcompagnon.ui.WorldState(
                                id = id,
                                name = label,
                                description = description,
                            )
                        )
                        navController.navigate(Route.RoleSelection.path) {
                            popUpTo(Route.FirstLaunchWorldSelection.path) { inclusive = true }
                        }
                    },
                    onBack = { /* Pas de retour possible au premier lancement */ },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
            }

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
                    onOpenCampaigns = {
                        navController.navigate(Route.Campaigns.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenGroups = {
                        navController.navigate(Route.Groups.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenBoutiques = {
                        navController.navigate(Route.Boutiques.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenMusic = {
                        navController.navigate(Route.Music.path) {
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
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

            composable(Route.MjCharacterCreation.path) {
                MjCharacterCreationScreen(
                    currentWorld = currentWorld,
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
                            popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
                    onCreateQuick = { navController.navigate(Route.MjCharacterCreation.path) },
                    onCreateWizard = { navController.navigate(Route.CharacterCreation.path + "?isMj=true") },
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
                        onLevelUp = { GameState.levelUpCharacter(it.id) },
                        onManageSpells = { character ->
                            navController.navigate(
                                Route.SpellManagement.path.replace("{characterId}", character.id) + "?isMj=$isMj"
                            )
                        },
                        onChooseCharacter = {
                            navController.navigate(Route.CharacterSelection.path + "?isMj=false")
                        },
                        onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    )
                }
            }

            composable(
                route = Route.SpellManagement.path + "?isMj={isMj}",
                arguments = listOf(
                    navArgument("characterId") { type = NavType.StringType },
                    navArgument("isMj") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                val isMj = backStackEntry.arguments?.getBoolean("isMj") ?: false
                SpellManagementScreen(
                    characterId = characterId,
                    isMjMode = isMj,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                )
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
                    onArmeArmureMagiqueClick = { name: String -> navController.navigate(Route.SrdSectionDetail.path.replace("{kind}", "arme_magique").replace("{entryName}", name)) }
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

            composable(Route.Groups.path) {
                GroupsScreen(
                    currentWorld = currentWorld,
                    onOpenBestiaryDetail = { monsterName ->
                        navController.navigate(Route.BestiaryDetail.path.replace("{monsterName}", monsterName))
                    },
                    onOpenEquipmentDetail = { equipmentName ->
                        navController.navigate(Route.EquipmentDetail.path.replace("{equipmentName}", equipmentName))
                    },
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
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
                route = Route.CampaignEditor.path,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId")
                CampaignEditorScreen(
                    campaignId = if (campaignId == "new") null else campaignId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
                    onOpenCarte = { id -> navController.navigate(Route.CarteCampagne.path.replace("{campagneId}", id)) },
                    onOpenEvenements = { id -> navController.navigate(Route.Evenements.path.replace("{campagneId}", id)) },
                    onOpenVilles = { id -> navController.navigate(Route.Villes.path.replace("{campagneId}", id)) },
                )
            }

            composable(
                route = Route.CarteCampagne.path,
                arguments = listOf(navArgument("campagneId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campagneId = backStackEntry.arguments?.getString("campagneId") ?: return@composable
                CarteCampagneScreen(
                    campagneId = campagneId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
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

            composable(
                route = Route.Villes.path,
                arguments = listOf(navArgument("campagneId") { type = NavType.StringType })
            ) { backStackEntry ->
                val campagneId = backStackEntry.arguments?.getString("campagneId") ?: return@composable
                VillesListScreen(
                    campagneId = campagneId,
                    onBack = { navController.popBackStack() },
                    onOpenMenu = { drawerScope.launch { drawerState.open() } },
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
                )
            }

            composable(Route.Settings.path) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Overlay du dé accessible depuis n'importe quel écran
        DiceOverlay(currentWorld = currentWorld)

        // Barre de menu globale accessible depuis n'importe quel écran
        // (sauf sur l'écran de sélection de rôle qui affiche ses propres boutons)
        if (appRole != null && navController.currentDestination?.route != Route.RoleSelection.path) {
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
                        popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateLibrary = {
                    navController.navigate(Route.Library.path.replace("{initialTab}", "monsters")) {
                        popUpTo(currentHomeRoute(appRole)) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onHomeTap = {
                    val homeRoute = currentHomeRoute(appRole)
                    navController.navigate(homeRoute) {
                        popUpTo(homeRoute) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
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
        "rule" -> Route.Library.path.replace("{initialTab}", "rules")
        else -> null
    }
    route?.let { navController.navigate(it) { launchSingleTop = true } }
}