package com.example.famekodriver.backend.services

import com.uber.h3core.H3Core

object H3Helper {
    private val h3: H3Core = H3Core.newInstance()
    private const val DEFAULT_RESOLUTION = 7

    fun getIndex(lat: Double, lng: Double, res: Int = DEFAULT_RESOLUTION): String {
        return h3.latLngToCellAddress(lat, lng, res)
    }

    fun getNeighbors(index: String): List<String> {
        return h3.gridDisk(index, 1)
    }
    
    fun getNeighborsInRadius(index: String, k: Int): List<String> {
        return h3.gridDisk(index, k)
    }
}
