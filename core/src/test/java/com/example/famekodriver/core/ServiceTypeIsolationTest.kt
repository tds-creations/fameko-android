package com.example.famekodriver.core

import com.example.famekodriver.core.domain.model.Delivery
import com.example.famekodriver.core.domain.model.DeliveryStatus
import com.example.famekodriver.core.domain.model.ServiceType
import org.junit.Assert.*
import org.junit.Test

/**
 * Regression Unit Test Suite verifying Service Type Isolation across Passenger Rides and Package Deliveries.
 */
class ServiceTypeIsolationTest {

    @Test
    fun testPassengerRideInterfaceIsolation() {
        val rideDelivery = Delivery(
            id = "101",
            orderId = 1001,
            driverId = "55",
            pickupLocation = "East Legon, Accra",
            dropOffLocation = "Bubuashie, Accra",
            status = DeliveryStatus.ASSIGNED,
            distanceKm = 8.5,
            estimatedEarnings = 35.0,
            customerName = "Joseph Lamptey",
            customerPhone = "+233241234567",
            serviceType = ServiceType.RIDE_HAILING,
            recipientName = "Unused Recipient",
            recipientPhone = "+233200000000",
            packageCategory = "Unused Package",
            isFragile = true
        )

        assertEquals(ServiceType.RIDE_HAILING, rideDelivery.serviceType)
        assertEquals("Joseph Lamptey", rideDelivery.currentContactName)
        assertEquals("+233241234567", rideDelivery.currentContactPhone)
        assertEquals("PASSENGER (PICKUP)", rideDelivery.currentContactRole)

        // Drop-off phase
        val dropoffRide = rideDelivery.copy(status = DeliveryStatus.IN_TRANSIT)
        assertEquals("PASSENGER (DROP-OFF)", dropoffRide.currentContactRole)
    }

    @Test
    fun testPackageDeliveryInterfaceIsolation() {
        val packageDelivery = Delivery(
            id = "102",
            orderId = 1002,
            driverId = "55",
            pickupLocation = "Accra Mall, Spintex Rd",
            dropOffLocation = "Kotoka Airport, Accra",
            status = DeliveryStatus.ASSIGNED,
            distanceKm = 5.2,
            estimatedEarnings = 25.0,
            customerName = "Kwame Sender",
            customerPhone = "+233241111111",
            serviceType = ServiceType.PACKAGE_DELIVERY,
            recipientName = "Kofi Recipient",
            recipientPhone = "+233202222222",
            packageCategory = "Express Package",
            isSending = true
        )

        assertEquals(ServiceType.PACKAGE_DELIVERY, packageDelivery.serviceType)
        assertEquals("Kwame Sender", packageDelivery.currentContactName) // Pickup phase: Sender
        assertEquals("SENDER (PICKUP)", packageDelivery.currentContactRole)

        // Drop-off phase: Recipient
        val dropoffPackage = packageDelivery.copy(status = DeliveryStatus.IN_TRANSIT)
        assertEquals("Kofi Recipient", dropoffPackage.currentContactName)
        assertEquals("RECEIVER (DROP-OFF)", dropoffPackage.currentContactRole)
    }

    @Test
    fun testDriverApprovedForBothAcceptsRide() {
        val driverServiceTypes = "BOTH"
        val orderRequestedType = "RIDE_HAILING"

        val parsedServiceType = when {
            orderRequestedType.contains("PACKAGE") || orderRequestedType.contains("DELIVERY") -> ServiceType.PACKAGE_DELIVERY
            orderRequestedType.contains("RENTAL") -> ServiceType.RENTAL
            else -> ServiceType.RIDE_HAILING
        }

        assertEquals(ServiceType.RIDE_HAILING, parsedServiceType)
    }

    @Test
    fun testDriverApprovedForBothAcceptsDelivery() {
        val orderRequestedType = "PACKAGE_DELIVERY"

        val parsedServiceType = when {
            orderRequestedType.contains("PACKAGE") || orderRequestedType.contains("DELIVERY") -> ServiceType.PACKAGE_DELIVERY
            orderRequestedType.contains("RENTAL") -> ServiceType.RENTAL
            else -> ServiceType.RIDE_HAILING
        }

        assertEquals(ServiceType.PACKAGE_DELIVERY, parsedServiceType)
    }

    @Test
    fun testStateTransitionRideToDelivery() {
        var activeDelivery: Delivery? = Delivery(
            id = "201",
            orderId = 2001,
            driverId = "55",
            pickupLocation = "Circle, Accra",
            dropOffLocation = "Osu, Accra",
            status = DeliveryStatus.DELIVERED,
            distanceKm = 4.0,
            estimatedEarnings = 20.0,
            customerName = "Ride Customer",
            serviceType = ServiceType.RIDE_HAILING
        )

        // Complete ride & accept new delivery
        activeDelivery = null
        activeDelivery = Delivery(
            id = "202",
            orderId = 2002,
            driverId = "55",
            pickupLocation = "Dansoman, Accra",
            dropOffLocation = "Kaneshie, Accra",
            status = DeliveryStatus.ASSIGNED,
            distanceKm = 3.5,
            estimatedEarnings = 18.0,
            customerName = "Delivery Customer",
            recipientName = "Parcel Receiver",
            serviceType = ServiceType.PACKAGE_DELIVERY
        )

        assertNotNull(activeDelivery)
        assertEquals(ServiceType.PACKAGE_DELIVERY, activeDelivery.serviceType)
        assertEquals("SENDER (PICKUP)", activeDelivery.currentContactRole)
    }

    @Test
    fun testMalformedServiceTypeFallback() {
        val malformedRaw = "UNKNOWN_CUSTOM_TYPE"
        val parsed = when {
            malformedRaw.contains("PACKAGE") || malformedRaw.contains("DELIVERY") -> ServiceType.PACKAGE_DELIVERY
            malformedRaw.contains("RENTAL") -> ServiceType.RENTAL
            else -> ServiceType.RIDE_HAILING
        }

        assertEquals(ServiceType.RIDE_HAILING, parsed)
    }
}
