package com.jc2.jdrcompagnon.ui.screens.joueur.character

import android.content.Context
import com.jc2.jdrcompagnon.feature_combat.domain.model.lancerFormuleDes
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EffetActif
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import kotlin.random.Random

/**
 * Durée d'un effet lue dans un texte de règles : ligne « Durée : … » d'un sort
 * (sorts_srd521.md) ou « pendant 1 heure » dans la description d'un objet. Les dés sont lancés
 * (« pendant 2d4 minutes »). Null pour un effet instantané ou sans durée reconnue.
 */
object DureeEffet {

    data class Duree(val secondes: Long, val libelle: String, val concentration: Boolean = false)

    private const val QUANTITE = """(\d+\s*d\s*\d+(?:\s*[+\-−]\s*\d+)?|\d+)"""
    private val ligneDureeSort = Regex("""(?im)^[*\s]*Durée[*\s]*:[*\s]*(.+)$""")
    private val quantiteUnite = Regex("""$QUANTITE\s*(round|minute|heure|jour)s?""", RegexOption.IGNORE_CASE)
    private val pendant = Regex("""(?:pendant|durant)\s+$QUANTITE\s*(round|minute|heure|jour)s?""", RegexOption.IGNORE_CASE)

    /** Durée d'un sort d'après sa ligne « Durée : ». */
    fun depuisSort(rawMarkdown: String, aleatoire: Random = Random): Duree? {
        val ligne = ligneDureeSort.find(rawMarkdown)?.groupValues?.get(1)?.trim() ?: return null
        if (ligne.contains("instantan", ignoreCase = true)) return null
        val m = quantiteUnite.find(ligne) ?: return null
        val secondes = secondes(m.groupValues[1], m.groupValues[2], aleatoire) ?: return null
        return Duree(secondes, ligne.trimEnd('.'), concentration = ligne.contains("concentration", ignoreCase = true))
    }

    /** Durée d'un objet d'après sa description (« pendant 1 heure après avoir bu cette potion »). */
    fun depuisDescription(texte: String, aleatoire: Random = Random): Duree? {
        val m = pendant.find(texte) ?: return null
        val secondes = secondes(m.groupValues[1], m.groupValues[2], aleatoire) ?: return null
        return Duree(secondes, libelle(secondes))
    }

    /** Durée telle qu'écrite dans la description (« 1 heure », « 2d4 minutes »), sans lancer de dé. */
    fun texteDescription(texte: String): String? =
        pendant.find(texte)?.let { "${it.groupValues[1]} ${it.groupValues[2].lowercase()}(s)" }

    private fun secondes(quantite: String, unite: String, aleatoire: Random): Long? {
        val q = quantite.replace(" ", "").replace('−', '-')
        val nombre = if ('d' in q.lowercase()) lancerFormuleDes(q, aleatoire) else q.toIntOrNull()
        nombre ?: return null
        val parUnite = when (unite.lowercase()) {
            "round" -> ScenarioClockState.SECONDES_PAR_ROUND.toLong()
            "minute" -> 60L
            "heure" -> 3600L
            else -> 86_400L
        }
        return nombre * parUnite
    }

    /** « 1 h 30 min », « 8 min », « 42 s »... pour afficher une durée ou un temps restant. */
    fun libelle(secondes: Long): String {
        val s = secondes.coerceAtLeast(0)
        val j = s / 86_400
        val h = s % 86_400 / 3600
        val min = s % 3600 / 60
        val sec = s % 60
        return listOfNotNull(
            j.takeIf { it > 0 }?.let { "$it j" },
            h.takeIf { it > 0 }?.let { "$it h" },
            min.takeIf { it > 0 }?.let { "$it min" },
            sec.takeIf { it > 0 && j == 0L && h == 0L }?.let { "$it s" },
        ).joinToString(" ").ifEmpty { "0 s" }
    }
}

/**
 * Utilisation d'un objet consommable (potion, poussière de disparition...) ou d'un sort à
 * durée : l'objet est retiré de l'inventaire (un exemplaire), les soins décrits sont appliqués
 * (« Vous regagnez 2d4 + 2 points de vie ») et l'effet à durée est ajouté à la fiche
 * ([EffetActif]), qui s'écoule avec l'horloge de scénario.
 */
