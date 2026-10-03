package com.jc2.jdrcompagnon.feature_boutique.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import com.jc2.jdrcompagnon.network.BoutiqueJoueurData
import com.jc2.jdrcompagnon.network.ServiceJoueurData
import com.jc2.jdrcompagnon.ui.Character

private val TexteSecondaire = Color.White.copy(alpha = 0.8f)

/**
 * Services proposés par les boutiques du lieu où se trouve le groupe (menu latéral joueur).
 * « Utiliser » paie le service avec l'or de [personnage] puis appelle [onUtiliser] (décompte de
 * la place côté MJ). Sans personnage sélectionné, la liste reste consultable.
 */
@Composable
fun ServicesLieuDialog(
    lieu: String,
    boutiques: List<BoutiqueJoueurData>,
    personnage: Character?,
    onUtiliser: (BoutiqueJoueurData, ServiceJoueurData) -> Unit,
    onDismiss: () -> Unit,
) {
    var aConfirmer by remember { mutableStateOf<Pair<BoutiqueJoueurData, ServiceJoueurData>?>(null) }
    var dernierUtilise by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Services — $lieu", fontWeight = FontWeight.Bold)
                personnage?.let {
                    Text("${it.name} · ${it.gold} po", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dernierUtilise?.let {
                    Text("✔ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (boutiques.all { it.services.isEmpty() }) {
                    Text("Aucun service proposé ici.", color = TexteSecondaire)
                }
                boutiques.filter { it.services.isNotEmpty() }.forEachIndexed { index, boutique ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(boutique.nom, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    Text("${boutique.type} · ${boutique.marchand}", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                    boutique.services.forEach { service ->
                        val epuise = service.quantiteDisponible == 0
                        val assezDOr = personnage != null && personnage.gold >= service.prixEnPo
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${service.nom} — ${service.prixEnPo} po", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                val detail = listOfNotNull(
                                    service.description.ifBlank { null },
                                    service.quantiteDisponible?.let { if (it == 0) "complet" else "$it places" },
                                ).joinToString(" · ")
                                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                            }
                            TextButton(
                                onClick = { aConfirmer = boutique to service },
                                enabled = !epuise && assezDOr,
                            ) { Text("Utiliser") }
                        }
                    }
                }
                if (personnage == null) {
                    Text("Choisissez un personnage pour utiliser un service.", style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
    )

    aConfirmer?.let { (boutique, service) ->
        AlertDialog(
            onDismissRequest = { aConfirmer = null },
            title = { Text("Utiliser ce service ?") },
            text = {
                Text("${service.nom} chez ${boutique.nom} pour ${service.prixEnPo} po, payés par ${personnage?.name.orEmpty()}.")
            },
            confirmButton = {
                TextButton(onClick = {
                    aConfirmer = null
                    onUtiliser(boutique, service)
                    dernierUtilise = "${service.nom} (${boutique.nom}) : ${service.prixEnPo} po payés"
                }) { Text("Payer") }
            },
            dismissButton = { TextButton(onClick = { aConfirmer = null }) { Text("Annuler") } },
        )
    }
}
