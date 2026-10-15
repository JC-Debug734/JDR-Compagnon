package com.jc2.jdrcompagnon.feature_preparation.data

import android.content.Context
import android.content.SharedPreferences
import com.jc2.jdrcompagnon.feature_preparation.domain.model.CarnetMonde
import com.jc2.jdrcompagnon.feature_preparation.domain.model.EntreeAgenda
import com.jc2.jdrcompagnon.feature_preparation.domain.model.TypeAgenda
import com.jc2.jdrcompagnon.feature_preparation.domain.model.FilEnSuspens
import com.jc2.jdrcompagnon.feature_preparation.domain.model.PreparationSeance
import com.jc2.jdrcompagnon.feature_preparation.domain.model.SeanceJouee
import com.jc2.jdrcompagnon.feature_preparation.domain.model.SecretIndice
import com.jc2.jdrcompagnon.ui.Character
import com.jc2.jdrcompagnon.ui.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Carnet du MJ, un par monde (clé = WorldState.id) : secrets et indices, fils en suspens,
 * préparation de la prochaine séance et séances jouées. Indépendant des campagnes ; enregistré
 * immédiatement à chaque modification, dans ses propres SharedPreferences.
 */
object CarnetMj {

    private const val PREFS = "carnet_mj"
    private const val KEY_CARNETS = "carnets"
    private val json = Json { ignoreUnknownKeys = true }
    private var prefs: SharedPreferences? = null

    private val _carnets = MutableStateFlow<Map<String, CarnetMonde>>(emptyMap())
    val carnets: StateFlow<Map<String, CarnetMonde>> = _carnets.asStateFlow()

    /** À appeler après GameState.init : reprend les notes autrefois rangées dans les campagnes. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs?.getString(KEY_CARNETS, null)?.let { texte ->
            try {
                _carnets.value = json.decodeFromString<Map<String, CarnetMonde>>(texte)
            } catch (e: Exception) {
                android.util.Log.e("CarnetMj", "Lecture du carnet impossible", e)
            }
        }
        migrerDepuisCampagnes()
        migrerVersAgenda()
    }

    /**
     * Versions précédentes de la préparation (scènes, scénarios et PNJ choisis à part, lieux et
     * récompenses en texte) : converties en entrées d'agenda et en notes.
     */
    private fun migrerVersAgenda() {
        val scenarios = GameState.mjScenarios.value
        val characters = GameState.characters.value
        _carnets.value.filterValues { c ->
            val p = c.preparation
            p.sceneIds.isNotEmpty() || p.scenarioIds.isNotEmpty() || p.pnjIds.isNotEmpty() || p.lieux.isNotBlank() || p.recompenses.isNotBlank()
        }.keys.forEach { monde ->
            maj(monde) { c ->
                val p = c.preparation
                val idsScenarios = (p.scenarioIds + p.sceneIds.mapNotNull { sceneId ->
                    scenarios.firstOrNull { s -> s.scenes.any { it.id == sceneId } }?.id
                }).distinct()
                val entrees = idsScenarios.mapNotNull { id -> scenarios.firstOrNull { it.id == id } }
                    .map { EntreeAgenda(type = TypeAgenda.SCENARIO, libelle = it.title, refId = it.id) } +
                    p.pnjIds.mapNotNull { id -> characters.firstOrNull { it.id == id } }
                        .map { EntreeAgenda(type = TypeAgenda.PNJ, libelle = it.name, refId = it.id) } +
                    listOfNotNull(p.lieux.trim().takeIf { it.isNotBlank() }?.let { EntreeAgenda(type = TypeAgenda.NOTE, libelle = "Lieux : $it") })
                val notes = listOf(p.notes.trim(), p.recompenses.trim().takeIf { it.isNotBlank() }?.let { "Récompenses : $it" }.orEmpty())
                    .filter { it.isNotBlank() }.joinToString("\n")
                c.copy(
                    preparation = p.copy(
                        agenda = p.agenda + entrees,
                        notes = notes,
                        sceneIds = emptyList(), scenarioIds = emptyList(), pnjIds = emptyList(), lieux = "", recompenses = "",
                    )
                )
            }
        }
    }

