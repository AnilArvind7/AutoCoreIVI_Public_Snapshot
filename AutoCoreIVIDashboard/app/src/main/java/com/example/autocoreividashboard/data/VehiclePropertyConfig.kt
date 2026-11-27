package com.example.autocoreividashboard.data

object VehiclePropertyConfig {

    const val DEFAULT_AREA_ID = 0

    val initialProperties = listOf(
        VehicleProperty(VehiclePropertyKey.SPEED, 0),
        VehicleProperty(VehiclePropertyKey.BATTERY_LEVEL, 100),
        VehicleProperty(VehiclePropertyKey.IGNITION_STATE, false),
        VehicleProperty(VehiclePropertyKey.PARKING_BRAKE_ON, false),
        VehicleProperty(VehiclePropertyKey.HEADLIGHT_STATE, false),
        VehicleProperty(VehiclePropertyKey.HEADLIGHT_BEAM, "LOW"),
        VehicleProperty(VehiclePropertyKey.HVAC_STATE, false),
        VehicleProperty(VehiclePropertyKey.CABIN_TEMPERATURE, 22),
        VehicleProperty(VehiclePropertyKey.GEAR_SELECTION, "P"),
        VehicleProperty(VehiclePropertyKey.FRONT_LEFT_DOOR, false),
        VehicleProperty(VehiclePropertyKey.FRONT_RIGHT_DOOR, false),
        VehicleProperty(VehiclePropertyKey.REAR_LEFT_DOOR, false),
        VehicleProperty(VehiclePropertyKey.REAR_RIGHT_DOOR, false)
    )
}
