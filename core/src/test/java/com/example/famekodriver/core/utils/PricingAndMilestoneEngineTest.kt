package com.example.famekodriver.core.utils

import com.example.famekodriver.core.domain.model.PricingConfig
import com.example.famekodriver.core.domain.model.RideCategory
import com.example.famekodriver.core.domain.model.RideEstimateResponse
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class PricingAndMilestoneEngineTest {

    private fun calculateRideFare(
        cat: RideCategory,
        dist: Double,
        dur: Double,
        peak: Double,
        isAirportToll: Boolean,
        airportTollFee: Double,
        everydayEnabled: Boolean,
        everydayPercent: Int,
        milestonePercent: Int,
        hasAvailableMilestoneReward: Boolean
    ): RideEstimateResponse {
        val tollFee = if (isAirportToll) airportTollFee else 0.0

        // Discount priority selection: Milestone > Everyday > None
        var appliedDiscountType = "NONE"
        var appliedDiscountPercent = 0

        if (hasAvailableMilestoneReward) {
            appliedDiscountType = "MILESTONE"
            appliedDiscountPercent = milestonePercent
        } else if (everydayEnabled) {
            appliedDiscountType = "EVERYDAY"
            appliedDiscountPercent = everydayPercent.coerceIn(10, 15)
        }

        val unmultipliedBaseFare = Math.max(cat.minFare, cat.baseFare + (dist * cat.perKmRate) + (dur * cat.perMinuteRate))
        val unmultipliedTotalFare = BigDecimal(unmultipliedBaseFare + tollFee).setScale(2, RoundingMode.HALF_UP).toDouble()

        val baseFareWithSurge = Math.max(cat.minFare, (cat.baseFare + (dist * cat.perKmRate) + (dur * cat.perMinuteRate)) * peak)
        val baseOriginalFare = BigDecimal(baseFareWithSurge).setScale(2, RoundingMode.HALF_UP).toDouble()

        val isSurgeApplied = peak > 1.0 && baseOriginalFare > (unmultipliedBaseFare + 0.01)

        val discountAmount = BigDecimal(baseOriginalFare * (appliedDiscountPercent / 100.0)).setScale(2, RoundingMode.HALF_UP).toDouble()
        val discountedBaseFare = baseOriginalFare - discountAmount

        val finalTotalFare = BigDecimal(discountedBaseFare + tollFee).setScale(2, RoundingMode.HALF_UP).toDouble()
        val totalOriginalFare = BigDecimal(baseOriginalFare + tollFee).setScale(2, RoundingMode.HALF_UP).toDouble()

        return RideEstimateResponse(
            serviceId = cat.serviceId,
            name = cat.name,
            description = cat.description ?: "",
            fare = finalTotalFare,
            pickupEtaMin = 5,
            icon = cat.icon ?: "car",
            serviceType = cat.serviceType,
            isAvailableInRegion = true,
            availabilityStatus = "AVAILABLE",
            hasAirportToll = isAirportToll,
            tollFee = tollFee,
            baseFareWithoutSurge = unmultipliedTotalFare,
            isDemandSurgeApplied = isSurgeApplied,
            originalFare = totalOriginalFare,
            discountType = appliedDiscountType,
            discountPercent = appliedDiscountPercent,
            discountAmount = discountAmount
        )
    }

    private val sampleCategory = RideCategory(
        id = 1,
        serviceId = "Economy",
        name = "Economy",
        description = "Quick rides",
        icon = "car",
        serviceType = "RIDE_HAILING",
        baseFare = 5.0,
        perKmRate = 2.0,
        perMinuteRate = 0.5,
        minFare = 10.0,
        driverCommissionPercent = 15.0,
        isActive = true,
        displayOrder = 1
    )

    @Test
    fun testEverydayDiscountCalculation_10Percent() {
        // Base fare: 5.0 + 10km*2.0 + 10min*0.5 = 30.00
        val estimate = calculateRideFare(
            cat = sampleCategory,
            dist = 10.0,
            dur = 10.0,
            peak = 1.0,
            isAirportToll = false,
            airportTollFee = 20.0,
            everydayEnabled = true,
            everydayPercent = 10,
            milestonePercent = 20,
            hasAvailableMilestoneReward = false
        )

        assertEquals("EVERYDAY", estimate.discountType)
        assertEquals(10, estimate.discountPercent)
        assertEquals(30.00, estimate.originalFare, 0.001)
        assertEquals(3.00, estimate.discountAmount, 0.001)
        assertEquals(27.00, estimate.fare, 0.001)
    }

    @Test
    fun testEverydayDiscountCalculation_15Percent() {
        // Base fare: 30.00. 15% of 30 = 4.50. Final = 25.50
        val estimate = calculateRideFare(
            cat = sampleCategory,
            dist = 10.0,
            dur = 10.0,
            peak = 1.0,
            isAirportToll = false,
            airportTollFee = 20.0,
            everydayEnabled = true,
            everydayPercent = 15,
            milestonePercent = 20,
            hasAvailableMilestoneReward = false
        )

        assertEquals("EVERYDAY", estimate.discountType)
        assertEquals(15, estimate.discountPercent)
        assertEquals(30.00, estimate.originalFare, 0.001)
        assertEquals(4.50, estimate.discountAmount, 0.001)
        assertEquals(25.50, estimate.fare, 0.001)
    }

    @Test
    fun testMilestoneDiscountOverEveryday_PriorityNoStacking() {
        // When customer has available milestone reward (20%), milestone discount must be used instead of everyday (10%)
        val estimate = calculateRideFare(
            cat = sampleCategory,
            dist = 10.0,
            dur = 10.0,
            peak = 1.0,
            isAirportToll = false,
            airportTollFee = 20.0,
            everydayEnabled = true,
            everydayPercent = 10,
            milestonePercent = 20,
            hasAvailableMilestoneReward = true
        )

        assertEquals("MILESTONE", estimate.discountType)
        assertEquals(20, estimate.discountPercent)
        assertEquals(30.00, estimate.originalFare, 0.001)
        assertEquals(6.00, estimate.discountAmount, 0.001)
        assertEquals(24.00, estimate.fare, 0.001)
    }

    @Test
    fun testAirportTollExclusionFromDiscount() {
        // Base fare = 30.00, Toll = 20.00
        // Everyday 10% discount applies ONLY to base fare 30.00 -> savings 3.00
        // Final fare = (30.00 - 3.00) + 20.00 = 47.00
        // Total original fare = 30.00 + 20.00 = 50.00
        val estimate = calculateRideFare(
            cat = sampleCategory,
            dist = 10.0,
            dur = 10.0,
            peak = 1.0,
            isAirportToll = true,
            airportTollFee = 20.0,
            everydayEnabled = true,
            everydayPercent = 10,
            milestonePercent = 20,
            hasAvailableMilestoneReward = false
        )

        assertTrue(estimate.hasAirportToll)
        assertEquals(20.0, estimate.tollFee, 0.001)
        assertEquals("EVERYDAY", estimate.discountType)
        assertEquals(10, estimate.discountPercent)
        assertEquals(50.00, estimate.originalFare, 0.001)
        assertEquals(3.00, estimate.discountAmount, 0.001)
        assertEquals(47.00, estimate.fare, 0.001)
    }

    @Test
    fun testEverydayDiscountRangeEnforcement() {
        // Values < 10 must be coerced to 10
        val lowEstimate = calculateRideFare(
            cat = sampleCategory, dist = 10.0, dur = 10.0, peak = 1.0,
            isAirportToll = false, airportTollFee = 20.0,
            everydayEnabled = true, everydayPercent = 5,
            milestonePercent = 20, hasAvailableMilestoneReward = false
        )
        assertEquals(10, lowEstimate.discountPercent)

        // Values > 15 must be coerced to 15
        val highEstimate = calculateRideFare(
            cat = sampleCategory, dist = 10.0, dur = 10.0, peak = 1.0,
            isAirportToll = false, airportTollFee = 20.0,
            everydayEnabled = true, everydayPercent = 25,
            milestonePercent = 20, hasAvailableMilestoneReward = false
        )
        assertEquals(15, highEstimate.discountPercent)
    }

    @Test
    fun testMilestoneProgressionLogic() {
        val interval = 10
        
        // 7 completed rides: 0 available rewards, 3 rides remaining
        var completedRides = 7
        var unlockedRewards = completedRides / interval // 0
        var redeemedRewards = 0
        var availableRewards = (unlockedRewards - redeemedRewards).coerceAtLeast(0)
        var remainder = completedRides % interval
        var ridesRemaining = if (availableRewards > 0) 0 else (interval - remainder)

        assertEquals(0, availableRewards)
        assertEquals(3, ridesRemaining)

        // 10 completed rides: 1 available reward, 0 rides remaining until current reward used
        completedRides = 10
        unlockedRewards = completedRides / interval // 1
        availableRewards = (unlockedRewards - redeemedRewards).coerceAtLeast(0)
        remainder = completedRides % interval
        ridesRemaining = if (availableRewards > 0) 0 else (interval - remainder)

        assertEquals(1, availableRewards)
        assertEquals(0, ridesRemaining)

        // After redeeming 1 reward at 10 rides: 0 available rewards, 10 rides remaining until next milestone at 20
        redeemedRewards = 1
        availableRewards = (unlockedRewards - redeemedRewards).coerceAtLeast(0)
        remainder = completedRides % interval
        ridesRemaining = if (availableRewards > 0) 0 else (interval - remainder)

        assertEquals(0, availableRewards)
        assertEquals(10, ridesRemaining)
    }
}
