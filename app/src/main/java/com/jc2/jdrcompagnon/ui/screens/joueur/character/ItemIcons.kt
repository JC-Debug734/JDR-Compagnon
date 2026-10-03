package com.jc2.jdrcompagnon.ui.screens.joueur

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules

/**
 * Icône représentant un objet précis (une épée pour une épée, un arc pour un arc...),
 * affichée sur un emplacement occupé à la place de l'icône générique de l'emplacement.
 * Material ne propose ni épée, ni arc, ni carquois : ces silhouettes sont dessinées ici
 * (grille 24×24, teintées par Icon comme les icônes Material).
 */
fun itemIcon(itemName: String): ImageVector {
    val lower = itemName.lowercase()
    fun has(vararg mots: String) = mots.any { lower.contains(it) }
    return when {
        has("carquois") -> ItemIcons.Quiver
        has("fléchette", "flechette") -> ItemIcons.Dagger
        ArmorRules.isArrowItem(itemName) || has("carreau") -> ItemIcons.Arrow
        has("arbalète", "arbalete") -> ItemIcons.Crossbow
        Regex("""\barc\b""").containsMatchIn(lower) -> ItemIcons.Bow
        has("dague", "couteau", "serpe") -> ItemIcons.Dagger
        has("épée", "epee", "rapière", "rapiere", "cimeterre", "sabre") -> ItemIcons.Sword
        has("hache", "hachette") -> ItemIcons.Axe
        has("lance", "pique", "javeline", "trident", "hallebarde", "coutille") -> ItemIcons.Spear
        has("bâton", "baton", "gourdin") -> ItemIcons.Staff
        has("marteau", "masse", "maillet", "morgenstern", "fléau", "fleau", "pic de guerre") -> Icons.Default.Hardware
        has("bouclier") -> Icons.Default.Shield
        ArmorRules.slotForItem(itemName) == EquipmentSlot.TORSO -> ItemIcons.Armor
        has("casque", "heaume") -> ItemIcons.Helmet
        has("potion", "fiole", "flasque", "élixir", "elixir", "huile") -> ItemIcons.Potion
        has("sac à dos", "sac a dos") -> Icons.Default.Backpack
        has("bourse", "sacoche") -> ItemIcons.Pouch
        has("torche", "lanterne", "bougie", "lampe") -> Icons.Default.LocalFireDepartment
        has("parchemin") -> Icons.Default.Description
        has("livre", "grimoire", "journal") -> Icons.Default.MenuBook
        has("corde", "grappin", "chaîne", "chaine") -> Icons.Default.Link
        has("ration", "nourriture") -> Icons.Default.Restaurant
        has("sac de couchage", "couverture", "tente", "natte", "hamac") -> Icons.Default.Bed
        has("outre", "gourde") -> Icons.Default.WaterDrop
        has("anneau", "amulette", "collier", "bijou", "diadème", "couronne") -> Icons.Default.Diamond
        has("gant") -> Icons.Default.PanTool
        has("botte", "bottine") -> Icons.Default.Hiking
        has("cape", "manteau") -> Icons.Default.DryCleaning
        ArmorRules.isClothing(itemName) -> Icons.Default.Checkroom
        has("instrument", "luth", "flûte", "flute", "lyre", "tambour", "cor ", "cornemuse", "viole") -> Icons.Default.MusicNote
        has("outil", "trousse", "matériel", "materiel", "kit") -> Icons.Default.Handyman
        has("symbole sacré", "symbole sacre", "focaliseur", "orbe", "baguette", "sceptre") -> Icons.Default.AutoAwesome
        else -> ArmorRules.slotForItem(itemName)?.slotIcon() ?: Icons.Default.Inventory2
    }
}

object ItemIcons {
    private fun icon(name: String, content: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(content).build()

    private fun ImageVector.Builder.fill(block: PathBuilder.() -> Unit) =
        path(fill = SolidColor(Color.Black), pathBuilder = block)

    private fun ImageVector.Builder.stroke(width: Float, block: PathBuilder.() -> Unit) =
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )

