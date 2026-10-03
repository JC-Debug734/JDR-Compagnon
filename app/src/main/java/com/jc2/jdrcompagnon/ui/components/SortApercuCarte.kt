package com.jc2.jdrcompagnon.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.mikepenz.markdown.m3.Markdown

/**
 * Carte d'un sort du SRD, sur le modèle des sorts de l'onglet Sorts de la fiche (SpellRow) :
 * nom, badges niveau/type, temps d'incantation et portée d'un coup d'œil, fiche complète
 * dépliable (flèche à droite). Utilisée partout où l'on CHOISIT des sorts (création, montée de
 * niveau, grimoire) pour retrouver la même lecture rapide que sur la fiche.
 *
 * [selectionne] non null ajoute une case à cocher : toucher la carte bascule la sélection
 * (si [selectionPossible] ou déjà cochée) ; sinon, toucher la carte déplie le détail.
 */
@Composable
fun SortApercuCarte(
    sort: SrdEntry,
    modifier: Modifier = Modifier,
    selectionne: Boolean? = null,
    selectionPossible: Boolean = true,
    onSelection: () -> Unit = {},
    mention: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    var deplie by remember(sort.name) { mutableStateOf(false) }
    val cliquable = selectionne == null || selectionne || selectionPossible
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (selectionne == true) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else SheetSurfaceLight.copy(alpha = 0.75f),
        border = if (selectionne == true) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = cliquable) { if (selectionne == null) deplie = !deplie else onSelection() }
                    .padding(start = if (selectionne != null) 4.dp else 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionne != null) {
                    Checkbox(checked = selectionne, enabled = cliquable, onCheckedChange = { onSelection() })
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        sort.name + (mention?.let { " $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (cliquable) SheetTextPrimary else SheetTextSecondary,
                    )
                    val badges = listOfNotNull(sort.niveauSort.ifBlank { null }, sort.typeSort.ifBlank { null })
                    if (badges.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            sort.niveauSort.takeIf { it.isNotBlank() }?.let { BadgeSort(it, SheetTextSecondary) }
                            sort.typeSort.takeIf { it.isNotBlank() }?.let { type ->
                                BadgeSort(
                                    type,
                                    when (type) {
                                        "Attaque" -> MaterialTheme.colorScheme.error
                                        "Soutien" -> MaterialTheme.colorScheme.tertiary
                                        else -> SheetTextSecondary
                                    }
                                )
                            }
                        }
                    }
                    val infos = listOfNotNull(
                        sort.tempsIncantation.takeIf { it.isNotBlank() }?.let { "Incantation : $it" },
                        sort.portee.takeIf { it.isNotBlank() }?.let { "Portée : $it" },
                    )
                    if (infos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(infos.joinToString("  •  "), style = MaterialTheme.typography.labelSmall, color = SheetTextSecondary)
                    }
                }
                IconButton(onClick = { deplie = !deplie }) {
                    Icon(
                        if (deplie) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (deplie) "Masquer le détail" else "Voir le détail du sort",
                        tint = SheetTextPrimary,
                    )
                }
            }
            if (deplie) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    sort.category.ifBlank { null }?.let { ecole ->
                        Text(
                            "École de magie : $ecole",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Markdown(content = sort.rawMarkdown.trim())
                }
            }
            actions?.let {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) { it() }
            }
        }
    }
}

@Composable
private fun BadgeSort(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
