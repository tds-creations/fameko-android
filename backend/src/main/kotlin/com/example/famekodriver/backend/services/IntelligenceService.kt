package com.example.famekodriver.backend.services

import com.example.famekodriver.backend.db.DatabaseRepository

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

        DatabaseRepository.saveSimulationRun("General Scenario", params, result)
        return result
    }

    fun getMovementHistory(limit: Int = 1000): List<Map<String, Any>> {
        return DatabaseRepository.getMovementHistory(limit)
    }

    fun getAnalyticsReport(): Map<String, Any> {
        return DatabaseRepository.getAnalyticsReport()
    }
}
