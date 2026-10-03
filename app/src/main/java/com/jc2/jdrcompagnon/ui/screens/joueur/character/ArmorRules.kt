package com.jc2.jdrcompagnon.ui.screens.joueur.character

import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.EquipmentSlot
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.EquipmentItem
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * Règles de calcul de la Classe d'Armure (CA) pour D&D 5e.
 * Séparées de GameState pour rester testables et réutilisables.
 *
 * V7 (slot-based) : la CA se calcule UNIQUEMENT depuis equippedSlots.
 * - TORSO occupé par une armure reconnue → base armure + mod Dex (max)
 * - TORSO vide → 10 + mod Dex
 * - OFF_HAND contient "bouclier" → +2
 */
object ArmorRules {

    /**
     * Détail du calcul de CA : valeur totale, texte explicatif et
     * indicateurs sur l'armure/bouclier équipés.
     */
    data class AcBreakdown(
        val total: Int,
        val label: String,
        val detail: String,
        val hasArmor: Boolean,
        val hasShield: Boolean,
        val armorName: String? = null,
        val maxDex: Int? = null
    )

    /**
     * Calcule la CA détaillée d'un personnage D&D depuis equippedSlots.
     */
    fun computeAc(character: Character): AcBreakdown {
        val dexMod = abilityModifier(character.dexterity)
        val torsoItem = character.equippedSlots[EquipmentSlot.TORSO]
        val armorSpec = torsoItem?.let { armorSpec(it) }
        val baseAc = armorSpec?.baseAc ?: 10
        val maxDex = armorSpec?.maxDexBonus

        val appliedDex = when {
            armorSpec == null -> dexMod
            armorSpec.category == ArmorCategory.HEAVY -> 0
            maxDex != null -> dexMod.coerceAtMost(maxDex)
            else -> dexMod
        }

        val offHandItem = character.equippedSlots[EquipmentSlot.OFF_HAND]
        val hasShield = offHandItem != null && offHandItem.contains("bouclier", ignoreCase = true)
        val shieldBonus = if (hasShield) 2 else 0
        val total = baseAc + appliedDex + shieldBonus

        val detail = buildString {
            val parts = mutableListOf<String>()
            if (armorSpec != null) {
                parts.add(armorSpec.displayName)
            }
            if (appliedDex != 0 || armorSpec == null) {
                parts.add(if (armorSpec != null && maxDex != null && dexMod > maxDex) "Dex (max +$maxDex)" else "Dex")
            }
            if (hasShield) {
                parts.add("Bouclier")
            }
            append(if (parts.isEmpty()) "Base" else parts.joinToString(" + "))
        }

        return AcBreakdown(
            total = total,
            label = "CA $total",
            detail = "CA $total ($detail)",
            hasArmor = armorSpec != null,
            hasShield = hasShield,
            armorName = armorSpec?.displayName ?: torsoItem,
            maxDex = maxDex
        )
    }


    private enum class ArmorCategory { LIGHT, MEDIUM, HEAVY }

    private data class ArmorSpec(
        val displayName: String,
        val baseAc: Int,
        val category: ArmorCategory,
        val maxDexBonus: Int? = null
    )

    private fun abilityModifier(score: Int): Int = (score - 10) / 2

    internal fun abilityModifierPublic(score: Int): Int = abilityModifier(score)

