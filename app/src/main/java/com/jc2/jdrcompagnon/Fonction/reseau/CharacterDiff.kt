package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.ui.Character
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Une différence de champ entre la version locale d'un personnage et celle reçue du MJ.
 * [isListDiff] : pour une liste (équipement, sac, sorts...) ou un texte multiligne, [localValue] et
 * [remoteValue] ne contiennent que les éléments présents d'un seul côté, pas la liste entière.
 */
data class CharacterFieldDiff(
    val fieldLabel: String,
    val localValue: String,
    val remoteValue: String,
    val isListDiff: Boolean = false,
)

// Libellés lisibles pour les champs les plus significatifs d'un point de vue joueur ; les autres
// champs retombent sur leur nom brut (JSON) plutôt que de devoir énumérer les ~80 propriétés de
// Character une par une.
private val fieldLabels: Map<String, String> = mapOf(
    "name" to "Nom",
    "level" to "Niveau",
    "experience" to "Expérience",
    "gold" to "Or",
    "currentHitPoints" to "Points de vie actuels",
    "maxHitPoints" to "Points de vie max",
    "armorClass" to "Classe d'armure",
    "equipment" to "Équipement",
    "backpackItems" to "Sac à dos",
    "equippedItems" to "Objets équipés",
    "attunedItems" to "Objets harmonisés",
    "weapons" to "Armes",
    "armor" to "Armures",
    "spells" to "Sorts",
    "preparedSpells" to "Sorts préparés",
    "damageResistances" to "Résistances",
    "damageVulnerabilities" to "Vulnérabilités",
    "notes" to "Notes",
)

/**
 * Compare deux versions d'un même personnage (même id) champ par champ, via leur représentation
 * JSON — évite de lister manuellement toutes les propriétés de [Character] et reste correct si
 * le modèle évolue. Ne retient que ce qui diffère réellement : pour une liste, seuls les éléments
 * ajoutés/retirés ; pour un sous-objet, seules les clés modifiées.
 */
fun diffCharacters(local: Character, remote: Character): List<CharacterFieldDiff> {
    val localJson = networkJson.encodeToJsonElement(local) as? JsonObject ?: return emptyList()
    val remoteJson = networkJson.encodeToJsonElement(remote) as? JsonObject ?: return emptyList()
    val keys = localJson.keys + remoteJson.keys
    return keys.filter { it != "id" }
        .flatMap { key -> diffElement(fieldLabels[key] ?: key, localJson[key], remoteJson[key]) }
        .sortedBy { it.fieldLabel }
}

private fun diffElement(label: String, local: JsonElement?, remote: JsonElement?): List<CharacterFieldDiff> {
    if (local == remote) return emptyList()
    return when {
        // Comparaison sur l'élément complet (deux objets de même nom mais modifiés restent
        // détectés), affichage sur son nom lisible.
        local is JsonArray && remote is JsonArray -> {
            val displayByKey = (local + remote).associate { it.toString() to it.readableValue() }
            listOfNotNull(
                listDiff(label, local.map { it.toString() }, remote.map { it.toString() }) { displayByKey[it] ?: it }
            )
        }
        local is JsonObject && remote is JsonObject ->
            (local.keys + remote.keys).flatMap { key -> diffElement("$label › $key", local[key], remote[key]) }
        // Équipement libre saisi "Épée, bouclier, ..." : comparé objet par objet.
        label == fieldLabels["equipment"] ->
            listOfNotNull(listDiff(label, local.textItems(listSeparators), remote.textItems(listSeparators)))
        local.isMultilineText() || remote.isMultilineText() ->
            listOfNotNull(listDiff(label, local.textItems(lineSeparators), remote.textItems(lineSeparators)))
        else -> listOf(CharacterFieldDiff(label, local.readableValue(), remote.readableValue()))
    }
}

/** Diff d'ensembles (avec doublons) : null si seul l'ordre change. */
private fun listDiff(
    label: String,
    local: List<String>,
    remote: List<String>,
    display: (String) -> String = { it },
): CharacterFieldDiff? {
    val onlyLocal = local.minusOnce(remote)
    val onlyRemote = remote.minusOnce(local)
    if (onlyLocal.isEmpty() && onlyRemote.isEmpty()) return null
    return CharacterFieldDiff(
        fieldLabel = label,
        localValue = onlyLocal.joinToString(", ", transform = display).ifEmpty { "—" },
        remoteValue = onlyRemote.joinToString(", ", transform = display).ifEmpty { "—" },
        isListDiff = true,
    )
}

/** Retire de la liste chaque élément de [other] une seule fois (différence de multiensembles). */
private fun List<String>.minusOnce(other: List<String>): List<String> {
    val remaining = other.groupingBy { it }.eachCount().toMutableMap()
    return filter { item ->
        val count = remaining[item] ?: 0
        if (count > 0) {
            remaining[item] = count - 1
            false
        } else {
            true
        }
    }
}

private fun JsonElement?.isMultilineText(): Boolean =
    this is JsonPrimitive && isString && content.contains('\n')

private val lineSeparators = Regex("\r?\n")
private val listSeparators = Regex("[,;\r\n]")

private fun JsonElement?.textItems(separators: Regex): List<String> = when (this) {
    is JsonPrimitive -> content.split(separators).map { it.trim() }.filter { it.isNotEmpty() }
    null -> emptyList()
    else -> listOf(toString())
}

private fun JsonElement?.readableValue(): String = when (this) {
    null -> "—"
    is JsonPrimitive -> content
    is JsonObject -> (this["name"] as? JsonPrimitive)?.content ?: toString()
    else -> toString()
}
