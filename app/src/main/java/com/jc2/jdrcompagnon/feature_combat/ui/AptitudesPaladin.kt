package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.feature_combat.domain.model.Paladin
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EffetActif
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ScenarioClockState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Aptitudes du Paladin à partir du niveau 3, avec leurs compteurs : Conduit divin (Perception
 * divine et options des serments), Frappes radiantes, aptitudes à utilisations des serments
 * (Défense glorieuse…), aptitude de niveau 20 (Nimbe sacré, Champion des Anciens, Légende
 * vivante, Ange vengeur), et le rappel des effets permanents (auras, Fidèle destrier…).
 * [onAnnonce] (libellé, texte) et [onDegats] (libellé, dés, formule) appliquent l'effet —
 * directement hors réseau, ou par envoi au MJ en combat — et renvoient le message à afficher, ou
 * null si l'envoi a échoué (la ressource n'est alors pas dépensée). [emplacementsRestants] permet
 * de récupérer l'aptitude de niveau 20 avec un emplacement du 5e niveau.
 */
@Composable
internal fun CarteAptitudesPaladin(
    personnage: Character,
    onAnnonce: (libelle: String, texte: String) -> String?,
    onDegats: (libelle: String, des: List<Int>, formule: String) -> String?,
    modifier: Modifier = Modifier,
    emplacementsRestants: Map<Int, Int> = emptyMap(),
) {
    if (!Paladin.aAptitudes(personnage)) return
    var message by remember { mutableStateOf<String?>(null) }
    val niveau = Paladin.niveau(personnage)
    val devotion = Paladin.niveauDevotion(personnage)
    val anciens = Paladin.niveauAnciens(personnage)
    val gloire = Paladin.niveauGloire(personnage)
    val vengeance = Paladin.niveauVengeance(personnage)
    val dd = Paladin.ddSorts(personnage)
    val conduitMax = Paladin.conduitDivinMax(personnage)
    val conduitRestant = Paladin.conduitDivinRestants(personnage)

    fun ajouterEffet(nom: String, duree: Long, description: String) {
        val maintenant = ScenarioClockState.totalSeconds()
        personnage.effetsActifs.filter { it.nom.equals(nom, ignoreCase = true) }.forEach { GameState.retirerEffet(personnage.id, it.id) }
        GameState.ajouterEffet(
            personnage.id,
            EffetActif(nom = nom, source = Paladin.CLASSE, description = description, debutSecondes = maintenant, finSecondes = maintenant + duree),
        )
    }

    fun terminerEffet(nom: String) {
        personnage.effetsActifs.filter { it.nom == nom }.forEach { GameState.retirerEffet(personnage.id, it.id) }
        message = "$nom terminé."
    }

    fun utiliserConduit(libelle: String, texte: String, effet: () -> Unit = {}) {
        val resultat = onAnnonce(libelle, texte)
        if (resultat != null) {
            GameState.utiliserConduitDivin(personnage.id, conduitMax)
            effet()
        }
        message = resultat ?: ECHEC_ENVOI
    }

    /** Option de Conduit divin qui pose un effet de [duree] secondes, avec « Terminer » tant qu'il est actif. */
    @Composable
    fun OptionAvecEffet(nom: String, detail: String, annonce: String, duree: Long, descriptionEffet: String) {
        val actif = Paladin.effetActif(personnage, nom)
        LignePaladin(
            nom + if (actif) " (actif)" else "",
            detail,
            bouton = when {
                actif -> "Terminer"
                conduitRestant > 0 -> "Utiliser"
                else -> null
            },
        ) {
            if (actif) terminerEffet(nom)
            else utiliserConduit(nom, annonce) { ajouterEffet(nom, duree, descriptionEffet) }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aptitudes du Paladin", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)

            // ── Conduit divin ──
            Text(
                "Conduit divin : $conduitRestant/$conduitMax (1 récupérée au repos court, toutes au repos long)",
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
            )
            LignePaladin(
                "Perception divine",
                "Action Bonus : vous détectez Célestes, Fiélons et Morts-vivants à 18 m (et leur type) pendant 10 minutes.",
                bouton = "Utiliser".takeIf { conduitRestant > 0 },
            ) {
                utiliserConduit("Perception divine", "détecte Célestes, Fiélons et Morts-vivants à 18 m pendant 10 minutes")
            }
            if (devotion >= 3) {
                val bonus = Paladin.modCha(personnage).coerceAtLeast(1)
                OptionAvecEffet(
                    Paladin.ARME_SACREE,
                    "En prenant l'action Attaquer : pendant 10 minutes, +$bonus au toucher avec l'arme de corps à corps en main " +
                        "principale (appliqué aux jets), lumière vive 6 m, dégâts radiants au choix.",
                    "arme imprégnée d'énergie positive : +$bonus au toucher pendant 10 minutes",
                    Paladin.DUREE_10_MINUTES, "+$bonus au toucher avec l'arme de corps à corps",
                )
            }
            if (anciens >= 3) {
                LignePaladin(
                    "Courroux de la nature",
                    "Action Magie : les créatures choisies à 4,50 m font un JS de Force DD $dd ; échec : Entravées 1 minute " +
                        "(nouveau JS à la fin de chacun de leurs tours).",
                    bouton = "Utiliser".takeIf { conduitRestant > 0 },
                ) {
                    utiliserConduit("Courroux de la nature", "lianes spectrales : créatures choisies à 4,50 m, JS Force DD $dd ou Entravées 1 minute")
                }
            }
            if (gloire >= 3) {
                OptionAvecEffet(
                    Paladin.ATHLETE_D_EXCEPTION,
                    "Action Bonus : pendant 1 heure, Avantage aux tests de Force (Athlétisme) et de Dextérité (Acrobaties), sauts +3 m.",
                    "Avantage en Athlétisme et Acrobaties, sauts +3 m pendant 1 heure",
                    3600L, "Avantage Athlétisme / Acrobaties, sauts +3 m",
                )
                val pvTemp = "2d8 + $niveau"
                LignePaladin(
                    "Châtiment d'exaltation",
                    "Juste après avoir lancé Châtiment divin : $pvTemp PV temporaires à répartir entre des créatures à 9 m (vous compris).",
                    bouton = "Lancer $pvTemp".takeIf { conduitRestant > 0 },
                ) {
                    val des = List(2) { (1..8).random() }
                    val total = des.sum() + niveau
                    utiliserConduit("Châtiment d'exaltation", "$total PV temporaires (${des.joinToString("+")} + $niveau) à répartir entre des créatures à 9 m")
                }
            }
            if (vengeance >= 3) {
                OptionAvecEffet(
                    Paladin.VOEU_D_HOSTILITE,
                    "En prenant l'action Attaquer : Avantage à vos jets d'attaque contre une créature à 9 m pendant 1 minute " +
                        "(transférable si elle tombe à 0 PV). Choisissez « Avantage » en lançant vos attaques contre elle.",
                    "vœu contre une créature à 9 m : Avantage aux attaques contre elle pendant 1 minute",
                    60L, "Avantage aux attaques contre la créature visée",
                )
            }
            if (niveau >= 9) {
                val n = Paladin.ciblesAbjuration(personnage)
                LignePaladin(
                    "Abjuration d'ennemis",
                    "Action Magie : jusqu'à $n créature(s) à 18 m font un JS de Sagesse DD $dd ; échec : Effrayées 1 minute " +
                        "(vitesse 0, pas de bonus de vitesse), et Désavantage contre les Fiélons/Morts-vivants.",
                    bouton = "Utiliser".takeIf { conduitRestant > 0 },
                ) {
                    utiliserConduit("Abjuration d'ennemis", "jusqu'à $n créature(s) à 18 m : JS Sagesse DD $dd, échec = Effrayé 1 minute")
                }
            }

            // ── Frappes radiantes ──
            if (Paladin.aFrappesRadiantes(personnage)) {
                LignePaladin(
                    "Frappes radiantes",
                    "Après une attaque au corps à corps ou à mains nues qui touche : +1d8 dégâts radiants.",
                    bouton = "Lancer 1d8",
                ) {
                    val d8 = (1..8).random()
                    message = onDegats("Dégâts (Frappes radiantes)", listOf(d8), "1d8 radiants (Frappes radiantes)") ?: ECHEC_ENVOI
                }
            }

            // ── Serment de Gloire : Défense glorieuse ──
            Paladin.defenseGlorieuseMax(personnage).takeIf { it > 0 }?.let { max ->
                val reste = Paladin.defenseGlorieuseRestantes(personnage)
                val bonus = Paladin.modCha(personnage).coerceAtLeast(1)
                LignePaladin(
                    "Défense glorieuse : $reste/$max",
                    "Réaction, quand vous ou une créature à 3 m êtes touchés : +$bonus à la CA contre cette attaque ; si elle échoue, " +
                        "vous pouvez attaquer l'assaillant avec une arme. Récupérée au repos long.",
                    bouton = "Utiliser".takeIf { reste > 0 },
                ) {
                    val resultat = onAnnonce("Défense glorieuse", "+$bonus à la CA de la cible contre l'attaque qui vient de la toucher")
                    if (resultat != null) GameState.utiliserDefenseGlorieuse(personnage.id)
                    message = resultat ?: ECHEC_ENVOI
                }
            }

            // ── Aptitude de niveau 20 du serment ──
            Paladin.aptitude20(personnage)?.let { (nom, duree) ->
                val actif = Paladin.effetActif(personnage, nom)
                val disponible = Paladin.aptitude20Disponible(personnage)
                val (detail, annonce) = when (nom) {
                    Paladin.NIMBE_SACRE -> {
                        val degats = Paladin.degatsNimbe(personnage)
                        "Action Bonus, 10 minutes : lumière du soleil dans l'aura, $degats dégâts radiants à chaque ennemi qui commence " +
                            "son tour dans l'aura (appliqués en combat), Avantage aux JS imposés par les Fiélons et Morts-vivants." to
                            "aura de lumière : $degats dégâts radiants aux ennemis qui commencent leur tour dans l'aura, pendant 10 minutes"
                    }
                    Paladin.CHAMPION_DES_ANCIENS ->
                        "Action Bonus, 1 minute : ennemis de l'aura avec Désavantage aux JS contre vos sorts et Conduit divin, vous " +
                            "regagnez ${Paladin.REGENERATION_CHAMPION} PV au début de chaque tour (appliqué en combat), sorts d'une action lancés en action Bonus." to
                            "puissance primordiale pendant 1 minute : Désavantage aux JS des ennemis de l'aura, régénération, sorts rapides"
                    Paladin.LEGENDE_VIVANTE ->
                        "Action Bonus, 10 minutes : Avantage aux tests de Charisme ; une fois par tour, une attaque à l'arme ratée " +
                            "peut toucher ; Réaction pour relancer un JS raté." to
                            "Légende vivante pendant 10 minutes : Charismatique, Frappe infaillible, relance des JS"
                    else ->
                        "Action Bonus, 10 minutes : Vitesse de vol 18 m (stationnaire) ; un ennemi qui commence son tour dans l'aura " +
                            "fait un JS de Sagesse DD $dd ou est Effrayé (appliqué en combat), et les attaques contre lui ont l'Avantage." to
                            "ailes spectrales (vol 18 m) et aura effrayante (JS Sagesse DD $dd) pendant 10 minutes"
                }
                val peutRecuperer = !disponible && !actif && (emplacementsRestants[5] ?: 0) > 0
                LignePaladin(
                    "$nom : " + when {
                        actif -> "actif"
                        disponible -> "disponible"
                        else -> "utilisé"
                    },
                    "$detail 1/repos long, ou récupérable avec un emplacement du 5e niveau.",
                    bouton = when {
                        actif -> "Terminer"
                        disponible -> "Activer"
                        peutRecuperer -> "Récupérer (empl. niv. 5)"
                        else -> null
                    },
                ) {
                    when {
                        actif -> terminerEffet(nom)
                        disponible -> {
                            val resultat = onAnnonce(nom, annonce)
                            if (resultat != null) {
                                GameState.setAptitude20Utilisee(personnage.id, true)
                                ajouterEffet(nom, duree, annonce)
                            }
                            message = resultat ?: ECHEC_ENVOI
                        }
                        else -> {
                            GameState.setAptitude20Utilisee(personnage.id, false, parEmplacement = true)
                            message = "$nom de nouveau disponible (emplacement du 5e niveau dépensé)."
                        }
                    }
                }
            }

            // ── Effets permanents ──
            val permanents = buildList {
                if (niveau >= 5) {
                    val gratuit = personnage.sortsArcanumUtilises.none { ArsenalPersonnage.memeSort(it, Paladin.SORT_FIDELE_DESTRIER) }
                    add("Fidèle destrier : ${Paladin.SORT_FIDELE_DESTRIER} toujours préparé, lancement sans emplacement " + if (gratuit) "disponible." else "utilisé (repos long).")
                }
                Paladin.bonusAuraProtection(personnage).takeIf { it > 0 }?.let { b ->
                    add("Aura de protection (${Paladin.rayonAura(personnage)} m) : +$b aux JS, pour vous et vos alliés dans l'aura (appliqué en combat). Inactive si vous êtes Neutralisé.")
                }
                if (niveau >= 10) add("Aura de courage : vous et vos alliés dans l'aura êtes immunisés contre Effrayé (appliqué en combat).")
                if (devotion >= 7) add("Aura de dévotion : vous et vos alliés dans l'aura êtes immunisés contre Charmé (appliqué en combat).")
                if (devotion >= 15) add("Châtiment de protection : en lançant Châtiment divin, vous et vos alliés dans l'aura bénéficiez d'un abri partiel (+2 CA et JS Dex) jusqu'au début de votre prochain tour.")
                if (anciens >= 7) add("Aura de garde : vous et vos alliés dans l'aura avez la Résistance aux dégâts nécrotiques, psychiques et radiants (appliqué en combat).")
                if (anciens >= 15) add(
                    "Sentinelle impérissable : " + (if (personnage.sentinelleUtilisee) "utilisée (repos long)" else "prête") +
                        " — réduit à 0 PV, vous restez à 1 PV et regagnez ${Paladin.pvSentinelle(personnage)} PV (appliqué en combat). Vous ne vieillissez plus."
                )
                if (gloire >= 7) add("Aura d'entrain : +3 m de Vitesse (appliqué en combat) ; un allié qui entre ou commence son tour dans l'aura gagne +3 m jusqu'à la fin de son tour suivant.")
                if (vengeance >= 7) add("Vengeur implacable : quand vous touchez par une attaque d'opportunité, la Vitesse de la cible tombe à 0 et vous pouvez vous déplacer de la moitié de votre Vitesse (même Réaction).")
                if (vengeance >= 15) add("Esprit de la vengeance : quand la cible de votre Vœu d'hostilité attaque, Réaction pour lui porter une attaque au corps à corps.")
            }
            if (permanents.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                permanents.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
        }
    }
}

@Composable
private fun LignePaladin(titre: String, detail: String, bouton: String?, onClick: () -> Unit) {
    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titre, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
        if (bouton != null) TextButton(onClick = onClick) { Text(bouton) }
    }
}