object Consommables {

    private val soinRegex = Regex("""regagnez\s+(\d+\s*d\s*\d+(?:\s*\+\s*\d+)?)\s+points?\s+de\s+vie""", RegexOption.IGNORE_CASE)

    /** Consommables équipés (emplacements, accessoires de dos, extérieur du sac) : prêts à l'emploi en combat. */
    suspend fun equipesDuPersonnage(context: Context, character: Character): List<EquipmentItem> {
        val equipes = (character.equippedSlots.values + character.equippedItems + character.backpackExteriorSlots.values + character.quiverContents)
            .map { it.lowercase() }.toSet()
        if (equipes.isEmpty()) return emptyList()
        val worldId = character.worldId.ifBlank { GameState.currentWorldId() ?: "donjon_et_dragon" }
        return SrdRepository.loadEquipmentList(context, worldId)
            .filter { it.consommable && it.name.lowercase() in equipes }
            .distinctBy { it.name.lowercase() }
    }

    /** Résumé court de l'effet d'un consommable (« soigne 2d4+2 PV · effet 1 h »), pour les listes. */
    fun resume(item: EquipmentItem): String = listOfNotNull(
        formuleSoin(item)?.let { "soigne $it PV" },
        // Durée telle qu'écrite (« 2d4 minutes ») : ses dés ne sont lancés qu'à l'utilisation.
        DureeEffet.texteDescription(item.rawMarkdown)?.let { "effet $it" },
    ).joinToString(" · ").ifEmpty { "Consommable" }

    /** Formule de soins décrite par l'objet (« 2d4 + 2 »), null s'il ne soigne pas. */
    fun formuleSoin(item: EquipmentItem): String? =
        soinRegex.find(item.rawMarkdown)?.groupValues?.get(1)?.replace(" ", "")

    fun utiliser(character: Character, item: EquipmentItem, aleatoire: Random = Random): String {
        GameState.consommerObjet(character.id, item.name)
        val lignes = mutableListOf("${character.name} utilise ${item.name}.")

        formuleSoin(item)?.let { formule ->
            val soin = lancerFormuleDes(formule, aleatoire) ?: return@let
            val pv = (character.currentHitPoints + soin).coerceAtMost(character.maxHitPoints)
            GameState.updateCharacterHp(character.id, pv)
            lignes += "Soins : $formule = $soin → $pv/${character.maxHitPoints} PV."
        }

        DureeEffet.depuisDescription(item.rawMarkdown, aleatoire)?.let { duree ->
            val maintenant = ScenarioClockState.totalSeconds()
            GameState.ajouterEffet(
                character.id,
                EffetActif(
                    nom = item.name.removePrefix("Potion de ").removePrefix("Potion d'").replaceFirstChar { it.uppercase() },
                    source = item.name,
                    description = item.rawMarkdown.substringAfter(". ").take(300),
                    debutSecondes = maintenant,
                    finSecondes = maintenant + duree.secondes,
                )
            )
            lignes += "Effet actif pendant ${duree.libelle}."
        }
        return lignes.joinToString(" ")
    }

    /**
     * Lance un sort à durée : ajoute son effet à la fiche. Un nouveau sort de concentration met
     * fin au précédent (règle de concentration). Null si le sort n'a pas de durée (instantané).
     */
    fun lancerSort(character: Character, sort: SrdEntry, aleatoire: Random = Random): String? {
        val duree = DureeEffet.depuisSort(sort.rawMarkdown, aleatoire) ?: return null
        val maintenant = ScenarioClockState.totalSeconds()
        val interrompus = if (duree.concentration) character.effetsActifs.filter { it.concentration } else emptyList()
        interrompus.forEach { GameState.retirerEffet(character.id, it.id) }
        GameState.ajouterEffet(
            character.id,
            EffetActif(
                nom = sort.name,
                source = "Sort",
                description = duree.libelle,
                debutSecondes = maintenant,
                finSecondes = maintenant + duree.secondes,
                concentration = duree.concentration,
            )
        )
        return "${sort.name} actif (${duree.libelle})." +
            if (interrompus.isNotEmpty()) " Concentration rompue : ${interrompus.joinToString { it.nom }}." else ""
    }
}
