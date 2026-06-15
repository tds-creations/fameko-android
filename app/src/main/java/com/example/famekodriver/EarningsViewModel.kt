package com.example.famekodriver

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.DriverStats
import kotlinx.coroutines.launch

class EarningsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DriverRepository()
    private val sessionManager = SessionManager(application)

    var stats by mutableStateOf(DriverStats())
        private set
    
    var isDailyFeePaid by mutableStateOf(false)
        private set
    
    var dailyFeeAmount by mutableDoubleStateOf(0.0)
        private set
    
    var isLoading by mutableStateOf(true)
        private set
    
    var checkoutUrl by mutableStateOf<String?>(null)
    var isPaying by mutableStateOf(false)

    var showManualProofDialog by mutableStateOf(false)
    var manualReference by mutableStateOf("")
    var momoNumber by mutableStateOf("")

    init {
        refresh()
    }

    fun generateReference(): String {
        val ref = "DF-" + (1000..9999).random() + "-" + (1000..9999).random()
        manualReference = ref
        return ref
    }

    fun refresh() {
        val id = sessionManager.getDriverId() ?: ""
        if (id.isEmpty()) {
            isLoading = false
            return
        }
        isLoading = true
        viewModelScope.launch {
            try {
                repository.getDriverStats(id).onSuccess { stats = it }
                repository.getDriverStatus(id).onSuccess { resp ->
                    isDailyFeePaid = resp.isDailyFeePaid
                    dailyFeeAmount = resp.dailyFeeAmount
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun payDailyFee(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val id = sessionManager.getDriverId() ?: ""
        if (id.isEmpty()) {
            onError("User session not found. Please log in again.")
            return
        }
        isPaying = true
        viewModelScope.launch {
            repository.payDailyFee(id).onSuccess { url ->
                if (url != null) {
                    checkoutUrl = url
                } else {
                    // This happens in TEST mode where it's auto-approved
                    refresh()
                    onSuccess("Daily fee approved automatically (Test Mode)")
                }
                isPaying = false
            }.onFailure {
                isPaying = false
                onError(it.message ?: "Payment initialization failed")
            }
        }
    }

    fun submitManualDailyFee(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val id = sessionManager.getDriverId() ?: ""
        if (id.isEmpty()) {
            onError("User session not found")
            return
        }
        if (manualReference.isBlank()) {
            onError("Reference cannot be empty")
            return
        }
        isPaying = true
        viewModelScope.launch {
            // Use DF_ prefix to indicate Daily Fee
            repository.requestTopUp(id, dailyFeeAmount.toInt(), "DF_$manualReference").onSuccess {
                isPaying = false
                showManualProofDialog = false
                manualReference = ""
                onSuccess()
            }.onFailure {
                isPaying = false
                onError(it.message ?: "Submission failed")
            }
        }
    }

}
