package com.jc2.jdrcompagnon.ui.screens.mj

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.InputChipDefaults
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material3.InputChip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.onFocusChanged
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import java.io.File
import java.util.zip.ZipInputStream
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.LibrarySearch
import com.jc2.jdrcompagnon.R
import com.jc2.jdrcompagnon.ui.WorldState
import com.jc2.jdrcompagnon.ui.theme.ForcedDarkPalette
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.RuleSection
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdEntry
import com.jc2.jdrcompagnon.feature_environnement.domain.model.TERRAINS_SRD
import com.jc2.jdrcompagnon.feature_environnement.domain.model.rencontrableDans
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdSectionEntry
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdDocSection
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentSummary
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomBookImages
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.PnjImport
import com.jc2.jdrcompagnon.ui.screens.mj.scenario.ScenarioImport
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.ReferenceEntryParser
import com.jc2.jdrcompagnon.ui.components.SrdMarkdownAvecTables
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Les 7 illustrations de couverture disponibles pour personnaliser l'apparence de
 * chaque livre sur l'étagère (voir [LibraryBookSettingsScreen]). Index utilisé comme
 * valeur stockée dans [LibraryBookSettingsStore] pour la couverture choisie.
 */
private val bookSkinDrawables = listOf(
    R.drawable.livre_1,
    R.drawable.livre_2,
    R.drawable.livre_3,
    R.drawable.livre_4,
    R.drawable.livre_5,
    R.drawable.livre_6,
    R.drawable.livre_7,
)

/**
 * Persistance (SharedPreferences, donc conservée à la fermeture de l'application) des
 * réglages par livre définis depuis l'écran de gestion (ouvert en cliquant la bibliothécaire) :
 * la couverture choisie et si le livre est visible pour les joueurs. Clé par monde + par
 * livre, pour pouvoir avoir des réglages différents selon le monde actif.
 */
private object LibraryBookSettingsStore {
    private const val PREFS_NAME = "library_book_settings"
    private const val NO_OVERRIDE = -1

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun skinKey(worldId: String?, tabKey: String) = "${worldId ?: "default"}:$tabKey:skin"
    private fun visibleKey(worldId: String?, tabKey: String) = "${worldId ?: "default"}:$tabKey:visible_to_players"

    /** Index de couverture choisi pour ce livre, ou null si aucun choix (cycle par défaut). */
    fun getSkinIndex(context: Context, worldId: String?, tabKey: String): Int? {
        val value = prefs(context).getInt(skinKey(worldId, tabKey), NO_OVERRIDE)
        return value.takeIf { it in bookSkinDrawables.indices }
    }

    fun setSkinIndex(context: Context, worldId: String?, tabKey: String, index: Int) {
        prefs(context).edit().putInt(skinKey(worldId, tabKey), index).apply()
    }

    /** Vrai par défaut : un livre est visible aux joueurs tant qu'on ne l'a pas masqué. */
    fun isVisibleToPlayers(context: Context, worldId: String?, tabKey: String): Boolean =
        prefs(context).getBoolean(visibleKey(worldId, tabKey), true)

    fun setVisibleToPlayers(context: Context, worldId: String?, tabKey: String, visible: Boolean) {
        prefs(context).edit().putBoolean(visibleKey(worldId, tabKey), visible).apply()
    }
}

/**
 * Un livre ajouté manuellement par l'utilisateur depuis l'écran de gestion des livres
 * (n'importe quel fichier, .md ou non, sélectionné via le sélecteur de fichiers système).
 * [fileName] est le nom du fichier copié dans le stockage interne de l'app (voir
 * [copyPickedFileToInternalStorage]) — la donnée reste disponible même si le fichier
 * d'origine est supprimé ou son URI révoquée.
 */
internal data class CustomBook(
    val id: String,
    val name: String,
    val fileName: String,
)

/**
 * Persistance (SharedPreferences) de la liste des livres personnalisés ajoutés par
 * l'utilisateur, par monde. La couverture et la disponibilité aux joueurs de chaque livre
 * personnalisé réutilisent [LibraryBookSettingsStore] avec sa clé "custom_<id>", comme
 * n'importe quel autre livre — seuls le nom et le fichier associé sont propres à ce store.
 */
internal object CustomBooksStore {
    private const val PREFS_NAME = "library_custom_books"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun idsKey(worldId: String?) = "${worldId ?: "default"}:ids"
    private fun nameKey(worldId: String?, id: String) = "${worldId ?: "default"}:$id:name"
    private fun fileKey(worldId: String?, id: String) = "${worldId ?: "default"}:$id:file"

    fun list(context: Context, worldId: String?): List<CustomBook> {
        val ids = prefs(context).getStringSet(idsKey(worldId), emptySet()).orEmpty()
        return ids.mapNotNull { id ->
            val name = prefs(context).getString(nameKey(worldId, id), null) ?: return@mapNotNull null
            val fileName = prefs(context).getString(fileKey(worldId, id), null) ?: return@mapNotNull null
            CustomBook(id = id, name = name, fileName = fileName)
        }.sortedBy { it.name.lowercase() }
    }

    /** Enregistre un nouveau livre personnalisé sous l'identifiant [id] (sans préfixe). */
    fun add(context: Context, worldId: String?, id: String, name: String, fileName: String) {
        val ids = prefs(context).getStringSet(idsKey(worldId), emptySet()).orEmpty().toMutableSet()
        ids.add(id)
        prefs(context).edit()
            .putStringSet(idsKey(worldId), ids)
            .putString(nameKey(worldId, id), name)
            .putString(fileKey(worldId, id), fileName)
            .apply()
    }

    fun remove(context: Context, worldId: String?, id: String) {
        // Fichiers du livre : dossier d'extraction d'un livre .zip ("<id>/…", images comprises),
        // sinon le fichier copié à l'ajout ("<id>.md", PDF...).
        prefs(context).getString(fileKey(worldId, id), null)?.let { fileName ->
            val dossier = fileName.substringBefore('/', "")
            when {
                dossier == id -> File(customBooksDir(context), dossier).deleteRecursively()
                '/' !in fileName -> File(customBooksDir(context), fileName).delete()
            }
        }
        val ids = prefs(context).getStringSet(idsKey(worldId), emptySet()).orEmpty().toMutableSet()
        ids.remove(id)
        prefs(context).edit()
            .putStringSet(idsKey(worldId), ids)
            .remove(nameKey(worldId, id))
            .remove(fileKey(worldId, id))
            .apply()
    }
}

/**
 * Normalise une catégorie déclarée dans le `Categories:` d'un `reference.md` d'univers
 * importé (français libre, ex. "objets", "règles") vers une clé d'onglet de bibliothèque
 * (ex. "equipment", "rules") — voir [WorldSelectionScreen] / `allTabKeys` ci-dessus.
 * Une clé déjà au format anglais (ou inconnue) est renvoyée telle quelle.
 */
private val categoryKeyAliases = mapOf(
    "monstres" to "monsters", "monster" to "monsters",
    "sorts" to "spells", "spell" to "spells",
    "regles" to "rules", "règles" to "rules", "rule" to "rules",
    "objets" to "equipment", "equipement" to "equipment", "équipement" to "equipment",
    "glossaire" to "glossary",
    "états" to "etats", "etat" to "etats", "état" to "etats", "conditions" to "etats",
    "espece" to "especes", "espèces" to "especes", "espece" to "especes",
    "historique" to "historiques",
    "don" to "dons",
    "armes magiques" to "armes_magiques", "armes_magique" to "armes_magiques",
    "monture" to "montures", "montures_vehicules" to "montures",
)

private fun normalizeCategoryKey(raw: String): String {
    val key = raw.trim().lowercase()
    return categoryKeyAliases[key] ?: key
}

/** Dossier interne où sont copiés les fichiers des livres personnalisés. */
internal fun customBooksDir(context: Context): File =
    File(context.filesDir, "custom_books").apply { mkdirs() }

/**
 * Copie le contenu d'un fichier choisi via le sélecteur système dans le stockage interne
 * de l'app, sous un nom unique dérivé de [id]. On copie le contenu plutôt que de garder
 * l'URI d'origine : l'autorisation d'accès à une URI "content://" choisie ponctuellement
 * n'est pas garantie de survivre au redémarrage de l'app. Retourne le nom du fichier copié
 * (à passer à [CustomBooksStore.add]), ou null en cas d'échec de lecture.
 */
private fun copyPickedFileToInternalStorage(context: Context, uri: Uri, id: String, originalName: String?): String? {
    val extension = originalName?.substringAfterLast('.', missingDelimiterValue = "md") ?: "md"
    val fileName = "$id.$extension"
    val target = File(customBooksDir(context), fileName)
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        fileName
    } catch (e: Exception) {
        null
    }
}

/** Contenu d'une archive .zip extraite dans [dossier] : le .md du livre (s'il y en a un) et les scénarios. */
private class ArchiveExtraite(val dossier: File, val livre: File?, val scenarios: List<File>)

/**
 * Archive .zip : un livre (.md de bibliothèque accompagné de ses images, ex. un bestiaire
 * illustré, voir [CustomBookImages]) et/ou des scénarios. L'archive est extraite dans
 * `custom_books/<id>/` — les entrées qui tenteraient d'écrire hors de ce dossier ("zip slip")
 * et les métadonnées macOS sont ignorées. Un .md est un scénario s'il est rangé dans un dossier
 * `Scenarios/` (ou `Scénarios/`) ou s'il ne contient aucune entrée `<!-- type: ... -->` ;
 * les autres forment le livre (plusieurs sont réunis en un seul `livre.md`). Null si l'archive
 * est illisible ou ne contient aucun .md.
 */
