package com.jc2.jdrcompagnon.feature_carte.domain.usecase

import com.jc2.jdrcompagnon.feature_carte.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire

/**
 * Table d'événements aléatoires génériques fournie par l'app (non stockée en base, campagneId
 * toujours null). Le MJ peut en tirer un pour animer un trajet, en plus de ses événements
 * personnalisés par campagne.
 */
class GenererEvenementsGeneriquesUseCase {

    private val evenements = listOf(
        EvenementAleatoire(
            id = "gen-rumeur-locale", campagneId = null,
            titre = "Rumeur locale",
            description = "Un voyageur croisé sur la route partage une rumeur sur la région (trésor caché, danger imminent, personnage important).",
            effets = listOf(EffetEvenement.Information("Une rumeur locale à exploiter en jeu."))
        ),
        EvenementAleatoire(
            id = "gen-marchand-ambulant", campagneId = null,
            titre = "Marchand ambulant",
            description = "Le groupe croise un marchand itinérant proposant quelques objets à prix variable.",
            effets = listOf(EffetEvenement.Information("Occasion d'achat/vente improvisée."))
        ),
        EvenementAleatoire(
            id = "gen-embuscade-evitee", campagneId = null,
            titre = "Embuscade évitée",
            description = "Des traces suspectes permettent au groupe d'éviter une embuscade de justesse.",
            effets = listOf(EffetEvenement.Information("Pas de combat, mais une menace identifiée sur la route."))
        ),
        EvenementAleatoire(
            id = "gen-aide-pnj", campagneId = null,
            titre = "Aide à un PNJ",
            description = "Un villageois en difficulté demande l'aide du groupe (roue cassée, animal égaré, blessé léger).",
            effets = listOf(EffetEvenement.GainReputation("Habitants locaux", 5))
        ),
        EvenementAleatoire(
            id = "gen-raccourci", campagneId = null,
            titre = "Raccourci découvert",
            description = "Un guide local ou un indice sur une carte révèle un raccourci praticable.",
            effets = listOf(EffetEvenement.Information("Un raccourci à noter sur la carte."))
        ),
        EvenementAleatoire(
            id = "gen-mauvais-temps", campagneId = null,
            titre = "Mauvais temps",
            description = "Une tempête ou une pluie battante ralentit la progression du groupe.",
            effets = listOf(EffetEvenement.Information("Le trajet en cours prend plus de temps que prévu."))
        ),
        EvenementAleatoire(
            id = "gen-patrouille", campagneId = null,
            titre = "Patrouille locale",
            description = "Une patrouille (garde, milice, faction locale) croise le groupe et l'interroge.",
            effets = listOf(EffetEvenement.Information("Contrôle de routine, éventuel accroc selon la réponse du groupe."))
        ),
        EvenementAleatoire(
            id = "gen-marque-confiance", campagneId = null,
            titre = "Marque de confiance",
            description = "Le groupe rend un service discret à un notable local, qui s'en souviendra.",
            effets = listOf(EffetEvenement.GainReputation("Notables locaux", 10))
        ),
        EvenementAleatoire(
            id = "gen-mefiance", campagneId = null,
            titre = "Incident maladroit",
            description = "Une maladresse ou un malentendu du groupe agace des habitants du coin.",
            effets = listOf(EffetEvenement.GainReputation("Habitants locaux", -5))
        ),
        EvenementAleatoire(
            id = "gen-vestige", campagneId = null,
            titre = "Vestige oublié",
            description = "Le groupe repère les restes d'un campement ou d'une bataille ancienne en chemin.",
            effets = listOf(EffetEvenement.Information("Un indice ou un objet abandonné à décrire."))
        ),
        EvenementAleatoire(
            id = "gen-animal-sauvage", campagneId = null,
            titre = "Animal sauvage",
            description = "Une faune locale (pas nécessairement hostile) croise la route du groupe.",
            effets = listOf(EffetEvenement.Information("Opportunité de rôle-play ou de petite rencontre."))
        ),
        EvenementAleatoire(
            id = "gen-messager", campagneId = null,
            titre = "Messager pressé",
            description = "Un messager croise le groupe, porteur d'une nouvelle qui peut concerner l'intrigue en cours.",
            effets = listOf(EffetEvenement.Information("Une nouvelle fraîche à intégrer au scénario."))
        ),
        EvenementAleatoire(
            id = "gen-troc", campagneId = null,
            titre = "Troc improvisé",
            description = "Des voyageurs proposent un échange d'objets plutôt qu'une vente classique.",
            effets = listOf(EffetEvenement.Information("Occasion de troc à négocier."))
        ),
        EvenementAleatoire(
            id = "gen-panne-materiel", campagneId = null,
            titre = "Incident matériel",
            description = "Une roue, une sangle ou un équipement cède en cours de route.",
            effets = listOf(EffetEvenement.Information("Réparation ou détour nécessaire."))
        ),
        EvenementAleatoire(
            id = "gen-faveur-rendue", campagneId = null,
            titre = "Faveur rendue",
            description = "Un PNJ déjà croisé auparavant reconnaît le groupe et lui rend service.",
            effets = listOf(EffetEvenement.GainReputation("Alliés de voyage", 8))
        )
    )

    operator fun invoke(): List<EvenementAleatoire> = evenements
}
