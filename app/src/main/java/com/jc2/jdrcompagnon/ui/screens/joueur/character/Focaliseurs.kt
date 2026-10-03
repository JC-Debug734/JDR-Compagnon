package com.jc2.jdrcompagnon.ui.screens.joueur.character

import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.ui.Character

/**
 * « Focaliseur arcanique / druidique » n'est pas un objet mais une PROPRIÉTÉ d'un objet : le
 * bâton de combat du Magicien est une vraie arme qui sert aussi de focaliseur arcanique.
 * L'équipement de départ écrit « focaliseur arcanique (bâton de combat) » : l'objet devient
 * « Bâton de combat » et sa propriété est retenue dans Character.focaliseurs (cf.
 * GameState.normaliserNomsObjets). Le grimoire est un focaliseur arcanique pour le Magicien
 * uniquement.
 */
object Focaliseurs {

    private val REGEX_FOCALISEUR = Regex("""^focaliseur\s+(arcanique|druidique)\s*\((.+)\)\s*$""", RegexOption.IGNORE_CASE)

    /** « Focaliseur arcanique (bâton de combat) » → (« arcanique », « bâton de combat »), null sinon. */
    fun separer(nom: String): Pair<String, String>? =
        REGEX_FOCALISEUR.find(nom.trim())?.let { m -> m.groupValues[1].lowercase() to m.groupValues[2].trim() }

    /** Libellé de la propriété (« Focaliseur arcanique »). */
    fun libelle(type: String) = "Focaliseur ${type.lowercase()}"

    /**
     * Type de focaliseur de [item] pour [c] (« arcanique », « druidique »), ou null : propriété
     * retenue sur la fiche, grimoire d'un Magicien, ou objet dont le nom est lui-même un
     * focaliseur (« Focaliseur arcanique (orbe) »).
     */
    fun type(c: Character, item: String): String? {
        c.focaliseurs.entries.firstOrNull { it.key.equals(item, ignoreCase = true) }?.let { return it.value }
        if (ArmorRules.isGrimoireItem(item) && estMagicien(c)) return "arcanique"
        separer(item)?.let { return it.first }
        val lower = item.lowercase()
        return when {
            lower.contains("focaliseur arcanique") -> "arcanique"
            lower.contains("focaliseur druidique") -> "druidique"
            else -> null
        }
    }

    fun estMagicien(c: Character): Boolean =
        ArsenalPersonnage.niveauxParClasse(c).keys.any { it.equals("Magicien", ignoreCase = true) }
}
