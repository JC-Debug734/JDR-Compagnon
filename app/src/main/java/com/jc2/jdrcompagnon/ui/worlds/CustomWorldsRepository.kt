package com.jc2.jdrcompagnon.ui.worlds

import android.content.Context
import android.net.Uri
import com.jc2.jdrcompagnon.ui.WorldState
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Métadonnées optionnelles d'un univers importé, lues depuis son fichier `reference.md`
 * (voir [ReferenceMdParser]). Toutes les valeurs sont facultatives : un `reference.md`
 * absent ou vide retombe sur le nom du dossier et l'apparence par défaut de l'app.
 */
internal data class WorldReference(
    val name: String? = null,
    val description: String? = null,
    val iconName: String? = null,
    val backgroundImage: String? = null,
    val primaryColorHex: String? = null,
    val secondaryColorHex: String? = null,
    val backgroundColorHex: String? = null,
    val categories: List<String>? = null,
)

/**
 * Parseur du fichier `reference.md` d'un univers importé : des lignes "Label: valeur"
 * (voir [fieldRegex]), toutes optionnelles — un univers minimal peut se limiter à
 * `Nom: ...` ou même n'avoir aucun `reference.md` du tout.
 *
 * Format attendu (un exemple, toutes les lignes sont facultatives) :
 *
 *   Nom: Ravenloft
 *   Description: Un monde de horreur gothique
 *   Icone: DarkMode
 *   Fond: fond.jpg
 *   Couleur primaire: #6A1B9A
 *   Couleur secondaire: #4A148C
 *   Couleur fond: #1A0D26
 *   Categories: monstres, objets, regles, classes
 */
internal object ReferenceMdParser {
    private val fieldRegex = Regex("""^([^:]+):\s*(.*)$""")

    // Table des libellés (minuscule, sans accent) reconnus, vers la clé canonique du champ.
    private val fieldAliases = mapOf(
        "nom" to "nom",
        "description" to "description",
        "icone" to "icone",
        "icon" to "icone",
        "fond" to "fond",
        "fond d'ecran" to "fond",
        "image de fond" to "fond",
        "couleur primaire" to "primaire",
        "couleur secondaire" to "secondaire",
        "couleur fond" to "couleur_fond",
        "categories" to "categories",
        "catégories" to "categories",
    )

    fun parse(rawMarkdown: String): WorldReference {
        val fields = mutableMapOf<String, String>()
        for (line in rawMarkdown.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val match = fieldRegex.find(trimmed) ?: continue
            val label = normalizeLabel(match.groupValues[1])
            val key = fieldAliases[label] ?: continue
            val value = match.groupValues[2].trim()
            if (value.isNotBlank()) fields[key] = value
        }

        return WorldReference(
            name = fields["nom"],
            description = fields["description"],
            iconName = fields["icone"],
            backgroundImage = fields["fond"],
            primaryColorHex = fields["primaire"],
            secondaryColorHex = fields["secondaire"],
            backgroundColorHex = fields["couleur_fond"],
            categories = fields["categories"]?.split(",")
                ?.map { it.trim().lowercase() }
                ?.filter { it.isNotBlank() },
        )
    }

    private fun normalizeLabel(raw: String): String =
        raw.trim().lowercase()
            .replace("è", "e").replace("é", "e").replace("ê", "e")
}

/**
 * Univers importés par l'utilisateur (archives .zip contenant un `reference.md` et des
 * fichiers markdown de contenu — voir [com.jc2.jdrcompagnon.ui.screens.mj.library.srd.CustomContentParser]),
 * stockés dans `filesDir/custom_worlds/<id>/`, un dossier par univers. Complète les deux
 * mondes intégrés (`donjon_et_dragon`, `naheulbeuk`) dans l'écran de sélection de monde.
 */
object CustomWorldsRepository {
    private const val ROOT_DIR = "custom_worlds"
    private const val REFERENCE_FILE = "reference.md"

    private fun worldsDir(context: Context): File =
        File(context.filesDir, ROOT_DIR).apply { mkdirs() }

    private fun worldDir(context: Context, worldId: String): File = File(worldsDir(context), worldId)

    /** Liste les univers importés, triés par nom affiché. */
    fun listCustomWorlds(context: Context): List<WorldState> =
        worldsDir(context).listFiles { file -> file.isDirectory }
            ?.mapNotNull { dir -> runCatching { toWorldState(dir) }.getOrNull() }
            ?.sortedBy { it.name.lowercase() }
            .orEmpty()

