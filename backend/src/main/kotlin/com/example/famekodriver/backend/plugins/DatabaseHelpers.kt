package com.example.famekodriver.backend.plugins

import com.example.famekodriver.backend.db.DatabaseInitializer

fun getCustomerIdForOrder(orderId: Int): Int? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT customer_id FROM orders WHERE id = ?").apply { setInt(1, orderId) }.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }
    return null
}

fun getDriverIdForOrder(orderId: Int): Int? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT driver_id FROM deliveries WHERE order_id = ?").apply { setInt(1, orderId) }.executeQuery()
        if (rs.next()) {
            val id = rs.getInt(1)
            return if (rs.wasNull()) null else id
        }
    }
    return null
}

fun getCustomerName(id: Int): String? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT name FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return rs.getString(1)
    }
    return null
}

fun getCustomerPhone(id: Int): String? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT phone FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return rs.getString(1)
    }
    return null
}

fun getUserFcmToken(id: Int, type: String): String? {
    val table = if (type == "customer") "customers" else "drivers"
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val rs = conn.prepareStatement("SELECT fcm_token FROM $table WHERE id = ?").apply { setInt(1, id) }.executeQuery()
        if (rs.next()) return rs.getString(1)
    }
    return null
}

fun getActiveCustomerIdForDriver(driverId: Int): Int? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = "SELECT o.customer_id FROM deliveries d JOIN orders o ON d.order_id = o.id WHERE d.driver_id = ? AND d.status NOT IN ('DELIVERED', 'CANCELLED') LIMIT 1"
        val rs = conn.prepareStatement(sql).apply { setInt(1, driverId) }.executeQuery()
        if (rs.next()) return rs.getInt(1)
    }
    return null
}

fun updateRentalStatusInDb(id: Int, status: String) {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        conn.autoCommit = false
        try {
            println("DEBUG: Updating Rental $id to status $status")
            // 1. Update Rental Status
            conn.prepareStatement("UPDATE rentals SET status = ? WHERE id = ?").use { stmt ->
                stmt.setString(1, status)
                stmt.setInt(2, id)
                stmt.executeUpdate()
            }

            // If status is BOOKED, also ensure payment_status is PAID
            if (status == "BOOKED") {
                conn.prepareStatement("UPDATE rentals SET payment_status = 'PAID' WHERE id = ?").use { stmt ->
                    stmt.setInt(1, id)
                    stmt.executeUpdate()
                }
            }

            // 2. Sync Vehicle Status
            var vId: Int? = null
            conn.prepareStatement("SELECT vehicle_id FROM rentals WHERE id = ?").use { stmt ->
                stmt.setInt(1, id)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        vId = rs.getInt("vehicle_id")
                    }
                }
            }

            if (vId != null) {
                println("DEBUG: Found Vehicle ID $vId for Rental $id")
                
                val normalizedStatus = status.uppercase()
                val isAvail = when (normalizedStatus) {
                    "BOOKED", "ACTIVE", "ASSIGNED", "IN_PROGRESS", "PAID" -> false
                    "COMPLETED", "CANCELLED", "REJECTED" -> true
                    else -> null
                }
                
                val vStatus = when (normalizedStatus) {
                    "BOOKED", "PAID" -> "BOOKED"
                    "ACTIVE", "ASSIGNED", "IN_PROGRESS" -> "OCCUPIED"
                    "COMPLETED", "CANCELLED", "REJECTED" -> "AVAILABLE"
                    else -> null
                }

                if (vStatus != null && isAvail != null) {
                    println("DEBUG: Updating Vehicle $vId to status $vStatus and is_available $isAvail")
                    conn.prepareStatement("UPDATE rental_vehicles SET status = ?, is_available = ? WHERE id = ?").use { stmt ->
                        stmt.setString(1, vStatus)
                        stmt.setBoolean(2, isAvail)
                        stmt.setInt(3, vId)
                        stmt.executeUpdate()
                    }
                }
            } else {
                println("DEBUG: WARNING: No vehicle found for rental $id")
            }
            conn.commit()
            println("DEBUG: Rental and Vehicle status update committed for rental $id")
        } catch (e: Exception) {
            println("DEBUG: ERROR updating rental status: ${e.message}")
            e.printStackTrace()
            conn.rollback()
            throw e
        }
    }
}

fun getRentalParticipants(rentalId: Int): RentalParticipants? {
    DatabaseInitializer.getDataSource().connection.use { conn ->
        val sql = """
            SELECT r.customer_id, v.owner_id, v.name as vehicle_name 
            FROM rentals r 
            JOIN rental_vehicles v ON r.vehicle_id = v.id 
            WHERE r.id = ?
        """.trimIndent()
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, rentalId)
        val rs = stmt.executeQuery()
        if (rs.next()) {
            return RentalParticipants(
                customerId = rs.getInt("customer_id"),
                ownerId = rs.getInt("owner_id"),
                vehicleName = rs.getString("vehicle_name")
            )
        }
    }
    return null
}

data class RentalParticipants(val customerId: Int, val ownerId: Int, val vehicleName: String)
