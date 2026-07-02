package com.example.famekodriver.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.famekodriver.core.data.local.dao.SavedPlaceDao
import com.example.famekodriver.core.data.local.dao.SyncQueueDao
import com.example.famekodriver.core.data.local.entity.SavedPlaceEntity
import com.example.famekodriver.core.data.local.entity.SyncOperationEntity

@Database(entities = [SavedPlaceEntity::class, SyncOperationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fameko_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
