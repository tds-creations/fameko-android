package com.example.famekodriver.core.data.local.dao

import androidx.room.*
import com.example.famekodriver.core.data.local.entity.SyncOperationEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(operation: SyncOperationEntity)

    @Update
    suspend fun update(operation: SyncOperationEntity)

    @Delete
    suspend fun delete(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_queue WHERE placeId = :placeId")
    suspend fun deleteByPlaceId(placeId: String)
}
