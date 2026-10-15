package com.jc2.jdrcompagnon.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jc2.jdrcompagnon.ui.AccesDossierJdr
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.PersonnageFichiers
import com.jc2.jdrcompagnon.ui.rememberImageFichier
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Histoire personnelle d'un personnage, en plein écran : lecture (markdown et images) ou
 * édition. Le texte est celui du fichier `<nom>-<id8>.md` rangé à côté de la fiche (voir
 * [PersonnageFichiers]) : à l'ouverture, une version du fichier modifiée hors de l'app est
 * reprise dans la fiche. Les images sont copiées dans le même dossier et insérées dans le texte
 * par leur nom (`![](nom.jpg)`), ce qui garde le .md lisible ailleurs avec ses images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoirePersonnelleDialog(
    character: Character,
    editable: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var edition by remember { mutableStateOf(editable && character.histoirePersonnelle.isBlank()) }
    var brouillon by remember { mutableStateOf(TextFieldValue(character.histoirePersonnelle)) }
    var message by remember { mutableStateOf<String?>(null) }
    // Image manquante à remplacer (nom imposé par le texte) ; incrémenté pour rafraîchir les images.
    var imageARemplacer by remember { mutableStateOf<String?>(null) }
    var versionImages by remember { mutableIntStateOf(0) }

    val accesDossier by AccesDossierJdr.accorde.collectAsState()
    val imagesRecues by PersonnageFichiers.imagesRecues.collectAsState()

    // Fichier .md modifié hors de l'app, ou collé à la main sous le nom exact de la fiche : sa
    // version est reprise automatiquement dans la fiche (relu dès que l'accès au dossier est accordé).
    LaunchedEffect(character.id, accesDossier) {
        val fichier = withContext(Dispatchers.IO) {
            AccesDossierJdr.estAccorde(context)
            PersonnageFichiers.lireHistoire(context, character)
        }
        if (fichier != null && fichier != character.histoirePersonnelle) {
            GameState.updateCharacterHistoire(character.id, fichier)
            brouillon = TextFieldValue(fichier)
            edition = false
        }
        versionImages++
    }

    // Histoire mise à jour ailleurs (réseau) pendant la lecture : affichée.
    LaunchedEffect(character.histoirePersonnelle) {
        if (!edition) brouillon = TextFieldValue(character.histoirePersonnelle)
    }

    val autoriserDossier = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && !AccesDossierJdr.enregistrer(context, uri)) {
            message = "Choisissez le dossier « JDRCompagnon » (dans Téléchargements)."
        }
    }

    fun inserer(texte: String) {
        val v = brouillon
        val avant = v.text.substring(0, v.selection.start)
        val apres = v.text.substring(v.selection.end)
        val prefixe = if (avant.isEmpty() || avant.endsWith("\n\n")) "" else if (avant.endsWith("\n")) "\n" else "\n\n"
        val ajout = "$prefixe$texte\n\n"
        brouillon = TextFieldValue(avant + ajout + apres, TextRange(avant.length + ajout.length))
    }

    val choisirImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val nomImpose = imageARemplacer
        imageARemplacer = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val nom = withContext(Dispatchers.IO) { PersonnageFichiers.ajouterImage(context, character, uri, nomImpose) }
            when {
                nom == null -> message = "Impossible de lire cette image."
                nomImpose != null -> versionImages++
                else -> inserer("![]($nom)")
            }
        }
    }

    val importerMd = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val texte = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            if (texte == null) message = "Impossible de lire ce fichier."
            else {
                brouillon = TextFieldValue(texte)
                edition = true
                val manquantes = PersonnageFichiers.imagesReferencees(texte).count { nom ->
                    withContext(Dispatchers.IO) { PersonnageFichiers.fichierImage(context, character, nom) } == null
                }
                if (manquantes > 0) message = "$manquantes image(s) introuvable(s) : touchez-les dans la lecture pour les choisir."
            }
        }
    }

    val modifie = brouillon.text != character.histoirePersonnelle
    // Fermer enregistre ce qui a été écrit, pour ne jamais perdre une histoire en cours.
    val fermer = {
        if (editable && modifie) GameState.updateCharacterHistoire(character.id, brouillon.text)
        onDismiss()
    }

    Dialog(
        onDismissRequest = fermer,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = SheetSurface,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text("Histoire — ${character.name}", fontWeight = FontWeight.Bold, color = SheetTextPrimary, maxLines = 1)
                    },
                    navigationIcon = {
                        IconButton(onClick = fermer) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer", tint = SheetTextPrimary)
                        }
                    },
                    actions = {
                        if (editable) {
                            IconButton(onClick = { edition = !edition }) {
                                Icon(
                                    if (edition) Icons.Default.MenuBook else Icons.Default.Edit,
                                    contentDescription = if (edition) "Lecture" else "Modifier",
                                    tint = ForcedDarkPalette.AccentGold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sans accès au dossier, un .md ou une image collés à la main restent invisibles (Android).
                if (accesDossier == false) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Pour reprendre automatiquement un fichier .md ou des images collés à la main dans le dossier " +
                                "du personnage, autorisez une fois l'accès au dossier « JDRCompagnon ».",
                            style = MaterialTheme.typography.bodySmall,
                            color = SheetTextSecondary
                        )
                        OutlinedButton(onClick = { autoriserDossier.launch(AccesDossierJdr.uriInitiale()) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Autoriser l'accès au dossier")
                        }
                    }
                }
                if (edition) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { choisirImage.launch("image/*") }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Image")
                        }
                        OutlinedButton(
                            onClick = { importerMd.launch(arrayOf("text/markdown", "text/plain", "text/*", "application/octet-stream")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Importer .md")
                        }
                    }
                    OutlinedTextField(
                        value = brouillon,
                        onValueChange = { brouillon = it },
                        label = { Text("Histoire (markdown : # titre, **gras**, *italique*, - liste)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                        colors = sheetTextFieldColors()
                    )
                    Button(
                        onClick = {
                            GameState.updateCharacterHistoire(character.id, brouillon.text)
                            edition = false
                        },
                        enabled = modifie,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enregistrer") }
                } else {
                    val texte = brouillon.text
                    if (texte.isBlank()) {
                        Text(
                            if (editable) "Aucune histoire pour l'instant. Touchez le crayon pour l'écrire."
                            else "Aucune histoire pour l'instant.",
                            color = SheetTextSecondary
                        )
                    }
                    PersonnageFichiers.decouper(texte).forEach { bloc ->
                        when (bloc) {
                            is PersonnageFichiers.Bloc.Texte ->
                                Markdown(content = bloc.contenu, colors = markdownColor(text = SheetTextPrimary))
                            is PersonnageFichiers.Bloc.Image -> ImageHistoire(
                                character = character,
                                nom = bloc.nom,
                                alt = bloc.alt,
                                version = versionImages + imagesRecues,
                                onChoisir = if (editable) {
                                    { imageARemplacer = bloc.nom; choisirImage.launch("image/*") }
                                } else null
                            )
                        }
                    }
                }
                Text(
                    "Fichier : ${PersonnageFichiers.cheminHistoire(character)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SheetTextSecondary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
        )
    }
}

/** Image de l'histoire ; introuvable, un cadre propose de la choisir (si [onChoisir]). */
@Composable
private fun ImageHistoire(
    character: Character,
    nom: String,
    alt: String,
    version: Int,
    onChoisir: (() -> Unit)?,
) {
    val context = LocalContext.current
    val fichier by produceState<java.io.File?>(null, nom, version) {
        value = withContext(Dispatchers.IO) { PersonnageFichiers.fichierImage(context, character, nom) }
    }
    val image = rememberImageFichier(fichier)
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = alt.ifBlank { null },
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(Color.White.copy(alpha = 0.06f))
                .then(if (onChoisir != null) Modifier.clickable(onClick = onChoisir) else Modifier)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Image introuvable : $nom" + if (onChoisir != null) "\nToucher pour la choisir" else "",
                color = SheetTextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
