package com.jc2.jdrcompagnon.feature_carte.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icônes de lieux dessinées pour l'univers médiéval-fantastique (taverne, temple, donjon, repaire
 * de dragon...), absentes des icônes Material. Grille 24×24 comme les icônes Material : teintées
 * par [androidx.compose.material3.Icon] comme les autres.
 *
 * Silhouettes pleines en règle pair-impair : un contour intérieur au contour principal y perce un
 * trou (fenêtres, portes) — les sous-contours d'une même icône ne doivent donc pas se chevaucher
 * ailleurs. Les détails fins (barreaux, ancre, pioches) sont des traits.
 */
internal object IconesLieuxDnd {

    private fun icone(nom: String, contenu: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(nom, 24.dp, 24.dp, 24f, 24f).apply(contenu).build()

    private fun ImageVector.Builder.plein(trace: PathBuilder.() -> Unit) =
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd, pathBuilder = trace)

    private fun ImageVector.Builder.trait(epaisseur: Float = 2f, trace: PathBuilder.() -> Unit) =
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = epaisseur,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = trace,
        )

    private fun PathBuilder.rect(x1: Float, y1: Float, x2: Float, y2: Float) {
        moveTo(x1, y1); horizontalLineTo(x2); verticalLineTo(y2); horizontalLineTo(x1); close()
    }

    private fun PathBuilder.cercle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    /** Chope mousseuse. */
    val taverne = icone("taverne") {
        plein {
            // Chope, avec deux cerclages évidés.
            moveTo(5f, 8f); horizontalLineTo(15f); verticalLineTo(20f)
            quadTo(15f, 21f, 14f, 21f); horizontalLineTo(6f); quadTo(5f, 21f, 5f, 20f); close()
            rect(7f, 11f, 8.3f, 18.5f)
            rect(11.7f, 11f, 13f, 18.5f)
            // Anse.
            moveTo(15f, 10f); horizontalLineTo(18f); quadTo(20f, 10f, 20f, 12f); verticalLineTo(16f)
            quadTo(20f, 18f, 18f, 18f); horizontalLineTo(15f); verticalLineTo(16f); horizontalLineTo(18f)
            verticalLineTo(12f); horizontalLineTo(15f); close()
            // Mousse.
            moveTo(4f, 7.5f); quadTo(3.5f, 4f, 7.5f, 4.5f); quadTo(10f, 1.5f, 13f, 3.8f)
            quadTo(16.5f, 3.5f, 16f, 7.5f); close()
        }
    }

    /** Fronton à colonnes. */
    val temple = icone("temple") {
        plein {
            moveTo(2f, 8.5f); lineTo(12f, 2.5f); lineTo(22f, 8.5f); close()
            rect(3f, 9.5f, 21f, 11.2f)
            rect(4.5f, 12f, 6.5f, 18.3f)
            rect(8.5f, 12f, 10.5f, 18.3f)
            rect(13.5f, 12f, 15.5f, 18.3f)
            rect(17.5f, 12f, 19.5f, 18.3f)
            rect(2f, 19f, 22f, 21.5f)
        }
    }

    /** Château fort : deux tours crénelées, courtine et porte en arc. */
    val chateau = icone("chateau") {
        plein {
            moveTo(2f, 21f); verticalLineTo(4f); horizontalLineTo(4f); verticalLineTo(6f); horizontalLineTo(6f)
            verticalLineTo(4f); horizontalLineTo(8f); verticalLineTo(10f); horizontalLineTo(9.5f); verticalLineTo(8.5f)
            horizontalLineTo(11f); verticalLineTo(10f); horizontalLineTo(13f); verticalLineTo(8.5f); horizontalLineTo(14.5f)
            verticalLineTo(10f); horizontalLineTo(16f); verticalLineTo(4f); horizontalLineTo(18f); verticalLineTo(6f)
            horizontalLineTo(20f); verticalLineTo(4f); horizontalLineTo(22f); verticalLineTo(21f); horizontalLineTo(14f)
            verticalLineTo(16f); quadTo(14f, 13.5f, 12f, 13.5f); quadTo(10f, 13.5f, 10f, 16f); verticalLineTo(21f); close()
            rect(4.2f, 9f, 5.8f, 12f)
            rect(18.2f, 9f, 19.8f, 12f)
        }
    }

    /** Tour de mage au toit pointu, surmontée d'une étoile. */
    val tourMage = icone("tour_mage") {
        plein {
            moveTo(6f, 10f); lineTo(12f, 3f); lineTo(18f, 10f); close()
            moveTo(8f, 11f); horizontalLineTo(16f); lineTo(17f, 21f); horizontalLineTo(7f); close()
            moveTo(11f, 16f); verticalLineTo(14f); quadTo(11f, 13f, 12f, 13f); quadTo(13f, 13f, 13f, 14f); verticalLineTo(16f); close()
            // Étoile au sommet.
            moveTo(12f, 0.3f); lineTo(12.6f, 1.4f); lineTo(13.6f, 1.6f); lineTo(12.7f, 2.2f)
            lineTo(12f, 2.6f); lineTo(11.3f, 2.2f); lineTo(10.4f, 1.6f); lineTo(11.4f, 1.4f); close()
        }
        trait(1.3f) { moveTo(9.5f, 8.5f); quadTo(12f, 7f, 14.5f, 8.5f) }
    }

    /** Porte de donjon en arc, herse baissée. */
    val donjon = icone("donjon") {
        plein {
            moveTo(3f, 21f); verticalLineTo(9f); quadTo(3f, 3f, 12f, 3f); quadTo(21f, 3f, 21f, 9f); verticalLineTo(21f); close()
            moveTo(7f, 21f); verticalLineTo(11f); quadTo(7f, 7f, 12f, 7f); quadTo(17f, 7f, 17f, 11f); verticalLineTo(21f); close()
        }
        trait(1.2f) {
            moveTo(9.5f, 8.2f); verticalLineTo(21f)
            moveTo(12f, 7.2f); verticalLineTo(21f)
            moveTo(14.5f, 8.2f); verticalLineTo(21f)
            moveTo(7.3f, 12.5f); horizontalLineTo(16.7f)
            moveTo(7.3f, 16.8f); horizontalLineTo(16.7f)
        }
    }

    /** Entrée de caverne dans la montagne. */
    val grotte = icone("grotte") {
        plein {
            moveTo(1.5f, 20.5f); lineTo(8f, 8f); lineTo(11f, 11f); lineTo(14.5f, 4.5f); lineTo(22.5f, 20.5f); close()
            moveTo(8.5f, 20.5f); verticalLineTo(17f); quadTo(8.5f, 12.5f, 12.5f, 12.5f); quadTo(16.5f, 12.5f, 16.5f, 17f)
            verticalLineTo(20.5f); close()
        }
    }

    /** Deux sapins. */
    val foret = icone("foret") {
        plein {
            moveTo(8f, 2f); lineTo(12.5f, 9f); lineTo(10.5f, 9f); lineTo(14f, 14f); lineTo(9f, 14f); lineTo(9f, 18.5f)
            lineTo(7f, 18.5f); lineTo(7f, 14f); lineTo(2f, 14f); lineTo(5.5f, 9f); lineTo(3.5f, 9f); close()
            moveTo(17f, 6f); lineTo(20.5f, 12f); lineTo(19f, 12f); lineTo(22f, 17f); lineTo(18f, 17f); lineTo(18f, 21f)
            lineTo(16f, 21f); lineTo(16f, 17f); lineTo(13f, 17f); lineTo(15.5f, 12f); lineTo(14f, 12f); close()
        }
    }

    /** Massif montagneux, sommet enneigé. */
    val montagne = icone("montagne") {
        plein {
            moveTo(1f, 20f); lineTo(8f, 7f); lineTo(12f, 13f); lineTo(15f, 9f); lineTo(23f, 20f); close()
            moveTo(8f, 8.5f); lineTo(9.5f, 10.8f); lineTo(8.7f, 10.3f); lineTo(8f, 11.5f); lineTo(7.3f, 10.3f); lineTo(6.6f, 10.8f); close()
        }
    }

    /** Ancre de port. */
    val port = icone("port") {
        trait(1.8f) {
            cercle(12f, 4.3f, 1.8f)
            moveTo(12f, 6.1f); verticalLineTo(21f)
            moveTo(8f, 9f); horizontalLineTo(16f)
            moveTo(4.5f, 14f); quadTo(5f, 21f, 12f, 21f); quadTo(19f, 21f, 19.5f, 14f)
            moveTo(2.8f, 15.8f); lineTo(4.5f, 13.5f); lineTo(6.6f, 15.3f)
            moveTo(17.4f, 15.3f); lineTo(19.5f, 13.5f); lineTo(21.2f, 15.8f)
        }
    }

    /** Pioches croisées. */
    val mine = icone("mine") {
        trait(2f) {
            moveTo(4f, 20f); lineTo(15.5f, 8.5f)
            moveTo(20f, 20f); lineTo(8.5f, 8.5f)
            moveTo(10f, 4f); quadTo(17f, 2.5f, 20.5f, 9.5f)
            moveTo(14f, 4f); quadTo(7f, 2.5f, 3.5f, 9.5f)
        }
    }

    /** Pierre tombale gravée d'une croix. */
    val cimetiere = icone("cimetiere") {
        plein {
            moveTo(6f, 20f); verticalLineTo(9f); quadTo(6f, 4f, 12f, 4f); quadTo(18f, 4f, 18f, 9f); verticalLineTo(20f); close()
            moveTo(11f, 7f); horizontalLineTo(13f); verticalLineTo(9f); horizontalLineTo(15.5f); verticalLineTo(11f)
            horizontalLineTo(13f); verticalLineTo(16.5f); horizontalLineTo(11f); verticalLineTo(11f); horizontalLineTo(8.5f)
            verticalLineTo(9f); horizontalLineTo(11f); close()
            rect(3f, 20.5f, 21f, 22f)
        }
    }

    /** Arche de pierre traversée d'un tourbillon magique. */
    val portail = icone("portail") {
        plein {
            moveTo(4f, 21f); verticalLineTo(10f); quadTo(4f, 3f, 12f, 3f); quadTo(20f, 3f, 20f, 10f); verticalLineTo(21f)
            horizontalLineTo(17f); verticalLineTo(10.5f); quadTo(17f, 6f, 12f, 6f); quadTo(7f, 6f, 7f, 10.5f); verticalLineTo(21f); close()
        }
        trait(1.3f) {
            moveTo(12f, 14f); quadTo(13.3f, 14f, 13.3f, 15.3f); quadTo(13.3f, 17.2f, 11.4f, 17.2f)
            quadTo(9.4f, 17.2f, 9.4f, 14.8f); quadTo(9.4f, 11.2f, 12.6f, 11.2f); quadTo(15.5f, 11.2f, 15.5f, 15f)
            quadTo(15.5f, 19.5f, 11.8f, 19.5f)
        }
    }

    /** Dragon ailes déployées. */
    val dragon = icone("dragon") {
        plein {
            moveTo(2f, 7f); quadTo(7f, 5f, 12f, 10f); quadTo(17f, 5f, 22f, 7f); lineTo(20f, 9f); lineTo(21f, 12f)
            lineTo(18f, 11f); lineTo(17f, 14f); lineTo(15f, 12f); lineTo(12f, 17f); lineTo(9f, 12f); lineTo(7f, 14f)
            lineTo(6f, 11f); lineTo(3f, 12f); lineTo(4f, 9f); close()
            // Tête et cornes.
            moveTo(10.8f, 8.3f); lineTo(10.2f, 4.2f); lineTo(11.4f, 5.4f); lineTo(12f, 4f); lineTo(12.6f, 5.4f)
            lineTo(13.8f, 4.2f); lineTo(13.2f, 8.3f); close()
        }
        // Queue.
        trait(1.3f) { moveTo(12f, 17f); quadTo(12.5f, 20.5f, 15.5f, 21f) }
    }

    /** Pont de pierre à arche. */
    val pont = icone("pont") {
        plein {
            moveTo(2f, 10f); horizontalLineTo(22f); verticalLineTo(20f); horizontalLineTo(19f)
            quadTo(19f, 14f, 12f, 14f); quadTo(5f, 14f, 5f, 20f); horizontalLineTo(2f); close()
            rect(2f, 6f, 22f, 7.2f)
            rect(3f, 7.2f, 4.2f, 10f)
            rect(8.2f, 7.2f, 9.4f, 10f)
            rect(14.6f, 7.2f, 15.8f, 10f)
            rect(19.8f, 7.2f, 21f, 10f)
        }
    }

    /** Hameau : deux maisons. */
    val village = icone("village") {
        plein {
            moveTo(2f, 21f); verticalLineTo(13f); lineTo(7f, 8f); lineTo(12f, 13f); verticalLineTo(21f); close()
            rect(6f, 17f, 8f, 21f)
            moveTo(13f, 21f); verticalLineTo(11f); lineTo(17.5f, 6.5f); lineTo(22f, 11f); verticalLineTo(21f); close()
            rect(16.5f, 13f, 18.5f, 15f)
        }
    }

    /** Coffre au trésor. */
    val tresor = icone("tresor") {
        plein {
            moveTo(3f, 11f); quadTo(3f, 6f, 8f, 6f); horizontalLineTo(16f); quadTo(21f, 6f, 21f, 11f); close()
            rect(3f, 12f, 21f, 20f)
            rect(10.5f, 12f, 13.5f, 15.5f)
        }
    }

    /** Crâne : repaire de monstres, lieu dangereux. */
    val crane = icone("crane") {
        plein {
            moveTo(12f, 3f); quadTo(20f, 3f, 20f, 11f); quadTo(20f, 14f, 17f, 15f); verticalLineTo(18f)
            horizontalLineTo(7f); verticalLineTo(15f); quadTo(4f, 14f, 4f, 11f); quadTo(4f, 3f, 12f, 3f); close()
            cercle(8.8f, 10.5f, 2f)
            cercle(15.2f, 10.5f, 2f)
            moveTo(12f, 12.8f); lineTo(13f, 14.8f); horizontalLineTo(11f); close()
            rect(8f, 18.8f, 16f, 21f)
        }
    }

    /** Tente de campement. */
    val campement = icone("campement") {
        plein {
            moveTo(2f, 20f); lineTo(12f, 4.5f); lineTo(22f, 20f); close()
            moveTo(12f, 11f); lineTo(16f, 20f); horizontalLineTo(8f); close()
        }
        trait(1.5f) { moveTo(12f, 4.5f); lineTo(10.5f, 2f); moveTo(12f, 4.5f); lineTo(13.5f, 2f) }
    }

    /** Colonnes brisées et fût tombé. */
    val ruine = icone("ruine") {
        plein {
            moveTo(4f, 20f); verticalLineTo(9f); lineTo(5.5f, 7.5f); lineTo(7f, 9.5f); verticalLineTo(20f); close()
            moveTo(10f, 20f); verticalLineTo(12f); lineTo(11.5f, 13f); lineTo(13f, 11f); verticalLineTo(20f); close()
            rect(15f, 17.3f, 22f, 20f)
            rect(2f, 20.5f, 22f, 22f)
        }
    }
}
