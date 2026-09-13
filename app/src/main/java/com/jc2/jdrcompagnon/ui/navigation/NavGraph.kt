package com.jc2.jdrcompagnon.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.jc2.jdrcompagnon.ui.screens.RoleSelectionScreen
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
import com.jc2.jdrcompagnon.ui.screens.mj.MusicScreen
import com.jc2.jdrcompagnon.ui.screens.world.WorldSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.BestiaryDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.EquipmentDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.LibraryScreen
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioEditorScreen
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioReaderScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.SpellDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.SrdSectionDetailScreen
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem

@Composable
fun JdrNavGraph(overrideStartDestination: String = Route.RoleSelection.path) {
    val navController = rememberNavController()
    val currentWorld by GameState.currentWorld.collectAsState()
    val appRole by GameState.appRole.collectAsState()
    val playerName by GameState.playerName.collectAsState()

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
                    onBack = { /* Pas de retour possible au premier lancement */ }
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
                    onSelectContext = { navController.navigate(Route.WorldSelection.path) }
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
                    onOpenLibrary = { navController.navigate(Route.Library.path.replace("{initialTab}", "monsters")) },
                    onOpenScenarioEditor = { scenarioId ->
                        navController.navigate(Route.ScenarioEditor.path.replace("{scenarioId}", scenarioId ?: "new"))
                    },
                    onOpenCampaigns = { navController.navigate(Route.Campaigns.path) },
                    onOpenMusic = { navController.navigate(Route.Music.path) },
                    onOpenLanHost = { groupId, campaignTitle ->
                        val params = buildList {
                            if (groupId != null) add("groupId=${java.net.URLEncoder.encode(groupId, "UTF-8")}")
                            if (campaignTitle != null) add("campaignTitle=${java.net.URLEncoder.encode(campaignTitle, "UTF-8")}")
                        }
                        val destination = if (params.isEmpty()) Route.LanHost.path else "${Route.LanHost.path}?${params.joinToString("&")}"
                        navController.navigate(destination)
                    },
                    onOpenInternalLink = { type, name ->
                        navigateToInternalLink(navController, type, name)
                    },
                    onBack = { navController.popBackStack() }
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
                )
            }

            composable(Route.JoueurHome.path) {
                JoueurHomeScreen(
                    currentWorld = currentWorld,
                    onChooseCharacter = { navController.navigate(Route.CharacterSelection.path) },
                    onCreateCharacter = { navController.navigate(Route.CharacterCreation.path) },
                    onBack = { navController.popBackStack() },
                    onViewCharacter = { character ->
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}")
                    },
                    onOpenMusic = { navController.navigate(Route.Music.path) },
                    onSelectWorld = { navController.navigate(Route.WorldSelection.path) }
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
                        GameState.selectCharacter(character.id)
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}?isMj=$isMj")
                    },
                    onCreateCharacter = { navController.navigate(Route.MjCharacterCreation.path) },
                    isMjMode = isMj,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.CharacterSelection.path) {
                val currentWorld by GameState.currentWorld.collectAsState()
                CharacterSelectionScreen(
                    currentWorld = currentWorld,
                    onViewCharacter = { character ->
                        GameState.selectCharacter(character.id)
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}")
                    },
                    onBack = { navController.popBackStack() }
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
                            navController.navigate(Route.SpellManagement.path.replace("{characterId}", character.id))
                        }
                    )
                }
            }

            composable(
                route = Route.SpellManagement.path,
                arguments = listOf(navArgument("characterId") { type = NavType.StringType })
            ) { backStackEntry ->
                val characterId = backStackEntry.arguments?.getString("characterId") ?: ""
                SpellManagementScreen(
                    characterId = characterId,
                    onBack = { navController.popBackStack() }
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
                        onCancel = { navController.popBackStack() }
                    )
                }
            }

            composable(Route.CharacterCreation.path) {
                CharacterCreationScreen(
                    currentWorld = currentWorld,
                    onCharacterCreated = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
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
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
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
                        navController.navigate(Route.ScenarioReader.path.replace("{scenarioId}", scenarioId))
                    }
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
                    }
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
                    }
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
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.Music.path) {
                MusicScreen(
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.Campaigns.path) {
                CampaignListScreen(
                    currentWorld = currentWorld,
                    onBack = { navController.popBackStack() },
                    onOpenCampaignEditor = { campaignId ->
                        navController.navigate(Route.CampaignEditor.path.replace("{campaignId}", campaignId ?: "new"))
                    }
                )
            }

            composable(
                route = Route.CampaignEditor.path,
                arguments = listOf(navArgument("campaignId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val campaignId = backStackEntry.arguments?.getString("campaignId")
                CampaignEditorScreen(
                    campaignId = if (campaignId == "new") null else campaignId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Overlay du dé accessible depuis n'importe quel écran
        DiceOverlay(currentWorld = currentWorld)

        // Barre de menu globale accessible depuis n'importe quel écran
        AppBottomBar(
            onNavigateConnection = {
                when (appRole) {
                    AppRole.MJ -> navController.navigate(Route.LanHost.path)
                    AppRole.JOUEUR -> navController.navigate(Route.LanJoin.path)
                    null -> navController.navigate(Route.LanTools.path)
                }
            },
            onNavigateLibrary = { navController.navigate(Route.Library.path.replace("{initialTab}", "monsters")) },
            onHomeTap = {
                val homeRoute = when (appRole) {
                    AppRole.MJ -> Route.MjHome.path
                    AppRole.JOUEUR -> {
                        val lastCharacterId = GameState.selectedCharacterId.value
                        val hasLastCharacter = lastCharacterId != null &&
                                GameState.characters.value.any { it.id == lastCharacterId }
                        if (hasLastCharacter) {
                            "${Route.CharacterSheet.path}/$lastCharacterId?isMj=false"
                        } else {
                            Route.JoueurHome.path
                        }
                    }
                    null -> Route.RoleSelection.path
                }
                navController.navigate(homeRoute) {
                    launchSingleTop = true
                    popUpTo(homeRoute) { inclusive = false }
                }
            },
            onHomeLongPress = {
                // Seul chemin de retour vers le choix de rôle : on vide toute la
                // pile pour repartir sur une base propre.
                navController.navigate(Route.RoleSelection.path) {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
        navController.navigate(route)
        return
    }
    val route = when (type) {
        "monster" -> Route.BestiaryDetail.path.replace("{monsterName}", name)
        "equipment" -> Route.EquipmentDetail.path.replace("{equipmentName}", name)
        "spell" -> Route.SpellDetail.path.replace("{spellName}", name)
        "rule" -> Route.Library.path.replace("{initialTab}", "rules")
        else -> null
    }
    route?.let { navController.navigate(it) }
}