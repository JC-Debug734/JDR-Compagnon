package com.jc2.jdrcompagnon.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironmentImageStore
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironnementRepository
import com.jc2.jdrcompagnon.feature_environnement.data.EnvironnementRepositoryImpl
import com.jc2.jdrcompagnon.feature_environnement.domain.model.Environnement
import com.jc2.jdrcompagnon.feature_environnement.domain.model.EpreuveEnvironnementale
import com.jc2.jdrcompagnon.feature_environnement.domain.usecase.catalogueEpreuves
import com.jc2.jdrcompagnon.feature_environnement.domain.model.LootEntry
import com.jc2.jdrcompagnon.feature_environnement.presentation.EnvironmentDetailViewModel
import com.jc2.jdrcompagnon.feature_environnement.presentation.EnvironmentViewModel
import com.jc2.jdrcompagnon.ui.availableLoopTracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ⚠️ Comme HistoriqueDependencies/CarteDependencies : pas de Hilt/Koin dans le projet, et pas de
 * base Room propre — réutilise celle initialisée par BoutiqueDependencies.init (voir
 * GameState.init), qui doit avoir été appelé avant toute utilisation de cet objet.
 */
object EnvironmentDependencies {

    val repository: EnvironnementRepository by lazy {
        EnvironnementRepositoryImpl(BoutiqueDependencies.requireDatabase().environnementDao(), EvenementDependencies.repository)
    }

    fun newViewModel(): EnvironmentViewModel = EnvironmentViewModel(repository)

    fun newDetailViewModel(environnementId: String): EnvironmentDetailViewModel =
        EnvironmentDetailViewModel(environnementId, repository)

    private fun epreuvesDuCatalogue(vararg noms: String): List<EpreuveEnvironnementale> =
        catalogueEpreuves.filter { it.nom in noms }

    /** Épreuves de tous les environnements d'un monde, avec le nom de leur environnement. */
    suspend fun epreuvesDuMonde(worldId: String?): List<Pair<String, EpreuveEnvironnementale>> {
        val monde = worldId ?: return emptyList()
        return repository.observerEnvironnements(monde).first()
            .flatMap { environnement -> environnement.epreuves.map { environnement.nom to it } }
    }

    /**
     * Résout un lien de scénario `#epreuve:[Nom]` : d'abord parmi les épreuves des environnements
     * du monde (versions éventuellement adaptées par le MJ), sinon dans le catalogue de l'app.
     */
    suspend fun trouverEpreuve(worldId: String?, nom: String): EpreuveEnvironnementale? =
        epreuvesDuMonde(worldId).firstOrNull { it.second.nom.equals(nom, ignoreCase = true) }?.second
            ?: catalogueEpreuves.firstOrNull { it.nom.equals(nom, ignoreCase = true) }

