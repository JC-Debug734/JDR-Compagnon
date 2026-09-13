package com.jc2.jdrcompagnon.ui.screens.joueur

// Classes de D&D
val dndClasses = listOf(
    "Guerrier", "Magicien", "Rôdeur", "Clerc", "Voleur", "Paladin",
    "Barde", "Druide", "Ensorceleur", "Moine", "Barbare", "Sorcier",
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

// Compétences de D&D
val skillList = listOf(
    "Acrobaties", "Arcanes", "Athlétisme", "Furtivité", "Dressage",
    "Escamotage", "Histoire", "Intimidation", "Investigation", "Médecine",
    "Nature", "Perception", "Persuasion", "Religion", "Représentation",
    "Survie", "Tromperie"
)

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
