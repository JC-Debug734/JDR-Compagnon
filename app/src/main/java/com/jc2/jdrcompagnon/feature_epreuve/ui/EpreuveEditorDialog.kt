package com.jc2.jdrcompagnon.feature_epreuve.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_environnement.ui.GalerieImagesDialog
import com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveImageStore
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ReglesEpreuve

/**
 * Création ([initiale] null) ou modification d'une épreuve. Les images choisies sont copiées
 * tout de suite (pour l'aperçu) ; à l'enregistrement on supprime celles qui ne servent plus,
 * à l'annulation toutes celles copiées pendant l'édition.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EpreuveEditorDialog(
    initiale: Epreuve?,
    worldId: String,
    onSave: (Epreuve) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val base = remember { initiale ?: Epreuve(worldId = worldId, nom = "") }
    var nom by remember { mutableStateOf(base.nom) }
    var description by remember { mutableStateOf(base.description) }
    var reussites by remember { mutableIntStateOf(base.reussitesRequises) }
    var image by remember { mutableStateOf(base.imageFileName) }
    val copiesEdition = remember { mutableStateListOf<String>() }
    val complications = remember { mutableStateListOf<ComplicationEpreuve>().apply { addAll(base.complications) } }
    var galerieOuverte by remember { mutableStateOf(false) }
    var erreurNom by remember { mutableStateOf(false) }

    fun nouvelleImage(fichier: String?) {
        if (fichier == null) return
        copiesEdition += fichier
        image = fichier
    }

    val choisirImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) nouvelleImage(EpreuveImageStore.copier(context, uri, base.id))
    }

    fun annuler() {
        copiesEdition.forEach { EpreuveImageStore.supprimer(context, it) }
        onDismiss()
    }

    if (galerieOuverte) {
        GalerieImagesDialog(
            onChoisir = { assetName ->
                galerieOuverte = false
                nouvelleImage(EpreuveImageStore.copierDepuisAsset(context, assetName, base.id))
            },
            onDismiss = { galerieOuverte = false },
        )
    }

    AlertDialog(
        onDismissRequest = ::annuler,
        title = { Text(if (initiale == null) "Nouvelle épreuve" else "Modifier l'épreuve") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it; erreurNom = false },
                    label = { Text("Nom (ex : Traversée du marais)") },
                    isError = erreurNom,
                    supportingText = if (erreurNom) ({ Text("Le nom est obligatoire") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description lue aux joueurs") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Réussites nécessaires", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { reussites = (reussites - 1).coerceAtLeast(ReglesEpreuve.REUSSITES_MIN) }) {
                        Icon(Icons.Default.Remove, contentDescription = "Moins")
                    }
                    Text("$reussites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { reussites = (reussites + 1).coerceAtMost(ReglesEpreuve.REUSSITES_MAX) }) {
                        Icon(Icons.Default.Add, contentDescription = "Plus")
                    }
                }

                Text("Image affichée sur la table", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                val apercu = rememberImageEpreuve(image, echantillonnage = 2)
                if (apercu != null) {
                    Image(
                        bitmap = apercu,
                        contentDescription = "Image de l'épreuve",
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { galerieOuverte = true }) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galerie")
                    }
                    OutlinedButton(onClick = { choisirImage.launch("image/*") }) {
                        Text(if (image == null) "Depuis l'appareil" else "Changer")
                    }
                    if (image != null) {
                        OutlinedButton(onClick = { image = null }) { Text("Retirer") }
                    }
                }

                Text("Complications en cas d'échec", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Chaque échec en tire une au hasard. Elles doivent toujours desservir le groupe.",
                    style = MaterialTheme.typography.bodySmall,
                )
                complications.forEachIndexed { index, complication ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = complication.titre,
                                    onValueChange = { complications[index] = complication.copy(titre = it) },
                                    label = { Text("Complication ${index + 1}") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { complications.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer la complication", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            OutlinedTextField(
                                value = complication.description,
                                onValueChange = { complications[index] = complication.copy(description = it) },
                                label = { Text("Effet (dégâts, perte de temps...)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                TextButton(onClick = { complications += ComplicationEpreuve("") }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter une complication")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nom.isBlank()) {
                    erreurNom = true
                    return@TextButton
                }
                copiesEdition.filter { it != image }.forEach { EpreuveImageStore.supprimer(context, it) }
                if (base.imageFileName != null && base.imageFileName != image) EpreuveImageStore.supprimer(context, base.imageFileName)
                onSave(
                    base.copy(
                        nom = nom.trim(),
                        description = description.trim(),
                        reussitesRequises = reussites,
                        imageFileName = image,
                        complications = complications
                            .map { ComplicationEpreuve(it.titre.trim(), it.description.trim()) }
                            .filter { it.titre.isNotEmpty() },
                    )
                )
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = ::annuler) { Text("Annuler") } },
    )
}

/** Image d'une épreuve, sous-échantillonnée ([echantillonnage] = inSampleSize) pour les vignettes. */
@Composable
internal fun rememberImageEpreuve(fileName: String?, echantillonnage: Int = 1): ImageBitmap? {
    val context = LocalContext.current
    return remember(fileName, echantillonnage) {
        fileName ?: return@remember null
        runCatching {
            val file = EpreuveImageStore.fichier(context, fileName)
            if (!file.exists()) return@runCatching null
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = echantillonnage })?.asImageBitmap()
        }.getOrNull()
    }
}
