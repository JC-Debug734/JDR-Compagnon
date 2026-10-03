package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import android.content.Context
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.components.FilePortraits
import com.jc2.jdrcompagnon.ui.components.PnjPortraits
import java.io.File
import kotlin.math.roundToInt

/**
 * Fiches PNJ créées à l'import d'un livre personnalisé à partir de ses entrées
 * `<!-- type: pnj -->` (voir [CustomContentParser]) : même format qu'un monstre (CA, Pv,
 * Caractéristiques, Compétences, Image...), converti en [Character] de type "PNJ" de l'univers
 * courant — utilisable ensuite dans les scénarios (#pnj:[Nom]), les groupes et les combats.
 *
 * Un PNJ dont une fiche de même nom existe déjà dans l'univers n'est pas recréé (réimport du
 * même livre). Son image, déjà résolue en chemin absolu par [CustomBookImages], est copiée dans
 * `pnj_portraits/` : la fiche garde son portrait même si le livre est retiré ensuite.
 */
object PnjImport {

    private const val PORTRAITS_DIR = "pnj_portraits"
    private val statRegex = Regex("""\b(For|Dex|Con|Int|Sag|Cha)\s+(\d+)""")
    private val leadingInt = Regex("""^\s*(\d+)""")
    private val leadingDecimal = Regex("""^\s*(\d+(?:[.,]\d+)?)""")

    private val tailles = mapOf(
        "TP" to "Très petite", "P" to "Petite", "M" to "Moyenne",
        "G" to "Grande", "TG" to "Très grande", "Gig" to "Gigantesque",
    )

    /** Crée les fiches des [pnjs] absents de l'univers ; retourne le nombre de fiches créées. */
    fun creerFiches(context: Context, pnjs: List<SrdEntry>, worldId: String, source: String): Int {
        if (pnjs.isEmpty()) return 0
        val existants = GameState.characters.value
            .filter { it.type == "PNJ" && it.worldId == worldId }
            .map { it.name.lowercase() }
            .toMutableSet()
        var crees = 0
        for (pnj in pnjs) {
            if (!existants.add(pnj.name.lowercase())) continue
            GameState.addCharacter(ficheDepuisProfil(context, pnj, worldId, "PNJ", "Importé du livre « $source »"))
            crees++
        }
        return crees
    }

    /**
     * Fiche de personnage de type [type] ("PNJ", "Créature"...) à partir d'un profil au format
     * monstre (livre importé ou bestiaire) : caractéristiques, CA, PV, vitesse, compétences,
     * actions, et son image en portrait si elle en a une. [note] ouvre les notes MJ.
     */
    fun ficheDepuisProfil(context: Context, pnj: SrdEntry, worldId: String, type: String, note: String): Character {
        val f = pnj.fields
        val stats = statRegex.findAll(f["Caractéristiques"].orEmpty())
            .associate { it.groupValues[1] to it.groupValues[2].toInt() }
        val pv = leadingInt.find(f["Pv"].orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 10
        // Vitesse du bloc en mètres ("9 m"), la fiche en pieds (1,50 m = 5 pieds).
        val vitesse = leadingDecimal.find(f["Vitesse"].orEmpty())?.groupValues?.get(1)
            ?.replace(',', '.')?.toDoubleOrNull()?.let { (it / 0.3).roundToInt() } ?: 30
        val competences = f["Compétences"].orEmpty().split(',')
            .map { it.substringBefore('+').substringBefore('−').substringBefore('-').trim() }
            .filter { it.isNotBlank() }
        val espece = f["Type"].orEmpty().substringAfter('(', "").substringBefore(')')
            .split(',').first().trim()
            .takeUnless { it.isBlank() || it.startsWith("tout") }
            ?.replaceFirstChar { it.uppercase() }
            .orEmpty()
        val langues = f["Langues"].orEmpty().split(',', ';').map { it.trim() }
            .filter { it.isNotBlank() && it != "—" && it != "-" }

        // Sans image : portrait PNJ par défaut pour un PNJ, rien pour une créature (les
        // portraits PNJ sont des humanoïdes).
        val portrait = copierPortrait(context, pnj)?.let { FilePortraits.idFor(it) }
            ?: if (type == "PNJ") PnjPortraits.defaultFor(context, pnj.name).orEmpty() else ""

        return Character(
            name = pnj.name,
            type = type,
            worldId = worldId,
            race = espece,
            alignment = f["Alignement"].orEmpty(),
            size = tailles[f["Taille"].orEmpty().trim()].orEmpty(),
            strength = stats["For"] ?: 10,
            dexterity = stats["Dex"] ?: 10,
            constitution = stats["Con"] ?: 10,
            intelligence = stats["Int"] ?: 10,
            wisdom = stats["Sag"] ?: 10,
            charisma = stats["Cha"] ?: 10,
            maxHitPoints = pv,
            currentHitPoints = pv,
            armorClass = leadingInt.find(f["CA"].orEmpty())?.groupValues?.get(1)?.toIntOrNull() ?: 10,
            speed = vitesse,
            proficiencyBonus = bonusMaitrise(f["FP"].orEmpty()),
            skills = competences.associateWith { ProficiencyLevel.PROFICIENT },
            skillProficiencies = competences,
            equipment = f["Équipement"].orEmpty(),
            languages = langues,
            traits = listOfNotNull(
                f["Sens"]?.takeIf { it.isNotBlank() }?.let { "Sens : $it" },
                f["Résistances"]?.takeIf { it.isNotBlank() }?.let { "Résistances : $it" },
                f["Immunités"]?.takeIf { it.isNotBlank() }?.let { "Immunités : $it" },
            ).joinToString("\n"),
            classFeatures = pnj.body,
            dmNotes = note +
                (f["FP"]?.takeIf { it.isNotBlank() }?.let { " — FP $it" } ?: "") + ".",
            portrait = portrait,
            createdBy = "MJ",
        )
    }

    /** Bonus de maîtrise d'après le FP (règles SRD) : +2 jusqu'à FP 4, +3 de 5 à 8, etc. */
    private fun bonusMaitrise(fp: String): Int {
        val valeur = fp.substringBefore('(').trim()
        val niveau = if ('/' in valeur) 0 else valeur.toIntOrNull() ?: 0
        return 2 + maxOf(0, (niveau - 1) / 4)
    }

    /**
     * Copie l'image du profil dans pnj_portraits/ : image d'un livre importé (chemin absolu),
     * image choisie par le MJ pour ce monstre, ou image des assets du bestiaire.
     */
    private fun copierPortrait(context: Context, pnj: SrdEntry): File? {
        val dossier = File(context.filesDir, PORTRAITS_DIR).apply { mkdirs() }
        val fichier = pnj.image?.takeIf { it.startsWith("/") }?.let(::File)?.takeIf { it.isFile }
            ?: MonsterImages.fichierImporte(context, pnj)
        if (fichier != null) {
            val cible = File(dossier, "${java.util.UUID.randomUUID()}.${fichier.extension.ifBlank { "img" }}")
            return runCatching { fichier.copyTo(cible) }.getOrNull()
        }
        val asset = MonsterImages.assetPathFor(context, pnj) ?: return null
        val cible = File(dossier, "${java.util.UUID.randomUUID()}.${asset.substringAfterLast('.')}")
        return runCatching {
            context.assets.open(asset).use { input -> cible.outputStream().use { input.copyTo(it) } }
            cible
        }.getOrNull()
    }
}
