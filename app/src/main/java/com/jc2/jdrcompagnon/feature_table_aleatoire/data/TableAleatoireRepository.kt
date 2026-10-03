package com.jc2.jdrcompagnon.feature_table_aleatoire.data

import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.local.TableAleatoireDao
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.local.TableAleatoireEntity
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface TableAleatoireRepository {
    fun observerTables(worldId: String): Flow<List<TableAleatoire>>
    fun observerTablesParType(worldId: String, type: TypeTable): Flow<List<TableAleatoire>>
    suspend fun getTable(id: String): TableAleatoire?
    suspend fun sauvegarder(table: TableAleatoire)
    suspend fun supprimer(id: String)
    suspend fun compterPourMonde(worldId: String): Int
    suspend fun reinitialiserDeclenchements()
}

/**
 * Les entrées d'événements ne stockent qu'une référence (evenementId + poids) vers la
 * bibliothèque (feature_evenement) ; l'événement est résolu à chaque lecture, ce qui répercute
 * immédiatement une modification faite depuis la bibliothèque. Les entrées antérieures à la
 * bibliothèque (titre/description/effets recopiés dans la table) sont converties à la première
 * lecture : un événement est créé par entrée, puis la table est réécrite avec les références.
 */
class TableAleatoireRepositoryImpl(
    private val dao: TableAleatoireDao,
    private val evenementRepository: EvenementRepository,
    // Complète un événement issu d'une ancienne entrée avec son modèle d'exemple éventuel
    // (EvenementDependencies.enrichirDepuisModele) ; identité par défaut (tests).
    private val enrichir: (Evenement) -> Evenement = { it },
) : TableAleatoireRepository {

    override fun observerTables(worldId: String): Flow<List<TableAleatoire>> =
        combine(dao.observerTables(worldId), evenementRepository.observerEvenements(worldId)) { entities, evenements ->
            val parId = evenements.associateBy { it.id }
            entities.map { convertirAnciennesEntrees(it).toDomain(parId) }
        }

    override fun observerTablesParType(worldId: String, type: TypeTable): Flow<List<TableAleatoire>> =
        combine(dao.observerTablesParType(worldId, type.name), evenementRepository.observerEvenements(worldId)) { entities, evenements ->
            val parId = evenements.associateBy { it.id }
            entities.map { convertirAnciennesEntrees(it).toDomain(parId) }
        }

    override suspend fun getTable(id: String): TableAleatoire? {
        val entity = dao.getTableParId(id)?.let { convertirAnciennesEntrees(it) } ?: return null
        val ids = decoderEntrees(entity.entreesEvenementsJson).mapNotNull { it.evenementId }
        val parId = evenementRepository.getEvenements(ids).associateBy { it.id }
        return entity.toDomain(parId)
    }

    override suspend fun sauvegarder(table: TableAleatoire) {
        dao.sauvegarder(table.toEntity())
    }

    override suspend fun supprimer(id: String) = dao.supprimer(id)

    override suspend fun compterPourMonde(worldId: String): Int = dao.compterPourMonde(worldId)

    override suspend fun reinitialiserDeclenchements() = dao.reinitialiserDeclenchements()

    // Une même table peut être lue en parallèle (liste observée + détail) : sans verrou, chaque
    // lecture convertirait la table et créerait ses événements en double.
    private val conversionMutex = Mutex()

    /** Sans effet (et sans écriture) si toutes les entrées sont déjà des références. */
    private suspend fun convertirAnciennesEntrees(entity: TableAleatoireEntity): TableAleatoireEntity {
        if (decoderEntrees(entity.entreesEvenementsJson).none { it.evenementId == null }) return entity
        return conversionMutex.withLock { convertir(dao.getTableParId(entity.id) ?: entity) }
    }

    private suspend fun convertir(entity: TableAleatoireEntity): TableAleatoireEntity {
        val entrees = decoderEntrees(entity.entreesEvenementsJson)
        if (entrees.none { it.evenementId == null }) return entity

        val crees = mutableListOf<Evenement>()
        val converties = entrees.mapNotNull { dto ->
            if (dto.evenementId != null) return@mapNotNull dto
            val titre = dto.titre?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val evenement = Evenement(
                worldId = entity.worldId,
                type = dto.type?.let { runCatching { TypeEvenement.valueOf(it) }.getOrNull() } ?: TypeEvenement.RENCONTRE,
                titre = titre,
                description = dto.description,
                effets = dto.effets.map { it.toDomain() }
            )
            crees += enrichir(evenement)
            EntreeEvenementDto(id = dto.id, evenementId = evenement.id, poids = dto.poids)
        }
        evenementRepository.sauvegarderTous(crees)
        val convertie = entity.copy(entreesEvenementsJson = json.encodeToString(converties))
        dao.sauvegarder(convertie)
        return convertie
    }
}

