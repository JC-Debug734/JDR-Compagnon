package com.jc2.jdrcompagnon.feature_butin.domain

/**
 * Butin écrit dans un scénario, lien `#butin:[450 pc; 182 pa; Longue-vue fêlée (100 po); 3x Potion de guérison]` :
 * éléments séparés par « ; », chacun étant soit des pièces (« 450 pc », « 2 500 po »), soit un
 * objet avec une quantité facultative (« 3x Potion », « Potion x3 ») et une note entre
 * parenthèses à la fin (sa valeur, où il se trouve...).
 */
object ButinTexte {

    private val PiecesRegex = Regex("""^(\d[\d\s  ]*)\s*(pp|po|pe|pa|pc)$""", RegexOption.IGNORE_CASE)
    private val QuantiteAvantRegex = Regex("""^(\d+)\s*[x×]\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val QuantiteApresRegex = Regex("""^(.+?)\s+[x×](\d+)$""", RegexOption.IGNORE_CASE)
    private val NoteRegex = Regex("""^(.+?)\s*\(([^()]*)\)$""")

    fun lire(texte: String): Butin {
        var bourse = Bourse()
        val objets = mutableListOf<ObjetButin>()
        texte.split(';').map { it.trim() }.filter { it.isNotBlank() }.forEach { element ->
            val pieces = PiecesRegex.matchEntire(element)
            if (pieces != null) {
                val n = pieces.groupValues[1].filter { it.isDigit() }.toIntOrNull() ?: 0
                bourse += when (pieces.groupValues[2].lowercase()) {
                    "pp" -> Bourse(pp = n)
                    "po" -> Bourse(po = n)
                    "pe" -> Bourse(pe = n)
                    "pa" -> Bourse(pa = n)
                    else -> Bourse(pc = n)
                }
                return@forEach
            }
            val (sansNote, note) = NoteRegex.matchEntire(element)
                ?.let { it.groupValues[1].trim() to it.groupValues[2].trim() } ?: (element to "")
            val avant = QuantiteAvantRegex.matchEntire(sansNote)
            val apres = QuantiteApresRegex.matchEntire(sansNote)
            val (nom, quantite) = when {
                avant != null -> avant.groupValues[2].trim() to (avant.groupValues[1].toIntOrNull() ?: 1)
                apres != null -> apres.groupValues[1].trim() to (apres.groupValues[2].toIntOrNull() ?: 1)
                else -> sansNote to 1
            }
            objets += ObjetButin(nom = nom, quantite = quantite.coerceAtLeast(1), note = note)
        }
        return Butin(bourse, objets)
    }

    /** Libellé lisible du lien dans le texte du scénario (« ; » remplacés par des virgules). */
    fun libelle(texte: String): String = texte.split(';').map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
}
