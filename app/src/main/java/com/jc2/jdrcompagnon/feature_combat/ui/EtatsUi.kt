package com.jc2.jdrcompagnon.feature_combat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.feature_combat.domain.model.ConditionCombat
import com.jc2.jdrcompagnon.feature_combat.domain.model.Etats

/**
 * Effets cumulés des états d'une créature (catalogue Etats), du plus contraignant au moins
 * contraignant : « ⛔ Paralysé : ne peut pas agir », puis un effet par ligne.
 */
@Composable
fun ResumeEffetsEtats(conditions: Collection<ConditionCombat>, modifier: Modifier = Modifier, couleur: Color = Color.Unspecified) {
    if (conditions.isEmpty()) return
    Column(modifier = modifier.padding(top = 4.dp)) {
        Etats.bloquant(conditions)?.let {
            Text(
                "⛔ ${it.label} : ne peut entreprendre aucune action",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Etats.resumeEffets(conditions).forEach { effet ->
            Text("• $effet", style = MaterialTheme.typography.labelSmall, color = couleur)
        }
    }
}
