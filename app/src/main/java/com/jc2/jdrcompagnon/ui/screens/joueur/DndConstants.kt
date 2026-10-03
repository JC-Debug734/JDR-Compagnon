package com.jc2.jdrcompagnon.ui.screens.joueur

// Classes de D&D
val dndClasses = listOf(
    "Guerrier", "Magicien", "Rôdeur", "Clerc", "Voleur", "Paladin",
    "Barde", "Druide", "Ensorceleur", "Moine", "Barbare", "Sorcier",
)

// Caractéristique d'incantation associée à chaque classe de lanceur de sorts D&D 5e
// (utilisée pour calculer le modificateur d'incantation, le DD de sauvegarde et le
// bonus d'attaque avec un sort — voir GameState.spellcastingAbility). Absente de
// cette map = classe non lanceuse de sorts (ex: Guerrier, Voleur, Barbare, Moine).
val spellcastingAbilityByClass: Map<String, String> = mapOf(
    "Magicien" to "Intelligence",
    "Clerc" to "Sagesse",
    "Druide" to "Sagesse",
    "Rôdeur" to "Sagesse",
    "Barde" to "Charisme",
    "Ensorceleur" to "Charisme",
    "Sorcier" to "Charisme",
    "Paladin" to "Charisme",
    "Occultiste" to "Charisme",
)

// Races de D&D (étendues pour MJ)
val dndRaces = listOf(
    "Humain", "Elfe", "Nain", "Halfelin", "Gnome", "Demi-elfe",
    "Demi-orc", "Tieffelin", "Dragonborn", "Gobelin", "Orc", "Hobgoblin"
)

// Historiques de D&D
val dndBackgrounds = listOf(
    "Acolyte", "Charlatan", "Criminel", "Entertainer", "Herboriste",
    "Marin", "Militaire", "Noble", "Paysan", "Savant", "Voyageur"
)

// Alignements D&D
val dndAlignments = listOf(
    "Loyal Bon", "Neutre Bon", "Chaotique Bon",
    "Loyal Neutre", "Neutre", "Chaotique Neutre",
    "Loyal Mauvais", "Neutre Mauvais", "Chaotique Mauvais"
)

// Compétences de D&D 5e, rangées par caractéristique (source unique de vérité :
// skillAbility ci-dessous en dérive l'abréviation affichée et le modificateur
// appliqué, pour ne pas dupliquer cette association ailleurs).
val skillList = listOf(
    // Force
    "Athlétisme",
    // Dextérité
    "Acrobaties", "Discrétion", "Escamotage",
    // Intelligence
    "Arcanes", "Histoire", "Investigation", "Nature", "Religion",
    // Sagesse
    "Dressage", "Intuition", "Médecine", "Perception", "Survie",
    // Charisme
    "Intimidation", "Persuasion", "Représentation", "Tromperie"
)

/**
 * Caractéristique associée à chaque compétence de [skillList], sous sa forme
 * complète (ex: "Force") — utilisée pour calculer le modificateur applicable.
 */
val skillAbility: Map<String, String> = mapOf(
    "Athlétisme" to "Force",
    "Acrobaties" to "Dextérité",
    "Discrétion" to "Dextérité",
    "Escamotage" to "Dextérité",
    "Arcanes" to "Intelligence",
    "Histoire" to "Intelligence",
    "Investigation" to "Intelligence",
    "Nature" to "Intelligence",
    "Religion" to "Intelligence",
    "Dressage" to "Sagesse",
    "Intuition" to "Sagesse",
    "Médecine" to "Sagesse",
    "Perception" to "Sagesse",
    "Survie" to "Sagesse",
    "Intimidation" to "Charisme",
    "Persuasion" to "Charisme",
    "Représentation" to "Charisme",
    "Tromperie" to "Charisme"
)

/** Abréviation à 3 lettres de la caractéristique associée à une compétence. */
fun skillAbilityAbbreviation(skill: String): String = when (skillAbility[skill]) {
    "Force" -> "FOR"
    "Dextérité" -> "DEX"
    "Intelligence" -> "INT"
    "Sagesse" -> "SAG"
    "Charisme" -> "CHA"
    else -> "?"
}

// Capacités de sauvegarde
val savingThrows = listOf(
    "Force", "Dextérité", "Constitution", "Intelligence", "Sagesse", "Charisme"
)

object DndConstants {
    enum class DndAlignment(val displayName: String) {
        LAWFUL_GOOD("Loyal Bon"),
        NEUTRAL_GOOD("Neutre Bon"),
        CHAOTIC_GOOD("Chaotique Bon"),
        LAWFUL_NEUTRAL("Loyal Neutre"),
        TRUE_NEUTRAL("Neutre"),
        CHAOTIC_NEUTRAL("Chaotique Neutre"),
        LAWFUL_EVIL("Loyal Mauvais"),
        NEUTRAL_EVIL("Neutre Mauvais"),
        CHAOTIC_EVIL("Chaotique Mauvais");

        companion object {
            fun entriesList(): List<DndAlignment> = entries.toList()
        }
    }
}
