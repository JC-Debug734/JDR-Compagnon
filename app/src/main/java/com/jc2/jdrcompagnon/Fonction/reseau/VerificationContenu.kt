package com.jc2.jdrcompagnon.network

import android.content.Context
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.ClasseParser
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.sousClasseDuPersonnage
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Vérifie, côté joueur, que sa bibliothèque (contenu de base et imports) contient ce que
 * référence un personnage ou un objet reçu du MJ : classes, sous-classes, espèce, historique,
 * sorts et objets (magiques ou non). Ce qui manque est signalé au MJ
 * (NetworkMessage.TYPE_CONTENT_MISSING) — en général un import qu'il n'a pas partagé ou que le
 * joueur n'a pas téléchargé (voir [ImportsPartages]).
 *
 * Une catégorie absente de tout le monde (ex. aucune classe dans un univers sans création de
 * personnage) n'est pas vérifiée : rien n'y serait jamais trouvé.
 */
object VerificationContenu {

    private const val MONDE_DEFAUT = "donjon_et_dragon"

    private fun memeNom(a: String, b: String) = a.trim().equals(b.trim(), ignoreCase = true)

    /** Noms connus des objets du monde : équipement (imports compris) et armes/armures magiques. */
    private suspend fun nomsObjets(context: Context, monde: String): List<String> =
        SrdRepository.loadEquipmentList(context, monde).map { it.name } +
            SrdRepository.loadArmesArmuresMagiques(context, monde).map { it.name }

    /**
     * Un objet est connu s'il porte le nom d'un objet de la bibliothèque, ou en reprend le nom
     * (« Flèche (20) », « Épée longue +1 » à partir d'« Épée longue »), comme les autres écrans
     * qui retrouvent un objet par son nom (SrdRepository.findEquipmentItemCached).
     */
    private fun objetConnu(nom: String, connus: List<String>): Boolean {
        val n = nom.trim().lowercase()
        return connus.any { c -> val k = c.trim().lowercase(); k.isNotEmpty() && (k == n || n.contains(k) || k.contains(n)) }
    }

    suspend fun manquantsPersonnage(context: Context, c: Character): List<String> {
        val monde = c.worldId.ifBlank { MONDE_DEFAUT }
        val manquants = mutableListOf<String>()

        val classes = ClasseParser.parse(SrdRepository.loadClasses(context, monde).joinToString("\n\n") { it.rawMarkdown })
        if (classes.isNotEmpty()) {
            (listOf(c.characterClass) + c.classesSecondaires.map { it.classe }).filter { it.isNotBlank() }.distinct().forEach { nom ->
                val classe = classes.firstOrNull { memeNom(it.nom, nom) }
                if (classe == null) {
                    manquants += "Classe « $nom »"
                    return@forEach
                }
                val sousClasse = sousClasseDuPersonnage(c, nom)
                if (sousClasse.isNotBlank() && classe.sousClasses.none { memeNom(it.nom, sousClasse) }) {
                    manquants += "Sous-classe « $sousClasse » ($nom)"
                }
            }
        }

        val especes = SrdRepository.loadEspeces(context, monde)
        // L'espèce peut être précisée (« Elfe (drow) ») : son nom de base suffit.
        if (c.race.isNotBlank() && especes.isNotEmpty() &&
            especes.none { memeNom(it.name, c.race) || c.race.trim().lowercase().startsWith(it.name.trim().lowercase()) }
        ) manquants += "Espèce « ${c.race} »"

        val historiques = SrdRepository.loadHistoriques(context, monde)
        if (c.background.isNotBlank() && historiques.isNotEmpty() && historiques.none { memeNom(it.name, c.background) }) {
            manquants += "Historique « ${c.background} »"
        }

        val sorts = SrdRepository.loadSpells(context, monde).map { it.name }
        if (sorts.isNotEmpty()) {
            (c.spells + c.sortsSpeciaux.keys).distinct()
                .filter { s -> s.isNotBlank() && sorts.none { memeNom(it, s) } }
                .forEach { manquants += "Sort « $it »" }
        }

        val objets = nomsObjets(context, monde)
        if (objets.isNotEmpty()) {
            (c.backpackItems + c.equippedItems + c.equippedSlots.values + c.quiverContents +
                c.backpackExteriorSlots.values + c.attunedItems)
                .map { it.trim() }.filter { it.isNotBlank() }.distinct()
                .filter { !objetConnu(it, objets) }
                .forEach { manquants += "Objet « $it »" }
        }
        return manquants
    }

    suspend fun manquantsObjet(context: Context, nom: String, worldId: String?): List<String> {
        val objets = nomsObjets(context, worldId?.ifBlank { null } ?: MONDE_DEFAUT)
        return if (objets.isEmpty() || objetConnu(nom, objets)) emptyList() else listOf("Objet « $nom »")
    }
}
