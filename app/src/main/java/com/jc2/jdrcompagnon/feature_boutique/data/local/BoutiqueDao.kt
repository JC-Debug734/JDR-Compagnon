package com.jc2.jdrcompagnon.feature_boutique.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Agrège une boutique avec ses articles, employés et services en une seule requête. */
data class BoutiqueAvecDetails(
    @Embedded val boutique: BoutiqueEntity,
    @Relation(parentColumn = "id", entityColumn = "boutiqueId")
    val articles: List<ArticleEnVenteEntity>,
    @Relation(parentColumn = "id", entityColumn = "boutiqueId")
    val employes: List<EmployeEntity>,
    @Relation(parentColumn = "id", entityColumn = "boutiqueId")
    val services: List<ServiceEntity>
)

@Dao
interface BoutiqueDao {

    @Transaction
    @Query("SELECT * FROM boutiques ORDER BY nom ASC")
    fun observerToutesLesBoutiques(): Flow<List<BoutiqueAvecDetails>>

    @Transaction
    @Query("SELECT * FROM boutiques WHERE id = :id")
    suspend fun getBoutiqueParId(id: String): BoutiqueAvecDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererBoutique(boutique: BoutiqueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererArticles(articles: List<ArticleEnVenteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererEmployes(employes: List<EmployeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererServices(services: List<ServiceEntity>)

    @Query("DELETE FROM articles_en_vente WHERE boutiqueId = :boutiqueId")
    suspend fun supprimerArticlesDeBoutique(boutiqueId: String)

    @Query("DELETE FROM employes_boutique WHERE boutiqueId = :boutiqueId")
    suspend fun supprimerEmployesDeBoutique(boutiqueId: String)

    @Query("DELETE FROM services_boutique WHERE boutiqueId = :boutiqueId")
    suspend fun supprimerServicesDeBoutique(boutiqueId: String)

    @Query("DELETE FROM boutiques WHERE id = :id")
    suspend fun supprimerBoutique(id: String)

    /**
     * Remplace intégralement le contenu (articles/employés/services) d'une boutique.
     * Les anciennes lignes sont supprimées avant réinsertion : sans ça, articles/employés/
     * services retirés côté domaine restaient orphelins en base (id autoGenerate, donc jamais
     * remplacés par OnConflictStrategy.REPLACE).
     */
    @Transaction
    suspend fun remplacerBoutiqueComplete(
        boutique: BoutiqueEntity,
        articles: List<ArticleEnVenteEntity>,
        employes: List<EmployeEntity>,
        services: List<ServiceEntity>
    ) {
        insererBoutique(boutique)
        supprimerArticlesDeBoutique(boutique.id)
        insererArticles(articles)
        supprimerEmployesDeBoutique(boutique.id)
        insererEmployes(employes)
        supprimerServicesDeBoutique(boutique.id)
        insererServices(services)
    }
}