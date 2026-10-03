package com.jc2.jdrcompagnon.feature_combat.domain.model

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.BACK_WEAPON_SLOTS
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.screens.joueur.character.ArmorRules
import com.jc2.jdrcompagnon.ui.screens.joueur.character.creation.Classe
import com.jc2.jdrcompagnon.ui.screens.joueur.spellcastingAbilityByClass
import com.jc2.jdrcompagnon.ui.stackCount

/** Un sort du SRD : nom, niveau (0 = sort mineur), fiche détaillée (cf. SpellParser) et école de magie. */
data class SortSrd(val nom: String, val niveau: Int, val rawMarkdown: String, val ecole: String = "")

/**
 * Effet de combat d'une aptitude de classe ou de sous-classe, lu dans une balise
 * `<!-- id: …; combat: TYPE[; ecole: École][; carac: Caractéristique] -->` de classes_srd521.md
 * (cf. ArsenalPersonnage.effetsCombatClasse). [classe] = classe qui accorde l'aptitude : l'effet ne
 * s'applique qu'aux sorts lancés via cette classe, et seulement à ceux de [ecole] si elle est donnée.
 */
data class EffetCombatClasse(
    val type: String,
    val source: String,
    val classe: String,
    val ecole: String? = null,
    val carac: String? = null,
) {
    companion object {
        /** Sort mineur à dégâts : moitié des dégâts sur une attaque ratée ou un JS réussi (Sort mineur appuyé). */
        const val DEMI_DEGATS_MINEUR = "demi-degats-mineur"
        /** Sort de zone : des alliés réussissent leur JS et ne subissent aucun dégât (Façonneur de sorts). */
        const val PROTECTION_ALLIES = "protection-allies"
        /** Modificateur de [carac] ajouté à un jet de dégâts du sort (Évocation améliorée). */
        const val BONUS_DEGATS_CARAC = "bonus-degats-carac"
    }
}

/** Arme tenue en main, avec ses jets calculés pour le personnage. */
data class ArmeEnMain(
    val nom: String,
    val main: String,
    val bonusToucher: Int,
    val formuleDegats: String,
    val typeDegats: String?,
    val proprietes: String,
    val botte: String?,
    val maitrisee: Boolean,
    val aDistance: Boolean,
    // Détail du calcul et règles utiles (« For +3, maîtrise +2 », « Action bonus (Légère) »…).
    val notes: List<String>,
    // Emplacement où l'arme est portée (null = mains nues).
    val slot: EquipmentSlot? = null,
    // Arme à munitions (propriété « Munitions (24/96 ; flèches) ») : type de munition et nombre
    // disponible (carquois + emplacements + sac). Sans munition, l'attaque est impossible.
    val munition: String? = null,
    val munitionsRestantes: Int = 0,
    // Arme de lancer (propriété « Lancer ») : chaque lancer retire un exemplaire de [slot].
    val lancer: Boolean = false,
    // Exemplaires portés sur l'emplacement (pile de javelines dans le dos).
    val quantite: Int = 1,
) {
    /** Arme à munitions sans munition : l'attaque est interdite. */
    val sansMunition: Boolean get() = munition != null && munitionsRestantes <= 0
}

/** Capacité de classe qui modifie les attaques (Attaque supplémentaire, Attaque sournoise…). */
data class CapaciteAttaque(val nom: String, val detail: String)

enum class JetSort { ATTAQUE, SAUVEGARDE, AUCUN }

/** Sort prêt à être lancé, avec ce qu'il faut lancer comme dés. */
data class SortPret(
    val nom: String,
    val niveau: Int,
    val tempsIncantation: String,
    val portee: String,
    val jet: JetSort,
    // Bonus d'attaque de sort (jet ATTAQUE) ou DD (jet SAUVEGARDE).
    val bonusAttaque: Int,
    val dd: Int,
    val sauvegarde: String?,
    val formuleDegats: String?,
    val typeDegats: String?,
    val soin: String?,
    val resume: String,
    // Fiche complète du sort (école, portée, durée, description), pour le détail côté joueur.
    val fiche: String = "",
    // Sort mineur appuyé : moitié des dégâts sur une attaque ratée ou un JS réussi.
    val demiDegatsSiEchec: Boolean = false,
    // Rappels des aptitudes qui modifient ce sort (Façonneur de sorts, Évocation améliorée…).
    val notesClasse: List<String> = emptyList(),
    // Lançable à son niveau sans emplacement (Character.sortsSpeciaux) : « a-volonte » ou
    // « predilection » ; [gratuitDisponible] faux quand la prédilection est déjà utilisée.
    val lancementGratuit: String? = null,
    val gratuitDisponible: Boolean = false,
    // Sort de zone (sphère, cône, ligne…) à dégâts : touche plusieurs cibles, chacune fait son JS.
    val zone: Boolean = false,
)

