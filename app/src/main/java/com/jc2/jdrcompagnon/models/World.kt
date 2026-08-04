package com.jc2.jdrcompagnon.models

/**
 * Modèle pour les mondes de JDR (Donjon ou Nauveubeuk).
 */
data class World(
    val id: String,
    val name: String,
    val description: String,
    val colorPrimary: Int,      // Hexa sans alpha (0xFFAARRGGBB)
    val colorSecondary: Int,    // Hexa sans alpha
    val iconKey: String         // Clé pour sélectionner l'icone Compose à utiliser dans UI
) {

    companion object {
        val DONJON = World(
            id = "donjon",
            name = "Le Donjon",
            description = """C'est la frontière du chaos. Un monde brutal et dense où les créatures sauvages patouillent dans des forêts éternelles et des forteresses en ruine. Les défis sont directs : combat, exploration, survie. Parfait pour les guerriers et les vampires.""".trimIndent(),
            colorPrimary = 0xF54336,
            colorSecondary = 0xFFCDD2,
            iconKey = "Sword"
        )

        val NAEVEUBEUK = World(
            id = "nauveubeuk",
            name = "Nauveubeuk",
            description = """Un monde mystérieux, plein d'artefacts perdus et d'histoires inexplorées. Les secrets du Donjon sont ici — mais au prix d'une curiosité dangereuse. Idéal pour les mages, explorateurs, et ceux qui cherchent à comprendre le monde entier.""".trimIndent(),
            colorPrimary = 0xF9A825,
            colorSecondary = 0xFFCDD2,
            iconKey = "Dragon"
        )

        val ALL_WORLDS = listOf(DONJON, NAEVEUBEUK)
    }
}