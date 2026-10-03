package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/** Violet du grimoire (icône, contour du panneau). */
internal val CouleurGrimoire = Color(0xFF9B6BD8)
private val REGEX_CLASSES = Regex("""\*\*Classes\s*:\*\*\s*(.+)""")

/** Le sort figure-t-il dans la liste de sorts du Magicien (champ « **Classes :** ») ? */
private fun SrdEntry.sortDeMagicien(): Boolean =
    REGEX_CLASSES.find(rawMarkdown)?.groupValues?.get(1).orEmpty()
        .split(",").any { it.trim().equals("Magicien", ignoreCase = true) }

/**
 * Contenu du grimoire du Magicien, ouvert comme le sac à dos : on touche le grimoire (dans le
 * sac ou sur un emplacement utilitaire) pour l'ouvrir juste en dessous (cf. SilhouetteSlots).
 * On y trouve les sorts consignés (tous les sorts du Magicien, par niveau, avec la même carte
 * que la fiche) et l'action « Recopier un sort », qui fait choisir le matériel d'écriture dans
 * l'inventaire (GameState.recopierSortDansGrimoire).
 */
@Composable
fun GrimoirePanel(character: Character, grimoireName: String, onItemDoubleClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    var copieOuverte by remember { mutableStateOf(false) }
    var sortsSrd by remember(character.worldId) { mutableStateOf<List<SrdEntry>>(emptyList()) }
    LaunchedEffect(character.worldId) {
        sortsSrd = SrdRepository.loadSpells(context, character.worldId.ifBlank { "donjon_et_dragon" })
    }
    // Matériel rangé dans le grimoire par l'ancienne version : rendu au sac.
    LaunchedEffect(character.id, character.grimoireContents.isNotEmpty()) {
        if (character.grimoireContents.isNotEmpty()) GameState.rendreContenuGrimoire(character.id)
    }
    fun niveau(e: SrdEntry) = ArsenalPersonnage.niveauDepuisLibelle(e.niveauSort)

    val niveauMagicien = ArsenalPersonnage.niveauxParClasse(character).entries
        .firstOrNull { it.key.equals("Magicien", ignoreCase = true) }?.value ?: 0
    val sortsDuGrimoire = character.spells.filter { s ->
        niveauMagicien == 0 || (character.spellClasses[s] ?: character.characterClass).equals("Magicien", ignoreCase = true)
    }
    val entrees = sortsDuGrimoire.map { nom -> nom to sortsSrd.firstOrNull { ArsenalPersonnage.memeSort(it.name, nom) } }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SheetSurfaceLight,
        border = BorderStroke(1.dp, CouleurGrimoire),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onItemDoubleClick(grimoireName) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = CouleurGrimoire)
                Spacer(Modifier.width(8.dp))
                Text(grimoireName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SheetTextPrimary, modifier = Modifier.weight(1f))
                Text("${sortsDuGrimoire.size} sort(s)", style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary)
            }

            HorizontalDivider(thickness = 0.5.dp, color = SheetBorder)
            Text("Sorts consignés :", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SheetTextSecondary)
            if (entrees.isEmpty()) Text("Le grimoire est vierge.", style = MaterialTheme.typography.bodySmall, color = SheetTextSecondary)
            entrees.groupBy { (_, e) -> e?.let(::niveau) ?: 0 }.toSortedMap().forEach { (n, sorts) ->
                Text(if (n == 0) "Sorts mineurs" else "Niveau $n", style = MaterialTheme.typography.labelMedium, color = CouleurGrimoire)
                sorts.forEach { (nom, e) ->
                    if (e != null) SortApercuCarte(e)
                    else Text(nom, style = MaterialTheme.typography.bodyMedium, color = SheetTextPrimary)
                }
            }

            if (niveauMagicien > 0) {
                OutlinedButton(onClick = { copieOuverte = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Recopier un sort")
                }
            }
        }
    }

    if (copieOuverte) {
        // Sorts de Magicien d'un niveau que le personnage peut préparer (plus haut emplacement
        // d'un lanceur complet), pas encore dans le grimoire.
        val niveauMax = ((niveauMagicien + 1) / 2).coerceIn(1, 9)
        val candidats = sortsSrd.filter { e ->
            niveau(e) in 1..niveauMax && e.sortDeMagicien() && !ArsenalPersonnage.sortConnu(character, e.name)
        }.sortedWith(compareBy({ niveau(it) }, { it.name }))
        CopieSortDialog(character, candidats, niveauMax, ::niveau) { copieOuverte = false }
    }
}

/**
 * Recopier un sort en deux temps : choix du sort, puis choix du matériel dans l'inventaire —
 * matériel de calligraphe et encre (non consommés) et un parchemin par niveau du sort (consommés).
 */