/**
 * Ce qu'un personnage peut faire en combat, calculé depuis sa fiche : armes en main (bonus
 * d'attaque et dégâts), capacités de classe liées aux attaques, sorts prêts. Pur (aucun accès
 * au stockage) : les armes et sorts du SRD sont fournis par l'appelant.
 */
object ArsenalPersonnage {

    /** Classes qui changent leurs sorts préparés après un repos long (règles 2024). */
    val classesQuiPreparent = setOf("Clerc", "Druide", "Magicien", "Paladin")

    /** Classes disposant de la Maîtrise des armes (bottes) en 2024. */
    private val classesAvecBottes = setOf("Barbare", "Guerrier", "Paladin", "Rôdeur", "Roublard", "Voleur")

    const val SORT_CHATIMENT_DIVIN = "Châtiment divin"

    /** Bottes d'arme choisies d'office à la création (le joueur les change au repos long). */
    val bottesParDefaut: Map<String, List<String>> = mapOf("Paladin" to listOf("Épée longue", "Javeline"))

    /** Nombre d'armes dont le personnage peut utiliser la botte (0 = pas de Maîtrise des armes). */
    fun nombreBottes(c: Character): Int = listOf(
        niveau(c, "Barbare").let { n -> when { n >= 10 -> 4; n >= 4 -> 3; n >= 1 -> 2; else -> 0 } },
        niveau(c, "Guerrier").let { n -> when { n >= 16 -> 6; n >= 10 -> 5; n >= 4 -> 4; n >= 1 -> 3; else -> 0 } },
        if (niveau(c, "Paladin", "Rôdeur", "Roublard", "Voleur") >= 1) 2 else 0,
    ).max()

    /**
     * Armes dont la botte est active : le choix enregistré ([Character.weaponMasteries]), sinon le
     * défaut de la classe (Paladin : épée longue et javeline), sinon null = toutes les armes
     * maîtrisées (personnages créés avant le choix des bottes).
     */
    fun bottesActives(c: Character): List<String>? =
        c.weaponMasteries.ifEmpty { null }
            ?: bottesParDefaut.entries.firstOrNull { (classe, _) -> c.characterClass.equals(classe, ignoreCase = true) }?.value

    /** Réserve de Puissance curative (Imposition des mains) : 5 × niveau de Paladin, 0 sinon. */
    fun reservePuissanceCurative(c: Character): Int = 5 * niveau(c, "Paladin")

    /** Châtiment de paladin (niveau 2) : Châtiment divin toujours préparé, une fois gratuit par repos long. */
    fun aChatimentDePaladin(c: Character): Boolean = niveau(c, "Paladin") >= 2

    /** Niveau de Magicien du personnage (0 s'il n'en a pas). */
    fun niveauMagicien(c: Character): Int = niveau(c, "Magicien")

    /**
     * Restauration magique (Magicien) : somme des niveaux des emplacements récupérables, la moitié
     * du niveau de Magicien arrondie au supérieur (0 sans niveau de Magicien).
     */
    fun budgetRestaurationMagique(c: Character): Int = (niveauMagicien(c) + 1) / 2

    /** Restauration magique : aucun emplacement du 6e niveau ou plus. */
    const val NIVEAU_MAX_RESTAURATION_MAGIQUE = 5

    /** Mémorisation de sort (Magicien niv. 5) : un sort préparé échangé à chaque repos court. */
    const val NIVEAU_MEMORISATION_DE_SORT = 5

    /** Don Sauvagerie martiale : dés de dégâts de l'arme lancés deux fois, meilleur résultat gardé. */
    fun aSauvagerieMartiale(c: Character): Boolean = c.feats.contains("Sauvagerie martiale", ignoreCase = true)

    fun mod(score: Int) = Math.floorDiv(score - 10, 2)

