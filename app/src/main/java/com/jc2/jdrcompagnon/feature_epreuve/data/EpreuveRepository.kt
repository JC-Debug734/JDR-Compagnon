package com.jc2.jdrcompagnon.feature_epreuve.data

import com.jc2.jdrcompagnon.feature_epreuve.data.local.EpreuveDao
import com.jc2.jdrcompagnon.feature_epreuve.data.local.EpreuveEntity
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.DifficulteEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface EpreuveRepository {
    fun observerEpreuves(worldId: String): Flow<List<Epreuve>>
    suspend fun getEpreuveParId(id: String): Epreuve?
    suspend fun sauvegarder(epreuve: Epreuve)
    suspend fun supprimer(id: String)
}

class EpreuveRepositoryImpl(private val dao: EpreuveDao) : EpreuveRepository {

    override fun observerEpreuves(worldId: String): Flow<List<Epreuve>> =
        dao.observerEpreuves(worldId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEpreuveParId(id: String): Epreuve? = dao.getEpreuveParId(id)?.toDomain()

    override suspend fun sauvegarder(epreuve: Epreuve) = dao.sauvegarder(epreuve.toEntity())

    override suspend fun supprimer(id: String) = dao.supprimer(id)
}

private val json = Json { ignoreUnknownKeys = true }

private fun EpreuveEntity.toDomain(): Epreuve = Epreuve(
    id = id,
    worldId = worldId,
    nom = nom,
    description = description,
    reussitesRequises = reussitesRequises,
    imageFileName = imageFileName,
    complications = runCatching { json.decodeFromString<List<ComplicationEpreuve>>(complicationsJson) }.getOrDefault(emptyList()),
    musicTrackId = musicTrackId,
    difficulte = difficulte?.let { nom -> DifficulteEpreuve.entries.firstOrNull { it.name == nom } },
)

private fun Epreuve.toEntity(): EpreuveEntity = EpreuveEntity(
    id = id,
    worldId = worldId,
    nom = nom,
    description = description,
    reussitesRequises = reussitesRequises,
    imageFileName = imageFileName,
    complicationsJson = json.encodeToString(complications.filter { it.titre.isNotBlank() }),
    musicTrackId = musicTrackId,
    difficulte = difficulte?.name,
)
