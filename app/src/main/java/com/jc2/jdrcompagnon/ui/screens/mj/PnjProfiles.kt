package com.jc2.jdrcompagnon.ui.screens.mj

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.calculateProficiencyBonus
import com.jc2.jdrcompagnon.ui.components.PnjPortraits
import com.jc2.jdrcompagnon.ui.components.rememberCharacterPortraitPainter

/**
 * Profil type de PNJ proposé à la création d'une fiche PNJ (outil Fiches, bouton "+") :
 * statistiques inspirées des blocs PNJ du SRD (garde, bandit, noble, prêtre...) et briefing
 * de départ. Le PNJ créé reste une fiche ordinaire, entièrement modifiable ensuite.
 */
data class PnjProfile(
    val id: String,
    val label: String,
    val description: String,
    val characterClass: String = "",
    val level: Int = 1,
    val alignment: String = "Neutre",
    val background: String = "",
    // FOR, DEX, CON, INT, SAG, CHA
    val stats: List<Int>,
    val hitPoints: Int,
    val armorClass: Int,
    val speed: Int = 30,
    val skills: List<String> = emptyList(),
    val equipment: String = "",
    val capacites: String = "",
    val comportement: String = "",
    val intentions: String = "",
    val objectif: String = "",
)

val pnjProfiles = listOf(
    PnjProfile(
        id = "roturier", label = "Roturier",
        description = "Paysan, domestique, badaud : la population ordinaire.",
        stats = listOf(10, 10, 10, 10, 10, 10), hitPoints = 4, armorClass = 10,
        skills = listOf("Dressage"), equipment = "Gourdin, vêtements de travail",
        comportement = "Prudent avec les étrangers, bavard une fois en confiance.",
        objectif = "Vivre tranquillement et nourrir sa famille."
    ),
    PnjProfile(
        id = "marchand", label = "Marchand",
        description = "Commerçant ambulant ou tenancier d'échoppe.",
        background = "Marchand", stats = listOf(10, 11, 10, 13, 12, 14), hitPoints = 9, armorClass = 11,
        skills = listOf("Persuasion", "Intuition", "Tromperie"),
        equipment = "Dague, balance, livre de comptes, bourse bien garnie",
        comportement = "Affable, négocie chaque pièce.",
        intentions = "Vendre au meilleur prix, acheter à bas coût.",
        objectif = "Faire fortune et ouvrir un comptoir en ville."
    ),
    PnjProfile(
        id = "aubergiste", label = "Aubergiste",
        description = "Tient une taverne, entend toutes les rumeurs.",
        stats = listOf(12, 10, 13, 11, 13, 13), hitPoints = 11, armorClass = 10,
        skills = listOf("Intuition", "Persuasion", "Perception"),
        equipment = "Gourdin sous le comptoir, tablier, clés des chambres",
        comportement = "Jovial, protège ses habitués.",
        intentions = "Garder l'auberge pleine et sans bagarre.",
        objectif = "Rembourser ses dettes et agrandir l'établissement."
    ),
    PnjProfile(
        id = "garde", label = "Garde",
        description = "Soldat de la milice ou garde du corps.",
        characterClass = "Guerrier", alignment = "Loyal Neutre",
        stats = listOf(13, 12, 12, 10, 11, 10), hitPoints = 11, armorClass = 16,
        skills = listOf("Perception", "Athlétisme"),
        equipment = "Lance, cotte de mailles, bouclier",
        capacites = "Lance : +3 au toucher, 1d6+1 perforants (1d8+1 à deux mains).",
        comportement = "Méfiant, applique le règlement à la lettre.",
        intentions = "Maintenir l'ordre.",
        objectif = "Finir son service sans ennuis."
    ),
    PnjProfile(
        id = "bandit", label = "Bandit",
        description = "Brigand de grand chemin, voleur des bas quartiers.",
        characterClass = "Voleur", alignment = "Chaotique Neutre",
        stats = listOf(11, 12, 12, 10, 10, 10), hitPoints = 11, armorClass = 12,
        skills = listOf("Discrétion", "Intimidation"),
        equipment = "Cimeterre, arbalète légère, armure de cuir",
        capacites = "Cimeterre : +3 au toucher, 1d6+1 tranchants. Arbalète légère : +3, 1d8+1 perforants.",
        comportement = "Menaçant en groupe, fuit quand la chance tourne.",
        intentions = "Détrousser les voyageurs.",
        objectif = "Un gros coup pour se retirer."
    ),
    PnjProfile(
        id = "brute", label = "Brute",
        description = "Homme de main, videur, gros bras d'une guilde.",
        characterClass = "Barbare", alignment = "Neutre Mauvais",
        stats = listOf(15, 11, 14, 10, 10, 11), hitPoints = 32, armorClass = 11, level = 3,
        skills = listOf("Intimidation", "Athlétisme"),
        equipment = "Masse d'armes, arbalète lourde, armure de cuir",
        capacites = "Attaques multiples (2 masse d'armes). Tactique de meute.",
        comportement = "Obéit à qui le paie, cogne d'abord.",
        intentions = "Faire respecter les ordres de son employeur."
    ),
    PnjProfile(
        id = "noble", label = "Noble",
        description = "Aristocrate, courtisan ou riche bourgeois.",
        background = "Noble", alignment = "Loyal Neutre",
        stats = listOf(11, 12, 11, 12, 14, 16), hitPoints = 9, armorClass = 15,
        skills = listOf("Persuasion", "Intuition", "Tromperie", "Histoire"),
        equipment = "Rapière, cuirasse, vêtements fins, chevalière",
        capacites = "Parade : +2 à la CA contre une attaque de corps à corps (réaction).",
        comportement = "Hautain, sensible aux marques de respect.",
        intentions = "Étendre son influence.",
        objectif = "Élever sa maison au-dessus de ses rivales."
    ),
    PnjProfile(
        id = "pretre", label = "Prêtre",
        description = "Desservant d'un temple, guérisseur ou prédicateur.",
        characterClass = "Clerc", background = "Acolyte", alignment = "Neutre Bon", level = 5,
        stats = listOf(10, 10, 12, 13, 16, 13), hitPoints = 27, armorClass = 13,
        skills = listOf("Médecine", "Persuasion", "Religion"),
        equipment = "Masse d'armes, chemise de mailles, symbole sacré",
        capacites = "Incantation (SAG) : lumière, flamme sacrée, soins, bénédiction, arme spirituelle, esprits gardiens.",
        comportement = "Bienveillant mais ferme sur le dogme.",
        intentions = "Soigner les fidèles, convertir les autres.",
        objectif = "Rebâtir le sanctuaire de son dieu."
    ),
    PnjProfile(
        id = "mage", label = "Mage érudit",
        description = "Sage, alchimiste ou magicien de la cour.",
        characterClass = "Magicien", background = "Sage", level = 9,
        stats = listOf(9, 14, 11, 17, 12, 11), hitPoints = 40, armorClass = 12,
        skills = listOf("Arcanes", "Histoire", "Investigation"),
        equipment = "Bâton, grimoire, sacoche à composantes",
        capacites = "Incantation (INT) : projectile magique, bouclier, boule de feu, contresort, invisibilité supérieure, cône de froid.",
        comportement = "Distrait, condescendant envers les profanes.",
        intentions = "Obtenir un savoir ou un artefact rare.",
        objectif = "Percer un secret arcanique oublié."
    ),
    PnjProfile(
        id = "eclaireur", label = "Éclaireur",
        description = "Guide, chasseur ou pisteur des terres sauvages.",
        characterClass = "Rôdeur", background = "Sauvageon", level = 2,
        stats = listOf(11, 14, 12, 11, 13, 11), hitPoints = 16, armorClass = 13,
        skills = listOf("Nature", "Perception", "Discrétion", "Survie"),
        equipment = "Épée courte, arc long, armure de cuir",
        capacites = "Ouïe et vue aiguisées : avantage aux jets de Perception (ouïe/vue).",
        comportement = "Taciturne, à l'aise loin des villes.",
        intentions = "Être payé pour guider, sans prendre de risques inutiles."
    ),
    PnjProfile(
        id = "espion", label = "Espion",
        description = "Informateur, agent secret, receleur.",
        characterClass = "Voleur", background = "Criminel", level = 4,
        stats = listOf(10, 15, 10, 12, 14, 16), hitPoints = 27, armorClass = 12,
        skills = listOf("Tromperie", "Intuition", "Investigation", "Persuasion", "Escamotage", "Discrétion"),
        equipment = "Épée courte, arbalète de poing, déguisements",
        capacites = "Ruse : Se désengager ou Se cacher en action bonus. Attaque sournoise (+2d6, 1/tour).",
        comportement = "Charmant, ne dit jamais tout.",
        intentions = "Récolter des secrets à revendre.",
        objectif = "Servir (ou trahir) son véritable maître."
    ),
    PnjProfile(
        id = "chevalier", label = "Chevalier",
        description = "Guerrier juré au service d'un seigneur ou d'un ordre.",
        characterClass = "Paladin", background = "Soldat", alignment = "Loyal Bon", level = 8,
        stats = listOf(16, 11, 14, 11, 11, 15), hitPoints = 52, armorClass = 18,
        skills = listOf("Athlétisme", "Intimidation"),
        equipment = "Épée à deux mains, arbalète lourde, harnois",
        capacites = "Attaques multiples (2). Brave : avantage aux sauvegardes contre la peur. Commandement (1/repos).",
        comportement = "Honorable, direct, fidèle à sa parole.",
        intentions = "Protéger les faibles et servir son suzerain.",
        objectif = "Accomplir la quête qui lui a été confiée."
    ),
    PnjProfile(
        id = "veteran", label = "Vétéran",
        description = "Mercenaire endurci, ancien soldat, capitaine.",
        characterClass = "Guerrier", background = "Soldat", level = 9,
        stats = listOf(16, 13, 14, 10, 11, 10), hitPoints = 58, armorClass = 17,
        skills = listOf("Athlétisme", "Perception"),
        equipment = "Épée longue, épée courte, arbalète lourde, clibanion",
        capacites = "Attaques multiples (2 épée longue + 1 épée courte).",
        comportement = "Pragmatique, a tout vu.",
        intentions = "Vendre son épée au plus offrant."
    ),
)

