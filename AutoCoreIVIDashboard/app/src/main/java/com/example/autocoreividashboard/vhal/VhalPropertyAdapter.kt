package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey

/**
 * Adapts app-level vehicle properties to the Mock VHAL API.
 *
 * The Mock VHAL remains responsible for access, area, and value validation.
 */
class VhalPropertyAdapter(
    private val mockVhal: MockVhal
) {

    fun getProperty(key: VehiclePropertyKey): VehicleProperty<*> {
        val mapping = VhalPropertyMappings.getMapping(key)

        val vhalValue = requireNotNull(
            mockVhal.getProperty(
                propertyId = mapping.propertyId.id,
                areaId = mapping.areaId
            )
        ) {
            "No VHAL value for property=$key, areaId=${mapping.areaId}"
        }

        return VhalValueConverter.convertToVehicleProperty(vhalValue)
    }

    fun setProperty(key: VehiclePropertyKey, value: Any) {
        val mapping = VhalPropertyMappings.getMapping(key)

        // Convert app units/types to the VHAL representation first.
        val converted = VhalValueConverter.convert(
            VehicleProperty(
                key = key,
                value = value,
                areaId = mapping.areaId
            )
        )

        // MockVhalImpl validates access mode, area, type, and range,
        // then propagates the accepted value to VehiclePropertyManager.
        mockVhal.setPropertyValue(
            propertyId = mapping.propertyId.id,
            areaId = mapping.areaId,
            value = converted.value
        )
    }
}
