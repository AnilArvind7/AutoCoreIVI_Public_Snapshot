package com.example.autocoreividashboard.vhal

object VhalPropertyConfigValidator {

    fun validate(
        configs: List<VhalPropertyConfig>
    ): List<String> {

        val errors = mutableListOf<String>()

        // Check duplicate property IDs
        val duplicateIds = configs
            .groupBy { it.propertyId }
            .filterValues { it.size > 1 }
            .keys

        duplicateIds.forEach {
            errors.add("Duplicate property ID: $it")
        }

        configs.forEach { config ->

            // Every property must have at least one area
            if (config.areaIds.isEmpty()) {
                errors.add(
                    "Property ${config.propertyId} has no area IDs"
                )
            }

            // Area IDs must be unique within a property
            if (config.areaIds.size != config.areaIds.distinct().size) {
                errors.add(
                    "Property ${config.propertyId} has duplicate area IDs"
                )
            }

            // Validate numeric range
            if (
                config.minValue != null &&
                config.maxValue != null &&
                config.minValue > config.maxValue
            ) {
                errors.add(
                    "Invalid min/max range for ${config.propertyId}"
                )
            }
        }

        return errors
    }
}
