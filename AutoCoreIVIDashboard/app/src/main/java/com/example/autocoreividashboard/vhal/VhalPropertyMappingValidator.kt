package com.example.autocoreividashboard.vhal

import com.example.autocoreividashboard.data.VehiclePropertyKey

object VhalPropertyMappingValidator {

    fun validate(): List<String> {
        val errors = mutableListOf<String>()

        VehiclePropertyKey.values().forEach { key ->

            val mapping = VhalPropertyMappings.getMapping(key)

            val config = VhalPropertyConfigs.getConfig(
                mapping.propertyId
            )

            if (config == null) {
                errors.add(
                    "Missing VHAL configuration for $key: ${mapping.propertyId}"
                )
            } else if (mapping.areaId !in config.areaIds) {
                errors.add(
                    "Area ${mapping.areaId} is not configured for $key"
                )
            }
        }

        return errors
    }
}
