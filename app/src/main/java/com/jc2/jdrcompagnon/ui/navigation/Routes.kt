package com.jc2.jdrcompagnon.ui.navigation

// Routes de l'app — sealed pour éviter les fautes de frappe au call site.
sealed class Route(val path: String) {
    data object FirstLaunchWorldSelection : Route("first_launch_world_selection")
    data object RoleSelection : Route("role_selection")
    data object MjHome : Route("mj")
    data object MjCharacterCreation : Route("mj_character_creation")
    data object JoueurHome : Route("joueur")
    data object CharacterSelection : Route("character_selection")
    data object CharacterSheet : Route("character_sheet")
    data object CharacterCreation : Route("character_creation")
    data object WorldSelection : Route("world_selection")
    data object Donjon : Route("donjon")
    data object Naheulbeuk : Route("naheulbeuk")
}