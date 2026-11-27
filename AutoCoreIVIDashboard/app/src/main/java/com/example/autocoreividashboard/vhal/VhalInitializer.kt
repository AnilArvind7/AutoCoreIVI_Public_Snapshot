package com.example.autocoreividashboard.vhal

import android.util.Log
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehiclePropertyStore

object VhalInitializer {

    private const val TAG = "AutoCoreVHAL"

    fun initialize(): Boolean {

        Log.d(TAG, "Initializing VHAL configuration...")

        // Validate property configurations
        val configErrors = VhalPropertyConfigs.validateAll()

        if (configErrors.isNotEmpty()) {
            configErrors.forEach {
                Log.e(TAG, "Configuration error: $it")
            }
            return false
        }

        // Validate vehicle property mappings
        val mappingErrors = VhalPropertyMappingValidator.validate()

        if (mappingErrors.isNotEmpty()) {
            mappingErrors.forEach {
                Log.e(TAG, "Mapping error: $it")
            }
            return false
        }

        // Seed the central property store with native VHAL values.
        val nativeKeys = listOf(
            VehiclePropertyKey.SPEED,
            VehiclePropertyKey.BATTERY_LEVEL
        )

        nativeKeys.forEach { key ->
            runCatching {
                VhalRuntime.propertyAdapter.getProperty(key)
            }.onSuccess { property ->
                VehiclePropertyStore.manager.setProperty(property)
                Log.d(TAG, "Seeded $key = ${property.value}")
            }.onFailure { error ->
                Log.e(TAG, "Could not seed native property: $key", error)
            }
        }

        Log.d(TAG, "VHAL configuration initialized successfully")

        return true
    }
}
