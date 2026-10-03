package com.jc2.jdrcompagnon.ui.screens.joueur.character

import android.content.Context
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerFormuleDes
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlin.random.Random

/**
 * Objets à charges (bâton des chants d'oiseaux, baguettes...) : champs **Charges**,
 * **Recharge**, **Pouvoir** et **Destruction** de l'objet (voir EquipmentParser), charges
 * restantes suivies par personnage dans [Character.itemCharges].
 *
 * - Utilisation ([utiliser]) : depuis l'onglet Combat de la fiche, tant que l'objet est équipé
 *   (et harmonisé s'il l'exige, voir Harmonisation) ; 1 charge dépensée. Dépenser la dernière fait lancer un d20 si l'objet a un seuil de
 *   **Destruction** : sur ce résultat ou moins, l'objet est détruit (retiré de l'inventaire).
 * - Recharge ([rechargerAuReposLong]) : à chaque repos long, pour tous les objets portés (sac
 *   compris), la formule **Recharge** est lancée et les charges ajoutées, sans dépasser le maximum.
 */
object ObjetsACharges {

    private val formuleRegex = Regex("""\d+\s*d\s*\d+(?:\s*[+\-−]\s*\d+)?""", RegexOption.IGNORE_CASE)

    /** Résultat de la dépense d'une charge. [jetD20] n'est lancé que pour la dernière charge. */
    data class Utilisation(val restantes: Int, val jetD20: Int?, val detruit: Boolean)

    /** Tous les objets que porte le personnage, équipés ou dans son sac. */
    fun objetsPortes(character: Character): List<String> =
        (character.equippedSlots.values + character.equippedItems + character.backpackItems +
            character.backpackExteriorSlots.values).distinct()

    fun estEquipe(character: Character, nom: String): Boolean =
        nom in character.equippedSlots.values || nom in character.equippedItems

    fun restantes(character: Character, item: EquipmentItem): Int {
        val max = item.charges ?: return 0
        return (character.itemCharges[item.name] ?: max).coerceIn(0, max)
    }

    /** Objets à charges portés par le personnage, d'après la bibliothèque de son univers. */
    suspend fun objetsDuPersonnage(context: Context, character: Character): List<EquipmentItem> {
        val portes = objetsPortes(character).map { it.lowercase() }.toSet()
        if (portes.isEmpty()) return emptyList()
        val worldId = character.worldId.ifBlank { GameState.currentWorldId() ?: "donjon_et_dragon" }
        return SrdRepository.loadEquipmentList(context, worldId)
            .filter { it.charges != null && it.name.lowercase() in portes }
            .distinctBy { it.name.lowercase() }
    }

    /** Dépense d'une charge (logique pure, voir [utiliser]). */
    fun depenser(restantes: Int, destruction: Int?, aleatoire: Random = Random): Utilisation {
        val apres = (restantes - 1).coerceAtLeast(0)
        if (apres > 0 || destruction == null) return Utilisation(apres, null, false)
        val jet = aleatoire.nextInt(1, 21)
        return Utilisation(apres, jet, jet <= destruction)
    }

    /** Charges récupérées au repos long d'après [formule] ("1d6 + 4", "3"...), sans le plafond. */
    fun jetRecharge(formule: String, aleatoire: Random = Random): Int? {
        formuleRegex.find(formule)?.value?.let { f ->
            return lancerFormuleDes(f.replace(" ", "").replace('−', '-'), aleatoire)
        }
        return Regex("""\d+""").find(formule)?.value?.toIntOrNull()
    }

    /** Utilise le pouvoir de [item] : dépense une charge et renvoie le message à afficher. */
    fun utiliser(character: Character, item: EquipmentItem, aleatoire: Random = Random): String {
        if (!Harmonisation.estActif(character, item)) {
            return "${item.name} : ${Harmonisation.libelle(item).lowercase()}, ses propriétés magiques restent inactives."
        }
        val avant = restantes(character, item)
        if (avant <= 0) return "${item.name} n'a plus de charge."
        val resultat = depenser(avant, item.destruction, aleatoire)
        return if (resultat.detruit) {
            GameState.removeItemCompletely(character.id, item.name)
            "Dernière charge dépensée — d20 : ${resultat.jetD20}. ${item.name} est détruit à jamais !"
        } else {
            GameState.setItemCharges(character.id, item.name, resultat.restantes)
            "${item.name} : 1 charge dépensée (${resultat.restantes}/${item.charges})." +
                (resultat.jetD20?.let { " Dernière charge — d20 : $it, l'objet résiste." } ?: "")
        }
    }

    /**
     * Repos long : chaque objet à charges porté récupère le résultat de sa formule **Recharge**.
     * Renvoie une ligne par objet rechargé, pour le compte rendu du repos.
     */
    suspend fun rechargerAuReposLong(context: Context, character: Character, aleatoire: Random = Random): List<String> =
        objetsDuPersonnage(context, character).mapNotNull { item ->
            val max = item.charges ?: return@mapNotNull null
            val avant = restantes(character, item)
            if (avant >= max || item.recharge.isBlank()) return@mapNotNull null
            val jet = jetRecharge(item.recharge, aleatoire) ?: return@mapNotNull null
            val apres = (avant + jet).coerceAtMost(max)
            GameState.setItemCharges(character.id, item.name, apres)
            "${item.name} : ${item.recharge} = $jet, +${apres - avant} charge(s) ($apres/$max)."
        }
}