    private fun PathBuilder.rect(left: Float, top: Float, right: Float, bottom: Float) {
        moveTo(left, top); lineTo(right, top); lineTo(right, bottom); lineTo(left, bottom); close()
    }

    /** Épée droite, en diagonale. */
    val Sword: ImageVector by lazy {
        icon("Sword") {
            group(rotate = 45f, pivotX = 12f, pivotY = 12f) {
                fill { moveTo(12f, 0.5f); lineTo(13.4f, 3f); lineTo(13.4f, 15f); lineTo(10.6f, 15f); lineTo(10.6f, 3f); close() }
                fill { rect(7f, 15f, 17f, 16.8f) }
                fill { rect(11f, 16.8f, 13f, 21f) }
                fill { rect(10.3f, 21f, 13.7f, 23f) }
            }
        }
    }

    /** Dague : lame courte, en diagonale. */
    val Dagger: ImageVector by lazy {
        icon("Dagger") {
            group(rotate = 45f, pivotX = 12f, pivotY = 12f) {
                fill { moveTo(12f, 3f); lineTo(13.6f, 6f); lineTo(13.6f, 14f); lineTo(10.4f, 14f); lineTo(10.4f, 6f); close() }
                fill { rect(8f, 14f, 16f, 15.6f) }
                fill { rect(11f, 15.6f, 13f, 19.5f) }
                fill { rect(10.3f, 19.5f, 13.7f, 21.2f) }
            }
        }
    }

    /** Hache : manche + fer en demi-lune. */
    val Axe: ImageVector by lazy {
        icon("Axe") {
            group(rotate = 25f, pivotX = 12f, pivotY = 12f) {
                fill { rect(11f, 2f, 13f, 23f) }
                fill {
                    moveTo(13f, 4f)
                    curveTo(18f, 3f, 21.5f, 6f, 21.5f, 9.5f)
                    curveTo(21.5f, 13f, 18f, 15.5f, 13f, 14.5f)
                    close()
                }
            }
        }
    }

    /** Lance / javeline : hampe + fer triangulaire, en diagonale. */
    val Spear: ImageVector by lazy {
        icon("Spear") {
            group(rotate = 45f, pivotX = 12f, pivotY = 12f) {
                fill { moveTo(12f, 0.5f); lineTo(14.6f, 6.5f); lineTo(9.4f, 6.5f); close() }
                fill { rect(11.1f, 6.5f, 12.9f, 23.5f) }
            }
        }
    }

    /** Bâton : long bois en diagonale, pommeau rond. */
    val Staff: ImageVector by lazy {
        icon("Staff") {
            stroke(2.4f) { moveTo(5f, 21f); lineTo(17f, 7f) }
            fill {
                moveTo(18.5f, 2.5f)
                curveTo(20.5f, 2.5f, 21.5f, 3.5f, 21.5f, 5.5f)
                curveTo(21.5f, 7.5f, 20.5f, 8.5f, 18.5f, 8.5f)
                curveTo(16.5f, 8.5f, 15.5f, 7.5f, 15.5f, 5.5f)
                curveTo(15.5f, 3.5f, 16.5f, 2.5f, 18.5f, 2.5f)
                close()
            }
        }
    }

    /** Arc : branche courbe + corde. */
    val Bow: ImageVector by lazy {
        icon("Bow") {
            stroke(2.4f) { moveTo(7f, 2.5f); quadTo(23f, 12f, 7f, 21.5f) }
            stroke(1f) { moveTo(7f, 2.5f); lineTo(7f, 21.5f) }
        }
    }

    /** Flèche en diagonale : hampe, pointe, empennage. */
    val Arrow: ImageVector by lazy {
        icon("Arrow") {
            stroke(1.8f) { moveTo(4.5f, 19.5f); lineTo(18f, 6f) }
            fill { moveTo(21.5f, 2.5f); lineTo(19.8f, 9.2f); lineTo(14.8f, 4.2f); close() }
            stroke(1.6f) {
                moveTo(4.5f, 19.5f); lineTo(2.5f, 17.5f)
                moveTo(4.5f, 19.5f); lineTo(6.5f, 21.5f)
                moveTo(6.5f, 17.5f); lineTo(4.5f, 15.5f)
                moveTo(6.5f, 17.5f); lineTo(8.5f, 19.5f)
            }
        }
    }

