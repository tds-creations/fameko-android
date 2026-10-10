package com.example.famekodriver.backend.services

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.SetParams
import com.example.famekodriver.core.domain.model.SOSAlert

object RedisManager {
    private val pool: JedisPool by lazy {
        val redisUrl = System.getenv("REDIS_URL") ?: System.getenv("REDIS_PUBLIC_URL")
        val host = System.getenv("REDISHOST") ?: System.getenv("REDIS_HOST") ?: "localhost"
        val port = (System.getenv("REDISPORT") ?: System.getenv("REDIS_PORT"))?.toInt() ?: 6379
        val password = System.getenv("REDISPASSWORD") ?: System.getenv("REDIS_PASSWORD")
        
        val config = JedisPoolConfig().apply {
            maxTotal = 16
            maxIdle = 8
            minIdle = 1
            testOnBorrow = false
            testWhileIdle = false
            blockWhenExhausted = false
        }

        println("Initializing Redis connection... (Url present: ${redisUrl != null})")

        try {
            if (!redisUrl.isNullOrBlank()) {
                val cleanUrl = if (redisUrl.startsWith("redis://")) redisUrl else "redis://$redisUrl"
                JedisPool(config, java.net.URI(cleanUrl), 2000)
            } else if (!password.isNullOrBlank()) {
                JedisPool(config, host, port, 2000, password)
            } else {
                JedisPool(config, host, port, 2000)
            }
        } catch (e: Throwable) {
            println("CRITICAL: Failed to initialize Redis Pool: ${e.message}")
            JedisPool(config, "localhost", 6379)
        }
    }

    /**
     * Set a value with an optional TTL (Time To Live)
     */
    fun set(key: String, value: String, ttlSeconds: Long? = null) {
        try {
            pool.resource.use { jedis ->
                if (ttlSeconds != null) {
                    jedis.setex(key, ttlSeconds, value)
                } else {
                    jedis.set(key, value)
                }
            }
        } catch (e: Throwable) {
            println("Redis Set Error: ${e.message}")
        }
    }

    /**
     * Get a value by key
     */
    fun get(key: String): String? {
        return try {
            pool.resource.use { jedis ->
                jedis.get(key)
            }
        } catch (e: Throwable) {
            println("Redis Get Error: ${e.message}")
            null
        }
    }

    /**
     * Delete a key
     */
    fun delete(key: String) {
        try {
            pool.resource.use { jedis ->
                jedis.del(key)
            }
        } catch (e: Throwable) {
            println("Redis Delete Error: ${e.message}")
        }
    }

    // --- TEMPORARY DATABASE FEATURES ---

    /**
     * Store active driver location for real-time tracking
     */
    fun updateDriverLocation(driverId: String, lat: Double, lng: Double, bearing: Float = 0f) {
        try {
            pool.resource.use { jedis ->
                jedis.geoadd("active_drivers_geo", lng, lat, driverId)
                val data = mapOf(
                    "lat" to lat.toString(),
                    "lng" to lng.toString(),
                    "bearing" to bearing.toString(),
                    "last_update" to System.currentTimeMillis().toString()
                )
                jedis.hmset("driver_stats:$driverId", data)
                jedis.expire("driver_stats:$driverId", 300)
            }
        } catch (e: Throwable) {
            println("Redis Location Update Error: ${e.message}")
        }
    }

    /**
     * Get nearby driver IDs using Redis Geospatial search
     */
    fun getNearbyDrivers(lat: Double, lng: Double, radiusKm: Double): List<String> {
        return try {
            pool.resource.use { jedis ->
                val results = jedis.georadius("active_drivers_geo", lng, lat, radiusKm, redis.clients.jedis.args.GeoUnit.KM)
                results.map { it.memberByString }
            }
        } catch (e: Throwable) {
            println("Redis GeoRadius Error: ${e.message}")
            emptyList()
        }
    }

    /**
     * Remove a driver from the active tracking set
     */
    fun removeDriverLocation(driverId: String) {
        try {
            pool.resource.use { jedis ->
                jedis.zrem("active_drivers_geo", driverId)
                jedis.del("driver_stats:$driverId")
            }
        } catch (e: Throwable) {
            println("Redis Driver Removal Error: ${e.message}")
        }
    }

    /**
     * Cache order data during dispatch to avoid repeated DB hits
     */
    fun cacheOrderData(orderId: Int, json: String) {
        set("dispatch_cache:$orderId", json, 300)
    }

    fun tryLockDriver(driverId: String, ttlSeconds: Long = 15): Boolean {
        return try {
            pool.resource.use { jedis ->
                val key = "driver_lock:$driverId"
                val params = SetParams().nx().ex(ttlSeconds)
                val result = jedis.set(key, "LOCKED", params)
                result == "OK"
            }
        } catch (e: Throwable) {
            println("Redis Error: ${e.message}")
            true 
        }
    }

