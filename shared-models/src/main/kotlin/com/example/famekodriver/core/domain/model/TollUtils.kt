package com.example.famekodriver.core.domain.model

object TollUtils {
    fun isAirportTollZone(locationName: String?, lat: Double?, lng: Double?): Boolean {
        if (!locationName.isNullOrBlank()) {
            val name = locationName.lowercase()
            if (name.contains("airport") || name.contains("kotoka") || name.contains("acc") || name.contains("terminal 3") || name.contains("airport city")) {
                return true
            }
        }
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            // Kotoka International Airport / Airport City GPS Box
            if (lat in 5.5900..5.6200 && lng in -0.1800..-0.1500) {
                return true
            }
        }
        return false
    }

    fun calculateTollFee(
        pickupLocation: String?, pickupLat: Double?, pickupLng: Double?,
        dropOffLocation: String?, dropOffLat: Double?, dropOffLng: Double?,
        standardToll: Double = 10.0
    ): Double {
        val isPickupToll = isAirportTollZone(pickupLocation, pickupLat, pickupLng)
        val isDropOffToll = isAirportTollZone(dropOffLocation, dropOffLat, dropOffLng)
        return if (isPickupToll || isDropOffToll) standardToll else 0.0
    }
}
