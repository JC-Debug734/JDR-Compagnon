package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import com.jc2.jdrcompagnon.feature_combat.domain.model.RessourcesClasse
import com.jc2.jdrcompagnon.feature_combat.domain.model.Roublard
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Aptitudes du Roublard : Attaque sournoise à lancer, avec les effets de Frappe malicieuse / Coups
 * vicieux (dés sacrifiés), Frappe éclair de l'Assassin et Rupture spirituelle de l'Âme acérée ;
 * rappels (Ruse, Visée appliquée, Esquive instinctive, Savoir-faire) ; ressources des sous-classes
 * (Énergie psionique, Voile psychique, Voleur de sorts) et Coup de chance. [onAnnonce] et
 * [onDegats] appliquent l'effet — directement hors réseau, ou par envoi au MJ en combat — et
 * renvoient le message à afficher, ou null si l'envoi a échoué (rien n'est alors dépensé).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CarteAptitudesRoublard(
    personnage: Character,
    onAnnonce: (libelle: String, texte: String) -> String?,
    onDegats: (libelle: String, des: List<Int>, formule: String) -> String?,
    modifier: Modifier = Modifier,
) {
    val niveau = Roublard.niveau(personnage)
    if (niveau == 0) return
    var message by remember { mutableStateOf<String?>(null) }
    val ressources = Roublard.ressources(personnage)
    val energie = ressources.firstOrNull { it.cle == Roublard.ENERGIE_PSIONIQUE }
    fun d(faces: String) = (1..(faces.removePrefix("d").toIntOrNull() ?: 6)).random()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aptitudes du Roublard", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)

            // ── Attaque sournoise ──
            val nbDes = Roublard.desAttaqueSournoise(personnage)
            val options = Roublard.optionsFrappeMalicieuse(personnage)
            var choisis by remember { mutableStateOf(setOf<String>()) }
            var premierRound by remember { mutableStateOf(false) }
            var rupture by remember { mutableStateOf(false) }
            val cout = options.filter { it.nom in choisis }.sumOf { it.cout }
            val desLances = (nbDes - cout).coerceAtLeast(0)
            val assassin = Roublard.niveauAssassin(personnage)
            val ruptureDispo = ressources.firstOrNull { it.cle == Roublard.RUPTURE_SPIRITUELLE }?.let { RessourcesClasse.restantes(personnage, it) > 0 } == true
            Text("Attaque sournoise : ${nbDes}d6", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                "Une fois par tour, après avoir touché avec une arme Finesse ou à distance, avec l'Avantage ou un allié au contact de la cible (sans Désavantage).",
                style = MaterialTheme.typography.bodySmall,
            )
            if (options.isNotEmpty()) {
                Text("Frappe malicieuse (${Roublard.effetsMax(personnage)} effet(s), DD ${Roublard.dd(personnage)}) :", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { o ->
                        val selectionne = o.nom in choisis
                        val possible = selectionne || (choisis.size < Roublard.effetsMax(personnage) && cout + o.cout <= nbDes)
                        FilterChip(
                            selected = selectionne, enabled = possible,
                            onClick = { choisis = if (selectionne) choisis - o.nom else choisis + o.nom },
                            label = { Text("${o.nom} (−${o.cout}d6)") },
                        )
                    }
                }
                options.filter { it.nom in choisis }.forEach { Text("• ${it.nom} : ${it.effet}", style = MaterialTheme.typography.bodySmall) }
            }
            if (assassin >= 3) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = premierRound, onCheckedChange = { premierRound = it })
                Text(
                    "Premier round (Frappe éclair : +$niveau dégâts" + (if (assassin >= 17) ", Coup meurtrier : JS Con DD ${Roublard.dd(personnage)} ou dégâts doublés" else "") + ")",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (ruptureDispo) Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = rupture, onCheckedChange = { rupture = it })
                Text("Rupture spirituelle (Lame psychique) : JS Sagesse DD ${Roublard.dd(personnage)} ou Étourdie 1 minute", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = {
                val des = List(desLances) { d("d6") }
                val bonus = if (premierRound && assassin >= 3) niveau else 0
                val effets = options.filter { it.nom in choisis }.map { "${it.nom} : ${it.effet}" } +
                    (if (rupture) listOf("Rupture spirituelle : JS Sagesse DD ${Roublard.dd(personnage)} ou Étourdie 1 minute") else emptyList()) +
                    (if (premierRound && assassin >= 17) listOf("Coup meurtrier : JS Constitution DD ${Roublard.dd(personnage)} ou dégâts de l'attaque doublés") else emptyList())
                val formule = "${desLances}d6" + (if (bonus > 0) " + $bonus (Frappe éclair)" else "") + " (Attaque sournoise)" +
                    if (effets.isNotEmpty()) " — " + effets.joinToString(" ; ") else ""
                val resultat = onDegats("Dégâts (Attaque sournoise)", des + listOfNotNull(bonus.takeIf { it > 0 }), formule)
                if (resultat != null && rupture) {
                    ressources.firstOrNull { it.cle == Roublard.RUPTURE_SPIRITUELLE }?.let { GameState.utiliserCompteur(personnage.id, it.cle, it.max) }
                    rupture = false
                }
                message = resultat ?: ECHEC_ENVOI
            }, modifier = Modifier.fillMaxWidth()) { Text("Lancer l'Attaque sournoise (${desLances}d6" + (if (premierRound && assassin >= 3) " + $niveau" else "") + ")") }

            // ── Rappels ──
            val rappels = buildList {
                if (niveau >= 2) add("Ruse : action Bonus, Désengagement, Furtivité ou Pointe.")
                if (niveau >= 3) add("Visée appliquée : action Bonus sans s'être déplacé, Avantage à la prochaine attaque ce tour, Vitesse 0" +
                    if (assassin >= 9) " (Visée mobile : Vitesse conservée)." else ".")
                if (niveau >= 5) add("Esquive instinctive : Réaction, dégâts d'une attaque qui vous touche divisés par deux.")
                if (niveau >= 7) add("Esquive totale : appliquée en combat aux JS de Dextérité à demi-dégâts. Savoir-faire : un d20 de 9 ou moins compte comme 10 aux tests de compétence maîtrisée.")
                if (niveau >= 15) add("Esprit fuyant : maîtrise des JS de Sagesse et de Charisme (appliquée).")
                if (niveau >= 18) add("Insaisissable : pas d'Avantage contre vous (appliqué en combat), sauf si vous êtes Neutralisé.")
                if (assassin >= 3) add("Assassinat : Avantage à l'Initiative.")
                if (Roublard.niveauVoleur(personnage) >= 3) add("Mains lestes : action Bonus, Escamotage (crocheter, désamorcer, faire les poches) ou utiliser un objet magique. Monte-en-l'air : Vitesse d'escalade, sauts avec la Dextérité.")
                if (Roublard.niveauVoleur(personnage) >= 13) add("Utilisation d'objets magiques : 4 objets harmonisés (appliqué), 1d6 = 6 → aucune charge dépensée, tout parchemin utilisable.")
                if (Roublard.niveauVoleur(personnage) >= 17) add("Réflexes de voleur : deux tours au premier round (le second à votre Initiative − 10).")
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            rappels.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }

            // ── Ressources ──
            ressources.forEach { r ->
                val reste = RessourcesClasse.restantes(personnage, r)
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Text("${r.nom} : $reste/${r.max}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${r.detail} Récupération : ${r.recuperation.libelle}.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (r.cle) {
                        Roublard.ENERGIE_PSIONIQUE -> {
                            val de = r.de ?: "d6"
                            var dernier by remember { mutableStateOf<Int?>(null) }
                            TextButton(enabled = reste > 0, onClick = {
                                val v = d(de)
                                dernier = v
                                message = "Dé d'Énergie psionique : +$v au test ou à l'attaque raté(e). Dépensez-le seulement s'il réussit."
                            }) { Text("Lancer 1$de (Savoir-faire / Lames chercheuses)") }
                            if (dernier != null) TextButton(enabled = reste > 0, onClick = {
                                GameState.utiliserCompteur(personnage.id, r.cle, r.max)
                                message = "Dé dépensé (le jet a réussi)."
                                dernier = null
                            }) { Text("Réussi : dépenser le dé") }
                            if (Roublard.niveauAmeAceree(personnage) >= 9) TextButton(enabled = reste > 0, onClick = {
                                val v = d(de)
                                val resultat = onAnnonce("Téléportation psychique", "téléportation jusqu'à ${3 * v} m (1$de = $v)")
                                if (resultat != null) GameState.utiliserCompteur(personnage.id, r.cle, r.max)
                                message = resultat ?: ECHEC_ENVOI
                            }) { Text("Téléportation psychique") }
                        }
                        Roublard.MURMURES_PSYCHIQUES -> {
                            val de = energie?.de ?: "d6"
                            TextButton(onClick = {
                                val v = d(de)
                                val gratuit = reste > 0
                                val payable = gratuit || (energie != null && RessourcesClasse.restantes(personnage, energie) > 0)
                                if (!payable) {
                                    message = "Plus de dé d'Énergie psionique."
                                    return@TextButton
                                }
                                val resultat = onAnnonce("Murmures psychiques", "télépathie pendant $v heure(s) avec jusqu'à ${personnage.proficiencyBonus} créature(s)")
                                if (resultat != null) {
                                    if (gratuit) GameState.utiliserCompteur(personnage.id, r.cle, r.max)
                                    else energie?.let { GameState.utiliserCompteur(personnage.id, it.cle, it.max) }
                                }
                                message = resultat ?: ECHEC_ENVOI
                            }) { Text(if (reste > 0) "Utiliser (gratuit)" else "Utiliser (1 dé)") }
                        }
                        else -> {
                            TextButton(enabled = reste > 0, onClick = {
                                val resultat = onAnnonce(r.nom, "utilisé")
                                if (resultat != null) GameState.utiliserCompteur(personnage.id, r.cle, r.max)
                                message = resultat ?: ECHEC_ENVOI
                            }) { Text("Utiliser") }
                            // Voile psychique : récupérable avec un dé ; Rupture spirituelle : avec trois.
                            val coutDes = when (r.cle) {
                                Roublard.VOILE_PSYCHIQUE -> 1
                                Roublard.RUPTURE_SPIRITUELLE -> 3
                                else -> 0
                            }
                            if (coutDes > 0 && reste == 0 && energie != null && RessourcesClasse.restantes(personnage, energie) >= coutDes) {
                                TextButton(onClick = {
                                    GameState.utiliserCompteur(personnage.id, energie.cle, energie.max, coutDes)
                                    GameState.recupererCompteur(personnage.id, r.cle)
                                    message = "${r.nom} récupéré ($coutDes dé(s) d'Énergie psionique)."
                                }) { Text("Récupérer ($coutDes dé${if (coutDes > 1) "s" else ""})") }
                            }
                        }
                    }
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
        }
    }
}
