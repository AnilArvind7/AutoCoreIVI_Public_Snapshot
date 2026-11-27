package com.example.autocoreividashboard.data

import kotlinx.coroutines.flow.StateFlow
interface VehicleRepository {

    // Existing dashboard UI state
    val vehicleState: StateFlow<VehicleUiState>

    // Observe all vehicle properties
    val vehicleProperties:
            StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>>

    // Read an individual vehicle property
    fun getProperty(
        key: VehiclePropertyKey
    ): VehicleProperty<*>?

    fun connect()

    fun disconnect()
}
