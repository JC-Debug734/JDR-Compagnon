package com.jc2.jdrcompagnon.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID

/** Image fournie avec l'app (res/drawable-nodpi, fond transparent), proposée en galerie. */
data class VignettePredefinie(val cle: String, val libelle: String, @DrawableRes val drawable: Int)

/**
 * Image de vignette d'un élément (enseigne de boutique, emblème de ville) : soit une image
 * fournie avec l'app, codée "<prefixe><clé>", soit une image choisie sur l'appareil, copiée dans
 * le stockage interne ([nomDossier]) — l'URI "content://" d'origine ne survit pas forcément au
 * redémarrage de l'app.
 */
open class CatalogueVignettes(
    private val prefixe: String,
    private val nomDossier: String,
    val predefinies: List<VignettePredefinie>,
) {
    fun valeur(cle: String): String = prefixe + cle

    /** Valeur à stocker pour la clé [cle] si elle désigne une image fournie, sinon null. */
    fun valeurSiConnue(cle: String?): String? =
        cle?.takeIf { c -> predefinies.any { it.cle == c } }?.let(::valeur)

    /** Image fournie désignée par [image], ou null si c'est une image de l'appareil (ou une clé inconnue). */
    fun predefinie(image: String): VignettePredefinie? =
        image.takeIf { it.startsWith(prefixe) }
            ?.removePrefix(prefixe)
            ?.let { cle -> predefinies.firstOrNull { it.cle == cle } }

    private fun dossier(context: Context): File =
        File(context.filesDir, nomDossier).apply { mkdirs() }

    /** Fichier d'une image de l'appareil, null pour une image fournie. */
    fun fichier(context: Context, image: String): File? =
        if (image.startsWith(prefixe)) null else File(dossier(context), image)

    /** Copie l'image choisie sur l'appareil ; retourne la valeur à stocker, ou null en cas d'échec. */
    fun copier(context: Context, uri: Uri): String? {
        val fileName = "${UUID.randomUUID()}.img"
        val target = File(dossier(context), fileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            fileName
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    /** Duplique une image de l'appareil (copie d'un élément) ; une image fournie est simplement reprise. */
    fun dupliquer(context: Context, image: String?): String? {
        val source = image?.let { fichier(context, it) } ?: return image
        if (!source.exists()) return null
        val fileName = "${UUID.randomUUID()}.img"
        return runCatching { source.copyTo(File(dossier(context), fileName)); fileName }.getOrNull()
    }

    /** Supprime le fichier d'une image de l'appareil devenue inutile (sans effet pour une image fournie). */
    fun supprimer(context: Context, image: String?) {
        image?.let { fichier(context, it) }?.delete()
    }
}

/**
 * Affiche une vignette du [catalogue]. N'affiche rien si l'image est introuvable (fichier
 * supprimé, image reçue d'un autre appareil sans son fichier...).
 */
@Composable
fun VignetteImage(
    catalogue: CatalogueVignettes,
    image: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val predefinie = remember(catalogue, image) { catalogue.predefinie(image) }
    if (predefinie != null) {
        Image(
            painter = painterResource(predefinie.drawable),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
        return
    }
    val context = LocalContext.current
    val bitmap = remember(catalogue, image) {
        runCatching {
            val file = catalogue.fichier(context, image)?.takeIf { it.exists() } ?: return@runCatching null
            // Sous-échantillonnage : l'image n'est montrée qu'en vignette.
            val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bornes)
            var echantillon = 1
            while (maxOf(bornes.outWidth, bornes.outHeight) / (echantillon * 2) >= 256) echantillon *= 2
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = echantillon })
                ?.asImageBitmap()
        }.getOrNull()
    } ?: return
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Crop
    )
}

/** Galerie des images fournies du [catalogue] ; [onChoisir] reçoit la valeur à stocker. */
@Composable
fun GalerieVignettesDialog(
    catalogue: CatalogueVignettes,
    titre: String,
    onChoisir: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titre) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                modifier = Modifier.heightIn(max = 480.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(catalogue.predefinies, key = { it.cle }) { vignette ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onChoisir(catalogue.valeur(vignette.cle)) }
                            .padding(4.dp)
                    ) {
                        Image(
                            painter = painterResource(vignette.drawable),
                            contentDescription = vignette.libelle,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        )
                        Text(vignette.libelle, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

/**
 * Choix de la vignette d'un élément enregistré au fil de l'eau (écran d'édition) : aperçu, galerie
 * des images fournies, image de l'appareil, retrait. [onChanger] reçoit la nouvelle valeur ;
 * l'ancienne image de l'appareil, devenue inutile, est alors supprimée.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VignetteSelecteur(
    catalogue: CatalogueVignettes,
    image: String?,
    titreGalerie: String,
    onChanger: (String?) -> Unit,
) {
    val context = LocalContext.current
    var galerieOuverte by remember { mutableStateOf(false) }
    fun changer(nouvelle: String?) {
        val ancienne = image
        onChanger(nouvelle)
        if (ancienne != nouvelle) catalogue.supprimer(context, ancienne)
    }
    val choisirImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) catalogue.copier(context, uri)?.let { changer(it) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        image?.let {
            VignetteImage(catalogue, it, Modifier.size(88.dp), contentDescription = titreGalerie)
            Spacer(Modifier.width(12.dp))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { galerieOuverte = true }) { Text("Galerie") }
            OutlinedButton(onClick = { choisirImage.launch("image/*") }) { Text("Appareil") }
            if (image != null) OutlinedButton(onClick = { changer(null) }) { Text("Retirer") }
        }
    }
    if (galerieOuverte) {
        GalerieVignettesDialog(
            catalogue = catalogue,
            titre = titreGalerie,
            onChoisir = { galerieOuverte = false; changer(it) },
            onDismiss = { galerieOuverte = false }
        )
    }
}
