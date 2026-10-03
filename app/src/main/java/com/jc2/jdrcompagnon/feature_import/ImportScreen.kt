package com.jc2.jdrcompagnon.feature_import

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFolderUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.jc2.jdrcompagnon.ui.WorldState
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Outil IMPORT (tableau de bord MJ) : un seul endroit pour importer n'importe quel fichier ou
 * dossier — le genre de contenu est reconnu automatiquement par [ImportCentral] — et
 * l'historique de tous les imports ([ImportHistorique]). Chaque import de l'historique peut
 * être supprimé en entier : tout ce qu'il a créé est retiré ([ImportCentral.supprimerImport]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    currentWorld: WorldState?,
    onOpenMenu: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val historique by ImportHistorique.entrees.collectAsState()
    val entrees = historique ?: remember { ImportHistorique.charger(context) }
    var enCours by remember { mutableStateOf(false) }
    var dernier by remember { mutableStateOf<ImportHistorique.Entree?>(null) }
    var aideOuverte by remember { mutableStateOf(ImportPrefs.aideOuverte(context)) }
    var aSupprimer by remember { mutableStateOf<ImportHistorique.Entree?>(null) }
    var confirmerVider by remember { mutableStateOf(false) }

    fun lancer(action: suspend () -> ImportHistorique.Entree) {
        enCours = true
        scope.launch {
            dernier = action()
            enCours = false
        }
    }

    val fichierLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) lancer { ImportCentral.importerFichier(context, uri, currentWorld?.id) }
    }
    val dossierLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) lancer { ImportCentral.importerDossier(context, uri, currentWorld?.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                actions = {
                    if (entrees.isNotEmpty()) {
                        IconButton(onClick = { confirmerVider = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Vider l'historique", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "actions") {
                Carte {
                    currentWorld?.name?.let {
                        Text("Univers : $it", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.size(8.dp))
                    }
                    Button(
                        onClick = { fichierLauncher.launch(arrayOf("*/*")) },
                        enabled = !enCours,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Importer un fichier")
                    }
                    OutlinedButton(
                        onClick = { dossierLauncher.launch(null) },
                        enabled = !enCours,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.DriveFolderUpload, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Importer un dossier", color = Color.White)
                    }
                    if (enCours) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Import en cours…", color = Color.White)
                        }
                    }
                    // Compte rendu du dernier import : déplié d'office, c'est le retour immédiat.
                    dernier?.let { LigneHistorique(it, modifier = Modifier.padding(top = 8.dp), deplieParDefaut = true) }
                }
            }

            item(key = "aide") {
                Carte {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            aideOuverte = !aideOuverte
                            ImportPrefs.setAideOuverte(context, aideOuverte)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Ce que l'import reconnaît",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (aideOuverte) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (aideOuverte) "Replier" else "Déplier",
                            tint = Color.White,
                        )
                    }
                    if (aideOuverte) {
                        listOf(
                            "Livre .md" to "monstres, PNJ, objets, sorts, règles… (balises <!-- type: ... -->). Les PNJ deviennent des fiches PNJ.",
                            "Livre .zip" to "un ou plusieurs .md avec leurs images (ligne Image: images/nom.webp), et ses scénarios rangés dans un dossier Scenarios/.",
                            "Univers .zip" to "archive contenant un reference.md.",
                            "Scénario .md" to "fichier de scénario (scènes, images, lieux, PNJ).",
                            "Fiche .json" to "personnage exporté depuis JDRCompagnon.",
                            "Musique" to "mp3, ogg, wav, m4a, flac…",
                            "Dossier" to "campagne (avec campagne.json) ou dossier de scénarios avec Personnages/ et Bibliotheque/.",
                            "Autre fichier" to "PDF… ajouté comme livre à consulter.",
                        ).forEach { (titre, detail) ->
                            Text(
                                "• $titre : $detail",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }

            item(key = "titre_historique") {
                Text(
                    "Historique",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (entrees.isEmpty()) {
                item(key = "vide") {
                    Text("Aucun import pour l'instant.", color = Color.White.copy(alpha = 0.8f))
                }
            }
            items(entrees, key = { "${it.date}_${it.fichier}" }) { entree ->
                Carte {
                    LigneHistorique(
                        entree,
                        onSupprimer = if (entree.elements.isNotEmpty() && !entree.supprime && !enCours) {
                            { aSupprimer = entree }
                        } else null,
                    )
                }
            }
        }
    }

    if (confirmerVider) {
        AlertDialog(
            onDismissRequest = { confirmerVider = false },
            title = { Text("Vider l'historique ?") },
            text = { Text("La liste des imports est effacée. Le contenu importé reste dans l'app, mais ne pourra plus être retiré depuis cet écran.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmerVider = false
                    ImportHistorique.vider(context)
                    dernier = null
                }) { Text("Vider", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmerVider = false }) { Text("Annuler") } },
        )
    }

    aSupprimer?.let { entree ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer cet import ?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("« ${entree.fichier} » : tout ce que cet import a ajouté sera retiré de l'app, avec ses fichiers.")
                    entree.elements.groupBy { it.type }.forEach { (type, elements) ->
                        Text(
                            libelleType(type) + " : " + elements.joinToString(", ") { it.libelle },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (entree.elements.any { it.type == "personnage" }) {
                        Text(
                            "Les fiches créées par l'import sont supprimées même si elles ont été modifiées depuis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    aSupprimer = null
                    lancer { entree.copy(message = ImportCentral.supprimerImport(context, entree), supprime = true) }
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } },
        )
    }
}

private fun libelleType(type: String): String = when (type) {
    "scenario" -> "Scénarios"
    "campagne" -> "Campagnes"
    "personnage" -> "Fiches"
    "livre" -> "Livres"
    "univers" -> "Univers"
    "musique" -> "Musiques"
    else -> type
}

@Composable
private fun Carte(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

/**
 * Ligne d'historique : seul le nom du fichier est affiché ; un tap déplie les détails
 * (genre, date, compte rendu) et le bouton de suppression de l'import.
 */
@Composable
private fun LigneHistorique(
    entree: ImportHistorique.Entree,
    modifier: Modifier = Modifier,
    deplieParDefaut: Boolean = false,
    onSupprimer: (() -> Unit)? = null,
) {
    var deplie by remember(entree.date, entree.fichier) { mutableStateOf(deplieParDefaut) }
    Row(
        modifier = modifier.fillMaxWidth().clickable { deplie = !deplie },
        verticalAlignment = if (deplie) Alignment.Top else Alignment.CenterVertically,
    ) {
        Icon(
            when {
                entree.supprime -> Icons.Default.RemoveCircleOutline
                entree.succes -> Icons.Default.CheckCircle
                else -> Icons.Default.Error
            },
            contentDescription = when {
                entree.supprime -> "Supprimé"
                entree.succes -> "Réussi"
                else -> "Échec"
            },
            tint = when {
                entree.supprime -> Color.White.copy(alpha = 0.6f)
                entree.succes -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entree.fichier, fontWeight = FontWeight.Bold, color = Color.White)
            if (deplie) {
                Text(
                    "${entree.genre.libelle} · ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entree.date))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(entree.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
            }
        }
        if (deplie && onSupprimer != null) {
            IconButton(onClick = onSupprimer) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer cet import", tint = MaterialTheme.colorScheme.error)
            }
        }
        Icon(
            if (deplie) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (deplie) "Masquer les détails" else "Voir les détails",
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = if (deplie) 2.dp else 0.dp),
        )
    }
}

/** Repli de l'aide, conservé entre les écrans et les redémarrages. */
private object ImportPrefs {
    private const val PREFS_NAME = "import_screen"
    private const val KEY_AIDE = "aide_ouverte"

    fun aideOuverte(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AIDE, true)

    fun setAideOuverte(context: Context, ouverte: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_AIDE, ouverte).apply()
    }
}
