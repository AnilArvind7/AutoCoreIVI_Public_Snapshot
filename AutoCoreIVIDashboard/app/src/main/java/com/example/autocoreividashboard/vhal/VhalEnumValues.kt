package com.example.autocoreividashboard.vhal

object VhalEnumValues {

    // VehicleIgnitionState
    const val IGNITION_OFF = 2
    const val IGNITION_ON = 4

    // Simplified lighting simulation values.
    // Validate against individual VHAL property definitions.
    const val LIGHT_OFF = 0
    const val LIGHT_ON = 1

    // VehicleGear
    const val GEAR_NEUTRAL = 1
    const val GEAR_REVERSE = 2
    const val GEAR_PARK = 4
    const val GEAR_DRIVE = 8

    // Simulation-defined door positions.
    const val DOOR_CLOSED = 0
    const val DOOR_OPEN = 100
}
