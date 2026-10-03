package com.jc2.jdrcompagnon.feature_environnement.domain.model

import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import java.text.Normalizer

/**
 * Types de terrain utilisés par la ligne "Environnement:" des monstres du SRD
 * (assets/dnd/monster_srd521.md). Servent de filtre commun au bestiaire d'un environnement
 * (EnvironmentDetailScreen), au composeur de combat et au filtre de la bibliothèque, pour ne
 * plus dépendre d'une égalité exacte entre le nom libre de l'environnement et ces valeurs.
 */
val TERRAINS_SRD: List<String> = listOf(
    "Aquatique", "Arctique", "Désert", "Donjon/Ruines", "Forêt", "Grotte/Souterrain",
    "Marais", "Montagne", "Plaines", "Planaire", "Urbain", "Volcanique"
)

/**
 * Mots-clés (sans accents, minuscules) qui, présents comme mot dans un nom d'environnement,
 * évoquent un terrain. Un mot-clé de 5 lettres ou plus accepte aussi les mots qui commencent par
 * lui (pluriels, "volcan" → "volcanique"), les plus courts doivent correspondre exactement
 * ("mer" ne doit pas reconnaître "mercenaire").
 */
private val MOTS_CLES_TERRAIN: Map<String, List<String>> = mapOf(
    "Aquatique" to listOf("aquatique", "mer", "mers", "ocean", "lac", "lacs", "riviere", "fleuve", "cote", "plage", "recif", "lagon"),
    "Arctique" to listOf("arctique", "neige", "glace", "glacier", "toundra", "polaire", "banquise"),
    "Désert" to listOf("desert", "dune", "dunes", "sable", "sables", "aride", "oasis"),
    "Donjon/Ruines" to listOf("donjon", "ruine", "crypte", "tombe", "tombeau", "temple", "forteresse", "chateau", "catacombe"),
    "Forêt" to listOf("foret", "bois", "sylve", "jungle", "bosquet"),
    "Grotte/Souterrain" to listOf("grotte", "souterrain", "caverne", "mine", "mines", "tunnel", "gouffre", "outreterre"),
    "Marais" to listOf("marais", "marecage", "tourbiere", "bayou"),
    "Montagne" to listOf("montagne", "mont", "monts", "pic", "pics", "col", "falaise", "colline"),
    "Plaines" to listOf("plaine", "prairie", "champ", "champs", "steppe", "lande", "landes", "campagne", "route"),
    "Planaire" to listOf("planaire", "plan", "abysse", "enfer", "enfers", "feerie", "astral", "ethere"),
    "Urbain" to listOf("urbain", "ville", "cite", "village", "rue", "ruelle", "taverne", "marche", "quartier", "egout"),
    "Volcanique" to listOf("volcan", "lave", "magma", "cendre")
)

private fun normaliser(texte: String): String =
    Normalizer.normalize(texte, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** Terrains devinés à partir du nom (et de la description) d'un environnement, dans l'ordre de [TERRAINS_SRD]. */
fun devinerTerrains(nom: String, description: String = ""): List<String> {
    val mots = normaliser("$nom $description").split(Regex("[^a-z]+")).filter { it.isNotBlank() }
    fun correspond(motCle: String) = mots.any { it == motCle || (motCle.length >= 5 && it.startsWith(motCle)) }
    return TERRAINS_SRD.filter { terrain -> MOTS_CLES_TERRAIN[terrain].orEmpty().any(::correspond) }
}

/**
 * Terrains servant réellement au filtre du bestiaire : ceux choisis par le MJ, sinon ceux devinés
 * depuis le nom de l'environnement (les environnements existants n'ont pas encore de terrain).
 */
val Environnement.terrainsEffectifs: List<String>
    get() = terrains.ifEmpty { devinerTerrains(nom, description) }

/**
 * Vrai si le monstre peut être rencontré dans l'un des [terrains]. Tolère les valeurs partielles du
 * SRD ("Souterrain" pour "Grotte/Souterrain") en comparant chaque partie des terrains composés.
 */
fun SrdEntry.rencontrableDans(terrains: Collection<String>): Boolean {
    if (terrains.isEmpty()) return false
    val cibles = terrains.flatMap { t -> t.split("/") + t }.map(::normaliser).toSet()
    return environments.any { env ->
        (env.split("/") + env).any { normaliser(it.trim()) in cibles }
    }
}
