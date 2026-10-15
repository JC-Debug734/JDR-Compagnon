package com.jc2.jdrcompagnon.ui.screens.mj.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmeSrd
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArmesPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.AttaqueMonstre
import com.jc2.jdrcompagnon.feature_combat.domain.model.TypeAttaqueMonstre
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.screens.joueur.character.parserCapacitesFiche
import com.jc2.jdrcompagnon.ui.screens.joueur.skillAbility
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry

/**
 * Vue simplifiée d'une fiche de PNJ, mise en page comme un bloc de stats du bestiaire
 * ([MonsterStatBlock]) : la fiche est convertie en entrée de monstre (champs « CA », « Pv »,
 * « Caractéristiques »… et sections « Traits » / « Actions »), pour un rendu identique sans
 * dupliquer la mise en page. Les attaques sont calculées depuis les armes du PNJ.
 */
@Composable
fun PnjStatBlock(character: Character, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val monde = character.worldId.ifBlank { "donjon_et_dragon" }
    var armesSrd by remember(monde) { mutableStateOf<List<ArmeSrd>>(emptyList()) }
    LaunchedEffect(monde, character.actionsPnj.isEmpty()) {
        // Actions fixes : inutile de charger les armes du SRD.
        if (character.actionsPnj.isNotEmpty()) return@LaunchedEffect
        armesSrd = com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository.loadEquipmentList(context, monde)
            .filter { it.damage.isNotBlank() }
            .map { ArmeSrd.depuisFiche(it.name, it.damage, it.properties, it.rawMarkdown) }
    }
    val entree = remember(character, armesSrd) {
        val attaques = ArmesPersonnage.attaques(
            nomsArmes = (character.weapons + character.equippedSlots.values + character.equippedItems).distinct(),
            armes = armesSrd,
            force = character.strength,
            dexterite = character.dexterity,
            maitrise = character.proficiencyBonus,
        )
        character.versBlocDeStats(attaques)
    }
    MonsterStatBlock(entree, modifier = modifier)
}

private fun signe(valeur: Int) = if (valeur >= 0) "+$valeur" else "−${-valeur}"

private fun mod(score: Int) = Math.floorDiv(score - 10, 2)