private fun extraireArchive(context: Context, uri: Uri, id: String): ArchiveExtraite? {
    val dir = File(customBooksDir(context), id).apply { mkdirs() }
    val dirPath = dir.canonicalPath
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                // Compress-Archive (Windows PowerShell 5.1) écrit "images\gobelin.png" : on
                // rétablit le séparateur standard pour recréer les sous-dossiers.
                val name = entry.name.replace('\\', '/')
                val target = File(dir, name)
                val safe = target.canonicalPath.startsWith(dirPath + File.separator)
                if (safe && !entry.isDirectory && !name.endsWith("/") && !name.startsWith("__MACOSX")) {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { out -> zip.copyTo(out) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val mdFiles = dir.walkTopDown()
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .sortedBy { it.path }
            .toList()
        if (mdFiles.isEmpty()) {
            dir.deleteRecursively()
            return null
        }
        val (scenarios, livres) = mdFiles.partition { fichier ->
            val dossiers = fichier.relativeTo(dir).invariantSeparatorsPath.split('/').dropLast(1)
            dossiers.any { it.equals("Scenarios", ignoreCase = true) || it.equals("Scénarios", ignoreCase = true) } ||
                CustomContentParser.parse(fichier.readText()).summary.total == 0
        }
        val livre = when (livres.size) {
            0 -> null
            1 -> livres.first()
            else -> File(dir, "livre.md").apply { writeText(livres.joinToString("\n\n") { it.readText() }) }
        }
        ArchiveExtraite(dir, livre, scenarios)
    } catch (e: Exception) {
        dir.deleteRecursively()
        null
    }
}

/** Ce qu'a apporté l'ajout d'un livre : contenu reconnu, fiches PNJ et scénarios créés. */
internal data class LivreAjoute(
    // Null si l'archive ne contenait que des scénarios (aucun livre ajouté à la bibliothèque).
    val id: String?,
    val fileName: String?,
    val summary: CustomContentSummary?,
    val pnjCrees: Int,
    val scenarios: List<String> = emptyList(),
    val pnjScenarios: Int = 0,
    // Scénarios importés (titre -> id), fiches non-PJ créées et campagne livrée par l'archive
    // (CampagneArchive), que l'outil Import crée ensuite pour relier le tout.
    val scenarioIds: Map<String, String> = emptyMap(),
    val pnjIds: List<String> = emptyList(),
    val campagne: com.jc2.jdrcompagnon.feature_import.CampagneArchive.Contenu? = null,
)

/**
 * Ajoute [uri] (.md, .zip avec images et scénarios, ou tout autre fichier) comme livre
 * personnalisé de [worldId] sous le nom [nom] : copie dans le stockage interne, enregistrement,
 * puis — pour un livre markdown — création des fiches PNJ de ses entrées `<!-- type: pnj -->`
 * (voir [PnjImport]). Les scénarios d'une archive sont importés ensuite (voir [ScenarioImport]),
 * pour que leurs liens retrouvent les monstres, PNJ et objets du livre. Utilisé par l'outil
 * Import. Retourne null si le fichier est illisible.
 */
internal fun ajouterLivrePersonnalise(context: Context, uri: Uri, worldId: String?, nom: String): LivreAjoute? {
    val id = "${System.currentTimeMillis()}"
    val nomFichier = queryDisplayName(context, uri)
    val monde = worldId ?: "donjon_et_dragon"
    if (nomFichier?.endsWith(".zip", ignoreCase = true) != true) {
        val fileName = copyPickedFileToInternalStorage(context, uri, id, nomFichier) ?: return null
        return enregistrerLivre(context, id, fileName, worldId, nom)
    }

    val fichesAvant = com.jc2.jdrcompagnon.ui.GameState.characters.value.map { it.id }.toSet()
    val archive = extraireArchive(context, uri, id) ?: return null
    val booksDir = customBooksDir(context)
    val livre = archive.livre?.let { enregistrerLivre(context, id, it.relativeTo(booksDir).invariantSeparatorsPath, worldId, nom) }
    var pnjScenarios = 0
    val scenarioIds = linkedMapOf<String, String>()
    archive.scenarios.forEach { fichier ->
        val texte = runCatching { fichier.readText() }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
        val (scenario, pnj) = ScenarioImport.importer(context, texte, monde)
        pnjScenarios += pnj
        scenarioIds[scenario.title] = scenario.id
    }
    val campagne = com.jc2.jdrcompagnon.feature_import.CampagneArchive.lire(archive.dossier)
    val pnjIds = com.jc2.jdrcompagnon.ui.GameState.characters.value.filter { it.id !in fichesAvant && it.type != "PJ" }.map { it.id }
    // Les scénarios sont copiés dans l'app (images comprises) : leurs .md extraits ne servent plus.
    if (livre == null) archive.dossier.deleteRecursively() else archive.scenarios.forEach { it.delete() }
    return (livre ?: LivreAjoute(null, null, null, 0)).copy(
        scenarios = scenarioIds.keys.toList(), pnjScenarios = pnjScenarios,
        scenarioIds = scenarioIds, pnjIds = pnjIds, campagne = campagne,
    )
}

/** Enregistre le livre déjà copié ([fileName], relatif à [customBooksDir]) et crée ses fiches PNJ. */
private fun enregistrerLivre(context: Context, id: String, fileName: String, worldId: String?, nom: String): LivreAjoute {
    CustomBooksStore.add(context, worldId, id, nom, fileName)
    SrdRepository.invalidateWorld(worldId)
    if (!fileName.endsWith(".md", ignoreCase = true)) return LivreAjoute(id, fileName, null, 0)

    val content = readCustomBookContent(context, fileName) ?: return LivreAjoute(id, fileName, null, 0)
    val parsed = CustomContentParser.parse(content)
    val booksDir = customBooksDir(context)
    val pnjs = fileName.substringBefore('/', "").ifBlank { null }
        ?.let { CustomBookImages.resolve(parsed.pnjs, File(booksDir, fileName), File(booksDir, it)) }
        ?: parsed.pnjs
    val pnjCrees = PnjImport.creerFiches(context, pnjs, worldId ?: "donjon_et_dragon", nom)
    return LivreAjoute(id, fileName, parsed.summary, pnjCrees)
}

/** Lit le contenu texte d'un livre personnalisé depuis le stockage interne. */
internal fun readCustomBookContent(context: Context, fileName: String): String? =
    try {
        File(customBooksDir(context), fileName).readText()
    } catch (e: Exception) {
        null
    }

/** Nom d'affichage d'un fichier choisi via le sélecteur système (colonne DISPLAY_NAME). */
private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }

/**
 * Extrait le facteur de puissance (FP, ex-"Défi") et les PX d'un monstre depuis la ligne
 * "**FP :** X (Y PX...)" présente dans son bloc de statistiques (SRD 5.2.1). Tolère les
 * deux ordres "Y PX" et "PX Y" rencontrés selon les profils. Retourne null si non trouvé.
 */
private val monsterChallengeRegex =
    Regex("""\*\*FP\s*:?\*\*\s+([^(]+?)\s*\(\s*(?:PX\s*)?([\d][\d\s]*?)\s*(?:PX\s*)?[,;)]""")

fun monsterChallenge(entry: SrdEntry): Pair<String, String>? {
    val match = monsterChallengeRegex.find(entry.rawMarkdown) ?: return null
    return match.groupValues[1].trim() to match.groupValues[2].trim()
}

/**
 * Libellé affiché à droite dans la liste des monstres (ex: "FP 1/4 · 50 PX").
 */
fun monsterChallengeLabel(entry: SrdEntry): String? {
    val (cr, xp) = monsterChallenge(entry) ?: return null
    return "FP $cr · $xp PX"
}

/**
 * Clé de tri numérique pour un niveau de défi ("-" et fractions inclus), afin d'ordonner
 * les options du filtre de façon croissante plutôt qu'alphabétique.
 */
fun challengeSortKey(cr: String): Double = when (cr) {
    "-" -> -1.0
    "1/8" -> 0.125
    "1/4" -> 0.25
    "1/2" -> 0.5
    else -> cr.toDoubleOrNull() ?: 999.0
}

/**
 * Tranches de défi du filtre des monstres (une puce chacune au lieu d'une ligne par FP), alignées
 * sur les paliers de niveau des personnages, avec leurs PX : [max] = FP le plus haut inclus.
 */
internal enum class TrancheDefi(val label: String, val max: Double) {
    MINEURE("FP 0 à ½ · jusqu'à 100 PX", 0.5),
    PALIER_1("FP 1 à 4 · 200 à 1 100 PX", 4.0),
    PALIER_2("FP 5 à 10 · 1 800 à 5 900 PX", 10.0),
    PALIER_3("FP 11 à 16 · 7 200 à 15 000 PX", 16.0),
    PALIER_4("FP 17 et + · 18 000 PX et plus", Double.MAX_VALUE);

    companion object {
        fun de(cr: String): TrancheDefi = challengeSortKey(cr).let { v -> entries.first { v <= it.max } }
    }
}

/** Classes qui ont accès à un sort (« Barde, Clerc, Druide »), sans espaces superflus. */
internal fun classesDuSort(entry: SrdSectionEntry): List<String> =
    entry.classes.split(',').map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Extrait le niveau d'un sort depuis la ligne italique qui suit son titre
 * (ex: "*Evocation de 2ème niveau*" -> "Niveau 2", "*Tour de magie de conjuration*" -> "Mineur").
 * Retourne null si aucun niveau n'est reconnu.
 */
fun spellLevelLabel(entry: SrdEntry): String? {
    val firstItalicLine = entry.rawMarkdown.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("*") && it.endsWith("*") && !it.startsWith("**") }
        ?.trim('*')
        ?.trim()
        ?.lowercase()
        ?: return null
    if ("tour de magie" in firstItalicLine || "cantrip" in firstItalicLine) return "Mineur"
    if ("niveau" !in firstItalicLine && "level" !in firstItalicLine) return null
    val level = Regex("""\d+""").find(firstItalicLine)?.value?.toIntOrNull() ?: return null
    return "Niveau $level"
}

/**
 * Rangée "Filtrer (N) / Réinitialiser" réutilisable au-dessus des listes filtrables.
 */
@Composable
private fun FilterButtonRow(
    activeFilterCount: Int,
    onOpen: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (activeFilterCount == 0) "Filtrer" else "Filtrer ($activeFilterCount)")
        }
        if (activeFilterCount > 0) {
            TextButton(onClick = onReset) {
                Text("Réinitialiser")
            }
        }
    }
}

/**
 * Ligne à cocher réutilisable pour les écrans de filtre (catégorie d'équipement,
 * école de magie...).
 */
@Composable
private fun CheckableFilterRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Spacer(modifier = Modifier.width(4.dp))
        Text(label)
    }
}

