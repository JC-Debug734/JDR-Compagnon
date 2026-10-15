package com.jc2.jdrcompagnon.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.jc2.jdrcompagnon.core.database.AppDatabase
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepository
import com.jc2.jdrcompagnon.feature_boutique.data.BoutiqueRepositoryImpl
import com.jc2.jdrcompagnon.feature_boutique.data.SrdEquipementAdapter
import com.jc2.jdrcompagnon.feature_boutique.domain.model.EquipementReference
import com.jc2.jdrcompagnon.feature_boutique.domain.model.StandingBoutique
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ApprovisionnerBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CalculerPrixArticleUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.CreerBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.DeterminerBudgetAchatUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.EffectuerVisiteBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.EquipementSourcePort
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererEmployesUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererInventaireUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererNomBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererNomMarchandUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererServicesUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererStandingAleatoireUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.GenererTraitCaractereUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ListerEquipementsDisponiblesUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ModifierBoutiqueUseCase
import com.jc2.jdrcompagnon.feature_boutique.domain.usecase.ReparerPrixBoutiquesUseCase
import kotlinx.coroutines.launch
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueDetailViewModel
import com.jc2.jdrcompagnon.feature_boutique.presentation.BoutiqueViewModel
import com.jc2.jdrcompagnon.ui.GameState

/**
 * ⚠️ Le projet n'utilise actuellement ni Hilt ni Koin (GameState, SrdRepository sont de
 * simples singletons `object` initialisés avec un Context). Ce provider suit donc la même
 * convention plutôt que d'introduire un framework de DI pour cette seule feature.
 *
 * Si le projet passe un jour à Hilt/Koin globalement, ce fichier redevient un simple
 * @Module / module Koin — les Use Cases et le Repository, eux, ne changent pas.
 *
 * [init] doit être appelé une fois avec un Context applicatif avant toute utilisation,
 * par exemple depuis GameState.init(context) (voir la modification apportée à GameState.kt).
 */
object BoutiqueDependencies {

    private var database: AppDatabase? = null

    fun init(context: Context) {
        if (database != null) return
        database = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.NOM_BASE
        )
            .addMigrations(AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_11_12, AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15, AppDatabase.MIGRATION_15_16, AppDatabase.MIGRATION_16_17, AppDatabase.MIGRATION_17_18, AppDatabase.MIGRATION_18_19, AppDatabase.MIGRATION_19_20, AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22, AppDatabase.MIGRATION_22_23, AppDatabase.MIGRATION_23_24, AppDatabase.MIGRATION_24_25, AppDatabase.MIGRATION_25_26, AppDatabase.MIGRATION_26_27, AppDatabase.MIGRATION_27_28, AppDatabase.MIGRATION_28_29, AppDatabase.MIGRATION_29_30, AppDatabase.MIGRATION_30_31, AppDatabase.MIGRATION_31_32, AppDatabase.MIGRATION_32_33)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /** Interne au module (pas private) : réutilisé par CarteDependencies pour partager la même base Room. */
    internal fun requireDatabase(): AppDatabase =
        database ?: error("BoutiqueDependencies.init(context) doit être appelé avant toute utilisation (voir GameState.init).")

    val repository: BoutiqueRepository by lazy {
        BoutiqueRepositoryImpl(requireDatabase().boutiqueDao())
    }

    private var applicationContext: Context? = null

    /** Enregistré séparément de [database] car SrdEquipementAdapter a aussi besoin du Context. */
    fun initEquipementSource(context: Context) {
        applicationContext = context.applicationContext
    }

    private val equipementSource: EquipementSourcePort by lazy {
        SrdEquipementAdapter(
            applicationContext ?: error("BoutiqueDependencies.initEquipementSource(context) doit être appelé avant toute utilisation.")
        )
    }

