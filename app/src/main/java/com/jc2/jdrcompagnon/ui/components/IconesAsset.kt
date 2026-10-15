package com.jc2.jdrcompagnon.ui.components

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

/**
 * Icônes illustrées de l'appli (assets/dnd/icone/) : barre du bas ([BARRE_*]) et onglets de la
 * fiche de personnage ([FICHE_*]). Leur fond bleu nuit se fond dans [FondIcones] (bords adoucis
 * en transparence), d'où la couleur de la barre du bas.
 */
object IconesAsset {
    const val BARRE_ACCUEIL = "dnd/icone/barre/accueil.png"
    const val BARRE_CONNEXION = "dnd/icone/barre/connexion.png"
    const val BARRE_BIBLIOTHEQUE = "dnd/icone/barre/bibliotheque.png"
    const val BARRE_DE = "dnd/icone/barre/de.png"

    const val FICHE_APERCU = "dnd/icone/fiche/apercu.png"
    const val FICHE_COMBAT = "dnd/icone/fiche/combat.png"
    const val FICHE_EQUIPEMENT = "dnd/icone/fiche/equipement.png"
    const val FICHE_SORTS = "dnd/icone/fiche/sorts.png"
    const val FICHE_NOTES = "dnd/icone/fiche/notes.png"

    /** Bleu nuit du fond des icônes illustrées. */
    val FondIcones = Color(0xFF111D30)

    /** Parchemin doré des libellés, assorti aux icônes. */
    val TexteDore = Color(0xFFE8C77A)

    /** Liseré doré fin (haut de la barre du bas, séparateurs et flanc du menu latéral MJ). */
    val LisereDore = Color(0xFFD4AF37).copy(alpha = 0.45f)
}

/**
 * Icône illustrée chargée depuis les assets (une seule fois par composition) ; null si le fichier
 * manque — l'appelant garde alors son icône Material de repli.
 */
@Composable
fun rememberIconeAsset(chemin: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(chemin) {
        chemin?.let { c ->
            runCatching { context.assets.open(c).use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
        }
    }
}
