package com.jc2.jdrcompagnon.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.screens.mj.CustomBook
import com.jc2.jdrcompagnon.ui.screens.mj.customBooksDir
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentSummary
import java.io.File

/**
 * Type de contenu d'un import, pour filtrer des imports mixtes (un même livre peut apporter
 * sous-classes, sorts et monstres) : écran serveur (imports partagés) et outil Import.
 */
enum class CategorieContenu(val libelle: String) {
    CLASSES("Classes"),
    SOUS_CLASSES("Sous-classes"),
    ESPECES("Espèces"),
    HISTORIQUES("Historiques"),
    DONS("Dons"),
    SORTS("Sorts"),
    OBJETS("Objets"),
    MONSTRES("Monstres"),
    PNJ("PNJ"),
    REGLES("Règles"),
    SCENARIOS("Scénarios"),
    CAMPAGNES("Campagnes"),
    FICHES("Fiches"),
    UNIVERS("Univers"),
    MUSIQUES("Musiques"),
    LIEUX("Lieux"),
    EPREUVES("Épreuves"),
    EVENEMENTS("Événements"),
    ETATS("Illustrations d'états"),
    DOCUMENTS("Documents"),
}

/** Nombre d'entrées de chaque type dans un livre (types absents omis). */
fun CustomContentSummary.categories(): Map<CategorieContenu, Int> = buildMap {
    fun mettre(c: CategorieContenu, n: Int) { if (n > 0) put(c, n) }
    mettre(CategorieContenu.CLASSES, classes)
    mettre(CategorieContenu.SOUS_CLASSES, sousClasses)
    mettre(CategorieContenu.ESPECES, especes)
    mettre(CategorieContenu.HISTORIQUES, historiques)
    mettre(CategorieContenu.DONS, dons)
    mettre(CategorieContenu.SORTS, spells)
    mettre(CategorieContenu.OBJETS, equipment)
    mettre(CategorieContenu.MONSTRES, monsters)
    mettre(CategorieContenu.PNJ, pnjs)
    mettre(CategorieContenu.REGLES, rules)
}

/**
 * Contenu d'un livre de la bibliothèque, lu dans son fichier. Un livre non markdown (PDF...)
 * ou sans entrée reconnue est un simple document.
 */
internal fun categoriesDuLivre(context: Context, livre: CustomBook): Map<CategorieContenu, Int> {
    if (!livre.fileName.endsWith(".md", ignoreCase = true)) return mapOf(CategorieContenu.DOCUMENTS to 1)
    val texte = runCatching { File(customBooksDir(context), livre.fileName).readText() }.getOrNull()
        ?: return mapOf(CategorieContenu.DOCUMENTS to 1)
    return CustomContentParser.parse(texte).summary.categories().ifEmpty { mapOf(CategorieContenu.DOCUMENTS to 1) }
}

/** Résumé lisible : « 3 sous-classes · 12 sorts ». */
fun resumeCategories(categories: Map<CategorieContenu, Int>): String =
    CategorieContenu.entries.filter { it in categories }.joinToString(" · ") { c ->
        if (c == CategorieContenu.DOCUMENTS) c.libelle else "${categories[c]} ${c.libelle.lowercase()}"
    }

/** Vrai si [categories] contient l'un des types choisis ; aucun type choisi = tout passe. */
fun correspondAuFiltre(categories: Collection<CategorieContenu>, filtre: Set<CategorieContenu>): Boolean =
    filtre.isEmpty() || categories.any { it in filtre }

/**
 * Puces de filtre par type de contenu : seuls les types présents dans [disponibles] sont
 * proposés ; plusieurs peuvent être choisis (un import passe s'il contient l'un d'eux).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltreCategoriesContenu(
    disponibles: Set<CategorieContenu>,
    selection: Set<CategorieContenu>,
    onSelectionChange: (Set<CategorieContenu>) -> Unit,
    modifier: Modifier = Modifier,
    couleurTexte: Color = Color.Unspecified,
) {
    if (disponibles.size < 2) return
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = selection.isEmpty(),
            onClick = { onSelectionChange(emptySet()) },
            label = { Text("Tout", color = couleurTexte) },
            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
        )
        CategorieContenu.entries.filter { it in disponibles }.forEach { c ->
            val choisi = c in selection
            FilterChip(
                selected = choisi,
                onClick = { onSelectionChange(if (choisi) selection - c else selection + c) },
                label = { Text(c.libelle, color = couleurTexte) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer),
            )
        }
    }
}