    /**
     * Renvoie la spécification d'une armure reconnue, ou null si ce n'est pas une armure.
     * Le nom affiché est normalisé pour un rendu plus propre.
     */
    private fun armorSpec(itemName: String): ArmorSpec? {
        return when {
            itemName.contains("harnois", ignoreCase = true) ->
                ArmorSpec("Harnois", 18, ArmorCategory.HEAVY)
            itemName.contains("demi-plate", ignoreCase = true) ||
                    itemName.contains("demi plate", ignoreCase = true) ->
                ArmorSpec("Demi-plate", 15, ArmorCategory.HEAVY)
            itemName.contains("clibanion", ignoreCase = true) ->
                ArmorSpec("Clibanion", 17, ArmorCategory.HEAVY)
            itemName.contains("cotte de mailles", ignoreCase = true) ->
                ArmorSpec("Cotte de mailles", 16, ArmorCategory.HEAVY)
            itemName.contains("broigne", ignoreCase = true) ->
                ArmorSpec("Broigne", 14, ArmorCategory.HEAVY)
            itemName.contains("plastron", ignoreCase = true) ||
                    itemName.contains("armure d'écailles", ignoreCase = true) ||
                    itemName.contains("armure d'ecailles", ignoreCase = true) ||
                    itemName.contains("armure d'anneaux", ignoreCase = true) ->
                ArmorSpec("Plastron", 14, ArmorCategory.HEAVY)
            itemName.contains("chemise de mailles", ignoreCase = true) ->
                ArmorSpec("Chemise de mailles", 13, ArmorCategory.MEDIUM, maxDexBonus = 2)
            itemName.contains("cotte de clous", ignoreCase = true) ->
                ArmorSpec("Cotte de clous", 13, ArmorCategory.MEDIUM, maxDexBonus = 2)
            itemName.contains("armure de peau", ignoreCase = true) ->
                ArmorSpec("Armure de peau", 12, ArmorCategory.MEDIUM, maxDexBonus = 2)
            itemName.contains("armure de cuir clouté", ignoreCase = true) ->
                ArmorSpec("Armure de cuir clouté", 13, ArmorCategory.LIGHT)
            itemName.contains("armure de cuir", ignoreCase = true) ->
                ArmorSpec("Armure de cuir", 11, ArmorCategory.LIGHT)
            itemName.contains("armure matelassée", ignoreCase = true) ||
                    itemName.contains("matelassée", ignoreCase = true) ||
                    itemName.contains("matelassee", ignoreCase = true) ->
                ArmorSpec("Armure matelassée", 11, ArmorCategory.LIGHT)
            else -> null
        }
    }

    /**
     * Détermine le slot naturel d'un item pour la gestion d'équipement V7.
     * Retourne null si l'item n'a pas de slot reconnu (place dans le sac).
     */
    fun slotForItem(itemName: String): EquipmentSlot? {
        val lower = itemName.lowercase()
        return when {
            armorSpec(itemName) != null -> EquipmentSlot.TORSO
            lower.contains("bouclier") -> EquipmentSlot.OFF_HAND
            isClothing(itemName) -> EquipmentSlot.CLOTHING
            // "Sac à dos" testé AVANT le reste des mots-clés BACK ci-dessous, qui incluait
            // autrefois "sac à dos" : le sac a désormais son propre emplacement dédié
            // (EquipmentSlot.BACKPACK), distinct de BACK (cape/manteau/carquois).
            isBackpackItem(itemName) -> EquipmentSlot.BACKPACK
            isPouchItem(itemName) -> EquipmentSlot.BELT_POUCH_1
            isTwoHandedWeapon(itemName) -> EquipmentSlot.MAIN_HAND
            contientMot(lower, "arc") ||
                    lower.contains("hallebarde") ||
                    contientMot(lower, "pique") ||
                    lower.contains("gourdin") ||
                    lower.contains("dague") ||
                    lower.contains("hache") ||
                    lower.contains("épée") ||
                    lower.contains("epee") ||
                    contientMot(lower, "lance") ||
                    lower.contains("marteau") ||
                    lower.contains("rapière") ||
                    lower.contains("arbalète") ||
                    contientMot(lower, "masse") ||
                    lower.contains("serpe") ||
                    lower.contains("trident") ||
                    lower.contains("couteau") ||
                    lower.contains("fléau") ||
                    lower.contains("fleau") ||
                    lower.contains("fléchette") ||
                    lower.contains("flechette") ||
                    lower.contains("javeline") ||
                    lower.contains("sarbacane") ||
                    lower.contains("filet") ||
                    lower.contains("bâton") ||
                    lower.contains("baton") ||
                    lower.contains("maillet") ||
                    lower.contains("morgenstern") ||
                    lower.contains("pic de guerre") ||
                    lower.contains("fouet") -> EquipmentSlot.MAIN_HAND
            lower.contains("casque") ||
                    lower.contains("chapeau") ||
                    lower.contains("bandeau") ||
                    lower.contains("diadème") ||
                    lower.contains("couronne") -> EquipmentSlot.HEAD
            lower.contains("cape") ||
                    lower.contains("manteau") ||
                    lower.contains("tapis") ||
                    lower.contains("carquois") -> EquipmentSlot.BACK
            lower.contains("anneau") ||
                    lower.contains("amulette") ||
                    lower.contains("collier") ||
                    lower.contains("bracelet") ||
                    lower.contains("ceinture") ||
                    lower.contains("gant") ||
                    lower.contains("botte") ||
                    lower.contains("bottine") -> EquipmentSlot.ACCESSORY
            else -> null
        }
    }

