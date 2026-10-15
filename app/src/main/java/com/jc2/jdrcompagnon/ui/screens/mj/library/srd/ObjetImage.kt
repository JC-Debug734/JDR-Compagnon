package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Illustration d'un objet livrée avec un livre .zip (champ **Image**, chemin absolu une fois
 * résolu par CustomBookImages.resolveObjets) ; n'affiche rien si l'objet n'en a pas.
 */
@Composable
fun ObjetImage(item: EquipmentItem, modifier: Modifier = Modifier, hauteurMax: Dp = 260.dp) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, item.image) {
        value = withContext(Dispatchers.IO) { item.image?.let { charger(it) } }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = hauteurMax)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
}

private const val COTE_MAX_PX = 1024
private val cache = mutableMapOf<String, ImageBitmap>()

/** Décode [chemin] en réduisant les grandes images, mis en cache ; null si introuvable. */
private fun charger(chemin: String): ImageBitmap? {
    val fichier = File(chemin).takeIf { it.isAbsolute && it.isFile } ?: return null
    synchronized(cache) { cache[fichier.absolutePath]?.let { return it } }
    val bornes = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(fichier.absolutePath, bornes)
    var echantillon = 1
    while (maxOf(bornes.outWidth, bornes.outHeight) / echantillon > COTE_MAX_PX) echantillon *= 2
    val bitmap = BitmapFactory.decodeFile(fichier.absolutePath, BitmapFactory.Options().apply { inSampleSize = echantillon })
        ?.asImageBitmap() ?: return null
    synchronized(cache) { cache[fichier.absolutePath] = bitmap }
    return bitmap
}
