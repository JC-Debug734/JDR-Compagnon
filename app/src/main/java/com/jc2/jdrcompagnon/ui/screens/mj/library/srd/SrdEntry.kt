package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

/**
 * Représente une entrée du SRD (System Reference Document).
 * Utilisée pour les monstres, les sorts, et tout contenu markdown indexé.
 */
data class SrdEntry(
    val name: String,
    val rawMarkdown: String,
    val category: String = "",
    val firstLetter: Char = name.firstOrNull()?.uppercaseChar() ?: 'A',
    // Renseignés uniquement pour les sorts (cf. SpellParser.parse) : niveau normalisé
    // ("Sort mineur", "Niveau 3"...) et type déduit de la description ("Attaque",
    // "Soutien", "Autre"), affichés sur chaque carte de sort de la fiche de personnage
    // (cf. SpellRow dans CharacterSheetScreen.kt) sans avoir à déplier le détail.
    val niveauSort: String = "",
    val typeSort: String = "",
    // Sorts uniquement : temps d'incantation et portée bruts ("action", "9 m"...), affichés
    // eux aussi directement sur la carte de sort de la fiche.
    val tempsIncantation: String = "",
    val portee: String = "",
    // Renseigné uniquement pour les monstres (cf. MonsterParser.parse) : environnements où
    // ce monstre peut être rencontré, utilisés par la feature Environnement pour suggérer
    // un bestiaire adapté sans dupliquer l'information en base.
    val environments: List<String> = emptyList(),
    // Monstres uniquement : image déclarée dans le .md par une ligne "Image: gobelin.png"
    // (voir MonsterParser / MonsterImages), null sinon — l'image est alors cherchée par nom.
    val image: String? = null,
    // Monstres uniquement : champs bruts de l'en-tête ("CA" → "17 Initiative +7 (17)"...) et
    // corps (sections "## Traits", "## Actions"...), pour le rendu en bloc de stats
    // (MonsterStatBlock). Vides pour une entrée sans structure : rendu markdown de repli.
    val fields: Map<String, String> = emptyMap(),
    val body: String = "",
)