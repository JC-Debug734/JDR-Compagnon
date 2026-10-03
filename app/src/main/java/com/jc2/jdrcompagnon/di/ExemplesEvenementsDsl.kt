package com.jc2.jdrcompagnon.di

import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.ProfilEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import kotlin.math.abs

// Petites fonctions pour écrire les événements d'exemple (EvenementDependencies et
// TableAleatoireDependencies) de façon lisible.

internal fun info(texte: String) = EffetEvenement(categorie = CategorieEffet.AUTRE, description = texte)

internal fun reputation(faction: String, delta: Int) = EffetEvenement(
    categorie = CategorieEffet.REPUTATION,
    gain = delta >= 0,
    quantite = abs(delta),
    cible = faction
)

internal fun or(delta: Int) = EffetEvenement(categorie = CategorieEffet.OR, gain = delta >= 0, quantite = abs(delta))

internal fun objet(nom: String, quantite: Int = 1) = EffetEvenement(categorie = CategorieEffet.OBJET, quantite = quantite, cible = nom)

// [titre] (facultatif) nomme le choix des joueurs quand il y en a plusieurs : "Négociation
// réussie", "Combat"... sinon le libellé de la nature est affiché.
internal fun reussite(description: String, vararg effets: EffetEvenement, titre: String = "", combat: Boolean = false) =
    IssueEvenement(nature = NatureIssue.REUSSITE, titre = titre, description = description, effets = effets.toList(), declencheCombat = combat)

internal fun partielle(description: String, vararg effets: EffetEvenement, titre: String = "", combat: Boolean = false) =
    IssueEvenement(nature = NatureIssue.PARTIELLE, titre = titre, description = description, effets = effets.toList(), declencheCombat = combat)

internal fun echec(description: String, vararg effets: EffetEvenement, titre: String = "", combat: Boolean = false) =
    IssueEvenement(nature = NatureIssue.ECHEC, titre = titre, description = description, effets = effets.toList(), declencheCombat = combat)

internal fun autre(titre: String, description: String, vararg effets: EffetEvenement, combat: Boolean = false) =
    IssueEvenement(nature = NatureIssue.AUTRE, titre = titre, description = description, effets = effets.toList(), declencheCombat = combat)

/** Monstre du bestiaire (nom exact de la fiche SRD), en [quantite] exemplaires. */
internal fun monstre(nom: String, quantite: Int = 1) = ProfilEvenement(type = TypeProfil.MONSTRE, nom = nom, quantite = quantite)
