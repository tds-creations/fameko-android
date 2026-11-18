package com.example.famekodriver.core.utils

import com.example.famekodriver.core.domain.model.ManeuverType
import com.example.famekodriver.core.domain.model.TomTomInstruction
import com.example.famekodriver.core.domain.model.TomTomPosition
import com.example.famekodriver.core.network.OsrmLeg
import com.example.famekodriver.core.network.OsrmManeuver
import com.example.famekodriver.core.network.OsrmStep
import org.junit.Assert.*
import org.junit.Test

class RouteInstructionParserTest {

    @Test
    fun testParseOsrmLegsStructuredInstructions() {
        val osrmSteps = listOf(
            OsrmStep(
                name = "Temple Street",
                distance = 150.0,
                duration = 20.0,
                maneuver = OsrmManeuver(
                    type = "depart",
                    modifier = "straight",
                    location = listOf(-0.1870, 5.6037)
                )
            ),
            OsrmStep(
                name = "Santa Maria Road",
                distance = 350.0,
                duration = 45.0,
                maneuver = OsrmManeuver(
                    type = "turn",
                    modifier = "left",
                    location = listOf(-0.1880, 5.6050)
                )
            ),
            OsrmStep(
                name = "Accra Bypass",
                distance = 600.0,
                duration = 70.0,
                maneuver = OsrmManeuver(
                    type = "roundabout",
                    modifier = "straight",
                    location = listOf(-0.1890, 5.6100),
                    exit = 2
                )
            )
        )

        val leg = OsrmLeg(steps = osrmSteps)
        val instructions = RouteInstructionParser.parseOsrmLegs(listOf(leg))

        assertEquals(3, instructions.size)
        assertEquals(ManeuverType.DEPART, instructions[0].maneuverType)
        assertEquals("Temple Street", instructions[0].streetName)

        assertEquals(ManeuverType.TURN_LEFT, instructions[1].maneuverType)
        assertEquals("Santa Maria Road", instructions[1].streetName)
        assertEquals("left", instructions[1].turnDirection)

        assertEquals(ManeuverType.ROUNDABOUT, instructions[2].maneuverType)
        assertEquals(2, instructions[2].roundaboutExit)
    }

    @Test
    fun testParseTomTomInstructions() {
        val tomTomInstructions = listOf(
            TomTomInstruction(
                point = TomTomPosition(lat = 5.6037, lon = -0.1870),
                instructionType = "LOCATION_DEPARTURE",
                message = "Head north on Temple Street",
                street = "Temple Street",
                pointIndex = 0,
                routeOffsetInMeters = 0
            ),
            TomTomInstruction(
                point = TomTomPosition(lat = 5.6080, lon = -0.1890),
                instructionType = "TURN_RIGHT",
                message = "Turn right onto Sowutuom Road",
                street = "Sowutuom Road",
                pointIndex = 12,
                routeOffsetInMeters = 450
            ),
            TomTomInstruction(
                point = TomTomPosition(lat = 5.6120, lon = -0.1910),
                instructionType = "MAKE_UTURN",
                message = "Make a U-turn on Nyamekye Road",
                street = "Nyamekye Road",
                pointIndex = 25,
                routeOffsetInMeters = 1100
            )
        )

        val instructions = RouteInstructionParser.parseTomTomInstructions(tomTomInstructions)

        assertEquals(3, instructions.size)
        assertEquals(ManeuverType.DEPART, instructions[0].maneuverType)
        assertEquals(ManeuverType.TURN_RIGHT, instructions[1].maneuverType)
        assertEquals("Sowutuom Road", instructions[1].streetName)
        assertEquals(ManeuverType.UTURN, instructions[2].maneuverType)
    }

    @Test
    fun testMapOsrmManeuverTypeModifiers() {
        assertEquals(ManeuverType.UTURN, RouteInstructionParser.mapOsrmManeuverType("turn", "uturn"))
        assertEquals(ManeuverType.SHARP_LEFT, RouteInstructionParser.mapOsrmManeuverType("turn", "sharp left"))
        assertEquals(ManeuverType.SLIGHT_RIGHT, RouteInstructionParser.mapOsrmManeuverType("turn", "slight right"))
        assertEquals(ManeuverType.ROUNDABOUT, RouteInstructionParser.mapOsrmManeuverType("roundabout", "right"))
    }
}
