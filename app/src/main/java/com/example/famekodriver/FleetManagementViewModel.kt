package com.example.famekodriver

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch

class FleetManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DriverRepository.getInstance()
    private val sessionManager = SessionManager(application.applicationContext)
    
    var vehicles by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    
    val companyName = sessionManager.getCompanyName() ?: "My Fleet"
    val ownerIdStr = sessionManager.getDriverId() ?: ""
    private val userRole = sessionManager.getUserRole()

    init {
        loadVehicles()
    }

    fun loadVehicles() {
        if (ownerIdStr.isBlank()) {
            isLoading = false
            return
        }
        isLoading = true
        viewModelScope.launch {
            repository.getFleetVehicles(ownerIdStr, userRole).onSuccess {
                vehicles = it
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }
}
