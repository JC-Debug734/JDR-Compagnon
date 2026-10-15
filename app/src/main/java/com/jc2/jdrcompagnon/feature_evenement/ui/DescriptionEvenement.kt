package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.PlainScenarioRenderer

private val BalisesLire = Regex("""\{/?lire\}""")

/** Description d'événement sans ses balises {lire}…{/lire}, sur une ligne : pour les aperçus de liste. */
fun descriptionSansBalises(texte: String): String =
    texte.replace(BalisesLire, "").replace(Regex("""\s+"""), " ").trim()

/**
 * Description complète d'un événement (ou d'une issue) : les passages {lire}…{/lire} s'affichent
 * comme dans un scénario, en bloc vert avec lecture à voix haute ; sinon, texte simple.
 */
@Composable
fun DescriptionEvenement(texte: String, modifier: Modifier = Modifier) {
    if (BalisesLire.containsMatchIn(texte)) {
        PlainScenarioRenderer(content = texte, modifier = modifier)
    } else {
        Text(texte, modifier = modifier)
    }
}

/** Aperçu d'une description dans une liste : balises retirées, [lignes] lignes au plus. */
@Composable
fun ApercuDescriptionEvenement(texte: String, lignes: Int, couleur: Color = Color.Unspecified) {
    Text(
        descriptionSansBalises(texte),
        style = MaterialTheme.typography.bodySmall,
        color = couleur,
        maxLines = lignes,
        overflow = TextOverflow.Ellipsis,
    )
}
