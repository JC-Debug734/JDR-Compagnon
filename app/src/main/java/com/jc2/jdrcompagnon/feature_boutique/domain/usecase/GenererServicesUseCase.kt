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
        val prixBaseEnPc: Int, // pièces de cuivre (1 po = 100 pc)
        val quantiteDisponible: Int? = null,
        val standingMinimum: StandingBoutique = StandingBoutique.MODESTE
    )

    private val catalogue: Map<TypeBoutique, List<ModeleService>> = mapOf(
        TypeBoutique.AUBERGE to listOf(
            ModeleService("Lit en dortoir", "Une paillasse dans le dortoir commun, pour la nuit.", 50, quantiteDisponible = 6),
            ModeleService("Chambre privée", "Une chambre fermant à clé, avec un lit correct.", 800, quantiteDisponible = 3),
            ModeleService("Repas simple", "Soupe, pain et fromage.", 30),
            ModeleService("Repas copieux", "Un vrai repas chaud avec viande et vin.", 400, standingMinimum = StandingBoutique.PROSPERE),
            ModeleService("Boisson", "Bière, hydromel ou vin de la maison.", 4)
        ),
        TypeBoutique.TEMPLE to listOf(
            ModeleService("Bénédiction", "Une courte prière et une bénédiction du prêtre.", 50),
            ModeleService("Soins légers", "Imposition des mains pour soigner des blessures mineures.", 1000),
            ModeleService("Guérison de maladie", "Rituel de purification contre une maladie.", 5000, standingMinimum = StandingBoutique.CORRECT),
            ModeleService("Résurrection", "Rituel majeur pour ramener un défunt à la vie.", 200000, standingMinimum = StandingBoutique.LUXUEUX),
            ModeleService("Don au temple", "Offrande libre en échange de la bienveillance des dieux.", 0)
        ),
        TypeBoutique.GUILDE_RECRUTEMENT to listOf(
            ModeleService("Homme de main", "Recrue robuste sans qualification particulière, tarif journalier.", 200),
            ModeleService("Porteur", "Aide au transport des bagages, tarif journalier.", 20),
            ModeleService("Guide local", "Connaît la région et ses dangers, tarif journalier.", 500),
            ModeleService("Éclaireur", "Discret et observateur, tarif journalier.", 800, standingMinimum = StandingBoutique.CORRECT),
            ModeleService("Garde du corps", "Combattant expérimenté, tarif journalier.", 1500, standingMinimum = StandingBoutique.PROSPERE)
        ),
        TypeBoutique.ECURIE to listOf(
            ModeleService("Location de cheval (jour)", "Cheval de selle loué à la journée.", 400),
            ModeleService("Achat cheval de selle", "Monture d'usage courant.", 7500),
            ModeleService("Achat cheval de trait", "Monture robuste pour tirer une charge.", 5000),
            ModeleService("Achat cheval de guerre", "Monture entraînée au combat.", 40000, standingMinimum = StandingBoutique.PROSPERE),
            ModeleService("Réparation d'attelage", "Remise en état d'une calèche ou d'un chariot.", 1000)
        ),
        TypeBoutique.BANQUE to listOf(
            ModeleService("Prêt sur gage", "Avance de fonds contre un objet de valeur laissé en garantie.", 0),
            ModeleService("Change de devises", "Conversion entre monnaies locales et étrangères.", 0),
            ModeleService("Location de coffre-fort", "Emplacement sécurisé pour entreposer des biens, tarif mensuel.", 500)
        ),
        TypeBoutique.GUILDE_MAGES to listOf(
            ModeleService("Identification d'objet", "Détermine les propriétés magiques d'un objet.", 2000),
            ModeleService("Copie de sort", "Transcription d'un sort dans un grimoire.", 5000),
            ModeleService("Enchantement mineur", "Ajout d'une propriété magique simple à un objet.", 15000, standingMinimum = StandingBoutique.PROSPERE),
            ModeleService("Enchantement majeur", "Ajout d'une propriété magique puissante à un objet.", 100000, standingMinimum = StandingBoutique.LUXUEUX)
        ),
        TypeBoutique.VENDEUR_AMBULANT to listOf(
            ModeleService("Repas sur le pouce", "Galette, saucisse sèche et un peu de bière, servis au bord de la route.", 20),
            ModeleService("Rumeurs de la route", "Le colporteur raconte ce qu'il a vu et entendu en chemin.", 10)
        )
    )

    /** Prix de catalogue (en pc, avant standing) d'un service généré portant ce nom ; null = inconnu. */
    fun prixDeBase(nom: String): Int? =
        catalogue.values.flatten().firstOrNull { it.nom.equals(nom.trim(), ignoreCase = true) }?.prixBaseEnPc

    operator fun invoke(
        type: TypeBoutique,
        standing: StandingBoutique,
        random: Random = Random.Default
    ): List<Service> {
        val modeles = catalogue[type] ?: return emptyList()

        return modeles
            .filter { standing.ordinal >= it.standingMinimum.ordinal }
            .map { modele ->
                val prixAjuste = (modele.prixBaseEnPc * standing.multiplicateurPrix).toInt()
                val variation = if (prixAjuste > 0) random.nextInt(-prixAjuste / 5, prixAjuste / 5 + 1) else 0
                Service(
                    nom = modele.nom,
                    description = modele.description,
                    prixEnPc = (prixAjuste + variation).coerceAtLeast(0),
                    quantiteDisponible = modele.quantiteDisponible
                )
            }
    }
}
