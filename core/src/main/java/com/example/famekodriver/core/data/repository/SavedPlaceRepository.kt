package com.example.famekodriver.core.data.repository

import android.content.Context
import androidx.work.*
import com.example.famekodriver.core.data.local.AppDatabase
import com.example.famekodriver.core.data.local.entity.SavedPlaceEntity
import com.example.famekodriver.core.data.local.entity.SyncOperationEntity
import com.example.famekodriver.core.data.worker.SavedPlaceSyncWorker
import com.example.famekodriver.core.domain.model.SavedPlace
import com.example.famekodriver.core.network.NetworkClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavedPlaceRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val savedPlaceDao = database.savedPlaceDao()
    private val syncQueueDao = database.syncQueueDao()
    private val api = NetworkClient.famekoApi
    private val gson = Gson()

    private val _savedPlaces = MutableStateFlow<List<SavedPlace>>(emptyList())
    val savedPlaces: StateFlow<List<SavedPlace>> = _savedPlaces.asStateFlow()

    init {
        savedPlaceDao.getSavedPlacesFlow()
            .map { entities -> entities.map { it.toDomainModel() } }
            .onEach { _savedPlaces.value = it }
            .launchIn(CoroutineScope(Dispatchers.IO))
    }

    suspend fun fetchSavedPlaces(customerId: String): Result<Unit> {
        return try {
            val networkPlaces = api.getSavedPlaces(customerId)
            savedPlaceDao.insertPlaces(networkPlaces.map { it.toEntity() })
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlace(place: SavedPlace) {
        savedPlaceDao.insertPlace(place.toEntity())
        enqueueSyncOperation("CREATE", place.id, gson.toJson(place))
    }

    suspend fun updateSavedPlace(place: SavedPlace) {
        savedPlaceDao.insertPlace(place.toEntity())
        enqueueSyncOperation("UPDATE", place.id, gson.toJson(place))
    }

    suspend fun deleteSavedPlace(placeId: String) {
        savedPlaceDao.deletePlace(placeId)
        enqueueSyncOperation("DELETE", placeId)
    }

    private suspend fun enqueueSyncOperation(operation: String, placeId: String, data: String? = null) {
        syncQueueDao.enqueue(SyncOperationEntity(
            operation = operation,
            placeId = placeId,
            data = data
        ))
        triggerSyncWorker()
    }

    private fun triggerSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SavedPlaceSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SavedPlaceSync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun SavedPlaceEntity.toDomainModel() = SavedPlace(
        id = id,
        customerId = customerId,
        label = label,
        address = address,
        latitude = latitude,
        longitude = longitude
    )

    private fun SavedPlace.toEntity() = SavedPlaceEntity(
        id = id,
        customerId = customerId,
        label = label,
        address = address,
        latitude = latitude,
        longitude = longitude
    )
}
