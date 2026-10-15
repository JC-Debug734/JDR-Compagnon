package com.jc2.jdrcompagnon.feature_boutique.domain.model

import kotlinx.serialization.Serializable

/**
 * Service rendu par une boutique (nuitée, soin, recrutement, réparation, etc.). Les boutiques
 * non MARCHAND en reçoivent à la génération ; le MJ peut en ajouter à toute boutique.
 */
@Serializable
data class Service(
    val nom: String,
    val description: String,
    val prixEnPc: Int, // en pièces de cuivre (1 po = 100 pc), cf. Monnaie
    val quantiteDisponible: Int? = null, // null = illimité (ex: repas), sinon places limitées (ex: chambres)
    val actif: Boolean = true, // le MJ peut désactiver un service sans le supprimer (ex: chambres toutes prises)
    // Ajouté ou modifié à la main par le MJ : conservé lors d'une nouvelle visite ou d'un
    // changement de type/standing (seuls les services générés sont renouvelés).
    val personnalise: Boolean = false,
    // Action sur l'application quand un joueur utilise le service (AUTO = déduite du nom).
    val effet: EffetService = EffetService.AUTO,
) {
    /** Effet réellement appliqué : celui choisi par le MJ, ou déduit du nom du service. */
    val effetReel: EffetService get() = if (effet == EffetService.AUTO) EffetService.deduire(nom) else effet
}

/**
 * Action d'un service sur la fiche du personnage qui l'utilise (voir EffetsServices) : un repas
 * remet le compteur de faim à zéro, une nuit en chambre déclenche un repos long, etc.
 */
@Serializable
enum class EffetService(val label: String) {
    AUTO("Automatique (d'après le nom)"),
    AUCUN("Aucun effet"),
    REPAS("Repas : faim remise à zéro"),
    NUIT("Nuit en chambre : repos long"),
    SOINS("Soins : PV au maximum"),
    GUERISON("Guérison : poison et maladie retirés"),
    BENEDICTION("Bénédiction : inspiration héroïque"),
    BAIN("Bain : un niveau d'épuisement en moins"),
    MONTURE("Monture : ajoutée aux possessions"),
    RECRUE("Recrue : ajoutée aux possessions (suivant)"),
    // Bonne aventure : trois cartes de Tarokka tirées (passé, présent, futur), voir TirageTarokka.
    TAROKKA("Bonne aventure : tirage de 3 cartes de Tarokka"),
    ;

    companion object {
        fun depuis(nom: String?): EffetService = entries.firstOrNull { it.name == nom } ?: AUTO

        private fun contient(nom: String, vararg mots: String) = mots.any { nom.contains(it, ignoreCase = true) }

        /** Effet déduit du nom d'un service (« Repas simple », « Chambre privée », « Soins légers »…). */
        fun deduire(nom: String): EffetService = when {
            contient(nom, "résurrection", "resurrection") -> AUCUN
            contient(nom, "bonne aventure") -> TAROKKA
            contient(nom, "repas", "ration", "dîner", "diner", "souper", "déjeuner", "dejeuner", "nourriture", "banquet") -> REPAS
            contient(nom, "chambre", "dortoir", "nuit", "nuitée", "lit ") || nom.trim().equals("lit", ignoreCase = true) -> NUIT
            contient(nom, "guérison", "guerison", "purification", "antidote", "remède", "remede") -> GUERISON
            contient(nom, "soin") -> SOINS
            contient(nom, "bénédiction", "benediction") -> BENEDICTION
            contient(nom, "bain", "thermes", "étuve") -> BAIN
            contient(nom, "cheval", "monture", "poney", "mule", "chameau") -> MONTURE
            contient(nom, "homme de main", "porteur", "guide", "éclaireur", "eclaireur", "garde du corps", "mercenaire") -> RECRUE
            else -> AUCUN
        }
    }
}
