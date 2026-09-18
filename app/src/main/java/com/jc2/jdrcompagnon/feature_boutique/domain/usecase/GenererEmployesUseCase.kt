package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.Employe
import com.jc2.jdrcompagnon.feature_boutique.domain.model.RoleEmploye
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import kotlin.random.Random

/**
 * Génère les employés (purement descriptifs, aucune stat/combat) selon le standing.
 * Une plage [min, max] d'effectif par standing, avec un pool de noms génériques
 * à remplacer par un générateur de noms fantasy si l'app en a déjà un ailleurs
 * (cf. NameEntryDialog qui génère déjà des noms aléatoires pour les joueurs).
 */
class GenererEmployesUseCase(
    private val genererTraitCaractere: GenererTraitCaractereUseCase
) {

    private companion object {
        val NOMS_GENERIQUES = listOf(
            "Bram", "Tessia", "Old Finn", "Yelena", "Corentin",
            "Maelys", "Doran", "Ilsa", "Perrin", "Osgood"
        )

        val PLAGE_EFFECTIF: Map<StandingBoutique, IntRange> = mapOf(
            StandingBoutique.MODESTE to 0..1,
            StandingBoutique.CORRECT to 1..2,
            StandingBoutique.PROSPERE to 2..3,
            StandingBoutique.LUXUEUX to 3..5
        )

        val ROLES_PAR_STANDING: Map<StandingBoutique, List<RoleEmploye>> = mapOf(
            StandingBoutique.MODESTE to listOf(RoleEmploye.VENDEUR),
            StandingBoutique.CORRECT to listOf(RoleEmploye.VENDEUR, RoleEmploye.APPRENTI),
            StandingBoutique.PROSPERE to listOf(
                RoleEmploye.VENDEUR, RoleEmploye.APPRENTI, RoleEmploye.COMPTABLE, RoleEmploye.ARTISAN
            ),
            StandingBoutique.LUXUEUX to RoleEmploye.entries.toList() // inclut GARDE
        )
    }

    operator fun invoke(standing: StandingBoutique, random: Random = Random.Default): List<Employe> {
        val effectif = random.nextInt(
            PLAGE_EFFECTIF.getValue(standing).first,
            PLAGE_EFFECTIF.getValue(standing).last + 1
        )
        val rolesDisponibles = ROLES_PAR_STANDING.getValue(standing)

        return (1..effectif).map {
            Employe(
                nom = NOMS_GENERIQUES.random(random),
                role = rolesDisponibles.random(random),
                trait = genererTraitCaractere(random)
            )
        }
    }
}