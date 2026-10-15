package com.jc2.jdrcompagnon.ui.navigation

// Routes de l'app — sealed pour éviter les fautes de frappe au call site.
sealed class Route(val path: String) {
    data object RoleSelection : Route("role_selection")
    data object MjHome : Route("mj")
    data object MjCharacterCreation : Route("mj_character_creation")
    data object JoueurHome : Route("joueur")
    data object CharacterSelection : Route("character_selection")
    data object CharacterSheet : Route("character_sheet")
    data object CharacterEdit : Route("character_edit/{characterId}")
    data object CharacterCreation : Route("character_creation")
    data object SrdSectionDetail : Route("srd_entry/{kind}/{entryName}")
    data object WorldSelection : Route("world_selection")
    data object ScenarioEditor : Route("scenario_editor/{scenarioId}")
    data object ScenarioReader : Route("scenario_reader/{scenarioId}")
    data object Scenarios : Route("scenarios")
    data object CampaignEditor : Route("campaign_editor/{campaignId}")
    data object Campaigns : Route("campaigns")
    data object CampaignOverview : Route("campaign_overview/{campaignId}")
    // Carnet du MJ du monde courant : préparation de séance, secrets, fils (feature_preparation).
    data object PreparationSeance : Route("preparation_seance")
    // Outil FACTIONS (feature_faction) : factions du monde courant et fiche d'une faction.
    data object Factions : Route("factions")
    data object FactionDetail : Route("faction_detail/{factionId}")
    data object CarteCampagne : Route("carte_campagne/{campagneId}?carteId={carteId}") {
        /** carteId null = carte principale de la campagne. */
        fun creer(campagneId: String, carteId: String? = null): String =
            "carte_campagne/$campagneId" + (carteId?.let { "?carteId=$it" } ?: "")
    }
    data object Evenements : Route("evenements/{campagneId}")
    // Outil ÉPREUVES (feature_epreuve) : épreuves environnementales, lancées aussi par #epreuve:.
    data object Epreuves : Route("epreuves")
    // Outil COMPORTEMENT (feature_combat) : comportements de l'IA de combat et grille par classe.
    data object Comportement : Route("comportement")
    // Outil BOSS (feature_combat) : boss intégrés et créés par le MJ, lancement d'un combat de boss.
    data object Boss : Route("boss_outil")
    data object EpreuveResolution : Route("epreuve_resolution")
    data object Villes : Route("villes/{campagneId}")
    // Outil VILLES : bibliothèque de villes et villes des campagnes (VillesOutilScreen).
    data object VillesOutil : Route("villes_outil")
    data object VilleDetail : Route("ville_detail/{campagneId}/{villeId}")
    data object Groups : Route("groups")
    data object Repos : Route("repos")
    data object Horloge : Route("horloge")
    data object GroupDetail : Route("group_detail/{groupId}")
    data object Boutiques : Route("boutiques")
    data object BoutiqueDetail : Route("boutique_detail/{boutiqueId}")
    data object Environnements : Route("environnements")
    data object EnvironnementDetail : Route("environnement_detail/{environnementId}")
    data object TablesAleatoires : Route("tables_aleatoires")
    data object TableAleatoireDetail : Route("table_aleatoire_detail/{tableId}")
    data object BibliothequeEvenements : Route("bibliotheque_evenements")
    // Outil « Combat » : réglages du combat ; la référence des actions est un écran à part.
    data object CombatActions : Route("combat_actions")
    data object CombatReference : Route("combat_reference")
    data object SimulationCombat : Route("simulation_combat/{personnageId}")
    data object CombatEnCours : Route("combat_en_cours")
    data object CombatJoueur : Route("combat_joueur")
    data object Music : Route("music")
    data object Import : Route("import")
    data object Library : Route("library/{initialTab}")
    data object BestiaryDetail : Route("bestiary/{monsterName}")
    data object SpellDetail : Route("spell/{spellName}")
    data object EquipmentDetail : Route("equipment/{equipmentName}")
    data object RuleDetail : Route("rule/{ruleName}")
    data object LanTools : Route("lan_tools")
    data object LanHost : Route("lan_tools_host")
    data object LanJoin : Route("lan_tools_join")
    data object Settings : Route("settings")
    data object ProposalStatus : Route("proposal_status")
    data object PlayerQuests : Route("player_quests")
    data object PlayerGroup : Route("player_group")
}