@Serializable
private data class EffetEvenementDto(
    val id: String = UUID.randomUUID().toString(),
    val categorie: String = CategorieEffet.AUTRE.name,
    val gain: Boolean = true,
    val quantite: Int = 1,
    val cible: String = "",
    val description: String = ""
) {
    fun toDomain() = EffetEvenement(
        id = id,
        categorie = runCatching { CategorieEffet.valueOf(categorie) }.getOrDefault(CategorieEffet.AUTRE),
        gain = gain,
        quantite = quantite,
        cible = cible,
        description = description
    )
}

/**
 * Format actuel : [id], [evenementId], [poids]. Les autres champs n'existent que dans l'ancien
 * format (événement recopié dans la table) et ne servent qu'à sa conversion.
 */
@Serializable
private data class EntreeEvenementDto(
    val id: String = UUID.randomUUID().toString(),
    val evenementId: String? = null,
    val poids: Int = 1,
    val type: String? = null,
    val titre: String? = null,
    val description: String = "",
    val effets: List<EffetEvenementDto> = emptyList()
)

@Serializable
private data class EntreeLootDto(
    val id: String,
    val equipementNom: String,
    val poids: Int = 1,
    val quantiteMin: Int = 1,
    val quantiteMax: Int = 1
)

private val json = Json { ignoreUnknownKeys = true }

private fun decoderEntrees(texte: String): List<EntreeEvenementDto> =
    runCatching { json.decodeFromString<List<EntreeEvenementDto>>(texte) }.getOrDefault(emptyList())

private fun TableAleatoireEntity.toDomain(evenements: Map<String, Evenement>): TableAleatoire = TableAleatoire(
    id = id,
    nom = nom,
    worldId = worldId,
    type = runCatching { TypeTable.valueOf(type) }.getOrDefault(TypeTable.EVENEMENTS),
    intervalleHeures = intervalleHeures,
    active = active,
    derniereDeclenchementMinutes = derniereDeclenchementMinutes,
    entreesEvenements = decoderEntrees(entreesEvenementsJson).mapNotNull { dto ->
        val evenementId = dto.evenementId ?: return@mapNotNull null
        EntreeEvenement(id = dto.id, evenementId = evenementId, poids = dto.poids, evenement = evenements[evenementId])
    },
    entreesLoot = runCatching { json.decodeFromString<List<EntreeLootDto>>(entreesLootJson) }.getOrDefault(emptyList())
        .map { dto -> EntreeLoot(dto.id, dto.equipementNom, dto.poids, dto.quantiteMin, dto.quantiteMax) }
)

private fun TableAleatoire.toEntity(): TableAleatoireEntity = TableAleatoireEntity(
    id = id,
    nom = nom,
    worldId = worldId,
    type = type.name,
    intervalleHeures = intervalleHeures,
    active = active,
    derniereDeclenchementMinutes = derniereDeclenchementMinutes,
    entreesEvenementsJson = json.encodeToString(
        entreesEvenements.map { EntreeEvenementDto(id = it.id, evenementId = it.evenementId, poids = it.poids) }
    ),
    entreesLootJson = json.encodeToString(
        entreesLoot.map { EntreeLootDto(it.id, it.equipementNom, it.poids, it.quantiteMin, it.quantiteMax) }
    )
)
