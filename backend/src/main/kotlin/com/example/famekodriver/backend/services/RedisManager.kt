package com.example.famekodriver.backend.services

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.params.SetParams

object RedisManager {
    private val pool: JedisPool by lazy {
        val host = System.getenv("REDIS_HOST") ?: "localhost"
        val port = System.getenv("REDIS_PORT")?.toInt() ?: 6379
        val password = System.getenv("REDIS_PASSWORD") ?: "A10cpdb1ap3ta6u54w97nk20biosdkauaazuhbsoz1srqhyoeky"
        
        val config = JedisPoolConfig().apply {
            maxTotal = 16
            maxIdle = 8
            minIdle = 2
        }
        
        if (!password.isNullOrBlank()) {
            JedisPool(config, host, port, 2000, password)
        } else {
            JedisPool(host, port)
        }
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