/**
 * Écran de bibliothèque SRD avec onglets (Monstres, Sorts, Règles, Équipement, Glossaire).
 *
 * @param currentWorld Le monde actuellement sélectionné
 * @param onBack Action de retour
 * @param initialTab Onglet pré-sélectionné ("monsters" ou "spells"), par défaut "monsters"
 * @param onMonsterClick Callback naviguant vers le détail d'un monstre
 * @param onSpellClick Callback naviguant vers le détail d'un sort
 * @param onEquipmentClick Callback naviguant vers le détail d'un équipement
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    currentWorld: WorldState?,
    onBack: () -> Unit,
    initialTab: String = "monsters",
    onMonsterClick: (String) -> Unit = {},
    onSpellClick: (String) -> Unit = {},
    onEquipmentClick: (EquipmentItem) -> Unit = {},
    onClasseClick: (String) -> Unit = {},
    onEspeceClick: (String) -> Unit = {},
    onHistoriqueClick: (String) -> Unit = {},
    onDonClick: (String) -> Unit = {},
    onArmeArmureMagiqueClick: (String) -> Unit = {},
    onRuleClick: (String) -> Unit = {},
    onGlossaryClick: (String) -> Unit = {},
    onEtatClick: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val allTabs = listOf(
        "Monstres", "Sorts", "Règles", "Équipement", "Glossaire",
        "Classes", "Espèces", "Historiques", "Dons", "Armes magiques", "Montures", "États",
    )
    val allTabIcons = listOf(
        Icons.Filled.Pets,
        Icons.Filled.AutoAwesome,
        Icons.Filled.Gavel,
        Icons.Filled.Shield,
        Icons.AutoMirrored.Filled.MenuBook,
        Icons.Filled.School,
        Icons.Filled.Groups,
        Icons.Filled.History,
        Icons.Filled.Star,
        Icons.Filled.Bolt,
        Icons.Filled.DirectionsCar,
        Icons.Filled.HealthAndSafety,
    )
    val allTabKeys = listOf(
        "monsters", "spells", "rules", "equipment", "glossary",
        "classes", "especes", "historiques", "dons", "armes_magiques", "montures", "etats",
    )
    // Un univers importé peut limiter les onglets affichés via `Categories:` dans son
    // reference.md (voir CustomWorldsRepository/ReferenceMdParser) — ex. Naheulbeuk-like,
    // sans Monstres/Sorts/Classes. Absent ou vide = tous les onglets, comme avant.
    val enabledCategoryKeys = currentWorld?.enabledCategories
        ?.takeIf { it.isNotEmpty() }
        ?.map { normalizeCategoryKey(it) }
        ?.toSet()
    val visibleTabIndices = allTabKeys.indices.filter { index ->
        enabledCategoryKeys == null || allTabKeys[index] in enabledCategoryKeys
    }
    val tabs = visibleTabIndices.map { allTabs[it] }
    val tabIcons = visibleTabIndices.map { allTabIcons[it] }
    val tabKeys = visibleTabIndices.map { allTabKeys[it] }
    // Livres personnalisés ajoutés par l'utilisateur (fichier .md ou autre, couverture et
    // nom choisis depuis l'écran de gestion des livres) ; rechargés à chaque changement de
    // monde ou de réglages (voir plus bas). Déclaré ici, avant `libraryBooks`, qui en a
    // besoin : Kotlin exige qu'une variable locale soit déclarée avant son utilisation.
    var customBooks by remember { mutableStateOf<List<CustomBook>>(emptyList()) }
    val libraryBooks = remember(tabs, customBooks) {
        tabs.mapIndexed { index, label ->
            LibraryBook(label = label, icon = tabIcons[index], tabIndex = index, tabKey = tabKeys[index])
        } + customBooks.mapIndexed { index, book ->
            LibraryBook(
                label = book.name,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                tabIndex = tabs.size + index,
                tabKey = "custom_${book.id}",
            )
        }
    }
    val initialIndex = tabKeys.indexOf(initialTab).coerceAtLeast(0)
    var selectedTab by rememberSaveable { mutableStateOf(initialIndex) }
    // Vue étagère (accueil) par défaut ; si l'appelant demande explicitement un onglet
    // autre que celui par défaut ("monsters"), on considère que c'est un lien direct
    // et on saute l'étagère pour aller droit au contenu.
    var showShelf by rememberSaveable { mutableStateOf(initialTab == "monsters") }
    var hasVisitedShelf by rememberSaveable { mutableStateOf(showShelf) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    // Filtre par catégorie pour l'onglet Équipement : vide = toutes les catégories affichées
    var selectedEquipmentCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par école de magie pour l'onglet Sorts : vide = toutes les écoles affichées
    var selectedSpellLevels by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Classes ayant accès au sort (Barde, Clerc…) : un sort est gardé s'il est accessible à l'une d'elles.
    var selectedSpellClasses by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par niveau de défi pour l'onglet Monstres : vide = tous les défis affichés
    var selectedChallenges by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par terrain (ligne "Environnement:" du SRD) pour l'onglet Monstres : vide = tous
    var selectedMonsterTerrains by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par catégorie pour l'onglet Dons : vide = toutes les catégories affichées
    var selectedDonCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    // Filtre par catégorie pour l'onglet Armes magiques : vide = toutes les catégories affichées
    var selectedArmeMagiqueCategories by rememberSaveable { mutableStateOf(setOf<String>()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()

    // Recherche globale (icône bibliothécaire) : cherche un mot-clé dans le nom ET le
    // contenu de tous les livres à la fois, contrairement à `searchQuery` qui ne filtre
    // que par nom dans le livre actuellement ouvert. Toujours visible sur l'étagère (plus
    // besoin de toucher la bibliothécaire pour la faire apparaître) : jamais remise à
    // false ailleurs dans ce fichier, seul son texte se vide.
    var showGlobalSearch by rememberSaveable { mutableStateOf(true) }
    var globalSearchQuery by rememberSaveable { mutableStateOf("") }
    // Catégorie à laquelle la recherche globale est restreinte ("Monstres"...), null = tout.
    var globalSearchScope by rememberSaveable { mutableStateOf<String?>(null) }
    var globalSearchFocused by remember { mutableStateOf(false) }
    // Bas de la bibliothécaire (px, repère racine) : le contenu qui défile commence sous elle.
    var librarianBottomPx by remember { mutableStateOf(0f) }
    // Bord gauche de la bibliothécaire : les champs de recherche s'arrêtent avant elle (sinon
    // leur croix d'effacement passait sous l'avatar, qui captait le clic).
    var librarianLeftPx by remember { mutableStateOf(-1f) }
    val librarianReserveEnd = run {
        val density = LocalDensity.current
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        if (librarianLeftPx < 0f) 170.dp
        else (screenWidth - with(density) { librarianLeftPx.toDp() } + 8.dp).coerceAtLeast(16.dp)
    }
    // Cible de défilement pour les livres à page unique (Règles, Montures) : initialisée
    // par le lien profond éventuel, puis mise à jour quand on clique un résultat de
    // recherche globale pointant vers une section de l'un de ces deux livres.
    var activeMontureSectionTarget by remember { mutableStateOf<String?>(null) }

    // Écran de gestion des livres, désormais ouvert en cliquant la bibliothécaire :
    // couverture personnalisée + disponibilité aux joueurs pour chaque livre, réglages
    // persistés dans LibraryBookSettingsStore, plus l'ajout de livres personnalisés
    // (CustomBooksStore). `bookSettingsVersion` est incrémenté à chaque modification pour
    // forcer l'étagère à relire les réglages (une SharedPreferences modifiée ne recompose
    // rien toute seule).
    var showBookSettings by rememberSaveable { mutableStateOf(false) }
    var bookSettingsVersion by remember { mutableStateOf(0) }

    // Livre personnalisé actuellement affiché, le cas échéant (`customBooks` lui-même est
    // déclaré plus haut, avant `libraryBooks` qui en a besoin) : pilote l'affichage de son
    // contenu, en parallèle du mécanisme `selectedTab`/`tabKeys` réservé aux livres intégrés.
    var selectedCustomBookId by rememberSaveable { mutableStateOf<String?>(null) }
    // Fiche ouverte depuis un livre personnalisé sans marqueurs `<!-- type: -->` (texte libre
    // découpé par titres) : affichée sur place, faute d'écran de détail dédié ailleurs.
    var openedCustomEntry by remember { mutableStateOf<SrdSectionEntry?>(null) }

    // États de chargement
    var monsters by remember { mutableStateOf<List<SrdEntry>?>(null) }
    var spells by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var equipment by remember { mutableStateOf<List<EquipmentItem>?>(null) }
    // Règles et glossaire découpés en entrées (une règle = une fiche), voir ReferenceEntryParser.
    var ruleEntries by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var glossaryEntries by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    // Livre États : généré depuis le catalogue des états (voir LivreEtats).
    var etatsEntries by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var classes by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var especes by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var historiques by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var dons by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var armesMagiques by remember { mutableStateOf<List<SrdSectionEntry>?>(null) }
    var montures by remember { mutableStateOf<List<SrdDocSection>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // S'assurer que les données sont rechargées quand le monde change
    val worldIdKey = currentWorld?.id

    // Incrémenté par "Forcer la mise à jour" (écran de gestion des livres) : ajouté aux
    // clés du LaunchedEffect de chargement ci-dessous pour le forcer à se relancer même
    // si l'onglet et le monde n'ont pas changé — nécessaire puisque remettre les données
    // à null ne suffit pas à lui seul à redéclencher un LaunchedEffect déjà stable sur
    // ses clés actuelles.
    var srdRefreshTrigger by remember { mutableStateOf(0) }

    // Force une recopie des fichiers SRD depuis les assets (voir
    // SrdRepository.forceRefreshFromAssets) et vide les données déjà chargées en mémoire
    // dans cet écran, pour que l'onglet actuellement ouvert se recharge avec le contenu
    // frais dès la fin de cette fonction (via srdRefreshTrigger).
    val onForceSrdRefresh: () -> Unit = {
        SrdRepository.forceRefreshFromAssets(context)
        monsters = null
        spells = null
        equipment = null
        ruleEntries = null
        glossaryEntries = null
        etatsEntries = null
        classes = null
        especes = null
        historiques = null
        dons = null
        armesMagiques = null
        montures = null
        srdRefreshTrigger++
    }

    // Recharge la liste des livres personnalisés à chaque changement de monde et à chaque
    // ajout/suppression (bookSettingsVersion, incrémenté par onSettingChanged côté écran
    // de gestion des livres).
    // Livres d'une campagne non sélectionnée masqués (PorteeCampagne) ; versionLivres change
    // quand on sélectionne une autre campagne : liste et contenu déjà chargé sont rafraîchis.
    val versionLivres by com.jc2.jdrcompagnon.ui.PorteeCampagne.versionLivres.collectAsState()
    LaunchedEffect(worldIdKey, bookSettingsVersion, versionLivres) {
        customBooks = CustomBooksStore.list(context, worldIdKey)
            .filter { com.jc2.jdrcompagnon.ui.PorteeCampagne.livreVisible(it.id, worldIdKey) }
    }
    var versionLivresChargee by remember { mutableStateOf(versionLivres) }
    LaunchedEffect(versionLivres) {
        if (versionLivres == versionLivresChargee) return@LaunchedEffect
        versionLivresChargee = versionLivres
        monsters = null
        spells = null
        equipment = null
        ruleEntries = null
        glossaryEntries = null
        etatsEntries = null
        classes = null
        especes = null
        historiques = null
        dons = null
        armesMagiques = null
        montures = null
        srdRefreshTrigger++
    }

    // Chargement des données selon l'onglet sélectionné ET du monde (+ srdRefreshTrigger,
    // voir sa déclaration ci-dessus, pour permettre un rechargement forcé sans changer
    // d'onglet ni de monde)
    LaunchedEffect(selectedTab, worldIdKey, srdRefreshTrigger) {
        val worldId = currentWorld?.id
        // Réinitialiser les données monde-dépendantes quand le monde change
        if (worldIdKey == null || worldId != worldIdKey) {
            monsters = null
            spells = null
            equipment = null
            ruleEntries = null
            glossaryEntries = null
            etatsEntries = null
            classes = null
            especes = null
            historiques = null
            dons = null
            armesMagiques = null
            montures = null
            selectedEquipmentCategories = emptySet()
            selectedSpellLevels = emptySet()
            selectedSpellClasses = emptySet()
            selectedChallenges = emptySet()
            selectedMonsterTerrains = emptySet()
            selectedDonCategories = emptySet()
            selectedArmeMagiqueCategories = emptySet()
            activeMontureSectionTarget = null
        }
        when (tabKeys[selectedTab]) {
            "monsters" -> {
                if (monsters == null) {
                    try {
                        monsters = SrdRepository.loadMonsters(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "spells" -> {
                if (spells == null) {
                    try {
                        spells = SrdRepository.loadSpellsIndex(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "rules" -> {
                if (ruleEntries == null) {
                    try {
                        ruleEntries = SrdRepository.loadRuleEntries(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "equipment" -> {
                if (equipment == null) {
                    try {
                        equipment = SrdRepository.loadEquipmentList(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "glossary" -> {
                if (glossaryEntries == null) {
                    try {
                        glossaryEntries = SrdRepository.loadGlossaryEntries(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "etats" -> {
                if (etatsEntries == null) {
                    try {
                        etatsEntries = SrdRepository.loadEtats(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "classes" -> {
                if (classes == null) {
                    try {
                        classes = SrdRepository.loadClasses(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "especes" -> {
                if (especes == null) {
                    try {
                        especes = SrdRepository.loadEspeces(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "historiques" -> {
                if (historiques == null) {
                    try {
                        historiques = SrdRepository.loadHistoriques(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "dons" -> {
                if (dons == null) {
                    try {
                        dons = SrdRepository.loadDons(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "armes_magiques" -> {
                if (armesMagiques == null) {
                    try {
                        armesMagiques = SrdRepository.loadArmesArmuresMagiques(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
            "montures" -> {
                if (montures == null) {
                    try {
                        montures = SrdRepository.loadMonturesVehicules(context, worldId)
                    } catch (e: Exception) {
                        loadError = "Erreur de chargement : ${e.message}"
                    }
                }
            }
        }
    }

    // Chargement de TOUTES les catégories dès l'ouverture de la recherche globale (et
    // non plus seulement celle de l'onglet actif), pour pouvoir chercher un mot-clé
    // dans tous les livres à la fois. Chaque catégorie déjà chargée (ex. via l'onglet
    // actif) n'est pas rechargée.
    LaunchedEffect(showGlobalSearch, worldIdKey) {
        if (!showGlobalSearch) return@LaunchedEffect
        val worldId = currentWorld?.id
        try {
            if (monsters == null) monsters = SrdRepository.loadMonsters(context, worldId)
            if (spells == null) spells = SrdRepository.loadSpellsIndex(context, worldId)
            if (ruleEntries == null) ruleEntries = SrdRepository.loadRuleEntries(context, worldId)
            if (equipment == null) equipment = SrdRepository.loadEquipmentList(context, worldId)
            if (glossaryEntries == null) glossaryEntries = SrdRepository.loadGlossaryEntries(context, worldId)
            if (etatsEntries == null) etatsEntries = SrdRepository.loadEtats(context, worldId)
            if (classes == null) classes = SrdRepository.loadClasses(context, worldId)
            if (especes == null) especes = SrdRepository.loadEspeces(context, worldId)
            if (historiques == null) historiques = SrdRepository.loadHistoriques(context, worldId)
            if (dons == null) dons = SrdRepository.loadDons(context, worldId)
            if (armesMagiques == null) armesMagiques = SrdRepository.loadArmesArmuresMagiques(context, worldId)
            if (montures == null) montures = SrdRepository.loadMonturesVehicules(context, worldId)
        } catch (e: Exception) {
            loadError = "Erreur de chargement : ${e.message}"
        }
    }

    // Résultats de la recherche globale : cherche `globalSearchQuery` dans le nom ET le
    // contenu de chaque entrée, toutes catégories confondues. `onSelect` sait exactement
    // où naviguer pour chaque type de livre.
    val globalSearchResults = remember(
        globalSearchQuery, globalSearchScope, monsters, spells, ruleEntries, equipment, glossaryEntries, etatsEntries,
        classes, especes, historiques, dons, armesMagiques, montures,
    ) {
        val query = globalSearchQuery.trim()
        if (query.length < 2) return@remember emptyList<GlobalSearchResult>()

        buildList {
            monsters?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Monstres", score) {
                        selectedTab = tabKeys.indexOf("monsters")
                        showShelf = false
                        globalSearchQuery = ""
                        onMonsterClick(entry.name)
                    })
                }
            }
            spells?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Sorts", score) {
                        selectedTab = tabKeys.indexOf("spells")
                        showShelf = false
                        globalSearchQuery = ""
                        onSpellClick(entry.name)
                    })
                }
            }
            ruleEntries?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Règles", score) {
                        selectedTab = tabKeys.indexOf("rules")
                        showShelf = false
                        globalSearchQuery = ""
                        onRuleClick(entry.name)
                    })
                }
            }
            equipment?.forEach { item ->
                LibrarySearch.score(query, item.name, item.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(item.name, "Équipement", score) {
                        selectedTab = tabKeys.indexOf("equipment")
                        showShelf = false
                        globalSearchQuery = ""
                        onEquipmentClick(item)
                    })
                }
            }
            glossaryEntries?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Glossaire", score) {
                        selectedTab = tabKeys.indexOf("glossary")
                        showShelf = false
                        globalSearchQuery = ""
                        onGlossaryClick(entry.name)
                    })
                }
            }
            etatsEntries?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "États", score) {
                        selectedTab = tabKeys.indexOf("etats")
                        showShelf = false
                        globalSearchQuery = ""
                        onEtatClick(entry.name)
                    })
                }
            }
            classes?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Classes", score) {
                        selectedTab = tabKeys.indexOf("classes")
                        showShelf = false
                        globalSearchQuery = ""
                        onClasseClick(entry.name)
                    })
                }
            }
            especes?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Espèces", score) {
                        selectedTab = tabKeys.indexOf("especes")
                        showShelf = false
                        globalSearchQuery = ""
                        onEspeceClick(entry.name)
                    })
                }
            }
            historiques?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Historiques", score) {
                        selectedTab = tabKeys.indexOf("historiques")
                        showShelf = false
                        globalSearchQuery = ""
                        onHistoriqueClick(entry.name)
                    })
                }
            }
            dons?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Dons", score) {
                        selectedTab = tabKeys.indexOf("dons")
                        showShelf = false
                        globalSearchQuery = ""
                        onDonClick(entry.name)
                    })
                }
            }
            armesMagiques?.forEach { entry ->
                LibrarySearch.score(query, entry.name, entry.rawMarkdown)?.let { score ->
                    add(GlobalSearchResult(entry.name, "Armes magiques", score) {
                        selectedTab = tabKeys.indexOf("armes_magiques")
                        showShelf = false
                        globalSearchQuery = ""
                        onArmeArmureMagiqueClick(entry.name)
                    })
                }
            }
            montures?.forEach { section ->
                LibrarySearch.score(query, section.title, section.content)?.let { score ->
                    add(GlobalSearchResult(section.title, "Montures", score) {
                        selectedTab = tabKeys.indexOf("montures")
                        showShelf = false
                        globalSearchQuery = ""
                        activeMontureSectionTarget = section.title
                    })
                }
            }
        }.filter { globalSearchScope == null || it.categoryLabel == globalSearchScope }
            .sortedByDescending { it.score }
    }

    // Fenêtre de catégories de la recherche globale : nombre d'entrées de chaque livre visible
    // (null = encore en chargement). Un clic sur une catégorie restreint la recherche à elle
    // (globalSearchScope), en utilisant les mêmes libellés que GlobalSearchResult.categoryLabel.
    val globalSearchCategories: List<Pair<String, Int?>> = listOf(
        Triple("monsters", "Monstres", monsters?.size),
        Triple("spells", "Sorts", spells?.size),
        Triple("rules", "Règles", ruleEntries?.size),
        Triple("equipment", "Équipement", equipment?.size),
        Triple("glossary", "Glossaire", glossaryEntries?.size),
        Triple("etats", "États", etatsEntries?.size),
        Triple("classes", "Classes", classes?.size),
        Triple("especes", "Espèces", especes?.size),
        Triple("historiques", "Historiques", historiques?.size),
        Triple("dons", "Dons", dons?.size),
        Triple("armes_magiques", "Armes magiques", armesMagiques?.size),
        Triple("montures", "Montures", montures?.size),
    ).filter { (key, _, count) -> key in tabKeys && count != 0 }
        .map { (_, label, count) -> label to count }

    // Fond dédié à la bibliothèque, fourni en asset (comme ic_acceuil dans AppBottomBar) ;
    // repli silencieux sur le fond sombre uniforme (ForcedDarkPalette) si le fichier est
    // absent, pour ne jamais casser l'écran.
    val bibliothequeBackground = remember {
        runCatching {
            context.assets.open("dnd/fond_ecran/fe_bliblio.png").use { android.graphics.BitmapFactory.decodeStream(it) }
                ?.asImageBitmap()
        }.getOrNull()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (bibliothequeBackground != null) {
            Image(
                bitmap = bibliothequeBackground,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        // Retour système (bouton/geste) : même logique que la flèche de la barre du haut —
        // depuis un livre ou les réglages on revient à l'étagère de tous les livres, au lieu
        // de quitter la Bibliothèque (ce qui ramenait à l'accueil). Seule l'étagère sans
        // recherche en cours laisse le retour à la navigation.
        // Bibliothèque ouverte directement sur un livre (ex. lien #rule: d'un scénario) sans être
        // passée par l'étagère : le retour ramène alors à l'écran d'origine.
        LaunchedEffect(showShelf) { if (showShelf) hasVisitedShelf = true }
        BackHandler(enabled = showBookSettings || (!showShelf && hasVisitedShelf) || (showShelf && globalSearchQuery.isNotBlank())) {
            when {
                showBookSettings -> showBookSettings = false
                showShelf -> globalSearchQuery = ""
                openedCustomEntry != null -> openedCustomEntry = null
                else -> {
                    selectedCustomBookId = null
                    showShelf = true
                }
            }
        }
        // Fenêtre des catégories de recherche ouverte : le retour la referme d'abord (prioritaire
        // sur le BackHandler ci-dessus, déclaré avant).
        val focusManager = LocalFocusManager.current
        BackHandler(enabled = showShelf && globalSearchFocused) { focusManager.clearFocus() }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when {
                                    showBookSettings -> "Réglages des livres"
                                    showShelf -> "Bibliothèque"
                                    openedCustomEntry != null -> openedCustomEntry?.name.orEmpty()
                                    selectedCustomBookId != null ->
                                        customBooks.find { it.id == selectedCustomBookId }?.name.orEmpty()
                                    else -> tabs.getOrNull(selectedTab).orEmpty()
                                },
                                fontWeight = FontWeight.Bold,
                            )
                            currentWorld?.name?.let { worldName ->
                                Text(
                                    text = worldName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            when {
                                showBookSettings -> showBookSettings = false
                                showShelf && globalSearchQuery.isNotBlank() -> globalSearchQuery = ""
                                showShelf -> onBack()
                                openedCustomEntry != null -> openedCustomEntry = null
                                !hasVisitedShelf -> onBack()
                                else -> {
                                    selectedCustomBookId = null
                                    showShelf = true
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            containerColor = if (bibliothequeBackground != null) Color.Transparent else ForcedDarkPalette.Background,
        ) { innerPadding ->
            if (!SrdRepository.isLibraryAvailable(currentWorld?.id)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune bibliothèque disponible pour ${currentWorld?.name ?: "cet univers"}.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                return@Scaffold
            }

            if (showShelf) {
                val trimmedGlobalQuery = globalSearchQuery.trim()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showBookSettings) {
                        BelowLibrarian(librarianBottomPx) {
                            LibraryBookSettingsScreen(
                                books = libraryBooks,
                                worldId = currentWorld?.id,
                                onSettingChanged = { bookSettingsVersion++ },
                                onForceRefresh = onForceSrdRefresh,
                            )
                        }
                    } else {
                        if (showGlobalSearch) {
                            // Réserve de l'espace à droite (padding end) pour que le champ ne
                            // passe jamais sous la bibliothécaire, superposée en absolu par-dessus
                            // tout l'écran (voir plus bas). La roue crantée qui ouvrait autrefois
                            // les réglages des livres est partie : c'est la bibliothécaire elle-même
                            // qui les ouvre désormais.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = librarianReserveEnd, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = globalSearchQuery,
                                    onValueChange = { globalSearchQuery = it },
                                    placeholder = {
                                        Text(globalSearchScope?.let { "Rechercher dans : $it" } ?: "Comment puis-je vous aider ?")
                                    },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                                    trailingIcon = {
                                        if (globalSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { globalSearchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Effacer la recherche")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { globalSearchFocused = it.isFocused },
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = librarySearchFieldColors(),
                                )
                            }
                            // Catégorie choisie : rappel sous le champ, avec une croix pour
                            // revenir à une recherche dans toute la bibliothèque.
                            globalSearchScope?.let { scope ->
                                InputChip(
                                    selected = true,
                                    onClick = { globalSearchScope = null },
                                    label = { Text("Dans : $scope", color = Color.White) },
                                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Chercher partout", tint = Color.White, modifier = Modifier.size(16.dp)) },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                    ),
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                        }

                        if (showGlobalSearch && globalSearchFocused && trimmedGlobalQuery.length < 2) {
                            // Fenêtre ouverte au clic sur la recherche : nombre d'entrées par
                            // catégorie ; un clic restreint la recherche à cette catégorie.
                            BelowLibrarian(librarianBottomPx) {
                                GlobalSearchCategoryPanel(
                                    categories = globalSearchCategories,
                                    selected = globalSearchScope,
                                    onSelect = { globalSearchScope = it },
                                )
                            }
                        } else if (showGlobalSearch && trimmedGlobalQuery.length >= 2) {
                            BelowLibrarian(librarianBottomPx) {
                            if (globalSearchResults.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                ) {
                                    Text(
                                        text = "Aucun résultat pour « $trimmedGlobalQuery ».",
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(24.dp),
                                    )
                                }
                            } else {
                                // Une case par résultat (fond translucide arrondi comme les autres
                                // listes de l'app), texte en blanc pour rester lisible sur le fond.
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    items(
                                        globalSearchResults,
                                        key = { it.categoryLabel + "_" + it.entryName },
                                    ) { result ->
                                        Surface(
                                            onClick = result.onSelect,
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                            ) {
                                                Text(
                                                    text = result.entryName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                )
                                                Text(
                                                    text = result.categoryLabel,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            }
                        } else {
                            BelowLibrarian(librarianBottomPx) {
                                LibraryBookshelf(
                                    books = libraryBooks,
                                    modifier = Modifier.fillMaxSize(),
                                    worldId = currentWorld?.id,
                                    settingsVersion = bookSettingsVersion,
                                    onBookClick = { index ->
                                        if (index < tabs.size) {
                                            selectedTab = index
                                            selectedCustomBookId = null
                                        } else {
                                            selectedCustomBookId = customBooks.getOrNull(index - tabs.size)?.id
                                        }
                                        openedCustomEntry = null
                                        searchQuery = ""
                                        showShelf = false
                                    },
                                )
                            }
                        }
                    }
                }
                return@Scaffold
            }

            if (selectedCustomBookId != null) {
                val book = customBooks.find { it.id == selectedCustomBookId }
                val content = remember(book?.fileName) {
                    book?.fileName?.let { readCustomBookContent(context, it) }
                }
                // Même présentation que les livres intégrés : une liste d'entrées cliquables
                // groupées par type, chacune ouvrant sa fiche (plutôt qu'un long texte brut).
                val rows = remember(content) { content?.let { customBookRows(it) }.orEmpty() }
                val rowsByEntry = remember(rows) { rows.associateBy { it.entry } }
                val openedEntry = openedCustomEntry
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    if (openedEntry == null && rows.isNotEmpty()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Rechercher...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = librarianReserveEnd, top = 8.dp, bottom = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = librarySearchFieldColors(),
                        )
                    }
                    LibrarianClearance(librarianBottomPx)
                    when {
                        book == null -> CustomBookMessage("Ce livre n'existe plus.")
                        content == null -> CustomBookMessage("Impossible de lire le contenu de ce fichier.")
                        openedEntry != null -> CustomBookEntryCard(openedEntry.category, openedEntry.rawMarkdown)
                        // Aucun titre exploitable : le texte entier dans une seule fiche.
                        rows.isEmpty() -> CustomBookEntryCard("", content)
                        else -> SectionEntryByCategoryList(
                            entries = rows.map { it.entry }
                                .filter { searchQuery.isBlank() || LibrarySearch.matchesName(searchQuery, it.name) },
                            onItemClick = { entry ->
                                val row = rowsByEntry[entry]
                                when (row?.kind) {
                                    CustomBookRowKind.MONSTRE -> onMonsterClick(entry.name)
                                    CustomBookRowKind.SORT -> onSpellClick(entry.name)
                                    CustomBookRowKind.OBJET -> row.equipment?.let(onEquipmentClick)
                                    CustomBookRowKind.CLASSE -> onClasseClick(entry.name)
                                    CustomBookRowKind.ESPECE -> onEspeceClick(entry.name)
                                    CustomBookRowKind.HISTORIQUE -> onHistoriqueClick(entry.name)
                                    CustomBookRowKind.DON -> onDonClick(entry.name)
                                    CustomBookRowKind.REGLE -> onRuleClick(entry.name)
                                    CustomBookRowKind.LIBRE, null -> openedCustomEntry = entry
                                }
                            },
                            subtitle = { rowsByEntry[it]?.subtitle.orEmpty() },
                            keepFileOrder = rows.all { it.kind == CustomBookRowKind.LIBRE },
                        )
                    }
                }
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Barre de recherche (marge de droite réservée pour la bibliothécaire, qui
                // flotte par-dessus tous les écrans de la bibliothèque, pas seulement l'étagère)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Rechercher") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = librarianReserveEnd, top = 8.dp, bottom = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = librarySearchFieldColors(),
                )
                // Filtres et liste commencent sous la bibliothécaire (en-têtes de lettre, etc.).
                LibrarianClearance(librarianBottomPx)

                // Le bandeau d'onglets a été abandonné : la navigation entre catégories
                // se fait uniquement via l'étagère (retour avec la flèche du haut).

                // Bouton de filtre, uniquement pour l'onglet Équipement
                if (tabKeys[selectedTab] == "equipment") {
                    val availableCategories = equipment?.map { it.category }?.distinct().orEmpty()
                    if (availableCategories.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedEquipmentCategories.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedEquipmentCategories = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Monstres
                if (tabKeys[selectedTab] == "monsters") {
                    val availableChallenges = monsters?.mapNotNull { monsterChallenge(it)?.first }?.distinct().orEmpty()
                    if (availableChallenges.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedChallenges.size + selectedMonsterTerrains.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedChallenges = emptySet(); selectedMonsterTerrains = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Sorts
                if (tabKeys[selectedTab] == "spells") {
                    val availableSpellLevels = spells?.map { it.category }?.distinct().orEmpty()
                    if (availableSpellLevels.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedSpellLevels.size + selectedSpellClasses.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedSpellLevels = emptySet(); selectedSpellClasses = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Dons
                if (tabKeys[selectedTab] == "dons") {
                    val availableDonCategories = dons?.map { it.category }?.distinct().orEmpty()
                    if (availableDonCategories.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedDonCategories.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedDonCategories = emptySet() },
                        )
                    }
                }

                // Bouton de filtre, uniquement pour l'onglet Armes magiques
                if (tabKeys[selectedTab] == "armes_magiques") {
                    val availableArmeCategories = armesMagiques?.map { it.category }?.distinct().orEmpty()
                    if (availableArmeCategories.isNotEmpty()) {
                        FilterButtonRow(
                            activeFilterCount = selectedArmeMagiqueCategories.size,
                            onOpen = { showFilterSheet = true },
                            onReset = { selectedArmeMagiqueCategories = emptySet() },
                        )
                    }
                }

                // Contenu selon l'onglet
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (tabKeys[selectedTab]) {
                        "monsters" -> {
                            val monsterList = monsters
                            if (monsterList == null) {
                                LoadingBox()
                            } else {
                                val filtered = if (selectedChallenges.isEmpty()) {
                                    monsterList
                                } else {
                                    // selectedChallenges contient des noms de tranches (TrancheDefi).
                                    monsterList.filter { m -> monsterChallenge(m)?.first?.let { TrancheDefi.de(it).name } in selectedChallenges }
                                }.let { parDefi ->
                                    if (selectedMonsterTerrains.isEmpty()) parDefi
                                    else parDefi.filter { it.rencontrableDans(selectedMonsterTerrains) }
                                }
                                val bySearch = if (searchQuery.isBlank()) {
                                    filtered
                                } else {
                                    filtered.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                AlphabeticalEntryList(
                                    entries = bySearch,
                                    onItemClick = { onMonsterClick(it.name) },
                                    trailingLabel = { monsterChallengeLabel(it) },
                                )
                            }
                        }
                        "spells" -> {
                            val spellList = spells
                            if (spellList == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    spellList
                                } else {
                                    spellList.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                val filtered = bySearch
                                    .filter { selectedSpellLevels.isEmpty() || it.category in selectedSpellLevels }
                                    .filter { s -> selectedSpellClasses.isEmpty() || classesDuSort(s).any { it in selectedSpellClasses } }
                                SectionEntryByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onSpellClick(it.name) },
                                    subtitle = { it.rawMarkdown },
                                    secondaryText = { entry -> entry.classes.ifBlank { null }?.let { "Classes : $it" } },
                                )
                            }
                        }
                        // Règles et glossaire : une entrée par règle, groupées par section dans
                        // l'ordre du livre, chacune ouvrant sa fiche (comme l'équipement).
                        "rules" -> {
                            val list = ruleEntries
                            if (list == null) {
                                LoadingBox()
                            } else {
                                SectionEntryByCategoryList(
                                    entries = list.filter { searchQuery.isBlank() || LibrarySearch.matchesName(searchQuery, it.name) },
                                    onItemClick = { onRuleClick(it.name) },
                                    keepFileOrder = true,
                                )
                            }
                        }
                        "equipment" -> {
                            val equipmentList = equipment
                            if (equipmentList == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    equipmentList
                                } else {
                                    equipmentList.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                val filtered = if (selectedEquipmentCategories.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedEquipmentCategories }
                                }
                                EquipmentByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onEquipmentClick(it) }
                                )
                            }
                        }
                        "glossary" -> {
                            val list = glossaryEntries
                            if (list == null) {
                                LoadingBox()
                            } else {
                                SectionEntryByCategoryList(
                                    entries = list.filter { searchQuery.isBlank() || LibrarySearch.matchesName(searchQuery, it.name) },
                                    onItemClick = { onGlossaryClick(it.name) },
                                    keepFileOrder = true,
                                )
                            }
                        }
                        "etats" -> {
                            val list = etatsEntries
                            if (list == null) {
                                LoadingBox()
                            } else {
                                SectionEntryByCategoryList(
                                    entries = list.filter { searchQuery.isBlank() || LibrarySearch.matchesName(searchQuery, it.name) },
                                    onItemClick = { onEtatClick(it.name) },
                                    keepFileOrder = true,
                                )
                            }
                        }
                        "classes" -> {
                            val list = classes
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                SectionEntryAlphabeticalList(
                                    entries = bySearch,
                                    onItemClick = { onClasseClick(it.name) },
                                )
                            }
                        }
                        "especes" -> {
                            val list = especes
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                SectionEntryAlphabeticalList(
                                    entries = bySearch,
                                    onItemClick = { onEspeceClick(it.name) },
                                )
                            }
                        }
                        "historiques" -> {
                            val list = historiques
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                SectionEntryAlphabeticalList(
                                    entries = bySearch,
                                    onItemClick = { onHistoriqueClick(it.name) },
                                )
                            }
                        }
                        "dons" -> {
                            val list = dons
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                val filtered = if (selectedDonCategories.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedDonCategories }
                                }
                                SectionEntryByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onDonClick(it.name) },
                                )
                            }
                        }
                        "armes_magiques" -> {
                            val list = armesMagiques
                            if (list == null) {
                                LoadingBox()
                            } else {
                                val bySearch = if (searchQuery.isBlank()) {
                                    list
                                } else {
                                    list.filter { LibrarySearch.matchesName(searchQuery, it.name) }
                                }
                                val filtered = if (selectedArmeMagiqueCategories.isEmpty()) {
                                    bySearch
                                } else {
                                    bySearch.filter { it.category in selectedArmeMagiqueCategories }
                                }
                                SectionEntryByCategoryList(
                                    entries = filtered,
                                    onItemClick = { onArmeArmureMagiqueClick(it.name) },
                                )
                            }
                        }
                        "montures" -> {
                            val sections = montures
                            if (sections == null) {
                                LoadingBox()
                            } else {
                                DocSectionTocScreen(
                                    sections = sections,
                                    initialSectionTitle = activeMontureSectionTarget,
                                )
                            }
                        }
                    }

                    loadError?.let { error ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (showFilterSheet) {
                val activeTabKey = tabKeys[selectedTab]
                val availableCategories = equipment?.map { it.category }?.distinct().orEmpty()
                val availableSpellLevels = spells?.map { it.category }?.distinct().orEmpty()
                // Seules les tranches et classes présentes dans le livre du monde sont proposées.
                val availableTranches = monsters
                    ?.mapNotNull { monsterChallenge(it)?.first?.let(TrancheDefi::de) }
                    ?.distinct()
                    ?.sortedBy { it.ordinal }
                    .orEmpty()
                val availableSpellClasses = spells?.flatMap(::classesDuSort)?.distinct()?.sorted().orEmpty()
                val availableDonCategories = dons?.map { it.category }?.distinct().orEmpty()
                val availableArmeCategories = armesMagiques?.map { it.category }?.distinct().orEmpty()
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = filterSheetState,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = when (activeTabKey) {
                                "spells" -> "Filtrer les sorts"
                                "monsters" -> "Filtrer les monstres"
                                "dons" -> "Filtrer les dons"
                                "armes_magiques" -> "Filtrer les armes magiques"
                                else -> "Filtrer l'équipement"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        when (activeTabKey) {
                            "spells" -> {
                                // Puces plutôt qu'une ligne par option : tout tient sur un écran.
                                Text(
                                    text = "Classe",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    availableSpellClasses.forEach { classe ->
                                        FilterChip(
                                            selected = classe in selectedSpellClasses,
                                            onClick = {
                                                selectedSpellClasses = if (classe in selectedSpellClasses) selectedSpellClasses - classe else selectedSpellClasses + classe
                                            },
                                            label = { Text(classe) },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Niveau",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    availableSpellLevels.forEach { level ->
                                        FilterChip(
                                            selected = level in selectedSpellLevels,
                                            onClick = {
                                                selectedSpellLevels = if (level in selectedSpellLevels) selectedSpellLevels - level else selectedSpellLevels + level
                                            },
                                            label = { Text(level) },
                                        )
                                    }
                                }
                            }
                            "monsters" -> {
                                // Cinq tranches de défi (FP et PX) au lieu d'une ligne par FP.
                                Text(
                                    text = "Niveau de défi",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    availableTranches.forEach { tranche ->
                                        FilterChip(
                                            selected = tranche.name in selectedChallenges,
                                            onClick = {
                                                selectedChallenges = if (tranche.name in selectedChallenges) selectedChallenges - tranche.name else selectedChallenges + tranche.name
                                            },
                                            label = { Text(tranche.label) },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Environnement",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TERRAINS_SRD.forEach { terrain ->
                                        FilterChip(
                                            selected = terrain in selectedMonsterTerrains,
                                            onClick = {
                                                selectedMonsterTerrains = if (terrain in selectedMonsterTerrains) selectedMonsterTerrains - terrain else selectedMonsterTerrains + terrain
                                            },
                                            label = { Text(terrain) },
                                        )
                                    }
                                }
                            }
                            "dons" -> {
                                Text(
                                    text = "Catégorie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableDonCategories.forEach { category ->
                                    CheckableFilterRow(
                                        label = category,
                                        checked = category in selectedDonCategories,
                                        onToggle = { checked ->
                                            selectedDonCategories = if (checked) {
                                                selectedDonCategories + category
                                            } else {
                                                selectedDonCategories - category
                                            }
                                        },
                                    )
                                }
                            }
                            "armes_magiques" -> {
                                Text(
                                    text = "Catégorie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableArmeCategories.forEach { category ->
                                    CheckableFilterRow(
                                        label = category,
                                        checked = category in selectedArmeMagiqueCategories,
                                        onToggle = { checked ->
                                            selectedArmeMagiqueCategories = if (checked) {
                                                selectedArmeMagiqueCategories + category
                                            } else {
                                                selectedArmeMagiqueCategories - category
                                            }
                                        },
                                    )
                                }
                            }
                            else -> {
                                Text(
                                    text = "Catégorie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                availableCategories.forEach { category ->
                                    CheckableFilterRow(
                                        label = category,
                                        checked = category in selectedEquipmentCategories,
                                        onToggle = { checked ->
                                            selectedEquipmentCategories = if (checked) {
                                                selectedEquipmentCategories + category
                                            } else {
                                                selectedEquipmentCategories - category
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = {
                                when (activeTabKey) {
                                    "spells" -> { selectedSpellLevels = emptySet(); selectedSpellClasses = emptySet() }
                                    "monsters" -> { selectedChallenges = emptySet(); selectedMonsterTerrains = emptySet() }
                                    "dons" -> selectedDonCategories = emptySet()
                                    "armes_magiques" -> selectedArmeMagiqueCategories = emptySet()
                                    else -> selectedEquipmentCategories = emptySet()
                                }
                            }) {
                                Text("Réinitialiser")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { showFilterSheet = false }) {
                                Text("Appliquer")
                            }
                        }
                    }
                }
            }
        }

        // La bibliothécaire : superposée par-dessus tout l'écran (et non dans le TopAppBar,
        // dont la hauteur fixe rognait toute icône plus grande que la barre elle-même).
        // Toujours accessible, ouvre l'écran de gestion des livres (couverture, disponibilité
        // aux joueurs, ajout d'un livre personnalisé) et ramène à l'étagère. La recherche
        // globale, elle, est désormais toujours visible en haut de l'étagère (plus besoin de
        // la bibliothécaire pour l'ouvrir) — padding du haut encore augmenté pour que la tête
        // ne soit plus du tout rognée en haut de l'écran (au-dessus de la zone sûre, sous la
        // barre de statut), taille remontée à 121dp (+10% par rapport aux 110dp précédents).
        Image(
            painter = painterResource(R.drawable.av_bliblio),
            contentDescription = "Réglages des livres",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 50.dp, end = 1.dp)
                .size(161.dp)
                .onGloballyPositioned {
                    librarianBottomPx = it.positionInRoot().y + it.size.height
                    librarianLeftPx = it.positionInRoot().x
                }
                .clickable {
                    showShelf = true
                    showBookSettings = true
                },
        )
    }
}

/**
 * Couleurs des champs de recherche de la bibliothèque : texte, indication et icônes en blanc
 * sur une case de fond translucide (comme les cartes de l'app), lisibles sur le décor.
 */
