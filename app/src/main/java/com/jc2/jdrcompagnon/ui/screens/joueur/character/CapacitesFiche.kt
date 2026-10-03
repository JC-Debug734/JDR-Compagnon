package com.jc2.jdrcompagnon.ui.screens.joueur.character

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.components.SheetTextPrimary
import com.jc2.jdrcompagnon.ui.components.SheetTextSecondary

/** Vert des choix faits (valeurs retenues à la création / montée de niveau). */
val CouleurChoix = Color(0xFF4ADE80)

/**
 * Une capacité de la fiche (trait d'espèce, aptitude de classe, don) : [titre] affiché sur
 * la ligne repliée, [choix] = valeurs retenues (partie après " — " du titre), mises en vert
 * dans la description.
 */
data class CapaciteFiche(val titre: String, val choix: List<String>, val description: String)

/**
 * Début d'une nouvelle capacité : "Nom (contexte)[ [niveau N]][ — choix][ : description]"
 * (cf. entreeFicheAvecChoix, versCharacter, LevelUpDialog), ou "Don d'historique : X".
 * Le nom commence par une majuscule, sans phrase (". "/", ") avant la parenthèse — ce qui
 * écarte les paragraphes de description qui contiendraient une parenthèse.
 */
private val REGEX_DEBUT_CAPACITE = Regex(
    """^\p{Lu}[^|#*\n.,:]{0,80}\([^()\n]{1,80}\)(?:\s*\[niveau \d+])?(?:\s—\s[^\n]*?)?(?:\s:\s.*|\s*)$"""
)

private fun estDebutCapacite(ligne: String): Boolean =
    ligne.startsWith("Don d'historique : ") || REGEX_DEBUT_CAPACITE.matches(ligne)

/**
 * Découpe le texte d'une section (traits, capacités, dons) en capacités. Les entrées sont
 * séparées par une ligne vide, mais la description d'une capacité peut elle-même contenir
 * des lignes vides (paragraphes, sous-titres "###", tables du SRD) : un bloc ne démarre une
 * nouvelle capacité que si sa première ligne a la forme d'un titre (cf. estDebutCapacite),
 * sinon il est rattaché à la capacité précédente — au lieu de devenir une carte orpheline.
 */
fun parserCapacitesFiche(texte: String): List<CapaciteFiche> {
    val blocs = texte.split(Regex("""\n\s*\n""")).map { it.trim('\n', '\r') }.filter { it.isNotBlank() }
    val entrees = mutableListOf<MutableList<String>>()
    blocs.forEach { bloc ->
        if (entrees.isEmpty() || estDebutCapacite(bloc.lineSequence().first().trim())) entrees += mutableListOf(bloc)
        else entrees.last() += bloc
    }
    return entrees.map { morceaux ->
        val premier = morceaux.first()
        val premiereLigne = premier.lineSequence().first().trim()
        val idx = premiereLigne.indexOf(" : ")
        val titre = if (idx < 0) premiereLigne else premiereLigne.substring(0, idx).trim()
        val suiteLigne = if (idx < 0) "" else premiereLigne.substring(idx + 3).trim()
        val suiteBloc = premier.substringAfter("\n", "").trim('\n', '\r')
        val description = (listOf(listOf(suiteLigne, suiteBloc).filter { it.isNotBlank() }.joinToString("\n")) + morceaux.drop(1))
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
        val choix = titre.substringAfter(" — ", "")
            .split(",")
            .map { it.trim() }
            .filter { it.length >= 3 }
        CapaciteFiche(titre, choix, description)
    }
}

/**
 * Rendu de la description d'une capacité : sous-titres ("###"), gras/italique, listes à
 * puces et tables du SRD (une carte par ligne de table, lisible sur téléphone plutôt qu'un
 * tableau aux colonnes écrasées). Les choix faits sont en vert : lignes de choix (avant
 * "Pourquoi ce choix"), valeurs retenues dans le texte, et ligne de table choisie encadrée.
 */
