package com.jc2.jdrcompagnon.ui.screens.mj.library.srd

import java.io.File

/**
 * Images des monstres fournies avec le contenu importé par l'utilisateur plutôt qu'avec
 * l'APK : livre personnalisé ajouté sous forme d'archive .zip (le .md et ses images, voir
 * `copyPickedFileToInternalStorage` dans LibraryScreen) ou univers importé (dossier
 * `custom_worlds/<id>`, voir [com.jc2.jdrcompagnon.ui.worlds.CustomWorldsRepository]).
 *
 * Pour chaque monstre, l'image est cherchée dans le dossier [root] du livre :
 * 1. Déclarée par la ligne `Image: images/gobelin.png` du bloc du monstre (chemin relatif au
 *    .md, puis à la racine de l'archive ; à défaut, n'importe quel fichier de même nom dans
 *    l'archive, pour tolérer une arborescence différente de celle déclarée).
 * 2. Sinon, un fichier nommé d'après le monstre, comme pour les assets (voir
 *    [MonsterImages.slug]) : "Gobelin chef" → gobelin_chef.png/jpg/jpeg/webp.
 *
 * Une image trouvée remplace [SrdEntry.image] par son chemin absolu, que [MonsterImages.load]
 * décode depuis le stockage interne. Une image introuvable laisse l'entrée intacte : une
 * ligne `Image:` pointant vers les assets de l'app reste ainsi valable.
 */
internal object CustomBookImages {

    private val extensions = listOf("png", "jpg", "jpeg", "webp")
    private val markdownImage = Regex("""!\[[^\]]*]\(([^)]+)\)""")

    fun resolve(monsters: List<SrdEntry>, mdFile: File, root: File): List<SrdEntry> {
        if (monsters.isEmpty() || !root.isDirectory) return monsters
        val rootPath = root.canonicalPath
        fun File.isInsideRoot() = canonicalPath.startsWith(rootPath + File.separator)

        val imagesByName by lazy {
            root.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in extensions }
                .associateBy { it.name.lowercase() }
        }

        return monsters.map { monster ->
            val declared = monster.image?.let { (markdownImage.find(it)?.groupValues?.get(1) ?: it).trim() }
            val found = if (declared != null) {
                listOfNotNull(mdFile.parentFile?.let { File(it, declared) }, File(root, declared))
                    .firstOrNull { it.isFile && it.isInsideRoot() }
                    ?: imagesByName[File(declared).name.lowercase()]
            } else {
                val slug = MonsterImages.slug(monster.name)
                extensions.firstNotNullOfOrNull { imagesByName["$slug.$it"] }
            }
            if (found != null) monster.copy(image = found.absolutePath) else monster
        }
    }
}
