package com.example.famekodriver.core.utils

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

/**
 * Regression and Unit tests for Economy & Comfort automatic service matching eligibility,
 * dispatch filtering, and fare integrity.
 */
class ServiceMatchingEligibilityTest {

    data class DriverProfile(
        val id: Int,
        val name: String,
        val vehicleType: String, // "Car", "Okada", "Truck"
        val vehicleCategory: String, // "Economy", "Comfort"
        val isOnline: Boolean = true,
        val status: String = "APPROVED"
    )

    data class RideOrder(
        val orderId: Int,
        val requestedServiceCategory: String, // "Economy", "Comfort", "Okada", "Truck"
        val estimatedFare: Double
    )

    fun isDriverEligibleForRequest(driver: DriverProfile, requestedCategory: String): Boolean {
        if (!driver.isOnline || driver.status != "APPROVED") return false

        val req = requestedCategory.lowercase(Locale.US)
        val dCat = driver.vehicleCategory.lowercase(Locale.US)
        val dType = driver.vehicleType.lowercase(Locale.US)

        // Non-car specific vehicle types
        if (req.contains("okada") || req.contains("bike") || req.contains("motor")) {
            return dType.contains("okada") || dType.contains("bike") || dType.contains("motor")
        }
        if (req.contains("pragya") || req.contains("tricycle")) {
            return dType.contains("pragya") || dType.contains("tricycle")
        }
        if (req.contains("truck")) return dType.contains("truck")
        if (req.contains("aboboyaa")) return dType.contains("aboboyaa")
        if (req.contains("bicycle")) return dType.contains("bicycle")

        // Ride-hailing car matching (Economy vs Comfort)
        if (req.contains("comfort")) {
            // Only Comfort drivers can receive Comfort requests
            return dCat.contains("comfort")
        }

        // Economy or general car requests:
        // Both Economy and Comfort drivers are eligible!
        return dType.contains("car") || dType.contains("saloon") || dType.contains("taxi") || dCat.contains("economy") || dCat.contains("comfort")
    }

    fun calculateFinalFare(requestedCategory: String, driverCategory: String, baseFareEconomy: Double, baseFareComfort: Double): Double {
        // Fare MUST follow customer's requested service category regardless of driver category!
        val isComfort = requestedCategory.equals("Comfort", ignoreCase = true)
        return if (isComfort && driverCategory.isNotBlank()) baseFareComfort else baseFareEconomy
    }

    // --- PHASE 11.A CATEGORY ELIGIBILITY TESTS ---

    @Test
    fun testEconomyDriverEconomyRequestPass() {
        val driver = DriverProfile(1, "John", "Car", "Economy")
        assertTrue(isDriverEligibleForRequest(driver, "Economy"))
    }

    @Test
    fun testEconomyDriverComfortRequestReject() {
        val driver = DriverProfile(1, "John", "Car", "Economy")
        assertFalse(isDriverEligibleForRequest(driver, "Comfort"))
    }

    @Test
    fun testComfortDriverEconomyRequestPass() {
        val driver = DriverProfile(2, "Alice", "Car", "Comfort")
        assertTrue(isDriverEligibleForRequest(driver, "Economy"))
    }

    @Test
    fun testComfortDriverComfortRequestPass() {
        val driver = DriverProfile(2, "Alice", "Car", "Comfort")
        assertTrue(isDriverEligibleForRequest(driver, "Comfort"))
    }

    // --- PHASE 11.B DISPATCH TESTS ---

    @Test
    fun testEconomyRequestDispatchesToBothEconomyAndComfortDrivers() {
        val economyDriver = DriverProfile(10, "Driver A", "Car", "Economy")
        val comfortDriver = DriverProfile(11, "Driver B", "Car", "Comfort")
        val drivers = listOf(economyDriver, comfortDriver)

        val eligible = drivers.filter { isDriverEligibleForRequest(it, "Economy") }
        assertEquals(2, eligible.size)
        assertTrue(eligible.contains(economyDriver))
        assertTrue(eligible.contains(comfortDriver))
    }

    @Test
    fun testComfortRequestDispatchesToComfortDriverOnly() {
        val economyDriver = DriverProfile(10, "Driver A", "Car", "Economy")
        val comfortDriver = DriverProfile(11, "Driver B", "Car", "Comfort")
        val drivers = listOf(economyDriver, comfortDriver)

        val eligible = drivers.filter { isDriverEligibleForRequest(it, "Comfort") }
        assertEquals(1, eligible.size)
        assertEquals(comfortDriver, eligible.first())
    }

    @Test
    fun testOfflineOrUnapprovedComfortDriverIsExcluded() {
        val offlineComfortDriver = DriverProfile(12, "Driver C", "Car", "Comfort", isOnline = false)
        val unapprovedDriver = DriverProfile(13, "Driver D", "Car", "Comfort", status = "PENDING")

        assertFalse(isDriverEligibleForRequest(offlineComfortDriver, "Comfort"))
        assertFalse(isDriverEligibleForRequest(unapprovedDriver, "Comfort"))
    }

    // --- PHASE 11.C PRICING TESTS ---

    @Test
    fun testComfortDriverAcceptingEconomyRequestChargedEconomyFare() {
        val order = RideOrder(1001, "Economy", estimatedFare = 15.0)
        val comfortDriver = DriverProfile(2, "Alice", "Car", "Comfort")

        val appliedFare = calculateFinalFare(
            requestedCategory = order.requestedServiceCategory,
            driverCategory = comfortDriver.vehicleCategory,
            baseFareEconomy = 15.0,
            baseFareComfort = 22.0
        )

        assertEquals(15.0, appliedFare, 0.001)
        assertEquals("Comfort", comfortDriver.vehicleCategory) // Driver profile category remains unchanged!
    }

    @Test
    fun testComfortDriverAcceptingComfortRequestChargedComfortFare() {
        val order = RideOrder(1002, "Comfort", estimatedFare = 22.0)
        val comfortDriver = DriverProfile(2, "Alice", "Car", "Comfort")

        val appliedFare = calculateFinalFare(
            requestedCategory = order.requestedServiceCategory,
            driverCategory = comfortDriver.vehicleCategory,
            baseFareEconomy = 15.0,
            baseFareComfort = 22.0
        )

        assertEquals(22.0, appliedFare, 0.001)
    }

    // --- PHASE 11.F RIDE VS DELIVERY SEPARATION ---

    @Test
    fun testRideCategoriesDoNotInterfereWithDeliveryVehicleTypes() {
        val okadaDriver = DriverProfile(30, "Rider", "Okada", "Economy")
        val truckDriver = DriverProfile(31, "Hauler", "Truck", "Economy")

        assertFalse(isDriverEligibleForRequest(okadaDriver, "Comfort"))
        assertFalse(isDriverEligibleForRequest(truckDriver, "Comfort"))
        assertTrue(isDriverEligibleForRequest(okadaDriver, "Okada"))
        assertTrue(isDriverEligibleForRequest(truckDriver, "Truck"))
    }
}
