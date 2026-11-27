package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VhalPropertyAdapterTest {

    private val mockVhal = MockVhalImpl()
    private val adapter = VhalPropertyAdapter(mockVhal)

    @Test
    fun `setting HVAC state updates VHAL and property manager`() {
        adapter.setProperty(
            key = VehiclePropertyKey.HVAC_STATE,
            value = true
        )

        val vhalValue = mockVhal.getProperty(
            propertyId = VhalPropertyId.HVAC_POWER_ON.id,
            areaId = VhalHvacAreaId.ROW_1_LEFT.id
        )

        val managedValue = VehiclePropertyStore.manager.getProperty(
            VehiclePropertyKey.HVAC_STATE
        )

        assertEquals(true, vhalValue?.value)
        assertEquals(true, managedValue?.value)
    }

    @Test
    fun `setting parking brake updates VHAL and property manager`() {
        adapter.setProperty(
            key = VehiclePropertyKey.PARKING_BRAKE_ON,
            value = true
        )

        val vhalValue = mockVhal.getProperty(
            propertyId = VhalPropertyId.PARKING_BRAKE_ON.id,
            areaId = VhalAreaId.GLOBAL
        )

        val managedValue = VehiclePropertyStore.manager.getProperty(
            VehiclePropertyKey.PARKING_BRAKE_ON
        )

        assertEquals(true, vhalValue?.value)
        assertEquals(true, managedValue?.value)
    }

    @Test
    fun `setting read only gear property throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            adapter.setProperty(
                key = VehiclePropertyKey.GEAR_SELECTION,
                value = "D"
            )
        }
    }

    @Test
    fun `getting property returns cached VHAL value`() {
        mockVhal.setPropertyValue(
            propertyId = VhalPropertyId.PARKING_BRAKE_ON.id,
            areaId = VhalAreaId.GLOBAL,
            value = true
        )

        val result = adapter.getProperty(
            VehiclePropertyKey.PARKING_BRAKE_ON
        )

        assertEquals(VehiclePropertyKey.PARKING_BRAKE_ON, result.key)
        assertEquals(true, result.value)
    }
}
