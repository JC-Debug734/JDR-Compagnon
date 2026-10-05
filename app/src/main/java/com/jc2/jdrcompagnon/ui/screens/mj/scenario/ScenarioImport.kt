package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import com.jc2.jdrcompagnon.feature_exploration.ExplorationGrille
import com.jc2.jdrcompagnon.feature_exploration.ExplorationSession
import com.jc2.jdrcompagnon.feature_exploration.ZoneExploration
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.ProficiencyLevel
import com.jc2.jdrcompagnon.ui.calculateProficiencyBonus

/**
 * Lignes d'en-tête d'un fichier .md de scénario, lues à l'import (en plus des scènes lues par
 * GameState.parseScenarioMarkdown) pour qu'un seul fichier apporte tout le nécessaire :
 *
 * - `{mimage: nom=carte-ferme; data=<PNG/JPEG en base64>}` : image embarquée, affichée dans une
 *   scène par une ligne `{image:carte-ferme}` (voir PlainScenarioRenderer) ;
 * - `{mlieu: nom=Ferme Crâne-beurré; image=carte-ferme}` : lieu du scénario (bandeau de la
 *   première page du lecteur, image de la table de jeu) ;
 * - `{mpnj: nom=...; race=...; for=16; ...}` : fiche PNJ créée à l'import si aucun PNJ du même
 *   nom n'existe déjà dans le monde — les liens `#pnj:[Nom]` et `#event:[Nom]` du scénario
 *   fonctionnent alors directement.
 * - `{mzone: image=carte-ferme; colonnes=30; nom=Étang; rects=690,590,190,150|...}` : zone
 *   d'exploration préparée pour une image (rectangles x,y,largeur,hauteur en pixels de l'image,
 *   séparés par |), prête à être dévoilée d'un clic sur la page table (feature_exploration).
 *   Une case couverte par plusieurs zones appartient à la plus petite (voir casesExclusives).
 * - `{mchapitre: numero=3; titre=Quand la maison brûle; scenario=2}` : chapitre et numéro du
 *   scénario, qui rangent les listes de scénarios dans l'ordre de jeu (voir ChapitresScenarios).
 * - `{mevenement: titre=...; type=RENCONTRE; ...}`, dans une scène : événement de la
 *   bibliothèque créé à l'import et rattaché à la scène (voir ScenarioEvenements).
 *
 * Valeurs échappées comme {mdiscussion:} (\; \} \n \\), cf. GameState.metaLineFields.
 */
data class ScenarioExtras(
    val images: Map<String, String> = emptyMap(), // nom -> base64
    val lieuNom: String? = null,
    val lieuImage: String? = null,
    val pnjs: List<Map<String, String>> = emptyList(),
    val zones: List<ZonePreparee> = emptyList(),
)

/** Zone d'exploration décrite par une ligne `{mzone:}`, dans l'ordre du fichier. */
data class ZonePreparee(
    val image: String,
    val colonnes: Int?,
    val nom: String,
    val rectangles: List<IntArray>,
)

object ScenarioImport {

    private val ImageLigneRegex = Regex("""(?m)^[ \t]*\{image:\s*([^}\n]+?)\s*\}[ \t]*$""")

    fun extraireExtras(markdown: String): ScenarioExtras {
        val images = mutableMapOf<String, String>()
        var lieu: Map<String, String>? = null
        val pnjs = mutableListOf<Map<String, String>>()
        val zones = mutableListOf<ZonePreparee>()
        markdown.lineSequence().forEach { line ->
            GameState.metaLineFields(line, "mzone")?.let { f ->
                val image = f["image"]?.takeIf { it.isNotBlank() } ?: return@let
                val nom = f["nom"]?.takeIf { it.isNotBlank() } ?: return@let
                val rects = f["rects"].orEmpty().split('|').mapNotNull { r ->
                    r.split(',').mapNotNull { it.trim().toIntOrNull() }.takeIf { it.size == 4 }?.toIntArray()
                }
                if (rects.isNotEmpty()) zones += ZonePreparee(image, f["colonnes"]?.trim()?.toIntOrNull(), nom, rects)
            }
            GameState.metaLineFields(line, "mimage")?.let { f ->
                val nom = f["nom"]?.takeIf { it.isNotBlank() }
                val data = f["data"]?.filterNot { it.isWhitespace() }
                if (nom != null && !data.isNullOrBlank()) images[nom] = data
            }
            GameState.metaLineFields(line, "mlieu")?.let { lieu = it }
            GameState.metaLineFields(line, "mpnj")?.let { f -> if (!f["nom"].isNullOrBlank()) pnjs += f }
        }
        return ScenarioExtras(
            images = images,
            lieuNom = lieu?.get("nom")?.takeIf { it.isNotBlank() },
            lieuImage = lieu?.get("image")?.takeIf { it.isNotBlank() },
            pnjs = pnjs,
            zones = zones,
        )
    }

