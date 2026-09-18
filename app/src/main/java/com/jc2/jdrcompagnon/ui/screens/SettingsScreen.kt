package com.jc2.jdrcompagnon.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.BuildConfig
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.update.UpdateManager
import com.jc2.jdrcompagnon.update.UpdateUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerName by GameState.playerName.collectAsState()
    val updateState by UpdateManager.state.collectAsState()
    val lastRemoteVersion by UpdateManager.lastRemoteVersion.collectAsState()

    var pseudoInput by remember(playerName) { mutableStateOf(playerName.orEmpty()) }
    var pseudoEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (updateState is UpdateUiState.Idle) {
            UpdateManager.checkForUpdate(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Pseudo du joueur ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pseudo",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = pseudoInput,
                        onValueChange = { pseudoInput = it },
                        label = { Text("Ton nom") },
                        singleLine = true,
                        enabled = pseudoEditing,
                        modifier = Modifier.weight(1f),
                    )
                    if (pseudoEditing) {
                        IconButton(
                            onClick = {
                                if (pseudoInput.isNotBlank()) {
                                    GameState.setPlayerName(pseudoInput.trim())
                                    pseudoEditing = false
                                    Toast.makeText(context, "Pseudo enregistré", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = pseudoInput.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Enregistrer")
                        }
                    } else {
                        IconButton(onClick = { pseudoEditing = true }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Modifier")
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Version de l'application ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Version",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Version actuelle",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        lastRemoteVersion?.let {
                            Text(
                                text = "v$it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Recherche de mise à jour ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Mise à jour disponible",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    IconButton(
                        onClick = {
                            val current = updateState
                            if (current is UpdateUiState.UpdateAvailable) {
                                UpdateManager.openDownloadPage(context, current.release)
                            } else {
                                scope.launch {
                                    val found = UpdateManager.checkForUpdate(context)
                                    if (!found) {
                                        Toast.makeText(
                                            context,
                                            "Aucune mise à jour disponible",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        },
                        enabled = updateState is UpdateUiState.Idle || updateState is UpdateUiState.UpdateAvailable,
                    ) {
                        when (updateState) {
                            is UpdateUiState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(4.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            is UpdateUiState.UpdateAvailable -> {
                                Icon(
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = "Télécharger",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Vérifier",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(
                    text = when (updateState) {
                        is UpdateUiState.Checking -> "Vérification en cours..."
                        is UpdateUiState.UpdateAvailable -> {
                            val latest = lastRemoteVersion ?: "?"
                            "Une nouvelle version v$latest est disponible"
                        }
                        else -> if (lastRemoteVersion != null && lastRemoteVersion != BuildConfig.VERSION_NAME) {
                            "Nouvelle version v$lastRemoteVersion disponible"
                        } else {
                            "Vous avez la dernière version"
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
