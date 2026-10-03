package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import java.text.Normalizer
import java.util.WeakHashMap

/**
 * Recherche tolérante de la bibliothèque :
 * - insensible aux majuscules, aux accents (é = e), à la ponctuation et aux apostrophes
 *   (l'eau = l eau = leau n'est pas garanti, mais "l'eau" = "l eau"), œ/æ → oe/ae ;
 * - mots dans n'importe quel ordre : "rouge dragon" trouve "Dragon rouge adulte" ;
 * - singulier/pluriel : "gobelins" trouve "Gobelin" ;
 * - fautes de frappe légères sur le nom : "dargon" trouve "Dragon" (1 lettre d'écart dès
 *   4 lettres, 2 dès 8) ;
 * - abréviations courantes du jeu : pv, ca, js, dd, fp, pnj... ;
 * - classement : nom identique, puis nom qui commence par la recherche, puis tous les mots dans
 *   le nom, puis trouvé seulement dans le texte de la fiche.
 */
object LibrarySearch {

    private val cache = WeakHashMap<String, String>()
    private val nonAlnum = Regex("[^a-z0-9]+")
    private val marks = Regex("\\p{Mn}+")

    // Abréviations → forme longue (et réciproquement, voir [alternatives]).
    private val abreviations = mapOf(
        "pv" to "points de vie",
        "ca" to "classe d armure",
        "js" to "jet de sauvegarde",
        "dd" to "degre de difficulte",
        "fp" to "facteur de puissance",
        "pnj" to "personnage non joueur",
        "mj" to "maitre du jeu",
        "po" to "piece d or",
        "pa" to "piece d argent",
        "pc" to "piece de cuivre",
    )

    fun normalize(text: String): String = synchronized(cache) {
        cache.getOrPut(text) {
            Normalizer.normalize(text.lowercase().replace("œ", "oe").replace("æ", "ae"), Normalizer.Form.NFD)
                .replace(marks, "")
                .replace(nonAlnum, " ")
                .trim()
        }
    }

    /** Pluriel simple : "gobelins" → "gobelin", "chevaux" → "chevau" (comparé des deux côtés). */
    private fun stem(word: String): String =
        if (word.length > 3 && (word.endsWith('s') || word.endsWith('x'))) word.dropLast(1) else word

    private fun words(normalized: String): List<String> =
        normalized.split(' ').filter { it.isNotEmpty() }

    /** Formes acceptées pour un mot de la recherche : lui-même, ou la forme longue d'une abréviation. */
    private fun alternatives(token: String): List<String> =
        listOfNotNull(token, abreviations[token])

    /**
     * Variantes de la recherche : telle quelle, et avec chaque forme longue remplacée par son
     * abréviation ("points de vie" → "pv"), pour trouver les textes qui n'emploient que l'une.
     */
    private fun queryVariants(normalizedQuery: String): List<String> =
        listOf(normalizedQuery) + abreviations.entries
            .filter { (_, long) -> " $normalizedQuery ".contains(" $long ") }
            .map { (abbr, long) -> " $normalizedQuery ".replace(" $long ", " $abbr ").trim() }

    private fun distanceMax(token: String) = when {
        token.length >= 8 -> 2
        token.length >= 4 -> 1
        else -> 0
    }

    /**
     * Distance d'édition bornée (Damerau-Levenshtein restreinte) : une lettre en trop, en moins,
     * remplacée ou deux lettres inversées ("dargon" / "dragon") comptent chacune pour 1.
     */
    private fun withinDistance(a: String, b: String, max: Int): Boolean {
        if (max == 0) return a == b
        if (kotlin.math.abs(a.length - b.length) > max) return false
        val d = Array(a.length + 1) { i -> IntArray(b.length + 1) { j -> if (i == 0) j else if (j == 0) i else 0 } }
        for (i in 1..a.length) {
            var rowMin = Int.MAX_VALUE
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                var v = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
                if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, d[i - 2][j - 2] + 1)
                d[i][j] = v
                rowMin = minOf(rowMin, v)
            }
            if (rowMin > max) return false
        }
        return d[a.length][b.length] <= max
    }

    /** Un mot (ou une expression d'abréviation) retrouvé parmi les mots du nom. */
    private fun tokenInName(token: String, nameWords: List<String>, normalizedName: String): Boolean =
        alternatives(token).any { alt ->
            if (' ' in alt) {
                " $normalizedName ".contains(" $alt ")
            } else {
                val t = stem(alt)
                val d = distanceMax(t)
                nameWords.any { w ->
                    val sw = stem(w)
                    // Mot entier ou début de mot (frappe en cours) à [d] fautes près.
                    sw.contains(t) || (d > 0 && (withinDistance(t, sw, d) ||
                        (sw.length > t.length && withinDistance(t, sw.take(t.length), d))))
                }
            }
        }

    private fun tokenInText(token: String, normalizedText: String): Boolean =
        alternatives(token).any { alt -> normalizedText.contains(stem(alt)) }

    /**
     * Score de pertinence de l'entrée [name] (et de son texte [content]) pour [query] ; null si
     * elle ne correspond pas. Plus le score est haut, plus le résultat est pertinent.
     */
    fun score(query: String, name: String, content: String? = null): Int? {
        val q = normalize(query)
        if (words(q).isEmpty()) return 0
        return queryVariants(q).mapNotNull { scoreNormalized(it, name, content) }.maxOrNull()
    }

    private fun scoreNormalized(q: String, name: String, content: String?): Int? {
        val tokens = words(q)
        val n = normalize(name)
        val nameWords = words(n)
        if (tokens.all { tokenInName(it, nameWords, n) }) {
            return when {
                n == q -> 1000
                n.startsWith(q) -> 800
                else -> 600
            }
        }
        if (content == null) return null
        val text = n + " " + normalize(content)
        if (!tokens.all { tokenInName(it, nameWords, n) || tokenInText(it, text) }) return null
        return 100 + 50 * tokens.count { tokenInName(it, nameWords, n) }
    }

    /** Filtre par nom seul (recherche dans un livre ouvert) ; une recherche vide garde tout. */
    fun matchesName(query: String, name: String): Boolean = query.isBlank() || score(query, name) != null
}
