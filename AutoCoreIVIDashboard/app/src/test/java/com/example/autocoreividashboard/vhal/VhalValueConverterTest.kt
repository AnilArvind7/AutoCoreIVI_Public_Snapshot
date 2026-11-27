package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey
import org.junit.Assert.assertEquals
import org.junit.Test

class VhalValueConverterTest {

    @Test
    fun `gear selection converts to VHAL enum values`() {
        val cases = listOf(
            "P" to VhalEnumValues.GEAR_PARK,
            "R" to VhalEnumValues.GEAR_REVERSE,
            "N" to VhalEnumValues.GEAR_NEUTRAL,
            "D" to VhalEnumValues.GEAR_DRIVE
        )

        cases.forEach { (gear, expected) ->
            val result = VhalValueConverter.convert(
                VehicleProperty(
                    key = VehiclePropertyKey.GEAR_SELECTION,
                    value = gear
                )
            )

            assertEquals(expected, result.value)
        }
    }

    @Test
    fun `parking brake Boolean converts without changing value`() {
        val result = VhalValueConverter.convert(
            VehicleProperty(
                key = VehiclePropertyKey.PARKING_BRAKE_ON,
                value = true
            )
        )

        assertEquals(true, result.value)
    }

    @Test
    fun `door state converts to VHAL enum values`() {
        val openResult = VhalValueConverter.convert(
            VehicleProperty(
                key = VehiclePropertyKey.FRONT_LEFT_DOOR,
                value = true
            )
        )

        val closedResult = VhalValueConverter.convert(
            VehicleProperty(
                key = VehiclePropertyKey.FRONT_LEFT_DOOR,
                value = false
            )
        )

        assertEquals(VhalEnumValues.DOOR_OPEN, openResult.value)
        assertEquals(VhalEnumValues.DOOR_CLOSED, closedResult.value)
    }

    @Test
    fun `HVAC temperature converts from app integer to VHAL float`() {
        val result = VhalValueConverter.convert(
            VehicleProperty(
                key = VehiclePropertyKey.CABIN_TEMPERATURE,
                value = 22
            )
        )

        assertEquals(22f, result.value)
    }

    @Test
    fun `HVAC temperature converts from VHAL float to app integer`() {
        val result = VhalValueConverter.convertToVehicleProperty(
            VhalPropertyValue(
                propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
                areaId = VhalHvacAreaId.ROW_1_LEFT.id,
                value = 22.5f
            )
        )

        assertEquals(VehiclePropertyKey.CABIN_TEMPERATURE, result.key)
        assertEquals(22, result.value)
    }
}
