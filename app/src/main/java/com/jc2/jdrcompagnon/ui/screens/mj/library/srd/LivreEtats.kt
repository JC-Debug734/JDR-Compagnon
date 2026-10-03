package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats

/**
 * Livre « États » de la bibliothèque, généré depuis le catalogue [Etats] (même source que le
 * combat et le menu latéral MJ) : une fiche par état — règles, effets gérés par l'application —
 * et la liste des monstres du bestiaire qui l'infligent, retrouvés dans le texte de leurs actions,
 * traits et réactions.
 */
object LivreEtats {

    const val CATEGORIE_REGLES = "Règles des états"
    const val CATEGORIE_ETATS = "États"
    const val CATEGORIE_AUTRES = "Suivi en combat"

    /** Une action de monstre qui inflige un état : « Goule — Griffe (JS Constitution DD 10) ». */
    data class Source(val monstre: String, val action: String, val section: String, val sauvegarde: String?)

    private val ligneActionRegex = Regex("""^(.+?)\.\s+(.*)$""")
    private val sectionRegex = Regex("""^##\s+(.+?)\s*$""")
    private val parenthesesRegex = Regex("""\s*\([^)]*\)""")

    /** Pour chaque état, les actions de monstres qui l'infligent (bestiaire du monde). */
    fun sourcesParEtat(monstres: List<SrdEntry>): Map<ConditionCombat, List<Source>> {
        val resultat = mutableMapOf<ConditionCombat, MutableList<Source>>()
        monstres.forEach { monstre ->
            var section = ""
            monstre.rawMarkdown.lines().forEach { brute ->
                val ligne = brute.trim()
                sectionRegex.find(ligne)?.let { section = it.groupValues[1]; return@forEach }
                if (!ligne.contains("état")) return@forEach
                val match = ligneActionRegex.find(ligne) ?: return@forEach
                val action = match.groupValues[1].replace(parenthesesRegex, "").trim().take(60)
                Etats.infligesPar(match.groupValues[2]).forEach { inflige ->
                    val sauvegarde = inflige.dd?.let { "JS ${inflige.sauvegarde ?: ""} DD $it".replace("  ", " ") }
                        ?: inflige.evasionDd?.let { "évasion DD $it" }
                    val liste = resultat.getOrPut(inflige.condition) { mutableListOf() }
                    if (liste.none { it.monstre == monstre.name && it.action == action }) {
                        liste += Source(monstre.name, action, section, sauvegarde)
                    }
                }
            }
        }
        return resultat.mapValues { (_, l) -> l.sortedWith(compareBy({ it.monstre.lowercase() }, { it.action })) }
    }

    fun construire(monstres: List<SrdEntry>): List<SrdSectionEntry> {
        val sources = sourcesParEtat(monstres)
        val intro = SrdSectionEntry(
            name = "Les états",
            category = CATEGORIE_REGLES,
            rawMarkdown = """
                Les états modifient les capacités d'une créature. Ils résultent d'un sort, d'une capacité de classe, de l'attaque d'un monstre ou d'un autre effet.

                - **Durée** : un état dure jusqu'à ce qu'il soit contré (se relever met fin à À terre) ou pendant la durée indiquée par l'effet qui l'impose.
                - **Pas de cumul** : si plusieurs effets imposent le même état, chacun garde sa propre durée, mais les effets de l'état ne s'aggravent pas (sauf l'Épuisement, qui se compte en niveaux).
                - Une créature subit un état ou ne le subit pas.

                ## Dans l'application
                - Le champ **Condition** d'une fiche peut contenir plusieurs états séparés par des virgules (« Paralysé, Empoisonné ») ainsi que des mentions libres.
                - Les états se cochent depuis le **menu latéral MJ** (personnage du groupe), la fiche, ou la carte d'un combattant ; tout reste synchronisé entre la fiche et le combat.
                - Les attaques des monstres qui infligent un état l'appliquent d'office quand elles touchent ; si un jet de sauvegarde est prévu, le MJ l'applique en un geste en cas d'échec.
            """.trimIndent(),
        )
        val fiches = Etats.fiches.map { fiche ->
            SrdSectionEntry(
                name = fiche.condition.label,
                category = if (fiche.officiel) CATEGORIE_ETATS else CATEGORIE_AUTRES,
                rawMarkdown = contenu(
                    resume = fiche.resume,
                    regles = fiche.regles,
                    inclut = fiche.inclut.map { it.label },
                    effets = Etats.resumeEffets(listOf(fiche.condition)),
                    automatise = fiche.automatise,
                    sources = sources[fiche.condition].orEmpty(),
                ),
            )
        }
        val epuisement = SrdSectionEntry(
            name = Etats.NOM_EPUISEMENT,
            category = CATEGORIE_ETATS,
            rawMarkdown = contenu(
                resume = "Fatigue accumulée, en niveaux de 1 à 6.",
                regles = Etats.reglesEpuisement,
                inclut = emptyList(),
                effets = emptyList(),
                automatise = Etats.automatiseEpuisement,
                sources = emptyList(),
            ),
        )
        // Ordre alphabétique des états officiels (comme dans les règles), puis Concentration.
        val (officiels, autres) = (fiches + epuisement).partition { it.category == CATEGORIE_ETATS }
        return listOf(intro) + officiels.sortedBy { Etats.cle(it.name) } + autres
    }

    private fun contenu(
        resume: String,
        regles: List<String>,
        inclut: List<String>,
        effets: List<String>,
        automatise: List<String>,
        sources: List<Source>,
    ): String = buildString {
        appendLine("*$resume*")
        appendLine()
        regles.forEach { appendLine("- $it") }
        if (inclut.isNotEmpty()) {
            appendLine()
            appendLine("Inclut aussi l'état ${inclut.joinToString(" et ")}.")
        }
        if (effets.isNotEmpty()) {
            appendLine()
            appendLine("## Effets cumulés")
            effets.forEach { appendLine("- $it") }
        }
        if (automatise.isNotEmpty()) {
            appendLine()
            appendLine("## Dans l'application")
            automatise.forEach { appendLine("- $it") }
        }
        appendLine()
        appendLine("## Monstres qui l'infligent")
        if (sources.isEmpty()) {
            appendLine("Aucun monstre du bestiaire ne l'inflige directement.")
        } else {
            appendLine("${sources.map { it.monstre }.distinct().size} monstre(s) :")
            appendLine()
            sources.forEach { s ->
                val section = s.section.takeIf { it.isNotBlank() && it != "Actions" }?.let { ", $it" } ?: ""
                appendLine("- **${s.monstre}** — ${s.action}$section" + (s.sauvegarde?.let { " ($it)" } ?: ""))
            }
        }
    }
}
