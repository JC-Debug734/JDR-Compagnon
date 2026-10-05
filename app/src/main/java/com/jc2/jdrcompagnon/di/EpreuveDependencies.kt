package com.jc2.jdrcompagnon.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveImageStore
import com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveRepository
import com.jc2.jdrcompagnon.feature_epreuve.data.EpreuveRepositoryImpl
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.ComplicationEpreuve
import com.jc2.jdrcompagnon.feature_epreuve.domain.model.Epreuve
import com.jc2.jdrcompagnon.feature_epreuve.presentation.EpreuveListViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ⚠️ Comme TableAleatoireDependencies/EnvironmentDependencies : pas de Hilt/Koin, réutilise la
 * base Room initialisée par BoutiqueDependencies.init (voir GameState.init).
 */
object EpreuveDependencies {

    val repository: EpreuveRepository by lazy {
        EpreuveRepositoryImpl(BoutiqueDependencies.requireDatabase().epreuveDao())
    }

    fun newListViewModel(): EpreuveListViewModel = EpreuveListViewModel(repository)

    /** Épreuves d'un monde (instantané), pour les liens de scénario `#epreuve:[Nom]`. */
    suspend fun epreuvesDuMonde(worldId: String?): List<Epreuve> =
        worldId?.let { repository.observerEpreuves(it).first() }.orEmpty()

    /** Résout un lien de scénario `#epreuve:[Nom]` parmi les épreuves de l'outil ÉPREUVES. */
    suspend fun trouverEpreuve(worldId: String?, nom: String): Epreuve? =
        epreuvesDuMonde(worldId).firstOrNull { it.nom.equals(nom.trim(), ignoreCase = true) }

    private const val PREFS = "epreuves_outil"

    /**
     * Ajoute quelques épreuves d'exemple la première fois que l'outil est ouvert dans un monde.
     * Mémorisé par monde (et non "si la liste est vide") : un MJ qui supprime toutes les
     * épreuves ne les voit pas réapparaître.
     */
    fun seedExamplesIfNeeded(context: Context, worldId: String) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cle = "seed_$worldId"
        if (prefs.getBoolean(cle, false)) return
        prefs.edit().putBoolean(cle, true).apply()
        CoroutineScope(Dispatchers.IO).launch {
            exemples(worldId).forEach { (epreuve, asset) ->
                val image = asset?.let { EpreuveImageStore.copierDepuisAsset(appContext, it, epreuve.id) }
                repository.sauvegarder(epreuve.copy(imageFileName = image))
            }
        }
    }

    private fun exemples(worldId: String): List<Pair<Epreuve, String?>> = listOf(
        Epreuve(
            worldId = worldId,
            nom = "Traversée du marais maudit",
            description = "Une brume verdâtre recouvre les eaux stagnantes. Le sentier disparaît sous la vase et des formes bougent entre les roseaux.",
            reussitesRequises = 4,
            complications = listOf(
                ComplicationEpreuve("Sables mouvants", "Un personnage s'enfonce jusqu'à la taille : il doit être tiré de là (perte d'une heure)."),
                ComplicationEpreuve("Sangsues géantes", "Les sangsues s'accrochent aux jambes : 1d4 dégâts perforants à chacun."),
                ComplicationEpreuve("Miasmes", "Une bulle de gaz éclate : JS de Constitution DD 12 ou empoisonné jusqu'au prochain repos court."),
                ComplicationEpreuve("Provisions gâchées", "Un sac tombe à l'eau : le groupe perd une journée de rations."),
                ComplicationEpreuve("Égarés", "La brume se referme : le groupe tourne en rond et perd une demi-journée."),
            ),
        ) to "marais_maudit.webp",
        Epreuve(
            worldId = worldId,
            nom = "Ascension du col enneigé",
            description = "Le vent hurle sur la paroi gelée. Le seul passage vers la citadelle longe un à-pic vertigineux.",
            reussitesRequises = 3,
            complications = listOf(
                ComplicationEpreuve("Chute de pierres", "Des rochers dévalent la pente : JS de Dextérité DD 13 ou 2d6 dégâts contondants."),
                ComplicationEpreuve("Gelures", "Le froid mord : un personnage gagne un niveau d'épuisement."),
                ComplicationEpreuve("Corde rompue", "La corde cède : un personnage glisse et perd 1d10 PV en se rattrapant."),
                ComplicationEpreuve("Avalanche lointaine", "Un grondement : le passage principal est bloqué, il faut un détour de plusieurs heures dans le froid."),
            ),
        ) to "montagne_citadelle.webp",
        Epreuve(
            worldId = worldId,
            nom = "Poursuite dans la cité marchande",
            description = "Le voleur file entre les étals, renverse les paniers et s'engouffre dans les ruelles bondées.",
            reussitesRequises = 3,
            complications = listOf(
                ComplicationEpreuve("Étal renversé", "Un marchand furieux exige réparation (10 po) et retient un personnage."),
                ComplicationEpreuve("Garde de la ville", "Un garde arrête le groupe pour trouble à l'ordre public."),
                ComplicationEpreuve("Bourse coupée", "Dans la cohue, un complice dérobe la bourse d'un personnage."),
                ComplicationEpreuve("Impasse", "La ruelle se termine en cul-de-sac : le voleur prend de l'avance."),
            ),
        ) to "cite_marchande.webp",
    )
}

class EpreuveListViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return EpreuveDependencies.newListViewModel() as T
    }
}
