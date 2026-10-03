package com.jc2.jdrcompagnon.ui.screens.mj.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdSectionEntry
import com.jc2.jdrcompagnon.ui.components.SrdMarkdownAvecTables

/**
 * Écran de détail générique pour une entrée [SrdSectionEntry] (nom + catégorie +
 * markdown), réutilisé par les onglets Dons, Armes magiques, Classes, Espèces
 * et Historiques de la bibliothèque, qui partagent tous ce même type
 * (contrairement aux sorts/monstres/équipement qui ont leur propre écran).
 *
 * @param kind Identifie la collection dans laquelle chercher l'entrée :
 * "don", "arme_magique", "classe", "espece", "historique", "regle", "glossaire" ou "etat".
 * @param entryName Nom exact de l'entrée à afficher.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SrdSectionDetailScreen(
    kind: String,
    entryName: String?,
    currentWorld: WorldState? = null,
    onBack: () -> Unit,
    onOpenMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    var entry by remember { mutableStateOf<SrdSectionEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(kind, entryName, currentWorld?.id) {
        loading = true
        error = null
        if (entryName.isNullOrBlank()) {
            error = "Aucune entrée spécifiée"
            loading = false
            return@LaunchedEffect
        }
        try {
            entry = when (kind) {
                "don" -> SrdRepository.getDonByName(context, entryName, currentWorld?.id)
                "arme_magique" -> SrdRepository.getArmeArmureMagiqueByName(context, entryName, currentWorld?.id)
                "classe" -> SrdRepository.getClasseByName(context, entryName, currentWorld?.id)
                "espece" -> SrdRepository.getEspeceByName(context, entryName, currentWorld?.id)
                "historique" -> SrdRepository.getHistoriqueByName(context, entryName, currentWorld?.id)
                "regle" -> SrdRepository.getRuleEntryByName(context, entryName, currentWorld?.id)
                "glossaire" -> SrdRepository.getGlossaryEntryByName(context, entryName, currentWorld?.id)
                "etat" -> SrdRepository.getEtatByName(context, entryName, currentWorld?.id)
                else -> null
            }
            if (entry == null) {
                error = "\"$entryName\" introuvable"
            }
        } catch (e: Exception) {
            error = "Erreur de chargement : ${e.message}"
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = entry?.name ?: entryName ?: "Détail",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        when {
            loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            entry != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Même carte translucide arrondie que le reste de l'app.
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            entry!!.category.ifBlank { null }?.let { category ->
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            SrdMarkdownAvecTables(markdown = entry!!.rawMarkdown)
                        }
                    }
                }
            }
        }
    }
}