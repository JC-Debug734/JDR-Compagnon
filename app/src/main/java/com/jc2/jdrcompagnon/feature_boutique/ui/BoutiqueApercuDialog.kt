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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique

private val TexteSecondaire = Color.White.copy(alpha = 0.8f)

/**
 * Aperçu d'une boutique depuis la lecture d'une scène : marchand, services proposés (actifs)
 * et articles en vente, avec leurs tarifs. Consultation seule — la gestion se fait dans
 * l'écran Boutique.
 */
@Composable
fun BoutiqueApercuDialog(boutique: Boutique, onDismiss: () -> Unit) {
    val services = boutique.services.filter { it.actif }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(boutique.nom, fontWeight = FontWeight.Bold)
                Text(
                    "${boutique.type.label} · ${boutique.standing.label} · ${boutique.marchand.nom}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TexteSecondaire
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (boutique.marchand.trait.isNotBlank()) {
                    Text(boutique.marchand.trait, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
                }
                Text("Services", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                if (services.isEmpty()) Text("Aucun service proposé", color = TexteSecondaire)
                services.forEach { service ->
                    LigneTarif(
                        nom = service.nom,
                        detail = listOfNotNull(
                            service.description.ifBlank { null },
                            service.quantiteDisponible?.let { "$it places" }
                        ).joinToString(" · "),
                        prix = service.prixEnPo
                    )
                }
                if (boutique.inventaire.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Articles en vente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    boutique.inventaire.forEach { article ->
                        LigneTarif(nom = article.equipement.nom, detail = "x${article.quantiteStock}", prix = article.prixApplique)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun LigneTarif(nom: String, detail: String, prix: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(nom, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = TexteSecondaire)
        }
        Text("$prix po", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
