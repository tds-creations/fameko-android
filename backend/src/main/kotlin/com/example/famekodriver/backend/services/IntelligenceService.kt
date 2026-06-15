package com.example.famekodriver.backend.services

import com.example.famekodriver.backend.db.DatabaseInitializer
import com.google.gson.Gson
import java.sql.Connection

data class SimulationParams(
    val driverCount: Int,
    val customerCount: Int,
    val weather: String, // CLEAR, RAIN, STORM
    val timeOfDay: String, // MORNING, AFTERNOON, EVENING, NIGHT
    val region: String? = null
)

data class SimulationResult(
    val estimatedRevenue: Double,
    val averageETA: Double,
    val bottleneckAreas: List<String>,
    val demandCoverage: Double,
    val recommendedSurge: Double
)

object IntelligenceService {
    private val gson = Gson()

    fun runSimulation(params: SimulationParams): SimulationResult {
        // Base values
        var revenue = params.customerCount * 25.0
        var eta = 5.0
        var coverage = (params.driverCount.toDouble() / params.customerCount.toDouble()).coerceAtMost(1.0) * 100.0
        var surge = 1.0

        // Weather impact
        when (params.weather) {
            "RAIN" -> {
                eta *= 1.4
                surge = 1.2
                revenue *= 1.1 // Surge increases revenue per trip
            }
            "STORM" -> {
                eta *= 2.0
                surge = 1.5
                revenue *= 1.2
                coverage *= 0.7 // Fewer drivers willing to work
            }
        }

        // Time of day impact
        when (params.timeOfDay) {
            "MORNING", "EVENING" -> {
                eta *= 1.2
                revenue *= 1.2 // High demand
            }
        }

        // Mock bottleneck areas based on region
        val bottlenecks = if (coverage < 80) {
            listOf("Downtown", "Airport Road", "East Legon")
        } else {
            emptyList()
        }

        val result = SimulationResult(
            estimatedRevenue = revenue,
            averageETA = eta,
            bottleneckAreas = bottlenecks,
            demandCoverage = coverage,
            recommendedSurge = surge
        )

        saveSimulationRun("General Scenario", params, result)
        return result
    }

    private fun saveSimulationRun(name: String, params: SimulationParams, result: SimulationResult) {
        DatabaseInitializer.getDataSource().connection.use { conn ->
            val sql = "INSERT INTO simulation_runs (scenario_name, parameters, results) VALUES (?, ?, ?)"
            conn.prepareStatement(sql).apply {
                setString(1, name)
                setString(2, gson.toJson(params))
                setString(3, gson.toJson(result))
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
            // Total logs
            val rsCount = conn.createStatement().executeQuery("SELECT COUNT(*) FROM movement_logs")
            if (rsCount.next()) stats["totalLogs"] = rsCount.getInt(1)

            // Busy hours
            val rsHours = conn.createStatement().executeQuery(
                "SELECT EXTRACT(HOUR FROM created_at) as hour, COUNT(*) as count FROM movement_logs GROUP BY hour ORDER BY hour ASC"
            )
            val busyHours = mutableListOf<Map<String, Any>>()
            while (rsHours.next()) {
                busyHours.add(mapOf("hour" to rsHours.getInt("hour"), "count" to rsHours.getInt("count")))
            }
            stats["busyHours"] = busyHours

            // Revenue trend (last 7 days)
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

            // Entity distribution
            val rsDist = conn.createStatement().executeQuery("SELECT entity_type, COUNT(*) as count FROM movement_logs GROUP BY entity_type")
            val distribution = mutableMapOf<String, Int>()
            while (rsDist.next()) {
                distribution[rsDist.getString("entity_type")] = rsDist.getInt("count")
            }
            stats["distribution"] = distribution
        }
        return stats
    }
}