    fun unlockDriver(driverId: String) {
        try {
            pool.resource.use { jedis ->
                jedis.del("driver_lock:$driverId")
            }
        } catch (e: Throwable) {
            println("Redis Error: ${e.message}")
        }
    }

    // --- SOS MANAGEMENT ---

    private val SOS_KEY = "active_sos_alerts"

    fun addSOS(alert: SOSAlert) {
        try {
            pool.resource.use { jedis ->
                val json = com.google.gson.Gson().toJson(alert)
                jedis.hset(SOS_KEY, alert.id.toString(), json)
                jedis.expire(SOS_KEY, 86400)
            }
        } catch (e: Throwable) {
            println("Redis SOS Add Error: ${e.message}")
        }
    }

    fun getAllSOS(): List<SOSAlert> {
        return try {
            pool.resource.use { jedis ->
                jedis.hgetAll(SOS_KEY).values.map { 
                    com.google.gson.Gson().fromJson(it, SOSAlert::class.java)
                }
            }
        } catch (e: Throwable) {
            emptyList<SOSAlert>()
        }
    }

    fun resolveSOS(id: Int) {
        try {
            pool.resource.use { jedis ->
                jedis.hdel(SOS_KEY, id.toString())
            }
        } catch (e: Throwable) {
            println("Redis SOS Resolve Error: ${e.message}")
        }
    }

    fun getActiveSOSCount(): Int {
        return try {
            pool.resource.use { jedis ->
                jedis.hlen(SOS_KEY).toInt()
            }
        } catch (e: Throwable) {
            0
        }
    }

    fun storeLoginOtp(phone: String, otp: String) {
        set("login_otp:$phone", otp, 300)
    }

    fun verifyLoginOtp(phone: String, otp: String): Boolean {
        val key = "login_otp:$phone"
        val stored = get(key)
        if (stored != null && stored == otp) {
            delete(key)
            return true
        }
        return false
    }

    fun storeResetOtp(email: String, otp: String) {
        set("reset_otp:$email", otp, 600)
    }

    fun verifyResetOtp(email: String, otp: String): Boolean {
        val stored = get("reset_otp:$email")
        return stored != null && stored == otp
    }

    // --- CHAT STORAGE ---

    fun saveChatMessage(convId: Int, messageJson: String) {
        try {
            pool.resource.use { jedis ->
                val key = "chat:$convId"
                jedis.rpush(key, messageJson)
                jedis.expire(key, 43200) 
            }
        } catch (e: Throwable) {
            println("Redis Chat Save Error: ${e.message}")
        }
    }

    fun getChatHistory(convId: Int): List<String> {
        return try {
            pool.resource.use { it.lrange("chat:$convId", 0, -1) }
        } catch (e: Throwable) {
            println("Redis Chat Fetch Error: ${e.message}")
            emptyList()
        }
    }

    fun getActiveConversationIds(): List<Int> {
        return try {
            pool.resource.use { jedis ->
                val keys = jedis.keys("chat:*")
                keys.map { it.removePrefix("chat:").toInt() }
            }
        } catch (e: Throwable) {
            println("Redis Keys Error: ${e.message}")
            emptyList()
        }
    }

    fun setChatRetention(convId: Int) {
        try {
            pool.resource.use { it.expire("chat:$convId", 86400) }
        } catch (e: Throwable) {
            println("Redis Chat TTL Error: ${e.message}")
        }
    }

    fun recordDriverEarningsToday(driverId: String, earnings: Double) {
        try {
            val todayDate = java.time.LocalDate.now().toString()
            val earningsKey = "driver_today_earnings:$driverId:$todayDate"
            val tripsKey = "driver_today_trips:$driverId:$todayDate"
            pool.resource.use { jedis ->
                jedis.incrByFloat(earningsKey, earnings)
                jedis.expire(earningsKey, 172800)
                jedis.incr(tripsKey)
                jedis.expire(tripsKey, 172800)
            }
        } catch (e: Throwable) {
            println("Redis Record Driver Earnings Error: ${e.message}")
        }
    }

    fun getDriverEarningsToday(driverId: String): Pair<Double, Int>? {
        return try {
            val todayDate = java.time.LocalDate.now().toString()
            val earningsKey = "driver_today_earnings:$driverId:$todayDate"
            val tripsKey = "driver_today_trips:$driverId:$todayDate"
            pool.resource.use { jedis ->
                val earningsStr = jedis.get(earningsKey)
                val tripsStr = jedis.get(tripsKey)
                if (earningsStr != null || tripsStr != null) {
                    val earnings = earningsStr?.toDoubleOrNull() ?: 0.0
                    val trips = tripsStr?.toIntOrNull() ?: 0
                    Pair(earnings, trips)
                } else null
            }
        } catch (e: Throwable) {
            println("Redis Get Driver Earnings Error: ${e.message}")
            null
        }
    }
}
