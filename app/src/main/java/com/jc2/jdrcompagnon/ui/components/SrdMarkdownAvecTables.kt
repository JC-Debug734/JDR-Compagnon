package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor

/**
 * Rendu markdown d'une entrée SRD où les tables `| a | b |` sont remplacées par des cartes
 * lisibles sur téléphone (le rendu table du Markdown de mikepenz écrase les colonnes et
 * coupe le texte hors de l'écran).
 *
 * - Table de progression (1re colonne « Niveau ») : une carte par niveau, pastille du niveau,
 *   aptitudes gagnées en titre, autres colonnes en puces ; les valeurs qui changent par
 *   rapport au niveau précédent sont mises en évidence.
 * - Autre table : une carte par ligne, 1re cellule en titre, puis « En-tête : valeur ».
 */
@Composable
fun SrdMarkdownAvecTables(markdown: String, modifier: Modifier = Modifier) {
    val blocs = remember(markdown) { decouperBlocs(markdown) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocs.forEach { bloc ->
            when (bloc) {
                is BlocMd.Texte -> Markdown(content = bloc.contenu, colors = markdownColor(text = Color.White))
                is BlocMd.Table ->
                    if (bloc.estProgression()) TableProgression(bloc) else TableEnCartes(bloc)
            }
        }
    }
}

private sealed interface BlocMd {
    data class Texte(val contenu: String) : BlocMd
    data class Table(val entetes: List<String>, val lignes: List<List<String>>) : BlocMd
}

private val separateurTable = Regex("""^\|?[\s:|-]+\|?$""")

private fun cellules(ligne: String) =
    ligne.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }

/** Découpe le markdown en blocs de texte et blocs de table (en-tête + séparateur requis). */
private fun decouperBlocs(markdown: String): List<BlocMd> {
    val lignes = markdown.lines()
    val blocs = mutableListOf<BlocMd>()
    val texte = StringBuilder()
    fun viderTexte() {
        if (texte.isNotBlank()) blocs += BlocMd.Texte(texte.toString().trim('\n'))
        texte.clear()
    }
    var i = 0
    while (i < lignes.size) {
        val ligne = lignes[i]
        val estTable = ligne.trim().startsWith("|") &&
            lignes.getOrNull(i + 1)?.trim()?.let { it.startsWith("|") && separateurTable.matches(it) } == true
        if (!estTable) {
            texte.append(ligne).append('\n')
            i++
            continue
        }
        viderTexte()
        val entetes = cellules(ligne)
        i += 2
        val donnees = mutableListOf<List<String>>()
        while (i < lignes.size && lignes[i].trim().startsWith("|")) {
            donnees += cellules(lignes[i])
            i++
        }
        blocs += BlocMd.Table(entetes, donnees)
    }
    viderTexte()
    return blocs
}

private fun BlocMd.Table.estProgression() =
    entetes.firstOrNull()?.startsWith("Niveau", ignoreCase = true) == true &&
        lignes.isNotEmpty() && lignes.all { it.firstOrNull()?.toIntOrNull() != null }

private fun estVide(valeur: String) = valeur.isBlank() || valeur == "—" || valeur == "-" || valeur == "0"

/** Retire le gras/italique markdown (`**x**`, `*x*`) d'une cellule. */
private fun sansMarkdown(valeur: String) = valeur.replace("**", "").replace(Regex("""(?<!\w)\*|\*(?!\w)"""), "")

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TableProgression(table: BlocMd.Table) {
    // Colonne des aptitudes : celle dont l'en-tête parle d'aptitudes, sinon aucune
    // (ex. table d'emplacements de sort : uniquement des nombres).
    val idxAptitudes = table.entetes.indexOfFirst { it.contains("Aptitude", ignoreCase = true) }
    val primaire = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        table.lignes.forEachIndexed { rang, ligne ->
            val precedente = table.lignes.getOrNull(rang - 1)
            val aptitudes = ligne.getOrNull(idxAptitudes)?.let(::sansMarkdown).orEmpty()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(primaire.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ligne.first(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (idxAptitudes >= 0) {
                            if (estVide(aptitudes)) {
                                Text(
                                    text = "Aucune nouvelle aptitude",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = Color.White.copy(alpha = 0.6f),
                                )
                            } else {
                                Text(
                                    text = aptitudes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ligne.forEachIndexed { idx, valeur ->
                                if (idx == 0 || idx == idxAptitudes || estVide(valeur)) return@forEachIndexed
                                val change = precedente != null && precedente.getOrNull(idx) != valeur
                                PuceStat(
                                    libelle = libelleCourt(table.entetes.getOrNull(idx).orEmpty()),
                                    valeur = sansMarkdown(valeur),
                                    miseEnAvant = change,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** En-têtes longs raccourcis pour tenir dans une puce. */
private fun libelleCourt(entete: String) = when (entete) {
    "Bonus de maîtrise" -> "Maîtrise"
    else -> entete
}

@Composable
private fun PuceStat(libelle: String, valeur: String, miseEnAvant: Boolean) {
    val primaire = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (miseEnAvant) primaire.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
        border = if (miseEnAvant) BorderStroke(1.dp, primaire) else null,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f))) { append("$libelle ") }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append(valeur)
                    if (miseEnAvant) append(" ▲")
                }
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun TableEnCartes(table: BlocMd.Table) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        table.lignes.forEach { ligne ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = sansMarkdown(ligne.firstOrNull().orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    ligne.drop(1).forEachIndexed { idx, valeur ->
                        if (valeur.isBlank()) return@forEachIndexed
                        Text(
                            text = buildAnnotatedString {
                                table.entetes.getOrNull(idx + 1)?.takeIf { it.isNotBlank() }?.let {
                                    withStyle(SpanStyle(color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)) {
                                        append("$it : ")
                                    }
                                }
                                append(sansMarkdown(valeur))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