/** Espèces proposées à la création (le portrait, lui, vient toujours de assets/dnd/PNJ). */
private val especesPortraits = listOf(
    // Plusieurs orthographes par espèce ("|"), telles qu'utilisées dans les noms de fichiers.
    "Humain" to "humain", "Elfe" to "elfe", "Nain" to "nain", "Halfelin" to "halfelin|halfling",
    "Gnome" to "gnome", "Orc" to "orc", "Tieffelin" to "tieffelin|tiefelin", "Drakéide" to "drakeide|drakeid",
    "Goliath" to "goliath", "Gobelin" to "gobelin",
)

private val prenomsPnj = listOf(
    "Aldric", "Berthe", "Cédric", "Dora", "Émeric", "Fiona", "Gaspard", "Hilda", "Isaure",
    "Jorik", "Katel", "Lothar", "Maëlle", "Nestor", "Odile", "Perrin", "Rosalind", "Sven",
    "Tiphaine", "Ulric", "Vianne", "Wendel", "Yseult", "Zoran",
)
private val surnomsPnj = listOf(
    "le Borgne", "la Rousse", "Deux-Lames", "Main-Leste", "de la Tour", "Cœur-Franc",
    "Pierrefeu", "le Sage", "Barbegrise", "des Marais", "Brisefer", "l'Ancien",
)

