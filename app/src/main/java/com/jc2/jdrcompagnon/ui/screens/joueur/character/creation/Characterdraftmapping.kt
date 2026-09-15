package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import com.jc2.jdrcompagnon.ui.Character

/**
 * Convertit le brouillon rempli par le wizard en [Character] réel, prêt pour
 * GameState.addCharacter(...). Le nom est saisi en amont du wizard (voir
 * CharacterCreationScreen.EtapeNomPersonnage) donc passé ici plutôt que stocké
 * dans CharacterDraft.
 *
 * `background` reçoit le nom de l'historique SRD (Acolyte/Criminel/Sage/Soldat) :
 * CharacterSheetScreen l'affiche sous le libellé "Historique" (onglet Notes,
 * DetailRow). Le don, les traits d'espèce et les aptitudes de classe de niveau 1
 * vont dans `traits` (affiché sur la fiche via le bloc "Traits & capacités" de
 * l'onglet Notes).
 *
 * Champ non couvert par ce mapping, car le wizard ne propose pas encore ce choix :
 * - `weapons` / `armor` : l'équipement choisi est stocké en texte libre dans
 *   `equipment` (et son or dans `gold`), pas décomposé objet par objet dans
 *   `backpackItems`/`weapons`/`armor` — ces listes attendent des noms d'objets
 *   exacts de la bibliothèque SRD, que le texte d'équipement de départ ne donne
 *   pas sous cette forme (ex. "Hache à deux mains, 4 hachettes" reste une chaîne).
 */
fun CharacterDraft.versCharacter(nom: String, worldId: String, createdBy: String = "Joueur"): Character {
    val modConstitution = modificateurs[Caracteristique.CONSTITUTION] ?: 0
    val pv = classe?.pvNiveau1(modConstitution) ?: 10

    val vitesse = espece?.vitesse
        ?.replace(",", ".")
        ?.let { Regex("""[\d.]+""").find(it)?.value?.toDoubleOrNull()?.toInt() }
        ?: 9

    val traitsTexte = buildString {
        historique?.let { h ->
            appendLine("Don d'historique : ${h.don} — outils : ${h.maitriseOutils}")
        }
        espece?.traits?.forEach { t -> appendLine("${t.nom} (${espece.nom}) : ${t.description}") }
        classe?.aptitudesNiveau1?.forEach { a -> appendLine("${a.nom} (${classe.nom}) : ${a.description}") }
    }.trim()

    val equipementTexte = listOfNotNull(equipementClasseTexte, equipementHistoriqueTexte)
        .joinToString(", ")

    val competencesFinales = (historique?.maitrisesCompetence.orEmpty() + competencesClasse).distinct()

    return Character(
        name = nom,
        type = "PJ",
        worldId = worldId,
        characterClass = classe?.nom.orEmpty(),
        race = espece?.nom.orEmpty(),
        alignment = alignement?.label.orEmpty(),
        background = historique?.nom.orEmpty(),
        strength = valeursFinales[Caracteristique.FORCE] ?: 10,
        dexterity = valeursFinales[Caracteristique.DEXTERITE] ?: 10,
        constitution = valeursFinales[Caracteristique.CONSTITUTION] ?: 10,
        intelligence = valeursFinales[Caracteristique.INTELLIGENCE] ?: 10,
        wisdom = valeursFinales[Caracteristique.SAGESSE] ?: 10,
        charisma = valeursFinales[Caracteristique.CHARISME] ?: 10,
        maxHitPoints = pv,
        currentHitPoints = pv,
        speed = vitesse,
        gold = orDepart,
        equipment = equipementTexte,
        proficiencyBonus = BONUS_MAITRISE_NIVEAU_1,
        savingThrowProficiencies = classe?.maitriseJetsSauvegarde?.map { it.label } ?: emptyList(),
        skillProficiencies = competencesFinales,
        spells = sortsChoisis,
        traits = traitsTexte,
        personalityTraits = personnalite,
        ideals = ideaux,
        bonds = liens,
        flaws = defauts,
        createdBy = createdBy
        // `size` volontairement absent : Character le déduit de `race` via
        // sizeForRace ailleurs dans le code quand il est vide.
    )
}