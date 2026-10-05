package com.jc2.jdrcompagnon.ui.screens.mj.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterImages
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
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.MonsterImage
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.mikepenz.markdown.m3.Markdown

/**
 * Écran de détail d'un monstre du bestiaire.
 * Affiche le profil du monstre en bloc de stats façon manuel (MonsterStatBlock), dans une carte.
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
    onOpenMenu: () -> Unit = {},
) {
    val context = LocalContext.current
    var monster by remember { mutableStateOf<SrdEntry?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val entry = monster ?: return@rememberLauncherForActivityResult
        if (uri != null && !MonsterImages.importer(context, uri, entry)) {
            Toast.makeText(context, "Impossible d'importer cette image", Toast.LENGTH_SHORT).show()
        }
    }

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
                    val entry = monster!!
                    // Même carte que les autres écrans (fond translucide arrondi), avec l'image,
                    // les boutons d'image puis le bloc de stats façon manuel.
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            MonsterImage(entry, modifier = Modifier.padding(bottom = 8.dp))
                            // Image personnelle du MJ (galerie du téléphone), gardée sur l'appareil :
                            // fonctionne aussi pour les monstres des livres personnalisés.
                            val imageVersion by MonsterImages.version.collectAsState()
                            val hasImport = remember(entry.name, imageVersion) { MonsterImages.hasImport(context, entry) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                                OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (hasImport) "Changer l'image" else "Choisir une image")
                                }
                                if (hasImport) {
                                    TextButton(onClick = { MonsterImages.supprimerImport(context, entry) }) {
                                        Text("Retirer mon image")
                                    }
                                }
                            }
                            // Comportement de l'IA en combat : réservé au MJ.
                            val role by com.jc2.jdrcompagnon.ui.GameState.appRole.collectAsState()
                            if (role == com.jc2.jdrcompagnon.ui.AppRole.MJ) {
                                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                                    com.jc2.jdrcompagnon.feature_combat.ui.ComportementIaMonstreCard(entry.rawMarkdown)
                                }
                                // Butin crédible tiré d'après la fiche (type, puissance, équipement).
                                var afficherButin by remember { mutableStateOf(false) }
                                OutlinedButton(onClick = { afficherButin = true }, modifier = Modifier.padding(bottom = 12.dp)) {
                                    Text("💰 Tirer un butin")
                                }
                                if (afficherButin) {
                                    com.jc2.jdrcompagnon.feature_butin.ui.ButinMonstreDialog(
                                        nom = entry.name,
                                        cle = "bestiaire:${entry.name}",
                                        worldId = currentWorld?.id,
                                        onDismiss = { afficherButin = false },
                                    )
                                }
                            }
                            if (entry.aUnBlocDeStats()) {
                                MonsterStatBlock(entry, modifier = Modifier.clip(RoundedCornerShape(4.dp)))
                            } else {
                                // Monstre d'un livre personnalisé sans champs structurés : texte brut.
                                Markdown(content = entry.rawMarkdown)
                            }
                        }
                    }
                }
            }
        }
    }
}