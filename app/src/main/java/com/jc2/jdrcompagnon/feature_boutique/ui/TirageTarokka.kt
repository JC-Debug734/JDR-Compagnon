package com.jc2.jdrcompagnon.feature_boutique.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Jeu de Tarokka fourni avec l'app (assets/tarokka : cartes.json et une image par carte, plus
 * dos_de_carte.jpg) pour la bonne aventure (EffetService.TAROKKA) : trois cartes tirées au hasard
 * dans les 54, lues comme passé, présent et futur ; leurs phrases mises bout à bout racontent le
 * destin du personnage.
 */
object TarokkaDeck {

    private const val DOSSIER = "tarokka"
    const val DOS = "dos_de_carte.jpg"

    /** [couleur] : EPEES, ETOILES, PIECES, GLYPHES ou HAUT_JEU ; [valeur] : "Maître", "1".."9" ou vide (haut jeu). */
    @Serializable
    data class Carte(
        val nom: String,
        val couleur: String,
        val valeur: String = "",
        val image: String,
        val signification: String = "",
        val passe: String,
        val present: String,
        val futur: String,
    ) {
        val libelleCouleur: String
            get() = when (couleur) {
                "EPEES" -> "Épées"
                "ETOILES" -> "Étoiles"
                "PIECES" -> "Pièces"
                "GLYPHES" -> "Glyphes"
                else -> "Haut jeu"
            }
        val libelle: String
            get() = if (couleur == "HAUT_JEU") "$nom (haut jeu)" else "$nom ($valeur, $libelleCouleur)"
    }

    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cache: List<Carte>? = null

    fun cartes(context: Context): List<Carte> = cache ?: runCatching {
        context.assets.open("$DOSSIER/cartes.json").bufferedReader().use { json.decodeFromString<List<Carte>>(it.readText()) }
    }.getOrDefault(emptyList()).also { if (it.isNotEmpty()) cache = it }

    /** Trois cartes différentes (passé, présent, futur), ou moins si le jeu est introuvable. */
    fun tirer(context: Context): List<Carte> = cartes(context).shuffled().take(3)

    fun image(context: Context, fichier: String): ImageBitmap? = runCatching {
        context.assets.open("$DOSSIER/$fichier").use { BitmapFactory.decodeStream(it)?.asImageBitmap() }
    }.getOrNull()

    /** Conclusion de la diseuse, d'après la couleur dominante du tirage. */
    fun conclusion(tirage: List<Carte>): String {
        val parCouleur = tirage.groupingBy { it.couleur }.eachCount()
        val dominante = parCouleur.entries.firstOrNull { it.value >= 2 }?.key
        return when (dominante) {
            "EPEES" -> "« Ta vie s'écrit avec une lame. Veille à savoir contre qui la tourner. »"
            "ETOILES" -> "« Tu cherches la vérité. Prends garde : certaines vérités dévorent ceux qui les trouvent. »"
            "PIECES" -> "« Tout a un prix en Barovie. Tu paieras le tien, mais choisis bien la monnaie. »"
            "GLYPHES" -> "« Une lumière veille sur toi. Ne la laisse pas s'éteindre. »"
            "HAUT_JEU" -> "« Les puissances de ce monde ont les yeux sur toi. Tu n'es pas venu ici par hasard. »"
            else -> "« Les cartes hésitent. Ton avenir n'est pas encore écrit... profites-en. »"
        }
    }

    /** Remarques particulières de la diseuse (Sombre Seigneur tiré, Brumes au futur...). */
    fun presages(tirage: List<Carte>): List<String> = buildList {
        if (tirage.any { it.nom.contains("Sombre Seigneur") }) add("La vieille femme pose la main sur la tienne : « Il te connaît déjà. »")
        if (tirage.getOrNull(2)?.nom == "Les Brumes") add("Elle rassemble les cartes sans finir : « Je ne vois rien. Personne ne voit à travers les brumes. »")
        else if (tirage.getOrNull(2)?.couleur == "HAUT_JEU") add("Elle marque un long silence avant de parler de ton avenir.")
    }

