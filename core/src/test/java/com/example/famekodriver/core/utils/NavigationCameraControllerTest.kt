package com.example.famekodriver.core.utils

import org.junit.Assert.*
import org.junit.Test

enum class CameraModeTest {
    FOLLOWING,
    SUSPENDED,
    OVERVIEW
}

class NavigationCameraControllerTest {

    class LatestTargetCameraQueue {
        var activeMode = CameraModeTest.FOLLOWING
        var isAnimating = false
        var pendingTarget: String? = null
        var executedTargets = mutableListOf<String>()

        fun onNewLocationReceived(target: String) {
            if (activeMode != CameraModeTest.FOLLOWING) return
            pendingTarget = target
            if (!isAnimating) {
                processNextTarget()
            }
        }

        private fun processNextTarget() {
            val targetToAnimate = pendingTarget ?: return
            pendingTarget = null
            if (activeMode != CameraModeTest.FOLLOWING) return

            isAnimating = true
            executedTargets.add(targetToAnimate)
        }

        fun onAnimationComplete() {
            isAnimating = false
            if (activeMode == CameraModeTest.FOLLOWING && pendingTarget != null) {
                processNextTarget()
            }
        }
    }

    @Test
    fun testLatestTargetWinsOverwritingIntermediateTargets() {
        val queue = LatestTargetCameraQueue()

        // 1st location fix
        queue.onNewLocationReceived("Target_1")
        assertTrue(queue.isAnimating)
        assertEquals(listOf("Target_1"), queue.executedTargets)
        assertNull(queue.pendingTarget)

        // 2nd and 3rd location fixes arrive while 1st animation is active
        queue.onNewLocationReceived("Target_2")
        assertEquals("Target_2", queue.pendingTarget)

        queue.onNewLocationReceived("Target_3") // Overwrites Target_2
        assertEquals("Target_3", queue.pendingTarget)

        // 1st animation completes
        queue.onAnimationComplete()

        // Should execute Target_3 next (skipping Target_2)
        assertEquals(listOf("Target_1", "Target_3"), queue.executedTargets)
        assertNull(queue.pendingTarget)
    }

    @Test
    fun testSuspendedModeBlocksPendingCameraTargetProcessing() {
        val queue = LatestTargetCameraQueue()

        queue.onNewLocationReceived("Target_1")
        queue.onNewLocationReceived("Target_2")

        // Driver pans map -> Mode changes to SUSPENDED
        queue.activeMode = CameraModeTest.SUSPENDED

        // Animation completes
        queue.onAnimationComplete()

        // Target_2 should NOT execute because mode is SUSPENDED
        assertEquals(listOf("Target_1"), queue.executedTargets)
    }

    @Test
    fun testSpeedAdaptiveZoomClamping() {
        fun calculateAdaptiveZoom(speedKmh: Double): Double {
            val validSpeed = maxOf(0.0, speedKmh)
            return (18.0 - (validSpeed / 100.0)).coerceIn(15.0, 18.0)
        }

        assertEquals(18.0, calculateAdaptiveZoom(0.0), 0.001)
        assertEquals(17.5, calculateAdaptiveZoom(50.0), 0.001)
        assertEquals(17.0, calculateAdaptiveZoom(100.0), 0.001)
        assertEquals(15.0, calculateAdaptiveZoom(350.0), 0.001) // Clamped at 15.0
        assertEquals(18.0, calculateAdaptiveZoom(-20.0), 0.001) // Clamped at 18.0
    }
}