    /** Niveau du personnage dans chaque classe (principale + multiclassage). */
    fun niveauxParClasse(c: Character): Map<String, Int> {
        val secondaires = c.classesSecondaires.associate { it.classe to it.niveau }
        return secondaires + (c.characterClass to (c.level - secondaires.values.sum()).coerceAtLeast(1))
    }

    private fun niveau(c: Character, vararg classes: String): Int =
        niveauxParClasse(c).filterKeys { k -> classes.any { it.equals(k, ignoreCase = true) } }.values.maxOrNull() ?: 0

    private fun texteAptitudes(c: Character) = (c.classFeatures + " " + c.feats + " " + c.subclass).lowercase()

    private fun signe(v: Int) = if (v >= 0) "+$v" else "$v"

    private fun formule(des: String, bonus: Int) = des + when {
        bonus > 0 -> " + $bonus"
        bonus < 0 -> " - ${-bonus}"
        else -> ""
    }

    /** Le personnage maîtrise-t-il [arme] (formation aux armes de sa classe) ? */
    fun maitrise(c: Character, arme: ArmeSrd): Boolean {
        val entrainement = (c.weaponArmorTraining + " " + c.proficiencies).lowercase()
        if (entrainement.isBlank()) return true
        return entrainement.contains("armes de guerre") ||
            (arme.categorie.contains("Courante", ignoreCase = true) && entrainement.contains("armes courantes")) ||
            entrainement.contains(arme.nom.lowercase())
    }

    /** Type de munition d'une arme : « Munitions (24/96 ; flèches) » → « flèches ». */
    fun typeMunition(proprietes: String): String? =
        Regex("""Munitions\s*\([^;)]*;\s*([^)]+)\)""", RegexOption.IGNORE_CASE).find(proprietes)?.groupValues?.get(1)?.trim()

    /** Munitions de [type] portées par le personnage : carquois, emplacements (hors armes de dos) et sac. */
    fun munitionsDisponibles(c: Character, type: String): Int {
        val estMunition = { nom: String -> ArmorRules.estMunitionDe(nom, type) }
        val emplacements = c.equippedSlots.filter { (s, nom) -> s !in BACK_WEAPON_SLOTS && estMunition(nom) }.keys.sumOf { c.stackCount(it) }
        return c.quiverContents.count(estMunition) + emplacements + c.backpackItems.count(estMunition)
    }

