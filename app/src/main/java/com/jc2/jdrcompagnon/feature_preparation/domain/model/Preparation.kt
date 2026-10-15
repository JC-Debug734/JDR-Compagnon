package com.jc2.jdrcompagnon.feature_preparation.domain.model

import kotlinx.serialization.Serializable

/**
 * Carnet du MJ pour un monde : ses notes ne dépendent d'aucune campagne (plusieurs campagnes
 * peuvent se jouer dans le même monde et partager PNJ, secrets et intrigues).
 */
@Serializable
data class CarnetMonde(
    val secrets: List<SecretIndice> = emptyList(),
    val fils: List<FilEnSuspens> = emptyList(),
    val preparation: PreparationSeance = PreparationSeance(),
    val seances: List<SeanceJouee> = emptyList(),
)

/**
 * Secret ou indice que les joueurs peuvent découvrir (méthode « Lazy DM ») : rattaché à des PNJ
 * qui le connaissent et, en option, à une campagne, un scénario et un lieu. [decouvert] est coché
 * quand les joueurs l'apprennent ; [decouvertSeance] garde le numéro de la séance où c'est arrivé.
 */
@Serializable
data class SecretIndice(
    val id: String = java.util.UUID.randomUUID().toString(),
    val texte: String,
    val decouvert: Boolean = false,
    val decouvertSeance: Int? = null,
    val pnjIds: List<String> = emptyList(),
    val campagneId: String? = null,
    val scenarioId: String? = null,
    val lieu: String = "",
)

/**
 * Fil en suspens : promesse faite, PNJ vexé, piste laissée de côté... tout ce qui doit revenir
 * plus tard. Reste ouvert d'une séance à l'autre jusqu'à ce que le MJ le marque [resolu].
 */
@Serializable
data class FilEnSuspens(
    val id: String = java.util.UUID.randomUUID().toString(),
    val texte: String,
    val resolu: Boolean = false,
    val ouvertSeance: Int? = null,
    val pnjIds: List<String> = emptyList(),
    val campagneId: String? = null,
)

/** Nature d'une entrée de l'agenda de séance. */
@Serializable
enum class TypeAgenda(val label: String, val emoji: String) {
    SCENARIO("Scénario", "📖"),
    COMBAT("Combat", "⚔️"),
    BOSS("Boss", "👑"),
    EPREUVE("Épreuve", "🧗"),
    ENVIRONNEMENT("Environnement", "🌲"),
    LIEU("Lieu", "📍"),
    BOUTIQUE("Boutique", "🛒"),
    PNJ("PNJ", "👤"),
    NOTE("Note", "📝"),
}

/**
 * Entrée de l'agenda : ce qui peut se jouer pendant la séance, dans l'ordre prévu. [refId] désigne
 * l'élément de l'appli (scénario, épreuve, environnement, ville, boutique, PNJ) ; [libelle] garde
 * son nom pour l'affichage. Combat : [libelle] = composition (`Gobelin x3, Loup x2`, comme les
 * liens `#combat:`). Lieu : [detail] = campagne de la ville (pour ouvrir sa fiche), [refId] null
 * pour un lieu saisi librement. Note : [libelle] = texte libre.
 */
@Serializable
data class EntreeAgenda(
    val id: String = java.util.UUID.randomUUID().toString(),
    val type: TypeAgenda,
    val libelle: String,
    val refId: String? = null,
    val detail: String = "",
    val fait: Boolean = false,
)

/**
 * Fiche de préparation de la prochaine séance [numero] : un [agenda] ordonné qui pointe vers les
 * éléments de l'appli, les secrets à placer ([secretIds]) et les récompenses prévues.
 */
@Serializable
data class PreparationSeance(
    val numero: Int = 1,
    val ouOnSEstArrete: String = "",
    val ouverture: String = "",
    val agenda: List<EntreeAgenda> = emptyList(),
    val secretIds: List<String> = emptyList(),
    // Récompenses prévues (même modèle que celles des quêtes : objets, XP, or...).
    val recompensesPrevues: List<com.jc2.jdrcompagnon.feature_quete.domain.model.QuestReward> = emptyList(),
    // Compte rendu de la distribution, une fois les récompenses données à un groupe ; vide = pas encore.
    val recompensesDistribuees: String = "",
    val notes: String = "",
    val notesAudio: List<NoteAudio> = emptyList(),
    // Anciennes versions, converties en entrées d'agenda et en notes au chargement (CarnetMj.init).
    val sceneIds: List<String> = emptyList(),
    val scenarioIds: List<String> = emptyList(),
    val pnjIds: List<String> = emptyList(),
    val lieux: String = "",
    val recompenses: String = "",
)

/** Séance terminée : numéro, date (ms), résumé saisi à la clôture, notes et récompenses données. */
@Serializable
data class SeanceJouee(
    val numero: Int,
    val date: Long,
    val resume: String = "",
    val notes: String = "",
    val recompenses: String = "",
    val notesAudio: List<NoteAudio> = emptyList(),
)

/**
 * Note audio enregistrée par le MJ (note rapide) : fichier [fichier] dans le stockage interne de
 * l'app (voir NotesAudio), date de l'enregistrement (ms) et durée.
 */
@Serializable
data class NoteAudio(
    val id: String = java.util.UUID.randomUUID().toString(),
    val fichier: String,
    val date: Long,
    val dureeMs: Long = 0,
)
