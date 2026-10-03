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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CarteUiState {
    data object Loading : CarteUiState
    data class Success(
        val carte: CarteCampagne,
        val points: List<PointInteret>,
        val toutesLesBoutiques: List<Boutique>,
        // Lieux de la campagne placés sur aucune carte : proposés au MJ pour les placer ici.
        val pointsNonPlaces: List<PointInteret> = emptyList()
    ) : CarteUiState
}

class CarteCampagneViewModel(
    private val campagneId: String,
    // Carte affichée ; null = première carte de la campagne (créée si la campagne n'en a aucune).
    private val carteId: String?,
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
            val carte = if (carteId != null) {
                repository.getCarte(campagneId, carteId)
            } else {
                // Ouverture sans carte précise (menu latéral) : la première carte existante,
                // sinon une carte par défaut créée pour l'occasion.
                repository.observerCartes(campagneId).first().firstOrNull() ?: repository.getCarte(campagneId)
            }
            repository.sauvegarderCarte(carte) // assure qu'elle existe dès la première ouverture
            carteState.value = carte
        }
    }

    private val idCarte: String? get() = carteState.value?.id

    val uiState: StateFlow<CarteUiState> = combine(
        carteState,
        repository.observerPoints(campagneId),
        boutiqueRepository.observerToutesLesBoutiques()
    ) { carte, points, boutiques ->
        if (carte == null) CarteUiState.Loading
        else CarteUiState.Success(
            carte = carte,
            points = points.filter { it.carteId == carte.id },
            toutesLesBoutiques = boutiques,
            pointsNonPlaces = points.filter { it.carteId == null }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarteUiState.Loading)

    /** Nouveau lieu dont l'icône est centrée librement en ([fx], [fy]), fractions de la carte. */
    fun onCreerPoint(nom: String, type: TypePointInteret, fx: Float, fy: Float, description: String) {
        val id = idCarte ?: return
        viewModelScope.launch { creerPointInteret(campagneId, nom, type, 0, 0, description, carteId = id, fx = fx, fy = fy) }
    }

    /** Place librement sur cette carte, en ([fx], [fy]), un lieu existant qui n'était sur aucune carte. */
    fun onPlacerPoint(point: PointInteret, fx: Float, fy: Float) {
        val id = idCarte ?: return
        viewModelScope.launch { repository.sauvegarderPoint(point.copy(carteId = id, fx = fx, fy = fy)) }
    }

    fun onBasculerVisibiliteJoueurs(point: PointInteret, visible: Boolean) {
        viewModelScope.launch { repository.definirVisibiliteJoueurs(point.id, visible) }
    }

    /** Événements de la bibliothèque rattachés au lieu (voir EvenementsLiesSection). */
    fun onDefinirEvenements(point: PointInteret, evenementIds: List<String>) {
        viewModelScope.launch { repository.definirEvenementsDuPoint(point.id, evenementIds) }
    }

    /** Retire le lieu de cette carte sans le supprimer : il redevient "non placé". */
    fun onRetirerPointDeLaCarte(point: PointInteret) {
        viewModelScope.launch { repository.sauvegarderPoint(point.copy(carteId = null)) }
    }

    fun onSupprimerPoint(id: String) {
        viewModelScope.launch { supprimerPointInteret(id) }
    }

    /** Déplacement libre (pas d'aimantation à la grille) : centre de l'icône en fractions de la carte. */
    fun onDeplacerPoint(point: PointInteret, fx: Float, fy: Float) {
        viewModelScope.launch { deplacerPointInteret(point, fx, fy) }
    }

    /** [iconKey] : voir IconesPointInteret.PALETTE ; null pour revenir à l'icône/couleur par défaut du type. */
    fun onPersonnaliserPoint(point: PointInteret, iconKey: String?, couleurArgb: Int?) {
        viewModelScope.launch { repository.sauvegarderPoint(dernierEtat(point).copy(iconKey = iconKey, couleurArgb = couleurArgb)) }
    }

    /**
     * Version la plus récente de [point] : l'écran peut transmettre une copie périmée (gestes
     * mémorisés), qui écraserait sinon une icône, une couleur ou une position enregistrée depuis.
     */
    private fun dernierEtat(point: PointInteret): PointInteret =
        (uiState.value as? CarteUiState.Success)?.points?.firstOrNull { it.id == point.id } ?: point

    fun onLierBoutique(ville: PointInteret, boutique: Boutique) {
        viewModelScope.launch { lierBoutiqueAVille(ville, boutique) }
    }

    fun onDelierBoutique(ville: PointInteret, boutique: Boutique) {
        viewModelScope.launch { delierBoutique(ville, boutique) }
    }

    fun onRedimensionnerCarte(largeurCases: Int, hauteurCases: Int, echelleKmParCase: Int) {
        viewModelScope.launch {
            val actuelle = carteState.value ?: CarteCampagne(campagneId = campagneId, id = carteId ?: campagneId)
            val nouvelleCarte = actuelle.copy(largeurCases = largeurCases, hauteurCases = hauteurCases, echelleKmParCase = echelleKmParCase)
            repository.sauvegarderCarte(nouvelleCarte)
            carteState.value = nouvelleCarte
        }
    }

    /** [caseA] / [caseB] : (colonne, ligne) de la case sous le centre de chaque icône. */
    fun calculerTrajet(caseA: Pair<Int, Int>, caseB: Pair<Int, Int>, vitesse: VitesseDeplacement): ResultatTrajet? =
        calculerTrajet(listOf(caseA, caseB), vitesse)

    fun calculerTrajet(etapes: List<Pair<Int, Int>>, vitesse: VitesseDeplacement): ResultatTrajet? {
        val carte = carteState.value ?: return null
        return calculerTempsTrajet(etapes, carte, vitesse)
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
