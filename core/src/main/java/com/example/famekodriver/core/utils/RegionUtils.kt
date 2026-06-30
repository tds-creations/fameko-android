package com.example.famekodriver.core.utils

object RegionUtils {
    val GHANA_REGIONS = listOf(
        "Ahafo", "Ashanti", "Bono", "Bono East", "Central", "Eastern",
        "Greater Accra", "Northern", "North East", "Oti", "Savannah",
        "Upper East", "Upper West", "Volta", "Western", "Western North"
    )

    fun extractRegion(address: String?): String? {
        if (address == null) return null
        return GHANA_REGIONS.find { region ->
            address.contains(region, ignoreCase = true)
        }
    }
}