@Composable
private fun CopieSortDialog(
    character: Character,
    candidats: List<SrdEntry>,
    niveauMax: Int,
    niveau: (SrdEntry) -> Int,
    onDismiss: () -> Unit,
) {
    var choisi by remember { mutableStateOf<SrdEntry?>(null) }
    val filtre = rememberFiltreSorts()
    var etapeMateriel by remember { mutableStateOf(false) }
    var calligraphie by remember { mutableStateOf<String?>(null) }
    var encre by remember { mutableStateOf<String?>(null) }
    // Parchemins choisis : nom → nombre d'exemplaires.
    var parchemins by remember { mutableStateOf(mapOf<String, Int>()) }
    var message by remember { mutableStateOf<String?>(null) }

    // Inventaire (sac, objets portés, extérieur du sac) avec le nombre d'exemplaires.
    val inventaire = (character.backpackItems + character.equippedItems + character.backpackExteriorSlots.values)
        .groupingBy { it }.eachCount()
    val outils = inventaire.keys.filter { ArmorRules.isCalligraphie(it) }
    val encres = inventaire.keys.filter { ArmorRules.isEncre(it) }
    val feuilles = inventaire.filterKeys { ArmorRules.isParchemin(it) }
    val sort = choisi
    val pages = sort?.let { niveau(it).coerceAtLeast(1) } ?: 0
    val totalParchemins = parchemins.values.sum()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (etapeMateriel && sort != null) "Recopier ${sort.name}" else "Recopier un sort") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!etapeMateriel || sort == null) {
                    Text(
                        "Sort de Magicien de niveau $niveauMax au plus, trouvé sur un parchemin ou dans un autre grimoire. " +
                            "Coût par niveau du sort : 2 heures, 50 po et une feuille de parchemin.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (candidats.isEmpty()) Text("Aucun nouveau sort à recopier.", style = MaterialTheme.typography.bodySmall)
                    FiltreSortsBarre(filtre, candidats)
                    filtre.appliquer(candidats).forEach { e ->
                        SortApercuCarte(
                            sort = e,
                            selectionne = choisi == e,
                            onSelection = { choisi = if (choisi == e) null else e; parchemins = emptyMap(); message = null },
                            mention = "— ${niveau(e) * 50} po, ${niveau(e) * 2} h",
                        )
                    }
                } else {
                    Text(
                        "Niveau ${niveau(sort)} : ${pages * 2} heures, ${pages * 50} po (vous avez ${character.gold} po). " +
                            "Choisissez le matériel dans votre inventaire.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SectionMateriel("Matériel de calligraphe (conservé)", outils, "Aucun matériel de calligraphe dans l'inventaire.") { nom ->
                        FilterChip(selected = calligraphie == nom, onClick = { calligraphie = if (calligraphie == nom) null else nom }, label = { Text(nom) })
                    }
                    SectionMateriel("Encre (conservée)", encres, "Aucune encre dans l'inventaire.") { nom ->
                        FilterChip(selected = encre == nom, onClick = { encre = if (encre == nom) null else nom }, label = { Text(nom) })
                    }
                    Text(
                        "Parchemins consommés : $totalParchemins/$pages",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalParchemins == pages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    if (feuilles.isEmpty()) {
                        Text("Aucun parchemin dans l'inventaire.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    feuilles.forEach { (nom, dispo) ->
                        val n = parchemins[nom] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$nom ($dispo)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(enabled = n > 0, onClick = { parchemins = parchemins + (nom to n - 1) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Un de moins")
                            }
                            Text("$n", fontWeight = FontWeight.Bold)
                            IconButton(enabled = n < dispo && totalParchemins < pages, onClick = { parchemins = parchemins + (nom to n + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "Un de plus")
                            }
                        }
                    }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            if (!etapeMateriel || sort == null) {
                Button(enabled = sort != null, onClick = { etapeMateriel = true; message = null }) { Text("Choisir le matériel") }
            } else {
                Button(
                    enabled = calligraphie != null && encre != null && totalParchemins == pages,
                    onClick = {
                        val feuillesChoisies = parchemins.flatMap { (nom, n) -> List(n) { nom } }
                        message = GameState.recopierSortDansGrimoire(character.id, sort.name, niveau(sort), calligraphie, encre, feuillesChoisies)
                        if (message == null) onDismiss()
                    },
                ) { Text("Recopier") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (etapeMateriel) etapeMateriel = false else onDismiss() }) {
                Text(if (etapeMateriel) "Retour" else "Annuler")
            }
        },
    )
}

/** Choix d'un matériel parmi ceux de l'inventaire (puces), ou message s'il n'y en a aucun. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionMateriel(titre: String, options: List<String>, siVide: String, puce: @Composable (String) -> Unit) {
    Text(titre, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    if (options.isEmpty()) {
        Text(siVide, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { options.forEach { puce(it) } }
    }
}
