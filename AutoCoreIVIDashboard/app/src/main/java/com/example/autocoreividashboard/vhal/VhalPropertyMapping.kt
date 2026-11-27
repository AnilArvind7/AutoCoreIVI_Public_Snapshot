package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehiclePropertyKey
import android.car.VehiclePropertyIds

data class VhalPropertyMapping(
    val propertyId: VhalPropertyId,
    val areaId: Int = VhalAreaId.GLOBAL
)

object VhalPropertyMappings {

    fun getMapping(key: VehiclePropertyKey): VhalPropertyMapping {
        return when (key) {

            VehiclePropertyKey.SPEED ->
                VhalPropertyMapping(VhalPropertyId.PERF_VEHICLE_SPEED)

            VehiclePropertyKey.BATTERY_LEVEL ->
                VhalPropertyMapping(VhalPropertyId.EV_BATTERY_LEVEL)

            VehiclePropertyKey.IGNITION_STATE ->
                VhalPropertyMapping(VhalPropertyId.IGNITION_STATE)

            VehiclePropertyKey.PARKING_BRAKE_ON ->
                VhalPropertyMapping(VhalPropertyId.PARKING_BRAKE_ON)

            VehiclePropertyKey.HEADLIGHT_STATE ->
                VhalPropertyMapping(VhalPropertyId.HEADLIGHTS_STATE)

            VehiclePropertyKey.HEADLIGHT_BEAM ->
                VhalPropertyMapping(VhalPropertyId.HIGH_BEAM_LIGHTS_STATE)

            VehiclePropertyKey.HVAC_STATE ->
                VhalPropertyMapping(
                    VhalPropertyId.HVAC_POWER_ON,
                    VhalHvacAreaId.ROW_1_LEFT.id
                )

            VehiclePropertyKey.CABIN_TEMPERATURE ->
                VhalPropertyMapping(
                    VhalPropertyId.HVAC_TEMPERATURE_SET,
                    VhalHvacAreaId.ROW_1_LEFT.id
                )

            VehiclePropertyKey.GEAR_SELECTION ->
                VhalPropertyMapping(VhalPropertyId.GEAR_SELECTION)

            VehiclePropertyKey.FRONT_LEFT_DOOR ->
                VhalPropertyMapping(
                    VhalPropertyId.DOOR_POS,
                    VhalDoorAreaId.ROW_1_LEFT.id
                )

            VehiclePropertyKey.FRONT_RIGHT_DOOR ->
                VhalPropertyMapping(
                    VhalPropertyId.DOOR_POS,
                    VhalDoorAreaId.ROW_1_RIGHT.id
                )

            VehiclePropertyKey.REAR_LEFT_DOOR ->
                VhalPropertyMapping(
                    VhalPropertyId.DOOR_POS,
                    VhalDoorAreaId.ROW_2_LEFT.id
                )

            VehiclePropertyKey.REAR_RIGHT_DOOR ->
                VhalPropertyMapping(
                    VhalPropertyId.DOOR_POS,
                    VhalDoorAreaId.ROW_2_RIGHT.id
                )
        }
    }

    fun getKey(
        propertyId: Int,
        areaId: Int
    ): VehiclePropertyKey {

        return VehiclePropertyKey.values().firstOrNull { key ->
            val mapping = getMapping(key)

            mapping.propertyId.id == propertyId &&
                    mapping.areaId == areaId
        } ?: throw IllegalArgumentException(
            "No mapping found for propertyId=$propertyId, areaId=$areaId"
        )
    }
}
