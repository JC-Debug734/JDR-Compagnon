package com.jc2.jdrcompagnon.feature_evenement.data

import com.jc2.jdrcompagnon.feature_evenement.data.local.EvenementDao
import com.jc2.jdrcompagnon.feature_evenement.data.local.EvenementEntity
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.IssueEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.NatureIssue
import com.jc2.jdrcompagnon.feature_evenement.domain.model.ProfilEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeProfil
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface EvenementRepository {
    fun observerEvenements(worldId: String): Flow<List<Evenement>>
    suspend fun getEvenement(id: String): Evenement?
    /** Événements référencés par [ids], dans l'ordre de [ids] ; les ids inconnus sont ignorés. */
    suspend fun getEvenements(ids: List<String>): List<Evenement>
    suspend fun sauvegarder(evenement: Evenement)
    suspend fun sauvegarderTous(evenements: List<Evenement>)
    suspend fun supprimer(id: String)
    suspend fun compterPourMonde(worldId: String): Int
}

class EvenementRepositoryImpl(
    private val dao: EvenementDao
) : EvenementRepository {

    override fun observerEvenements(worldId: String): Flow<List<Evenement>> =
        dao.observerEvenements(worldId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEvenement(id: String): Evenement? = dao.getParId(id)?.toDomain()

    override suspend fun getEvenements(ids: List<String>): List<Evenement> {
        if (ids.isEmpty()) return emptyList()
        val parId = dao.getParIds(ids).associateBy { it.id }
        return ids.mapNotNull { parId[it]?.toDomain() }
    }

    override suspend fun sauvegarder(evenement: Evenement) = dao.sauvegarder(evenement.toEntity())

    override suspend fun sauvegarderTous(evenements: List<Evenement>) =
        dao.sauvegarderTous(evenements.map { it.toEntity() })

    override suspend fun supprimer(id: String) = dao.supprimer(id)

    override suspend fun compterPourMonde(worldId: String): Int = dao.compterPourMonde(worldId)
}

@Serializable
private data class EffetEvenementDto(
    val id: String,
    val categorie: String,
    val gain: Boolean = true,
    val quantite: Int = 1,
    val cible: String = "",
    val description: String = ""
)

@Serializable
private data class IssueEvenementDto(
    val id: String,
    val nature: String,
    val titre: String = "",
    val description: String = "",
    val effets: List<EffetEvenementDto> = emptyList(),
    val declencheCombat: Boolean = false
)

@Serializable
private data class ProfilEvenementDto(val type: String, val nom: String, val quantite: Int = 1)

private val json = Json { ignoreUnknownKeys = true }

private fun EffetEvenementDto.toDomain() = EffetEvenement(
    id = id,
    categorie = runCatching { CategorieEffet.valueOf(categorie) }.getOrDefault(CategorieEffet.AUTRE),
    gain = gain,
    quantite = quantite,
    cible = cible,
    description = description
)

private fun EffetEvenement.toDto() = EffetEvenementDto(id, categorie.name, gain, quantite, cible, description)

private fun EvenementEntity.toDomain(): Evenement = Evenement(
    id = id,
    worldId = worldId,
    campagneId = campagneId,
    type = runCatching { TypeEvenement.valueOf(type) }.getOrDefault(TypeEvenement.RENCONTRE),
    titre = titre,
    description = description,
    effets = runCatching { json.decodeFromString<List<EffetEvenementDto>>(effetsJson) }.getOrDefault(emptyList())
        .map { it.toDomain() },
    objectif = objectif,
    issues = runCatching { json.decodeFromString<List<IssueEvenementDto>>(issuesJson) }.getOrDefault(emptyList())
        .map { dto ->
            IssueEvenement(
                id = dto.id,
                nature = runCatching { NatureIssue.valueOf(dto.nature) }.getOrDefault(NatureIssue.AUTRE),
                titre = dto.titre,
                description = dto.description,
                effets = dto.effets.map { it.toDomain() },
                declencheCombat = dto.declencheCombat
            )
        },
    profils = runCatching { json.decodeFromString<List<ProfilEvenementDto>>(profilsJson) }.getOrDefault(emptyList())
        .map { dto ->
            ProfilEvenement(
                type = runCatching { TypeProfil.valueOf(dto.type) }.getOrDefault(TypeProfil.MONSTRE),
                nom = dto.nom,
                quantite = dto.quantite.coerceAtLeast(1)
            )
        }
)

private fun Evenement.toEntity(): EvenementEntity = EvenementEntity(
    id = id,
    worldId = worldId,
    campagneId = campagneId,
    type = type.name,
    titre = titre,
    description = description,
    effetsJson = json.encodeToString(effets.map { it.toDto() }),
    objectif = objectif,
    issuesJson = json.encodeToString(
        issues.map { IssueEvenementDto(it.id, it.nature.name, it.titre, it.description, it.effets.map { effet -> effet.toDto() }, it.declencheCombat) }
    ),
    profilsJson = json.encodeToString(profils.map { ProfilEvenementDto(it.type.name, it.nom, it.quantite) })
)
