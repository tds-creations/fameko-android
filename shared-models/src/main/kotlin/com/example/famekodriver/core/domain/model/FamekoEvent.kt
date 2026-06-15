package com.example.famekodriver.core.domain.model

sealed class FamekoEvent {
    data class NewDeliveryRequest(val delivery: Delivery) : FamekoEvent()
    data class DeliveryStatusChanged(val deliveryId: String, val status: DeliveryStatus) : FamekoEvent()
    data class NewMessage(val message: Message) : FamekoEvent()
    data class IncomingCall(val callId: String, val callerName: String, val customerId: Int? = null, val driverId: Int? = null) : FamekoEvent()
    data class CallAccepted(val callId: String) : FamekoEvent()
    data class CallRejected(val callId: String, val reason: String?) : FamekoEvent()
    data class CallEnded(val callId: String) : FamekoEvent()
    data class OrderCancelled(val orderId: Int) : FamekoEvent()
    data class OrderAccepted(val orderId: Int) : FamekoEvent()
    data class OrderStatusUpdate(val orderId: Int) : FamekoEvent()
    data class DriverLocationUpdate(val driverId: String, val lat: Double, val lng: Double, val bearing: Float) : FamekoEvent()
    data class NearbyDriversUpdate(val drivers: List<DriverLocation>) : FamekoEvent()
    data class DriverStatsUpdate(val stats: DriverStats) : FamekoEvent()
    data class AudioDataReceived(val data: ByteArray) : FamekoEvent()
    data class NotificationReceived(val id: Int, val title: String, val message: String, val type: String, val createdAt: String) : FamekoEvent()
    data class RentalDestinationUpdated(val rentalId: Int, val location: String) : FamekoEvent()
    data object Ping : FamekoEvent()
}

data class WebSocketMessage(
    val type: String,
    val payload: String
)
