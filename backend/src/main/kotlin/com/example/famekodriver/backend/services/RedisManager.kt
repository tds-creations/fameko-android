package com.example.famekodriver.backend.services

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.SetParams

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
}
