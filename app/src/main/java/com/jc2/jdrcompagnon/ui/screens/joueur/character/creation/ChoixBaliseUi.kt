package com.jc2.jdrcompagnon.ui.screens.joueur.character.creation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.jc2.jdrcompagnon.ui.components.FiltreSortsIcone
import com.jc2.jdrcompagnon.ui.components.SortApercuCarte
import com.jc2.jdrcompagnon.ui.components.rememberFiltreSorts
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry

/**
 * Un choix balisé d'espèce (cf. [ChoixBalise]) : le trait concerné, POURQUOI ce choix est demandé
 * (texte du trait), puis ses options. Utilisé à la création (étape « Particularités d'espèce »)
 * comme à la montée de niveau, pour que tout choix soit repéré de la même façon.
 *
 * [indisponibles] : options déjà acquises ailleurs (ex. compétence déjà maîtrisée par la classe),
 * affichées grisées et non sélectionnables. [versExpertise] : compétences déjà maîtrisées qui
 * restent sélectionnables et passeront en Expertise (cf. cumulerMaitrisesCompetences).
 */
@Composable
fun ChoixBaliseCarte(
    choix: ChoixBalise,
    selection: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    indisponibles: Set<String> = emptySet(),
    versExpertise: Set<String> = emptySet(),
    // Fiches des sorts (effet "sorts") : options affichées en cartes de sort filtrables.
    sorts: List<SrdEntry> = emptyList(),
) {
    var raisonDepliee by remember(choix.id) { mutableStateOf(false) }
    // Choix de sorts : fiches correspondant aux options, affichées comme à l'étape Sorts.
    val fichesSorts = if (choix.effet == EffetBalise.SORTS && choix.impose == null && choix.enAttente == null) {
        choix.options.mapNotNull { o -> sorts.firstOrNull { it.name.equals(o, ignoreCase = true) } }
    } else emptyList()
    val filtre = rememberFiltreSorts(choix.id)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Titre avec le compteur ; un appui dessus affiche le texte du trait (sans consigne
            // ni "Pourquoi ce choix ?" permanents, jugés superflus).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⚑ ${choix.nomTrait}" + if (choix.impose == null) " (${selection.size}/${choix.nombre})" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                        .then(if (choix.raison.isNotBlank()) Modifier.clickable { raisonDepliee = !raisonDepliee } else Modifier)
                )
                // Icône de filtre (trois traits) en haut de la carte pour une liste de sorts.
                if (fichesSorts.size > 1) FiltreSortsIcone(filtre, fichesSorts, teinte = MaterialTheme.colorScheme.onSurface)
            }
            if (choix.impose == null) {
                consigne(choix)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            if (raisonDepliee && choix.raison.isNotBlank()) {
                Text(
                    choix.raison,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.clickable { raisonDepliee = false }
                )
            }
            // Valeur imposée par la source (ex. liste Clerc d'« Initié à la magie (Clerc) ») : rien à choisir.
            choix.impose?.let { imposees ->
                Text(
                    "✔ Imposé par la source : ${imposees.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                return@Column
            }
            choix.enAttente?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                return@Column
            }
            if (fichesSorts.isNotEmpty()) {
                filtre.appliquer(fichesSorts).forEach { fiche ->
                    val option = choix.options.first { it.equals(fiche.name, ignoreCase = true) }
                    val choisie = option in selection
                    val bloquee = option in indisponibles && !choisie
                    SortApercuCarte(
                        sort = fiche,
                        selectionne = choisie,
                        selectionPossible = !bloquee && (choix.nombre == 1 || selection.size < choix.nombre),
                        mention = if (bloquee) "(déjà acquis)" else null,
                        onSelection = {
                            if (!bloquee) onSelectionChange(
                                when {
                                    choisie -> selection - option
                                    choix.nombre == 1 -> listOf(option)
                                    selection.size < choix.nombre -> selection + option
                                    else -> selection
                                }
                            )
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                // Options sans fiche dans la bibliothèque : liste classique ci-dessous.
            }
            choix.options.filter { o -> fichesSorts.none { it.name.equals(o, ignoreCase = true) } }.forEach { option ->
                val choisie = option in selection
                val bloquee = option in indisponibles && !choisie
                fun basculer() {
                    if (bloquee) return
                    onSelectionChange(
                        when {
                            choisie -> selection - option
                            choix.nombre == 1 -> listOf(option)
                            selection.size < choix.nombre -> selection + option
                            else -> selection
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !bloquee) { basculer() },
                    verticalAlignment = Alignment.Top
                ) {
                    if (choix.nombre == 1) {
                        RadioButton(selected = choisie, onClick = { basculer() }, enabled = !bloquee)
                    } else {
                        Checkbox(checked = choisie, onCheckedChange = { basculer() }, enabled = !bloquee)
                    }
                    Column(modifier = Modifier.padding(top = 12.dp).weight(1f)) {
                        Text(
                            option + when {
                                bloquee -> " (déjà acquis)"
                                versExpertise.any { it.equals(option, ignoreCase = true) } -> " (déjà maîtrisée → Expertise)"
                                option.equals(choix.recommande, ignoreCase = true) -> " — recommandé"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bloquee) Color.Gray else Color.Unspecified
                        )
                        choix.descriptions[option]?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                maxLines = if (choisie) Int.MAX_VALUE else 3
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Consigne lisible selon la cible du choix ; null pour un choix générique (le compteur du titre suffit). */
private fun consigne(choix: ChoixBalise): String? {
    val n = choix.nombre
    return when (choix.effet) {
        EffetBalise.COMPETENCES -> "Choisissez $n compétence${if (n > 1) "s" else ""} à maîtriser"
        EffetBalise.DON -> "Choisissez $n don${if (n > 1) "s" else ""} d'origines"
        EffetBalise.TAILLE -> "Choisissez la catégorie de taille"
        EffetBalise.INCANTATION -> "Choisissez la caractéristique d'incantation"
        EffetBalise.OUTILS -> "Choisissez $n outil${if (n > 1) "s" else ""} à maîtriser"
        EffetBalise.SORTS -> "Choisissez $n ${choix.libelle?.removeSuffix(" choisis")?.lowercase() ?: "sort${if (n > 1) "s" else ""}"}"
        EffetBalise.EQUIPEMENT -> "Choisissez l'objet reçu"
        EffetBalise.COMPETENCES_OU_OUTILS -> "Choisissez $n compétence${if (n > 1) "s" else ""} ou outil${if (n > 1) "s" else ""} à maîtriser"
        EffetBalise.NOTE -> choix.libelle?.let { "Choisissez : ${it.removeSuffix(" choisie").lowercase()}" } ?: "Faites votre choix"
        else -> null
    }
}