    /**
     * Pré-remplit quelques environnements d'exemple la première fois qu'un monde n'en a aucun,
     * pour que l'outil ne s'ouvre jamais complètement vide. Musique choisie parmi
     * availableLoopTracks quand une piste au nom proche existe, sinon aucune (le MJ complète).
     * Image prise dans la galerie de l'app (voir [IMAGES_EXEMPLES]).
     */
    fun seedExamplesIfNeeded(context: Context, worldId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (repository.compterPourMonde(worldId) > 0) {
                affecterImagesAuxExemplesExistants(context, worldId)
                return@launch
            }
            fun musicIdContaining(vararg keywords: String): String? =
                availableLoopTracks.firstOrNull { track ->
                    keywords.any { track.displayName.contains(it, ignoreCase = true) }
                }?.id

            val exemples = listOf(
                Environnement(
                    nom = "Forêt profonde",
                    description = "Bois ancien, lumière filtrée, sentiers à peine visibles.",
                    worldId = worldId,
                    musicTrackId = musicIdContaining("forêt", "foret", "nature"),
                    rumeurs = listOf(
                        "Des bûcherons ont disparu sans laisser de trace près de la clairière nord.",
                        "Une biche blanche porterait chance à qui la suit sans lui faire de mal."
                    ),
                    rencontresAleatoires = listOf(
                        "Une meute de loups affamés suit le groupe à distance.",
                        "Un druide méfiant demande la raison de leur présence.",
                        "Un arbre creux dissimule un petit campement abandonné."
                    ),
                    tableButin = listOf(LootEntry("Herbes médicinales", 3), LootEntry("Peau de bête", 2), LootEntry("Arc court", 1)),
                    epreuves = epreuvesDuCatalogue("Forêt labyrinthique")
                ),
                Environnement(
                    nom = "Montagnes escarpées",
                    description = "Pics rocheux, cols venteux, à-pics vertigineux.",
                    worldId = worldId,
                    musicTrackId = musicIdContaining("montagne", "epique", "épique"),
                    rumeurs = listOf(
                        "Un dragon aurait établi son antre dans les hauteurs.",
                        "Des mineurs nains cherchent des bras solides pour une expédition."
                    ),
                    rencontresAleatoires = listOf(
                        "Un éboulement bloque le chemin.",
                        "Des griffons nichent sur une corniche voisine.",
                        "Un marchand nain, bloqué par la neige, propose du troc."
                    ),
                    tableButin = listOf(LootEntry("Minerai de fer", 3), LootEntry("Corde de 15 mètres", 2), LootEntry("Gemme brute", 1)),
                    epreuves = epreuvesDuCatalogue("Tempête sur le col", "Crue soudaine")
                ),
                Environnement(
                    nom = "Marais fétide",
                    description = "Eau stagnante, brume basse, moustiques et lumières trompeuses.",
                    worldId = worldId,
                    musicTrackId = musicIdContaining("marais", "sombre", "mystère", "mystere"),
                    rumeurs = listOf(
                        "Des feux follets attireraient les voyageurs vers des sables mouvants.",
                        "Une sorcière du marais échangerait des services contre des ingrédients rares."
                    ),
                    rencontresAleatoires = listOf(
                        "Un banc de sangsues géantes surgit de l'eau.",
                        "Une pirogue abandonnée dérive, chargée de provisions avariées.",
                        "Des feux follets égarent le groupe hors du sentier."
                    ),
                    tableButin = listOf(LootEntry("Champignons luminescents", 3), LootEntry("Fiole de poison", 1), LootEntry("Amulette ternie", 1)),
                    epreuves = epreuvesDuCatalogue("Marais fétide")
                ),
                Environnement(
                    nom = "Donjon en ruines",
                    description = "Couloirs effondrés, poussière séculaire, pièges oubliés.",
                    worldId = worldId,
                    musicTrackId = musicIdContaining("donjon", "tension", "combat"),
                    rumeurs = listOf(
                        "Un trésor maudit dormirait dans la salle du trône effondrée.",
                        "Des pilleurs de tombes ne sont jamais revenus du niveau inférieur."
                    ),
                    rencontresAleatoires = listOf(
                        "Un piège à fléchettes se déclenche sur une dalle descellée.",
                        "Des morts-vivants errent dans une galerie voisine.",
                        "Un passage secret s'ouvre derrière une tapisserie en lambeaux."
                    ),
                    tableButin = listOf(LootEntry("Pièces anciennes", 3), LootEntry("Parchemin illisible", 2), LootEntry("Arme rouillée", 2)),
                    epreuves = epreuvesDuCatalogue("Galeries effondrées")
                ),
                Environnement(
                    nom = "Ville animée",
                    description = "Ruelles bondées, marchés bruyants, gardes en patrouille.",
                    worldId = worldId,
                    musicTrackId = musicIdContaining("taverne", "ville", "marché", "marche"),
                    rumeurs = listOf(
                        "Un guet-apens aurait été tendu au marchand d'épices la nuit dernière.",
                        "Le conseil de la ville chercherait discrètement des aventuriers de confiance."
                    ),
                    rencontresAleatoires = listOf(
                        "Un pickpocket tente sa chance sur un membre du groupe.",
                        "Une bagarre éclate entre deux tavernes rivales.",
                        "Un héraut annonce une prime pour la capture d'un criminel recherché."
                    ),
                    tableButin = listOf(LootEntry("Bourse de cuir", 2), LootEntry("Carte de la ville", 2), LootEntry("Bijou de pacotille", 1)),
                    epreuves = epreuvesDuCatalogue("Émeute en ville", "Poursuite dans les ruelles", "Incendie")
                )
            )
            exemples.forEach { repository.sauvegarder(avecImageParDefaut(context, it)) }
            marquerImagesAffectees(context, worldId)
        }
    }

    /** Image de la galerie (assets/dnd/environnements) associée à chaque environnement d'exemple. */
    private val IMAGES_EXEMPLES = mapOf(
        "Montagnes escarpées" to "montagne_citadelle.webp",
        "Marais fétide" to "marais_maudit.webp",
        "Ville animée" to "cite_marchande.webp"
    )

    private fun avecImageParDefaut(context: Context, environnement: Environnement): Environnement {
        if (environnement.imageFileName != null) return environnement
        val asset = IMAGES_EXEMPLES[environnement.nom] ?: return environnement
        val fileName = EnvironmentImageStore.copierDepuisAsset(context, asset, environnement.id, null)
            ?: return environnement
        return environnement.copy(imageFileName = fileName)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("environnements_images_defaut", Context.MODE_PRIVATE)

    private fun marquerImagesAffectees(context: Context, worldId: String) =
        prefs(context).edit().putBoolean(worldId, true).apply()

    /**
     * Mondes créés avant l'ajout des images : affecte une fois l'image par défaut aux exemples
     * restés sans image. Fait une seule fois par monde pour respecter un "Retirer" du MJ ensuite.
     */
    private suspend fun affecterImagesAuxExemplesExistants(context: Context, worldId: String) {
        if (prefs(context).getBoolean(worldId, false)) return
        repository.observerEnvironnements(worldId).first()
            .filter { it.imageFileName == null && it.nom in IMAGES_EXEMPLES }
            .forEach { repository.sauvegarder(avecImageParDefaut(context, it)) }
        marquerImagesAffectees(context, worldId)
    }
}

class EnvironmentViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EnvironmentDependencies.newViewModel() as T
    }
}

class EnvironmentDetailViewModelFactory(private val environnementId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EnvironmentDependencies.newDetailViewModel(environnementId) as T
    }
}