/** Convertit une fiche de personnage en « entrée de bestiaire » lisible par [MonsterStatBlock]. */
fun Character.versBlocDeStats(attaques: List<AttaqueMonstre>): SrdEntry {
    val maitrise = proficiencyBonus
    val caracs = listOf(
        "For" to strength, "Dex" to dexterity, "Con" to constitution,
        "Int" to intelligence, "Sag" to wisdom, "Cha" to charisma,
    )
    val nomsSauvegardes = mapOf(
        "For" to "Force", "Dex" to "Dextérité", "Con" to "Constitution",
        "Int" to "Intelligence", "Sag" to "Sagesse", "Cha" to "Charisme",
    )
    val sauvegardesMaitrisees = savingThrowProficiencies.toSet() + savingThrows.filterValues { it != ProficiencyLevel.NONE }.keys
    val competencesMaitrisees = skillProficiencies.toSet() + skills.filterValues { it != ProficiencyLevel.NONE }.keys
    val expertises = skillExpertise.toSet() + skills.filterValues { it == ProficiencyLevel.EXPERTISE }.keys
    val ligneCaracs = caracs.joinToString(" ") { (label, score) ->
        val m = mod(score)
        val js = m + if (nomsSauvegardes[label] in sauvegardesMaitrisees) maitrise else 0
        "$label $score ${signe(m)} ${signe(js)}"
    }

    val competences = competencesMaitrisees.sorted().joinToString(", ") { comp ->
        val base = GameState.abilityModifierForSkill(comp, this)
        val bonus = when (comp) {
            in expertises -> maitrise * 2
            else -> maitrise
        }
        "$comp ${signe(base + bonus)}"
    }
    val perception = 10 + mod(wisdom) + if ("Perception" in competencesMaitrisees) maitrise else 0
    val equipement = (weapons + armor + equippedItems).map { it.trim() }.filter { it.isNotBlank() }.distinct()
        .joinToString(", ") { it.replaceFirstChar { c -> c.lowercase() } }
    val classe = listOf(characterClass, subclass).filter { it.isNotBlank() }.joinToString(" — ")
    val initiative = initiativeBonus.takeIf { it != 0 } ?: mod(dexterity)

    val fields = buildMap<String, String> {
        put("Type", listOf("Humanoïde", race.takeIf { it.isNotBlank() }?.let { "($it)" }).filterNotNull().joinToString(" "))
        // `size` seul désignerait la taille de la map en construction.
        this@versBlocDeStats.size.takeIf { it.isNotBlank() }?.let { put("Taille", it) }
        alignment.takeIf { it.isNotBlank() }?.let { put("Alignement", it) }
        put("CA", "$armorClass Initiative ${signe(initiative)} (${10 + initiative})")
        put("Pv", if (currentHitPoints != maxHitPoints) "$currentHitPoints / $maxHitPoints" else "$maxHitPoints")
        put("Vitesse", "${"%.1f".format(speed * 0.3).replace(".0", "").replace('.', ',')} m")
        put("Caractéristiques", ligneCaracs)
        if (competences.isNotBlank()) put("Compétences", competences)
        if (damageResistances.isNotEmpty()) put("Résistances", damageResistances.joinToString(", "))
        if (damageVulnerabilities.isNotEmpty()) put("Vulnérabilités", damageVulnerabilities.joinToString(", "))
        if (equipement.isNotBlank()) put("Équipement", equipement)
        put("Sens", "Perception passive $perception")
        if (languages.isNotEmpty()) put("Langues", languages.joinToString(", "))
        put("FP", "Niveau $level" + (classe.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: "") + " ; BM ${signe(maitrise)}")
    }

    val traits = (parserCapacitesFiche(traits) + parserCapacitesFiche(classFeatures) + parserCapacitesFiche(feats))
        .map { c ->
            val description = c.description.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
            "${c.titre.substringBefore(" — ").trimEnd('.')}. ${description.ifBlank { c.choix.joinToString(", ").ifBlank { "—" } }}"
        }
    // Actions fixes du PNJ : reprises telles quelles ; sinon attaques calculées depuis ses armes.
    val actions = if (actionsPnj.isNotEmpty()) actionsPnj.map { it.ligneBloc } else attaques.map { a ->
        val (portee, nature) = when (a.type) {
            TypeAttaqueMonstre.DISTANCE -> "portée ${a.porteeLongue?.let { "${it.toInt()} m" } ?: "à distance"}" to "À distance"
            TypeAttaqueMonstre.POLYVALENTE -> "allonge 1,50 m ou portée ${a.porteeLongue?.let { "${it.toInt()} m" } ?: "courte"}" to "Corps à corps ou distance"
            else -> "allonge 1,50 m" to "Corps à corps"
        }
        val degats = a.formuleDegats?.let { "${a.degatsMoyens} ($it)" } ?: "${a.degatsMoyens}"
        "${a.nom}. $nature : ${a.bonusToucher?.let { signe(it) } ?: "+0"}, $portee. Touché : $degats dégâts ${a.typeDegats.orEmpty()}".trimEnd() + "."
    }
    val body = buildString {
        if (traits.isNotEmpty()) {
            appendLine("## Traits")
            traits.forEach { appendLine(it) }
        }
        if (actions.isNotEmpty()) {
            appendLine("## Actions")
            actions.forEach { appendLine(it) }
        }
        if (spells.isNotEmpty()) {
            appendLine("## Sorts")
            appendLine("Sorts connus. ${spells.joinToString(", ")}")
        }
    }
    return SrdEntry(name = name, rawMarkdown = "", fields = fields, body = body)
}
