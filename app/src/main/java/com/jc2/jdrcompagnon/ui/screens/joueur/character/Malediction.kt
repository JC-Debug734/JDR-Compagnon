package com.jc2.jdrcompagnon.ui.screens.joueur.character

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Objets maudits (champ **Maudit** Oui de l'objet, voir EquipmentParser).
 *
 * - Tant qu'il reste dans le sac, l'objet se manipule normalement (le personnage ne l'a pas encore mis).
 * - Une fois équipé, il ne peut plus être retiré : ni rangé dans le sac, ni déplacé vers un autre
 *   emplacement, ni délogé par un autre objet, ni jeté (gardes dans GameState : unequipFromSlot,
 *   unequipItem, equipInSlot, removeItemCompletely).
 * - Le MJ peut lever la malédiction (Délivrance des malédictions...) depuis la fiche de l'objet :
 *   il est noté dans [Character.maledictionsLevees] et se retire de nouveau normalement.
 *
 * L'objet est reconnu d'après les listes d'équipement déjà chargées (SrdRepository.objetMaudit) :
 * l'écran d'inventaire les charge à son ouverture.
 */
object Malediction {

    /** Fiche de l'objet [nom] s'il est maudit, null sinon. */
    fun objet(nom: String): EquipmentItem? = SrdRepository.objetMaudit(nom)

    /** Malédiction levée par le MJ pour ce personnage ? */
    fun estLevee(character: Character, nom: String): Boolean =
        character.maledictionsLevees.any { it.equals(nom, ignoreCase = true) }

    /** [nom] est porté par [character] et sa malédiction l'empêche de l'enlever. */
    fun bloque(character: Character, nom: String): Boolean =
        (nom in character.equippedItems || nom in character.equippedSlots.values) && !estLevee(character, nom) && objet(nom) != null

    /** Message affiché quand le joueur tente de retirer l'objet. */
    fun message(nom: String): String {
        val effet = objet(nom)?.malediction.orEmpty()
        return "$nom est maudit : impossible de l'enlever." +
            if (effet.isNotBlank()) " $effet" else " Seule une Délivrance des malédictions (MJ) peut le libérer."
    }
}