    /**
     * Fichiers markdown de contenu (monstres/objets/règles...) de l'univers [worldId],
     * `reference.md` exclu — fusionnés par
     * [com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository] avec le contenu officiel.
     * Liste vide pour un monde qui n'est pas un univers importé.
     */
    fun contentFiles(context: Context, worldId: String?): List<File> {
        if (worldId.isNullOrBlank()) return emptyList()
        val dir = worldDir(context, worldId)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles { file ->
            file.isFile && file.extension.equals("md", ignoreCase = true) &&
                !file.name.equals(REFERENCE_FILE, ignoreCase = true)
        }?.toList().orEmpty()
    }

    /**
     * Extrait l'archive .zip [uri] dans un nouveau dossier `custom_worlds/<id>`, l'identifiant
     * étant dérivé du nom déclaré dans `reference.md` (ou de [fallbackName] à défaut), rendu
     * unique si besoin. Les entrées qui tenteraient d'écrire hors du dossier cible ("zip
     * slip") sont ignorées. Retourne l'état du nouvel univers, ou une erreur si l'archive
     * est illisible ou ne contient aucun fichier.
     */
    fun importWorldFromZip(context: Context, uri: Uri, fallbackName: String): Result<WorldState> {
        val tempDir = File(context.cacheDir, "world_import_${System.currentTimeMillis()}").apply { mkdirs() }
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(IllegalArgumentException("Fichier illisible"))

            var extractedCount = 0
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val target = File(tempDir, entry.name)
                    val safe = target.canonicalPath == tempDir.canonicalPath ||
                        target.canonicalPath.startsWith(tempDir.canonicalPath + File.separator)
                    if (safe) {
                        if (entry.isDirectory) {
                            target.mkdirs()
                        } else {
                            target.parentFile?.mkdirs()
                            target.outputStream().use { out -> zip.copyTo(out) }
                            extractedCount++
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            if (extractedCount == 0) {
                return Result.failure(IllegalArgumentException("Archive vide ou invalide"))
            }

            val contentRoot = resolveContentRoot(tempDir)
            val reference = File(contentRoot, REFERENCE_FILE)
                .takeIf { it.exists() }
                ?.let { ReferenceMdParser.parse(it.readText()) }
                ?: WorldReference()

            val worldId = uniqueSlug(context, reference.name?.takeIf { it.isNotBlank() } ?: fallbackName)
            val finalDir = worldDir(context, worldId)
            if (!contentRoot.renameTo(finalDir)) {
                contentRoot.copyRecursively(finalDir, overwrite = true)
            }
            Result.success(toWorldState(finalDir))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /** Retire un univers importé (dossier et tout son contenu). */
    fun deleteCustomWorld(context: Context, worldId: String): Boolean =
        worldDir(context, worldId).deleteRecursively()

    /**
     * Si l'archive extraite ne contient qu'un seul dossier à sa racine et pas de
     * `reference.md` directement à la racine, ce dossier est considéré comme la racine du
     * contenu — cas fréquent d'un .zip généré à partir d'un dossier nommé.
     */
    private fun resolveContentRoot(extractedDir: File): File {
        if (File(extractedDir, REFERENCE_FILE).exists()) return extractedDir
        val children = extractedDir.listFiles().orEmpty()
        val singleDir = children.singleOrNull { it.isDirectory }
        return if (children.size == 1 && singleDir != null) singleDir else extractedDir
    }

    private fun uniqueSlug(context: Context, name: String): String {
        val base = name.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "univers" }
        var candidate = base
        var suffix = 2
        while (worldDir(context, candidate).exists()) {
            candidate = "${base}_$suffix"
            suffix++
        }
        return candidate
    }

    private fun toWorldState(dir: File): WorldState {
        val reference = File(dir, REFERENCE_FILE)
            .takeIf { it.exists() }
            ?.let { ReferenceMdParser.parse(it.readText()) }
            ?: WorldReference()

        val backgroundImagePath = reference.backgroundImage
            ?.let { relative -> File(dir, relative) }
            ?.takeIf { it.exists() }
            ?.absolutePath

        return WorldState(
            id = dir.name,
            name = reference.name?.takeIf { it.isNotBlank() } ?: dir.name,
            description = reference.description.orEmpty(),
            isCustom = true,
            iconName = reference.iconName,
            primaryColorHex = reference.primaryColorHex,
            secondaryColorHex = reference.secondaryColorHex,
            backgroundColorHex = reference.backgroundColorHex,
            backgroundImagePath = backgroundImagePath,
            enabledCategories = reference.categories,
        )
    }
}
