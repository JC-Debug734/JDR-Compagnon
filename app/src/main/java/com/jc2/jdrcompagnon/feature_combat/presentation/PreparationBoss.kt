package com.jc2.jdrcompagnon.feature_combat.presentation

import android.content.Context
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossCatalogue
import com.jc2.jdrcompagnon.feature_combat.domain.model.BossDefinition
import com.jc2.jdrcompagnon.feature_combat.domain.model.Combattant
import com.jc2.jdrcompagnon.feature_combat.domain.model.EtatBoss
import com.jc2.jdrcompagnon.feature_combat.domain.model.ProfilCombatMonstre
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.feature_group.domain.EncounterDifficulty
import java.util.UUID

/**
 * Boss prêt à entrer en combat : ses combattants (le boss, puis ses points faibles) et son état
 * de boss, renforts de chaque phase déjà chargés depuis le bestiaire.
 */
data class BossPrepare(
    val bossCombattantId: String,
    val combattants: List<Combattant>,
    val etat: EtatBoss,
)

object PreparationBoss {

    /** Profil de la fiche de base du boss (null si elle est introuvable dans le bestiaire). */
    suspend fun profil(context: Context, def: BossDefinition, worldId: String?): ProfilCombatMonstre? =
        SrdRepository.getMonsterByName(context, def.monstreBase, worldId)?.let { ProfilCombatMonstre.depuisFiche(it.rawMarkdown) }

    /**
     * Prépare [def] pour un groupe de [nbPersonnages] et la [difficulte] choisie (null = scénario) :
     * PV, actions et résistances légendaires ajustés, comportement
     * de sa première phase, points faibles (inertes) et renforts des phases suivantes.
     */
    suspend fun preparer(
        context: Context,
        def: BossDefinition,
        worldId: String?,
        nbPersonnages: Int,
        difficulte: EncounterDifficulty? = null,
    ): BossPrepare {
        val profil = profil(context, def, worldId) ?: ProfilCombatMonstre(15, def.pvPour4 ?: 100, null, 0)
        val pv = BossCatalogue.pvAjustes(def, profil.pvMoyens, nbPersonnages, difficulte)
        val premiere = def.phases.firstOrNull()
        val boss = profil.versCombattant(def.nom, def.monstreBase, pv).copy(
            bossId = def.id,
            profilIA = premiere?.profil ?: profil.comportement.profil,
            raisonProfil = "Boss" + (premiere?.let { " — phase « ${it.nom} »" } ?: ""),
        )
        val pointsFaibles = def.pointsFaibles.map { p ->
            Combattant(
                id = UUID.randomUUID().toString(),
                nom = p.nom,
                estMonstre = true,
                ca = p.ca,
                pvMax = p.pv,
                pv = p.pv,
                bonusInitiative = 0,
                // Ne joue pas : son initiative ne sert qu'à le ranger en fin d'ordre.
                initiative = 0,
                typeCreature = "Objet",
                defenses = p.defenses,
                inerte = true,
            )
        }
        val renforts = def.phases.withIndex().filter { it.value.renforts.isNotEmpty() }.associate { (i, phase) ->
            i to phase.renforts.flatMap { ligne ->
                val fiche = SrdRepository.getMonsterByName(context, ligne.monstreNom, worldId)
                val p = fiche?.let { ProfilCombatMonstre.depuisFiche(it.rawMarkdown) } ?: return@flatMap emptyList()
                (1..ligne.quantite).map { n ->
                    p.versCombattant(if (ligne.quantite > 1) "${fiche.name} $n (renfort)" else "${fiche.name} (renfort)", fiche.name)
                }
            }
        }
        val etat = EtatBoss(
            legendairesAuto = ReglagesCombatStore.courant.legendairesAuto,
            definition = def,
            caBase = boss.ca,
            pointsMaxBase = BossCatalogue.pointsAjustes(def, nbPersonnages, difficulte),
            resistances = BossCatalogue.resistancesAjustees(def, difficulte),
            pointsFaiblesIds = pointsFaibles.map { it.id },
            renforts = renforts,
        )
        return BossPrepare(boss.id, listOf(boss) + pointsFaibles, etat)
    }
}