    private fun enregistrer() {
        prefs?.edit()?.putString(KEY_CARNETS, json.encodeToString(_carnets.value))?.apply()
    }

    fun carnet(worldId: String): CarnetMonde = _carnets.value[worldId] ?: CarnetMonde()

    private fun maj(worldId: String, transformation: (CarnetMonde) -> CarnetMonde) {
        val avant = carnet(worldId)
        val apres = transformation(avant)
        if (apres == avant) return
        _carnets.value = _carnets.value + (worldId to apres)
        enregistrer()
    }

    /**
     * Première version : notes rangées dans chaque campagne (MjCampaign.secrets...). Elles
     * rejoignent le carnet du monde de la campagne (secrets et fils gardent la campagne en
     * étiquette), puis la campagne est vidée.
     */
    private fun migrerDepuisCampagnes() {
        GameState.mjCampaigns.value
            .filter { it.secrets.isNotEmpty() || it.fils.isNotEmpty() || it.seances.isNotEmpty() || it.preparation != PreparationSeance() }
            .forEach { c ->
                maj(c.worldId) { carnet ->
                    val prep = carnet.preparation
                    carnet.copy(
                        secrets = carnet.secrets + c.secrets.map { it.copy(campagneId = it.campagneId ?: c.id) },
                        fils = carnet.fils + c.fils.map { it.copy(campagneId = it.campagneId ?: c.id) },
                        seances = carnet.seances + c.seances,
                        preparation = if (prep == PreparationSeance()) c.preparation else prep.copy(
                            sceneIds = (prep.sceneIds + c.preparation.sceneIds).distinct(),
                            scenarioIds = (prep.scenarioIds + c.preparation.scenarioIds).distinct(),
                            pnjIds = (prep.pnjIds + c.preparation.pnjIds).distinct(),
                            secretIds = (prep.secretIds + c.preparation.secretIds).distinct(),
                        ),
                    )
                }
                GameState.updateMjCampaign(c.copy(secrets = emptyList(), fils = emptyList(), seances = emptyList(), preparation = PreparationSeance()))
            }
    }

    /** Monde d'un secret ou d'un fil, retrouvé par son id. */
    private fun mondeDe(predicat: (CarnetMonde) -> Boolean): String? = _carnets.value.entries.firstOrNull { predicat(it.value) }?.key

    /** PNJ (et créatures) du monde : ordre alphabétique. */
    fun pnjsDuMonde(worldId: String, characters: List<Character>): List<Character> =
        characters
            .filter { it.type != "PJ" && (it.worldId == worldId || it.worldId.isBlank()) }
            .sortedBy { it.name.lowercase() }

    // --- Secrets et indices ---

    fun enregistrerSecret(worldId: String, secret: SecretIndice) = maj(worldId) { c ->
        val secrets = if (c.secrets.any { it.id == secret.id }) c.secrets.map { if (it.id == secret.id) secret else it }
        else c.secrets + secret
        c.copy(secrets = secrets)
    }

