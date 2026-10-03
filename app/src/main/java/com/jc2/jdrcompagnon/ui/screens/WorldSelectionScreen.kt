package com.jc2.jdrcompagnon.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository
import com.jc2.jdrcompagnon.ui.worlds.toWorldColorOrNull

/**
 * Choix de l'univers, présenté comme le choix du rôle (RoleSelectionScreen) : de grandes cases
 * empilées portant seulement le nom de l'univers, sans texte de présentation. L'univers actif
 * est coché ; un univers importé peut être retiré (icône corbeille) ; la dernière case importe
 * un nouvel univers depuis une archive .zip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldSelectionScreen(
    onWorldSelected: (String, String, String, ImageVector, Color, Color) -> Unit,
    // Distinct de [onWorldSelected] : un univers importé porte des métadonnées (icône,
    // couleurs, fond d'écran, catégories) qui ne peuvent pas transiter par les types UI-only
    // (ImageVector/Color) du premier callback — l'appelant reçoit directement le WorldState
    // déjà résolu par [CustomWorldsRepository], prêt pour [GameState.selectWorld].
    onCustomWorldSelected: (WorldState) -> Unit = {},
    onBack: (() -> Unit)? = null,
    onOpenMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    val currentWorld by GameState.currentWorld.collectAsState()
    var customWorlds by remember { mutableStateOf(CustomWorldsRepository.listCustomWorlds(context)) }
    var importError by remember { mutableStateOf<String?>(null) }
    var worldToDelete by remember { mutableStateOf<WorldState?>(null) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val fallbackName = queryDisplayName(context, uri)?.substringBeforeLast('.') ?: "Univers importé"
            CustomWorldsRepository.importWorldFromZip(context, uri, fallbackName)
                .onSuccess { customWorlds = CustomWorldsRepository.listCustomWorlds(context) }
                .onFailure { importError = it.message ?: "Import impossible" }
        }
    }

    val dndLabel = stringResource(R.string.world_dnd_label)
    val dndDescription = stringResource(R.string.world_dnd_description)
    val naheulLabel = stringResource(R.string.world_naheul_label)
    val naheulDescription = stringResource(R.string.world_naheul_description)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        // Cases en bas de l'écran, comme le choix du rôle ; défilement si beaucoup d'univers.
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WorldButton(
                label = dndLabel,
                selected = currentWorld?.id == "donjon_et_dragon",
                onClick = {
                    onWorldSelected("donjon_et_dragon", dndLabel, dndDescription, Icons.Filled.Shield, Color(0xFFC62828), Color(0xFFFFD54F))
                },
            )
            WorldButton(
                label = naheulLabel,
                selected = currentWorld?.id == "naheulbeuk",
                onClick = {
                    onWorldSelected("naheulbeuk", naheulLabel, naheulDescription, Icons.Filled.Landscape, Color(0xFF2E7D32), Color(0xFFFFB300))
                },
            )
            customWorlds.forEach { world ->
                WorldButton(
                    label = world.name,
                    selected = currentWorld?.id == world.id,
                    color = world.primaryColorHex.toWorldColorOrNull() ?: RoleSelectionColor,
                    onClick = { onCustomWorldSelected(world) },
                    onDelete = { worldToDelete = world },
                )
            }
            WorldButton(
                label = "Importer un univers",
                selected = false,
                onClick = { zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
            )
        }
        }
    }

    val error = importError
    if (error != null) {
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Import impossible") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { importError = null }) { Text("OK") }
            },
        )
    }

    // Confirmation avant de retirer un univers importé : action irréversible (dossier et
    // tout son contenu supprimés), d'où la confirmation contrairement au retrait d'un simple
    // livre personnalisé dans la bibliothèque.
    val toDelete = worldToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { worldToDelete = null },
            title = { Text("Retirer cet univers ?") },
            text = {
                Text(
                    "« ${toDelete.name} » et tout son contenu importé seront définitivement " +
                        "supprimés de l'appareil.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    CustomWorldsRepository.deleteCustomWorld(context, toDelete.id)
                    customWorlds = CustomWorldsRepository.listCustomWorlds(context)
                    // Univers actif supprimé : AppEntryPoint repasse automatiquement sur D&D.
                    if (GameState.currentWorld.value?.id == toDelete.id) GameState.clearWorld()
                    worldToDelete = null
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { worldToDelete = null }) { Text("Annuler") }
            },
        )
    }
}

/**
 * Case d'univers au style des cases du choix du rôle (bordure dorée, fond translucide, nom en
 * blanc centré). L'univers actif a une bordure plus épaisse et une coche ; [onDelete] ajoute une
 * corbeille (univers importés uniquement).
 */
@Composable
private fun WorldButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = RoleSelectionColor,
    onDelete: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(if (selected) 4.dp else 2.dp, color, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = color.copy(alpha = if (selected) 0.25f else 0.1f),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Univers actif",
                    tint = color,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp),
            )
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer cet univers", tint = Color.White)
                }
            }
        }
    }
}

/** Nom d'affichage d'un fichier choisi via le sélecteur système (colonne DISPLAY_NAME). */
private fun queryDisplayName(context: android.content.Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }
