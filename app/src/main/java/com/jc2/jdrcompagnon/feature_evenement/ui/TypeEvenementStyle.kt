package com.jc2.jdrcompagnon.feature_evenement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement

/** Couleur d'un type d'événement, lisible sur le fond sombre de l'app. */
val TypeEvenement.couleur: Color
    get() = when (this) {
        TypeEvenement.RENCONTRE -> Color(0xFFE57373)
        TypeEvenement.DECOUVERTE -> Color(0xFF81C784)
        TypeEvenement.RUMEUR_INDICE -> Color(0xFF64B5F6)
        TypeEvenement.PERIL -> Color(0xFFFFB74D)
        TypeEvenement.COMPLICATION -> Color(0xFFBA68C8)
        TypeEvenement.OPPORTUNITE -> Color(0xFFD4AF37)
        TypeEvenement.REPIT -> Color(0xFF4DD0E1)
    }

val TypeEvenement.icone: ImageVector
    get() = when (this) {
        TypeEvenement.RENCONTRE -> Icons.Default.Groups
        TypeEvenement.DECOUVERTE -> Icons.Default.Explore
        TypeEvenement.RUMEUR_INDICE -> Icons.Default.RecordVoiceOver
        TypeEvenement.PERIL -> Icons.Default.Warning
        TypeEvenement.COMPLICATION -> Icons.Default.Shuffle
        TypeEvenement.OPPORTUNITE -> Icons.Default.Handshake
        TypeEvenement.REPIT -> Icons.Default.LocalFireDepartment
    }

/** Pastille ronde teintée de la couleur du type, avec son icône. */
@Composable
fun TypeEvenementBadge(type: TypeEvenement, modifier: Modifier = Modifier, taille: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(taille)
            .background(type.couleur.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(type.icone, contentDescription = type.label, tint = type.couleur, modifier = Modifier.size(taille * 0.6f))
    }
}

/** Couleur d'une issue : vert réussite, jaune partielle, rouge échec, gris autre. */
val NatureIssue.couleur: Color
    get() = when (this) {
        NatureIssue.REUSSITE -> Color(0xFF81C784)
        NatureIssue.PARTIELLE -> Color(0xFFFFD54F)
        NatureIssue.ECHEC -> Color(0xFFE57373)
        NatureIssue.AUTRE -> Color(0xFFB0BEC5)
    }
