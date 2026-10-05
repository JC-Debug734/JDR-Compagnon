package com.jc2.jdrcompagnon.ui.screens.mj.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry

// Palette "manuel des monstres" : parchemin, rouge sombre des titres, liseré orangé.
private val Parchemin = Color(0xFFFDF1DC)
private val RougeTitre = Color(0xFF7A200D)
private val Lisere = Color(0xFFE69A28)
private val Encre = Color(0xFF1F1A17)

/** Une caractéristique du bloc : valeur, modificateur et bonus de jet de sauvegarde. */
data class CaracMonstre(val label: String, val valeur: String, val mod: String, val sauvegarde: String)

/** Une capacité (trait, action...) : nom en gras-italique, ou null pour un paragraphe libre. */
data class CapaciteMonstre(val nom: String?, val texte: String)

/** Section du corps ("Traits", "Actions"...) et ses capacités. */
data class SectionMonstre(val titre: String, val capacites: List<CapaciteMonstre>)

internal object MonsterStatBlockParser {
    private val caracRegex = Regex("""(For|Dex|Con|Int|Sag|Cha)\s+(\d+)\s+([+\-−–]\s?\d+)\s+([+\-−–]\s?\d+)""")
    // "Nom. Description" : nom court sans ":" (sinon c'est un paragraphe, ex. "Utilisations : 3.").
    private val capaciteRegex = Regex("""^([^.:]{1,70})\.\s+(.+)$""")

    fun caracteristiques(raw: String): List<CaracMonstre> =
        caracRegex.findAll(raw).map { m ->
            CaracMonstre(
                label = m.groupValues[1].uppercase(),
                valeur = m.groupValues[2],
                mod = m.groupValues[3].normaliserSigne(),
                sauvegarde = m.groupValues[4].normaliserSigne(),
            )
        }.toList()

    /** "17 Initiative +7 (17)" → ("17", "+7 (17)"). */
    fun caEtInitiative(raw: String): Pair<String, String?> {
        val index = raw.indexOf("Initiative")
        return if (index < 0) raw.trim() to null
        else raw.substring(0, index).trim() to raw.substring(index + "Initiative".length).trim().ifBlank { null }
    }

    fun sections(body: String): List<SectionMonstre> {
        val blocs = body.split(Regex("""(?m)^## """))
        return blocs.mapIndexedNotNull { index, bloc ->
            val lignes = bloc.lines()
            // Texte éventuel avant la première section : sans titre.
            val titre = if (index == 0) "" else lignes.first().trim()
            val contenu = if (index == 0) lignes else lignes.drop(1)
            val capacites = contenu.map { it.trim() }.filter { it.isNotEmpty() }.map { ligne ->
                capaciteRegex.find(ligne)?.let { CapaciteMonstre(it.groupValues[1].trim(), it.groupValues[2].trim()) }
                    ?: CapaciteMonstre(null, ligne)
            }
            if (titre.isEmpty() && capacites.isEmpty()) null else SectionMonstre(titre, capacites)
        }
    }

    private fun String.normaliserSigne() = replace('−', '-').replace('–', '-').replace(" ", "")
}

// Mots-clés mis en italique dans les descriptions, comme dans le manuel.
private val motsClesItalique = Regex(
    """(Corps à corps ou distance :|Corps à corps :|Distance :|Touché :|Échec ou réussite :|Échec :|Réussite :|Raté :|JS [\p{L}]+ :)"""
)

// Noms des états des règles (Agrippé, À terre…), mis en gras rouge dans le texte des capacités.
private val nomsEtats = Regex(
    "(?<!\\p{L})(" + Etats.fiches.filter { it.officiel }.map { it.condition.label }
        .sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } + ")(?!\\p{L})"
)

private fun texteCapacite(capacite: CapaciteMonstre): AnnotatedString = buildAnnotatedString {
    capacite.nom?.let {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append("$it. ") }
    }
    // Mots-clés en italique et noms d'états en gras, dans l'ordre du texte.
    val reperes = (motsClesItalique.findAll(capacite.texte).map { it to false } +
        nomsEtats.findAll(capacite.texte).map { it to true })
        .sortedBy { it.first.range.first }
    var dernier = 0
    reperes.forEach { (m, estEtat) ->
        if (m.range.first < dernier) return@forEach
        append(capacite.texte.substring(dernier, m.range.first))
        val style = if (estEtat) SpanStyle(fontWeight = FontWeight.Bold, color = RougeTitre) else SpanStyle(fontStyle = FontStyle.Italic)
        withStyle(style) { append(m.value) }
        dernier = m.range.last + 1
    }
    append(capacite.texte.substring(dernier))
}

/**
 * États reconnus dans une capacité, et ce qu'en fait le combat : la section « Actions » est jouée
 * par l'application (état appliqué d'office si l'attaque touche, ou proposé au MJ sur JS raté) ;
 * les autres sections (traits, réactions, actions légendaires) restent à appliquer par le MJ.
 */