    private val calculerPrixArticle = CalculerPrixArticleUseCase()
    private val genererStandingAleatoire = GenererStandingAleatoireUseCase()
    private val genererTraitCaractere = GenererTraitCaractereUseCase()
    private val genererEmployes = GenererEmployesUseCase(genererTraitCaractere)
    private val genererServices = GenererServicesUseCase()
    private val genererInventaire by lazy { GenererInventaireUseCase(equipementSource, calculerPrixArticle) }
    private val determinerBudgetAchatUseCase = DeterminerBudgetAchatUseCase()
    private val creerBoutiqueUseCase by lazy {
        CreerBoutiqueUseCase(genererStandingAleatoire, genererInventaire, genererEmployes, genererServices, determinerBudgetAchatUseCase, genererTraitCaractere)
    }
    private val modifierBoutiqueUseCase by lazy {
        ModifierBoutiqueUseCase(genererStandingAleatoire, genererInventaire, genererEmployes, genererServices)
    }
    private val effectuerVisiteBoutiqueUseCase by lazy {
        EffectuerVisiteBoutiqueUseCase(genererInventaire, genererServices, determinerBudgetAchatUseCase)
    }
    private val genererNomBoutiqueUseCase = GenererNomBoutiqueUseCase()
    private val genererNomMarchandUseCase = GenererNomMarchandUseCase()
    private val listerEquipementsUseCase by lazy { ListerEquipementsDisponiblesUseCase(equipementSource) }
    private val approvisionnerBoutiqueUseCase by lazy {
        ApprovisionnerBoutiqueUseCase(equipementSource, calculerPrixArticle)
    }

    /** Prix à 0 des anciennes boutiques (« Gratuit ») : réparés une fois au lancement. */
    fun reparerPrixAZero() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                ReparerPrixBoutiquesUseCase(repository, equipementSource, calculerPrixArticle, genererServices)(listOf(mondeActif(), "donjon_et_dragon"))
            }.onFailure { android.util.Log.e("BoutiqueDependencies", "Réparation des prix impossible", it) }
        }
    }

    /** Le monde actif est lu depuis GameState au moment de l'appel, jamais mis en cache ici. */
    private fun mondeActif(): String = GameState.currentWorld.value?.id ?: "donjon_et_dragon"

    /** Objet du SRD de [monde] portant ce nom (casse ignorée), avec son coût de base ; null = inconnu ou sans prix. */
    suspend fun equipementSrd(nomObjet: String, monde: String? = null): EquipementReference? =
        equipementSource.getEquipementsDisponibles(monde?.ifBlank { null } ?: mondeActif())
            .firstOrNull { it.nom.equals(nomObjet.trim(), ignoreCase = true) }

    /** Prix appliqué par une boutique de ce standing à un objet de ce coût de base. */
    fun prixEnBoutique(coutBaseEnPc: Int, standing: StandingBoutique): Int = calculerPrixArticle(coutBaseEnPc, standing)

    fun newViewModel(): BoutiqueViewModel = BoutiqueViewModel(
        repository = repository,
        creerBoutique = creerBoutiqueUseCase,
        modifierBoutique = modifierBoutiqueUseCase,
        genererNomBoutique = genererNomBoutiqueUseCase,
        genererNomMarchand = genererNomMarchandUseCase,
        mondeActif = ::mondeActif,
        campagneCourante = GameState.currentCampaignId
    )

    fun newDetailViewModel(boutiqueId: String): BoutiqueDetailViewModel = BoutiqueDetailViewModel(
        boutiqueId = boutiqueId,
        repository = repository,
        listerEquipements = listerEquipementsUseCase,
        calculerPrixArticle = calculerPrixArticle,
        determinerBudgetAchat = determinerBudgetAchatUseCase,
        approvisionnerBoutique = approvisionnerBoutiqueUseCase,
        effectuerVisite = effectuerVisiteBoutiqueUseCase,
        genererNomEmploye = genererNomMarchandUseCase,
        genererTraitCaractere = genererTraitCaractere,
        mondeActif = ::mondeActif
    )
}

/** Factory Compose standard pour que BoutiqueViewModel survive aux rotations d'écran. */
class BoutiqueViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BoutiqueDependencies.newViewModel() as T
    }
}

/** Factory Compose pour BoutiqueDetailViewModel — un boutiqueId différent par instance. */
class BoutiqueDetailViewModelFactory(private val boutiqueId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BoutiqueDependencies.newDetailViewModel(boutiqueId) as T
    }
}