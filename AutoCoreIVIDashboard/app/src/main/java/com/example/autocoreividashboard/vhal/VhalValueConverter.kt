package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey


object VhalValueConverter {

    fun convert(
        property: VehicleProperty<*>
    ): VhalPropertyValue {

        val mapping = VhalPropertyMappings.getMapping(property.key)

        val convertedValue: Any = when (property.key) {

            VehiclePropertyKey.SPEED -> {
                val speedKmh = property.value as Int
                speedKmh / 3.6f
            }

            VehiclePropertyKey.BATTERY_LEVEL -> {
                val percentage = property.value as Int
                percentage.coerceIn(0, 100).toFloat()
            }

            VehiclePropertyKey.IGNITION_STATE -> {
                if (property.value as Boolean) {
                    VhalEnumValues.IGNITION_ON
                } else {
                    VhalEnumValues.IGNITION_OFF
                }
            }

            VehiclePropertyKey.PARKING_BRAKE_ON -> {
                property.value as Boolean
            }

            VehiclePropertyKey.HEADLIGHT_STATE -> {
                if (property.value as Boolean) {
                    VhalEnumValues.LIGHT_ON
                } else {
                    VhalEnumValues.LIGHT_OFF
                }
            }

            VehiclePropertyKey.HEADLIGHT_BEAM -> {
                when (property.value as String) {
                    "HIGH" -> VhalEnumValues.LIGHT_ON
                    "LOW" -> VhalEnumValues.LIGHT_OFF
                    else -> throw IllegalArgumentException(
                        "Unsupported headlight beam: ${property.value}"
                    )
                }
            }

            VehiclePropertyKey.HVAC_STATE -> {
                property.value as Boolean
            }

            VehiclePropertyKey.CABIN_TEMPERATURE -> {
                (property.value as Int).toFloat()
            }

            VehiclePropertyKey.GEAR_SELECTION -> {
                when (property.value as String) {
                    "N" -> VhalEnumValues.GEAR_NEUTRAL
                    "R" -> VhalEnumValues.GEAR_REVERSE
                    "P" -> VhalEnumValues.GEAR_PARK
                    "D" -> VhalEnumValues.GEAR_DRIVE
                    else -> throw IllegalArgumentException(
                        "Unsupported gear: ${property.value}"
                    )
                }
            }

            VehiclePropertyKey.FRONT_LEFT_DOOR,
            VehiclePropertyKey.FRONT_RIGHT_DOOR,
            VehiclePropertyKey.REAR_LEFT_DOOR,
            VehiclePropertyKey.REAR_RIGHT_DOOR -> {
                if (property.value as Boolean) {
                    VhalEnumValues.DOOR_OPEN
                } else {
                    VhalEnumValues.DOOR_CLOSED
                }
            }
        }

        return VhalPropertyValue(
            propertyId = mapping.propertyId.id,
            areaId = mapping.areaId,
            value = convertedValue,
            timestampMillis = property.timestampMillis
        )
    }

    fun convertToVehicleProperty(
        property: VhalPropertyValue
    ): VehicleProperty<out Any> {

        val key = VhalPropertyMappings.getKey(
            property.propertyId,
            property.areaId
        )

        val convertedValue: Any = when (key) {

            VehiclePropertyKey.SPEED -> {
                ((property.value as Float) * 3.6f).toInt()
            }

            VehiclePropertyKey.BATTERY_LEVEL -> {
                (property.value as Float).toInt().coerceIn(0, 100)
            }

            VehiclePropertyKey.IGNITION_STATE -> {
                when (property.value as Int) {
                    VhalEnumValues.IGNITION_ON -> true
                    VhalEnumValues.IGNITION_OFF -> false
                    else -> throw IllegalArgumentException(
                        "Unsupported ignition value: ${property.value}"
                    )
                }
            }

            VehiclePropertyKey.PARKING_BRAKE_ON -> {
                property.value as Boolean
            }

            VehiclePropertyKey.HEADLIGHT_STATE -> {
                when (property.value as Int) {
                    VhalEnumValues.LIGHT_ON -> true
                    VhalEnumValues.LIGHT_OFF -> false
                    else -> throw IllegalArgumentException(
                        "Unsupported headlight value: ${property.value}"
                    )
                }
            }

            VehiclePropertyKey.HEADLIGHT_BEAM -> {
                when (property.value as Int) {
                    VhalEnumValues.LIGHT_ON -> "HIGH"
                    VhalEnumValues.LIGHT_OFF -> "LOW"
                    else -> throw IllegalArgumentException(
                        "Unsupported headlight beam: ${property.value}"
                    )
                }
            }

            VehiclePropertyKey.HVAC_STATE -> {
                property.value as Boolean
            }

            VehiclePropertyKey.CABIN_TEMPERATURE -> {
                (property.value as Float).toInt()
            }

            VehiclePropertyKey.GEAR_SELECTION -> {
                when (property.value as Int) {
                    VhalEnumValues.GEAR_PARK -> "P"
                    VhalEnumValues.GEAR_REVERSE -> "R"
                    VhalEnumValues.GEAR_NEUTRAL -> "N"
                    VhalEnumValues.GEAR_DRIVE -> "D"
                    else -> throw IllegalArgumentException(
                        "Unsupported gear value: ${property.value}"
                    )
                }
            }

            VehiclePropertyKey.FRONT_LEFT_DOOR,
            VehiclePropertyKey.FRONT_RIGHT_DOOR,
            VehiclePropertyKey.REAR_LEFT_DOOR,
            VehiclePropertyKey.REAR_RIGHT_DOOR -> {
                when (property.value as Int) {
                    VhalEnumValues.DOOR_OPEN -> true
                    VhalEnumValues.DOOR_CLOSED -> false
                    else -> throw IllegalArgumentException(
                        "Unsupported door position: ${property.value}"
                    )
                }
            }
        }

        return VehicleProperty(
            key = key,
            value = convertedValue,
            areaId = property.areaId,
            timestampMillis = property.timestampMillis
        )
    }
}
