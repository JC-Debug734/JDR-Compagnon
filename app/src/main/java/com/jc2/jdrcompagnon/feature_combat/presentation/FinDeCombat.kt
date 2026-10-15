package com.jc2.jdrcompagnon.feature_combat.presentation

import android.content.Context
import com.jc2.jdrcompagnon.feature_butin.domain.GenerateurButin
import com.jc2.jdrcompagnon.feature_butin.ui.ButinsGeneres
import com.jc2.jdrcompagnon.feature_butin.ui.genererMonstre
import com.jc2.jdrcompagnon.feature_butin.ui.graine
import com.jc2.jdrcompagnon.feature_butin.ui.retirerObjetDuPnj
import com.jc2.jdrcompagnon.feature_group.domain.model.GroupItem
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.components.objetsDuLibelle
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fin de combat (tous les ennemis vaincus, voir CombatEnCours.termine) : le butin de chaque vaincu
 * est envoyé au groupe, comme tout objet proposé au groupe depuis un butin.
 *
 * - Objets : une offre « Je le veux » par exemplaire aux joueurs connectés du groupe
 *   (NetworkSessionManager.sendLootOfferToGroup) ; ce sont eux qui choisissent qui le prend.
 *   Sans joueur connecté, ils vont dans l'inventaire commun du groupe, à répartir plus tard.
 * - Pièces : versées au trésor commun du groupe.
 *
 * Le butin d'un vaincu est le même que celui affiché par sa fouille (ButinsGeneres, clé
 * « combat:<id> ») : ce que le MJ a déjà remis pendant le combat n'est pas renvoyé.
 */
object FinDeCombat {

    /** Ce qui a été envoyé : groupe, objets proposés (ou rangés), pièces d'or versées au trésor. */
    data class Bilan(
        val groupe: String?,
        val objets: List<String>,
        val orPo: Int,
        // Aucun joueur du groupe connecté : objets rangés dans l'inventaire commun.
        val inventaireCommun: Boolean,
    ) {
        val vide: Boolean get() = objets.isEmpty() && orPo == 0
    }

    /** Groupe du combat : le groupe actif, sinon celui des personnages qui ont combattu. */
    private fun groupeDuCombat(etat: CombatEnCours): GameState.MjGroup? {
        val groupes = GameState.mjGroups.value
        groupes.firstOrNull { it.id == GameState.currentGroupId.value }?.let { return it }
        val combattants = etat.combattants.filter { !it.estMonstre }.mapNotNull { it.characterId }.toSet()
        return groupes.firstOrNull { g -> g.memberIds.any { it in combattants } }
    }

    suspend fun distribuerButin(context: Context, etat: CombatEnCours): Bilan = withContext(Dispatchers.IO) {
        val groupe = groupeDuCombat(etat) ?: return@withContext Bilan(null, emptyList(), 0, false)
        val worldId = GameState.currentWorldId()
        val joueursConnectes = NetworkSessionManager.joueursDuGroupe(groupe.memberIds).isNotEmpty()
        val objets = mutableListOf<String>()
        var orPo = 0

        // Butin propre à chaque boss vaincu (écaille, croc…), en plus de celui de sa fiche.
        etat.boss.forEach { (id, b) ->
            if (etat.combattants.any { it.id == id && it.horsCombat }) objets += b.definition.butin
        }
        etat.combattants.filter { it.estMonstre && it.horsCombat && !it.inerte }.forEach { vaincu ->
            val cle = "combat:${vaincu.id}"
            val pnj = vaincu.characterId?.let { id -> GameState.characters.value.firstOrNull { it.id == id } }
            if (ButinsGeneres.entrees.value[cle] == null) {
                val butin = if (pnj != null) GenerateurButin.pourPersonnage(pnj, graine(cle))
                else {
                    val nom = vaincu.monstreNom ?: vaincu.nom
                    genererMonstre(nom, SrdRepository.getMonsterByName(context, nom, worldId), graine(cle))
                }
                ButinsGeneres.enregistrer(cle, butin)
            }
            val entree = ButinsGeneres.entrees.value[cle] ?: return@forEach
            entree.butin.objets.forEachIndexed { index, objet ->
                if (index in entree.objetsRemis) return@forEachIndexed
                objets += objetsDuLibelle(objet.libelle)
                ButinsGeneres.marquerObjet(cle, index)
                retirerObjetDuPnj(pnj?.id, objet)
            }
            if (!entree.bourseRemise && !entree.butin.bourse.vide) {
                orPo += entree.butin.bourse.valeurPo
                ButinsGeneres.marquerBourse(cle)
                pnj?.let { GameState.payer(it.id, entree.butin.bourse.valeurPc) }
            }
        }

        withContext(Dispatchers.Main) {
            if (joueursConnectes) {
                objets.forEach { NetworkSessionManager.sendLootOfferToGroup(groupe.id, it) }
            }
            val actuel = GameState.mjGroups.value.firstOrNull { it.id == groupe.id } ?: groupe
            val inventaire = if (joueursConnectes) actuel.inventory else {
                objets.groupingBy { it }.eachCount().entries.fold(actuel.inventory) { inv, (nom, n) ->
                    val existant = inv.firstOrNull { it.name.equals(nom, ignoreCase = true) && it.location.isBlank() }
                    if (existant != null) inv.map { if (it.id == existant.id) it.copy(quantity = it.quantity + n) else it }
                    else inv + GroupItem(name = nom, quantity = n)
                }
            }
            if (orPo > 0 || inventaire != actuel.inventory) {
                GameState.updateMjGroup(actuel.copy(gold = actuel.gold + orPo, inventory = inventaire))
            }
        }
        Bilan(groupe.name, objets, orPo, inventaireCommun = !joueursConnectes && objets.isNotEmpty())
    }
}
