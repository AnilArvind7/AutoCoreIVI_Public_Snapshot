package com.example.autocoreividashboard.vhal

import org.junit.Test
import org.junit.Assert.assertThrows
import org.junit.Assert.assertEquals
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyStore

class MockVhalImplTest {

    private val mockVhal = MockVhalImpl()

    @Test
    fun `writing to read only property should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.PERF_VEHICLE_SPEED.id,
                areaId = VhalAreaId.GLOBAL,
                value = 10f
            )
        }
    }

    @Test
    fun `writing to read write HVAC property should succeed`() {

        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id,
            value = true
        )

        val result = mockVhal.getProperty(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id
        )

        assertEquals(true, result?.value)
    }

    @Test
    fun `writing with invalid area ID should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.HVAC_POWER_ON.id,
                areaId = VhalAreaId.GLOBAL,
                value = true
            )
        }
    }

    @Test
    fun `writing incorrect data type should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.HVAC_POWER_ON.id,
                areaId = VhalHvacAreaId.ROW_1_LEFT.id,
                value = 1 // Expected Boolean
            )
        }
    }

    @Test
    fun `writing temperature outside configured range should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
                areaId = VhalHvacAreaId.ROW_1_LEFT.id,
                value = 35f
            )
        }
    }

    @Test
    fun `writing temperature below configured range should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
                areaId = VhalHvacAreaId.ROW_1_LEFT.id,
                value = 15f
            )
        }
    }

    @Test
    fun `writing minimum allowed temperature should succeed`() {

        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id,
            value = 16f
        )

        val result = mockVhal.getProperty(
            propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id
        )

        assertEquals(16f, result?.value)
    }

    @Test
    fun `writing maximum allowed temperature should succeed`() {

        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id,
            value = 30f
        )

        val result = mockVhal.getProperty(
            propertyId = VhalPropertyId.HVAC_TEMPERATURE_SET.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id
        )

        assertEquals(30f, result?.value)
    }

    @Test
    fun `writing unknown property ID should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = 999999,
                areaId = VhalAreaId.GLOBAL,
                value = 10f
            )
        }
    }

    @Test
    fun `writing unconfigured property should throw exception`() {

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.HVAC_TEMPERATURE_CURRENT.id,
                areaId = VhalAreaId.GLOBAL,
                value = 22f
            )
        }
    }

    @Test
    fun `accepted HVAC write updates VHAL cache and property manager`() {
        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id,
            value = true
        )

        val cachedValue = mockVhal.getProperty(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id
        )

        val managedValue = VehiclePropertyStore.manager.getProperty(
            VehiclePropertyKey.HVAC_STATE
        )

        assertEquals(true, cachedValue?.value)
        assertEquals(true, managedValue?.value)
    }

    @Test
    fun `accepted parking brake write updates VHAL cache and property manager`() {
        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.PARKING_BRAKE_ON.id,
            areaId = VhalAreaId.GLOBAL,
            value = true
        )

        val cachedValue = mockVhal.getProperty(
            propertyId = VhalPropertyId.PARKING_BRAKE_ON.id,
            areaId = VhalAreaId.GLOBAL
        )

        val managedValue = VehiclePropertyStore.manager.getProperty(
            VehiclePropertyKey.PARKING_BRAKE_ON
        )

        assertEquals(true, cachedValue?.value)
        assertEquals(true, managedValue?.value)
    }

    @Test
    fun `rejected HVAC write leaves VHAL cache and property manager unchanged`() {
        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id,
            value = true
        )

        assertThrows(IllegalArgumentException::class.java) {
            mockVhal.setPropertyValue(
                propertyId = VhalPropertyId.HVAC_POWER_ON.id,
                areaId = VhalAreaId.GLOBAL,
                value = false
            )
        }

        val cachedValue = mockVhal.getProperty(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id
        )

        val managedValue = VehiclePropertyStore.manager.getProperty(
            VehiclePropertyKey.HVAC_STATE
        )

        assertEquals(true, cachedValue?.value)
        assertEquals(true, managedValue?.value)
    }
}
