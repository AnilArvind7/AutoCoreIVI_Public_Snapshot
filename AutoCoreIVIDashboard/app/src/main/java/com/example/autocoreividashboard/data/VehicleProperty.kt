package com.example.autocoreividashboard.data

/**
 * Identifies individual vehicle properties.
 *
 * These are AutoCore internal identifiers,
 * not official Android VHAL property IDs.
 */
enum class VehiclePropertyKey {
    SPEED,
    BATTERY_LEVEL,
    IGNITION_STATE,
    HEADLIGHT_STATE,
    HEADLIGHT_BEAM,
    HVAC_STATE,
    CABIN_TEMPERATURE,
    GEAR_SELECTION,
    FRONT_LEFT_DOOR,
    FRONT_RIGHT_DOOR,
    REAR_LEFT_DOOR,
    REAR_RIGHT_DOOR,
    PARKING_BRAKE_ON
}

/**
 * Represents an individual vehicle property.
 */
data class VehicleProperty<T>(
    val key: VehiclePropertyKey,
    val value: T,
    val areaId: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis()
)
