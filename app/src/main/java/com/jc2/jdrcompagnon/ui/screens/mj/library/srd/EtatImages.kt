package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Illustrations des états (cartes « Étourdi », « À terre »...), livrées dans le dossier `Etats/`
 * (ou `États/`) d'une archive de campagne : chaque image est nommée d'après son état
 * (`etourdi.jpg`, `a-terre.png`, `neutralise.webp`...), accents et accords facultatifs. Copiées
 * dans le stockage de l'app, elles s'affichent sur la fiche de l'état dans la bibliothèque, pour
 * tous les univers ; une nouvelle image remplace la précédente.
 */
object EtatImages {

    private const val DOSSIER = "etats_images"
    private val extensions = setOf("png", "jpg", "jpeg", "webp")
    private val nomsDossier = setOf("etats", "états", "conditions")

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private fun dossier(context: Context) = File(context.filesDir, DOSSIER).apply { mkdirs() }

    /** Clé de fichier d'un état ([Etats.cle] de son libellé, sans espaces), null s'il n'est pas reconnu. */
    internal fun cleEtat(nom: String): String? {
        val libelle = nom.replace('-', ' ').replace('_', ' ')
        val etat = Etats.reconnaitre(libelle)?.label
            ?: Etats.NOM_EPUISEMENT.takeIf { Etats.cle(libelle) == Etats.cle(it) }
            ?: return null
        return Etats.cle(etat).replace(' ', '-').replace('\'', '-')
    }

    /**
     * Copie les images des dossiers d'états trouvés sous [racine] (archive extraite). Retourne le
     * nombre d'images importées ; un fichier qui ne correspond à aucun état est ignoré.
     */
    fun importerDepuis(context: Context, racine: File): Int {
        val sources = racine.walkTopDown()
            .filter { it.isDirectory && it.name.lowercase() in nomsDossier }
            .flatMap { d -> d.listFiles().orEmpty().asSequence() }
            .filter { it.isFile && it.extension.lowercase() in extensions }
            .toList()
        var n = 0
        sources.forEach { f ->
            val cle = cleEtat(f.nameWithoutExtension) ?: return@forEach
            val cible = dossier(context)
            cible.listFiles().orEmpty().filter { it.nameWithoutExtension == cle }.forEach { it.delete() }
            if (runCatching { f.copyTo(File(cible, "$cle.${f.extension.lowercase()}"), overwrite = true) }.isSuccess) n++
        }
        if (n > 0) _version.value++
        return n
    }

    /** Fichiers d'images d'états présents (nom de fichier), pour l'historique des imports. */
    fun fichiersPresents(context: Context): List<String> = dossier(context).listFiles().orEmpty().filter { it.isFile }.map { it.name }

    /** Supprime l'image [nomFichier] (suppression d'un import) ; true si elle existait. */
    fun supprimerFichier(context: Context, nomFichier: String): Boolean {
        val f = File(dossier(context), nomFichier)
        val ok = f.isFile && f.delete()
        if (ok) _version.value++
        return ok
    }

    /** Image importée pour l'état [nom], ou null. */
    fun fichier(context: Context, nom: String): File? {
        val cle = cleEtat(nom) ?: return null
        return dossier(context).listFiles().orEmpty().firstOrNull { it.nameWithoutExtension == cle }
    }
}

/** Illustration de l'état [nom] si une image a été importée (rien sinon). */
@Composable
fun EtatImage(nom: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version by EtatImages.version.collectAsState()
    val bitmap by produceState<ImageBitmap?>(initialValue = null, nom, version) {
        value = withContext(Dispatchers.IO) {
            EtatImages.fichier(context, nom)?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = nom,
            contentScale = ContentScale.Fit,
            modifier = modifier.fillMaxWidth().heightIn(max = 320.dp).clip(RoundedCornerShape(12.dp)),
        )
    }
}
