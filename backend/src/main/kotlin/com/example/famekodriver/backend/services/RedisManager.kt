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
            minIdle = 2
            testOnBorrow = true
            testWhileIdle = true
        }

        println("Initializing Redis connection... (Url present: ${redisUrl != null})")

        try {
            if (!redisUrl.isNullOrBlank()) {
                // Remove potential double slashes if Railway provides it weirdly
                val cleanUrl = if (redisUrl.startsWith("redis://")) redisUrl else "redis://$redisUrl"
                JedisPool(config, java.net.URI(cleanUrl))
            } else if (!password.isNullOrBlank()) {
                JedisPool(config, host, port, 2000, password)
            } else {
                JedisPool(config, host, port)
            }
        } catch (e: Exception) {
            println("CRITICAL: Failed to initialize Redis Pool: ${e.message}")
            // Fallback to local if URI parsing fails
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
                // Store coordinates in Geo set for radius searches
                jedis.geoadd("active_drivers_geo", lng, lat, driverId)
                
                // Store detailed stats in a hash for quick retrieval
                val data = mapOf(
                    "lat" to lat.toString(),
                    "lng" to lng.toString(),
                    "bearing" to bearing.toString(),
                    "last_update" to System.currentTimeMillis().toString()
                )
                jedis.hmset("driver_stats:$driverId", data)
                
                // Set TTL for the geo entry? Jedis doesn't support TTL on individual members of a GEO set.
                // We'll manage cleanup via a background job or by checking last_update in hash.
                jedis.expire("driver_stats:$driverId", 300) // Keep detail for 5 mins
            }
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            println("Redis Driver Removal Error: ${e.message}")
        }
    }

    /**
     * Cache order data during dispatch to avoid repeated DB hits
     */
    fun cacheOrderData(orderId: Int, json: String) {
        set("dispatch_cache:$orderId", json, 300) // Cache for 5 mins
    }

    fun tryLockDriver(driverId: String, ttlSeconds: Long = 15): Boolean {
        return try {
            pool.resource.use { jedis ->
                val key = "driver_lock:$driverId"
                val params = SetParams().nx().ex(ttlSeconds)
                val result = jedis.set(key, "LOCKED", params)
                result == "OK"
            }
        } catch (e: Exception) {
            println("Redis Error: ${e.message}")
            // Fallback to true (allow) or false (block) depending on desired safety
            true 
        }
    }

    fun unlockDriver(driverId: String) {
        try {
            pool.resource.use { jedis ->
                jedis.del("driver_lock:$driverId")
            }
        } catch (e: Exception) {
            println("Redis Error: ${e.message}")
        }
    }

    // --- SOS MANAGEMENT (Temporary Storage) ---

    private val SOS_KEY = "active_sos_alerts"

    fun addSOS(alert: SOSAlert) {
        try {
            pool.resource.use { jedis ->
                val json = com.google.gson.Gson().toJson(alert)
                jedis.hset(SOS_KEY, alert.id.toString(), json)
                // SOS alerts expire after 24 hours if not resolved
                jedis.expire(SOS_KEY, 86400)
            }
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            emptyList<SOSAlert>()
        }
    }

    fun resolveSOS(id: Int) {
        try {
            pool.resource.use { jedis ->
                jedis.hdel(SOS_KEY, id.toString())
            }
        } catch (e: Exception) {
            println("Redis SOS Resolve Error: ${e.message}")
        }
    }

    fun getActiveSOSCount(): Int {
        return try {
            pool.resource.use { jedis ->
                jedis.hlen(SOS_KEY).toInt()
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Store OTP for login (5 mins TTL)
     */
    fun storeLoginOtp(phone: String, otp: String) {
        set("login_otp:$phone", otp, 300)
    }

    /**
     * Verify OTP for login
     */
    fun verifyLoginOtp(phone: String, otp: String): Boolean {
        val key = "login_otp:$phone"
        val stored = get(key)
        if (stored != null && stored == otp) {
            delete(key) // Consume OTP after successful verification
            return true
        }
        return false
    }

    /**
     * Store OTP for password reset (10 mins TTL)
     */
    fun storeResetOtp(email: String, otp: String) {
        set("reset_otp:$email", otp, 600)
    }

    /**
     * Verify OTP for password reset
     */
    fun verifyResetOtp(email: String, otp: String): Boolean {
        val stored = get("reset_otp:$email")
        return stored != null && stored == otp
    }
}