@Composable
private fun librarySearchFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedPlaceholderColor = Color.White.copy(alpha = 0.8f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.8f),
    focusedLeadingIconColor = Color.White,
    unfocusedLeadingIconColor = Color.White,
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Color.White,
    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
    focusedLabelColor = Color.White,
    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
    cursorColor = Color.White,
)

/** Boutons texte des dialogues de la bibliothèque : libellés en blanc, comme le reste. */
@Composable
private fun libraryDialogButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = Color.White,
    disabledContentColor = Color.White.copy(alpha = 0.4f),
)

/**
 * Espace vide qui s'étend jusqu'au bas de la bibliothécaire (superposée en haut à droite de
 * l'écran) : placé au-dessus d'un contenu qui prend toute la largeur (listes, étagère...), il
 * garantit que rien ne passe sous elle. Hauteur nulle si le contenu commence déjà plus bas.
 */
@Composable
private fun LibrarianClearance(librarianBottomPx: Float) {
    val density = LocalDensity.current
    var topPx by remember { mutableStateOf(Float.MAX_VALUE) }
    val height = with(density) { (librarianBottomPx - topPx).coerceAtLeast(0f).toDp() }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            // positionInRoot et non boundsInRoot : pour un espace de hauteur nulle, boundsInRoot
            // renvoie Rect.Zero (top = 0), ce qui faisait osciller la hauteur à chaque image
            // (les livres montaient et descendaient en boucle). Seuil de 1 px contre le bruit.
            .onGloballyPositioned { coords ->
                val y = coords.positionInRoot().y
                if (kotlin.math.abs(y - topPx) > 1f) topPx = y
            }
    )
}

