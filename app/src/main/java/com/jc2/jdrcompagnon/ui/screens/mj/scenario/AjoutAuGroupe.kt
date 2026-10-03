package com.jc2.jdrcompagnon.ui.screens.mj.scenario

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.jc2.jdrcompagnon.network.NetworkSessionManager
import com.jc2.jdrcompagnon.ui.GameState
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.PnjImport
import com.jc2.jdrcompagnon.ui.screens.mj.library.srd.SrdRepository

/**
 * « Ajouter au groupe » depuis un lien du scénario (ou un profil de la scène) :
 * - objet (#equipment) → proposé au groupe : les joueurs connectés le voient sur leur écran de
 *   réclamation (« Je le veux » / « Passer »), comme « Envoyer à un groupe » ;
 * - PNJ → sa fiche rejoint les membres du groupe ;
 * - monstre ou animal du bestiaire → une fiche « Créature » est créée à partir de son profil
 *   (numérotée s'il y en a déjà une du même nom dans le groupe) et rejoint le groupe, rangée dans
 *   « Créatures du groupe ».
 */
object AjoutAuGroupe {

    /** Type de lien d'un objet (les autres types sont des profils). */
    fun estObjet(type: String): Boolean = type == "equipment"

    suspend fun ajouter(context: Context, groupe: GameState.MjGroup, type: String, nom: String, worldId: String?): String =
        if (estObjet(type)) proposerObjet(groupe, nom) else ajouterProfil(context, groupe, type, nom, worldId)

    private fun proposerObjet(groupe: GameState.MjGroup, nom: String): String {
        NetworkSessionManager.sendLootOfferToGroup(groupe.id, nom)
        return "« $nom » est proposé au groupe ${groupe.name} : les joueurs connectés peuvent le réclamer."
    }

    private suspend fun ajouterProfil(
        context: Context,
        groupe: GameState.MjGroup,
        type: String,
        nom: String,
        worldId: String?,
    ): String {
        val monde = worldId ?: groupe.worldId
        val actuel = GameState.mjGroups.value.firstOrNull { it.id == groupe.id } ?: groupe
        val membres = GameState.characters.value.filter { it.id in actuel.memberIds }

        // PNJ (ou toute fiche non-PJ du même nom, ex. une créature déjà créée) : on l'ajoute tel quel.
        val fiche = GameState.characters.value.firstOrNull {
            it.type != "PJ" && it.worldId == monde && it.name.equals(nom, ignoreCase = true)
        }
        // Un lien #monster vise le bestiaire ; il retombe sur la fiche si le bestiaire ne le connaît pas.
        // Un lien #pnj sans fiche existante peut aussi s'appuyer sur un profil du bestiaire.
        val profil = if (type == "monster" || fiche == null) SrdRepository.getMonsterByName(context, nom, monde) else null
        if (fiche != null && profil == null) {
            if (fiche.id in actuel.memberIds) return "${fiche.name} fait déjà partie du groupe ${actuel.name}."
            GameState.updateMjGroup(actuel.copy(memberIds = actuel.memberIds + fiche.id))
            return "${fiche.name} rejoint le groupe ${actuel.name}."
        }
        profil ?: return "« $nom » introuvable (ni fiche PNJ, ni bestiaire) : rien n'a été ajouté."
        val memesNoms = membres.count { it.name == profil.name || it.name.startsWith(profil.name + " ") }
        val nomFiche = if (memesNoms == 0) profil.name else "${profil.name} ${memesNoms + 1}"
        val typeFiche = if (type == "monster") "Créature" else "PNJ"
        val creature = PnjImport.ficheDepuisProfil(context, profil, monde, typeFiche, "Ajouté au groupe depuis le scénario")
            .copy(name = nomFiche)
        GameState.addCharacter(creature)
        GameState.updateMjGroup(actuel.copy(memberIds = actuel.memberIds + creature.id))
        return "$nomFiche rejoint le groupe ${actuel.name} (fiche ${typeFiche.lowercase()} créée)."
    }
}

/**
 * Déroulé de « Ajouter au groupe » : directement dans le groupe sélectionné (menu latéral MJ),
 * sinon choix du groupe parmi ceux de l'univers ; puis compte rendu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjouterAuGroupeDialog(type: String, nom: String, worldId: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val groupes by GameState.mjGroups.collectAsState()
    val groupeCourantId by GameState.currentGroupId.collectAsState()
    val groupesDuMonde = remember(groupes, worldId) { groupes.filter { it.worldId == (worldId ?: "") } }
    var choisi by remember { mutableStateOf(groupesDuMonde.firstOrNull { it.id == groupeCourantId }) }
    var resultat by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(choisi?.id) {
        choisi?.let { resultat = AjoutAuGroupe.ajouter(context, it, type, nom, worldId) }
    }

    when {
        resultat != null -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ajouter au groupe", fontWeight = FontWeight.Bold) },
            text = { Text(resultat!!) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        )
        choisi == null -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ajouter « $nom » à…", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (groupesDuMonde.isEmpty()) {
                        Text("Aucun groupe dans cet univers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    groupesDuMonde.forEach { groupe ->
                        ListItem(
                            headlineContent = { Text(groupe.name) },
                            modifier = Modifier.clickable { choisi = groupe },
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
        )
    }
}
