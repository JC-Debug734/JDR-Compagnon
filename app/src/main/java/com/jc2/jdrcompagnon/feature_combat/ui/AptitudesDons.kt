package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.jc2.jdrcompagnon.feature_combat.domain.model.Dons
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette

/**
 * Ressources à utilisations limitées des dons (Points de chance, Regain de vitalité, Esprit
 * préservé, Rituel rapide, sorts gratuits des Affinités, Télépathe, Initié à la magie…), avec
 * leurs compteurs. [onAnnonce] (libellé, texte) et [onSoin] (PV, détail) appliquent l'effet —
 * directement hors réseau, ou par envoi au MJ en combat — et renvoient le message à afficher,
 * ou null si l'envoi a échoué (rien n'est alors dépensé).
 */
@Composable
internal fun CarteRessourcesDons(
    personnage: Character,
    onAnnonce: (libelle: String, texte: String) -> String?,
    onSoin: (pv: Int, detail: String) -> String?,
    modifier: Modifier = Modifier,
) {
    val ressources = Dons.ressources(personnage)
    if (ressources.isEmpty()) return
    var message by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ressources des dons", fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
            ressources.forEach { r ->
                val restantes = Dons.restantes(personnage, r)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${r.nom} : $restantes/${r.max}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        // Baroud d'honneur se déclenche seul en combat, à 0 PV.
                        if (r.cle != Dons.BAROUD) {
                            TextButton(enabled = restantes > 0, onClick = {
                                val resultat = if (r.cle == Dons.REGAIN_VITALITE) {
                                    val pv = (1..10).random()
                                    onSoin(pv, "Regain de vitalité (1d10 = $pv)")
                                } else onAnnonce(r.nom, r.detail)
                                if (resultat != null) GameState.utiliserCompteur(personnage.id, r.cle, r.max)
                                message = resultat ?: ECHEC_ENVOI
                            }) { Text(if (r.cle == Dons.REGAIN_VITALITE) "Dépenser 1d10" else "Utiliser") }
                        }
                    }
                    Text(r.detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    Text("Récupération : ${r.recuperation.libelle}", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                }
            }
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
