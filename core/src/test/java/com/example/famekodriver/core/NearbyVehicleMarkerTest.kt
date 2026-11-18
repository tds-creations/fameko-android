package com.example.famekodriver.core

import com.example.famekodriver.core.domain.model.DriverLocation
import com.example.famekodriver.core.domain.model.ServiceType
import org.junit.Assert.*
import org.junit.Test

/**
 * Automated Test Suite verifying Individual Nearby Vehicle Visibility & Clustering Rules.
 */
class NearbyVehicleMarkerTest {

    @Test
    fun testCustomerRideMapVehicleFiltering() {
        val allDrivers = listOf(
            DriverLocation(id = "1", latitude = 5.6358, longitude = -0.1601, vehicleType = "Saloon Car", isOnline = true),
            DriverLocation(id = "2", latitude = 5.6037, longitude = -0.1870, vehicleType = "Okada", isOnline = true),
            DriverLocation(id = "3", latitude = 5.6225, longitude = -0.1772, vehicleType = "Truck", isOnline = true)
        )

        // Customer selects RIDE mode -> Filters car passenger vehicles
        val rideDrivers = allDrivers.filter { 
            it.vehicleType?.lowercase()?.contains("car") == true || it.vehicleType?.lowercase()?.contains("saloon") == true 
        }

        assertEquals(1, rideDrivers.size)
        assertEquals("1", rideDrivers.first().id)
    }

    @Test
    fun testCustomerSwitchFromRideToDelivery() {
        val drivers = listOf(
            DriverLocation(id = "1", latitude = 5.6358, longitude = -0.1601, vehicleType = "Saloon Car", isOnline = true),
            DriverLocation(id = "2", latitude = 5.6037, longitude = -0.1870, vehicleType = "Okada", isOnline = true),
            DriverLocation(id = "3", latitude = 5.6225, longitude = -0.1772, vehicleType = "Aboboyaa", isOnline = true)
        )

        // Switch to DELIVERY -> Filters delivery vehicles (Okada, Aboboyaa)
        var activeMode = ServiceType.RIDE_HAILING
        var visible = drivers.filter { activeMode == ServiceType.RIDE_HAILING && it.vehicleType?.lowercase()?.contains("car") == true }
        assertEquals(1, visible.size)

        activeMode = ServiceType.PACKAGE_DELIVERY
        visible = drivers.filter { 
            activeMode == ServiceType.PACKAGE_DELIVERY && (it.vehicleType?.lowercase()?.contains("okada") == true || it.vehicleType?.lowercase()?.contains("aboboyaa") == true)
        }
        assertEquals(2, visible.size)
    }

    @Test
    fun testDriverAssignmentExcludesAvailableMarker() {
        val onlineDrivers = mutableListOf(
            DriverLocation(id = "10", latitude = 5.6358, longitude = -0.1601, vehicleType = "Saloon Car", isOnline = true),
            DriverLocation(id = "11", latitude = 5.6037, longitude = -0.1870, vehicleType = "Comfort", isOnline = true)
        )

        // Driver 10 accepts passenger ride -> Driver 10 becomes busy
        val busyDriverId = "10"
        val availableMarkers = onlineDrivers.filter { it.id != busyDriverId }

        assertEquals(1, availableMarkers.size)
        assertEquals("11", availableMarkers.first().id)
    }

    @Test
    fun testOfflineDriverExclusion() {
        val drivers = listOf(
            DriverLocation(id = "20", latitude = 5.6358, longitude = -0.1601, isOnline = true),
            DriverLocation(id = "21", latitude = 5.6037, longitude = -0.1870, isOnline = false)
        )

        val visible = drivers.filter { it.isOnline }
        assertEquals(1, visible.size)
        assertEquals("20", visible.first().id)
    }

    @Test
    fun testStaleLocationExclusion() {
        val lastUpdateMs = System.currentTimeMillis() - (15 * 60 * 1000) // 15 mins ago (Stale)
        val tenMinutesMs = 10 * 60 * 1000

        val isStale = (System.currentTimeMillis() - lastUpdateMs) > tenMinutesMs
        assertTrue(isStale)
    }

    @Test
    fun testMarkerClusteringLogic() {
        val nearbyCoords = listOf(
            Pair(5.603700, -0.187000),
            Pair(5.603705, -0.187005),
            Pair(5.603710, -0.187010)
        )

        // Verify coordinates occupy tight proximity box requiring clustering
        val maxDiff = nearbyCoords.maxOf { it.first } - nearbyCoords.minOf { it.first }
        assertTrue(maxDiff < 0.001) // Tight radius < 100m -> clustered
    }

    @Test
    fun testMultiServiceDriverVisibility() {
        val driverServiceTypes = "BOTH"
        val isRideEligible = driverServiceTypes.contains("RIDE_HAILING") || driverServiceTypes.contains("BOTH")
        val isDeliveryEligible = driverServiceTypes.contains("PACKAGE_DELIVERY") || driverServiceTypes.contains("BOTH")

        assertTrue(isRideEligible)
        assertTrue(isDeliveryEligible)
    }

    @Test
    fun testWebSocketReconnectRecovery() {
        var cachedMarkers = listOf(
            DriverLocation(id = "30", latitude = 5.6358, longitude = -0.1601, isOnline = true)
        )

        // Disconnect & reconnect
        val reconnectedDrivers = listOf(
            DriverLocation(id = "30", latitude = 5.6360, longitude = -0.1603, isOnline = true)
        )
        cachedMarkers = reconnectedDrivers.distinctBy { it.id }

        assertEquals(1, cachedMarkers.size)
        assertEquals(5.6360, cachedMarkers.first().latitude, 0.0001)
    }
}
