package com.jc2.jdrcompagnon.feature_boutique.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.TypeBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CreerBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererNomBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererNomMarchandUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ModifierBoutiqueUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BoutiqueListUiState {
    data object Loading : BoutiqueListUiState
    data class Success(val boutiques: List<Boutique>) : BoutiqueListUiState
    data class Error(val message: String) : BoutiqueListUiState
}

/**
 * ViewModel = idiot volontairement limité à l'état + délégation.
 * Toute la logique de génération vit dans les Use Cases (couche Domain).
 * Aucune référence à Context ici.
 */
class BoutiqueViewModel(
    private val repository: BoutiqueRepository,
    private val creerBoutique: CreerBoutiqueUseCase,
    private val modifierBoutique: ModifierBoutiqueUseCase,
    private val genererNomBoutique: GenererNomBoutiqueUseCase,
    private val genererNomMarchand: GenererNomMarchandUseCase,
    private val mondeActif: () -> String // fourni par GameState via DI, pas de couplage direct ici
) : ViewModel() {

    val uiState: StateFlow<BoutiqueListUiState> = repository
        .observerToutesLesBoutiques()
        .map { boutiques -> BoutiqueListUiState.Success(boutiques) as BoutiqueListUiState }
        .catch { e -> emit(BoutiqueListUiState.Error(e.message ?: "Erreur inconnue")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BoutiqueListUiState.Loading
        )

    /** Événement remonté par CreerBoutiqueDialog (UDF : la vue ne fait que router l'événement). */
    fun onCreerBoutique(nom: String, nomMarchand: String, standingChoisi: StandingBoutique?, type: TypeBoutique) {
        viewModelScope.launch {
            val boutique = creerBoutique(
                nom = nom,
                nomMarchand = nomMarchand,
                standingChoisi = standingChoisi,
                type = type,
                monde = mondeActif()
            )
            repository.sauvegarderBoutique(boutique)
        }
    }

    /** Événement remonté par BoutiqueFormDialog en mode édition. */
    fun onModifierBoutique(
        boutiqueExistante: Boutique,
        nom: String,
        nomMarchand: String,
        standingChoisi: StandingBoutique?,
        type: TypeBoutique
    ) {
        viewModelScope.launch {
            val boutiqueMaj = modifierBoutique(
                boutiqueExistante = boutiqueExistante,
                nom = nom,
                nomMarchand = nomMarchand,
                standingChoisi = standingChoisi,
                type = type,
                monde = mondeActif()
            )
            repository.sauvegarderBoutique(boutiqueMaj)
        }
    }

    fun onSupprimerBoutique(id: String) {
        viewModelScope.launch { repository.supprimerBoutique(id) }
    }

    /** Boutons "nom aléatoire" du formulaire — génération pure, pas de coroutine nécessaire. */
    fun genererNomBoutiqueAleatoire(): String = genererNomBoutique()
    fun genererNomMarchandAleatoire(): String = genererNomMarchand()
}