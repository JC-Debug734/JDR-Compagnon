package com.jc2.jdrcompagnon.feature_table_aleatoire.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.TableAleatoireRepository
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TypeTable
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.VerifierDeclenchementTableUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** ViewModel de l'écran liste (TableAleatoireListScreen) : mêmes responsabilités qu'EnvironmentViewModel. */
class TableAleatoireListViewModel(
    private val repository: TableAleatoireRepository,
    private val verifierDeclenchement: VerifierDeclenchementTableUseCase,
    // Fourni par DI (LectureScenarioState.minutesEcoulees), pas de couplage direct ici : temps de
    // scénario compté depuis l'ouverture du scénario en lecture, null si aucun n'est ouvert.
    val minutesLecture: StateFlow<Long?>,
) : ViewModel() {

    fun observerTables(worldId: String): Flow<List<TableAleatoire>> = repository.observerTables(worldId)

    fun estDue(table: TableAleatoire, minutesLecture: Long?): Boolean =
        verifierDeclenchement(table, minutesLecture)

    fun creer(nom: String, worldId: String, type: TypeTable) {
        viewModelScope.launch {
            repository.sauvegarder(TableAleatoire(nom = nom, worldId = worldId, type = type))
        }
    }

    fun supprimer(id: String) {
        viewModelScope.launch { repository.supprimer(id) }
    }
}
