package com.jc2.jdrcompagnon.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState

typealias MjGroup = GameState.MjGroup

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCard(
    group: MjGroup,
    availableCharacters: List<Character>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onMemberToggle: (String) -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggleExpand,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                    Text("${group.memberIds.size} membres", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (expanded) "Réduire" else "Modifier", color = MaterialTheme.colorScheme.primary)
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Associer des PJ/PNJ", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                if (availableCharacters.isEmpty()) {
                    Text("Aucun personnage PJ/PNJ disponible.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    availableCharacters.forEach { character ->
                        val selected = group.memberIds.contains(character.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMemberToggle(character.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selected, onCheckedChange = { onMemberToggle(character.id) })
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(character.name, fontWeight = FontWeight.SemiBold)
                                Text(character.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
