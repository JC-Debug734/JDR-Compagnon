package com.jc2.jdrcompagnon.feature_boutique.domain.usecase

import com.jc2.jdrcompagnon.feature_boutique.domain.model.Service
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import kotlin.random.Random

/**
 * Génère le catalogue de services d'une boutique de type != MARCHAND (auberge, temple, guilde...).
 * Catalogue codé en dur par type, filtré/mis à l'échelle par le standing :
 * - le prix de base de chaque service est multiplié par standing.multiplicateurPrix (+ un peu
 *   d'aléatoire) ;
 * - certaines entrées haut de gamme n'apparaissent qu'à partir d'un standing minimum.
 */
class GenererServicesUseCase {

    private data class ModeleService(
        val nom: String,
        val description: String,
        val prixBaseEnPo: Int,
        val quantiteDisponible: Int? = null,
        val standingMinimum: StandingBoutique = StandingBoutique.MODESTE
    )

    private val catalogue: Map<TypeBoutique, List<ModeleService>> = mapOf(
        TypeBoutique.AUBERGE to listOf(
            ModeleService("Lit en dortoir", "Une paillasse dans le dortoir commun, pour la nuit.", 2, quantiteDisponible = 6),
            ModeleService("Chambre privée", "Une chambre fermant à clé, avec un lit correct.", 8, quantiteDisponible = 3),
            ModeleService("Repas simple", "Soupe, pain et fromage.", 1),
            ModeleService("Repas copieux", "Un vrai repas chaud avec viande et vin.", 4, standingMinimum = StandingBoutique.PROSPERE),
            ModeleService("Boisson", "Bière, hydromel ou vin de la maison.", 1)
        ),
        TypeBoutique.TEMPLE to listOf(
            ModeleService("Bénédiction", "Une courte prière et une bénédiction du prêtre.", 1),
            ModeleService("Soins légers", "Imposition des mains pour soigner des blessures mineures.", 10),
            ModeleService("Guérison de maladie", "Rituel de purification contre une maladie.", 50, standingMinimum = StandingBoutique.CORRECT),
            ModeleService("Résurrection", "Rituel majeur pour ramener un défunt à la vie.", 2000, standingMinimum = StandingBoutique.LUXUEUX),
            ModeleService("Don au temple", "Offrande libre en échange de la bienveillance des dieux.", 0)
        ),
        TypeBoutique.GUILDE_RECRUTEMENT to listOf(
            ModeleService("Homme de main", "Recrue robuste sans qualification particulière, tarif journalier.", 2),
            ModeleService("Porteur", "Aide au transport des bagages, tarif journalier.", 2),
            ModeleService("Guide local", "Connaît la région et ses dangers, tarif journalier.", 5),
            ModeleService("Éclaireur", "Discret et observateur, tarif journalier.", 8, standingMinimum = StandingBoutique.CORRECT),
            ModeleService("Garde du corps", "Combattant expérimenté, tarif journalier.", 15, standingMinimum = StandingBoutique.PROSPERE)
        ),
        TypeBoutique.ECURIE to listOf(
            ModeleService("Location de cheval (jour)", "Cheval de selle loué à la journée.", 4),
            ModeleService("Achat cheval de selle", "Monture d'usage courant.", 75),
            ModeleService("Achat cheval de trait", "Monture robuste pour tirer une charge.", 50),
            ModeleService("Achat cheval de guerre", "Monture entraînée au combat.", 400, standingMinimum = StandingBoutique.PROSPERE),
            ModeleService("Réparation d'attelage", "Remise en état d'une calèche ou d'un chariot.", 10)
        ),
        TypeBoutique.BANQUE to listOf(
            ModeleService("Prêt sur gage", "Avance de fonds contre un objet de valeur laissé en garantie.", 0),
            ModeleService("Change de devises", "Conversion entre monnaies locales et étrangères.", 0),
            ModeleService("Location de coffre-fort", "Emplacement sécurisé pour entreposer des biens, tarif mensuel.", 5)
        ),
        TypeBoutique.GUILDE_MAGES to listOf(
            ModeleService("Identification d'objet", "Détermine les propriétés magiques d'un objet.", 20),
            ModeleService("Copie de sort", "Transcription d'un sort dans un grimoire.", 50),
            ModeleService("Enchantement mineur", "Ajout d'une propriété magique simple à un objet.", 150, standingMinimum = StandingBoutique.PROSPERE),
            ModeleService("Enchantement majeur", "Ajout d'une propriété magique puissante à un objet.", 1000, standingMinimum = StandingBoutique.LUXUEUX)
        )
    )

    operator fun invoke(
        type: TypeBoutique,
        standing: StandingBoutique,
        random: Random = Random.Default
    ): List<Service> {
        val modeles = catalogue[type] ?: return emptyList()

        return modeles
            .filter { standing.ordinal >= it.standingMinimum.ordinal }
            .map { modele ->
                val prixAjuste = (modele.prixBaseEnPo * standing.multiplicateurPrix).toInt()
                val variation = if (prixAjuste > 0) random.nextInt(-prixAjuste / 5, prixAjuste / 5 + 1) else 0
                Service(
                    nom = modele.nom,
                    description = modele.description,
                    prixEnPo = (prixAjuste + variation).coerceAtLeast(0),
                    quantiteDisponible = modele.quantiteDisponible
                )
            }
    }
}
