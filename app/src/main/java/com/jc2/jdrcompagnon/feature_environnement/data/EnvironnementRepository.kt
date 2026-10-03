package com.jc2.jdrcompagnon.feature_environnement.data

import com.jc2.jdrcompagnon.feature_environnement.data.local.EnvironnementDao
import com.jc2.jdrcompagnon.feature_environnement.data.local.EnvironnementEntity
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import com.jc2.jdrcompagnon.feature_environnement.domain.model.LootEntry
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface EnvironnementRepository {
    fun observerEnvironnements(worldId: String): Flow<List<Environnement>>
    suspend fun getEnvironnementParId(id: String): Environnement?
    suspend fun sauvegarder(environnement: Environnement)
    suspend fun supprimer(id: String)
    suspend fun compterPourMonde(worldId: String): Int
}

/**
 * Les rumeurs et rencontres en texte libre (antérieures à la bibliothèque d'événements) sont
 * converties en événements de la bibliothèque — Rumeur / Indice et Rencontre — rattachés à
 * l'environnement : à la première lecture pour les données existantes, à l'écriture pour celles
 * fournies en texte (exemples pré-remplis, voir EnvironmentDependencies).
 */
class EnvironnementRepositoryImpl(
    private val dao: EnvironnementDao,
    private val evenementRepository: EvenementRepository,
) : EnvironnementRepository {

    // Même garde-fou que TableAleatoireRepositoryImpl : pas de double conversion en parallèle.
    private val conversionMutex = Mutex()

    override fun observerEnvironnements(worldId: String): Flow<List<Environnement>> =
        dao.observerEnvironnements(worldId).map { entities -> entities.map { convertirSiAncien(it).toDomain() } }

    override suspend fun getEnvironnementParId(id: String): Environnement? =
        dao.getEnvironnementParId(id)?.let { convertirSiAncien(it) }?.toDomain()

    override suspend fun sauvegarder(environnement: Environnement) {
        val entity = environnement.toEntity()
        if (aConvertir(entity)) conversionMutex.withLock { convertir(entity) } else dao.sauvegarder(entity)
    }

    override suspend fun supprimer(id: String) = dao.supprimer(id)

    override suspend fun compterPourMonde(worldId: String): Int = dao.compterPourMonde(worldId)

    private fun aConvertir(entity: EnvironnementEntity): Boolean =
        decoderTextes(entity.rumeursJson).isNotEmpty() || decoderTextes(entity.rencontresJson).isNotEmpty()

    private suspend fun convertirSiAncien(entity: EnvironnementEntity): EnvironnementEntity {
        if (!aConvertir(entity)) return entity
        return conversionMutex.withLock { convertir(dao.getEnvironnementParId(entity.id) ?: entity) }
    }

    private suspend fun convertir(entity: EnvironnementEntity): EnvironnementEntity {
        if (!aConvertir(entity)) return entity
        val evenements = decoderTextes(entity.rumeursJson).map { texte -> evenementDepuisTexte(entity.worldId, TypeEvenement.RUMEUR_INDICE, texte) } +
            decoderTextes(entity.rencontresJson).map { texte -> evenementDepuisTexte(entity.worldId, TypeEvenement.RENCONTRE, texte) }
        evenementRepository.sauvegarderTous(evenements)
        val ids = decoderTextes(entity.evenementsJson) + evenements.map { it.id }
        val convertie = entity.copy(rumeursJson = "[]", rencontresJson = "[]", evenementsJson = json.encodeToString(ids))
        dao.sauvegarder(convertie)
        return convertie
    }
}

/**
 * Événement tiré d'une ancienne ligne de texte : la phrase entière devient la description, le
 * titre en est le début (jusqu'à la première ponctuation, 48 caractères au plus).
 */
internal fun evenementDepuisTexte(worldId: String, type: TypeEvenement, texte: String): Evenement {
    val propre = texte.trim()
    val debut = propre.split('.', '!', '?', ',', ':', ';').first().trim().ifBlank { propre }
    val titre = if (debut.length <= 48) debut else debut.take(48).substringBeforeLast(' ').trimEnd() + "…"
    return Evenement(worldId = worldId, type = type, titre = titre.ifBlank { type.label }, description = propre)
}

@Serializable
private data class LootEntryDto(val nomObjet: String, val poids: Int = 1)

private val json = Json { ignoreUnknownKeys = true }

private fun decoderTextes(texte: String): List<String> =
    runCatching { json.decodeFromString<List<String>>(texte) }.getOrDefault(emptyList()).filter { it.isNotBlank() }

private fun EnvironnementEntity.toDomain(): Environnement = Environnement(
    id = id,
    nom = nom,
    description = description,
    worldId = worldId,
    imageFileName = imageFileName,
    musicTrackId = musicTrackId,
    monstresIds = runCatching { json.decodeFromString<List<String>>(monstresJson) }.getOrDefault(emptyList()),
    tableButin = runCatching { json.decodeFromString<List<LootEntryDto>>(butinJson) }.getOrDefault(emptyList())
        .map { LootEntry(it.nomObjet, it.poids) },
    epreuves = runCatching { json.decodeFromString<List<EpreuveEnvironnementale>>(epreuvesJson) }.getOrDefault(emptyList()),
    terrains = runCatching { json.decodeFromString<List<String>>(terrainsJson) }.getOrDefault(emptyList()),
    tablesAleatoiresIds = runCatching { json.decodeFromString<List<String>>(tablesAleatoiresJson) }.getOrDefault(emptyList()),
    evenementIds = decoderTextes(evenementsJson)
)

private fun Environnement.toEntity(): EnvironnementEntity = EnvironnementEntity(
    id = id,
    nom = nom,
    description = description,
    worldId = worldId,
    imageFileName = imageFileName,
    musicTrackId = musicTrackId,
    rumeursJson = json.encodeToString(rumeurs),
    rencontresJson = json.encodeToString(rencontresAleatoires),
    monstresJson = json.encodeToString(monstresIds),
    butinJson = json.encodeToString(tableButin.map { LootEntryDto(it.nomObjet, it.poids) }),
    epreuvesJson = json.encodeToString(epreuves),
    terrainsJson = json.encodeToString(terrains),
    tablesAleatoiresJson = json.encodeToString(tablesAleatoiresIds),
    evenementsJson = json.encodeToString(evenementIds.distinct())
)