    /** Les trois phrases (passé, présent, futur) enchaînées en une seule histoire. */
    fun histoire(tirage: List<Carte>): String {
        fun phrase(s: String) = s.trim().trimEnd('.')
        return "« ${phrase(tirage[0].passe)}... ${phrase(tirage[1].present)}... ${tirage[2].futur.trim()} »"
    }

    /** Lecture complète, telle qu'elle est notée sur la fiche du personnage. */
    fun texte(tirage: List<Carte>): String = buildString {
        append("Bonne aventure (Tarokka)\n")
        append("Passé : ${tirage[0].libelle}\nPrésent : ${tirage[1].libelle}\nFutur : ${tirage[2].libelle}\n")
        append(histoire(tirage)).append("\n")
        append(conclusion(tirage))
    }
}

private val POSITIONS = listOf("Le passé", "Le présent", "Le futur")

/**
 * Tirage de bonne aventure : trois cartes posées face cachée, retournées une à une (appui sur la
 * carte ou sur le bouton) ; une fois les trois révélées, l'histoire complète et la conclusion de la
 * diseuse s'affichent.
 */
@Composable
fun TirageTarokkaDialog(tirage: List<TarokkaDeck.Carte>, onDismiss: () -> Unit) {
    var revelees by remember(tirage) { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = ForcedDarkPalette.Background, contentColor = Color.White) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("La bonne aventure", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ForcedDarkPalette.AccentGold)
                if (tirage.size < 3) {
                    Text("Le jeu de Tarokka est introuvable dans l'application.")
                    TextButton(onClick = onDismiss) { Text("Fermer") }
                    return@Column
                }
                Text(
                    if (revelees < 3) "La diseuse pose trois cartes face cachée. Touchez une carte pour la retourner."
                    else "Les trois cartes sont retournées.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tirage.forEachIndexed { i, carte ->
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(POSITIONS[i], style = MaterialTheme.typography.labelMedium, color = ForcedDarkPalette.AccentGold)
                            ImageCarte(
                                fichier = if (i < revelees) carte.image else TarokkaDeck.DOS,
                                modifier = Modifier.padding(top = 4.dp)
                                    .clickable(enabled = i == revelees) { revelees++ },
                            )
                            if (i < revelees) {
                                Text(carte.nom, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
                if (revelees < 3) {
                    Button(onClick = { revelees++ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Retourner ${POSITIONS[revelees].lowercase()}")
                    }
                }
                // Phrase de chaque carte déjà retournée, à sa place dans l'histoire.
                tirage.take(revelees).forEachIndexed { i, carte ->
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${POSITIONS[i]} : ${carte.libelle}", style = MaterialTheme.typography.labelLarge, color = ForcedDarkPalette.AccentGold)
                            Text(listOf(carte.passe, carte.present, carte.futur)[i], fontStyle = FontStyle.Italic)
                            if (carte.signification.isNotBlank()) {
                                Text("Signification : ${carte.signification}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
                if (revelees == 3) {
                    Surface(shape = MaterialTheme.shapes.medium, color = ForcedDarkPalette.AccentGold.copy(alpha = 0.15f), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Ton destin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(TarokkaDeck.histoire(tirage), fontStyle = FontStyle.Italic)
                            TarokkaDeck.presages(tirage).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Text(TarokkaDeck.conclusion(tirage), fontWeight = FontWeight.SemiBold)
                            Text(
                                "La lecture est notée dans les notes du personnage.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Fermer") }
                }
            }
        }
    }
}

@Composable
private fun ImageCarte(fichier: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val image by produceState<ImageBitmap?>(null, fichier) {
        value = withContext(Dispatchers.IO) { TarokkaDeck.image(context, fichier) }
    }
    val forme = Modifier.fillMaxWidth().aspectRatio(0.714f).clip(RoundedCornerShape(6.dp))
    image?.let {
        Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.then(forme))
    } ?: Surface(color = Color.White.copy(alpha = 0.08f), modifier = modifier.then(forme)) {}
}
