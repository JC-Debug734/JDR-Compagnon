package com.jc2.jdrcompagnon.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jc2.jdrcompagnon.feature_boutique.data.local.ArticleEnVenteEntity
import com.jc2.jdrcompagnon.feature_boutique.data.local.BoutiqueDao
import com.jc2.jdrcompagnon.feature_boutique.data.local.BoutiqueEntity
import com.jc2.jdrcompagnon.feature_boutique.data.local.EmployeEntity
import com.jc2.jdrcompagnon.feature_boutique.data.local.ServiceEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.CarteCampagneEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.CarteDao
import com.jc2.jdrcompagnon.feature_carte.data.local.EvenementAleatoireEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.LieuNotableEntity
import com.jc2.jdrcompagnon.feature_carte.data.local.PointInteretEntity

/**
 * Base Room unique de l'app (single source of truth pour toute donnée locale relationnelle).
 * Placée dans core/ car elle est appelée à être partagée par de futures features
 * (ne pas créer une base Room par feature).
 */
@Database(
    entities = [
        BoutiqueEntity::class,
        ArticleEnVenteEntity::class,
        EmployeEntity::class,
        ServiceEntity::class,
        PointInteretEntity::class,
        CarteCampagneEntity::class,
        EvenementAleatoireEntity::class,
        LieuNotableEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boutiqueDao(): BoutiqueDao
    abstract fun carteDao(): CarteDao

    companion object {
        const val NOM_BASE = "jdrcompagnon.db"
    }
}
