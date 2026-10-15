package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.di.BoutiqueDependencies
import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.Monnaie
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

/**
 * Achat et vente d'objets entre un personnage et une boutique (écran de commerce du menu joueur,
 * cf. CommerceLieuDialog). Appliqué chez le MJ en partie réseau (seule source de vérité du stock
 * et de la caisse), sinon directement sur l'appareil.
 *
 * - Achat : l'article doit être en stock et le personnage assez riche ; la boutique encaisse.
 * - Vente : le marchand rachète à la moitié du coût de base SRD (règle D&D), dans la limite de sa
 *   caisse ; l'objet rejoint son stock au prix de son standing.
 *
 * Tous les montants sont en pièces de cuivre (1 po = 100 pc, cf. Monnaie) ; le personnage paie avec
 * toutes les pièces de sa bourse et reçoit la monnaie.
 */
object CommerceBoutiqueUseCase {

    data class Resultat(val accepte: Boolean, val message: String)

    // Deux joueurs qui achètent le dernier exemplaire en même temps : une transaction à la fois.
    private val verrou = Mutex()

    /** Prix de rachat d'un objet par un marchand (en pc) : moitié de son coût de base, 1 pc minimum. */
    fun prixRachat(coutBaseEnPc: Int): Int = (coutBaseEnPc / 2.0).roundToInt().coerceAtLeast(1)

    /** Prix de rachat de [nomObjet] en pc (null = objet que le marchand ne sait pas estimer, cf. EquipementSourcePort). */
    suspend fun prixRachat(nomObjet: String, monde: String?): Int? =
        BoutiqueDependencies.equipementSrd(nomObjet, monde)?.let { prixRachat(it.coutBaseEnPc) }

    suspend fun acheter(boutiqueId: String, characterId: String, objetNom: String): Resultat = verrou.withLock {
        val repo = BoutiqueDependencies.repository
        val boutique = repo.getBoutique(boutiqueId) ?: return Resultat(false, "Cette boutique n'existe plus.")
        val perso = GameState.characters.value.firstOrNull { it.id == characterId }
            ?: return Resultat(false, "Personnage introuvable.")
        val article = boutique.inventaire.firstOrNull { it.equipement.nom == objetNom }
            ?: return Resultat(false, "« $objetNom » n'est plus en vente chez ${boutique.nom}.")
        if (article.quantiteStock <= 0) return Resultat(false, "« $objetNom » est épuisé.")
        if (Monnaie.totalPc(perso) < article.prixEnPc) {
            return Resultat(false, "${perso.name} n'a pas assez d'argent (${Monnaie.libelle(article.prixEnPc)} demandés, bourse : ${Monnaie.libelleBourse(perso)}).")
        }
        repo.sauvegarderBoutique(
            boutique.copy(
                inventaire = boutique.inventaire.map { if (it === article) it.copy(quantiteStock = it.quantiteStock - 1) else it },
                argentDisponibleEnPc = boutique.argentDisponibleEnPc + article.prixEnPc,
            )
        )
        GameState.payer(characterId, article.prixEnPc)
        GameState.addItemToBackpack(characterId, objetNom)
        Resultat(true, "${perso.name} achète « $objetNom » pour ${Monnaie.libelle(article.prixEnPc)}.")
    }

    suspend fun vendre(boutiqueId: String, characterId: String, objetNom: String): Resultat = verrou.withLock {
        val repo = BoutiqueDependencies.repository
        val boutique = repo.getBoutique(boutiqueId) ?: return Resultat(false, "Cette boutique n'existe plus.")
        val perso = GameState.characters.value.firstOrNull { it.id == characterId }
            ?: return Resultat(false, "Personnage introuvable.")
        if (objetNom !in perso.backpackItems) return Resultat(false, "« $objetNom » n'est plus dans le sac de ${perso.name}.")
        val enStock = boutique.inventaire.firstOrNull { it.equipement.nom.equals(objetNom, ignoreCase = true) }
        val reference = BoutiqueDependencies.equipementSrd(objetNom, perso.worldId) ?: enStock?.equipement
            ?: return Resultat(false, "${boutique.marchand.nom} ne sait pas estimer « $objetNom ».")
        val prix = prixRachat(reference.coutBaseEnPc)
        if (boutique.argentDisponibleEnPc < prix) {
            return Resultat(false, "${boutique.marchand.nom} n'a plus assez d'argent pour racheter « $objetNom ».")
        }
        val inventaire = if (enStock != null) {
            boutique.inventaire.map { if (it === enStock) it.copy(quantiteStock = it.quantiteStock + 1) else it }
        } else {
            boutique.inventaire + ArticleEnVente(
                equipement = EquipementReference(reference.nom, reference.coutBaseEnPc, reference.type),
                prixEnPc = BoutiqueDependencies.prixEnBoutique(reference.coutBaseEnPc, boutique.standing),
                quantiteStock = 1,
            )
        }
        repo.sauvegarderBoutique(boutique.copy(inventaire = inventaire, argentDisponibleEnPc = boutique.argentDisponibleEnPc - prix))
        GameState.removeFromBackpack(characterId, objetNom, 1)
        GameState.recevoirPc(characterId, prix)
        Resultat(true, "${perso.name} vend « $objetNom » pour ${Monnaie.libelle(prix)}.")
    }
}
