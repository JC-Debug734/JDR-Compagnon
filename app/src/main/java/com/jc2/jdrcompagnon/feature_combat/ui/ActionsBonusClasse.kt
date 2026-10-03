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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ArsenalPersonnage
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/** Créature au contact que le Paladin peut toucher (lui-même, un allié, un PNJ ou un ennemi). */
internal data class CibleContact(val id: String, val nom: String)

/** Coût en Puissance curative pour retirer l'état Empoisonné. */
private const val COUT_POISON = 5

private const val ECHEC_ENVOI = "Envoi impossible : connexion au MJ perdue. Rien n'a été dépensé."

/** Le personnage a-t-il des actions bonus de classe gérées ici (Imposition des mains, Châtiment divin) ? */
internal fun aActionsBonusClasse(c: Character): Boolean =
    ArsenalPersonnage.reservePuissanceCurative(c) > 0 || ArsenalPersonnage.aChatimentDePaladin(c)

/**
 * Actions bonus du Paladin :
 * - Imposition des mains : Guérison (PV puisés dans la Puissance curative) ou retrait de l'état
 *   Empoisonné (5 points), sur n'importe quelle créature au contact — [cibles].
 * - Châtiment divin (niveau 2+) : après avoir touché au corps à corps, 2d8 radiants (+1d8 par
 *   niveau d'emplacement au-delà du 1er, +1d8 contre un Fiélon ou un Mort-vivant) ; une fois
 *   gratuit par repos long, sinon avec un emplacement.
 * La ressource est dépensée ici sur la fiche ; [onImposition] / [onChatiment] appliquent l'effet
 * (directement hors réseau, ou par envoi au MJ en combat) et renvoient le message à afficher,
 * ou null si l'envoi a échoué (la ressource n'est alors pas dépensée).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActionsBonusPaladin(
    personnage: Character,
    cibles: List<CibleContact>,
    emplacementsRestants: Map<Int, Int>,
    onImposition: (cible: CibleContact, pv: Int, retirerPoison: Boolean) -> String?,
    onChatiment: (des: List<Int>, formule: String) -> String?,
    modifier: Modifier = Modifier,
) {
    if (!aActionsBonusClasse(personnage)) return
    val reserve = ArsenalPersonnage.reservePuissanceCurative(personnage)
    val restant = (reserve - personnage.layOnHandsUsed).coerceAtLeast(0)
    var message by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Actions bonus", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)

            if (reserve > 0) {
                var retirerPoison by remember { mutableStateOf(false) }
                var cibleId by remember { mutableStateOf(cibles.firstOrNull()?.id) }
                var pv by remember { mutableIntStateOf(5) }
                val pvEffectifs = pv.coerceIn(1, restant.coerceAtLeast(1))
                val cible = cibles.firstOrNull { it.id == cibleId }
                Text("Imposition des mains", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Puissance curative : $restant / $reserve PV (récupérée au repos long)", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = !retirerPoison, onClick = { retirerPoison = false }, label = { Text("Guérison") })
                    FilterChip(selected = retirerPoison, onClick = { retirerPoison = true }, label = { Text("Retirer Empoisonné ($COUT_POISON pts)") })
                }
                Text("Cible au contact (PJ, PNJ, allié ou ennemi)", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cibles.forEach { c ->
                        FilterChip(selected = c.id == cibleId, onClick = { cibleId = c.id }, label = { Text(c.nom) })
                    }
                }
                if (!retirerPoison) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { pv = (pvEffectifs - 1).coerceAtLeast(1) }, enabled = pvEffectifs > 1) { Text("−") }
                        Text("$pvEffectifs PV", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { pv = (pvEffectifs + 1).coerceAtMost(restant) }, enabled = pvEffectifs < restant) { Text("+") }
                        TextButton(onClick = { pv = restant }, enabled = restant > 0) { Text("Tout") }
                    }
                }
                val cout = if (retirerPoison) COUT_POISON else pvEffectifs
                Button(
                    enabled = cible != null && restant >= cout && cout > 0,
                    onClick = {
                        val c = cible ?: return@Button
                        val resultat = onImposition(c, if (retirerPoison) 0 else pvEffectifs, retirerPoison)
                        if (resultat != null) GameState.setLayOnHandsUsed(personnage.id, personnage.layOnHandsUsed + cout, reserve)
                        message = resultat ?: ECHEC_ENVOI
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (retirerPoison) "Retirer Empoisonné" + (cible?.let { " de ${it.nom}" } ?: "")
                        else "Soigner " + (cible?.nom ?: "…") + " de $pvEffectifs PV"
                    )
                }
            }

            if (ArsenalPersonnage.aChatimentDePaladin(personnage)) {
                if (reserve > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                val gratuit = !personnage.divineSmiteFreeUsed
                // null = lancement gratuit (Châtiment de paladin), sinon niveau d'emplacement dépensé.
                var niveau by remember(gratuit) { mutableStateOf<Int?>(if (gratuit) null else emplacementsRestants.keys.filter { (emplacementsRestants[it] ?: 0) > 0 }.minOrNull()) }
                var fielon by remember { mutableStateOf(false) }
                Text("Châtiment divin", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Juste après avoir touché avec une arme de corps à corps ou à mains nues : dégâts radiants en plus.",
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = niveau == null,
                        enabled = gratuit,
                        onClick = { niveau = null },
                        label = { Text(if (gratuit) "Gratuit (1/repos long)" else "Gratuit utilisé") },
                    )
                    emplacementsRestants.keys.sorted().forEach { n ->
                        val reste = emplacementsRestants[n] ?: 0
                        FilterChip(selected = niveau == n, enabled = reste > 0, onClick = { niveau = n }, label = { Text("Niv. $n ($reste)") })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = fielon, onCheckedChange = { fielon = it })
                    Text("Cible Fiélon ou Mort-vivant (+1d8)", style = MaterialTheme.typography.bodySmall)
                }
                val niveauEffectif = niveau ?: 1
                val nbDes = 1 + niveauEffectif + if (fielon) 1 else 0
                val possible = (niveau == null && gratuit) || (niveau != null && (emplacementsRestants[niveau] ?: 0) > 0)
                Button(
                    enabled = possible,
                    onClick = {
                        val des = List(nbDes) { (1..8).random() }
                        val n = niveau
                        val source = if (n == null) "gratuit" else "emplacement niv. $n"
                        val resultat = onChatiment(des, "${nbDes}d8 radiants (Châtiment divin, $source)")
                        if (resultat != null) {
                            if (n == null) GameState.setDivineSmiteFreeUsed(personnage.id, true)
                            else GameState.setSpellSlotUsed(personnage.id, n, (personnage.spellSlotsUsed[n] ?: 0) + 1)
                        }
                        message = resultat ?: ECHEC_ENVOI
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Lancer Châtiment divin (${nbDes}d8)") }
            }

            message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = ForcedDarkPalette.AccentGold) }
        }
    }
}
