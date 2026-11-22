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

@Database(entities = [SavedPlaceEntity::class, SyncOperationEntity::class, NavigationRouteEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun navigationRouteDao(): NavigationRouteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fameko_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
