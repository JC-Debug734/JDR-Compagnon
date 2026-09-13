package com.jc2.jdrcompagnon.ui.screens.mj.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.mikepenz.markdown.m3.Markdown

/**
 * Écran de détail d'un monstre du bestiaire.
 * Affiche le stat block du monstre en markdown.
 *
 * @param monsterName Le nom du monstre à afficher
 * @param currentWorld Le monde actuellement sélectionné (détermine dans quelle bibliothèque chercher)
 * @param onBack Action de retour
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BestiaryDetailScreen(
    monsterName: String?,
    currentWorld: WorldState? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var monster by remember { mutableStateOf<SrdEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(monsterName, currentWorld?.id) {
        loading = true
        error = null
        if (monsterName.isNullOrBlank()) {
            error = "Aucun monstre spécifié"
            loading = false
        } else {
            try {
                monster = SrdRepository.getMonsterByName(context, monsterName, currentWorld?.id)
                if (monster == null) {
                    error = "Monstre \"$monsterName\" introuvable"
                }
            } catch (e: Exception) {
                error = "Erreur de chargement : ${e.message}"
            }
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = monster?.name ?: monsterName ?: "Monstre",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            monster != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Markdown(content = monster!!.rawMarkdown)
                }
            }
        }
    }
}