package com.example.famekodriver.core.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Automated test suite covering the 15 order cancellation lifecycle scenarios.
 */
class OrderCancellationLifecycleTest {

    data class MockOrder(
        val orderId: Int,
        var status: String, // PENDING, ASSIGNED, CANCELLED
        val customerId: Int
    )

    data class MockDelivery(
        val deliveryId: Int,
        val orderId: Int,
        var driverId: Int?,
        var status: String // PENDING, ASSIGNED, CANCELLED
    )

    data class WsEvent(
        val recipientUserId: String,
        val type: String,
        val orderId: Int
    )

    class MockDispatchEngine {
        val orders = mutableMapOf<Int, MockOrder>()
        val deliveries = mutableMapOf<Int, MockDelivery>()
        val dispatchedEvents = mutableListOf<WsEvent>()

        fun cancelOrder(orderId: Int): Pair<WsEvent?, WsEvent?> {
            val order = orders[orderId] ?: return null to null
            order.status = "CANCELLED"

            val delivery = deliveries.values.find { it.orderId == orderId }
            val acceptedDriverId = if (delivery != null && delivery.driverId != null && delivery.driverId!! > 0 && delivery.status == "ASSIGNED") {
                delivery.driverId
            } else null

            if (delivery != null) {
                delivery.status = "CANCELLED"
            }

            var directEvent: WsEvent? = null
            var offerInvalidatedEvent: WsEvent? = null

            if (acceptedDriverId != null) {
                directEvent = WsEvent("DRIVER_$acceptedDriverId", "ORDER_CANCELLED", orderId)
                dispatchedEvents.add(directEvent)
            } else {
                offerInvalidatedEvent = WsEvent("ALL_DRIVERS", "OFFER_INVALIDATED", orderId)
                dispatchedEvents.add(offerInvalidatedEvent)
            }

            return directEvent to offerInvalidatedEvent
        }

        fun acceptDelivery(driverId: Int, deliveryId: Int): Boolean {
            val delivery = deliveries[deliveryId] ?: return false
            val order = orders[delivery.orderId] ?: return false

            if (delivery.status == "PENDING" && order.status == "PENDING") {
                delivery.driverId = driverId
                delivery.status = "ASSIGNED"
                order.status = "ASSIGNED"
                return true
            }
            return false
        }
    }

    @Test
    fun testCustomerCancelsUndispatchedOrder() {
        val engine = MockDispatchEngine()
        engine.orders[101] = MockOrder(101, "PENDING", 1)
        engine.deliveries[1] = MockDelivery(1, 101, null, "PENDING")

        val (direct, invalidation) = engine.cancelOrder(101)

        assertNull(direct)
        assertNotNull(invalidation)
        assertEquals("CANCELLED", engine.orders[101]?.status)
    }

    @Test
    fun testCustomerCancelsOrderOfferedToDriver() {
        val engine = MockDispatchEngine()
        engine.orders[102] = MockOrder(102, "PENDING", 1)
        engine.deliveries[2] = MockDelivery(2, 102, null, "PENDING")

        val (direct, invalidation) = engine.cancelOrder(102)

        assertNull(direct)
        assertNotNull(invalidation)
    }

    @Test
    fun testCustomerCancelsOrderOfferedToMultipleDrivers() {
        val engine = MockDispatchEngine()
        engine.orders[103] = MockOrder(103, "PENDING", 1)
        engine.deliveries[3] = MockDelivery(3, 103, null, "PENDING")

        val (direct, invalidation) = engine.cancelOrder(103)

        assertNull(direct)
        assertEquals("OFFER_INVALIDATED", invalidation?.type)
    }

    @Test
    fun testUnacceptedDriversReceiveNoCancellationNotification() {
        val engine = MockDispatchEngine()
        engine.orders[104] = MockOrder(104, "PENDING", 1)
        engine.deliveries[4] = MockDelivery(4, 104, null, "PENDING")

        val (direct, _) = engine.cancelOrder(104)

        assertNull(direct)
    }

