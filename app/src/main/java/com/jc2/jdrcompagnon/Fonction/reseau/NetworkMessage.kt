package com.jc2.jdrcompagnon.network

import com.jc2.jdrcompagnon.ui.Character
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Message échangé entre MJ et Joueur sur la socket TCP existante.
 * Le canal transporte du texte ligne par ligne (BufferedReader.readLine()) :
 * un message = une ligne JSON, encodée avec networkJson puis envoyée via
 * PrintWriter(...).println(...).
 */
@Serializable
data class NetworkMessage(
    val type: String,
    val character: Character? = null,
    val characterId: String? = null,
    val characters: List<CharacterSummary>? = null,
    val accepted: Boolean? = null
) {
    companion object {
        /** MJ → Joueur : envoie un personnage complet (envoi ponctuel). */
        const val TYPE_CHARACTER_PUSH = "character_push"

        /** MJ → Joueur : demande de renvoyer l'état actuel d'un personnage. */
        const val TYPE_CHARACTER_SYNC_REQUEST = "character_sync_request"

        /** Joueur → MJ : réponse à une demande de synchronisation. */
        const val TYPE_CHARACTER_SYNC_RESPONSE = "character_sync_response"

        /**
         * MJ → Joueur : liste des personnages du groupe encore disponibles
         * (pas déjà réservés par un autre joueur). Envoyé à la connexion et
         * chaque fois que la disponibilité change (réservation, libération).
         */
        const val TYPE_AVAILABLE_CHARACTERS = "available_characters"

        /** Joueur → MJ : demande à réserver un personnage disponible du groupe. */
        const val TYPE_CHARACTER_CLAIM_REQUEST = "character_claim_request"

        /** MJ → Joueur : résultat de la réservation (accepted + personnage complet si oui). */
        const val TYPE_CHARACTER_CLAIM_RESPONSE = "character_claim_response"

        /**
         * Joueur → MJ : propose un des personnages de son propre appareil
         * (aucun personnage du groupe n'était disponible).
         */
        const val TYPE_CHARACTER_PROPOSAL = "character_proposal"

        /** MJ → Joueur : le MJ a accepté ou refusé la proposition. */
        const val TYPE_CHARACTER_PROPOSAL_RESULT = "character_proposal_result"
    }
}

/**
 * Résumé léger d'un personnage, utilisé pour la liste des personnages
 * disponibles (évite d'envoyer les fiches complètes tant qu'elles ne sont
 * pas réservées).
 */
@Serializable
data class CharacterSummary(
    val id: String,
    val name: String,
    val race: String,
    val characterClass: String,
    val level: Int
)

/** Instance Json partagée pour l'encodage/décodage des messages réseau. */
val networkJson = Json { ignoreUnknownKeys = true }