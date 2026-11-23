package com.example.famekodriver.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.famekodriver.core.data.local.dao.NavigationRouteDao
import com.example.famekodriver.core.data.local.dao.SavedPlaceDao
import com.example.famekodriver.core.data.local.dao.SyncQueueDao
import com.example.famekodriver.core.data.local.entity.NavigationRouteEntity
import com.example.famekodriver.core.data.local.entity.SavedPlaceEntity
import com.example.famekodriver.core.data.local.entity.SyncOperationEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SavedPlaceEntity::class, SyncOperationEntity::class, NavigationRouteEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun navigationRouteDao(): NavigationRouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `navigation_routes` (
                        `orderId` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `pickupLat` REAL NOT NULL,
                        `pickupLng` REAL NOT NULL,
                        `dropOffLat` REAL NOT NULL,
                        `dropOffLng` REAL NOT NULL,
                        `originalPathJson` TEXT NOT NULL,
                        `instructionsJson` TEXT NOT NULL,
                        `currentSegmentIndex` INTEGER NOT NULL,
                        `remainingDistanceKm` REAL NOT NULL,
                        `remainingDurationMin` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`orderId`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fameko_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
