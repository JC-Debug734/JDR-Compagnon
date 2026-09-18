package com.jc2.jdrcompagnon.feature_carte.data.mapper

import com.jc2.jdrcompagnon.feature_carte.data.local.CarteCampagneEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.EvenementAleatoireEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.LieuNotableEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.PointInteretEntity
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_carte.domain.model.EvenementAleatoire
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun PointInteretEntity.toDomain(): PointInteret = PointInteret(
    id = id,
    campagneId = campagneId,
    nom = nom,
    type = TypePointInteret.valueOf(type),
    x = x,
    y = y,
    description = description,
    boutiqueIds = if (boutiqueIds.isBlank()) emptyList() else boutiqueIds.split(","),
    scenarioIds = if (scenarioIds.isBlank()) emptyList() else scenarioIds.split(",")
)

fun PointInteret.toEntity(): PointInteretEntity = PointInteretEntity(
    id = id,
    campagneId = campagneId,
    nom = nom,
    type = type.name,
    x = x,
    y = y,
    description = description,
    boutiqueIds = boutiqueIds.joinToString(","),
    scenarioIds = scenarioIds.joinToString(",")
)

fun CarteCampagneEntity.toDomain(): CarteCampagne = CarteCampagne(
    campagneId = campagneId,
    largeurCases = largeurCases,
    hauteurCases = hauteurCases,
    echelleKmParCase = echelleKmParCase,
    imageFileName = imageFileName
)

fun CarteCampagne.toEntity(): CarteCampagneEntity = CarteCampagneEntity(
    campagneId = campagneId,
    largeurCases = largeurCases,
    hauteurCases = hauteurCases,
    echelleKmParCase = echelleKmParCase,
    imageFileName = imageFileName
)

/** DTO plat pour sérialiser EffetEvenement (sealed) sans configurer de module polymorphique kotlinx.serialization. */
@Serializable
private data class EffetEvenementDto(
    val kind: String, // "REPUTATION" ou "INFO"
    val factionNom: String? = null,
    val delta: Int? = null,
    val texte: String? = null
)

private fun EffetEvenement.toDto(): EffetEvenementDto = when (this) {
    is EffetEvenement.GainReputation -> EffetEvenementDto(kind = "REPUTATION", factionNom = factionNom, delta = delta)
    is EffetEvenement.Information -> EffetEvenementDto(kind = "INFO", texte = texte)
}

private fun EffetEvenementDto.toDomain(): EffetEvenement = when (kind) {
    "REPUTATION" -> EffetEvenement.GainReputation(factionNom = factionNom.orEmpty(), delta = delta ?: 0)
    else -> EffetEvenement.Information(texte = texte.orEmpty())
}

fun EvenementAleatoireEntity.toDomain(): EvenementAleatoire = EvenementAleatoire(
    id = id,
    campagneId = campagneId,
    villeId = villeId,
    titre = titre,
    description = description,
    effets = json.decodeFromString<List<EffetEvenementDto>>(effetsJson).map { it.toDomain() }
)

fun EvenementAleatoire.toEntity(): EvenementAleatoireEntity = EvenementAleatoireEntity(
    id = id,
    campagneId = campagneId ?: error("Un événement personnalisé doit avoir un campagneId non nul avant sauvegarde."),
    villeId = villeId,
    titre = titre,
    description = description,
    effetsJson = json.encodeToString(effets.map { it.toDto() })
)

fun LieuNotableEntity.toDomain(): LieuNotable = LieuNotable(id = id, villeId = villeId, nom = nom, description = description)

fun LieuNotable.toEntity(): LieuNotableEntity = LieuNotableEntity(id = id, villeId = villeId, nom = nom, description = description)