@Composable
private fun EtatsInfliges(capacite: CapaciteMonstre, section: String) {
    val infliges = Etats.infligesPar(capacite.texte)
    if (infliges.isEmpty()) return
    val joueeEnCombat = section.equals("Actions", ignoreCase = true)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = RougeTitre)) { append("⚠ Inflige : ") }
            append(infliges.joinToString(" · ") { inflige ->
                inflige.libelle + when {
                    !joueeEnCombat -> ""
                    inflige.surEchecJs -> " — sur JS raté"
                    else -> " — si l'attaque touche"
                }
            })
            if (!joueeEnCombat) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(" (à appliquer par le MJ)") }
            }
        },
        color = Encre,
        fontSize = 12.sp,
        modifier = Modifier
            .padding(top = 2.dp)
            .background(RougeTitre.copy(alpha = 0.08f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Filet rouge effilé vers la droite qui sépare les blocs, comme dans le manuel. */
@Composable
private fun FiletEffile() {
    Canvas(modifier = Modifier.fillMaxWidth().height(5.dp).padding(vertical = 0.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, RougeTitre)
    }
}

@Composable
private fun LigneLabel(label: String, valeur: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = RougeTitre)) { append("$label ") }
            append(valeur)
        },
        color = Encre,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 1.dp),
    )
}


/**
 * Profil de monstre mis en page comme un bloc de stats du manuel (parchemin, titre rouge en
 * petites capitales, filets effilés, tableau des caractéristiques). Pour une entrée sans
 * champs structurés (voir [aUnBlocDeStats]), l'appelant affiche le markdown brut.
 */
@Composable
fun MonsterStatBlock(monster: SrdEntry, modifier: Modifier = Modifier) {
    val f = monster.fields
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Parchemin)
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Lisere))
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                monster.name,
                color = RougeTitre,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp"),
            )
            val sousTitre = listOfNotNull(
                f["Type"]?.ifBlank { null }?.let { type ->
                    f["Taille"]?.ifBlank { null }?.let { "$type de taille $it" } ?: type
                },
                f["Alignement"]?.ifBlank { null }?.lowercase(),
            ).joinToString(", ")
            if (sousTitre.isNotBlank()) {
                Text(sousTitre, color = Encre, fontStyle = FontStyle.Italic, fontSize = 14.sp)
            }

            Espace(); FiletEffile(); Espace()
            f["CA"]?.let { raw ->
                val (ca, init) = MonsterStatBlockParser.caEtInitiative(raw)
                LigneLabel("Classe d'armure", ca)
                init?.let { LigneLabel("Initiative", it) }
            }
            f["Pv"]?.let { LigneLabel("Points de vie", it) }
            f["Vitesse"]?.let { LigneLabel("Vitesse", it) }

            val caracs = f["Caractéristiques"]?.let(MonsterStatBlockParser::caracteristiques).orEmpty()
            if (caracs.isNotEmpty()) {
                Espace(); FiletEffile(); Espace()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    caracs.forEach { c ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(c.label, color = RougeTitre, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${c.valeur} (${c.mod})", color = Encre, fontSize = 13.sp, textAlign = TextAlign.Center)
                            if (c.sauvegarde != c.mod) {
                                Text("JS ${c.sauvegarde}", color = Encre.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            val details = listOf(
                "Compétences", "Vulnérabilités", "Résistances", "Immunités", "Équipement", "Sens", "Langues",
            ).mapNotNull { cle -> f[cle]?.ifBlank { null }?.let { cle to it } }
            val fp = f["FP"]?.ifBlank { null }
            if (details.isNotEmpty() || fp != null) {
                Espace(); FiletEffile(); Espace()
                details.forEach { (cle, valeur) -> LigneLabel(cle, valeur) }
                fp?.let { LigneLabel("Puissance", it) }
            }
            f["Environnement"]?.ifBlank { null }?.let { LigneLabel("Environnement", it) }

            val sections = MonsterStatBlockParser.sections(monster.body)
            if (sections.isNotEmpty()) {
                Espace(); FiletEffile()
            }
            sections.forEach { section ->
                if (section.titre.isNotBlank()) {
                    Text(
                        section.titre,
                        color = RougeTitre,
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "smcp"),
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    HorizontalDivider(color = RougeTitre, thickness = 1.dp)
                }
                section.capacites.forEach { capacite ->
                    Text(
                        texteCapacite(capacite),
                        color = Encre,
                        fontSize = 14.sp,
                        fontStyle = if (capacite.nom == null && section.titre.isNotBlank()) FontStyle.Italic else FontStyle.Normal,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    EtatsInfliges(capacite, section.titre)
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(Lisere))
    }
}

@Composable
private fun Espace() = Box(Modifier.height(6.dp))

/** Vrai si l'entrée a les champs structurés d'un monstre (sinon : rendu markdown de repli). */
fun SrdEntry.aUnBlocDeStats(): Boolean = fields.containsKey("CA") || fields.containsKey("Caractéristiques")
