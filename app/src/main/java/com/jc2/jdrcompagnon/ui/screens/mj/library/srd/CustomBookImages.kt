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
        val trouver = chercheur(mdFile, root)
        return monsters.map { monster ->
            trouver(monster.image, monster.name)?.let { monster.copy(image = it.absolutePath) } ?: monster
        }
    }

    /** Même recherche pour les objets (champ **Image** d'un objet, voir EquipmentParser). */
    fun resolveObjets(objets: List<EquipmentItem>, mdFile: File, root: File): List<EquipmentItem> {
        if (objets.isEmpty() || !root.isDirectory) return objets
        val trouver = chercheur(mdFile, root)
        return objets.map { objet ->
            trouver(objet.image, objet.name)?.let { objet.copy(image = it.absolutePath) } ?: objet
        }
    }

    private val imageChampHistorique = Regex("""(?mi)^(\s*-\s*Image\s*:\s*)(.+?)\s*$""")

    /**
     * Même recherche pour les historiques (champ `- Image : images/acolyte.jpg`, voir HistoriqueParser) :
     * le champ est réécrit avec le chemin absolu de l'image trouvée, affichée à l'étape Historique
     * de la création de personnage. Un historique sans champ Image reste intact.
     */
    fun resolveHistoriques(historiques: List<SrdSectionEntry>, mdFile: File, root: File): List<SrdSectionEntry> {
        if (historiques.isEmpty() || !root.isDirectory) return historiques
        val trouver = chercheur(mdFile, root)
        return historiques.map { h ->
            val champ = imageChampHistorique.find(h.rawMarkdown) ?: return@map h
            val fichier = trouver(champ.groupValues[2], h.name) ?: return@map h
            h.copy(rawMarkdown = h.rawMarkdown.replaceRange(champ.groups[2]!!.range, fichier.absolutePath))
        }
    }

    /**
     * Même recherche pour les images insérées dans le texte d'une règle (`![Le Corbeau](images/corbeau.jpg)`,
     * ex. les cartes d'un jeu) : chaque chemin trouvé est réécrit en chemin absolu, affiché par
     * SrdMarkdownAvecTables. Une image introuvable reste telle quelle (non affichée).
     */
    fun resolveRegles(regles: List<RuleSection>, mdFile: File, root: File): List<RuleSection> {
        if (regles.isEmpty() || !root.isDirectory) return regles
        val trouver = chercheur(mdFile, root)
        return regles.map { r ->
            if (!markdownImage.containsMatchIn(r.content)) return@map r
            r.copy(content = markdownImage.replace(r.content) { m ->
                val fichier = trouver(m.groupValues[1], "") ?: return@replace m.value
                m.value.replaceRange(m.groups[1]!!.range.first - m.range.first, m.groups[1]!!.range.last - m.range.first + 1, fichier.absolutePath)
            })
        }
    }

    /** Recherche du fichier image d'une entrée (image déclarée ou null, nom de l'entrée) dans [root]. */
    private fun chercheur(mdFile: File, root: File): (String?, String) -> File? {
        val rootPath = root.canonicalPath
        fun File.isInsideRoot() = canonicalPath.startsWith(rootPath + File.separator)

        val imagesByName by lazy {
            root.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in extensions }
                .associateBy { it.name.lowercase() }
        }

        return { image, nom ->
            val declared = image?.let { (markdownImage.find(it)?.groupValues?.get(1) ?: it).trim() }
            if (declared != null) {
                listOfNotNull(mdFile.parentFile?.let { File(it, declared) }, File(root, declared))
                    .firstOrNull { it.isFile && it.isInsideRoot() }
                    ?: imagesByName[File(declared).name.lowercase()]
            } else {
                val slug = MonsterImages.slug(nom)
                extensions.firstNotNullOfOrNull { imagesByName["$slug.$it"] }
            }
        }
    }
}
