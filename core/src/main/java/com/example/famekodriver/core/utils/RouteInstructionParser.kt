package com.example.famekodriver.core.utils

import com.example.famekodriver.core.domain.model.ManeuverType
import com.example.famekodriver.core.domain.model.RouteInstruction
import com.example.famekodriver.core.domain.model.TomTomInstruction
import com.example.famekodriver.core.network.OsrmLeg
import java.util.Locale

object RouteInstructionParser {

    fun parseOsrmLegs(legs: List<OsrmLeg>?): List<RouteInstruction> {
        if (legs.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<RouteInstruction>()

        for (leg in legs) {
            val steps = leg.steps ?: continue
            for (step in steps) {
                val maneuver = step.maneuver ?: continue
                val loc = maneuver.location ?: continue
                if (loc.size < 2) continue

                val typeStr = maneuver.type ?: ""
                val modifierStr = maneuver.modifier ?: ""
                val street = step.name?.takeIf { it.isNotBlank() && !it.equals("undefined", true) } ?: ""

                val maneuverType = mapOsrmManeuverType(typeStr, modifierStr)
                val text = maneuver.instruction?.takeIf { it.isNotBlank() }
                    ?: buildOsrmText(typeStr, modifierStr, street, maneuver.exit)

                result.add(
                    RouteInstruction(
                        text = text,
                        distance = step.distance ?: 0.0,
                        duration = step.duration ?: 0.0,
                        point = loc,
                        streetName = street,
                        maneuverType = maneuverType,
                        turnDirection = modifierStr,
                        bearingBefore = maneuver.bearingBefore?.toFloat(),
                        bearingAfter = maneuver.bearingAfter?.toFloat(),
                        roundaboutExit = maneuver.exit
                    )
                )
            }
        }
        return result
    }

    fun parseTomTomInstructions(instructions: List<TomTomInstruction>?): List<RouteInstruction> {
        if (instructions.isNullOrEmpty()) return emptyList()
        val result = mutableListOf<RouteInstruction>()

        for (inst in instructions) {
            val pt = inst.point ?: continue
            val lat = pt.lat ?: continue
            val lon = pt.lon ?: continue

            val typeStr = inst.instructionType ?: ""
            val street = inst.street?.takeIf { it.isNotBlank() } ?: ""
            val maneuverType = mapTomTomInstructionType(typeStr)

            val text = inst.message?.takeIf { it.isNotBlank() }
                ?: buildTomTomText(typeStr, street, inst.possibleNumber)

            val exitNum = inst.possibleNumber?.toIntOrNull()

            result.add(
                RouteInstruction(
                    text = text,
                    distance = (inst.routeOffsetInMeters ?: 0).toDouble(),
                    duration = (inst.travelTimeInSeconds ?: 0).toDouble(),
                    point = listOf(lon, lat),
                    streetName = street,
                    maneuverType = maneuverType,
                    turnDirection = typeStr,
                    roundaboutExit = exitNum
                )
            )
        }
        return result
    }

    fun mapOsrmManeuverType(type: String, modifier: String): ManeuverType {
        val t = type.lowercase(Locale.US)
        val m = modifier.lowercase(Locale.US)

        return when {
            t == "depart" -> ManeuverType.DEPART
            t == "arrive" -> ManeuverType.ARRIVE
            t.contains("roundabout") || t.contains("rotary") -> ManeuverType.ROUNDABOUT
            m.contains("uturn") -> ManeuverType.UTURN
            m.contains("sharp left") -> ManeuverType.SHARP_LEFT
            m.contains("sharp right") -> ManeuverType.SHARP_RIGHT
            m.contains("slight left") -> ManeuverType.SLIGHT_LEFT
            m.contains("slight right") -> ManeuverType.SLIGHT_RIGHT
            m.contains("left") -> ManeuverType.TURN_LEFT
            m.contains("right") -> ManeuverType.TURN_RIGHT
            t.contains("ramp") -> if (m.contains("left")) ManeuverType.RAMP_LEFT else ManeuverType.RAMP_RIGHT
            t.contains("merge") -> ManeuverType.MERGE
            m.contains("straight") -> ManeuverType.STRAIGHT
            else -> ManeuverType.STRAIGHT
        }
    }

    fun mapTomTomInstructionType(type: String): ManeuverType {
        val t = type.uppercase(Locale.US)

        return when {
            t.contains("DEPARTURE") -> ManeuverType.DEPART
            t.contains("ARRIVAL") -> ManeuverType.ARRIVE
            t.contains("ROUNDABOUT") -> ManeuverType.ROUNDABOUT
            t.contains("UTURN") -> ManeuverType.UTURN
            t.contains("SHARP_LEFT") -> ManeuverType.SHARP_LEFT
            t.contains("SHARP_RIGHT") -> ManeuverType.SHARP_RIGHT
            t.contains("BEAR_LEFT") || t.contains("SLIGHT_LEFT") -> ManeuverType.SLIGHT_LEFT
            t.contains("BEAR_RIGHT") || t.contains("SLIGHT_RIGHT") -> ManeuverType.SLIGHT_RIGHT
            t.contains("LEFT") -> ManeuverType.TURN_LEFT
            t.contains("RIGHT") -> ManeuverType.TURN_RIGHT
            t.contains("STRAIGHT") -> ManeuverType.STRAIGHT
            else -> ManeuverType.STRAIGHT
        }
    }

    private fun buildOsrmText(type: String, modifier: String, street: String, exit: Int?): String {
        val streetText = if (street.isNotBlank()) " onto $street" else ""
        val mType = mapOsrmManeuverType(type, modifier)

        return when (mType) {
            ManeuverType.DEPART -> "Start navigation$streetText"
            ManeuverType.ARRIVE -> "You have arrived at your destination"
            ManeuverType.TURN_LEFT -> "Turn left$streetText"
            ManeuverType.TURN_RIGHT -> "Turn right$streetText"
            ManeuverType.SLIGHT_LEFT -> "Keep left$streetText"
            ManeuverType.SLIGHT_RIGHT -> "Keep right$streetText"
            ManeuverType.SHARP_LEFT -> "Sharp left turn$streetText"
            ManeuverType.SHARP_RIGHT -> "Sharp right turn$streetText"
            ManeuverType.UTURN -> "Make a U-turn$streetText"
            ManeuverType.ROUNDABOUT -> if (exit != null) "At the roundabout, take exit $exit" else "Enter the roundabout"
            else -> "Continue straight$streetText"
        }
    }

    private fun buildTomTomText(type: String, street: String, exit: String?): String {
        val streetText = if (street.isNotBlank()) " onto $street" else ""
        val mType = mapTomTomInstructionType(type)

        return when (mType) {
            ManeuverType.DEPART -> "Start navigation$streetText"
            ManeuverType.ARRIVE -> "You have arrived at your destination"
            ManeuverType.TURN_LEFT -> "Turn left$streetText"
            ManeuverType.TURN_RIGHT -> "Turn right$streetText"
            ManeuverType.SLIGHT_LEFT -> "Keep left$streetText"
            ManeuverType.SLIGHT_RIGHT -> "Keep right$streetText"
            ManeuverType.SHARP_LEFT -> "Sharp left turn$streetText"
            ManeuverType.SHARP_RIGHT -> "Sharp right turn$streetText"
            ManeuverType.UTURN -> "Make a U-turn$streetText"
            ManeuverType.ROUNDABOUT -> if (exit != null) "At the roundabout, take exit $exit" else "Enter the roundabout"
            else -> "Continue straight$streetText"
        }
    }
}