    /**
     * [mot] présent comme mot entier (au pluriel près) : évite que « arc » reconnaisse un
     * parchemin ou un focaliseur arcanique, ou « lance » une balance.
     */
    private fun contientMot(lower: String, mot: String): Boolean =
        Regex("""(^|[^\p{L}\d])${Regex.escape(mot)}[sx]?([^\p{L}\d]|$)""").containsMatchIn(lower)

    private fun isTwoHandedWeapon(itemName: String): Boolean {
        val lower = itemName.lowercase()
        val twoHandedKeywords = listOf("deux mains", "2 mains", "hallebarde", "arbalète")
        return twoHandedKeywords.any { lower.contains(it) } || listOf("arc", "pique", "2m").any { contientMot(lower, it) }
    }

    /**
     * Retourne true si l'item est une arme à deux mains.
     */
    fun twoHanded(itemName: String): Boolean = isTwoHandedWeapon(itemName)

    /** Vêtements (un seul emplacement, EquipmentSlot.CLOTHING) : "Beaux habits", "Robe", "Tenue de voyage"... */
    fun isClothing(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return listOf("vêtement", "vetement", "habits", "tenue", "robe").any { lower.contains(it) }
    }

    /** Le sac à dos lui-même (EquipmentSlot.BACKPACK), distinct des autres contenants. */
    fun isBackpackItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return lower.contains("sac à dos") || lower.contains("sac a dos")
    }

    /** Bourse/sacoche de ceinture (EquipmentSlot.BELT_POUCH_1/2 ou BACK_ACCESSORY_1..4). */
    fun isPouchItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return lower.contains("bourse") || (lower.contains("sacoche") && !lower.contains("composantes"))
    }

    /** Potion (peut être rangée sur un emplacement d'accessoire dans le dos). */
    fun isPotionItem(itemName: String): Boolean = itemName.lowercase().contains("potion")

    /**
     * Arme, y compris pour un emplacement supplémentaire dans le dos (BACK_WEAPON_1..3) —
     * délègue au même classement que slotForItem (MAIN_HAND) pour rester cohérent.
     */
    fun isWeaponItem(itemName: String): Boolean = slotForItem(itemName) == EquipmentSlot.MAIN_HAND

    /**
     * Arme de jet consommable (javeline, fléchette) : plusieurs exemplaires identiques
     * s'empilent sur un même emplacement d'arme de dos (cf. Character.slotStackCounts).
     */
    fun isStackableWeapon(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return listOf("javeline", "fléchette", "flechette").any { lower.contains(it) }
    }

    /** Carquois : porté sur l'emplacement BACK, il contient des flèches (cf. Character.quiverContents). */
    fun isQuiverItem(itemName: String): Boolean = itemName.lowercase().contains("carquois")

    /** Flèche (munition d'arc) rangeable dans un carquois — pas les fléchettes (armes de jet). */
    fun isArrowItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return (lower.contains("flèche") || lower.contains("fleche")) &&
            !lower.contains("fléchette") && !lower.contains("flechette")
    }

    /** Nombre maximal de flèches dans un carquois (SRD : 20). */
    const val QUIVER_CAPACITY = 20

    /**
     * L'objet est-il une munition du type [typeMunition] lu dans la propriété « Munitions (24/96 ;
     * flèches) » d'une arme (flèches, carreaux, billes, balles, dards) ? Les contenants (carquois,
     * étui pour carreaux) et les fléchettes (armes de jet) ne comptent pas.
     */
    fun estMunitionDe(itemName: String, typeMunition: String): Boolean {
        val lower = itemName.lowercase()
        if (listOf("carquois", "étui", "etui", "fléchette", "flechette").any { lower.contains(it) }) return false
        val type = typeMunition.lowercase().trim()
        val radical = when {
            type.startsWith("flèche") || type.startsWith("fleche") -> return isArrowItem(itemName)
            type.endsWith("eaux") -> type.removeSuffix("x")
            type.endsWith("s") -> type.removeSuffix("s")
            else -> type
        }
        return radical.isNotBlank() && lower.contains(radical)
    }

    /** Grimoire (livre de sorts du Magicien) : s'ouvre comme un contenant, cf. GrimoirePanel. */
    fun isGrimoireItem(itemName: String): Boolean = itemName.lowercase().contains("grimoire")

    fun isEncre(itemName: String): Boolean = itemName.lowercase().contains("encre")

    /** Parchemin vierge (pas un parchemin de sort, qui est un objet magique). */
    fun isParchemin(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return lower.contains("parchemin") && !lower.contains("sort") && !lower.contains("étui") && !lower.contains("etui")
    }

    fun isCalligraphie(itemName: String): Boolean = itemName.lowercase().let { it.contains("calligraph") || it.contains("porte-plume") }

    /** Matériel d'écriture rangeable dans le grimoire. */
    fun isGrimoireSupply(itemName: String): Boolean = isEncre(itemName) || isParchemin(itemName) || isCalligraphie(itemName)

    /**
     * Objet volumineux pouvant être arrimé sur un emplacement EXTÉRIEUR du sac à dos
     * (cf. Character.backpackExteriorSlots), ex. sac de couchage, corde.
     */
    fun isBackExteriorItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return listOf(
            "corde", "sac de couchage", "grappin", "tente", "natte", "hamac",
            "couverture", "bélier portable", "belier portable"
        ).any { lower.contains(it) }
    }

    /**
     * Focaliseur (arcanique, druidique, symbole sacré, sacoche à composantes) : aucune classe
     * ne dispose d'un emplacement dédié (choix produit, cf. discussion équipement) — juste un
     * type reconnu pour l'affichage (ex. étiquette dans le sac), toujours rangé comme un objet
     * de sac ordinaire tant qu'aucun emplacement ne lui est propre.
     */
    fun isFocusItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return lower.contains("focaliseur") ||
            lower.contains("symbole sacré") || lower.contains("symbole sacre") ||
            lower.contains("sacoche à composantes") || lower.contains("sacoche a composantes")
    }

    /**
     * Nom de l'objet tel qu'il existe dans la bibliothèque [nomsConnus] : lui-même s'il y
     * figure, sinon sa forme au singulier (« Dagues » → « Dague », « Javelines » → « Javeline »),
     * sinon inchangé. Les lots vendus au pluriel (« Flèches ») restent tels quels.
     */
    fun nomCanonique(nom: String, nomsConnus: Collection<String>): String {
        fun trouver(candidat: String) = nomsConnus.firstOrNull { it.equals(candidat, ignoreCase = true) }
        trouver(nom)?.let { return it }
        val mots = nom.trim().split(" ")
        fun singulier(mot: String) = when {
            mot.length > 3 && mot.endsWith("aux") -> mot.dropLast(3) + "al"
            mot.length > 2 && (mot.endsWith("s") || mot.endsWith("x")) -> mot.dropLast(1)
            else -> mot
        }
        val premierMot = (listOf(singulier(mots.first())) + mots.drop(1)).joinToString(" ")
        val tousLesMots = mots.joinToString(" ") { singulier(it) }
        return trouver(premierMot) ?: trouver(tousLesMots) ?: nom
    }

    /**
     * Un objet est-il autorisé sur cet emplacement précis ? Armes : mains et armes de dos
     * seulement, qui n'acceptent rien d'autre (sauf le bouclier en main secondaire). Tenues :
     * Vêtements seulement. Sac à dos : son emplacement dédié. Les autres emplacements (tête,
     * torse, dos, accessoire, utilitaires) acceptent tout le reste (potion, bourse, divers...).
     */
    fun itemAllowedInSlot(itemName: String, slot: EquipmentSlot): Boolean = when (slot) {
        EquipmentSlot.CLOTHING -> isClothing(itemName)
        EquipmentSlot.BACKPACK -> isBackpackItem(itemName)
        // Mains et armes de dos : des armes uniquement (plus le bouclier en main secondaire).
        EquipmentSlot.MAIN_HAND -> isWeaponItem(itemName)
        EquipmentSlot.OFF_HAND -> isWeaponItem(itemName) || slotForItem(itemName) == EquipmentSlot.OFF_HAND
        EquipmentSlot.BACK_WEAPON_1, EquipmentSlot.BACK_WEAPON_2, EquipmentSlot.BACK_WEAPON_3 -> isWeaponItem(itemName)
        // Les armes ne vont que dans les mains / le dos, les tenues que dans Vêtements.
        else -> !isWeaponItem(itemName) && !isClothing(itemName)
    }

    /**
     * Poids temporaires pour les tests sans contexte Android.
     */
    private val testWeightOverrides = mutableMapOf<String, Double>()

    /**
     * Remplace le poids d'un item pour les tests unitaires.
     */
    fun setTestWeight(itemName: String, pounds: Double) {
        testWeightOverrides[itemName] = pounds
    }

    /**
     * Efface tous les overrides de test.
     */
    fun clearTestWeights() {
        testWeightOverrides.clear()
    }

    /**
     * Point d'entrée public pour effacer les caches de tests.
     */
    fun clearCache() {
        SrdRepository.clearCache()
        clearTestWeights()
    }

    /**
     * Alias public de computeAc pour les tests et GameState.
     */
    fun effectiveArmorClass(character: Character): Int = computeAc(character).total

    /**
     * Alias public de computeAc pour le détail.
     */
    fun armorClassBreakdown(character: Character): AcBreakdown = computeAc(character)

    /**
     * Migre equippedItems legacy vers equippedSlots.
     */
    fun migrateEquippedItemsToSlots(character: Character): Character {
        val migratedSlots = character.equippedItems.mapNotNull { item ->
            slotForItem(item)?.let { slot -> slot to item }
        }.toMap()
        return character.copy(
            equippedSlots = character.equippedSlots + migratedSlots,
            equippedItems = character.equippedItems.filter { item ->
                val slot = slotForItem(item)
                slot == null || character.equippedSlots[slot] == item
            }
        )
    }

    /**
     * Retourne le poids en livres d'un item connu, ou null.
     */
    fun weightInPounds(itemName: String): Double? {
        testWeightOverrides[itemName]?.let { return it }
        val item = SrdRepository.findEquipmentItemCached(itemName)
        val poidsLot = item?.weight?.let { parseWeight(it) } ?: return null
        // Munitions vendues par lot ("Flèches", **Quantité** 20) : une entrée d'inventaire =
        // une unité (cf. parseListeEquipement, "20 flèches" -> 20 entrées), donc poids / lot.
        val quantiteLot = REGEX_QUANTITE_LOT.find(item.rawMarkdown)?.groupValues?.get(1)?.toIntOrNull()
        return if (quantiteLot != null && quantiteLot > 1) poidsLot / quantiteLot else poidsLot
    }

    private val REGEX_QUANTITE_LOT = Regex("""\*\*Quantité\*\*\s*(\d+)""")

    private fun parseWeight(weight: String): Double? {
        val grammes = Regex("""^([\d,.]+)\s*g$""").find(weight.trim())?.groupValues?.get(1)
        if (grammes != null) return grammes.replace(",", ".").toDoubleOrNull()?.let { it / 453.592 }
        return weight.trim()
            .replace(" lb", "", ignoreCase = true)
            .replace(" kg", "", ignoreCase = true)
            .replace("1/4", "0.25")
            .replace("1/2", "0.5")
            .replace(",", ".")
            .toDoubleOrNull()
            ?.let { if (weight.contains("kg", ignoreCase = true)) it / 0.453592 else it }
    }
}