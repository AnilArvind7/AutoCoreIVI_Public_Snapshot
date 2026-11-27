package com.example.autocorecarsimulator.data

object VehiclePropertyType {

    fun isValid(key: VehiclePropertyKey, value: Any): Boolean {
        return when (key) {

            VehiclePropertyKey.SPEED,
            VehiclePropertyKey.BATTERY_LEVEL,
            VehiclePropertyKey.CABIN_TEMPERATURE -> value is Int

            VehiclePropertyKey.IGNITION_STATE,
            VehiclePropertyKey.HEADLIGHT_STATE,
            VehiclePropertyKey.HVAC_STATE,
            VehiclePropertyKey.FRONT_LEFT_DOOR,
            VehiclePropertyKey.FRONT_RIGHT_DOOR,
            VehiclePropertyKey.REAR_LEFT_DOOR,
            VehiclePropertyKey.REAR_RIGHT_DOOR -> value is Boolean

            VehiclePropertyKey.HEADLIGHT_BEAM,
            VehiclePropertyKey.GEAR_SELECTION -> value is String
        }
    }
}
