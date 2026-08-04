package com.jc2.jdrcompagnon.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.DiceOverlay
import com.jc2.jdrcompagnon.ui.screens.RoleSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterSheetScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.JoueurHomeScreen
import com.jc2.jdrcompagnon.ui.screens.joueur.CharacterCreationScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MjHomeScreen
import com.jc2.jdrcompagnon.ui.screens.mj.MjCharacterCreationScreen
import com.jc2.jdrcompagnon.ui.screens.world.WorldSelectionScreen
import com.jc2.jdrcompagnon.ui.screens.world.WorldOption

@Composable
fun JdrNavGraph(overrideStartDestination: String = Route.RoleSelection.path) {
    val navController = rememberNavController()
    val currentWorld by GameState.currentWorld.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = overrideStartDestination,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(Route.FirstLaunchWorldSelection.path) {
                WorldSelectionScreen(
                    onWorldSelected = { world: WorldOption ->
                        GameState.selectWorld(
                            com.jc2.jdrcompagnon.ui.WorldState(
                                id = world.id,
                                name = world.label,
                                description = world.description,
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
                    onSelectMj = { navController.navigate(Route.MjHome.path) },
                    onSelectJoueur = { navController.navigate(Route.JoueurHome.path) }
                )
            }

            composable(Route.MjHome.path) {
                MjHomeScreen(
                    currentWorld = currentWorld,
                    onCreateCharacter = { navController.navigate(Route.MjCharacterCreation.path) },
                    onViewCharacters = { navController.navigate(Route.CharacterSelection.path + "?isMj=true") },
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
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
                        navController.navigate("${Route.CharacterSheet.path}/${character.id}?isMj=$isMj")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Route.CharacterSelection.path) {
                val currentWorld by GameState.currentWorld.collectAsState()
                CharacterSelectionScreen(
                    currentWorld = currentWorld,
                    onViewCharacter = { character ->
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
                    onWorldSelected = { world: WorldOption ->
                        GameState.selectWorld(
                            com.jc2.jdrcompagnon.ui.WorldState(
                                id = world.id,
                                name = world.label,
                                description = world.description,
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
                    onWorldSelected = { world: WorldOption ->
                        GameState.selectWorld(
                            com.jc2.jdrcompagnon.ui.WorldState(
                                id = world.id,
                                name = world.label,
                                description = world.description,
                            )
                        )
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Overlay du dé accessible depuis n'importe quel écran
        DiceOverlay(currentWorld = currentWorld)
    }
}