    /**
     * Zones d'exploration d'une image, converties en cases pour sa grille : la taille de grille
     * est celle de la première zone de l'image (sinon la valeur par défaut de l'exploration).
     */
    fun zonesPourImage(
        zones: List<ZonePreparee>,
        largeurImage: Int,
        hauteurImage: Int,
    ): Pair<Int, List<ZoneExploration>> {
        val colonnes = (zones.firstNotNullOfOrNull { it.colonnes } ?: ExplorationSession.COLONNES_DEFAUT)
            .coerceIn(ExplorationSession.PLAGE_COLONNES)
        val lignes = ExplorationGrille.lignes(colonnes, largeurImage, hauteurImage)
        val brutes = zones.map { z ->
            ZoneExploration(
                nom = z.nom,
                cases = ExplorationGrille.casesPourRectangles(z.rectangles, largeurImage, hauteurImage, colonnes, lignes)
            )
        }
        return colonnes to casesExclusives(brutes)
    }

    /**
     * Une case couverte par plusieurs zones n'appartient qu'à la plus petite (la plus précise) :
     * dévoiler une grande zone (ex. une piste dessinée par grands rectangles) ne dévoile plus les
     * salles qu'elle recouvre, chacune restant à dévoiler à part.
     */
    internal fun casesExclusives(zones: List<ZoneExploration>): List<ZoneExploration> {
        val proprietaire = mutableMapOf<Int, Int>()
        zones.forEachIndexed { i, z ->
            z.cases.forEach { c ->
                val actuel = proprietaire[c]
                if (actuel == null || z.cases.size < zones[actuel].cases.size) proprietaire[c] = i
            }
        }
        return zones.mapIndexed { i, z -> z.copy(cases = z.cases.filter { proprietaire[it] == i }.toSet()) }
    }

    /** Remplace les `{image:nom}` par le nom du fichier stocké pour cette image (inconnus laissés tels quels). */
    fun remplacerImages(contenu: String, fichiers: Map<String, String>): String =
        contenu.replace(ImageLigneRegex) { m -> fichiers[m.groupValues[1]]?.let { "{image:$it}" } ?: m.value }

