package com.jc2.jdrcompagnon.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Fichier SharedPreferences de l'état replié/déplié de toutes les cartes réductibles. */
private const val COLLAPSIBLE_SECTIONS_PREFS = "collapsible_sections"

/**
 * Carte de section réductible, au fond translucide commun à l'app (surface à 75 %) et au texte
 * blanc. Un appui sur l'en-tête (titre + flèche) la replie ou la déplie.
 *
 * L'état est persisté (SharedPreferences) sous [ownerKey] + [title] : [ownerKey] identifie
 * l'élément édité (ex. "campagne:<id>", "boutique:<id>"), pour que chaque campagne / boutique /
 * environnement garde sa propre configuration, même en changeant d'écran ou en relançant l'app.
 * [headerActions] s'affiche à droite du titre (boutons d'ajout...), avant la flèche. [count] : nombre
 * d'éléments de la section, affiché en pastille quand elle est repliée.
 *
 * [persistent] = false : l'état n'est pas enregistré, la carte s'ouvre selon [defaultExpanded] à
 * chaque ouverture de l'écran (conservé seulement au retour d'un écran ouvert depuis celui-ci).
 */
@Composable
fun CollapsibleSectionCard(
    ownerKey: String,
    title: String,
    modifier: Modifier = Modifier,
    headerActions: @Composable RowScope.() -> Unit = {},
    count: Int? = null,
    persistent: Boolean = true,
    defaultExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(COLLAPSIBLE_SECTIONS_PREFS, Context.MODE_PRIVATE) }
    val key = "$ownerKey:$title"
    var expanded by rememberSaveable(key) {
        mutableStateOf(if (persistent) prefs.getBoolean(key, defaultExpanded) else defaultExpanded)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = !expanded
                        if (persistent) prefs.edit().putBoolean(key, expanded).apply()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (expanded) headerActions()
                else if (count != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        contentColor = Color.White,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Réduire" else "Déplier",
                )
            }
            if (expanded) content()
        }
    }
}

/** Oublie l'état des cartes d'un élément supprimé (même [ownerKey] que ses cartes). */
fun forgetCollapsibleSections(context: Context, ownerKey: String) {
    val prefs = context.getSharedPreferences(COLLAPSIBLE_SECTIONS_PREFS, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    prefs.all.keys.filter { it.startsWith("$ownerKey:") }.forEach { editor.remove(it) }
    editor.apply()
}