    /** Arbalète vue de dessus : arc horizontal, fût, corde tendue. */
    val Crossbow: ImageVector by lazy {
        icon("Crossbow") {
            stroke(2.2f) { moveTo(3f, 10f); quadTo(12f, 2f, 21f, 10f) }
            fill { rect(10.9f, 4f, 13.1f, 22f) }
            stroke(1f) { moveTo(3f, 10f); lineTo(12f, 14f); lineTo(21f, 10f) }
        }
    }

    /** Carquois : étui avec trois flèches qui dépassent. */
    val Quiver: ImageVector by lazy {
        icon("Quiver") {
            fill { moveTo(7.5f, 10f); lineTo(16.5f, 10f); lineTo(15.2f, 22.5f); lineTo(8.8f, 22.5f); close() }
            stroke(1.4f) {
                moveTo(9.5f, 10f); lineTo(7.5f, 3.5f)
                moveTo(12f, 10f); lineTo(12f, 2.5f)
                moveTo(14.5f, 10f); lineTo(16.5f, 3.5f)
            }
            fill {
                moveTo(7f, 1.8f); lineTo(8.9f, 3.9f); lineTo(6.4f, 4.6f); close()
                moveTo(12f, 0.8f); lineTo(13.4f, 3.2f); lineTo(10.6f, 3.2f); close()
                moveTo(17f, 1.8f); lineTo(17.6f, 4.6f); lineTo(15.1f, 3.9f); close()
            }
        }
    }

    /** Potion : fiole ronde à col étroit. */
    val Potion: ImageVector by lazy {
        icon("Potion") {
            fill {
                moveTo(9.5f, 2f); lineTo(14.5f, 2f); lineTo(14.5f, 4f); lineTo(13.5f, 4f); lineTo(13.5f, 8.2f)
                curveTo(17f, 9.2f, 19.5f, 12f, 19.5f, 15f)
                curveTo(19.5f, 19.2f, 16.2f, 22f, 12f, 22f)
                curveTo(7.8f, 22f, 4.5f, 19.2f, 4.5f, 15f)
                curveTo(4.5f, 12f, 7f, 9.2f, 10.5f, 8.2f)
                lineTo(10.5f, 4f); lineTo(9.5f, 4f); close()
            }
        }
    }

    /** Bourse : petit sac fermé par un lien. */
    val Pouch: ImageVector by lazy {
        icon("Pouch") {
            fill {
                moveTo(8.5f, 3f); lineTo(15.5f, 3f); lineTo(13.6f, 6.8f)
                curveTo(18f, 8f, 20.5f, 12f, 20.5f, 16f)
                curveTo(20.5f, 20f, 17f, 22f, 12f, 22f)
                curveTo(7f, 22f, 3.5f, 20f, 3.5f, 16f)
                curveTo(3.5f, 12f, 6f, 8f, 10.4f, 6.8f)
                close()
            }
        }
    }

    /** Armure : cuirasse. */
    val Armor: ImageVector by lazy {
        icon("Armor") {
            fill {
                moveTo(6f, 3f); lineTo(9f, 3f)
                curveTo(9.5f, 5f, 14.5f, 5f, 15f, 3f)
                lineTo(18f, 3f); lineTo(21.5f, 6.5f); lineTo(19f, 9.5f); lineTo(19f, 20f)
                curveTo(15f, 22.5f, 9f, 22.5f, 5f, 20f)
                lineTo(5f, 9.5f); lineTo(2.5f, 6.5f); close()
            }
        }
    }

    /** Casque à nasal. */
    val Helmet: ImageVector by lazy {
        icon("Helmet") {
            fill {
                moveTo(4f, 14f)
                curveTo(4f, 7f, 7.5f, 3f, 12f, 3f)
                curveTo(16.5f, 3f, 20f, 7f, 20f, 14f)
                lineTo(20f, 19f); lineTo(14f, 19f); lineTo(14f, 12.5f); lineTo(10f, 12.5f); lineTo(10f, 19f); lineTo(4f, 19f)
                close()
            }
        }
    }
}
