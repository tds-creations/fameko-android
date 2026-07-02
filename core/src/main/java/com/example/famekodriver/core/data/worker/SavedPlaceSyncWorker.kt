package com.example.famekodriver.core.data.worker

import android.content.Context
import androidx.work.*
import com.example.famekodriver.core.data.local.AppDatabase
import com.example.famekodriver.core.domain.model.SavedPlace
import com.example.famekodriver.core.network.NetworkClient
import com.google.gson.Gson

class SavedPlaceSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val database = AppDatabase.getDatabase(context)
    private val syncQueueDao = database.syncQueueDao()
    private val api = NetworkClient.famekoApi
    private val gson = Gson()

    override suspend fun doWork(): Result {
        val pendingOps = syncQueueDao.getPendingOperations()
        if (pendingOps.isEmpty()) return Result.success()

        var hasError = false

        for (op in pendingOps) {
            try {
                val success = when (op.operation) {
                    "CREATE" -> {
                        val place = gson.fromJson(op.data, SavedPlace::class.java)
                        val response = api.savePlace(place)
                        response["success"] == true
                    }
                    "UPDATE" -> {
                        val place = gson.fromJson(op.data, SavedPlace::class.java)
                        val response = api.updateSavedPlace(op.placeId, place)
                        response["success"] == true
                    }
                    "DELETE" -> {
                        val response = api.deleteSavedPlace(op.placeId)
                        response["success"] == true
                    }
                    else -> true
                }

                if (success) {
                    syncQueueDao.delete(op)
                } else {
                    handleFailure(op)
                    hasError = true
                }
            } catch (e: Exception) {
                handleFailure(op)
                hasError = true
            }
        }

        return if (hasError) Result.retry() else Result.success()
    }

    private suspend fun handleFailure(op: com.example.famekodriver.core.data.local.entity.SyncOperationEntity) {
        if (op.retries < 3) {
            syncQueueDao.update(op.copy(retries = op.retries + 1))
        } else {
            syncQueueDao.update(op.copy(status = "FAILED"))
        }
    }
}
