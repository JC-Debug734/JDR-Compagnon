package com.jc2.jdrcompagnon.feature_carte.data.mapper

import com.jc2.jdrcompagnon.feature_carte.data.local.CarteCampagneEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.EvenementAleatoireEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.LieuNotableEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.PointInteretEntity
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_evenement.domain.model.CategorieEffet
import com.jc2.jdrcompagnon.feature_evenement.domain.model.EffetEvenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_carte.domain.model.LieuNotable
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs

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
    scenarioIds = if (scenarioIds.isBlank()) emptyList() else scenarioIds.split(","),
    iconKey = iconKey,
    couleurArgb = couleurArgb,
    carteId = carteId,
    fx = fx,
    fy = fy,
    visibleJoueurs = visibleJoueurs,
    evenementIds = if (evenementIds.isBlank()) emptyList() else evenementIds.split(",")
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
    scenarioIds = scenarioIds.joinToString(","),
    iconKey = iconKey,
    couleurArgb = couleurArgb,
    carteId = carteId,
    fx = fx,
    fy = fy,
    visibleJoueurs = visibleJoueurs,
    evenementIds = evenementIds.joinToString(",")
)

fun CarteCampagneEntity.toDomain(): CarteCampagne = CarteCampagne(
    id = id,
    nom = nom,
    campagneId = campagneId,
    largeurCases = largeurCases,
    hauteurCases = hauteurCases,
    echelleKmParCase = echelleKmParCase,
    imageFileName = imageFileName
)

fun CarteCampagne.toEntity(): CarteCampagneEntity = CarteCampagneEntity(
    id = id,
    nom = nom,
    campagneId = campagneId,
    largeurCases = largeurCases,
    hauteurCases = hauteurCases,
    echelleKmParCase = echelleKmParCase,
    imageFileName = imageFileName
)

/** Format JSON des effets des anciens événements de campagne (voir EvenementAleatoireEntity). */
@Serializable
private data class AncienEffetDto(
    val kind: String, // "REPUTATION" ou "INFO"
    val factionNom: String? = null,
    val delta: Int? = null,
    val texte: String? = null
)

/**
 * Convertit un ancien événement de campagne/ville en événement de la bibliothèque, propre à sa
 * campagne. Les anciens événements n'avaient pas de type : une réputation gagnée en fait une
 * Opportunité, une réputation perdue une Complication, sinon une Rencontre (modifiable ensuite).
 */
fun EvenementAleatoireEntity.versBibliotheque(worldId: String): Evenement {
    val anciens = runCatching { json.decodeFromString<List<AncienEffetDto>>(effetsJson) }.getOrDefault(emptyList())
    val effets = anciens.map { dto ->
        if (dto.kind == "REPUTATION") {
            val delta = dto.delta ?: 0
            EffetEvenement(categorie = CategorieEffet.REPUTATION, gain = delta >= 0, quantite = abs(delta), cible = dto.factionNom.orEmpty())
        } else {
            EffetEvenement(categorie = CategorieEffet.AUTRE, description = dto.texte.orEmpty())
        }
    }.filterNot { it.categorie == CategorieEffet.AUTRE && it.description.isBlank() }
    val reputations = anciens.filter { it.kind == "REPUTATION" }.map { it.delta ?: 0 }
    val type = when {
        reputations.any { it < 0 } -> TypeEvenement.COMPLICATION
        reputations.any { it > 0 } -> TypeEvenement.OPPORTUNITE
        else -> TypeEvenement.RENCONTRE
    }
    return Evenement(
        id = id,
        worldId = worldId,
        campagneId = campagneId,
        type = type,
        titre = titre,
        description = description,
        effets = effets
    )
}

fun LieuNotableEntity.toDomain(): LieuNotable = LieuNotable(id = id, villeId = villeId, nom = nom, description = description)

fun LieuNotable.toEntity(): LieuNotableEntity = LieuNotableEntity(id = id, villeId = villeId, nom = nom, description = description)