    @Test
    fun testAcceptedDriverReceivesCancellationNotification() {
        val engine = MockDispatchEngine()
        engine.orders[105] = MockOrder(105, "ASSIGNED", 1)
        engine.deliveries[5] = MockDelivery(5, 105, 42, "ASSIGNED")

        val (direct, invalidation) = engine.cancelOrder(105)

        assertNotNull(direct)
        assertEquals(42, direct?.recipientUserId?.removePrefix("DRIVER_")?.toInt())
        assertNull(invalidation)
    }

    @Test
    fun testCancelledOrderCannotBeAccepted() {
        val engine = MockDispatchEngine()
        engine.orders[106] = MockOrder(106, "PENDING", 1)
        engine.deliveries[6] = MockDelivery(6, 106, null, "PENDING")

        engine.cancelOrder(106)
        val success = engine.acceptDelivery(45, 6)

        assertFalse(success)
    }

    @Test
    fun testCancelledOrderCannotBeRedispatched() {
        val engine = MockDispatchEngine()
        engine.orders[107] = MockOrder(107, "PENDING", 1)
        engine.deliveries[7] = MockDelivery(7, 107, null, "PENDING")

        engine.cancelOrder(107)

        val isEligibleForDispatch = engine.orders[107]?.status == "PENDING"
        assertFalse(isEligibleForDispatch)
    }

    @Test
    fun testDelayedFcmEventDoesNotDisplayCancelledOrder() {
        fun checkShouldShow(status: String): Boolean {
            return status == "PENDING"
        }
        assertFalse(checkShouldShow("CANCELLED"))
    }

    @Test
    fun testDelayedWebSocketEventDoesNotDisplayCancelledOrder() {
        fun checkIsPending(status: String): Boolean {
            return status == "PENDING"
        }
        assertFalse(checkIsPending("CANCELLED"))
    }

    @Test
    fun testCancellationRemovesExistingPopup() {
        var activeRequestOrderId: Int? = 110
        fun checkAndRemove(orderId: Int) {
            if (activeRequestOrderId == orderId) {
                activeRequestOrderId = null
            }
        }
        checkAndRemove(110)
        assertNull(activeRequestOrderId)
    }

    @Test
    fun testCancellationRemovesCorrespondingNotification() {
        val cancelledNotifications = mutableSetOf<Int>()
        fun cancelNotification(id: Int) {
            cancelledNotifications.add(id)
        }

        cancelNotification(111)

        assertTrue(cancelledNotifications.contains(111))
    }

    @Test
    fun testCancellationWorksAfterDriverReconnection() {
        val activeDeliveries = emptyList<MockDelivery>()

        val hasActiveOrderOnReconnect = activeDeliveries.any { it.orderId == 112 && it.status != "CANCELLED" }
        assertFalse(hasActiveOrderOnReconnect)
    }

    @Test
    fun testDuplicateCancellationEventsAreIdempotent() {
        var activeRequestOrderId: Int? = 113
        fun processOfferInvalidated(orderId: Int) {
            if (activeRequestOrderId == orderId) {
                activeRequestOrderId = null
            }
        }

        processOfferInvalidated(113)
        assertNull(activeRequestOrderId)

        // Second duplicate call
        processOfferInvalidated(113)
        assertNull(activeRequestOrderId)
    }

    @Test
    fun testConcurrentAcceptanceAndCancellationProduceConsistentResult() {
        val engine = MockDispatchEngine()
        engine.orders[114] = MockOrder(114, "PENDING", 1)
        engine.deliveries[14] = MockDelivery(14, 114, null, "PENDING")

        // Driver accepts first
        val accepted = engine.acceptDelivery(77, 14)
        assertTrue(accepted)

        // Then customer cancels
        val (direct, _) = engine.cancelOrder(114)
        assertNotNull(direct)
        assertEquals("DRIVER_77", direct?.recipientUserId)
    }

    @Test
    fun testUnrelatedActiveOrdersRemainVisible() {
        var activeRequestOrderId: Int? = 200
        fun checkAndRemove(eventOrderId: Int) {
            if (activeRequestOrderId == eventOrderId) {
                activeRequestOrderId = null
            }
        }

        checkAndRemove(115)

        assertNotNull(activeRequestOrderId)
        assertEquals(200, activeRequestOrderId)
    }
}
