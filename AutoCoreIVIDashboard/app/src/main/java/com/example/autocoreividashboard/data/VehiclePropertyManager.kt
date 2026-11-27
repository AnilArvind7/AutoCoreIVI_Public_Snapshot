package com.example.autocoreividashboard.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VehiclePropertyManager {
    private val _properties =
        MutableStateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>>(
            VehiclePropertyConfig.initialProperties.associateBy { it.key }
        )

    val properties: StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>> =
        _properties.asStateFlow()

    // Read a vehicle property
    @Suppress("UNCHECKED_CAST")
    fun getProperty(
        key: VehiclePropertyKey
    ): VehicleProperty<*>? {
        return _properties.value[key]
    }

    // Write or update a vehicle property
    /*fun setProperty(property: VehicleProperty<*>) {
        require(
            VehiclePropertyType.isValid(property.key, property.value)
        ) {
            "Invalid value type for vehicle property: ${property.key}"
        }

        val updatedProperties = _properties.value.toMutableMap()
        updatedProperties[property.key] = property
        _properties.value = updatedProperties
    }*/
    fun setProperty(property: VehicleProperty<*>) {

        val value: Any = requireNotNull(property.value) {
            "Null value for vehicle property: ${property.key}"
        }

        require(
            VehiclePropertyType.isValid(property.key, value)
        ) {
            "Invalid value type for vehicle property: ${property.key}"
        }

        val updatedProperties = _properties.value.toMutableMap()
        updatedProperties[property.key] = property
        _properties.value = updatedProperties
    }

    // Observe all vehicle property updates
    fun observeProperties(): StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>> {
        return properties
    }
}
