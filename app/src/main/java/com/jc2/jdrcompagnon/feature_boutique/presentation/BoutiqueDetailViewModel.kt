package com.jc2.jdrcompagnon.feature_boutique.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.domain.model.ArticleEnVente
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Boutique
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Employe
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.model.RoleEmploye
import com.jc2.jdrcompagnon.feature_boutique.domain.model.Service
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ApprovisionnerBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CalculerPrixArticleUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.DeterminerBudgetAchatUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.EffectuerVisiteBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.FiltresApprovisionnement
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererNomMarchandUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererTraitCaractereUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ListerEquipementsDisponiblesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BoutiqueDetailUiState {
    data object Loading : BoutiqueDetailUiState
    data class Success(val boutique: Boutique) : BoutiqueDetailUiState
    data object Introuvable : BoutiqueDetailUiState
    data class Error(val message: String) : BoutiqueDetailUiState
}

/**
 * ViewModel dédié au détail d'UNE boutique : gestion manuelle des employés et de
 * l'inventaire, en plus de ce que la génération automatique a mis en place.
 */
class BoutiqueDetailViewModel(
    private val boutiqueId: String,
    private val repository: BoutiqueRepository,
    private val listerEquipements: ListerEquipementsDisponiblesUseCase,
    private val calculerPrixArticle: CalculerPrixArticleUseCase,
    private val determinerBudgetAchat: DeterminerBudgetAchatUseCase,
    private val approvisionnerBoutique: ApprovisionnerBoutiqueUseCase,
    private val effectuerVisite: EffectuerVisiteBoutiqueUseCase,
    private val genererNomEmploye: GenererNomMarchandUseCase,
    private val genererTraitCaractere: GenererTraitCaractereUseCase,
    private val mondeActif: () -> String
) : ViewModel() {

    val uiState: StateFlow<BoutiqueDetailUiState> = repository
        .observerToutesLesBoutiques()
        .map { liste ->
            val boutique = liste.firstOrNull { it.id == boutiqueId }
            if (boutique != null) BoutiqueDetailUiState.Success(boutique) else BoutiqueDetailUiState.Introuvable
        }
        .catch { e -> emit(BoutiqueDetailUiState.Error(e.message ?: "Erreur inconnue")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BoutiqueDetailUiState.Loading)

    fun genererNomEmployeAleatoire(): String = genererNomEmploye()
    fun genererTraitAleatoire(): String = genererTraitCaractere()

    /** Charge le catalogue SRD du monde actif — appelé depuis un LaunchedEffect côté UI. */
    suspend fun equipementsDisponibles(): List<EquipementReference> = listerEquipements(mondeActif())

    fun prixSuggere(coutBaseEnPo: Int, boutique: Boutique): Int = calculerPrixArticle(coutBaseEnPo, boutique.standing)

    fun budgetSuggere(boutique: Boutique): Int = determinerBudgetAchat(boutique.standing)

    fun onApprovisionner(boutique: Boutique, filtres: FiltresApprovisionnement, budgetEnPo: Int) {
        viewModelScope.launch {
            val nouveauxArticles = approvisionnerBoutique(
                monde = mondeActif(),
                standing = boutique.standing,
                filtres = filtres,
                budgetEnPo = budgetEnPo
            )
            repository.sauvegarderBoutique(boutique.copy(inventaire = boutique.inventaire + nouveauxArticles))
        }
    }

    fun onAjouterEmploye(boutique: Boutique, nom: String, role: RoleEmploye, trait: String) {
        viewModelScope.launch {
            repository.sauvegarderBoutique(boutique.copy(employes = boutique.employes + Employe(nom, role, trait)))
        }
    }

    /** [index] : position dans la liste affichée (Employe n'a pas d'identifiant propre). */
    fun onModifierEmploye(boutique: Boutique, index: Int, nom: String, role: RoleEmploye, trait: String) {
        viewModelScope.launch {
            val employesMaj = boutique.employes.mapIndexed { i, e -> if (i == index) Employe(nom, role, trait) else e }
            repository.sauvegarderBoutique(boutique.copy(employes = employesMaj))
        }
    }

    /** Service ajouté par le MJ : marqué personnalisé pour survivre aux nouvelles visites. */
    fun onAjouterService(boutique: Boutique, service: Service) {
        viewModelScope.launch {
            repository.sauvegarderBoutique(boutique.copy(services = boutique.services + service.copy(personnalise = true)))
        }
    }

    /** Un service paramétré par le MJ devient personnalisé (il n'est plus régénéré). */
    fun onModifierService(boutique: Boutique, index: Int, service: Service) {
        viewModelScope.launch {
            val servicesMaj = boutique.services.mapIndexed { i, s -> if (i == index) service.copy(personnalise = true) else s }
            repository.sauvegarderBoutique(boutique.copy(services = servicesMaj))
        }
    }

    fun onSupprimerService(boutique: Boutique, index: Int) {
        viewModelScope.launch {
            repository.sauvegarderBoutique(boutique.copy(services = boutique.services.filterIndexed { i, _ -> i != index }))
        }
    }

    /** Régénère le trait de caractère du marchand (bouton dé sur l'écran de détail). */
    fun onRegenererTraitMarchand(boutique: Boutique) {
        viewModelScope.launch {
            repository.sauvegarderBoutique(boutique.copy(marchand = boutique.marchand.copy(trait = genererTraitCaractere())))
        }
    }

    /** [index] car Employe n'a pas d'identifiant propre — suppression par position dans la liste affichée. */
    fun onSupprimerEmploye(boutique: Boutique, index: Int) {
        viewModelScope.launch {
            repository.sauvegarderBoutique(boutique.copy(employes = boutique.employes.filterIndexed { i, _ -> i != index }))
        }
    }

    fun onAjouterArticle(boutique: Boutique, equipement: EquipementReference, prix: Int, quantite: Int, toujoursDisponible: Boolean = false) {
        viewModelScope.launch {
            val article = ArticleEnVente(equipement = equipement, prixApplique = prix, quantiteStock = quantite, toujoursDisponible = toujoursDisponible)
            repository.sauvegarderBoutique(boutique.copy(inventaire = boutique.inventaire + article))
        }
    }

    fun onSupprimerArticle(boutique: Boutique, index: Int) {
        viewModelScope.launch {
            repository.sauvegarderBoutique(boutique.copy(inventaire = boutique.inventaire.filterIndexed { i, _ -> i != index }))
        }
    }

    /** [index] car ArticleEnVente n'a pas d'identifiant propre — même convention que la suppression. */
    fun onToggleArticlePermanent(boutique: Boutique, index: Int) {
        viewModelScope.launch {
            val inventaireMaj = boutique.inventaire.mapIndexed { i, article ->
                if (i == index) article.copy(toujoursDisponible = !article.toujoursDisponible) else article
            }
            repository.sauvegarderBoutique(boutique.copy(inventaire = inventaireMaj))
        }
    }

    /** [index] car Service n'a pas d'identifiant propre — même convention que les articles. */
    fun onToggleServiceActif(boutique: Boutique, index: Int) {
        viewModelScope.launch {
            val servicesMaj = boutique.services.mapIndexed { i, service ->
                if (i == index) service.copy(actif = !service.actif) else service
            }
            repository.sauvegarderBoutique(boutique.copy(services = servicesMaj))
        }
    }

    /** Le groupe visite la boutique : stock/services tournent, l'argent disponible se réinitialise. */
    fun onNouvelleVisite(boutique: Boutique) {
        viewModelScope.launch {
            val boutiqueMaj = effectuerVisite(boutique, mondeActif())
            repository.sauvegarderBoutique(boutiqueMaj)
        }
    }
}