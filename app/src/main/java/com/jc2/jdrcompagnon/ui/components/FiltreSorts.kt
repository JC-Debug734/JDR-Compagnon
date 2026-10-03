package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry

/**
 * Filtres d'une liste de sorts à choisir (création, montée de niveau, grimoire) : recherche
 * par nom et puces niveau / type / école, repliées derrière le bouton « Filtrer » (icône à
 * traits). Un ensemble vide = pas de filtre sur ce critère.
 */
@Stable
class FiltreSortsEtat {
    var ouvert by mutableStateOf(false)
    var recherche by mutableStateOf("")
    var niveaux by mutableStateOf(setOf<String>())
    var types by mutableStateOf(setOf<String>())
    var ecoles by mutableStateOf(setOf<String>())

    val nombreActifs: Int
        get() = niveaux.size + types.size + ecoles.size + if (recherche.isNotBlank()) 1 else 0

    fun reinitialiser() {
        recherche = ""; niveaux = emptySet(); types = emptySet(); ecoles = emptySet()
    }

    fun garde(sort: SrdEntry): Boolean =
        (recherche.isBlank() || sort.name.contains(recherche.trim(), ignoreCase = true)) &&
            (niveaux.isEmpty() || sort.niveauSort in niveaux) &&
            (types.isEmpty() || sort.typeSort in types) &&
            (ecoles.isEmpty() || sort.category in ecoles)

    fun appliquer(sorts: List<SrdEntry>): List<SrdEntry> = sorts.filter(::garde)
}

@Composable
fun rememberFiltreSorts(vararg cles: Any?): FiltreSortsEtat = remember(*cles) { FiltreSortsEtat() }

/** Ordre des niveaux : "Sort mineur" puis "Niveau 1", "Niveau 2"... */
private fun cleNiveau(niveau: String): Int =
    if (niveau.contains("mineur", ignoreCase = true)) 0 else Regex("""\d+""").find(niveau)?.value?.toIntOrNull() ?: 99

/**
 * Bouton « Filtrer (N) » (+ « Réinitialiser ») : ouvre la fenêtre de filtre [FiltreSortsDialogue]
 * sans déplacer les cartes de la liste.
 */
@Composable
fun FiltreSortsBarre(
    etat: FiltreSortsEtat,
    sorts: List<SrdEntry>,
    modifier: Modifier = Modifier,
    couleurTexte: Color = SheetTextPrimary,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { etat.ouvert = true }) {
            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp), tint = couleurTexte)
            Spacer(Modifier.width(6.dp))
            Text(if (etat.nombreActifs == 0) "Filtrer" else "Filtrer (${etat.nombreActifs})", color = couleurTexte)
        }
        Spacer(Modifier.weight(1f))
        if (etat.nombreActifs > 0) {
            TextButton(onClick = { etat.reinitialiser() }) { Text("Réinitialiser", color = couleurTexte) }
        }
    }
    FiltreSortsDialogue(etat, sorts)
}

/** Icône seule (trois traits) ouvrant la fenêtre de filtre, pour un en-tête de carte. */
@Composable
fun FiltreSortsIcone(etat: FiltreSortsEtat, sorts: List<SrdEntry>, teinte: Color = SheetTextPrimary) {
    IconButton(onClick = { etat.ouvert = true }) {
        BadgedBox(badge = { if (etat.nombreActifs > 0) Badge { Text("${etat.nombreActifs}") } }) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtrer les sorts", tint = teinte)
        }
    }
    FiltreSortsDialogue(etat, sorts)
}

/**
 * Fenêtre de filtre : recherche par nom + puces. Les puces ne proposent que les valeurs
 * présentes dans [sorts] (inutile d'afficher « Niveau 1 » dans une liste de sorts mineurs) ;
 * un critère à une seule valeur n'est pas affiché. Les filtres s'appliquent en direct.
 */
@Composable
private fun FiltreSortsDialogue(etat: FiltreSortsEtat, sorts: List<SrdEntry>) {
    if (!etat.ouvert) return
    val niveaux = remember(sorts) { sorts.map { it.niveauSort }.filter { it.isNotBlank() }.distinct().sortedBy(::cleNiveau) }
    val types = remember(sorts) { sorts.map { it.typeSort }.filter { it.isNotBlank() }.distinct().sorted() }
    val ecoles = remember(sorts) { sorts.map { it.category }.filter { it.isNotBlank() }.distinct().sorted() }
    val restants = etat.appliquer(sorts).size
    AlertDialog(
        onDismissRequest = { etat.ouvert = false },
        title = { Text("Filtrer les sorts") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = etat.recherche,
                    onValueChange = { etat.recherche = it },
                    label = { Text("Nom du sort") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                GroupePuces("Niveau", niveaux, etat.niveaux) { etat.niveaux = it }
                GroupePuces("Type", types, etat.types) { etat.types = it }
                GroupePuces("École", ecoles, etat.ecoles) { etat.ecoles = it }
            }
        },
        confirmButton = { TextButton(onClick = { etat.ouvert = false }) { Text("Voir $restants sort${if (restants > 1) "s" else ""}") } },
        dismissButton = {
            if (etat.nombreActifs > 0) TextButton(onClick = { etat.reinitialiser() }) { Text("Réinitialiser") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupePuces(
    titre: String,
    valeurs: List<String>,
    selection: Set<String>,
    onChange: (Set<String>) -> Unit,
) {
    if (valeurs.size < 2) return
    Text(titre, style = MaterialTheme.typography.labelMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        valeurs.forEach { v ->
            val choisie = v in selection
            FilterChip(
                selected = choisie,
                onClick = { onChange(if (choisie) selection - v else selection + v) },
                label = { Text(v) },
            )
        }
    }
}