    /** Fiche PNJ décrite par une ligne `{mpnj:}` (voir la liste des clés dans le README de l'import). */
    fun pnjVersCharacter(f: Map<String, String>, worldId: String): Character {
        fun int(cle: String, defaut: Int) = f[cle]?.trim()?.toIntOrNull() ?: defaut
        val niveau = int("niveau", 1)
        val pvMax = int("pvmax", int("pv", 10))
        val competences = f["competences"].orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }
        return Character(
            name = f["nom"].orEmpty().trim(),
            type = "PNJ",
            worldId = worldId,
            race = f["race"].orEmpty(),
            characterClass = f["classe"].orEmpty(),
            level = niveau,
            alignment = f["alignement"].orEmpty(),
            background = f["historique"].orEmpty(),
            strength = int("for", 10),
            dexterity = int("dex", 10),
            constitution = int("con", 10),
            intelligence = int("int", 10),
            wisdom = int("sag", 10),
            charisma = int("cha", 10),
            maxHitPoints = pvMax,
            currentHitPoints = int("pv", pvMax).coerceIn(0, pvMax),
            armorClass = int("ca", 10),
            speed = int("vitesse", 30),
            proficiencyBonus = calculateProficiencyBonus(niveau),
            skills = competences.associateWith { ProficiencyLevel.PROFICIENT },
            skillProficiencies = competences,
            equipment = f["equipement"].orEmpty(),
            classFeatures = f["capacites"].orEmpty(),
            appearance = f["apparence"].orEmpty(),
            comportement = f["comportement"].orEmpty(),
            intentions = f["intentions"].orEmpty(),
            objectif = f["objectif"].orEmpty(),
            dmNotes = f["notes"].orEmpty(),
            portrait = f["portrait"].orEmpty(),
            createdBy = "MJ",
        )
    }

    /**
     * Importe un scénario complet depuis son markdown : scènes, images embarquées (copiées dans
     * le stockage de l'app), lieu, et fiches PNJ. Retourne le scénario enregistré et le nombre de
     * PNJ créés.
     */
    fun importer(context: Context, markdown: String, worldId: String): Pair<GameState.MjScenario, Int> {
        val (scenario, pnjs) = preparer(context, markdown, worldId)
        GameState.addMjScenario(scenario, context)
        pnjs.forEach { GameState.addCharacter(it) }
        return scenario to pnjs.size
    }

    /**
     * Construit le scénario (images déjà copiées dans le stockage de l'app) et les fiches PNJ à
     * créer (celles dont le nom n'existe pas encore dans le monde), sans enregistrer scénario ni
     * fiches — pour GameState.syncScenariosFromDisk, qui enregistre ses scénarios en lot. Seuls les
     * événements {mevenement:} sont enregistrés ici (ids stables : sans doublon d'une fois à l'autre).
     */
    fun preparer(
        context: Context,
        markdown: String,
        worldId: String,
        // Id déjà porté par le fichier (commentaire caché) : conservé, pour qu'un même fichier
        // redonne toujours le même scénario au lieu d'une nouvelle copie à chaque synchronisation.
        idImpose: String? = null,
    ): Pair<GameState.MjScenario, List<Character>> {
        val (titre, scenes) = GameState.parseScenarioMarkdown(markdown)
        val extras = extraireExtras(markdown)
        val scenarioId = idImpose ?: java.util.UUID.randomUUID().toString()

        val fichiers = extras.images.mapNotNull { (nom, base64) ->
            val bytes = runCatching { android.util.Base64.decode(base64, android.util.Base64.DEFAULT) }.getOrNull()
                ?: return@mapNotNull null
            val slug = GameState.slugify(nom)
            val fichier = ScenarioImageStore.ecrire(context, "${scenarioId}_$slug.img", bytes)
            // Brouillard préparé pour cette image : zones prêtes à dévoiler d'un clic dès la
            // première ouverture de son exploration (clé identique à celle de PlainScenarioRenderer).
            val zonesImage = extras.zones.filter { it.image == nom }
            if (zonesImage.isNotEmpty()) {
                val dims = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, dims)
                val (colonnes, zones) = zonesPourImage(zonesImage, dims.outWidth, dims.outHeight)
                ExplorationSession.preparerCarte(context, "scenario-image:$fichier", colonnes, zones)
            }
            nom to fichier
        }.toMap()

        // Événements {mevenement:} des scènes : enregistrés dans la bibliothèque d'événements (ids
        // stables, une resynchronisation met à jour les mêmes) et rattachés à leur scène.
        val (scenesAvecEvenements, evenements) = ScenarioEvenements.extraire(scenes, scenarioId, worldId)
        if (evenements.isNotEmpty()) {
            runCatching {
                kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    com.jc2.jdrcompagnon.di.EvenementDependencies.repository.sauvegarderTous(evenements)
                }
            }.onFailure { android.util.Log.e("ScenarioImport", "Événements non enregistrés", it) }
        }

        val chapitre = ChapitresScenarios.lire(markdown)
        val scenario = GameState.MjScenario(
            id = scenarioId,
            title = titre,
            chapitreNumero = chapitre?.numero,
            chapitreTitre = chapitre?.titre.orEmpty(),
            numero = chapitre?.scenario,
            description = GameState.preambuleScenario(markdown),
            worldId = worldId,
            scenes = scenesAvecEvenements.map { it.copy(markdownContent = remplacerImages(it.markdownContent, fichiers)) },
            createdBy = "MJ",
            lieuNom = extras.lieuNom.orEmpty(),
            lieuImageFileName = extras.lieuImage?.let { fichiers[it] },
        )

        val existants = GameState.characters.value
            .filter { it.type == "PNJ" && it.worldId == worldId }
            .map { it.name.lowercase() }
            .toSet()
        val nouveaux = extras.pnjs
            .map { pnjVersCharacter(it, worldId) }
            .filter { it.name.lowercase() !in existants }
            .distinctBy { it.name.lowercase() }
        return scenario to nouveaux
    }
}
