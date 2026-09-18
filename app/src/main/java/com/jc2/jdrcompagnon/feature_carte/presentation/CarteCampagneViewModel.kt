package com.jc2.jdrcompagnon.feature_carte.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_carte.data.CarteRepository
import com.jc2.jdrcompagnon.feature_carte.domain.model.CarteCampagne
import com.jc2.jdrcompagnon.feature_carte.domain.model.PointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.TypePointInteret
import com.jc2.jdrcompagnon.feature_carte.domain.model.VitesseDeplacement
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.CalculerTempsTrajetUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.CreerPointInteretUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.DelierBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.DeplacerPointInteretUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.LierBoutiqueAVilleUseCase
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.ResultatTrajet
import com.jc2.jdrcompagnon.feature_carte.domain.usecase.SupprimerPointInteretUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CarteUiState {
    data object Loading : CarteUiState
    data class Success(
        val carte: CarteCampagne,
        val points: List<PointInteret>,
        val toutesLesBoutiques: List<Boutique>
    ) : CarteUiState
}

class CarteCampagneViewModel(
    private val campagneId: String,
    private val repository: CarteRepository,
    private val boutiqueRepository: BoutiqueRepository,
    private val creerPointInteret: CreerPointInteretUseCase,
    private val supprimerPointInteret: SupprimerPointInteretUseCase,
    private val deplacerPointInteret: DeplacerPointInteretUseCase,
    private val lierBoutiqueAVille: LierBoutiqueAVilleUseCase,
    private val delierBoutique: DelierBoutiqueUseCase,
    private val calculerTempsTrajet: CalculerTempsTrajetUseCase
) : ViewModel() {

    private val carteState = MutableStateFlow<CarteCampagne?>(null)

    init {
        viewModelScope.launch {
            val carte = repository.getCarte(campagneId)
            repository.sauvegarderCarte(carte) // assure qu'elle existe dès la première ouverture
            carteState.value = carte
        }
    }

    val uiState: StateFlow<CarteUiState> = combine(
        carteState,
        repository.observerPoints(campagneId),
        boutiqueRepository.observerToutesLesBoutiques()
    ) { carte, points, boutiques ->
        if (carte == null) CarteUiState.Loading else CarteUiState.Success(carte, points, boutiques)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarteUiState.Loading)

    fun onCreerPoint(nom: String, type: TypePointInteret, x: Int, y: Int, description: String) {
        viewModelScope.launch { creerPointInteret(campagneId, nom, type, x, y, description) }
    }

    fun onSupprimerPoint(id: String) {
        viewModelScope.launch { supprimerPointInteret(id) }
    }

    fun onDeplacerPoint(point: PointInteret, nouveauX: Int, nouveauY: Int) {
        viewModelScope.launch { deplacerPointInteret(point, nouveauX, nouveauY) }
    }

    fun onLierBoutique(ville: PointInteret, boutique: Boutique) {
        viewModelScope.launch { lierBoutiqueAVille(ville, boutique) }
    }

    fun onDelierBoutique(ville: PointInteret, boutique: Boutique) {
        viewModelScope.launch { delierBoutique(ville, boutique) }
    }

    fun onRedimensionnerCarte(largeurCases: Int, hauteurCases: Int, echelleKmParCase: Int) {
        viewModelScope.launch {
            val nouvelleCarte = CarteCampagne(campagneId, largeurCases, hauteurCases, echelleKmParCase)
            repository.sauvegarderCarte(nouvelleCarte)
            carteState.value = nouvelleCarte
        }
    }

    fun calculerTrajet(pointA: PointInteret, pointB: PointInteret, vitesse: VitesseDeplacement): ResultatTrajet? {
        val carte = carteState.value ?: return null
        return calculerTempsTrajet(pointA, pointB, carte, vitesse)
    }

    /** [fileName] est un nom de fichier déjà copié en stockage interne par l'UI (voir CarteImageStore), null pour retirer l'image. */
    fun onDefinirImageFond(fileName: String?) {
        viewModelScope.launch {
            val carte = carteState.value ?: return@launch
            val maj = carte.copy(imageFileName = fileName)
            repository.sauvegarderCarte(maj)
            carteState.value = maj
        }
    }
}