/** [content] placé sous la bibliothécaire (voir [LibrarianClearance]). */
@Composable
private fun BelowLibrarian(librarianBottomPx: Float, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        LibrarianClearance(librarianBottomPx)
        content()
    }
}

/**
 * Fenêtre ouverte au clic sur la recherche globale : nombre d'entrées de chaque catégorie
 * (ex. "Monstres — 318"). Choisir une catégorie restreint la recherche à elle ; "Toute la
 * bibliothèque" lève la restriction.
 */
@Composable
private fun GlobalSearchCategoryPanel(
    categories: List<Pair<String, Int?>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
            Text(
                "Où chercher ?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            val total = categories.sumOf { it.second ?: 0 }
            GlobalSearchCategoryRow("Toute la bibliothèque", total, selected == null) { onSelect(null) }
            categories.forEach { (label, count) ->
                GlobalSearchCategoryRow(label, count, selected == label) { onSelect(label) }
            }
        }
    }
}

@Composable
private fun GlobalSearchCategoryRow(label: String, count: Int?, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            Text(
                when (count) {
                    null -> "…"
                    1 -> "1 entrée"
                    else -> "$count entrées"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

/**
 * En-tête "Table des matières" pliable, réutilisé par tous les écrans de bibliothèque
 * qui affichent une table des matières (Règles, Montures...) : reste toujours visible
 * en haut de l'écran (il n'est pas dans la zone défilante), et un clic dessus replie ou
 * déplie la liste des entrées en dessous. La liste, quand dépliée, a une hauteur maximale
 * fixe et défile en interne (`heightIn(max=...)` + son propre `verticalScroll`) : c'est
 * ce qui manquait avant et qui laissait la table des matières prendre tout l'écran quand
 * elle contenait beaucoup d'entrées, ne laissant plus de place (ni de défilement possible)
 * au contenu réel en dessous.
 */
@Composable
private fun CollapsibleTableOfContents(
    titles: List<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEntryClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Table des matières",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Replier la table des matières" else "Déplier la table des matières",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                titles.forEachIndexed { index, title ->
                    Surface(
                        onClick = { onEntryClick(index) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "• $title",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * Box centrée avec CircularProgressIndicator.
 */
@Composable
private fun LoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/** Type d'une entrée listée dans un livre personnalisé : détermine l'écran de fiche ouvert. */
private enum class CustomBookRowKind { MONSTRE, SORT, OBJET, CLASSE, ESPECE, HISTORIQUE, DON, REGLE, LIBRE }

/**
 * Une entrée d'un livre personnalisé telle qu'affichée dans sa liste : [entry] porte le nom
 * et la catégorie (le type, ex. "Monstres"), [subtitle] la ligne grise sous le nom.
 */
private data class CustomBookRow(
    val entry: SrdSectionEntry,
    val kind: CustomBookRowKind,
    val subtitle: String = "",
    val equipment: EquipmentItem? = null,
)

/**
 * Entrées d'un livre personnalisé, groupées par type comme dans les livres intégrés. Les
 * entrées annotées `<!-- type: ... -->` (voir [CustomContentParser]) ouvrent la fiche de leur
 * type (monstre, sort...) ; un fichier sans aucune annotation est découpé par titres (voir
 * [ReferenceEntryParser]), chaque section devenant une fiche de texte libre.
 */
private fun customBookRows(content: String): List<CustomBookRow> {
    val parsed = CustomContentParser.parse(content)
    fun row(name: String, category: String, kind: CustomBookRowKind, subtitle: String = "", equipment: EquipmentItem? = null) =
        CustomBookRow(SrdSectionEntry(name = name, category = category, rawMarkdown = ""), kind, subtitle, equipment)

    val typed = buildList {
        parsed.monsters.forEach { add(row(it.name, "Monstres", CustomBookRowKind.MONSTRE, monsterChallengeLabel(it).orEmpty())) }
        // PNJ : devenus des fiches à l'import ; leur profil reste consultable ici en texte.
        parsed.pnjs.forEach {
            add(CustomBookRow(SrdSectionEntry(name = it.name, category = "PNJ", rawMarkdown = it.rawMarkdown), CustomBookRowKind.LIBRE))
        }
        parsed.spellsIndex.forEach { add(row(it.name, "Sorts", CustomBookRowKind.SORT, it.rawMarkdown)) }
        parsed.equipment.forEach { add(row(it.name, "Objets", CustomBookRowKind.OBJET, it.category, equipment = it)) }
        parsed.classes.forEach { add(row(it.name, "Classes", CustomBookRowKind.CLASSE)) }
        // Sous-classes : fusionnées dans leur classe pour le jeu ; consultables ici en texte.
        parsed.sousClasses.forEach {
            add(CustomBookRow(SrdSectionEntry(name = it.name, category = "Sous-classes", rawMarkdown = it.rawMarkdown), CustomBookRowKind.LIBRE, it.category))
        }
        parsed.especes.forEach { add(row(it.name, "Espèces", CustomBookRowKind.ESPECE)) }
        parsed.historiques.forEach { add(row(it.name, "Historiques", CustomBookRowKind.HISTORIQUE)) }
        parsed.dons.forEach { add(row(it.name, "Dons", CustomBookRowKind.DON, it.category)) }
        parsed.rules.forEach { add(row(it.title, "Règles", CustomBookRowKind.REGLE)) }
    }
    if (typed.isNotEmpty()) return typed

    return ReferenceEntryParser.parse(content).map {
        CustomBookRow(it.copy(category = it.category.ifBlank { "Contenu" }), CustomBookRowKind.LIBRE)
    }
}

/** Message centré (livre introuvable, fichier illisible) dans la vue d'un livre personnalisé. */
@Composable
private fun CustomBookMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

/**
 * Fiche d'une entrée de texte libre d'un livre personnalisé : même carte translucide arrondie
 * que les écrans de détail (règles, dons...), texte en blanc.
 */
@Composable
private fun CustomBookEntryCard(category: String, markdown: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
            contentColor = Color.White,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (category.isNotBlank()) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                SrdMarkdownAvecTables(markdown = markdown)
            }
        }
    }
}

/**
 * Liste alphabétique avec sticky headers et cellules cliquables.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlphabeticalEntryList(
    entries: List<SrdEntry>,
    onItemClick: (SrdEntry) -> Unit,
    trailingLabel: ((SrdEntry) -> String?)? = null,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun résultat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Grouper par première lettre
    val grouped = entries.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (letter, items) ->
            stickyHeader(key = "header_$letter") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = letter.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(items = items, key = { index, entry -> "entry_${entry.name}_${index}" }) { _, entry ->
                SrdEntryRow(
                    entry = entry,
                    onClick = { onItemClick(entry) },
                    trailingLabel = trailingLabel?.invoke(entry),
                )
            }
        }
    }
}

/**
 * Liste groupée par catégorie avec sticky headers de catégorie.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EquipmentByCategoryList(
    entries: List<EquipmentItem>,
    onItemClick: (EquipmentItem) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun équipement disponible",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Conserver l'ordre d'apparition des catégories dans le fichier
    val categoryOrder = entries.map { it.category }.distinct()
    val grouped = entries
        .filter { it.category.isNotBlank() && it.category != "Catégorie" }
        .groupBy { it.category }
        .toSortedMap(compareBy { categoryOrder.indexOf(it) })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (category, items) ->
            stickyHeader(key = "cat_header_$category") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(
                items = items.sortedBy { it.name.lowercase() },
                key = { index, entry -> "entry_${entry.category}_${entry.name}_${index}" }
            ) { _, entry ->
                EquipmentItemRow(entry = entry, onClick = { onItemClick(entry) })
            }
        }
    }
}

/**
 * Liste alphabétique avec sticky headers pour les entrées génériques SRD 5.2.1
 * (classes, espèces, historiques) qui n'ont pas de catégorie significative.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionEntryAlphabeticalList(
    entries: List<SrdSectionEntry>,
    onItemClick: (SrdSectionEntry) -> Unit,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun résultat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val grouped = entries.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (letter, items) ->
            stickyHeader(key = "header_$letter") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = letter.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(items = items, key = { index, entry -> "entry_${entry.name}_${index}" }) { _, entry ->
                SectionEntryRow(name = entry.name, category = "", onClick = { onItemClick(entry) })
            }
        }
    }
}

/**
 * Liste groupée par catégorie avec sticky headers de catégorie, pour les entrées
 * génériques SRD 5.2.1 à deux niveaux (dons, armes/armures magiques).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionEntryByCategoryList(
    entries: List<SrdSectionEntry>,
    onItemClick: (SrdSectionEntry) -> Unit,
    subtitle: ((SrdSectionEntry) -> String)? = null,
    secondaryText: ((SrdSectionEntry) -> String?)? = null,
    // true : entrées dans l'ordre du fichier (règles, glossaire, qui se lisent dans l'ordre)
    // plutôt que triées alphabétiquement dans chaque catégorie.
    keepFileOrder: Boolean = false,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Aucun résultat",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Conserver l'ordre d'apparition des catégories dans le fichier
    val categoryOrder = entries.map { it.category }.distinct()
    val grouped = entries
        .filter { it.category.isNotBlank() }
        .groupBy { it.category }
        .toSortedMap(compareBy { categoryOrder.indexOf(it) })

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        grouped.forEach { (category, items) ->
            stickyHeader(key = "cat_header_$category") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Text(
                        text = category,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            itemsIndexed(
                items = if (keepFileOrder) items else items.sortedBy { it.name.lowercase() },
                key = { index, entry -> "entry_${entry.category}_${entry.name}_${index}" }
            ) { _, entry ->
                SectionEntryRow(
                    name = entry.name,
                    category = subtitle?.invoke(entry).orEmpty(),
                    onClick = { onItemClick(entry) },
                    secondaryText = secondaryText?.invoke(entry).orEmpty(),
                )
            }
        }
    }
}

/**
 * Écran de sections libres (titre + contenu Markdown) avec table des matières
 * cliquable, pour les documents SRD 5.2.1 sans entrées nommées individuelles
 * (ex. montures et véhicules) : table des matières cliquable + contenu par section.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocSectionTocScreen(
    sections: List<SrdDocSection>,
    initialSectionTitle: String? = null,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var tocExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(sections, initialSectionTitle) {
        initialSectionTitle?.let { target ->
            val index = sections.indexOfFirst { it.title.equals(target, ignoreCase = true) }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Table des matières : toujours visible en haut, pliable/dépliable au clic.
        CollapsibleTableOfContents(
            titles = sections.map { it.title },
            expanded = tocExpanded,
            onToggleExpanded = { tocExpanded = !tocExpanded },
            onEntryClick = { index ->
                coroutineScope.launch {
                    listState.animateScrollToItem(index)
                }
            },
        )

        HorizontalDivider()

        // weight(1f) indispensable : sans ça, la table des matières (Column sans
        // hauteur bornée) pouvait s'étendre sur presque tout l'écran quand elle
        // contenait beaucoup d'entrées, ne laissant plus de place — ni de défilement
        // possible — à cette LazyColumn, d'où le contenu qui ne défilait pas.
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(sections, key = { it.title }) { section ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SrdMarkdownAvecTables(markdown = section.content)
                    }
                }
            }
        }
    }
}

/**
 * Ligne réutilisable pour une entrée générique SRD 5.2.1 (nom + catégorie optionnelle),
 * visuellement identique à [SrdEntryRow] mais indépendante du type [SrdEntry].
 */
@Composable
private fun SectionEntryRow(
    name: String,
    category: String,
    onClick: () -> Unit,
    secondaryText: String = "",
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (category.isNotBlank()) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (secondaryText.isNotBlank()) {
                    Text(
                        text = secondaryText,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Représente un onglet de la bibliothèque sous la forme d'un livre sur l'étagère
 * d'accueil. [tabIndex] correspond à l'index dans `tabKeys`/`tabs` de [LibraryScreen].
 * [tabKey] est la clé stable ("monsters", "rules"...) utilisée pour retrouver les
 * réglages persistés de ce livre dans [LibraryBookSettingsStore].
 */
private data class LibraryBook(
    val label: String,
    val icon: ImageVector,
    val tabIndex: Int,
    val tabKey: String,
)

/**
 * Un résultat de la recherche globale (icône "bibliothécaire") : un mot-clé trouvé dans
 * le nom OU le contenu d'une entrée, dans n'importe quel livre. [onSelect] encapsule la
 * navigation exacte (changer d'onglet puis ouvrir le détail, ou faire défiler jusqu'à la
 * bonne section pour les livres à page unique comme Règles/Montures).
 */
private data class GlobalSearchResult(
    val entryName: String,
    val categoryLabel: String,
    // Pertinence (LibrarySearch.score) : les résultats sont triés du plus au moins pertinent.
    val score: Int = 0,
    val onSelect: () -> Unit,
)

/**
 * Écran de gestion des livres (ouvert en cliquant la bibliothécaire) : pour chaque livre,
 * choisir sa couverture parmi les 7 illustrations disponibles et basculer sa disponibilité
 * aux joueurs. Chaque changement est persisté immédiatement (SharedPreferences via
 * [LibraryBookSettingsStore]), donc conservé à la fermeture de l'application, et
 * [onSettingChanged] est appelé pour que l'étagère se mette à jour au retour.
 *
 * Permet aussi d'ajouter un livre personnalisé (n'importe quel fichier, .md ou non,
 * choisi via le sélecteur système) — voir [CustomBooksStore] — et de retirer un livre
 * personnalisé déjà ajouté.
 *
 * Propose également "Forcer la mise à jour" ([onForceRefresh]), qui recopie les fichiers
 * SRD intégrés vers le stockage public même si une copie y existe déjà (voir
 * [SrdRepository.forceRefreshFromAssets]) — utile quand la copie sur l'appareil est restée
 * périmée malgré une mise à jour de l'application.
 */
@Composable
private fun LibraryBookSettingsScreen(
    books: List<LibraryBook>,
    worldId: String?,
    onSettingChanged: () -> Unit,
    onForceRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Livre dont la suppression attend confirmation.
    var bookToRemove by remember { mutableStateOf<LibraryBook?>(null) }

    bookToRemove?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToRemove = null },
            title = { Text("Retirer ce livre ?") },
            text = { Text("« ${book.label} » sera retiré de la bibliothèque, avec ses fichiers.") },
            confirmButton = {
                TextButton(onClick = {
                    bookToRemove = null
                    CustomBooksStore.remove(context, worldId, book.tabKey.removePrefix("custom_"))
                    SrdRepository.invalidateWorld(worldId)
                    onSettingChanged()
                }) { Text("Retirer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { bookToRemove = null }) { Text("Annuler") } },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "add_book") {
            Surface(
                // Ajout d'un livre (.md, .zip illustré, PDF...) : via l'outil IMPORT. La couverture
                // se choisit ensuite ci-dessous, comme pour n'importe quel livre.
                onClick = { com.jc2.jdrcompagnon.feature_import.ImportNavigation.ouvrir() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ajouter un livre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item(key = "force_update") {
            // Confirmation brève ("Fichiers SRD resynchronisés ✓") affichée quelques
            // secondes après un tap, pour que l'action — invisible sinon, puisqu'elle ne
            // fait rien de visible tant qu'on ne rouvre pas un livre — ait un retour
            // immédiat.
            var justRefreshed by remember { mutableStateOf(false) }
            LaunchedEffect(justRefreshed) {
                if (justRefreshed) {
                    delay(2000)
                    justRefreshed = false
                }
            }
            Surface(
                onClick = {
                    onForceRefresh()
                    justRefreshed = true
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Forcer la mise à jour",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (justRefreshed) {
                                "Fichiers SRD resynchronisés ✓"
                            } else {
                                "Recopie les fichiers SRD depuis l'application, même si une version existe déjà sur l'appareil"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (justRefreshed) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.8f)
                            },
                        )
                    }
                }
            }
        }

        items(books, key = { it.tabKey }) { book ->
            val isCustom = book.tabKey.startsWith("custom_")
            var selectedSkin by remember(book.tabKey, worldId) {
                mutableStateOf(LibraryBookSettingsStore.getSkinIndex(context, worldId, book.tabKey))
            }
            var visibleToPlayers by remember(book.tabKey, worldId) {
                mutableStateOf(LibraryBookSettingsStore.isVisibleToPlayers(context, worldId, book.tabKey))
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = book.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = book.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (isCustom) {
                            IconButton(onClick = { bookToRemove = book }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Retirer ce livre",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Couverture",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bookSkinDrawables.forEachIndexed { index, drawableRes ->
                            val isSelected = selectedSkin == index
                            // Bordure épaisse + pastille à coche en surimpression : le voile
                            // semi-transparent précédent était trop discret pour distinguer
                            // la couverture sélectionnée des autres au premier coup d'œil.
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clickable {
                                        selectedSkin = index
                                        LibraryBookSettingsStore.setSkinIndex(context, worldId, book.tabKey, index)
                                        onSettingChanged()
                                    },
                            ) {
                                Image(
                                    painter = painterResource(drawableRes),
                                    contentDescription = "Couverture ${index + 1}",
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sélectionnée",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(10.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Disponible pour les joueurs",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = visibleToPlayers,
                            onCheckedChange = { checked ->
                                visibleToPlayers = checked
                                LibraryBookSettingsStore.setVisibleToPlayers(context, worldId, book.tabKey, checked)
                                onSettingChanged()
                            },
                        )
                    }
                }
            }
        }
    }

}

/**
 * Étagère d'accueil de la bibliothèque : les onglets sont présentés comme de fines
 * tranches de livre, rangées par 4, dans l'esprit d'une vraie bibliothèque. Les tranches
 * utilisent un cycle de 3 illustrations (`livre_1/2/3`) issues de `res/drawable`, et la
 * planche d'étagère est l'illustration `bli_etagere` — plus de couleurs Material codées
 * en dur ici, tout vient des assets fournis.
 */
@Composable
private fun LibraryBookshelf(
    books: List<LibraryBook>,
    onBookClick: (Int) -> Unit,
    worldId: String?,
    settingsVersion: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bookSpacing = 10.dp
    val contentPaddingH = 20.dp
    // Largeur de livre fixe et cible (augmentée d'environ 20% par rapport à la largeur
    // trop fine précédente). Le nombre de livres par étagère n'est plus fixé en dur :
    // il est recalculé selon la largeur d'écran disponible, pour que cette largeur de
    // livre reste constante sur tous les écrans (plus de livres sur un écran large,
    // moins sur un écran étroit) plutôt que d'étirer ou de comprimer les livres.
    val bookWidth = 41.dp

    BoxWithConstraints(modifier = modifier) {
        val scope = this
        val availableWidth = scope.maxWidth - contentPaddingH * 2
        val booksPerShelf = ((availableWidth + bookSpacing) / (bookWidth + bookSpacing))
            .toInt()
            .coerceAtLeast(1)
        val shelves = books.chunked(booksPerShelf)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = contentPaddingH),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            itemsIndexed(shelves, key = { index, _ -> "shelf_$index" }) { shelfIndex, shelfBooks ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Superposition explicite dans un Box : la planche est dessinée en
                    // premier, la rangée de livres en second, donc les livres restent
                    // au-dessus de l'étagère (et non l'inverse) sur leur zone de recouvrement.
                    // La planche n'a pas de padding horizontal (elle va bord à bord), alors
                    // que la rangée de livres garde son retrait habituel via son propre padding.
                    val rowHeight = 260.dp
                    val shelfHeight = 32.dp
                    val overlap = 10.dp
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight + shelfHeight - overlap),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.bli_etagere),
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(shelfHeight)
                                .align(Alignment.BottomCenter),
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = contentPaddingH)
                                .height(rowHeight)
                                .align(Alignment.TopStart)
                                .offset(y = (-5).dp),
                            horizontalArrangement = Arrangement.spacedBy(bookSpacing, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            shelfBooks.forEachIndexed { i, book ->
                                // Couverture choisie dans l'écran de réglages (roue crantée)
                                // si elle existe, sinon cycle par défaut des 7 illustrations.
                                val overrideIndex = remember(book.tabKey, worldId, settingsVersion) {
                                    LibraryBookSettingsStore.getSkinIndex(context, worldId, book.tabKey)
                                }
                                val defaultSkin = bookSkinDrawables[(shelfIndex * booksPerShelf + i) % bookSkinDrawables.size]
                                val bookSkin = overrideIndex?.let { bookSkinDrawables.getOrNull(it) } ?: defaultSkin
                                BookSpine(
                                    book = book,
                                    bookSkinRes = bookSkin,
                                    onClick = { onBookClick(book.tabIndex) },
                                    modifier = Modifier
                                        .width(bookWidth)
                                        .fillMaxHeight(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fine tranche de livre cliquable : l'illustration [bookSkinRes] sert de fond (couverture
 * du livre), avec l'icône et le titre (pivoté à 90°, comme sur une vraie tranche)
 * superposés par-dessus. Un léger voile sombre en haut/bas garantit la lisibilité du
 * texte quelle que soit la luminosité de l'illustration de couverture.
 */
@Composable
private fun BookSpine(
    book: LibraryBook,
    bookSkinRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Couverture affichée en entier (FillBounds = image complète, sans rognage,
            // même si ça étire légèrement — préférable à un Crop qui coupait le haut/bas
            // des illustrations comme les épées).
            Image(
                painter = painterResource(bookSkinRes),
                contentDescription = book.label,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
            // Léger voile en bas uniquement, pour garder le titre lisible sans assombrir
            // le reste de la couverture.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Titre écrit à la verticale, comme sur une tranche de livre : la Text est
                // dimensionnée avant rotation (largeur = hauteur réellement disponible dans
                // cet espace, mesurée via BoxWithConstraints), hauteur = une ligne, puis
                // pivotée de 90°. `requiredWidth` (et non `width`) est indispensable ici :
                // un livre étroit donne un BoxWithConstraints étroit, et un simple `width`
                // aurait été contraint/rogné à cette largeur avant même la rotation, coupant
                // le texte. `requiredWidth` impose la largeur voulue quelles que soient les
                // contraintes du parent — la rotation ramène ensuite tout dans l'emprise du
                // livre (clippée par le Surface englobant), sans troncature.
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val scope = this
                    val availableHeight = scope.maxHeight
                    Text(
                        text = book.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                blurRadius = 6f,
                            ),
                        ),
                        modifier = Modifier
                            .requiredWidth(availableHeight)
                            .rotate(-90f),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.dp)
                        .background(ForcedDarkPalette.AccentGold.copy(alpha = 0.7f)),
                )
            }
        }
    }
}

@Composable
private fun SrdEntryRow(
    entry: SrdEntry,
    onClick: () -> Unit,
    trailingLabel: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (entry.category.isNotBlank() && entry.category != "Catégorie") {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!trailingLabel.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Petit badge mettant en avant une statistique d'équipement (poids ou prix) : fond teinté
 * AccentGold, très lisible en un coup d'œil, distinct du reste de la ligne qui reste en
 * texte neutre (dégâts, CA, propriétés).
 */
@Composable
private fun EquipmentStatBadge(emoji: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ForcedDarkPalette.AccentGold.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, ForcedDarkPalette.AccentGold.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ForcedDarkPalette.AccentGold,
            )
        }
    }
}

@Composable
private fun EquipmentItemRow(
    entry: EquipmentItem,
    onClick: () -> Unit,
) {
    val isWeapon = entry.category.lowercase().contains("arme")
    val isArmor = entry.category.lowercase().contains("armure")
    val hasWeight = entry.weight.isNotBlank() && entry.weight != "-"
    val hasCost = entry.cost.isNotBlank() && entry.cost != "-"
    // Poids et prix sont retirés du sous-titre : ils sont désormais mis en avant à part,
    // sous forme de badges (voir EquipmentStatBadge), plutôt que noyés dans le texte gris.
    val subtitle = buildList {
        if (isWeapon && entry.damage.isNotBlank()) add(entry.damage)
        if (isArmor && entry.ac.isNotBlank()) add("CA ${entry.ac}")
        if (entry.properties.isNotBlank()) add(entry.properties)
    }.joinToString(" • ").takeIf { it.isNotBlank() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        tonalElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hasWeight || hasCost) {
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    if (hasCost) {
                        EquipmentStatBadge(emoji = "💰", value = entry.cost)
                    }
                    if (hasWeight) {
                        if (hasCost) Spacer(modifier = Modifier.height(4.dp))
                        EquipmentStatBadge(emoji = "⚖️", value = entry.weight)
                    }
                }
            }
        }
    }
}