@Composable
fun CapaciteDescription(description: String, choix: List<String>) {
    val lignes = remember(description) { description.lines() }
    // Les lignes de choix (entreeFicheAvecChoix) précèdent "Pourquoi ce choix : ...".
    val finChoix = lignes.indexOfFirst { it.startsWith("Pourquoi ce choix") }
    val regexChoix = remember(choix) { regexValeurs(choix) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var i = 0
        while (i < lignes.size) {
            val ligne = lignes[i].trimEnd()
            val brut = ligne.trim()
            when {
                brut.isEmpty() -> Spacer(modifier = Modifier.height(4.dp))
                brut.startsWith("|") -> {
                    val table = mutableListOf<String>()
                    while (i < lignes.size && lignes[i].trim().startsWith("|")) table += lignes[i++].trim()
                    TableCapacite(table, regexChoix)
                    continue
                }
                i < finChoix -> Text(
                    // Ligne de choix fait : entièrement en vert.
                    text = inline(brut, null, base = SpanStyle(color = CouleurChoix, fontWeight = FontWeight.Bold)),
                    style = MaterialTheme.typography.bodyMedium
                )
                i == finChoix || brut.startsWith("Choix en jeu") -> Text(
                    text = inline(brut, regexChoix, base = SpanStyle(fontStyle = FontStyle.Italic)),
                    style = MaterialTheme.typography.bodySmall,
                    color = SheetTextSecondary
                )
                brut.startsWith("#") -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = inline(brut.trimStart('#').trim(), regexChoix),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SheetTextPrimary
                    )
                }
                Regex("""^[-*•]\s+""").containsMatchIn(brut) && !brut.startsWith("**") -> Row(modifier = Modifier.padding(start = 8.dp)) {
                    Text("•", color = SheetTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = inline(brut.replaceFirst(Regex("""^[-*•]\s+"""), ""), regexChoix),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SheetTextPrimary
                    )
                }
                else -> Text(
                    text = inline(brut, regexChoix),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SheetTextPrimary
                )
            }
            i++
        }
    }
}

/** Table markdown : une carte par ligne, en-têtes rappelés ; ligne choisie encadrée en vert. */
@Composable
private fun TableCapacite(lignes: List<String>, regexChoix: Regex?) {
    fun cellules(l: String) = l.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
    val lignesUtiles = lignes.filterNot { Regex("""^\|?[\s:|-]+\|?$""").matches(it) }
    if (lignesUtiles.isEmpty()) return
    val entetes = cellules(lignesUtiles.first())
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        lignesUtiles.drop(1).forEach { l ->
            val c = cellules(l)
            val choisie = regexChoix != null && c.firstOrNull()?.let { regexChoix.containsMatchIn(it) } == true
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (choisie) CouleurChoix.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                border = BorderStroke(if (choisie) 1.5.dp else 1.dp, if (choisie) CouleurChoix else Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = inline(c.firstOrNull().orEmpty() + if (choisie) "  ✓ choisi" else "", null),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (choisie) CouleurChoix else SheetTextPrimary
                    )
                    c.drop(1).forEachIndexed { idx, valeur ->
                        if (valeur.isBlank()) return@forEachIndexed
                        Text(
                            text = buildAnnotatedString {
                                entetes.getOrNull(idx + 1)?.takeIf { it.isNotBlank() }?.let {
                                    pushStyle(SpanStyle(color = SheetTextSecondary, fontWeight = FontWeight.SemiBold))
                                    append("$it : ")
                                    pop()
                                }
                                append(inline(valeur, regexChoix))
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetTextPrimary
                        )
                    }
                }
            }
        }
    }
}

/** Regex des valeurs choisies, en mots entiers (insensible à la casse), ou null si aucune. */
private fun regexValeurs(choix: List<String>): Regex? {
    val valeurs = choix.filter { it.length >= 3 }.sortedByDescending { it.length }
    if (valeurs.isEmpty()) return null
    return Regex(
        valeurs.joinToString("|", prefix = """(?<![\p{L}\p{N}])(?:""", postfix = """)(?![\p{L}\p{N}])""") { Regex.escape(it) },
        RegexOption.IGNORE_CASE
    )
}

/**
 * Markdown en ligne (**gras**, *italique*) -> AnnotatedString ; les astérisques orphelins
 * (gras non refermé du SRD) sont retirés. Les valeurs choisies ([regexChoix]) passent en vert.
 */
private fun inline(texte: String, regexChoix: Regex?, base: SpanStyle? = null): AnnotatedString {
    val resultat = buildAnnotatedString {
        base?.let { pushStyle(it) }
        val jetons = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")
        var pos = 0
        jetons.findAll(texte).forEach { m ->
            append(texte.substring(pos, m.range.first).replace("*", ""))
            val gras = m.groups[1]?.value
            pushStyle(if (gras != null) SpanStyle(fontWeight = FontWeight.Bold) else SpanStyle(fontStyle = FontStyle.Italic))
            append((gras ?: m.groupValues[2]).replace("*", ""))
            pop()
            pos = m.range.last + 1
        }
        append(texte.substring(pos).replace("*", ""))
        base?.let { pop() }
    }
    if (regexChoix == null) return resultat
    return buildAnnotatedString {
        append(resultat)
        regexChoix.findAll(resultat.text).forEach { m ->
            addStyle(SpanStyle(color = CouleurChoix, fontWeight = FontWeight.Bold), m.range.first, m.range.last + 1)
        }
    }
}

/** Markdown en ligne (**gras**, *italique*) d'un court texte SRD -> AnnotatedString affichable. */
fun markdownEnLigne(texte: String): AnnotatedString = inline(texte, null)