    /**
     * Toutes les armes équipées — main principale, main secondaire, puis armes portées dans le
     * dos — avec leurs jets calculés (mains nues si aucune arme n'est portée).
     */
    fun armesEnMain(c: Character, armesSrd: List<ArmeSrd>): List<ArmeEnMain> {
        val aptitudes = texteAptitudes(c)
        val modFor = mod(c.strength)
        val modDex = mod(c.dexterity)
        val bottes = classesAvecBottes.any { cl -> niveau(c, cl) > 0 }
        val bottesChoisies = bottesActives(c)
        fun trouver(nom: String?) = nom?.let { n ->
            armesSrd.firstOrNull { it.nom.equals(n, ignoreCase = true) }
                ?: armesSrd.filter { n.contains(it.nom, ignoreCase = true) }.maxByOrNull { it.nom.length }
        }
        val principaleNom = c.equippedSlots[EquipmentSlot.MAIN_HAND]
        val secondaireNom = c.equippedSlots[EquipmentSlot.OFF_HAND]
        val principale = trouver(principaleNom)
        val secondaire = trouver(secondaireNom)?.takeIf { secondaireNom != principaleNom }
        val deuxArmes = principale != null && secondaire != null

        fun calculer(arme: ArmeSrd, nomAffiche: String, main: String, secondaireMain: Boolean, slot: EquipmentSlot): ArmeEnMain? {
            val m = Regex("""(\d+d\d+)\s*(\p{L}+)?""").find(arme.degats) ?: return null
            val finesse = arme.proprietes.contains("Finesse", ignoreCase = true)
            val (caracLabel, modCarac) = when {
                arme.aDistance -> "Dex" to modDex
                finesse && modDex > modFor -> "Dex" to modDex
                else -> "For" to modFor
            }
            val maitrisee = maitrise(c, arme)
            val magie = ArmesPersonnage.bonusMagique(arme.copy(nom = nomAffiche))
            val notes = mutableListOf<String>()
            var toucher = modCarac + magie + if (maitrisee) c.proficiencyBonus else 0
            notes += "$caracLabel ${signe(modCarac)}" + (if (maitrisee) ", maîtrise +${c.proficiencyBonus}" else ", non maîtrisée") +
                if (magie != 0) ", magie ${signe(magie)}" else ""
            if (arme.aDistance && aptitudes.contains("archerie")) {
                toucher += 2
                notes += "Style Archerie : +2 au toucher"
            }
            // Arme de la main secondaire : attaque en action bonus (propriété Légère), sans le
            // modificateur aux dégâts sauf style Combat à deux armes.
            val ajouterModDegats = !secondaireMain || aptitudes.contains("deux armes") || modCarac < 0
            var bonusDegats = magie + if (ajouterModDegats) modCarac else 0
            if (!arme.aDistance && !deuxArmes && aptitudes.contains("duel") && !arme.proprietes.contains("Deux mains", ignoreCase = true)) {
                bonusDegats += 2
                notes += "Style Duel : +2 aux dégâts"
            }
            if (secondaireMain) notes += "Attaque en action bonus (arme Légère)" + if (!ajouterModDegats) ", sans modificateur aux dégâts" else ""
            if (arme.proprietes.contains("Deux mains", ignoreCase = true) && aptitudes.contains("deux mains")) {
                notes += "Style Armes à deux mains : relancez les 1 et 2 aux dégâts"
            }
            Regex("""\((\d+)\s*/\s*(\d+)""").find(arme.proprietes)?.let { notes += "Portée ${it.groupValues[1]}/${it.groupValues[2]} m" }
            val munition = typeMunition(arme.proprietes)
            val lancer = arme.proprietes.contains("Lancer", ignoreCase = true)
            return ArmeEnMain(
                nom = nomAffiche,
                main = main,
                bonusToucher = toucher,
                formuleDegats = formule(m.groupValues[1], bonusDegats),
                typeDegats = m.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() },
                proprietes = arme.proprietes,
                botte = arme.botte?.takeIf {
                    bottes && maitrisee && (bottesChoisies == null || bottesChoisies.any { b -> b.equals(arme.nom, ignoreCase = true) })
                },
                maitrisee = maitrisee,
                aDistance = arme.aDistance,
                notes = notes,
                slot = slot,
                munition = munition,
                munitionsRestantes = munition?.let { munitionsDisponibles(c, it) } ?: 0,
                lancer = lancer,
                quantite = c.stackCount(slot),
            )
        }

