package com.jc2.jdrcompagnon.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepositoryImpl
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.CalculerTempsTrajetUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.CreerPointInteretUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.DelierBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.DeplacerPointInteretUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.GenererEvenementsGeneriquesUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.LierBoutiqueAVilleUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.SupprimerPointInteretUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.TirerEvenementAleatoireUseCase
import com.jc2.jdrcompagnon.feature_carte.presentation.CarteCampagneViewModel
import com.jc2.jdrcompagnon.feature_carte.presentation.EvenementsViewModel
import com.jc2.jdrcompagnon.feature_carte.presentation.VilleDetailViewModel
import com.jc2.jdrcompagnon.feature_carte.presentation.VillesListViewModel
import com.jc2.jdrcompagnon.ui.GameState

/**
 * Provider manuel, même convention que BoutiqueDependencies (pas de Hilt/Koin dans le projet).
 * Réutilise la base Room de BoutiqueDependencies (BoutiqueDependencies.init(context) doit avoir
 * été appelé avant, ce qui est déjà le cas via GameState.init) plutôt que d'en ouvrir une seconde.
 */
object CarteDependencies {

    val repository: CarteRepository by lazy {
        CarteRepositoryImpl(BoutiqueDependencies.requireDatabase().carteDao())
    }

    private val creerPointInteret by lazy { CreerPointInteretUseCase(repository) }
    private val supprimerPointInteret by lazy { SupprimerPointInteretUseCase(repository) }
    private val deplacerPointInteret by lazy { DeplacerPointInteretUseCase(repository) }
    private val lierBoutiqueAVille by lazy { LierBoutiqueAVilleUseCase(BoutiqueDependencies.repository, repository) }
    private val delierBoutique by lazy { DelierBoutiqueUseCase(BoutiqueDependencies.repository, repository) }
    private val calculerTempsTrajet = CalculerTempsTrajetUseCase()
    private val genererEvenementsGeneriques = GenererEvenementsGeneriquesUseCase()
    private val tirerEvenementAleatoire by lazy { TirerEvenementAleatoireUseCase(genererEvenementsGeneriques) }

    fun newCarteViewModel(campagneId: String): CarteCampagneViewModel = CarteCampagneViewModel(
        campagneId = campagneId,
        repository = repository,
        boutiqueRepository = BoutiqueDependencies.repository,
        creerPointInteret = creerPointInteret,
        supprimerPointInteret = supprimerPointInteret,
        deplacerPointInteret = deplacerPointInteret,
        lierBoutiqueAVille = lierBoutiqueAVille,
        delierBoutique = delierBoutique,
        calculerTempsTrajet = calculerTempsTrajet
    )

    fun newEvenementsViewModel(campagneId: String): EvenementsViewModel = EvenementsViewModel(
        campagneId = campagneId,
        repository = repository,
        genererEvenementsGeneriques = genererEvenementsGeneriques,
        tirerEvenementAleatoire = tirerEvenementAleatoire
    )

    fun newVillesListViewModel(campagneId: String): VillesListViewModel = VillesListViewModel(
        campagneId = campagneId,
        repository = repository,
        creerPointInteret = creerPointInteret
    )

    fun newVilleDetailViewModel(campagneId: String, villeId: String): VilleDetailViewModel = VilleDetailViewModel(
        campagneId = campagneId,
        villeId = villeId,
        repository = repository,
        boutiqueRepository = BoutiqueDependencies.repository,
        lierBoutiqueAVille = lierBoutiqueAVille,
        delierBoutique = delierBoutique,
        tirerEvenementAleatoire = tirerEvenementAleatoire,
        mondeActif = { GameState.currentWorld.value?.id ?: "donjon_et_dragon" }
    )
}

class CarteCampagneViewModelFactory(private val campagneId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CarteDependencies.newCarteViewModel(campagneId) as T
    }
}

class EvenementsViewModelFactory(private val campagneId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CarteDependencies.newEvenementsViewModel(campagneId) as T
    }
}

class VillesListViewModelFactory(private val campagneId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CarteDependencies.newVillesListViewModel(campagneId) as T
    }
}

class VilleDetailViewModelFactory(private val campagneId: String, private val villeId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CarteDependencies.newVilleDetailViewModel(campagneId, villeId) as T
    }
}