    /** Une ligne non vide = un secret (liste recopiée depuis une feuille de notes). */
    fun ajouterSecrets(worldId: String, lignes: String, campagneId: String?) = maj(worldId) { c ->
        val nouveaux = lignes.lines()
            .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("*").trim() }
            .filter { it.isNotBlank() }
            .map { SecretIndice(texte = it, campagneId = campagneId) }
        c.copy(secrets = c.secrets + nouveaux)
    }

    fun supprimerSecret(worldId: String, secretId: String) = maj(worldId) { c ->
        c.copy(
            secrets = c.secrets.filterNot { it.id == secretId },
            preparation = c.preparation.copy(secretIds = c.preparation.secretIds - secretId),
        )
    }

    /**
     * Découvert / pas encore découvert. La découverte est datée de la séance en préparation
     * (celle qui se joue).
     */
    fun basculerDecouvert(secretId: String) {
        val monde = mondeDe { c -> c.secrets.any { it.id == secretId } } ?: return
        maj(monde) { c ->
            c.copy(secrets = c.secrets.map {
                if (it.id != secretId) it
                else if (it.decouvert) it.copy(decouvert = false, decouvertSeance = null)
                else it.copy(decouvert = true, decouvertSeance = c.preparation.numero)
            })
        }
    }

    /** Ajoute ou retire un secret de la liste « à placer » de la prochaine séance. */
    fun basculerAPlacer(worldId: String, secretId: String) = maj(worldId) { c ->
        val ids = c.preparation.secretIds
        c.copy(preparation = c.preparation.copy(secretIds = if (secretId in ids) ids - secretId else ids + secretId))
    }

    // --- Fils en suspens ---

    fun enregistrerFil(worldId: String, fil: FilEnSuspens) = maj(worldId) { c ->
        val fils = if (c.fils.any { it.id == fil.id }) c.fils.map { if (it.id == fil.id) fil else it }
        else c.fils + fil.copy(ouvertSeance = fil.ouvertSeance ?: c.seances.maxOfOrNull { it.numero })
        c.copy(fils = fils)
    }

    fun supprimerFil(worldId: String, filId: String) = maj(worldId) { c ->
        c.copy(fils = c.fils.filterNot { it.id == filId })
    }

    fun basculerResolu(worldId: String, filId: String) = maj(worldId) { c ->
        c.copy(fils = c.fils.map { if (it.id == filId) it.copy(resolu = !it.resolu) else it })
    }

    // --- Prochaine séance ---

    fun enregistrerPreparation(worldId: String, preparation: PreparationSeance) = maj(worldId) { c ->
        c.copy(preparation = preparation)
    }

    private fun majPreparation(worldId: String, transformation: (PreparationSeance) -> PreparationSeance) =
        maj(worldId) { c -> c.copy(preparation = transformation(c.preparation)) }

    /** Note prise en jeu (menu latéral) : ajoutée aux notes de la séance, avec l'heure réelle. */
    fun noteRapide(worldId: String, texte: String) {
        val t = texte.trim().ifBlank { return }
        val heure = java.text.SimpleDateFormat("HH:mm", java.util.Locale.FRANCE).format(java.util.Date())
        majPreparation(worldId) { p -> p.copy(notes = listOf(p.notes.trimEnd(), "• $heure — $t").filter { it.isNotBlank() }.joinToString("\n")) }
    }

    /** Note audio enregistrée en jeu : rattachée à la séance en cours. */
    fun ajouterNoteAudio(worldId: String, note: com.jc2.jdrcompagnon.feature_preparation.domain.model.NoteAudio) =
        majPreparation(worldId) { p -> p.copy(notesAudio = p.notesAudio + note) }

    /** Retire une note audio (séance en cours ou séance jouée) et supprime son fichier. */
    fun supprimerNoteAudio(context: Context, worldId: String, noteId: String) {
        val carnet = carnet(worldId)
        val note = (carnet.preparation.notesAudio + carnet.seances.flatMap { it.notesAudio }).firstOrNull { it.id == noteId } ?: return
        NotesAudio.fichier(context, note.fichier).delete()
        maj(worldId) { c ->
            c.copy(
                preparation = c.preparation.copy(notesAudio = c.preparation.notesAudio.filterNot { it.id == noteId }),
                seances = c.seances.map { s -> s.copy(notesAudio = s.notesAudio.filterNot { it.id == noteId }) },
            )
        }
    }

    // --- Agenda ---

    fun ajouterAgenda(worldId: String, entrees: List<EntreeAgenda>) = majPreparation(worldId) { p ->
        // Un même élément n'est ajouté qu'une fois (les notes et combats peuvent se répéter).
        val nouvelles = entrees.filter { e -> e.refId == null || p.agenda.none { it.type == e.type && it.refId == e.refId } }
        p.copy(agenda = p.agenda + nouvelles)
    }

    /**
     * Fixe les éléments d'un [type] dans l'agenda (choix multiple) : ceux déjà présents gardent leur
     * place et leur état, ceux décochés sont retirés, les nouveaux s'ajoutent à la fin. Les entrées
     * sans élément de l'appli (lieu libre) ne sont pas touchées.
     */
    fun definirAgendaType(worldId: String, type: TypeAgenda, choisis: List<EntreeAgenda>) = majPreparation(worldId) { p ->
        val ids = choisis.mapNotNull { it.refId }.toSet()
        val gardees = p.agenda.filter { it.type != type || it.refId == null || it.refId in ids }
        val nouvelles = choisis.filter { c -> gardees.none { it.type == type && it.refId == c.refId } }
        p.copy(agenda = gardees + nouvelles)
    }

    fun modifierAgenda(worldId: String, entree: EntreeAgenda) = majPreparation(worldId) { p ->
        p.copy(agenda = p.agenda.map { if (it.id == entree.id) entree else it })
    }

    fun supprimerAgenda(worldId: String, id: String) = majPreparation(worldId) { p -> p.copy(agenda = p.agenda.filterNot { it.id == id }) }

    fun basculerFait(worldId: String, id: String) = majPreparation(worldId) { p ->
        p.copy(agenda = p.agenda.map { if (it.id == id) it.copy(fait = !it.fait) else it })
    }

    /** Monte ([sens] = -1) ou descend (+1) une entrée dans l'agenda. */
    fun deplacerAgenda(worldId: String, id: String, sens: Int) = majPreparation(worldId) { p ->
        val i = p.agenda.indexOfFirst { it.id == id }
        val j = i + sens
        if (i < 0 || j !in p.agenda.indices) p
        else p.copy(agenda = p.agenda.toMutableList().apply { add(j, removeAt(i)) })
    }

    // --- Récompenses ---

    fun enregistrerRecompense(worldId: String, recompense: com.jc2.jdrcompagnon.feature_quete.domain.model.QuestReward) = majPreparation(worldId) { p ->
        val liste = if (p.recompensesPrevues.any { it.id == recompense.id }) p.recompensesPrevues.map { if (it.id == recompense.id) recompense else it }
        else p.recompensesPrevues + recompense
        p.copy(recompensesPrevues = liste)
    }

    fun supprimerRecompense(worldId: String, id: String) = majPreparation(worldId) { p ->
        p.copy(recompensesPrevues = p.recompensesPrevues.filterNot { it.id == id })
    }

    /** Distribue les récompenses prévues au groupe [groupId] (une seule fois) ; renvoie le compte rendu. */
    fun distribuerRecompenses(worldId: String, groupId: String): String? {
        val p = carnet(worldId).preparation
        if (p.recompensesDistribuees.isNotBlank() || p.recompensesPrevues.isEmpty()) return null
        val resume = com.jc2.jdrcompagnon.feature_quete.domain.ValiderQueteUseCase.distribuer(p.recompensesPrevues, groupId) ?: return null
        majPreparation(worldId) { it.copy(recompensesDistribuees = resume) }
        return resume
    }

    /**
     * Clôt la séance en préparation : elle rejoint les séances jouées avec son [resume] (qui
     * devient le « où on s'est arrêté » de la suivante), ses notes et ses récompenses. Les entrées
     * d'agenda non jouées passent à la séance suivante ; secrets non découverts et fils ouverts
     * restent aussi.
     */
    fun cloturerSeance(worldId: String, resume: String) = maj(worldId) { c ->
        val p = c.preparation
        val recompenses = p.recompensesDistribuees.ifBlank { p.recompensesPrevues.joinToString(", ") { it.resume } }
        c.copy(
            seances = c.seances + SeanceJouee(
                numero = p.numero,
                date = System.currentTimeMillis(),
                resume = resume.trim(),
                notes = p.notes.trim(),
                recompenses = recompenses,
                notesAudio = p.notesAudio,
            ),
            preparation = PreparationSeance(
                numero = p.numero + 1,
                ouOnSEstArrete = resume.trim(),
                agenda = p.agenda.filterNot { it.fait },
            ),
        )
    }
}
