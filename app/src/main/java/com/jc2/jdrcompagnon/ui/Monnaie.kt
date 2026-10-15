package com.jc2.jdrcompagnon.ui

import kotlin.math.roundToInt

/**
 * Monnaie D&D (SRD) : 1 pp = 10 po, 1 po = 2 pe = 10 pa = 100 pc. Les prix des boutiques et les
 * transactions comptent en pièces de cuivre (pc), seule unité qui représente exactement « 3 pa »
 * ou « 5 pc » ; la bourse d'un personnage garde une quantité par sorte de pièce
 * (Character.platinum, gold, electrum, silver, copper).
 */
object Monnaie {
    const val PC_PAR_PA = 10
    const val PC_PAR_PE = 50
    const val PC_PAR_PO = 100
    const val PC_PAR_PP = 1000

    /** Valeur totale de la bourse, en pièces de cuivre. */
    fun totalPc(c: Character): Int =
        c.platinum * PC_PAR_PP + c.gold * PC_PAR_PO + c.electrum * PC_PAR_PE + c.silver * PC_PAR_PA + c.copper

    /** Nombre de pièces de la bourse (pour son poids : 50 pièces par livre). */
    fun nombreDePieces(c: Character): Int = c.platinum + c.gold + c.electrum + c.silver + c.copper

    /**
     * Prix lisible : « 3 pa », « 1 po 5 pa », « 12 po » ; « Gratuit » pour 0. Les prix restent en
     * po, pa et pc comme dans le SRD (pas de pp ni de pe). [zero] : texte d'un montant nul.
     */
    fun libelle(pc: Int, zero: String = "Gratuit"): String {
        if (pc <= 0) return zero
        return listOf(pc / PC_PAR_PO to "po", pc % PC_PAR_PO / PC_PAR_PA to "pa", pc % PC_PAR_PA to "pc")
            .filter { it.first > 0 }
            .joinToString(" ") { "${it.first} ${it.second}" }
    }

    /** Contenu de la bourse : « 1 pp, 12 po, 3 pa » (« vide » sans aucune pièce). */
    fun libelleBourse(c: Character): String =
        listOf(c.platinum to "pp", c.gold to "po", c.electrum to "pe", c.silver to "pa", c.copper to "pc")
            .filter { it.first > 0 }
            .joinToString(", ") { "${it.first} ${it.second}" }
            .ifBlank { "vide" }

    private val MONTANT = Regex("""(\d+(?:[.,]\d+)?)\s*(pp|po|pe|pa|pc)?""", RegexOption.IGNORE_CASE)

    /**
     * Montant saisi ou écrit dans le SRD (« 5 pa », « 1,5 po », « 2 po 3 pa », « 12 » = 12 po), en
     * pièces de cuivre ; null si aucun nombre n'est reconnu.
     */
    fun parser(texte: String): Int? {
        val montants = MONTANT.findAll(texte).toList()
        if (montants.isEmpty()) return null
        return montants.sumOf { m ->
            val valeur = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            val parPiece = when (m.groupValues[2].lowercase()) {
                "pp" -> PC_PAR_PP
                "pe" -> PC_PAR_PE
                "pa" -> PC_PAR_PA
                "pc" -> 1
                else -> PC_PAR_PO
            }
            valeur * parPiece
        }.roundToInt()
    }

    /**
     * Bourse après avoir payé [prixPc], ou null si elle ne suffit pas. Les plus petites pièces partent
     * d'abord ; la monnaie éventuellement rendue l'est en po, pa et pc.
     */
    fun payer(c: Character, prixPc: Int): Character? {
        if (prixPc <= 0) return c
        if (totalPc(c) < prixPc) return null
        val valeurs = intArrayOf(1, PC_PAR_PA, PC_PAR_PE, PC_PAR_PO, PC_PAR_PP)
        val pieces = intArrayOf(c.copper, c.silver, c.electrum, c.gold, c.platinum)
        var reste = prixPc
        for (i in valeurs.indices) {
            if (reste <= 0) break
            val n = minOf(pieces[i], (reste + valeurs[i] - 1) / valeurs[i])
            pieces[i] -= n
            reste -= n * valeurs[i]
        }
        val paye = c.copy(copper = pieces[0], silver = pieces[1], electrum = pieces[2], gold = pieces[3], platinum = pieces[4])
        return recevoir(paye, -reste)
    }

    /** Bourse augmentée de [montantPc], versé en po, pa et pc. */
    fun recevoir(c: Character, montantPc: Int): Character {
        if (montantPc <= 0) return c
        return c.copy(
            gold = c.gold + montantPc / PC_PAR_PO,
            silver = c.silver + montantPc % PC_PAR_PO / PC_PAR_PA,
            copper = c.copper + montantPc % PC_PAR_PA,
        )
    }
}
