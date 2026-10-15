package com.jc2.jdrcompagnon.feature_epreuve.data

import android.content.Context
import android.util.Base64
import com.jc2.jdrcompagnon.di.EpreuveDependencies
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.DifficulteEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ReglesEpreuve
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.availableLoopTracks

/**
 * Import d'épreuves environnementales depuis un fichier markdown (outil IMPORT, voir
 * ImportCentral). Même syntaxe de lignes méta que les scénarios (GameState.metaLineFields :
 * champs séparés par `;`, `\;` pour un point-virgule, `\n` pour un retour à la ligne) :
 *
 * - `{mepreuve: nom=Col des Géants; difficulte=difficile; image=tempete; musique=shadowcharge; description=...}`
 *   ouvre une épreuve (`difficulte` : facile, moyenne, difficile ou mortelle, réussites et échecs
 *   adaptés au groupe ; sinon `reussites=5; echecs=3` fixes — `musique` : id ou nom d'une piste,
 *   `image` : nom d'un `{mimage:}`). Issues : `reussite=...` (épreuve surmontée), `echec=...`
 *   (trop d'échecs) et `combat=Kraken x1, Sahuagin x4` (combat final proposé si l'épreuve est
 *   perdue). Les textes acceptent les variables de ReglesEpreuve.adapter (`{DD}`, `{degats}`...) ;
 * - `{mcomplication: titre=Avalanche; description=...}` : complication de l'épreuve qui précède ;
 * - `{mimage: nom=tempete; data=<PNG/JPEG/WebP en base64>}` : image embarquée.
 *
 * Tout le reste du fichier (titres, notes du MJ) est ignoré. Une épreuve du même nom dans le
 * même monde est remplacée (réimporter un fichier corrigé ne crée pas de doublon).
 */
object EpreuveImport {

    private class Brouillon(val champs: Map<String, String>) {
        val complications = mutableListOf<ComplicationEpreuve>()
    }

    fun contientEpreuves(texte: String): Boolean =
        texte.lineSequence().any { GameState.metaLineFields(it, "mepreuve") != null }

    /** Retourne les noms des épreuves importées. */
    suspend fun importer(context: Context, texte: String, worldId: String): List<String> {
        val images = mutableMapOf<String, String>()
        val brouillons = mutableListOf<Brouillon>()
        texte.lineSequence().forEach { line ->
            GameState.metaLineFields(line, "mepreuve")?.let { brouillons += Brouillon(it) }
            GameState.metaLineFields(line, "mcomplication")?.let { f ->
                val titre = f["titre"]?.takeIf { it.isNotBlank() } ?: return@let
                brouillons.lastOrNull()?.complications?.add(ComplicationEpreuve(titre, f["description"].orEmpty()))
            }
            GameState.metaLineFields(line, "mimage")?.let { f ->
                val nom = f["nom"]?.takeIf { it.isNotBlank() }
                val data = f["data"]?.takeIf { it.isNotBlank() }
                if (nom != null && data != null) images[nom] = data
            }
        }

        val existantes = EpreuveDependencies.epreuvesDuMonde(worldId)
        return brouillons.mapNotNull { b ->
            val nom = b.champs["nom"]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val ancienne = existantes.firstOrNull { it.nom.equals(nom, ignoreCase = true) }
            val id = ancienne?.id ?: java.util.UUID.randomUUID().toString()
            val image = b.champs["image"]?.let { images[it] }?.let { base64 ->
                runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
                    ?.let { bytes -> EpreuveImageStore.ecrireOctets(context, id, bytes) }
            }
            if (image != null) EpreuveImageStore.supprimer(context, ancienne?.imageFileName)
            EpreuveDependencies.repository.sauvegarder(
                Epreuve(
                    id = id,
                    worldId = worldId,
                    nom = nom,
                    description = b.champs["description"].orEmpty(),
                    reussitesRequises = (b.champs["reussites"]?.trim()?.toIntOrNull() ?: 3)
                        .coerceIn(ReglesEpreuve.REUSSITES_MIN, ReglesEpreuve.REUSSITES_MAX),
                    imageFileName = image ?: ancienne?.imageFileName,
                    echecsMax = (b.champs["echecs"]?.trim()?.toIntOrNull() ?: ReglesEpreuve.ECHECS_DEFAUT)
                        .coerceIn(ReglesEpreuve.ECHECS_MIN, ReglesEpreuve.ECHECS_MAX),
                    complications = b.complications,
                    issueReussite = b.champs["reussite"].orEmpty(),
                    issueEchec = b.champs["echec"].orEmpty(),
                    combatEchec = b.champs["combat"].orEmpty(),
                    difficulte = b.champs["difficulte"]?.trim()?.let { d ->
                        DifficulteEpreuve.entries.firstOrNull { it.name.equals(d, ignoreCase = true) || it.libelle.equals(d, ignoreCase = true) }
                    },
                    musicTrackId = b.champs["musique"]?.trim()?.takeIf { it.isNotEmpty() }?.let { m ->
                        availableLoopTracks.firstOrNull { it.id == m || it.displayName.equals(m, ignoreCase = true) }?.id
                    },
                )
            )
            nom
        }
    }
}
