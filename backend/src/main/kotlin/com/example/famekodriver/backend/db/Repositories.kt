package com.example.famekodriver.backend.db

import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.backend.services.RedisManager
import com.example.famekodriver.backend.services.H3Helper
import com.example.famekodriver.backend.plugins.AdminPrincipal
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.mindrot.jbcrypt.BCrypt
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object DatabaseRepository {

    fun normalizePhone(phone: String?): String {
        if (phone == null) return ""
        var cleaned = phone.trim().replace(" ", "").replace("-", "")
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }
        if (cleaned.startsWith("+233")) {
            return cleaned
        }
        if (cleaned.startsWith("233")) {
            return "+$cleaned"
        }
        if (cleaned.startsWith("+")) {
            return cleaned
        }
        return "+233$cleaned"
    }

    fun getPricingConfig(): PricingConfig {
        val cached = RedisManager.get("config:pricing")
        if (cached != null) {
            try {
                return com.google.gson.Gson().fromJson(cached, PricingConfig::class.java)
            } catch (e: Exception) { }
        }

        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM pricing_config LIMIT 1")
            if (rs.next()) {
                val config = PricingConfig(
                    baseFare = rs.getDouble("base_fare"), perKmRate = rs.getDouble("per_km_rate"), perMinuteRate = rs.getDouble("per_minute_rate"),
                    minFare = rs.getDouble("min_fare"), milestoneInterval = rs.getInt("milestone_interval"), milestoneDiscountPercent = rs.getInt("milestone_discount_percent"),
                    peakMultiplier = rs.getDouble("peak_multiplier"), driverCommissionPercent = rs.getDouble("driver_commission_percent"),
                    dailyServiceFee = rs.getDouble("daily_service_fee"), rentalDailyRate = rs.getDouble("rental_daily_rate"), 
                    rentalCommissionPercent = rs.getDouble("rental_commission_percent"),
                    rentalOwnerCommissionPercent = rs.getDouble("rental_owner_commission_percent"),
                    rentalCustomerServiceFeePercent = rs.getDouble("rental_customer_service_fee_percent")
                )
                RedisManager.set("config:pricing", com.google.gson.Gson().toJson(config), 3600)
                return config
            }
        }
        return PricingConfig(0.0, 0.0, 0.0, 0.0, 0, 0, 1.0, 0.0, 0.0, 0.0, 0.0, 7.5, 7.5)
    }

    fun updatePricingConfig(
        baseFare: Double?, perKmRate: Double?, perMinuteRate: Double?, minFare: Double?, 
        milestoneInterval: Int?, milestoneDiscount: Int?, driverCommission: Double?, 
        peakMultiplier: Double?, dailyServiceFee: Double?,
        rentalOwnerComm: Double? = null, rentalGuestFee: Double? = null
    ) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE pricing_config SET base_fare=COALESCE(?, base_fare), per_km_rate=COALESCE(?, per_km_rate), per_minute_rate=COALESCE(?, per_minute_rate), min_fare=COALESCE(?, min_fare), milestone_interval=COALESCE(?, milestone_interval), milestone_discount_percent=COALESCE(?, milestone_discount_percent), driver_commission_percent=COALESCE(?, driver_commission_percent), peak_multiplier=COALESCE(?, peak_multiplier), daily_service_fee=COALESCE(?, daily_service_fee), rental_owner_commission_percent=COALESCE(?, rental_owner_commission_percent), rental_customer_service_fee_percent=COALESCE(?, rental_customer_service_fee_percent)"
            conn.prepareStatement(sql).apply {
                setObject(1, baseFare); setObject(2, perKmRate); setObject(3, perMinuteRate); setObject(4, minFare)
                setObject(5, milestoneInterval); setObject(6, milestoneDiscount); setObject(7, driverCommission); setObject(8, peakMultiplier); setObject(9, dailyServiceFee)
                setObject(10, rentalOwnerComm); setObject(11, rentalGuestFee)
                executeUpdate()
            }
        }
        RedisManager.delete("config:pricing")
    }

    fun updateRentalRate(type: String, rate: Double) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("UPDATE rental_rates SET daily_rate = ?, updated_at = CURRENT_TIMESTAMP WHERE vehicle_type = ?").apply {
                setDouble(1, rate)
                setString(2, type)
                executeUpdate()
            }
        }
    }

    fun getRideCategories(): List<RideCategory> {
        val list = mutableListOf<RideCategory>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM ride_categories WHERE is_active = true ORDER BY display_order ASC")
            while (rs.next()) {
                list.add(RideCategory(
                    id = rs.getInt("id"),
                    serviceId = rs.getString("service_id"),
                    name = rs.getString("name"),
                    description = rs.getString("description"),
                    icon = rs.getString("icon"),
                    serviceType = rs.getString("service_type"),
                    baseFare = rs.getDouble("base_fare"),
                    perKmRate = rs.getDouble("per_km_rate"),
                    perMinuteRate = rs.getDouble("per_minute_rate"),
                    minFare = rs.getDouble("min_fare"),
                    driverCommissionPercent = rs.getDouble("driver_commission_percent"),
                    isActive = rs.getBoolean("is_active"),
                    displayOrder = rs.getInt("display_order")
                ))
            }
        }
        return list
    }

    fun updateRideCategoryPricing(
        serviceId: String, 
        baseFare: Double, 
        perKmRate: Double, 
        perMinuteRate: Double, 
        minFare: Double, 
        commission: Double
    ) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                UPDATE ride_categories SET 
                base_fare = ?, per_km_rate = ?, per_minute_rate = ?, 
                min_fare = ?, driver_commission_percent = ?, updated_at = CURRENT_TIMESTAMP 
                WHERE service_id = ?
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setDouble(1, baseFare)
                setDouble(2, perKmRate)
                setDouble(3, perMinuteRate)
                setDouble(4, minFare)
                setDouble(5, commission)
                setString(6, serviceId)
                executeUpdate()
            }
        }
    }

    fun getAllTables(): List<String> {
        val tables = mutableListOf<String>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name"
            )
            while (rs.next()) {
                tables.add(rs.getString(1))
            }
        }
        return tables
    }

    fun getTableData(tableName: String, limit: Int = 100): Pair<List<String>, List<List<Any?>>> {
        val columns = mutableListOf<String>()
        val rows = mutableListOf<List<Any?>>()
        
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT * FROM \"$tableName\" LIMIT ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, limit)
            val rs = stmt.executeQuery()
            val meta = rs.metaData
            for (i in 1..meta.columnCount) {
                columns.add(meta.getColumnName(i))
            }
            while (rs.next()) {
                val row = mutableListOf<Any?>()
                for (i in 1..meta.columnCount) {
                    row.add(rs.getObject(i))
                }
                rows.add(row)
            }
        }
        return Pair(columns, rows)
    }

    fun getDriverStats(id: Int): DriverStats? {
        try {
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = """
                    SELECT 
                        COALESCE(ds.is_online, d.is_online) as is_online,
                        COALESCE(ds.total_deliveries, 0) as total_deliveries,
                        COALESCE(ds.total_earnings, 0.0) as total_earnings,
                        COALESCE(ds.earnings_today, 0.0) as earnings_today,
                        COALESCE(ds.completed_today, 0) as completed_today,
                        ds.updated_at,
                        d.rating as avg_rating, 
                        d.rating_count 
                    FROM drivers d
                    LEFT JOIN driver_stats ds ON d.id = ds.driver_id 
                    WHERE d.id = ?
                """.trimIndent()
                val stmt = conn.prepareStatement(sql)
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val updatedAt = rs.getTimestamp("updated_at")
                    val isToday = updatedAt != null && updatedAt.toLocalDateTime().toLocalDate() == LocalDate.now()
                    
                    return DriverStats(
                        isOnline = rs.getBoolean("is_online"),
                        activeDeliveries = 0,
                        completedToday = if (isToday) rs.getInt("completed_today") else 0,
                        earningsToday = if (isToday) rs.getDouble("earnings_today") else 0.0,
                        rating = rs.getDouble("avg_rating"),
                        ratingCount = rs.getInt("rating_count"),
                        totalDeliveries = rs.getInt("total_deliveries"),
                        completionRate = 100, 
                        totalEarnings = rs.getDouble("total_earnings")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return DriverStats(rating = 5.0)
    }

    fun isDailyFeePaid(driverId: Int): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT 1 FROM drivers WHERE id = ? AND (daily_fee_paid_at >= CURRENT_DATE OR daily_fee_expires_at > CURRENT_TIMESTAMP)").apply { setInt(1, driverId) }.executeQuery()
            return rs.next()
        }
    }

    fun updateDriverOnlineStatus(id: String, online: Boolean): String? {
        if (online) {
            val paid = isDailyFeePaid(id.toInt())
            if (!paid) {
                return "Daily service fee not paid. Please pay from your wallet to go online."
            }
        } else {
            RedisManager.removeDriverLocation(id)
        }
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("INSERT INTO driver_stats (driver_id, is_online) VALUES (?, ?) ON CONFLICT (driver_id) DO UPDATE SET is_online = EXCLUDED.is_online, updated_at = CURRENT_TIMESTAMP").apply { setInt(1, id.toInt()); setBoolean(2, online); executeUpdate() }
            conn.prepareStatement("UPDATE drivers SET is_online = ? WHERE id = ?").apply { setBoolean(1, online); setInt(2, id.toInt()); executeUpdate() }
        }
        return null
    }

    fun getDriverStatus(id: Int): DriverStatusResponse? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT status, profile_picture, license_image, insurance_cert, roadworthy_cert, id_front_image, emergency_contact_1, emergency_contact_2, daily_fee_paid_at, daily_fee_expires_at, vehicle_category, vehicle_type, vehicle_number, vehicle_model, is_online FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) {
                val missing = mutableListOf<String>()
                
                fun isMissing(col: String): Boolean {
                    val value = rs.getString(col) ?: return true
                    val clean = value.trim()
                    if (clean.isEmpty() || clean.lowercase() == "null" || clean.lowercase() == "undefined") return true
                    if (clean.contains("placeholder", ignoreCase = true) || clean.contains("default", ignoreCase = true)) return true
                    if (clean.length < 5) return true 
                    return false
                }
                
                if (isMissing("profile_picture")) missing.add("profile_pic")
                if (isMissing("license_image")) missing.add("drivers_license")
                if (isMissing("insurance_cert")) missing.add("insurance_cert")
                if (isMissing("roadworthy_cert")) missing.add("roadworthy_cert")
                if (isMissing("id_front_image")) missing.add("ghana_card")
                
                val isPaid = isDailyFeePaid(id)
                val expiryTime = rs.getString("daily_fee_expires_at")
                val config = getPricingConfig()

                return DriverStatusResponse(
                    success = true, 
                    status = rs.getString("status") ?: "PENDING", 
                    missingDocs = missing,
                    emergencyContact1 = rs.getString("emergency_contact_1"),
                    emergencyContact2 = rs.getString("emergency_contact_2"),
                    profilePicture = rs.getString("profile_picture"),
                    isOnline = rs.getBoolean("is_online"),
                    isDailyFeePaid = isPaid,
                    dailyFeeAmount = config.dailyServiceFee,
                    dailyFeeExpiryTime = expiryTime,
                    vehicleCategory = rs.getString("vehicle_category") ?: "Economy",
                    vehicleType = rs.getString("vehicle_type"),
                    plateNumber = rs.getString("vehicle_number"),
                    vehicleModel = rs.getString("vehicle_model")
                )
            }

            // Try fleet_owners
            val rsOwner = conn.prepareStatement("SELECT status, profile_picture, id_front_image, id_back_image, business_certificate, emergency_contact_1, emergency_contact_2 FROM fleet_owners WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rsOwner.next()) {
                val missing = mutableListOf<String>()
                
                fun isMissing(col: String): Boolean {
                    val value = rsOwner.getString(col) ?: return true
                    val clean = value.trim()
                    if (clean.isEmpty() || clean.lowercase() == "null" || clean.lowercase() == "undefined") return true
                    if (clean.contains("placeholder", ignoreCase = true) || clean.contains("default", ignoreCase = true)) return true
                    if (clean.length < 5) return true 
                    return false
                }
                
                if (isMissing("profile_picture")) missing.add("profile_pic")
                if (isMissing("id_front_image")) missing.add("ghana_card")
                if (isMissing("id_back_image")) missing.add("ghana_card_back")
                if (isMissing("business_certificate")) missing.add("business_cert")
                
                return DriverStatusResponse(
                    success = true,
                    status = rsOwner.getString("status") ?: "PENDING",
                    missingDocs = missing,
                    emergencyContact1 = rsOwner.getString("emergency_contact_1"),
                    emergencyContact2 = rsOwner.getString("emergency_contact_2"),
                    profilePicture = rsOwner.getString("profile_picture")
                )
            }
        }
        return null
    }

    fun getAllDrivers(region: String?): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = if (region == null) "SELECT * FROM drivers ORDER BY region ASC, status ASC, id DESC" else "SELECT * FROM drivers WHERE region = ? ORDER BY status ASC, id DESC"
            val stmt = conn.prepareStatement(sql); if (region != null) stmt.setString(1, region)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"), "name" to rs.getString("full_name"), "email" to rs.getString("email"),
                    "status" to rs.getString("status"), "phone" to rs.getString("phone"), "region" to (rs.getString("region") ?: "Unknown"),
                    "vehicle" to (rs.getString("vehicle_model") ?: rs.getString("vehicle_type") ?: "Car"), "is_online" to rs.getBoolean("is_online")
                ))
            }
        }
        return list
    }

    fun getAllCustomers(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM customers ORDER BY id DESC")
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"), 
                    "name" to rs.getString("name"), 
                    "email" to rs.getString("email"), 
                    "phone" to rs.getString("phone"), 
                    "region" to (rs.getString("region") ?: "N/A"),
                    "profile_pic" to (rs.getString("profile_picture") ?: ""),
                    "is_active" to rs.getBoolean("is_active"),
                    "date_joined" to (rs.getString("date_joined") ?: "")
                ))
            }
        }
        return list
    }

    fun getPlatformStats(): Map<String, Any> {
        var rev = 0.0
        var debt = 0.0
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rsRides = conn.createStatement().executeQuery("SELECT SUM(total_amount) FROM orders WHERE status = 'DELIVERED'")
            if (rsRides.next()) rev += rsRides.getDouble(1)
            
            val rsRentals = conn.createStatement().executeQuery("SELECT SUM(total_price) FROM rentals WHERE payment_status = 'PAID'")
            if (rsRentals.next()) rev += rsRentals.getDouble(1)

            val rsFees = conn.createStatement().executeQuery("SELECT SUM(amount) FROM payments WHERE payment_type = 'DAILY_FEE'")
            if (rsFees.next()) rev += rsFees.getDouble(1)
            
            val rsDebt = conn.createStatement().executeQuery("SELECT SUM(amount) FROM wallet_topups WHERE status = 'PENDING'")
            if (rsDebt.next()) debt = rsDebt.getDouble(1)
        }
        return mapOf("totalRevenue" to rev.toInt(), "totalDebt" to debt.toInt())
    }

    fun getActiveDeliveries(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT d.*, o.total_amount, dr.full_name as driver_name 
                FROM deliveries d 
                JOIN orders o ON d.order_id = o.id 
                LEFT JOIN drivers dr ON d.driver_id = dr.id
                WHERE d.status != 'DELIVERED' AND d.status != 'CANCELLED'
                ORDER BY d.id DESC
            """.trimIndent()
            val rs = conn.createStatement().executeQuery(sql)
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"), 
                    "pickup" to rs.getString("pickup_location"), 
                    "dropoff" to rs.getString("dropoff_location"), 
                    "status" to rs.getString("status"), 
                    "amount" to rs.getDouble("total_amount"),
                    "service_type" to (rs.getString("service_type") ?: "RIDE_HAILING"),
                    "driver_name" to (rs.getString("driver_name") ?: "Unassigned")
                ))
            }
        }
        return list
    }

    fun getAllFleetOwners(region: String?): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = if (region == null) "SELECT * FROM fleet_owners ORDER BY region ASC, status ASC, id DESC" else "SELECT * FROM fleet_owners WHERE region = ? ORDER BY status ASC, id DESC"
            val stmt = conn.prepareStatement(sql); if (region != null) stmt.setString(1, region)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("full_name"),
                    "email" to rs.getString("email"),
                    "phone" to rs.getString("phone"),
                    "company_name" to rs.getString("company_name"),
                    "registration_number" to (rs.getString("registration_number") ?: "N/A"),
                    "status" to rs.getString("status"),
                    "region" to (rs.getString("region") ?: "Unknown"),
                    "date_joined" to rs.getTimestamp("date_joined").toString()
                ))
            }
        }
        return list
    }

    fun getAllRentals(region: String?): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = if (region == null) {
                "SELECT r.*, c.name as customer_name, v.name as vehicle_name FROM rentals r JOIN customers c ON r.customer_id = c.id JOIN rental_vehicles v ON r.vehicle_id = v.id ORDER BY r.id DESC"
            } else {
                "SELECT r.*, c.name as customer_name, v.name as vehicle_name FROM rentals r JOIN customers c ON r.customer_id = c.id JOIN rental_vehicles v ON r.vehicle_id = v.id WHERE c.region = ? ORDER BY r.id DESC"
            }
            val stmt = conn.prepareStatement(sql)
            if (region != null) stmt.setString(1, region)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"), 
                    "customer_name" to rs.getString("customer_name"), 
                    "vehicle_name" to rs.getString("vehicle_name"), 
                    "status" to rs.getString("status"), 
                    "total_price" to rs.getDouble("total_price"),
                    "booking_code" to (rs.getString("booking_code") ?: ""),
                    "trip_notes" to (rs.getString("trip_notes") ?: ""),
                    "start_time" to (rs.getString("start_time") ?: ""),
                    "created_at" to (rs.getString("created_at") ?: "")
                ))
            }
        }
        return list
    }

    fun getDriverDetails(id: Int): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT * FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) return mapOf(
                "id" to rs.getInt("id"), "full_name" to rs.getString("full_name"), "email" to rs.getString("email"), "phone" to rs.getString("phone"),
                "status" to rs.getString("status"), "region" to rs.getString("region"), "license_number" to rs.getString("license_number"),
                "vehicle_type" to rs.getString("vehicle_type"), "vehicle_number" to rs.getString("vehicle_number"),
                "vehicle_category" to (rs.getString("vehicle_category") ?: "Economy"),
                "profile_picture" to rs.getString("profile_picture"), "license_image" to rs.getString("license_image"),
                "id_front_image" to rs.getString("id_front_image"), "id_back_image" to rs.getString("id_back_image"), 
                "vehicle_image" to rs.getString("vehicle_image"), "insurance_cert" to rs.getString("insurance_cert"),
                "roadworthy_cert" to rs.getString("roadworthy_cert")
            )
        }
        return null
    }

    fun updateDriverStatus(id: Int, status: String) {
        DatabaseInitializer.getDataSource().connection.use { it.prepareStatement("UPDATE drivers SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, id); executeUpdate() } }
    }

    fun approveDriver(id: Int, category: String) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("UPDATE drivers SET status = 'APPROVED', vehicle_category = ? WHERE id = ?").apply {
                setString(1, category)
                setInt(2, id)
                executeUpdate()
            }
        }
    }

    fun getFleetOwnerDetails(id: Int): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT * FROM fleet_owners WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) return mapOf(
                "id" to rs.getInt("id"), "full_name" to rs.getString("full_name"), "email" to rs.getString("email"), "phone" to rs.getString("phone"),
                "status" to rs.getString("status"), "region" to rs.getString("region"), "company_name" to rs.getString("company_name"),
                "registration_number" to rs.getString("registration_number"),
                "profile_picture" to rs.getString("profile_picture"), "id_front_image" to rs.getString("id_front_image"),
                "id_back_image" to rs.getString("id_back_image"), "business_certificate" to rs.getString("business_certificate")
            )
        }
        return null
    }

    fun updateFleetOwnerStatus(id: Int, status: String) {
        DatabaseInitializer.getDataSource().connection.use { it.prepareStatement("UPDATE fleet_owners SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, id); executeUpdate() } }
    }

    fun getPendingPayments(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT t.*, d.full_name, d.phone FROM wallet_topups t JOIN drivers d ON t.driver_id = d.id WHERE t.status = 'PENDING' ORDER BY t.created_at DESC")
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"), 
                    "driver_name" to rs.getString("full_name"), 
                    "driver_phone" to rs.getString("phone"),
                    "amount" to rs.getDouble("amount"), 
                    "payment_type" to (rs.getString("payment_type") ?: "TOPUP"),
                    "reference" to rs.getString("reference_code"),
                    "created_at" to rs.getString("created_at")
                ))
            }
        }
        return list
    }

    fun approvePayment(id: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT driver_id, amount, payment_type, reference_code FROM wallet_topups WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) {
                val dId = rs.getInt(1)
                val amt = rs.getDouble(2)
                val type = rs.getString(3) ?: "TOPUP"
                val ref = rs.getString(4)

                if (type == "DAILY_FEE") {
                    conn.prepareStatement("UPDATE drivers SET daily_fee_paid_at = CURRENT_DATE, daily_fee_expires_at = CURRENT_TIMESTAMP + interval '24 hours' WHERE id = ?").apply { setInt(1, dId); executeUpdate() }
                    recordPayment(dId, "DRIVER", amt, "DAILY_FEE", ref)
                } else {
                    conn.prepareStatement("UPDATE driver_stats SET total_earnings = total_earnings + ? WHERE driver_id = ?").apply { setDouble(1, amt); setInt(2, dId); executeUpdate() }
                    recordPayment(dId, "DRIVER", amt, "TOPUP", ref)
                }

                conn.prepareStatement("UPDATE wallet_topups SET status = 'APPROVED' WHERE id = ?").apply { setInt(1, id); executeUpdate() }
            }
        }
    }

    fun rejectPayment(id: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("UPDATE wallet_topups SET status = 'REJECTED' WHERE id = ?").apply {
                setInt(1, id)
                executeUpdate()
            }
        }
    }

    fun recordPayment(userId: Int, userType: String, amount: Double, type: String, reference: String) {
        try {
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val sql = "INSERT INTO payments (user_id, user_type, amount, payment_type, reference, status) VALUES (?, ?, ?, ?, ?, 'SUCCESS') ON CONFLICT (reference) DO NOTHING"
                conn.prepareStatement(sql).apply {
                    setInt(1, userId)
                    setString(2, userType)
                    setDouble(3, amount)
                    setString(4, type)
                    setString(5, reference)
                    executeUpdate()
                }
            }
        } catch (e: Exception) {
            println("Error recording payment: ${e.message}")
        }
    }

    fun approveDailyFee(driverId: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE drivers SET daily_fee_paid_at = CURRENT_DATE, daily_fee_expires_at = CURRENT_TIMESTAMP + interval '24 hours' WHERE id = ?"
            conn.prepareStatement(sql).apply {
                setInt(1, driverId)
                executeUpdate()
            }
        }
    }

    fun queueDailyFeeForApproval(driverId: Int, amount: Double, reference: String) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO wallet_topups (driver_id, amount, reference_code, payment_type, status) VALUES (?, ?, ?, 'DAILY_FEE', 'PENDING')"
            conn.prepareStatement(sql).apply {
                setInt(1, driverId)
                setDouble(2, amount)
                setString(3, reference)
                executeUpdate()
            }
        }
    }

    fun getRentalRates(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM rental_rates")
            while (rs.next()) list.add(mapOf("id" to rs.getInt("id"), "vehicle_type" to rs.getString("vehicle_type"), "daily_rate" to rs.getDouble("daily_rate")))
        }
        return list
    }

    fun getNearbyDrivers(lat: Double, lng: Double, radius: Double, vehicleType: String? = null): List<DriverLocation> {
        val redisIds = try {
            RedisManager.getNearbyDrivers(lat, lng, radius)
        } catch (e: Exception) {
            println("Redis lookup failed: ${e.message}")
            emptyList<String>()
        }

        val list = mutableListOf<DriverLocation>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val vehicleFilter = if (vehicleType != null) {
                val vType = vehicleType.lowercase()
                when {
                    vType.contains("economy") -> "AND (d.vehicle_type ILIKE '%car%' OR d.vehicle_type ILIKE '%economy%' OR d.vehicle_type ILIKE '%saloon%' OR d.vehicle_type ILIKE '%taxi%') AND (d.vehicle_category ILIKE 'Economy' OR d.vehicle_category IS NULL)"
                    vType.contains("comfort") -> "AND (d.vehicle_type ILIKE '%car%' OR d.vehicle_type ILIKE '%comfort%' OR d.vehicle_type ILIKE '%saloon%' OR d.vehicle_type ILIKE '%taxi%') AND d.vehicle_category ILIKE 'Comfort'"
                    vType.contains("car") -> "AND (d.vehicle_type ILIKE '%car%' OR d.vehicle_type ILIKE '%taxi%' OR d.vehicle_type ILIKE '%saloon%')"
                    vType.contains("okada") || vType.contains("bike") -> "AND (d.vehicle_type ILIKE '%okada%' OR d.vehicle_type ILIKE '%bike%')"
                    vType.contains("pragya") -> "AND (d.vehicle_type ILIKE '%pragya%' OR d.vehicle_type ILIKE '%tricycle%')"
                    vType.contains("aboboyaa") -> "AND d.vehicle_type ILIKE '%aboboyaa%'"
                    vType.contains("truck") -> "AND d.vehicle_type ILIKE '%truck%'"
                    vType.contains("bicycle") -> "AND d.vehicle_type ILIKE '%bicycle%'"
                    else -> "AND (d.vehicle_type ILIKE ? OR d.vehicle_category ILIKE ?)"
                }
            } else ""

            // Use H3 as a pre-filter but with a radius-aware K-ring to avoid missing drivers
            val h3Index = H3Helper.getIndex(lat, lng)
            val k = (radius / 1.1).toInt().coerceAtLeast(1).coerceAtMost(10) // Approx km to K-ring
            val neighbors = H3Helper.getNeighborsInRadius(h3Index, k)
            
            // Combine H3 filter with Redis IDs for maximum reach
            val h3List = neighbors.joinToString(",") { "'$it'" }
            val redisIdList = if (redisIds.isNotEmpty()) redisIds.joinToString(",") else "0"
            val geoFilter = "AND (ds.h3_index IN ($h3List) OR d.id IN ($redisIdList))"

            val useDistanceLimit = radius < 500.0 && !(lat == 0.0 && lng == 0.0)

            val sql = if (useDistanceLimit) {
                """
                    SELECT ds.*, d.vehicle_type, d.vehicle_category,
                    (6371 * acos(least(1.0, cos(radians(?)) * cos(radians(ds.latitude)) * cos(radians(ds.longitude) - radians(?)) + sin(radians(?)) * sin(radians(ds.latitude))))) AS distance
                    FROM driver_stats ds
                    JOIN drivers d ON ds.driver_id = d.id
                    WHERE ds.is_online = true AND d.status = 'APPROVED'
                    AND ds.latitude IS NOT NULL AND ds.longitude IS NOT NULL
                    $geoFilter
                    $vehicleFilter
                    AND (6371 * acos(least(1.0, cos(radians(?)) * cos(radians(ds.latitude)) * cos(radians(ds.longitude) - radians(?)) + sin(radians(?)) * sin(radians(ds.latitude)))) ) <= ?
                    ORDER BY distance ASC, ds.completed_today ASC
                """.trimIndent()
            } else {
                """
                    SELECT ds.*, d.vehicle_type, d.vehicle_category, 0.0 as distance
                    FROM driver_stats ds
                    JOIN drivers d ON ds.driver_id = d.id
                    WHERE ds.is_online = true AND d.status = 'APPROVED'
                    $geoFilter
                    $vehicleFilter
                    ORDER BY ds.completed_today ASC, d.id DESC
                """.trimIndent()
            }
            
            fun isPredefined(vt: String): Boolean {
                val v = vt.lowercase()
                return v.contains("economy") || v.contains("comfort") || v.contains("car") ||
                       v.contains("okada") || v.contains("bike") || v.contains("pragya") ||
                       v.contains("aboboyaa") || v.contains("truck") || v.contains("bicycle")
            }

            val stmt = conn.prepareStatement(sql)
            if (useDistanceLimit) {
                stmt.setDouble(1, lat); stmt.setDouble(2, lng); stmt.setDouble(3, lat)
                var paramIdx = 4
                if (vehicleType != null && !isPredefined(vehicleType)) {
                    stmt.setString(paramIdx++, vehicleType)
                    stmt.setString(paramIdx++, vehicleType)
                }
                stmt.setDouble(paramIdx++, lat); stmt.setDouble(paramIdx++, lng); stmt.setDouble(paramIdx++, lat)
                stmt.setDouble(paramIdx++, radius)
                stmt.setDouble(paramIdx++, lat); stmt.setDouble(paramIdx++, lng); stmt.setDouble(paramIdx++, lat)
            } else {
                if (vehicleType != null && !isPredefined(vehicleType)) {
                    stmt.setString(1, vehicleType)
                    stmt.setString(2, vehicleType)
                }
            }
            
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val dist = rs.getDouble("distance")
                val pickupEta = (dist / 30.0) * 60.0 + 2.0
                
                list.add(DriverLocation(
                    id = rs.getInt("driver_id").toString(),
                    latitude = rs.getDouble("latitude"),
                    longitude = rs.getDouble("longitude"),
                    bearing = rs.getFloat("bearing"),
                    vehicleType = rs.getString("vehicle_type"),
                    vehicleCategory = rs.getString("vehicle_category"),
                    pickupEtaMin = pickupEta
                ))
            }
        }
        return list
    }

    fun getAvailableDeliveriesByRadius(lat: Double, lng: Double, radius: Double, vehicleType: String, vehicleCategory: String? = null): List<Delivery> {
        val list = mutableListOf<Delivery>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val vType = vehicleType.lowercase()
            val vehicleFilter = when {
                vType.contains("car") || vType.contains("economy") || vType.contains("comfort") -> "AND (service_type ILIKE 'car' OR service_type ILIKE 'Economy' OR service_type ILIKE 'Comfort')"
                vType.contains("okada") || vType.contains("bike") || vType.contains("motorcycle") || vType.contains("rider") || vType.contains("motorbike") || vType.contains("motor") -> "AND (service_type ILIKE 'okada' OR service_type ILIKE 'bike')"
                vType.contains("pragya") -> "AND service_type ILIKE 'pragya'"
                vType.contains("aboboyaa") -> "AND service_type ILIKE 'aboboyaa'"
                vType.contains("truck") -> "AND service_type ILIKE 'truck'"
                vType.contains("bicycle") -> "AND service_type ILIKE 'bicycle'"
                else -> "AND (service_type ILIKE ? OR service_type ILIKE 'car')"
            }

            val sql = """
                SELECT d.*, c.name as customer_name, c.phone as customer_phone, c.profile_picture as customer_profile_pic, o.total_amount as total_fare,
                (6371 * acos(cos(radians(?)) * cos(radians(d.pickup_lat)) * cos(radians(d.pickup_lng) - radians(?)) + sin(radians(?)) * sin(radians(d.pickup_lat)))) AS distance
                FROM deliveries d
                JOIN orders o ON d.order_id = o.id
                JOIN customers c ON o.customer_id = c.id
                WHERE d.status = 'PENDING'
                $vehicleFilter
                AND (6371 * acos(cos(radians(?)) * cos(radians(d.pickup_lat)) * cos(radians(d.pickup_lng) - radians(?)) + sin(radians(?)) * sin(radians(d.pickup_lat)))) <= ?
                ORDER BY distance ASC
            """.trimIndent()
            
            val stmt = conn.prepareStatement(sql)
            stmt.setDouble(1, lat); stmt.setDouble(2, lng); stmt.setDouble(3, lat)
            
            var paramIdx = 4
            if (vType != "car" && vType != "okada" && vType != "pragya" && vType != "aboboyaa" && vType != "truck" && vType != "bicycle") {
                stmt.setString(paramIdx++, vehicleType)
            }
            
            stmt.setDouble(paramIdx++, lat); stmt.setDouble(paramIdx++, lng); stmt.setDouble(paramIdx++, lat)
            stmt.setDouble(paramIdx++, radius)
            
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val dist = rs.getDouble("distance")
                val pickupEta = (dist / 25.0) * 60.0 + 1.0 
                
                list.add(Delivery(
                    id = rs.getString("id"),
                    orderId = rs.getInt("order_id"),
                    driverId = null,
                    pickupLocation = rs.getString("pickup_location"),
                    dropOffLocation = rs.getString("dropoff_location"),
                    pickupLat = rs.getDouble("pickup_lat"),
                    pickupLng = rs.getDouble("pickup_lng"),
                    dropOffLat = rs.getDouble("dropoff_lat"),
                    dropOffLng = rs.getDouble("dropoff_lng"),
                    status = DeliveryStatus.valueOf(rs.getString("status").uppercase()),
                    distanceKm = rs.getDouble("distance_km"),
                    estimatedEarnings = rs.getDouble("estimated_earnings"),
                    pickupEtaMin = pickupEta,
                    customerName = rs.getString("customer_name"),
                    customerPhone = rs.getString("customer_phone"),
                    customerProfilePic = rs.getString("customer_profile_pic"),
                    totalFare = rs.getDouble("total_fare")
                ))
            }
        }
        return list
    }

    fun getFinancialStats(month: String?, date: String?): Map<String, Any> {
        val stats = mutableMapOf<String, Any>(
            "totalRevenue" to 0.0,
            "dailyFees" to 0.0,
            "rentalIncome" to 0.0,
            "totalDebt" to 0.0,
            "pendingPaymentsValue" to 0.0,
            "recentTransactions" to emptyList<Map<String, Any>>(),
            "topDebtors" to emptyList<Map<String, Any>>()
        )

        try {
            DatabaseInitializer.getDataSource().connection.use { conn ->
                var filter = ""
                if (!date.isNullOrBlank()) {
                    filter = " AND DATE(created_at) = '$date'"
                } else if (!month.isNullOrBlank()) {
                    filter = " AND TO_CHAR(created_at, 'YYYY-MM') = '$month'"
                }

                val rsFees = conn.createStatement().executeQuery("SELECT SUM(amount) FROM payments WHERE payment_type = 'DAILY_FEE' $filter")
                if (rsFees.next()) stats["dailyFees"] = rsFees.getDouble(1)

                val rsRentals = conn.createStatement().executeQuery("SELECT SUM(customer_service_fee) FROM rentals WHERE payment_status = 'PAID' $filter")
                if (rsRentals.next()) stats["rentalIncome"] = rsRentals.getDouble(1)

                val rsRides = conn.createStatement().executeQuery("SELECT SUM(total_amount) FROM orders WHERE status = 'DELIVERED' $filter")
                if (rsRides.next()) stats["totalRevenue"] = rsRides.getDouble(1)

                val rsDebt = conn.createStatement().executeQuery("SELECT SUM(amount) FROM wallet_topups WHERE status = 'PENDING'")
                if (rsDebt.next()) stats["pendingPaymentsValue"] = rsDebt.getDouble(1)
                stats["totalDebt"] = stats["pendingPaymentsValue"] as Double

                val transactions = mutableListOf<Map<String, Any>>()
                val rsTrans = conn.createStatement().executeQuery("SELECT p.*, d.full_name as driver_name, c.name as customer_name FROM payments p LEFT JOIN drivers d ON (p.user_id = d.id AND p.user_type = 'DRIVER') LEFT JOIN customers c ON (p.user_id = c.id AND p.user_type = 'CUSTOMER') ORDER BY p.created_at DESC LIMIT 15")
                while (rsTrans.next()) {
                    transactions.add(mapOf(
                        "driver" to (rsTrans.getString("driver_name") ?: rsTrans.getString("customer_name") ?: "System"),
                        "type" to "CREDIT",
                        "amount" to rsTrans.getDouble("amount"),
                        "desc" to (rsTrans.getString("payment_type") ?: "Payment"),
                        "date" to rsTrans.getString("created_at")
                    ))
                }
                stats["recentTransactions"] = transactions
                stats["topDebtors"] = emptyList<Map<String, Any>>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return stats
    }

    fun getAllAdmins(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM admins ORDER BY id DESC")
            while (rs.next()) {
                list.add(mapOf("id" to rs.getInt("id"), "username" to rs.getString("username"), "email" to rs.getString("email"), "role" to rs.getString("role"), "region" to (rs.getString("region") ?: "Nationwide"), "is_active" to rs.getBoolean("is_active")))
            }
        }
        return list
    }

    fun getLiveLocations(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT ds.*, d.full_name, d.vehicle_type FROM driver_stats ds JOIN drivers d ON ds.driver_id = d.id WHERE ds.is_online = true"
            val rs = conn.createStatement().executeQuery(sql)
            while (rs.next()) {
                list.add(mapOf("id" to rs.getInt("driver_id"), "name" to rs.getString("full_name"), "lat" to rs.getDouble("latitude"), "lng" to rs.getDouble("longitude"), "vehicle" to rs.getString("vehicle_type")))
            }
        }
        return list
    }

    fun loginCustomer(phone: String): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT id, name FROM customers WHERE TRIM(phone) = ? OR TRIM(phone) = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, phone.trim())
            val altPhone = if (phone.startsWith("+")) phone.substring(1) else "+$phone"
            stmt.setString(2, altPhone)
            val rs = stmt.executeQuery()
            if (rs.next()) return mapOf("id" to rs.getInt("id"), "name" to rs.getString("name"))
        }
        return null
    }

    fun getCustomerProfile(id: Int): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT id, name, email, phone, region, default_address, profile_picture FROM customers WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return mapOf(
                    "success" to true,
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("name"),
                    "email" to rs.getString("email"),
                    "phone" to rs.getString("phone"),
                    "region" to rs.getString("region"),
                    "address" to (rs.getString("default_address") ?: ""),
                    "profile_picture" to (rs.getString("profile_picture") ?: "")
                )
            }
        }
        return null
    }

    fun updateCustomerProfile(id: Int, name: String, email: String, phone: String, address: String, region: String, profilePicture: String?): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE customers SET name = ?, email = ?, phone = ?, default_address = ?, region = ?, profile_picture = COALESCE(?, profile_picture) WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, name)
            stmt.setString(2, email)
            stmt.setString(3, phone)
            stmt.setString(4, address)
            stmt.setString(5, region)
            stmt.setString(6, profilePicture)
            stmt.setInt(7, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun getSavedPlaces(customerId: Int): List<SavedPlace> {
        val list = mutableListOf<SavedPlace>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT * FROM saved_places WHERE customer_id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, customerId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(SavedPlace(
                    id = rs.getString("id"),
                    customerId = rs.getInt("customer_id").toString(),
                    label = rs.getString("label"),
                    address = rs.getString("address"),
                    latitude = rs.getDouble("latitude"),
                    longitude = rs.getDouble("longitude")
                ))
            }
        }
        return list
    }

    fun getActiveRental(customerId: Int): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT r.*, c.name as customer_name, c.profile_picture as customer_profile_pic,
                       dr.full_name as driver_name, dr.phone as driver_phone, dr.profile_picture as driver_profile_pic,
                       dr.vehicle_number as driver_plate, dr.vehicle_model as driver_model
                FROM rentals r 
                JOIN customers c ON r.customer_id = c.id
                LEFT JOIN drivers dr ON r.driver_id = dr.id
                WHERE r.customer_id = ? AND r.status IN ('PENDING', 'ASSIGNED', 'ACTIVE', 'IN_PROGRESS') 
                ORDER BY r.id DESC LIMIT 1
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, customerId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return mapOf(
                    "id" to rs.getInt("id"),
                    "customer_name" to rs.getString("customer_name"),
                    "customer_profile_pic" to (rs.getString("customer_profile_pic") ?: ""),
                    "driver_name" to (rs.getString("driver_name") ?: ""),
                    "driver_phone" to (rs.getString("driver_phone") ?: ""),
                    "driver_profile_pic" to (rs.getString("driver_profile_pic") ?: ""),
                    "driver_plate" to (rs.getString("driver_plate") ?: ""),
                    "driver_model" to (rs.getString("driver_model") ?: ""),
                    "pickup_location" to rs.getString("pickup_location"),
                    "destination_location" to (rs.getString("destination_location") ?: ""),
                    "pickup_lat" to rs.getDouble("pickup_lat"),
                    "pickup_lng" to rs.getDouble("pickup_lng"),
                    "destination_lat" to rs.getDouble("destination_lat"),
                    "destination_lng" to rs.getDouble("destination_lng"),
                    "stops" to (rs.getString("stops") ?: ""),
                    "booking_code" to rs.getString("booking_code"),
                    "status" to rs.getString("status"),
                    "is_unlocked" to rs.getBoolean("is_unlocked"),
                    "is_self_drive" to rs.getBoolean("is_self_drive"),
                    "vehicle_id" to rs.getInt("vehicle_id")
                )
            }
        }
        return null
    }

    fun getRentalHistory(customerId: Int): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT r.*, v.name as vehicle_name,
                       dr.full_name as driver_name, dr.profile_picture as driver_profile_pic,
                       dr.vehicle_number as driver_plate, dr.vehicle_model as driver_model
                FROM rentals r 
                JOIN rental_vehicles v ON r.vehicle_id = v.id 
                LEFT JOIN drivers dr ON r.driver_id = dr.id
                WHERE r.customer_id = ? ORDER BY r.id DESC
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, customerId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "vehicle_name" to rs.getString("vehicle_name"),
                    "status" to rs.getString("status"),
                    "total_price" to rs.getDouble("total_price"),
                    "booking_code" to (rs.getString("booking_code") ?: ""),
                    "pickup_location" to rs.getString("pickup_location"),
                    "destination_location" to (rs.getString("destination_location") ?: ""),
                    "pickup_lat" to rs.getDouble("pickup_lat"),
                    "pickup_lng" to rs.getDouble("pickup_lng"),
                    "destination_lat" to rs.getDouble("destination_lat"),
                    "destination_lng" to rs.getDouble("destination_lng"),
                    "stops" to (rs.getString("stops") ?: ""),
                    "trip_notes" to (rs.getString("trip_notes") ?: ""),
                    "start_time" to (rs.getString("start_time") ?: ""),
                    "created_at" to (rs.getString("created_at") ?: ""),
                    "is_self_drive" to rs.getBoolean("is_self_drive"),
                    "vehicle_id" to rs.getInt("vehicle_id"),
                    "driver_name" to (rs.getString("driver_name") ?: ""),
                    "driver_profile_pic" to (rs.getString("driver_profile_pic") ?: ""),
                    "driver_plate" to (rs.getString("driver_plate") ?: ""),
                    "driver_model" to (rs.getString("driver_model") ?: "")
                ))
            }
        }
        return list
    }

    fun getCustomerTrips(customerId: Int): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT o.id, o.pickup_location, o.dropoff_location, o.total_amount as fare, 
                       o.status, o.created_at, d.pickup_lat, d.pickup_lng, d.dropoff_lat, d.dropoff_lng,
                       dr.full_name as driver_name, dr.profile_picture as driver_profile_pic,
                       dr.vehicle_number as driver_plate, dr.vehicle_model as driver_model
                FROM orders o
                LEFT JOIN deliveries d ON o.id = d.order_id
                LEFT JOIN drivers dr ON d.driver_id = dr.id
                WHERE o.customer_id = ?
                ORDER BY o.id DESC
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, customerId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "pickup" to (rs.getString("pickup_location") ?: ""),
                    "dropoff" to (rs.getString("dropoff_location") ?: ""),
                    "fare" to rs.getDouble("fare"),
                    "status" to (rs.getString("status") ?: "UNKNOWN"),
                    "created_at" to rs.getTimestamp("created_at").toString(),
                    "dropoff_lat" to rs.getDouble("dropoff_lat"),
                    "dropoff_lng" to rs.getDouble("dropoff_lng"),
                    "driver_name" to (rs.getString("driver_name") ?: ""),
                    "driver_profile_pic" to (rs.getString("driver_profile_pic") ?: ""),
                    "driver_plate" to (rs.getString("driver_plate") ?: ""),
                    "driver_model" to (rs.getString("driver_model") ?: "")
                ))
            }
        }
        return list
    }

    fun addSavedPlace(req: SavedPlace): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO saved_places (id, customer_id, label, address, latitude, longitude) VALUES (?, ?, ?, ?, ?, ?)"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, req.id)
            stmt.setInt(2, req.customerId.toInt())
            stmt.setString(3, req.label)
            stmt.setString(4, req.address)
            stmt.setDouble(5, req.latitude)
            stmt.setDouble(6, req.longitude)
            return stmt.executeUpdate() > 0
        }
    }

    fun updateSavedPlace(id: String, req: SavedPlace): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE saved_places SET label = ?, address = ?, latitude = ?, longitude = ? WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, req.label)
            stmt.setString(2, req.address)
            stmt.setDouble(3, req.latitude)
            stmt.setDouble(4, req.longitude)
            stmt.setString(5, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun deleteSavedPlace(id: String): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "DELETE FROM saved_places WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, id)
            return stmt.executeUpdate() > 0
        }
    }

    fun getDriverProfile(id: Int): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            // Try drivers first
            val sql = "SELECT id, full_name, email, phone, region, profile_picture, vehicle_type, vehicle_number, vehicle_model, status FROM drivers WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return mapOf(
                    "success" to true,
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("full_name"),
                    "email" to rs.getString("email"),
                    "phone" to rs.getString("phone"),
                    "region" to rs.getString("region"),
                    "profile_picture" to rs.getString("profile_picture"),
                    "vehicle_type" to rs.getString("vehicle_type"),
                    "vehicle_number" to rs.getString("vehicle_number"),
                    "vehicle_model" to rs.getString("vehicle_model"),
                    "status" to rs.getString("status")
                )
            }

            // Try fleet_owners
            val sqlOwner = "SELECT id, full_name, email, phone, region, profile_picture, status FROM fleet_owners WHERE id = ?"
            val stmtOwner = conn.prepareStatement(sqlOwner)
            stmtOwner.setInt(1, id)
            val rsOwner = stmtOwner.executeQuery()
            if (rsOwner.next()) {
                return mapOf(
                    "success" to true,
                    "id" to rsOwner.getInt("id"),
                    "name" to rsOwner.getString("full_name"),
                    "email" to rsOwner.getString("email"),
                    "phone" to rsOwner.getString("phone"),
                    "region" to rsOwner.getString("region"),
                    "profile_picture" to rsOwner.getString("profile_picture"),
                    "vehicle_type" to "Fleet",
                    "status" to rsOwner.getString("status")
                )
            }
        }
        return null
    }

    fun submitRating(driverId: Int, rating: Float): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE drivers SET rating = (rating + ?) / 2.0 WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setDouble(1, rating.toDouble())
            stmt.setInt(2, driverId)
            return stmt.executeUpdate() > 0
        }
    }

    fun acceptDelivery(driverId: Int, deliveryId: Int): AuthResponse {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.autoCommit = false
            try {
                val rs = conn.prepareStatement("SELECT status, order_id FROM deliveries WHERE id = ? FOR UPDATE").apply { setInt(1, deliveryId) }.executeQuery()
                if (rs.next() && rs.getString("status") == "PENDING") {
                    val orderId = rs.getInt("order_id")
                    
                    conn.prepareStatement("UPDATE deliveries SET driver_id = ?, status = 'ASSIGNED' WHERE id = ?").apply {
                        setInt(1, driverId)
                        setInt(2, deliveryId)
                        executeUpdate()
                    }
                    
                    conn.prepareStatement("UPDATE orders SET status = 'ASSIGNED' WHERE id = ?").apply {
                        setInt(1, orderId)
                        executeUpdate()
                    }
                    
                    conn.commit()
                    return AuthResponse(true, "Delivery accepted successfully", orderId.toString(), null)
                } else {
                    conn.rollback()
                    return AuthResponse(false, "Delivery no longer available", null, null)
                }
            } catch (e: Exception) {
                conn.rollback()
                return AuthResponse(false, "Error: ${e.localizedMessage}", null, null)
            }
        }
    }

    fun getDriverDeliveries(driverId: Int): List<Delivery> {
        val list = mutableListOf<Delivery>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT d.*, c.name as customer_name, c.phone as customer_phone, c.profile_picture as customer_profile_pic, o.total_amount as total_fare
                FROM deliveries d
                JOIN orders o ON d.order_id = o.id
                JOIN customers c ON o.customer_id = c.id
                WHERE d.driver_id = ? AND d.status NOT IN ('DELIVERED', 'CANCELLED')
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, driverId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(Delivery(
                    id = rs.getInt("id").toString(),
                    orderId = rs.getInt("order_id"),
                    driverId = rs.getInt("driver_id").toString(),
                    pickupLocation = rs.getString("pickup_location"),
                    dropOffLocation = rs.getString("dropoff_location"),
                    pickupLat = rs.getDouble("pickup_lat"),
                    pickupLng = rs.getDouble("pickup_lng"),
                    dropOffLat = rs.getDouble("dropoff_lat"),
                    dropOffLng = rs.getDouble("dropoff_lng"),
                    status = DeliveryStatus.valueOf(rs.getString("status").uppercase()),
                    distanceKm = rs.getDouble("distance_km"),
                    estimatedEarnings = rs.getDouble("estimated_earnings"),
                    pickupEtaMin = 5.0,
                    customerName = rs.getString("customer_name"),
                    customerPhone = rs.getString("customer_phone"),
                    customerProfilePic = rs.getString("customer_profile_pic"),
                    totalFare = rs.getDouble("total_fare")
                ))
            }
        }
        return list
    }

    fun getDriverRentals(driverId: Int): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT r.*, c.name as customer_name, c.profile_picture as customer_profile_pic, v.name as vehicle_name FROM rentals r JOIN customers c ON r.customer_id = c.id JOIN rental_vehicles v ON r.vehicle_id = v.id WHERE r.driver_id = ? ORDER BY r.id DESC"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, driverId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "customer_name" to rs.getString("customer_name"),
                    "customer_profile_pic" to (rs.getString("customer_profile_pic") ?: ""),
                    "vehicle_name" to rs.getString("vehicle_name"),
                    "status" to rs.getString("status"),
                    "total_price" to rs.getDouble("total_price"),
                    "booking_code" to (rs.getString("booking_code") ?: ""),
                    "pickup_location" to rs.getString("pickup_location"),
                    "start_time" to (rs.getString("start_time") ?: "")
                ))
            }
        }
        return list
    }

    fun verifyRentalBooking(rentalId: Int, code: String): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT booking_code FROM rentals WHERE id = ?").apply { setInt(1, rentalId) }.executeQuery()
            if (rs.next() && rs.getString(1) == code) {
                updateRentalStatus(rentalId, "ACTIVE")
                return true
            }
        }
        return false
    }

    fun updateDeliveryStatus(deliveryId: Int, status: String): Pair<Int, Double>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT order_id, driver_id, estimated_earnings FROM deliveries WHERE id = ?").apply { setInt(1, deliveryId) }.executeQuery()
            if (rs.next()) {
                val orderId = rs.getInt(1)
                val driverId = rs.getInt(2)
                val earnings = rs.getDouble(3)
                
                conn.prepareStatement("UPDATE deliveries SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, deliveryId); executeUpdate() }
                conn.prepareStatement("UPDATE orders SET status = ? WHERE id = ?").apply { setString(1, status); setInt(2, orderId); executeUpdate() }
                
                if (status == "DELIVERED" && driverId != 0) {
                    val statsSql = """
                        INSERT INTO driver_stats (driver_id, earnings_today, total_earnings, completed_today, total_deliveries)
                        VALUES (?, ?, ?, 1, 1)
                        ON CONFLICT (driver_id) DO UPDATE SET
                            earnings_today = CASE 
                                WHEN driver_stats.updated_at::date = CURRENT_DATE THEN driver_stats.earnings_today + EXCLUDED.earnings_today 
                                ELSE EXCLUDED.earnings_today 
                            END,
                            total_earnings = driver_stats.total_earnings + EXCLUDED.total_earnings,
                            completed_today = CASE 
                                WHEN driver_stats.updated_at::date = CURRENT_DATE THEN driver_stats.completed_today + 1 
                                ELSE 1 
                            END,
                            total_deliveries = driver_stats.total_deliveries + 1,
                            updated_at = CURRENT_TIMESTAMP
                    """.trimIndent()
                    conn.prepareStatement(statsSql).apply {
                        setInt(1, driverId)
                        setDouble(2, earnings)
                        setDouble(3, earnings)
                        executeUpdate()
                    }
                }
                return orderId to earnings
            }
        }
        return null
    }

    fun getWalletBalance(driverId: Int): Double {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT total_earnings FROM driver_stats WHERE driver_id = ?").apply { setInt(1, driverId) }.executeQuery()
            return if (rs.next()) rs.getDouble(1) else 0.0
        }
    }

    fun getWalletTransactions(driverId: Int): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT * FROM payments WHERE user_id = ? AND user_type = 'DRIVER' ORDER BY created_at DESC").apply { setInt(1, driverId) }.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "amount" to rs.getDouble("amount"),
                    "type" to rs.getString("payment_type"),
                    "reference" to rs.getString("reference"),
                    "status" to rs.getString("status"),
                    "created_at" to rs.getTimestamp("created_at").toString()
                ))
            }
        }
        return list
    }

    fun requestWalletTopup(driverId: Int, amount: Double, reference: String, type: String): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO wallet_topups (driver_id, amount, reference_code, payment_type, status) VALUES (?, ?, ?, ?, 'PENDING') RETURNING id"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, driverId)
            stmt.setDouble(2, amount)
            stmt.setString(3, reference)
            stmt.setString(4, type)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt(1) else null
        }
    }

    fun getTopupHistory(driverId: Int): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT * FROM wallet_topups WHERE driver_id = ? ORDER BY created_at DESC").apply { setInt(1, driverId) }.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "amount" to rs.getDouble("amount"),
                    "reference" to rs.getString("reference_code"),
                    "status" to rs.getString("status"),
                    "payment_type" to (rs.getString("payment_type") ?: "TOPUP"),
                    "created_at" to rs.getTimestamp("created_at").toString()
                ))
            }
        }
        return list
    }

    fun saveSimulationRun(name: String, params: Any, result: Any) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO simulation_runs (scenario_name, parameters, results) VALUES (?, ?, ?)"
            conn.prepareStatement(sql).apply {
                setString(1, name)
                setString(2, com.google.gson.Gson().toJson(params))
                setString(3, com.google.gson.Gson().toJson(result))
                executeUpdate()
            }
        }
    }

    fun getMovementHistory(limit: Int = 1000): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT * FROM movement_logs ORDER BY created_at DESC LIMIT ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, limit)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "entity_id" to rs.getInt("entity_id"),
                    "entity_type" to rs.getString("entity_type"),
                    "lat" to rs.getDouble("latitude"),
                    "lng" to rs.getDouble("longitude"),
                    "created_at" to rs.getString("created_at")
                ))
            }
        }
        return list
    }

    fun getAnalyticsReport(): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rsCount = conn.createStatement().executeQuery("SELECT COUNT(*) FROM movement_logs")
            if (rsCount.next()) stats["totalLogs"] = rsCount.getInt(1)

            val rsHours = conn.createStatement().executeQuery(
                "SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count FROM movement_logs GROUP BY hour ORDER BY hour ASC"
            )
            val busyHours = mutableListOf<Map<String, Any>>()
            while (rsHours.next()) {
                busyHours.add(mapOf("hour" to rsHours.getInt("hour"), "count" to rsHours.getInt("count")))
            }
            stats["busyHours"] = busyHours

            val rsRev = conn.createStatement().executeQuery("""
                SELECT DATE(created_at) as date, SUM(amount) as total 
                FROM payments 
                WHERE created_at > CURRENT_DATE - INTERVAL '7 days' 
                GROUP BY date ORDER BY date ASC
            """.trimIndent())
            val revenueTrend = mutableListOf<Map<String, Any>>()
            while (rsRev.next()) {
                revenueTrend.add(mapOf("date" to rsRev.getString("date"), "total" to rsRev.getDouble("total")))
            }
            stats["revenueTrend"] = revenueTrend

            val rsDist = conn.createStatement().executeQuery("SELECT entity_type, COUNT(*) as count FROM movement_logs GROUP BY entity_type")
            val distribution = mutableMapOf<String, Int>()
            while (rsDist.next()) {
                distribution[rsDist.getString("entity_type")] = rsDist.getInt("count")
            }
            stats["distribution"] = distribution
        }
        return stats
    }

    fun getDriverForSos(driverId: Int): Map<String, String>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT full_name, phone, vehicle_number, vehicle_model FROM drivers WHERE id = ?").apply { setInt(1, driverId) }.executeQuery()
            if (rs.next()) {
                return mapOf(
                    "name" to rs.getString(1),
                    "phone" to rs.getString(2),
                    "plate" to rs.getString(3),
                    "model" to rs.getString(4)
                )
            }
        }
        return null
    }

    fun updateFcmToken(userId: Int, type: String, token: String) {
        val table = if (type == "customer") "customers" else "drivers"
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("UPDATE $table SET fcm_token = ? WHERE id = ?").apply {
                setString(1, token); setInt(2, userId); executeUpdate()
            }
        }
    }

    fun loginCustomer(phone: String, password: String): AuthResponse {
        val normalizedPhone = normalizePhone(phone)
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT id, name, password FROM customers WHERE TRIM(phone) = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, normalizedPhone)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val dbPass = rs.getString("password")
                val customerId = rs.getInt("id")
                
                var isMatch = false
                var needsMigration = false
                
                if (dbPass == "GOOGLE_AUTH") {
                    isMatch = true
                } else if (dbPass != null) {
                    try {
                        isMatch = BCrypt.checkpw(password, dbPass)
                    } catch (e: Throwable) {
                        if (dbPass == password) {
                            isMatch = true
                            needsMigration = true
                        }
                    }
                }

                if (isMatch) {
                    if (needsMigration) {
                        val hashed = BCrypt.hashpw(password, BCrypt.gensalt())
                        conn.prepareStatement("UPDATE customers SET password = ? WHERE id = ?").apply {
                            setString(1, hashed)
                            setInt(2, customerId)
                            executeUpdate()
                        }
                    }
                    return AuthResponse(true, "Success", customerId.toString(), rs.getString("name"))
                } else {
                    return AuthResponse(false, "Invalid password", null, null)
                }
            } else {
                return AuthResponse(false, "User not found with this phone number", null, null)
            }
        }
    }

    fun syncCustomerWithFirebase(email: String, firebaseUid: String): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            // Use LOWER() for case-insensitive email matching
            val selectSql = "SELECT id, name, firebase_uid FROM customers WHERE LOWER(email) = LOWER(?)"
            val selectStmt = conn.prepareStatement(selectSql)
            selectStmt.setString(1, email.trim())
            val rs = selectStmt.executeQuery()
            if (rs.next()) {
                val dbUid = rs.getString("firebase_uid")
                if (dbUid == null) {
                    conn.prepareStatement("UPDATE customers SET firebase_uid = ? WHERE id = ?").apply {
                        setString(1, firebaseUid)
                        setInt(2, rs.getInt("id"))
                        executeUpdate()
                    }
                }
                return mapOf("id" to rs.getInt("id"), "name" to rs.getString("name"))
            }
        }
        return null
    }

    fun registerCustomer(req: CustomerRegisterRequest): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO customers (name, email, phone, default_address, password, region, profile_picture, firebase_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, req.name.trim())
            stmt.setString(2, req.email.trim().lowercase())
            stmt.setString(3, normalizePhone(req.phone))
            stmt.setString(4, req.address.trim())
            val hashedPassword = if (req.password.trim() == "GOOGLE_AUTH") "GOOGLE_AUTH" else BCrypt.hashpw(req.password.trim(), BCrypt.gensalt())
            stmt.setString(5, hashedPassword)
            stmt.setString(6, req.region ?: "")
            stmt.setString(7, req.profilePicture ?: "")
            stmt.setString(8, req.firebaseUid)
            val rs = stmt.executeQuery()
            if (rs.next()) return rs.getInt(1)
        }
        return null
    }

    fun getCustomerEmail(id: Int): String? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT email FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) return rs.getString(1)
        }
        return null
    }

    fun createOrder(req: OrderCreateRequest, pin: String): Pair<Int, Int>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.autoCommit = false
            try {
                val isScheduled = !req.scheduledTime.isNullOrBlank()
                val initialStatus = if (isScheduled) "SCHEDULED" else "PENDING"
                
                val orderSql = "INSERT INTO orders (customer_id, total_amount, status, pickup_location, dropoff_location, verification_pin, scheduled_time) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id"
                val orderStmt = conn.prepareStatement(orderSql)
                orderStmt.setInt(1, req.customerId.toInt())
                orderStmt.setDouble(2, req.estimatedFare)
                orderStmt.setString(3, initialStatus)
                orderStmt.setString(4, req.pickupLocation)
                orderStmt.setString(5, req.dropOffLocation)
                orderStmt.setString(6, pin)
                
                if (isScheduled) {
                    try {
                        val formatted = req.scheduledTime!!.replace("T", " ")
                        val finalTime = if (formatted.length == 16) "$formatted:00" else formatted
                        orderStmt.setTimestamp(7, Timestamp.valueOf(finalTime))
                    } catch (e: Exception) {
                        orderStmt.setNull(7, java.sql.Types.TIMESTAMP)
                    }
                } else {
                    orderStmt.setNull(7, java.sql.Types.TIMESTAMP)
                }
                
                val rs = orderStmt.executeQuery()
                if (rs.next()) {
                    val orderId = rs.getInt(1)
                    
                    // 2. Create Delivery
                    val requestedCategory = req.requestedVehicleType ?: "Economy"
                    var commissionPercent = 15.0 // Default
                    
                    // Fetch commission from ride_categories
                    val catSql = "SELECT driver_commission_percent FROM ride_categories WHERE service_id = ?"
                    conn.prepareStatement(catSql).apply {
                        setString(1, requestedCategory)
                        val rsCat = executeQuery()
                        if (rsCat.next()) {
                            commissionPercent = rsCat.getDouble(1)
                        }
                    }
                    val driverEarnings = if (req.serviceType == ServiceType.RENTAL) {
                        req.estimatedFare * (1.0 - (commissionPercent / 100.0))
                    } else {
                        req.estimatedFare
                    }

                    val deliverySql = "INSERT INTO deliveries (order_id, pickup_location, dropoff_location, status, service_type, estimated_earnings, pickup_lat, pickup_lng, dropoff_lat, dropoff_lng, distance_km) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
                    val delStmt = conn.prepareStatement(deliverySql)
                    delStmt.setInt(1, orderId)
                    delStmt.setString(2, req.pickupLocation)
                    delStmt.setString(3, req.dropOffLocation)
                    delStmt.setString(4, initialStatus)
                    delStmt.setString(5, req.requestedVehicleType ?: req.serviceType.name)
                    delStmt.setDouble(6, driverEarnings) 
                    delStmt.setDouble(7, req.pickupLat)
                    delStmt.setDouble(8, req.pickupLng)
                    delStmt.setDouble(9, req.dropOffLat)
                    delStmt.setDouble(10, req.dropOffLng)
                    delStmt.setDouble(11, req.distanceKm)
                    
                    val rsDel = delStmt.executeQuery()
                    if (rsDel.next()) {
                        val deliveryId = rsDel.getInt(1)
                        conn.commit()
                        return orderId to deliveryId
                    }
                }
                conn.rollback()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
        return null
    }

    fun bookRental(req: RentalBookRequest, bookingCode: String): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val config = getPricingConfig()
            val guestFeePercent = config.rentalCustomerServiceFeePercent
            val ownerCommPercent = config.rentalOwnerCommissionPercent
            
            val days = max(1, req.durationHours / 24)
            val dailyRate = req.totalPrice / (days + (guestFeePercent / 100.0))
            
            val basePrice = days * dailyRate
            val guestFee = req.totalPrice - basePrice
            val ownerCommAmount = basePrice * (ownerCommPercent / 100.0)
            val ownerEarnings = basePrice - ownerCommAmount

            val sql = """
                INSERT INTO rentals (
                    customer_id, vehicle_id, pickup_location, pickup_lat, pickup_lng, 
                    duration_hours, total_price, base_price, customer_service_fee, 
                    owner_commission_amount, owner_earnings, start_time, trip_notes, 
                    stops, booking_code, is_self_drive, status, payment_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 'PENDING') RETURNING id
            """.trimIndent()
            
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, req.customerId)
            stmt.setInt(2, req.vehicleId)
            stmt.setString(3, req.pickupLocation)
            stmt.setDouble(4, req.pickupLat)
            stmt.setDouble(5, req.pickupLng)
            stmt.setInt(6, req.durationHours)
            stmt.setDouble(7, req.totalPrice)
            stmt.setDouble(8, basePrice)
            stmt.setDouble(9, guestFee)
            stmt.setDouble(10, ownerCommAmount)
            stmt.setDouble(11, ownerEarnings)
            
            val startTimeStr = req.startTime
            if (!startTimeStr.isNullOrBlank() && startTimeStr != "Select Date") {
                try {
                    val formatted = if (startTimeStr.length == 10) "$startTimeStr 00:00:00" else startTimeStr.replace("T", " ")
                    stmt.setTimestamp(12, Timestamp.valueOf(formatted))
                } catch (e: Exception) {
                    stmt.setNull(12, java.sql.Types.TIMESTAMP)
                }
            } else {
                stmt.setNull(12, java.sql.Types.TIMESTAMP)
            }
            
            stmt.setString(13, req.tripNotes ?: "")
            stmt.setString(14, req.stops ?: "")
            stmt.setString(15, bookingCode)
            stmt.setBoolean(16, req.isSelfDrive)
            
            val rs = stmt.executeQuery()
            if (rs.next()) return rs.getInt(1)
        }
        return null
    }

    fun getCustomerIdForRental(rentalId: Int): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT customer_id FROM rentals WHERE id = ?").apply { setInt(1, rentalId) }.executeQuery()
            if (rs.next()) return rs.getInt(1)
        }
        return null
    }

    fun getDriverEmail(id: Int): String? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT email FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) return rs.getString(1)
        }
        return null
    }

    fun initializePaystackPayment(email: String, amountGHS: Double, reference: String): Pair<String?, String?>? {
        try {
            val settings = getSystemSettings()
            val mode = settings["paystack_mode"] as? String ?: "TEST"
            val secret = if (mode == "LIVE") settings["paystack_live_secret"] as? String else settings["paystack_test_secret"] as? String
            
            if (secret.isNullOrBlank()) {
                println("Paystack Error: No secret key found for $mode mode")
                return null
            }

            val url = URL("https://api.paystack.co/transaction/initialize")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $secret")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JsonObject().apply {
                addProperty("email", email)
                addProperty("amount", (amountGHS * 100).toInt())
                addProperty("reference", reference)
                val baseUrl = System.getenv("BASE_URL") ?: "http://localhost:8080"
                addProperty("callback_url", "$baseUrl/payment-success")
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            
            if (conn.responseCode == 200) {
                val response = JsonParser.parseString(conn.inputStream.bufferedReader().readText()).asJsonObject
                val data = response.getAsJsonObject("data")
                return Pair(data.get("authorization_url").asString, data.get("access_code").asString)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun loginDriver(phone: String, password: String): AuthResponse {
        val normalizedPhone = normalizePhone(phone)
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT id, full_name, status, profile_picture, user_role, company_name, vehicle_type, password FROM drivers WHERE TRIM(phone) = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, normalizedPhone)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val dbPass = rs.getString("password")
                val driverId = rs.getInt("id")
                
                var isMatch = false
                var needsMigration = false
                
                if (dbPass == "GOOGLE_AUTH") {
                    isMatch = true
                } else if (dbPass != null) {
                    try {
                        isMatch = BCrypt.checkpw(password, dbPass)
                    } catch (e: Throwable) {
                        if (dbPass == password) {
                            isMatch = true
                            needsMigration = true
                        }
                    }
                }

                if (isMatch) {
                    if (needsMigration) {
                        val hashed = BCrypt.hashpw(password, BCrypt.gensalt())
                        conn.prepareStatement("UPDATE drivers SET password = ? WHERE id = ?").apply {
                            setString(1, hashed)
                            setInt(2, driverId)
                            executeUpdate()
                        }
                    }
                    return AuthResponse(
                        success = true, 
                        message = "Success", 
                        user_id = driverId.toString(), 
                        name = rs.getString("full_name"), 
                        status = rs.getString("status"), 
                        profile_picture = rs.getString("profile_picture"),
                        user_role = rs.getString("user_role"),
                        company_name = rs.getString("company_name"),
                        vehicle_type = rs.getString("vehicle_type")
                    )
                } else {
                    return AuthResponse(false, "Invalid password", null, null)
                }
            } else {
                val sqlOwner = "SELECT id, full_name, status, profile_picture, password FROM fleet_owners WHERE TRIM(phone) = ?"
                val stmtOwner = conn.prepareStatement(sqlOwner)
                stmtOwner.setString(1, normalizedPhone)
                val rsOwner = stmtOwner.executeQuery()
                if (rsOwner.next()) {
                    val dbPass = rsOwner.getString("password")
                    val ownerId = rsOwner.getInt("id")
                    
                    var isMatch = false
                    var needsMigration = false
                    
                    if (dbPass == "GOOGLE_AUTH") {
                        isMatch = true
                    } else if (dbPass != null) {
                        try {
                            isMatch = BCrypt.checkpw(password, dbPass)
                        } catch (e: Throwable) {
                            if (dbPass == password) {
                                isMatch = true
                                needsMigration = true
                            }
                        }
                    }

                    if (isMatch) {
                        if (needsMigration) {
                            val hashed = BCrypt.hashpw(password, BCrypt.gensalt())
                            conn.prepareStatement("UPDATE fleet_owners SET password = ? WHERE id = ?").apply {
                                setString(1, hashed)
                                setInt(2, ownerId)
                                executeUpdate()
                            }
                        }
                        return AuthResponse(
                            success = true,
                            message = "Success",
                            user_id = ownerId.toString(),
                            name = rsOwner.getString("full_name"),
                            status = rsOwner.getString("status"),
                            profile_picture = rsOwner.getString("profile_picture"),
                            user_role = "OWNER"
                        )
                    } else {
                        return AuthResponse(false, "Invalid password", null, null)
                    }
                } else {
                    return AuthResponse(false, "Driver not found with this phone number", null, null)
                }
            }
        }
    }

    fun validateAdmin(user: String, pass: String): AdminPrincipal? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT id, role, region, can_view_analytics, password FROM admins WHERE username = ? AND is_active = true"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, user)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val dbPass = rs.getString("password")
                val adminId = rs.getInt("id")
                
                var isMatch = false
                var needsMigration = false
                
                if (dbPass != null) {
                    try {
                        isMatch = BCrypt.checkpw(pass, dbPass)
                    } catch (e: Throwable) {
                        if (dbPass == pass) {
                            isMatch = true
                            needsMigration = true
                        }
                    }
                }

                if (isMatch) {
                    if (needsMigration) {
                        val hashed = BCrypt.hashpw(pass, BCrypt.gensalt())
                        conn.prepareStatement("UPDATE admins SET password = ? WHERE id = ?").apply {
                            setString(1, hashed)
                            setInt(2, adminId)
                            executeUpdate()
                        }
                    }
                    return AdminPrincipal(
                        user,
                        rs.getString("role"),
                        rs.getString("region"),
                        rs.getBoolean("can_view_analytics")
                    )
                }
            }
        }
        return null
    }

    fun syncDriverWithFirebase(email: String, firebaseUid: String): Driver? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val selectSql = "SELECT * FROM drivers WHERE email = ?"
            val selectStmt = conn.prepareStatement(selectSql)
            selectStmt.setString(1, email.lowercase())
            val rs = selectStmt.executeQuery()
            if (rs.next()) {
                val dbUid = rs.getString("firebase_uid")
                if (dbUid == null) {
                    conn.prepareStatement("UPDATE drivers SET firebase_uid = ? WHERE email = ?").apply {
                        setString(1, firebaseUid)
                        setString(2, email.lowercase())
                        executeUpdate()
                    }
                }
                return Driver(
                    id = rs.getInt("id"), fullName = rs.getString("full_name") ?: "", email = rs.getString("email") ?: "",
                    phone = rs.getString("phone") ?: "", region = rs.getString("region") ?: "", licenseNumber = rs.getString("license_number") ?: "",
                    vehicleType = rs.getString("vehicle_type"), vehicleNumber = rs.getString("vehicle_number"), status = rs.getString("status") ?: "PENDING",
                    isOnline = rs.getBoolean("is_online"), rating = rs.getDouble("rating"),
                    profilePicture = rs.getString("profile_picture"),
                    userRole = rs.getString("user_role") ?: "DRIVER",
                    companyName = rs.getString("company_name"),
                    registrationNumber = rs.getString("registration_number")
                )
            }
        }
        return null
    }

    fun loginDriver(phone: String): Driver? {
        val cleanPhone = phone.trim()
        val altPhone = if (cleanPhone.startsWith("+")) cleanPhone.substring(1) else "+$cleanPhone"
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT * FROM drivers WHERE TRIM(phone) = ? OR TRIM(phone) = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, cleanPhone)
            stmt.setString(2, altPhone)
            val rs = stmt.executeQuery()
            if (rs.next()) return Driver(
                id = rs.getInt("id"), fullName = rs.getString("full_name") ?: "", email = rs.getString("email") ?: "",
                phone = rs.getString("phone") ?: "", region = rs.getString("region") ?: "", licenseNumber = rs.getString("license_number") ?: "",
                vehicleType = rs.getString("vehicle_type"), vehicleNumber = rs.getString("vehicle_number"), status = rs.getString("status") ?: "PENDING",
                isOnline = rs.getBoolean("is_online"), rating = rs.getDouble("rating"),
                serviceType = try { ServiceType.valueOf(rs.getString("service_types").uppercase()) } catch(_: Exception) { ServiceType.BOTH },
                profilePicture = rs.getString("profile_picture"),
                userRole = rs.getString("user_role") ?: "DRIVER",
                companyName = rs.getString("company_name"),
                registrationNumber = rs.getString("registration_number")
            )

            val sqlOwner = "SELECT * FROM fleet_owners WHERE TRIM(phone) = ? OR TRIM(phone) = ?"
            val stmtOwner = conn.prepareStatement(sqlOwner)
            stmtOwner.setString(1, cleanPhone)
            stmtOwner.setString(2, altPhone)
            val rsOwner = stmtOwner.executeQuery()
            if (rsOwner.next()) return Driver(
                id = rsOwner.getInt("id"),
                fullName = rsOwner.getString("full_name") ?: "",
                email = rsOwner.getString("email") ?: "",
                phone = rsOwner.getString("phone") ?: "",
                region = rsOwner.getString("region") ?: "",
                licenseNumber = "N/A",
                vehicleType = "Fleet",
                vehicleNumber = "OWNER",
                status = rsOwner.getString("status") ?: "PENDING",
                isOnline = false,
                rating = 5.0,
                serviceType = ServiceType.BOTH,
                profilePicture = rsOwner.getString("profile_picture"),
                userRole = "OWNER",
                companyName = rsOwner.getString("company_name"),
                registrationNumber = rsOwner.getString("registration_number")
            )
        }
        return null
    }

    fun registerDriver(data: Map<String, String>): Int? {
        val role = data["user_role"]?.trim()?.uppercase() ?: "DRIVER"
        val email = data["email"]?.trim()?.lowercase() ?: ""

        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.autoCommit = false
            try {
                var primaryId: Int? = null
                var fleetOwnerId: Int? = null

                if (role == "OWNER" || role == "BOTH") {
                    val sql = "INSERT INTO fleet_owners (full_name, email, phone, password, company_name, registration_number, region, status, firebase_uid, emergency_contact_1, emergency_contact_2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, data["full_name"]?.trim() ?: "")
                    stmt.setString(2, email)
                    stmt.setString(3, normalizePhone(data["phone"]))
                    val password = data["password"]?.trim() ?: ""
                    val hashedPassword = if (password == "GOOGLE_AUTH") "GOOGLE_AUTH" else BCrypt.hashpw(password, BCrypt.gensalt())
                    stmt.setString(4, hashedPassword)
                    stmt.setString(5, data["company_name"]?.trim() ?: "My Fleet")
                    stmt.setString(6, data["registration_number"]?.trim())
                    stmt.setString(7, data["region"]?.trim() ?: "")
                    stmt.setString(8, "PENDING")
                    stmt.setString(9, data["firebase_uid"])
                    stmt.setString(10, data["emergency_contact_1"]?.trim())
                    stmt.setString(11, data["emergency_contact_2"]?.trim())
                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        fleetOwnerId = rs.getInt(1)
                        if (role == "OWNER") primaryId = fleetOwnerId
                    }
                }

                if (role == "DRIVER" || role == "BOTH") {
                    val sql = "INSERT INTO drivers (full_name, email, phone, region, password, license_number, vehicle_type, vehicle_number, service_types, user_role, company_name, registration_number, emergency_contact_1, emergency_contact_2, fleet_owner_id, firebase_uid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id"
                    val stmt = conn.prepareStatement(sql)
                    stmt.setString(1, data["full_name"]?.trim() ?: "")
                    stmt.setString(2, email)
                    stmt.setString(3, normalizePhone(data["phone"]))
                    stmt.setString(4, data["region"]?.trim() ?: "")
                    val password = data["password"]?.trim() ?: ""
                    val hashedPassword = if (password == "GOOGLE_AUTH") "GOOGLE_AUTH" else BCrypt.hashpw(password, BCrypt.gensalt())
                    stmt.setString(5, hashedPassword)
                    stmt.setString(6, data["license_number"]?.trim() ?: "")
                    stmt.setString(7, data["vehicle_type"]?.trim() ?: "")
                    stmt.setString(8, data["vehicle_number"]?.trim() ?: "")
                    stmt.setString(9, data["service_type"]?.trim()?.uppercase() ?: "BOTH")
                    stmt.setString(10, role)
                    stmt.setString(11, data["company_name"]?.trim())
                    stmt.setString(12, data["registration_number"]?.trim())
                    stmt.setString(13, data["emergency_contact_1"]?.trim())
                    stmt.setString(14, data["emergency_contact_2"]?.trim())
                    if (fleetOwnerId != null) stmt.setInt(15, fleetOwnerId) else stmt.setNull(15, java.sql.Types.INTEGER)
                    stmt.setString(16, data["firebase_uid"])

                    val rs = stmt.executeQuery()
                    if (rs.next()) {
                        val driverId = rs.getInt(1)
                        primaryId = driverId
                        conn.prepareStatement("INSERT INTO driver_stats (driver_id) VALUES (?)").apply {
                            setInt(1, driverId)
                            executeUpdate()
                        }
                    }
                }

                if (primaryId != null) {
                    conn.commit()
                    return primaryId
                }
                conn.rollback()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
        return null
    }

    fun createAdmin(user: String, email: String, pass: String, role: String, region: String?) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO admins (username, email, password, role, region) VALUES (?, ?, ?, ?, ?)"
            conn.prepareStatement(sql).apply {
                setString(1, user); setString(2, email)
                setString(3, BCrypt.hashpw(pass.trim(), BCrypt.gensalt()))
                setString(4, role); setString(5, region)
                executeUpdate()
            }
        }
    }

    fun loginAdmin(userOrEmail: String, pass: String): Map<String, Any>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT id, username, role, password FROM admins WHERE (LOWER(username) = ? OR LOWER(email) = ?)"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, userOrEmail.lowercase())
            stmt.setString(2, userOrEmail.lowercase())
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val dbPass = rs.getString("password")
                val adminId = rs.getInt("id")

                var isMatch = false
                var needsMigration = false
                
                if (dbPass != null) {
                    try {
                        isMatch = BCrypt.checkpw(pass, dbPass)
                    } catch (e: Throwable) {
                        if (dbPass == pass) {
                            isMatch = true
                            needsMigration = true
                        }
                    }
                }

                if (isMatch) {
                    if (needsMigration) {
                        val hashed = BCrypt.hashpw(pass, BCrypt.gensalt())
                        conn.prepareStatement("UPDATE admins SET password = ? WHERE id = ?").apply {
                            setString(1, hashed)
                            setInt(2, adminId)
                            executeUpdate()
                        }
                    }
                    return mapOf(
                        "id" to adminId,
                        "username" to rs.getString("username"),
                        "role" to rs.getString("role")
                    )
                }
            }
        }
        return null
    }

    fun getSystemSettings(): Map<String, Any> {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM system_settings LIMIT 1")
            if (rs.next()) {
                return mapOf(
                    "support_phone" to (rs.getString("support_phone") ?: ""),
                    "support_email" to (rs.getString("support_email") ?: ""),
                    "support_whatsapp" to (rs.getString("support_whatsapp") ?: ""),
                    "app_version_customer" to (rs.getString("app_version_customer") ?: "1.0.0"),
                    "app_version_driver" to (rs.getString("app_version_driver") ?: "1.0.0"),
                    "maintenance_mode" to rs.getBoolean("maintenance_mode"),
                    "paystack_mode" to (rs.getString("paystack_mode") ?: "TEST"),
                    "paystack_test_secret" to (rs.getString("paystack_test_secret") ?: ""),
                    "paystack_live_secret" to (rs.getString("paystack_live_secret") ?: ""),
                    "paystack_test_public" to (rs.getString("paystack_test_public") ?: ""),
                    "paystack_live_public" to (rs.getString("paystack_live_public") ?: "")
                )
            }
        }
        return mapOf(
            "support_phone" to "", "support_email" to "", "support_whatsapp" to "",
            "app_version_customer" to "1.0.0", "app_version_driver" to "1.0.0", "maintenance_mode" to false,
            "paystack_mode" to "TEST", "paystack_test_secret" to "", "paystack_live_secret" to "",
            "paystack_test_public" to "", "paystack_live_public" to ""
        )
    }

    fun updateSystemSettings(
        phone: String?, email: String?, whatsapp: String?, verCustomer: String?, verDriver: String?, maintenance: Boolean,
        psMode: String? = null, psTestSec: String? = null, psLiveSec: String? = null, psTestPub: String? = null, psLivePub: String? = null
    ) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE system_settings SET support_phone=COALESCE(?, support_phone), support_email=COALESCE(?, support_email), support_whatsapp=COALESCE(?, support_whatsapp), app_version_customer=COALESCE(?, app_version_customer), app_version_driver=COALESCE(?, app_version_driver), maintenance_mode=?, paystack_mode=COALESCE(?, paystack_mode), paystack_test_secret=COALESCE(?, paystack_test_secret), paystack_live_secret=COALESCE(?, paystack_live_secret), paystack_test_public=COALESCE(?, paystack_test_public), paystack_live_public=COALESCE(?, paystack_live_public), updated_at=CURRENT_TIMESTAMP"
            conn.prepareStatement(sql).apply {
                setString(1, phone)
                setString(2, email)
                setString(3, whatsapp)
                setString(4, verCustomer)
                setString(5, verDriver)
                setBoolean(6, maintenance)
                setString(7, psMode)
                setString(8, psTestSec)
                setString(9, psLiveSec)
                setString(10, psTestPub)
                setString(11, psLivePub)
                executeUpdate()
            }
        }
    }

    fun getOrderStatus(orderId: Int): String {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT status FROM orders WHERE id = ?").apply {
                setInt(1, orderId)
            }.executeQuery()
            return if (rs.next()) rs.getString("status") else "NOT_FOUND"
        }
    }

    fun updateDriverDocument(driverId: String, docType: String, fileUrl: String) {
        val column = when (docType) {
            "profile_pic" -> "profile_picture"
            "drivers_license" -> "license_image"
            "ghana_card" -> "id_front_image"
            "insurance_cert" -> "insurance_cert"
            "roadworthy_cert" -> "roadworthy_cert"
            "ghana_card_back" -> "id_back_image"
            "business_cert" -> "business_certificate"
            else -> null
        }

        if (column != null) {
            DatabaseInitializer.getDataSource().connection.use { conn ->
                val id = driverId.toInt()
                // Try drivers
                val sql = "UPDATE drivers SET $column = ?, status = CASE WHEN status = 'PENDING' THEN 'PENDING_DOCS' ELSE status END, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
                val stmt = conn.prepareStatement(sql)
                stmt.setString(1, fileUrl)
                stmt.setInt(2, id)
                val updated = stmt.executeUpdate()
                
                if (updated == 0) {
                    // Try fleet_owners
                    val fleetOwnerColumns = listOf("profile_picture", "id_front_image", "id_back_image", "business_certificate")
                    if (column in fleetOwnerColumns) {
                        val sqlOwner = "UPDATE fleet_owners SET $column = ?, status = CASE WHEN status = 'PENDING' THEN 'PENDING_DOCS' ELSE status END, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
                        val stmtOwner = conn.prepareStatement(sqlOwner)
                        stmtOwner.setString(1, fileUrl)
                        stmtOwner.setInt(2, id)
                        stmtOwner.executeUpdate()
                    }
                }
            }
        }
    }

    fun updateDriverLocation(id: String, lat: Double, lng: Double, bearing: Float) {
        RedisManager.updateDriverLocation(id, lat, lng, bearing)

        val h3Index = H3Helper.getIndex(lat, lng)
        DatabaseInitializer.getDataSource().connection.use { conn ->
            // Ensure is_online is true if receiving location updates (and daily fee is paid)
            val driverId = id.toIntOrNull() ?: 0
            val shouldBeOnline = if (driverId != 0) isDailyFeePaid(driverId) else false

            val sql = """
                INSERT INTO driver_stats (driver_id, latitude, longitude, bearing, h3_index, is_online) 
                VALUES (?, ?, ?, ?, ?, ?) 
                ON CONFLICT (driver_id) DO UPDATE SET 
                    latitude = EXCLUDED.latitude, 
                    longitude = EXCLUDED.longitude, 
                    bearing = EXCLUDED.bearing, 
                    h3_index = EXCLUDED.h3_index,
                    is_online = CASE WHEN EXCLUDED.is_online = true THEN true ELSE driver_stats.is_online END,
                    updated_at = CURRENT_TIMESTAMP
            """.trimIndent()
            
            conn.prepareStatement(sql).apply {
                setInt(1, driverId)
                setDouble(2, lat)
                setDouble(3, lng)
                setFloat(4, bearing)
                setString(5, h3Index)
                setBoolean(6, shouldBeOnline)
                executeUpdate()
            }
            
            if (shouldBeOnline) {
                conn.prepareStatement("UPDATE drivers SET is_online = true WHERE id = ? AND is_online = false").apply {
                    setInt(1, driverId)
                    executeUpdate()
                }
            }
            
            val logSql = "INSERT INTO movement_logs (entity_id, entity_type, latitude, longitude) VALUES (?, 'DRIVER', ?, ?)"
            conn.prepareStatement(logSql).apply {
                setInt(1, id.toInt())
                setDouble(2, lat)
                setDouble(3, lng)
                executeUpdate()
            }
        }
    }

    fun updateRentalVehicleLocation(vehicleId: Int, lat: Double, lng: Double) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE rental_vehicles SET latitude = ?, longitude = ? WHERE id = ?"
            conn.prepareStatement(sql).apply {
                setDouble(1, lat)
                setDouble(2, lng)
                setInt(3, vehicleId)
                executeUpdate()
            }
        }
    }

    fun updateEmergencyContacts(driverId: String, c1: String, c2: String) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val id = driverId.toInt()
            // Try updating drivers
            val sql = "UPDATE drivers SET emergency_contact_1 = ?, emergency_contact_2 = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, c1)
            stmt.setString(2, c2)
            stmt.setInt(3, id)
            val updated = stmt.executeUpdate()
            
            if (updated == 0) {
                // Try updating fleet_owners
                val sqlOwner = "UPDATE fleet_owners SET emergency_contact_1 = ?, emergency_contact_2 = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
                val stmtOwner = conn.prepareStatement(sqlOwner)
                stmtOwner.setString(1, c1)
                stmtOwner.setString(2, c2)
                stmtOwner.setInt(3, id)
                stmtOwner.executeUpdate()
            }
        }
    }

    fun verifyPaystackSignature(body: String, signature: String): Boolean {
        return try {
            val settings = getSystemSettings()
            val mode = settings["paystack_mode"] as? String ?: "TEST"
            val secret = if (mode == "LIVE") settings["paystack_live_secret"] as? String else settings["paystack_test_secret"] as? String
            
            if (secret.isNullOrBlank()) return false

            val hmac = Mac.getInstance("HmacSHA512")
            val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA512")
            hmac.init(secretKey)
            val hash = hmac.doFinal(body.toByteArray())
            val computedSignature = hash.joinToString("") { "%02x".format(it) }
            computedSignature == signature
        } catch (e: Exception) {
            false
        }
    }

    fun updateDriverVehicle(id: String, type: String, number: String, model: String, service: String) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE drivers SET vehicle_type = ?, vehicle_number = ?, vehicle_model = ?, service_types = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, type)
            stmt.setString(2, number)
            stmt.setString(3, model)
            stmt.setString(4, service)
            stmt.setInt(5, id.toInt())
            stmt.executeUpdate()
        }
    }

    fun getFleetVehicles(ownerId: Int): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT * FROM rental_vehicles 
                WHERE owner_id = ? 
                   OR fleet_owner_id = ? 
                   OR fleet_owner_id = (SELECT fleet_owner_id FROM drivers WHERE id = ?)
                ORDER BY id DESC
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, ownerId)
            stmt.setInt(2, ownerId)
            stmt.setInt(3, ownerId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("name"),
                    "model" to (rs.getString("model") ?: ""),
                    "vehicle_type" to rs.getString("vehicle_type"),
                    "vehicle_number" to (rs.getString("vehicle_number") ?: ""),
                    "daily_rate" to rs.getDouble("daily_rate"),
                    "status" to (rs.getString("status") ?: "AVAILABLE"),
                    "is_available" to rs.getBoolean("is_available"),
                    "description" to (rs.getString("description") ?: ""),
                    "features" to (rs.getString("features") ?: ""),
                    "image_urls" to (rs.getString("image_urls") ?: ""),
                    "location" to (rs.getString("location") ?: ""),
                    "seats" to rs.getInt("seats"),
                    "transmission" to (rs.getString("transmission") ?: "Automatic"),
                    "fuel_type" to (rs.getString("fuel_type") ?: "Petrol")
                ))
            }
        }
        return list
    }

    fun getAllFleetVehicles(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT v.*, d.full_name as owner_name FROM rental_vehicles v LEFT JOIN drivers d ON v.owner_id = d.id ORDER BY v.id DESC")
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("name"),
                    "model" to (rs.getString("model") ?: ""),
                    "vehicle_type" to rs.getString("vehicle_type"),
                    "vehicle_number" to (rs.getString("vehicle_number") ?: ""),
                    "daily_rate" to rs.getDouble("daily_rate"),
                    "status" to (rs.getString("status") ?: "AVAILABLE"),
                    "is_available" to rs.getBoolean("is_available"),
                    "description" to (rs.getString("description") ?: ""),
                    "features" to (rs.getString("features") ?: ""),
                    "image_urls" to (rs.getString("image_urls") ?: ""),
                    "location" to (rs.getString("location") ?: ""),
                    "owner_name" to (rs.getString("owner_name") ?: "System"),
                    "seats" to rs.getInt("seats"),
                    "transmission" to (rs.getString("transmission") ?: "Automatic"),
                    "fuel_type" to (rs.getString("fuel_type") ?: "Petrol")
                ))
            }
        }
        return list
    }

    fun addVehicleToFleet(
        ownerId: Int, name: String, model: String, type: String, number: String, rate: Double,
        description: String? = null, features: String? = null, imageUrls: String? = null,
        location: String? = null, lat: Double? = null, lng: Double? = null,
        seats: Int? = 5, transmission: String? = "Automatic", fuelType: String? = "Petrol"
    ) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            var actualFleetOwnerId: Int? = null
            val driverRs = conn.prepareStatement("SELECT fleet_owner_id FROM drivers WHERE id = ?").apply { setInt(1, ownerId) }.executeQuery()
            if (driverRs.next()) {
                actualFleetOwnerId = driverRs.getObject("fleet_owner_id") as? Int
            }
            if (actualFleetOwnerId == null) {
                val ownerRs = conn.prepareStatement("SELECT id FROM fleet_owners WHERE id = ?").apply { setInt(1, ownerId) }.executeQuery()
                if (ownerRs.next()) {
                    actualFleetOwnerId = ownerId
                }
            }

            val sql = """
                INSERT INTO rental_vehicles (
                    owner_id, fleet_owner_id, name, model, vehicle_type, vehicle_number, daily_rate, 
                    description, features, image_urls, location, latitude, longitude,
                    seats, transmission, fuel_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setInt(1, ownerId)
                if (actualFleetOwnerId != null) setInt(2, actualFleetOwnerId) else setNull(2, java.sql.Types.INTEGER)
                setString(3, name)
                setString(4, model)
                setString(5, type)
                setString(6, number)
                setDouble(7, rate)
                setString(8, description)
                setString(9, features)
                setString(10, imageUrls)
                setString(11, location)
                if (lat != null) setDouble(12, lat) else setNull(12, java.sql.Types.DOUBLE)
                if (lng != null) setDouble(13, lng) else setNull(13, java.sql.Types.DOUBLE)
                setInt(14, seats ?: 5)
                setString(15, transmission ?: "Automatic")
                setString(16, fuelType ?: "Petrol")
                executeUpdate()
            }
        }
    }

    fun updateFleetVehicle(
        id: Int, name: String, model: String, type: String, number: String, rate: Double,
        description: String? = null, features: String? = null, imageUrls: String? = null,
        status: String? = null, seats: Int? = null, transmission: String? = null, fuelType: String? = null
    ) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                UPDATE rental_vehicles SET 
                    name = ?, model = ?, vehicle_type = ?, vehicle_number = ?, daily_rate = ?, 
                    description = ?, features = ?, image_urls = ?, status = COALESCE(?, status),
                    seats = COALESCE(?, seats), transmission = COALESCE(?, transmission), fuel_type = COALESCE(?, fuel_type)
                WHERE id = ?
            """.trimIndent()
            conn.prepareStatement(sql).apply {
                setString(1, name)
                setString(2, model)
                setString(3, type)
                setString(4, number)
                setDouble(5, rate)
                setString(6, description)
                setString(7, features)
                setString(8, imageUrls)
                setString(9, status)
                if (seats != null) setInt(10, seats) else setNull(10, java.sql.Types.INTEGER)
                setString(11, transmission)
                setString(12, fuelType)
                setInt(13, id)
                executeUpdate()
            }
        }
    }

    fun deleteFleetVehicle(id: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("DELETE FROM rental_vehicles WHERE id = ?").apply {
                setInt(1, id)
                executeUpdate()
            }
        }
    }

    fun getAllProducts(): List<Map<String, Any?>> {
        val list = mutableListOf<Map<String, Any?>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.createStatement().executeQuery("SELECT * FROM products ORDER BY id DESC")
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("name"),
                    "description" to rs.getString("description"),
                    "price" to rs.getDouble("price"),
                    "category" to rs.getString("category"),
                    "location" to rs.getString("location"),
                    "images" to (rs.getString("images")?.split(",")?.map { it.replace("\\", "/") } ?: emptyList<String>()),
                    "seller_id" to rs.getInt("seller_id")
                ))
            }
        }
        return list
    }

    fun getProduct(id: Int): Map<String, Any?>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT * FROM products WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) {
                return mapOf(
                    "id" to rs.getInt("id"),
                    "name" to rs.getString("name"),
                    "description" to rs.getString("description"),
                    "price" to rs.getDouble("price"),
                    "category" to rs.getString("category"),
                    "location" to rs.getString("location"),
                    "images" to (rs.getString("images")?.split(",")?.map { it.replace("\\", "/") } ?: emptyList<String>()),
                    "seller_id" to rs.getInt("seller_id")
                )
            }
        }
        return null
    }

    fun userExistsInAnyTable(email: String): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val s1 = conn.prepareStatement("SELECT 1 FROM drivers WHERE LOWER(TRIM(email)) = ?")
            s1.setString(1, email.lowercase())
            if (s1.executeQuery().next()) return true

            val s2 = conn.prepareStatement("SELECT 1 FROM customers WHERE LOWER(TRIM(email)) = ?")
            s2.setString(1, email.lowercase())
            if (s2.executeQuery().next()) return true

            val s3 = conn.prepareStatement("SELECT 1 FROM fleet_owners WHERE LOWER(TRIM(email)) = ?")
            s3.setString(1, email.lowercase())
            if (s3.executeQuery().next()) return true
        }
        return false
    }

    fun updateUserPassword(email: String, newPass: String) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val hashedPassword = BCrypt.hashpw(newPass.trim(), BCrypt.gensalt())
            val tables = listOf("drivers", "customers", "fleet_owners")
            tables.forEach { table ->
                val sql = "UPDATE $table SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE LOWER(TRIM(email)) = ?"
                conn.prepareStatement(sql).apply {
                    setString(1, hashedPassword)
                    setString(2, email.lowercase())
                    executeUpdate()
                }
            }
        }
    }

    fun terminateDriverAccount(id: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT firebase_uid FROM drivers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) {
                val uid = rs.getString("firebase_uid")
                if (!uid.isNullOrBlank()) {
                    try { FirebaseAuth.getInstance().deleteUser(uid) } catch (e: Exception) { println("Firebase Delete Error: ${e.message}") }
                }
            }
            conn.prepareStatement("DELETE FROM driver_stats WHERE driver_id = ?").apply { setInt(1, id); executeUpdate() }
            conn.prepareStatement("DELETE FROM drivers WHERE id = ?").apply { setInt(1, id); executeUpdate() }
        }
    }

    fun terminateFleetOwnerAccount(id: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT firebase_uid FROM fleet_owners WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) {
                val uid = rs.getString("firebase_uid")
                if (!uid.isNullOrBlank()) {
                    try { FirebaseAuth.getInstance().deleteUser(uid) } catch (e: Exception) { println("Firebase Delete Error: ${e.message}") }
                }
            }
            conn.prepareStatement("DELETE FROM fleet_owners WHERE id = ?").apply { setInt(1, id); executeUpdate() }
        }
    }

    fun terminateCustomerAccount(id: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT firebase_uid FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) {
                val uid = rs.getString("firebase_uid")
                if (!uid.isNullOrBlank()) {
                    try { FirebaseAuth.getInstance().deleteUser(uid) } catch (e: Exception) { println("Firebase Delete Error: ${e.message}") }
                }
            }
            conn.prepareStatement("DELETE FROM customers WHERE id = ?").apply { setInt(1, id); executeUpdate() }
        }
    }

    fun updateRentalDestination(rentalId: Int, location: String, lat: Double, lng: Double, stops: String?): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "UPDATE rentals SET destination_location = ?, destination_lat = ?, destination_lng = ?, stops = ? WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setString(1, location)
            stmt.setDouble(2, lat)
            stmt.setDouble(3, lng)
            stmt.setString(4, stops)
            stmt.setInt(5, rentalId)
            return stmt.executeUpdate() > 0
        }
    }

    fun updateRentalStatus(id: Int, status: String) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.autoCommit = false
            try {
                println("DEBUG: Updating Rental $id to status $status")
                conn.prepareStatement("UPDATE rentals SET status = ? WHERE id = ?").use { stmt ->
                    stmt.setString(1, status)
                    stmt.setInt(2, id)
                    stmt.executeUpdate()
                }

                if (status == "BOOKED") {
                    conn.prepareStatement("UPDATE rentals SET payment_status = 'PAID' WHERE id = ?").use { stmt ->
                        stmt.setInt(1, id)
                        stmt.executeUpdate()
                    }
                }

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
                        conn.prepareStatement("UPDATE rental_vehicles SET status = ?, is_available = ? WHERE id = ?").use { stmt ->
                            stmt.setString(1, vStatus)
                            stmt.setBoolean(2, isAvail)
                            stmt.setInt(3, vId)
                            stmt.executeUpdate()
                        }
                    }
                }
                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            }
        }
    }

    fun getActiveCustomerIdForDriver(driverId: Int): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT o.customer_id FROM deliveries d JOIN orders o ON d.order_id = o.id WHERE d.driver_id = ? AND d.status NOT IN ('DELIVERED', 'CANCELLED') LIMIT 1"
            val rs = conn.prepareStatement(sql).apply { setInt(1, driverId) }.executeQuery()
            if (rs.next()) return rs.getInt(1)
        }
        return null
    }

    fun getExpiredRentals(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT id, customer_id, vehicle_id, start_time, duration_hours 
                FROM rentals 
                WHERE status IN ('ACTIVE', 'IN_PROGRESS') 
                AND start_time IS NOT NULL
            """.trimIndent()
            val rs = conn.createStatement().executeQuery(sql)
            while (rs.next()) {
                list.add(mapOf(
                    "id" to rs.getInt("id"),
                    "customer_id" to rs.getInt("customer_id"),
                    "vehicle_id" to rs.getInt("vehicle_id"),
                    "start_time" to rs.getTimestamp("start_time"),
                    "duration_hours" to rs.getInt("duration_hours")
                ))
            }
        }
        return list
    }

    fun getTimedOutOrders(): List<Pair<Int, Int>> {
        val list = mutableListOf<Pair<Int, Int>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT id, customer_id 
                FROM orders 
                WHERE status = 'PENDING' 
                AND created_at < CURRENT_TIMESTAMP - interval '5 minutes'
            """.trimIndent()
            val rs = conn.createStatement().executeQuery(sql)
            while (rs.next()) {
                list.add(rs.getInt("id") to rs.getInt("customer_id"))
            }
        }
        return list
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

    fun getVehicleOwnerInfo(vehicleId: Int): Pair<Int, String>? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "SELECT owner_id, name FROM rental_vehicles WHERE id = ?"
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, vehicleId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return rs.getInt(1) to rs.getString(2)
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

    fun getCustomerProfilePic(id: Int): String? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT profile_picture FROM customers WHERE id = ?").apply { setInt(1, id) }.executeQuery()
            if (rs.next()) return rs.getString(1)
        }
        return null
    }

    fun getOrderStatusDetails(orderId: Int): OrderStatusResponse? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT o.status, o.verification_pin, o.total_amount, d.id as delivery_id, d.driver_id, dr.full_name, dr.phone, dr.vehicle_type, 
                       dr.vehicle_model, dr.vehicle_number, dr.profile_picture, dr.rating, 
                       ds.latitude, ds.longitude, ds.bearing
                FROM orders o
                LEFT JOIN deliveries d ON o.id = d.order_id
                LEFT JOIN drivers dr ON d.driver_id = dr.id
                LEFT JOIN driver_stats ds ON dr.id = ds.driver_id
                WHERE o.id = ?
            """.trimIndent()
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, orderId)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                return OrderStatusResponse(
                    success = true,
                    status = rs.getString("status"),
                    driverId = rs.getString("driver_id"),
                    driverName = rs.getString("full_name"),
                    driverPhone = rs.getString("phone"),
                    driverVehicle = rs.getString("vehicle_type"),
                    driverVehicleModel = rs.getString("vehicle_model"),
                    driverVehicleNumber = rs.getString("vehicle_number"),
                    driverProfilePic = rs.getString("profile_picture"),
                    driverRating = rs.getDouble("rating"),
                    driverLat = rs.getDouble("latitude"),
                    driverLng = rs.getDouble("longitude"),
                    driverBearing = rs.getFloat("bearing"),
                    verificationPin = rs.getString("verification_pin"),
                    deliveryId = rs.getString("delivery_id"),
                    fare = rs.getDouble("total_amount")
                )
            }
        }
        return null
    }

    fun cancelOrder(orderId: Int) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE id = ?").apply { setInt(1, orderId); executeUpdate() }
            conn.prepareStatement("UPDATE deliveries SET status = 'CANCELLED' WHERE order_id = ?").apply { setInt(1, orderId); executeUpdate() }
        }
    }

    fun verifyOrderPin(orderId: Int, pin: String): Boolean {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT verification_pin FROM orders WHERE id = ?").apply { setInt(1, orderId) }.executeQuery()
            if (rs.next() && rs.getString(1) == pin) {
                conn.prepareStatement("UPDATE orders SET status = 'IN_PROGRESS' WHERE id = ?").apply { setInt(1, orderId); executeUpdate() }
                conn.prepareStatement("UPDATE deliveries SET status = 'IN_PROGRESS' WHERE order_id = ?").apply { setInt(1, orderId); executeUpdate() }
                return true
            }
        }
        return false
    }

    fun getDriverIdForOrder(orderId: Int): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT driver_id FROM deliveries WHERE order_id = ?").apply { setInt(1, orderId) }.executeQuery()
            if (rs.next()) return rs.getInt(1)
        }
        return null
    }

    fun getCustomerIdForOrder(orderId: Int): Int? {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT customer_id FROM orders WHERE id = ?").apply { setInt(1, orderId) }.executeQuery()
            if (rs.next()) return rs.getInt(1)
        }
        return null
    }

    fun getReachedDeliveries(): List<Map<String, Any>> {
        val list = mutableListOf<Map<String, Any>>()
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = """
                SELECT d.id, d.order_id, d.driver_id, o.customer_id
                FROM deliveries d
                JOIN orders o ON d.order_id = o.id
                JOIN driver_stats ds ON d.driver_id = ds.driver_id
                WHERE d.status = 'IN_PROGRESS'
                AND d.dropoff_lat IS NOT NULL AND d.dropoff_lng IS NOT NULL
                AND ds.latitude IS NOT NULL AND ds.longitude IS NOT NULL
                AND (6371 * acos(least(1.0, cos(radians(d.dropoff_lat)) * cos(radians(ds.latitude)) * cos(radians(ds.longitude) - radians(d.dropoff_lng)) + sin(radians(d.dropoff_lat)) * sin(radians(ds.latitude))))) <= 0.15
            """.trimIndent()
            val rs = conn.createStatement().executeQuery(sql)
            while (rs.next()) {
                list.add(mapOf(
                    "deliveryId" to rs.getInt("id"),
                    "orderId" to rs.getInt("order_id"),
                    "driverId" to rs.getInt("driver_id"),
                    "customerId" to rs.getInt("customer_id")
                ))
            }
        }
        return list
    }

    fun getUserFcmToken(userId: Int, type: String): String? {
        val table = if (type == "customer") "customers" else "drivers"
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val rs = conn.prepareStatement("SELECT fcm_token FROM $table WHERE id = ?").apply { setInt(1, userId) }.executeQuery()
            if (rs.next()) return rs.getString(1)
        }
        return null
    }
}

data class RentalParticipants(val customerId: Int, val ownerId: Int, val vehicleName: String)