        // Armes de dos : dégainées gratuitement (interaction avec un objet) pour attaquer. Une
        // arme déjà en main n'est pas répétée (les cartes et déclarations sont nommées par arme).
        val nomsEnMain = setOfNotNull(principaleNom?.lowercase(), secondaireNom?.lowercase())
        val armesDeDos = listOf(EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3)
            .mapNotNull { s -> c.equippedSlots[s]?.let { nom -> s to nom } }
            .filter { (_, nom) -> nom.lowercase() !in nomsEnMain }
            .distinctBy { (_, nom) -> nom.lowercase() }
            .mapNotNull { (s, nom) -> trouver(nom)?.let { calculer(it, nom, "Dans le dos", false, s) } }
        val enMain = listOfNotNull(
            principale?.let {
                calculer(it, principaleNom!!, if (it.proprietes.contains("Deux mains", true)) "Deux mains" else "Main principale", false, EquipmentSlot.MAIN_HAND)
            },
            secondaire?.let { calculer(it, secondaireNom!!, "Main secondaire", secondaireMain = principale != null, EquipmentSlot.OFF_HAND) },
        )
        if (enMain.isNotEmpty()) return enMain + armesDeDos
        return armesDeDos + listOf(frappeMainsNues(c))
    }

    const val MAINS_NUES = "Mains nues"
    const val EMPOIGNADE = "Empoignade"
    const val BOUSCULADE = "Bousculade"

    /** Frappe à mains nues (coup) : 1 + For (moine : dé d'arts martiaux, Dex possible). */
    fun frappeMainsNues(c: Character): ArmeEnMain {
        val modFor = mod(c.strength)
        val modDex = mod(c.dexterity)
        val moine = niveau(c, "Moine")
        val (carac, modCarac) = if (moine > 0 && modDex > modFor) "Dex" to modDex else "For" to modFor
        val de = if (moine > 0) desArtsMartiaux(moine) else null
        return ArmeEnMain(
            nom = MAINS_NUES,
            main = MAINS_NUES,
            bonusToucher = modCarac + c.proficiencyBonus,
            formuleDegats = de?.let { formule(it, modCarac) } ?: "${(1 + modCarac).coerceAtLeast(1)}",
            typeDegats = "contondants",
            proprietes = "",
            botte = null,
            maitrisee = true,
            aDistance = false,
            notes = listOf("$carac ${signe(modCarac)}, maîtrise +${c.proficiencyBonus}"),
        )
    }

    /**
     * DD de l'Empoignade et de la Bousculade (frappe à mains nues, règles 2024) : la cible fait un
     * jet de sauvegarde de Force ou de Dextérité (à son choix) contre 8 + For + maîtrise.
     */
    fun ddFrappeMainsNues(c: Character): Int = 8 + mod(c.strength) + c.proficiencyBonus

    /** Attaques par action Attaquer (Attaque supplémentaire). */
    fun nbAttaques(c: Character): Int {
        val guerrier = niveau(c, "Guerrier")
        return when {
            guerrier >= 20 -> 4
            guerrier >= 11 -> 3
            guerrier >= 5 || listOf("Barbare", "Paladin", "Rôdeur", "Moine").any { niveau(c, it) >= 5 } -> 2
            else -> 1
        }
    }

    /** Fougue (Guerrier niveau 2) : une action supplémentaire par repos court. */
    fun aFougue(c: Character): Boolean = niveau(c, "Guerrier") >= 2

    private fun desArtsMartiaux(niveau: Int) = when {
        niveau >= 17 -> "1d12"
        niveau >= 11 -> "1d10"
        niveau >= 5 -> "1d8"
        else -> "1d6"
    }

    /** Capacités de classe qui changent la façon d'attaquer. */
    fun capacitesAttaque(c: Character): List<CapaciteAttaque> {
        val aptitudes = texteAptitudes(c)
        val liste = mutableListOf<CapaciteAttaque>()
        val guerrier = niveau(c, "Guerrier")
        val nbAttaques = nbAttaques(c)
        if (nbAttaques > 1) liste += CapaciteAttaque("Attaque supplémentaire", "$nbAttaques attaques quand vous prenez l'action Attaquer")
        val roublard = niveau(c, "Roublard", "Voleur")
        if (roublard > 0) {
            liste += CapaciteAttaque(
                "Attaque sournoise",
                "+${(roublard + 1) / 2}d6 une fois par tour, arme Finesse ou à distance, avec l'Avantage ou un allié au contact de la cible"
            )
        }
        val barbare = niveau(c, "Barbare")
        if (barbare > 0) {
            val rage = when {
                barbare >= 16 -> 4
                barbare >= 9 -> 3
                else -> 2
            }
            liste += CapaciteAttaque("Rage", "+$rage aux dégâts des attaques utilisant la Force pendant la rage")
        }
        val moine = niveau(c, "Moine")
        if (moine > 0) liste += CapaciteAttaque("Arts martiaux", "Mains nues ${desArtsMartiaux(moine)} ; frappe à mains nues en action bonus")
        if (guerrier >= 3 && aptitudes.contains("champion")) {
            liste += CapaciteAttaque("Critique amélioré", "Coup critique sur 19 ou 20" + if (guerrier >= 15) " (18–20 au niveau 15)" else "")
        }
        if (aChatimentDePaladin(c)) {
            liste += CapaciteAttaque(
                "Châtiment divin",
                "Action bonus après avoir touché au corps à corps : +2d8 radiants (+1d8 par niveau d'emplacement au-delà du 1er, " +
                    "+1d8 contre un Fiélon ou un Mort-vivant). Une fois sans emplacement par repos long" +
                    if (c.divineSmiteFreeUsed) " (déjà utilisée)." else " (disponible)."
            )
        }
        if (aSauvagerieMartiale(c)) {
            liste += CapaciteAttaque(
                "Sauvagerie martiale",
                "Une fois par tour, quand vous touchez avec une arme : lancez deux fois les dés de dégâts de l'arme et gardez le meilleur résultat."
            )
        }
        if (guerrier >= 2) liste += CapaciteAttaque("Fougue", "Une action supplémentaire (repos court)")
        return liste
    }

    /**
     * Sorts prêts : sorts mineurs, puis sorts préparés — pour Clerc, Druide, Magicien et Paladin,
     * ceux cochés dans l'écran de repos ([Character.preparedSpells], tous tant qu'aucune
     * préparation n'a été faite), pour les autres classes tous les sorts connus.
     */
    fun sortsPrets(c: Character, sortsSrd: List<SortSrd>, effets: List<EffetCombatClasse> = emptyList()): List<SortPret> {
        val prepares = c.preparedSpells?.toSet()
        // Châtiment de paladin : Châtiment divin toujours préparé, hors limite de sorts préparés.
        val toujoursPrepares = sortsToujoursPreparesClasse(c)
        return (c.spells + toujoursPrepares).distinctBy { it.lowercase() }.mapNotNull { nom ->
            val srd = sortsSrd.firstOrNull { it.nom.equals(nom, ignoreCase = true) } ?: return@mapNotNull null
            val toujours = toujoursPrepares.any { it.equals(nom, ignoreCase = true) }
            val classe = c.spellClasses[nom] ?: if (memeSort(nom, SORT_CHATIMENT_DIVIN) && toujours) "Paladin" else c.characterClass
            val doitEtrePrepare = srd.niveau > 0 && classesQuiPreparent.any { it.equals(classe, ignoreCase = true) }
            if (doitEtrePrepare && !toujours && prepares != null && nom !in prepares) return@mapNotNull null
            val mode = c.sortsSpeciaux.entries.firstOrNull { memeSort(it.key, nom) }?.value
            sortPret(c, srd, classe, effets).copy(
                lancementGratuit = mode,
                gratuitDisponible = mode == SORT_A_VOLONTE ||
                    (mode == SORT_PREDILECTION && c.sortsPredilectionUtilises.none { memeSort(it, nom) }),
            )
        }.sortedWith(compareBy<SortPret> { it.niveau }.thenBy { it.nom })
    }

    /** Mode de Character.sortsSpeciaux : lançable à volonté sans emplacement (Maîtrise des sorts). */
    const val SORT_A_VOLONTE = "a-volonte"

    /** Mode de Character.sortsSpeciaux : une fois sans emplacement par repos court ou long (Sorts de prédilection). */
    const val SORT_PREDILECTION = "predilection"

    /**
     * Sorts toujours préparés par une aptitude de classe, hors limite de sorts préparés : Châtiment
     * divin (Châtiment de paladin) et sorts lançables sans emplacement (Maîtrise des sorts, Sorts de
     * prédilection). Les sorts de sous-classe s'y ajoutent ailleurs (sortsDeSousClasse).
     */
    fun sortsToujoursPreparesClasse(c: Character): List<String> =
        (if (aChatimentDePaladin(c)) listOf(SORT_CHATIMENT_DIVIN) else emptyList()) + c.sortsSpeciaux.keys

    /** Libellé court du mode d'un sort spécial (badge de la fiche, choix d'emplacement). */
    fun libelleSortSpecial(mode: String?): String? = when (mode) {
        SORT_A_VOLONTE -> "À volonté"
        SORT_PREDILECTION -> "Prédilection"
        else -> null
    }

    /**
     * Effets de combat des aptitudes atteintes par le personnage (classes et sous-classes, y compris
     * multiclassage) : balises « combat: » de classes_srd521.md, cf. [EffetCombatClasse]. Une
     * sous-classe secondaire est lue dans classFeatures (« Sous-classe (Classe) : Nom »).
     */
    fun effetsCombatClasse(c: Character, classes: List<Classe>): List<EffetCombatClasse> =
        niveauxParClasse(c).flatMap { (nomClasse, niveauClasse) ->
            val classe = classes.firstOrNull { it.nom.equals(nomClasse, ignoreCase = true) } ?: return@flatMap emptyList()
            val nomSousClasse = if (nomClasse.equals(c.characterClass, ignoreCase = true)) c.subclass.trim()
            else Regex("""Sous-classe \(${Regex.escape(classe.nom)}\) : (.+)""").find(c.classFeatures)?.groupValues?.get(1)?.trim().orEmpty()
            val sousClasse = classe.sousClasses.firstOrNull { it.nom.equals(nomSousClasse, ignoreCase = true) }
            (classe.aptitudes + sousClasse?.aptitudes.orEmpty())
                .filter { apt -> apt.niveaux.isNotEmpty() && apt.niveaux.min() <= niveauClasse }
                .flatMap { apt ->
                    apt.balises.mapNotNull { b ->
                        b["combat"]?.let { EffetCombatClasse(it.trim(), apt.nom, classe.nom, b["ecole"], b["carac"]) }
                    }
                }
        }

    private val zoneRegex = Regex("""\b(rayon|cône|cube|ligne|sphère|cylindre|émanation)\b""", RegexOption.IGNORE_CASE)

    private fun valeurCarac(c: Character, carac: String?): Int? = when (carac?.lowercase()) {
        "force" -> c.strength
        "dextérité" -> c.dexterity
        "constitution" -> c.constitution
        "intelligence" -> c.intelligence
        "sagesse" -> c.wisdom
        "charisme" -> c.charisma
        else -> null
    }

    private val tempsRegex = Regex("""\*\*Temps d'incantation :\*\*\s*(.+)""")
    private val porteeRegex = Regex("""\*\*Portée :\*\*\s*(.+)""")
    private val sauvegardeRegex = Regex("""jet de sauvegarde de (\p{L}+)""", RegexOption.IGNORE_CASE)
    private val degatsRegex = Regex("""(\d+)d(\d+)(\s*\+\s*votre modificateur de caractéristique d[’']incantation)?\s*dégâts\s+(?:de\s+|d[’'])?(\p{L}+)""")
    private val soinRegex = Regex("""(?:récupère|regagne)[^.]*?(\d+d\d+)(\s*\+\s*votre modificateur)?""")

    internal fun sortPret(c: Character, srd: SortSrd, classe: String, effets: List<EffetCombatClasse> = emptyList()): SortPret {
        val caracIncantation = spellcastingAbilityByClass.entries.firstOrNull { it.key.equals(classe, ignoreCase = true) }?.value
        val modIncantation = mod(
            when (caracIncantation) {
                "Intelligence" -> c.intelligence
                "Sagesse" -> c.wisdom
                "Charisme" -> c.charisma
                else -> 10
            }
        )
        val description = srd.rawMarkdown.substringAfter("\n\n", srd.rawMarkdown)
        val jet = when {
            description.contains("attaque de sort", ignoreCase = true) -> JetSort.ATTAQUE
            sauvegardeRegex.containsMatchIn(description) -> JetSort.SAUVEGARDE
            else -> JetSort.AUCUN
        }
        // Sort mineur : dés multipliés aux niveaux 5, 11 et 17 du personnage.
        val paliers = if (srd.niveau == 0) 1 + listOf(5, 11, 17).count { c.level >= it } else 1
        // Aptitudes qui s'appliquent à ce sort : accordées par la classe qui le lance, de son école le cas échéant.
        val effetsSort = effets.filter { e ->
            e.classe.equals(classe, ignoreCase = true) && (e.ecole == null || e.ecole.equals(srd.ecole, ignoreCase = true))
        }
        val notesClasse = mutableListOf<String>()
        // Évocation améliorée : modificateur de la caractéristique ajouté à un jet de dégâts (pas déjà compris dans la formule).
        val bonusCarac = effetsSort.firstOrNull { it.type == EffetCombatClasse.BONUS_DEGATS_CARAC }
        val degats = degatsRegex.find(description)?.let { m ->
            val des = "${m.groupValues[1].toInt() * paliers}d${m.groupValues[2]}"
            val formuleDegats = when {
                m.groupValues[3].isNotEmpty() -> formule(des, modIncantation)
                bonusCarac != null -> {
                    val bonusMod = mod(valeurCarac(c, bonusCarac.carac) ?: 10)
                    notesClasse += "${bonusCarac.source} : ${signe(bonusMod)} aux dégâts"
                    formule(des, bonusMod)
                }
                else -> des
            }
            formuleDegats to m.groupValues[4]
        }
        val demiDegatsSiEchec = srd.niveau == 0 && degats != null && effetsSort.any { it.type == EffetCombatClasse.DEMI_DEGATS_MINEUR }
        if (demiDegatsSiEchec) {
            notesClasse += "${effetsSort.first { it.type == EffetCombatClasse.DEMI_DEGATS_MINEUR }.source} : moitié des dégâts si l'attaque rate ou si la cible réussit son JS"
        }
        effetsSort.firstOrNull { it.type == EffetCombatClasse.PROTECTION_ALLIES }
            ?.takeIf { degats != null && zoneRegex.containsMatchIn(description) }
            ?.let { e ->
                val nb = 1 + srd.niveau
                notesClasse += "${e.source} : jusqu'à $nb allié(s) de la zone réussissent leur JS et ne subissent aucun dégât"
            }
        val soin = soinRegex.find(description)?.let { m ->
            if (m.groupValues[2].isNotEmpty()) formule(m.groupValues[1], modIncantation) else m.groupValues[1]
        }
        val sauvegarde = sauvegardeRegex.find(description)?.groupValues?.get(1)
        val bonus = modIncantation + c.proficiencyBonus
        val dd = 8 + bonus
        val resume = buildList {
            when (jet) {
                JetSort.ATTAQUE -> add("Attaque de sort ${signe(bonus)}")
                JetSort.SAUVEGARDE -> add("JS ${sauvegarde ?: ""} DD $dd".replace("  ", " "))
                JetSort.AUCUN -> {}
            }
            degats?.let { add("${it.first} ${it.second}") }
            soin?.let { add("soigne $it") }
            if (demiDegatsSiEchec) add("½ si échec")
        }.joinToString(" · ")
        return SortPret(
            nom = srd.nom,
            niveau = srd.niveau,
            tempsIncantation = tempsRegex.find(srd.rawMarkdown)?.groupValues?.get(1)?.trim().orEmpty(),
            portee = porteeRegex.find(srd.rawMarkdown)?.groupValues?.get(1)?.trim().orEmpty(),
            jet = jet,
            bonusAttaque = bonus,
            dd = dd,
            sauvegarde = sauvegarde,
            formuleDegats = degats?.first,
            typeDegats = degats?.second,
            soin = soin,
            resume = resume,
            fiche = srd.rawMarkdown,
            demiDegatsSiEchec = demiDegatsSiEchec,
            notesClasse = notesClasse,
            zone = degats != null && jet != JetSort.ATTAQUE && zoneRegex.containsMatchIn(description),
        )
    }

    /**
     * Deux noms de sort désignent-ils le même sort ? Insensible à la casse, aux espaces et aux
     * variantes d'apostrophe (’ / ') entre l'index des sorts et leurs fiches détaillées.
     */
    fun memeSort(a: String, b: String): Boolean = normaliserNomSort(a) == normaliserNomSort(b)

    private fun normaliserNomSort(nom: String) =
        nom.trim().lowercase().replace('’', '\'').replace('ʼ', '\'').replace(Regex("""\s+"""), " ")

    /** Le personnage connaît-il déjà [nom] ? */
    fun sortConnu(c: Character, nom: String): Boolean = c.spells.any { memeSort(it, nom) }

    /** Sort lançable en rituel : temps d'incantation « … ou rituel » (sorts_srd521.md). */
    fun estRituel(tempsIncantation: String): Boolean = tempsIncantation.contains("rituel", ignoreCase = true)

    /**
     * Durée d'une incantation en rituel, en minutes : temps d'incantation normal (« Action » = 0,
     * « 1 minute », « 1 heure »…) + 10 minutes.
     */
    fun dureeRituelMinutes(tempsIncantation: String): Int {
        val normal = tempsIncantation.substringBefore(" ou ", tempsIncantation).lowercase()
        val nombre = Regex("""\d+""").find(normal)?.value?.toIntOrNull() ?: 1
        val base = when {
            "heure" in normal -> nombre * 60
            "minute" in normal -> nombre
            else -> 0
        }
        return base + 10
    }

    /**
     * Peut-il lancer [nomSort] (rituel) en rituel ? Tout lanceur le peut pour un sort préparé ;
     * le Magicien (Savoir rituel) aussi pour tout sort de son grimoire, même non préparé.
     */
    fun rituelAutorise(c: Character, nomSort: String): Boolean {
        val classe = c.spellClasses[nomSort] ?: c.characterClass
        if (classe.equals("Magicien", ignoreCase = true) && niveauMagicien(c) >= 1) return true
        val prepares = c.preparedSpells ?: return true
        return !classesQuiPreparent.any { it.equals(classe, ignoreCase = true) } || prepares.any { memeSort(it, nomSort) }
    }

    /** Niveau d'un sort depuis SrdEntry.niveauSort (« Sort mineur » → 0, « Niveau 3 » → 3). */
    fun niveauDepuisLibelle(libelle: String): Int =
        if (libelle.contains("mineur", ignoreCase = true)) 0 else Regex("""\d+""").find(libelle)?.value?.toIntOrNull() ?: 0
}
