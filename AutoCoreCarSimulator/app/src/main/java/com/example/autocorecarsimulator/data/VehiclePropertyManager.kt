package com.example.autocorecarsimulator.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VehiclePropertyManager {

    private val _properties =
        MutableStateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>>(
            VehiclePropertyConfig.initialProperties.associateBy { it.key }
        )

    val properties: StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>>
        get() = _properties

    fun getProperty(key: VehiclePropertyKey): VehicleProperty<*>? {
        return _properties.value[key]
    }

    fun <T : Any> setProperty(property: VehicleProperty<T>) {

        require(VehiclePropertyType.isValid(property.key, property.value)) {
            "Invalid value type for vehicle property: ${property.key}"
        }

        val updatedProperties = _properties.value.toMutableMap()

        updatedProperties[property.key] = property as VehicleProperty<*>

        _properties.value = updatedProperties
    }

    fun observeProperties(): StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>> {
        return _properties
    }
}
