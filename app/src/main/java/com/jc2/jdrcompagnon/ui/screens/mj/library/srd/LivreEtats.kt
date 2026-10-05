package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats

/**
 * Livre « États » de la bibliothèque, généré depuis le catalogue [Etats] (même source que le
 * combat et le menu latéral MJ) : une fiche par état — règles, effets cumulés et ce que
 * l'application en fait. Les états infligés par un monstre sont signalés sur sa fiche
 * (MonsterStatBlock), pas listés ici.
 */
object LivreEtats {

    const val CATEGORIE_REGLES = "Règles des états"
    const val CATEGORIE_ETATS = "États"
    const val CATEGORIE_AUTRES = "Suivi en combat"

    fun construire(): List<SrdSectionEntry> {
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
                - Les états se donnent depuis le **menu latéral MJ** (personnage du groupe → « Donner un état »), la fiche, ou la carte d'un combattant ; tout reste synchronisé entre la fiche et le combat.
                - Sur la fiche d'un monstre, chaque capacité qui inflige un état l'indique. En combat, ses attaques l'appliquent d'office quand elles touchent ; si un jet de sauvegarde est prévu, le MJ l'applique en un geste en cas d'échec.
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
    }
}
