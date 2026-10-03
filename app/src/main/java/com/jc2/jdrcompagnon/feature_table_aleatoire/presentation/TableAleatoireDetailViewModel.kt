package com.jc2.jdrcompagnon.feature_table_aleatoire.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jc2.jdrcompagnon.feature_evenement.data.EvenementRepository
import com.jc2.jdrcompagnon.feature_evenement.domain.model.Evenement
import com.jc2.jdrcompagnon.feature_evenement.domain.model.TypeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.TableAleatoireRepository
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.EntreeEvenement
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.ResultatLoot
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.model.TableAleatoire

import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.TirageTableUseCase
import com.jc2.jdrcompagnon.feature_table_aleatoire.domain.usecase.VerifierDeclenchementTableUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/** ViewModel de l'écran détail (TableAleatoireDetailScreen) : charge, republie à chaque modification. */
class TableAleatoireDetailViewModel(
    private val tableId: String,
    private val repository: TableAleatoireRepository,
    private val evenementRepository: EvenementRepository,
    private val verifierDeclenchement: VerifierDeclenchementTableUseCase,
    private val tirage: TirageTableUseCase,
    val minutesLecture: StateFlow<Long?>, // LectureScenarioState.minutesEcoulees, fourni par DI
) : ViewModel() {

    private val _table = MutableStateFlow<TableAleatoire?>(null)
    val table: StateFlow<TableAleatoire?> = _table

    init {
        viewModelScope.launch {
            _table.value = repository.getTable(tableId)
        }
    }

    fun mettreAJour(updated: TableAleatoire) {
        _table.value = updated
        viewModelScope.launch { repository.sauvegarder(updated) }
    }

    /** Rattache des événements de la bibliothèque (poids 1) ; ceux déjà présents sont ignorés. */
    fun ajouterEvenements(evenements: List<Evenement>) {
        val current = _table.value ?: return
        val dejaPresents = current.entreesEvenements.map { it.evenementId }.toSet()
        val nouvelles = evenements.filter { it.id !in dejaPresents }
            .map { EntreeEvenement(evenementId = it.id, evenement = it) }
        if (nouvelles.isNotEmpty()) mettreAJour(current.copy(entreesEvenements = current.entreesEvenements + nouvelles))
    }

    /** Crée l'événement dans la bibliothèque puis le rattache à la table. */
    fun creerEvenement(evenement: Evenement) {
        viewModelScope.launch {
            evenementRepository.sauvegarder(evenement)
            ajouterEvenements(listOf(evenement))
        }
    }

    /** Modifie l'événement dans la bibliothèque : répercuté partout où il est utilisé. */
    fun modifierEvenement(evenement: Evenement) {
        viewModelScope.launch { evenementRepository.sauvegarder(evenement) }
        val current = _table.value ?: return
        _table.value = current.copy(
            entreesEvenements = current.entreesEvenements.map { if (it.evenementId == evenement.id) it.copy(evenement = evenement) else it }
        )
    }

    fun changerPoids(entreeId: String, poids: Int) {
        val current = _table.value ?: return
        mettreAJour(
            current.copy(
                entreesEvenements = current.entreesEvenements.map { if (it.id == entreeId) it.copy(poids = poids.coerceIn(1, 99)) else it }
            )
        )
    }

    /** Retire l'entrée de la table ; l'événement reste dans la bibliothèque. */
    fun retirerEntree(entreeId: String) {
        val current = _table.value ?: return
        mettreAJour(current.copy(entreesEvenements = current.entreesEvenements.filterNot { it.id == entreeId }))
    }

    fun estDue(minutes: Long? = minutesLecture.value): Boolean {
        val current = _table.value ?: return false
        return verifierDeclenchement(current, minutes)
    }

    fun minutesRestantes(minutes: Long? = minutesLecture.value): Long {
        val current = _table.value ?: return 0
        return verifierDeclenchement.minutesRestantes(current, minutes)
    }

    /**
     * Tire une entrée d'événement (filtrée par [typeFiltre] si fourni) au sein du pool pondéré,
     * puis acquitte le déclenchement — le compteur du prochain tirage repart du temps de fiction
     * courant, que le tirage soit automatique (table due) ou manuel (bouton "Tirer maintenant").
     */
    fun tirerEvenement(typeFiltre: TypeEvenement?, random: Random = Random(System.nanoTime())): Evenement? {
        val current = _table.value ?: return null
        val (tableMiseAJour, resultat) = tirage.tirerEvenement(current, typeFiltre, minutesLecture.value, random)
        if (resultat != null) mettreAJour(tableMiseAJour)
        return resultat
    }

    /**
     * Tire [n] entrées de loot (sans doublon, pondérées), chacune avec une quantité aléatoire
     * entre son minimum et son maximum, et acquitte le déclenchement comme [tirerEvenement].
     */
    fun tirerLoot(n: Int = 1, random: Random = Random(System.nanoTime())): List<ResultatLoot> {
        val current = _table.value ?: return emptyList()
        val (tableMiseAJour, resultats) = tirage.tirerLoot(current, n, minutesLecture.value, random)
        if (resultats.isNotEmpty()) mettreAJour(tableMiseAJour)
        return resultats
    }
}
