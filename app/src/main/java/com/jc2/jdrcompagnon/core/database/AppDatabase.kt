package com.jc2.jdrcompagnon.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.jc2.jdrcompagnon.feature_environnement.data.local.EnvironnementDao
import com.jc2.jdrcompagnon.feature_epreuve.data.local.EpreuveDao
import com.jc2.jdrcompagnon.feature_epreuve.data.local.EpreuveEntity
import com.jc2.jdrcompagnon.feature_evenement.data.local.EvenementDao
import com.jc2.jdrcompagnon.feature_evenement.data.local.EvenementEntity
import com.jc2.jdrcompagnon.feature_environnement.data.local.EnvironnementEntity
import com.jc2.jdrcompagnon.feature_historique.data.local.HistoriqueDao
import com.jc2.jdrcompagnon.feature_historique.data.local.HistoriqueEntreeEntity
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.local.TableAleatoireDao
import com.jc2.jdrcompagnon.feature_table_aleatoire.data.local.TableAleatoireEntity

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
        LieuNotableEntity::class,
        HistoriqueEntreeEntity::class,
        EnvironnementEntity::class,
        TableAleatoireEntity::class,
        EvenementEntity::class,
        EpreuveEntity::class
    ],
    version = 23,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boutiqueDao(): BoutiqueDao
    abstract fun carteDao(): CarteDao
    abstract fun historiqueDao(): HistoriqueDao
    abstract fun environnementDao(): EnvironnementDao
    abstract fun tableAleatoireDao(): TableAleatoireDao
    abstract fun evenementDao(): EvenementDao
    abstract fun epreuveDao(): EpreuveDao

    companion object {
        const val NOM_BASE = "jdrcompagnon.db"

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `historique_personnage` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `personnageId` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `description` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_historique_personnage_personnageId` ON `historique_personnage` (`personnageId`)"
                )
            }
        }

        /**
         * Épreuves environnementales rattachées aux environnements. Migration explicite (et non
         * destructive) pour ne pas effacer les données existantes lors de la montée de version.
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `environnements` ADD COLUMN `epreuvesJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** Terrains SRD et tables aléatoires liées aux environnements (filtre du bestiaire, tirages). */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `environnements` ADD COLUMN `terrainsJson` TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE `environnements` ADD COLUMN `tablesAleatoiresJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** Services de boutique ajoutés/paramétrés par le MJ, conservés lors d'une nouvelle visite. */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `services_boutique` ADD COLUMN `personnalise` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Plusieurs cartes par campagne : cartes_campagne passe d'une clé campagneId à un id propre
         * (la carte existante garde id = campagneId et devient "Carte principale"), et chaque point
         * d'intérêt peut désigner sa carte (carteId null = carte principale).
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cartes_campagne_new` (
                        `id` TEXT NOT NULL,
                        `campagneId` TEXT NOT NULL,
                        `nom` TEXT NOT NULL,
                        `largeurCases` INTEGER NOT NULL,
                        `hauteurCases` INTEGER NOT NULL,
                        `echelleKmParCase` INTEGER NOT NULL,
                        `imageFileName` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `cartes_campagne_new` (`id`, `campagneId`, `nom`, `largeurCases`, `hauteurCases`, `echelleKmParCase`, `imageFileName`)
                    SELECT `campagneId`, `campagneId`, 'Carte principale', `largeurCases`, `hauteurCases`, `echelleKmParCase`, `imageFileName`
                    FROM `cartes_campagne`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `cartes_campagne`")
                db.execSQL("ALTER TABLE `cartes_campagne_new` RENAME TO `cartes_campagne`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cartes_campagne_campagneId` ON `cartes_campagne` (`campagneId`)")
                db.execSQL("ALTER TABLE `points_interet` ADD COLUMN `carteId` TEXT")
            }
        }

        /**
         * carteId null ne veut plus dire "carte principale" mais "lieu non placé" : les lieux
         * existants sont rattachés explicitement à la carte historique de leur campagne (id =
         * campagneId), quand elle existe, pour rester affichés là où ils étaient.
         */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE `points_interet` SET `carteId` = `campagneId`
                    WHERE `carteId` IS NULL AND `campagneId` IN (SELECT `id` FROM `cartes_campagne`)
                    """.trimIndent()
                )
            }
        }

        /**
         * Lieux placés librement sur la carte (centre de l'icône en fraction de la carte, plus
         * attaché à une case) et visibilité des lieux pour les joueurs en exploration.
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `points_interet` ADD COLUMN `fx` REAL")
                db.execSQL("ALTER TABLE `points_interet` ADD COLUMN `fy` REAL")
                db.execSQL("ALTER TABLE `points_interet` ADD COLUMN `visibleJoueurs` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Bibliothèque d'événements (feature_evenement) : un événement est écrit une fois puis
         * référencé par id depuis les lieux (points_interet.evenementIds, ids séparés par des
         * virgules) et les environnements (environnements.evenementsJson). La conversion des
         * anciens événements (tables, campagnes, environnements) est faite en Kotlin, hors
         * migration SQL, car elle demande de lire du JSON et le monde de chaque campagne.
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `evenements` (
                        `id` TEXT NOT NULL,
                        `worldId` TEXT NOT NULL,
                        `campagneId` TEXT,
                        `type` TEXT NOT NULL,
                        `titre` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `effetsJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_evenements_worldId` ON `evenements` (`worldId`)")
                db.execSQL("ALTER TABLE `points_interet` ADD COLUMN `evenementIds` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `environnements` ADD COLUMN `evenementsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** Événements : ce qui est attendu des joueurs et issues possibles (réussite, échec...). */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `evenements` ADD COLUMN `objectif` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `evenements` ADD COLUMN `issuesJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** Événements : profils impliqués (monstres du bestiaire, PNJ), ouvrables à la résolution. */
        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `evenements` ADD COLUMN `profilsJson` TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /** Outil ÉPREUVES (feature_epreuve) : épreuves à réussites et complications d'échec. */
        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `epreuves` (
                        `id` TEXT NOT NULL,
                        `worldId` TEXT NOT NULL,
                        `nom` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `reussitesRequises` INTEGER NOT NULL,
                        `imageFileName` TEXT,
                        `complicationsJson` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_epreuves_worldId` ON `epreuves` (`worldId`)")
            }
        }

        /** Outil ÉPREUVES : musique d'ambiance jouée au lancement de l'épreuve. */
        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `epreuves` ADD COLUMN `musicTrackId` TEXT")
            }
        }

        /** Outil ÉPREUVES : difficulté (réussites et DD adaptés au groupe au lancement). */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `epreuves` ADD COLUMN `difficulte` TEXT")
            }
        }
    }
}