private fun nomPnjAleatoire(): String = "${prenomsPnj.random()} ${surnomsPnj.random()}"


/** Construit la fiche PNJ à partir d'un profil — une fiche ordinaire, modifiable ensuite. */
fun PnjProfile.toCharacter(name: String, race: String, portrait: String, worldId: String): Character {
    val bonus = calculateProficiencyBonus(level)
    return Character(
        name = name,
        type = "PNJ",
        worldId = worldId,
        characterClass = characterClass,
        race = race,
        level = level,
        alignment = alignment,
        background = background,
        strength = stats[0],
        dexterity = stats[1],
        constitution = stats[2],
        intelligence = stats[3],
        wisdom = stats[4],
        charisma = stats[5],
        maxHitPoints = hitPoints,
        currentHitPoints = hitPoints,
        armorClass = armorClass,
        speed = speed,
        proficiencyBonus = bonus,
        skills = skills.associateWith { ProficiencyLevel.PROFICIENT },
        skillProficiencies = skills,
        equipment = equipment,
        classFeatures = capacites,
        comportement = comportement,
        intentions = intentions,
        objectif = objectif,
        dmNotes = "Profil : $label",
        portrait = portrait,
        createdBy = "MJ",
    )
}

/**
 * Création d'un PNJ à partir d'un profil : choix du profil, puis nom, espèce, sexe et
 * portrait (les portraits de l'espèce choisie sont proposés en premier). [onCreate] reçoit la
 * fiche prête à enregistrer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PnjProfileDialog(
    worldId: String,
    onCreate: (Character) -> Unit,
    onDismiss: () -> Unit,
) {
    var profil by remember { mutableStateOf<PnjProfile?>(null) }
    var nom by remember { mutableStateOf(nomPnjAleatoire()) }
    var espece by remember { mutableStateOf(especesPortraits.first()) }
    var feminin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Portraits PNJ uniquement (assets/dnd/PNJ), ceux du sexe choisi en premier.
    var portrait by remember { mutableStateOf(PnjPortraits.idsFor(context, feminin).firstOrNull().orEmpty()) }

    fun choisirEspece(nouvelle: Pair<String, String>, nouveauFeminin: Boolean) {
        if (nouvelle != espece || nouveauFeminin != feminin) {
            portrait = PnjPortraits.idsFor(context, nouveauFeminin, nouvelle.second).firstOrNull() ?: portrait
        }
        espece = nouvelle
        feminin = nouveauFeminin
    }

    val choisi = profil
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (choisi == null) "Profil de PNJ" else "PNJ : ${choisi.label}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (choisi == null) {
                    Text("Choisissez un profil de départ :", style = MaterialTheme.typography.bodyMedium)
                    pnjProfiles.forEach { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { profil = p },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(p.label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text(
                                        "CA ${p.armorClass} · PV ${p.hitPoints}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(p.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } else {
                    Text(choisi.description, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = nom,
                        onValueChange = { nom = it },
                        label = { Text("Nom") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { nom = nomPnjAleatoire() }) {
                                Icon(Icons.Default.Casino, contentDescription = "Nom aléatoire")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Espèce", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        especesPortraits.forEach { e ->
                            FilterChip(
                                selected = espece == e,
                                onClick = { choisirEspece(e, feminin) },
                                label = { Text(e.first) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !feminin, onClick = { choisirEspece(espece, false) }, label = { Text("Homme") })
                        FilterChip(selected = feminin, onClick = { choisirEspece(espece, true) }, label = { Text("Femme") })
                    }
                    Text("Portrait", style = MaterialTheme.typography.labelLarge)
                    val proposes = remember(feminin, espece) { PnjPortraits.idsFor(context, feminin, espece.second) }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        proposes.forEach { optionId ->
                            val selection = optionId == portrait
                            val painter = rememberCharacterPortraitPainter(optionId) ?: return@forEach
                            Image(
                                painter = painter,
                                contentDescription = PnjPortraits.label(optionId),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        if (selection) 3.dp else 1.dp,
                                        if (selection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { portrait = optionId }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "La fiche reste modifiable après création (statistiques, portrait, briefing, réputation).",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            if (choisi != null) {
                TextButton(
                    enabled = nom.isNotBlank(),
                    onClick = { onCreate(choisi.toCharacter(nom.trim(), espece.first, portrait, worldId)) }
                ) { Text("Créer") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (choisi != null) profil = null else onDismiss() }) {
                Text(if (choisi != null) "Autre profil" else "Annuler")
            }
        }
    )
}
