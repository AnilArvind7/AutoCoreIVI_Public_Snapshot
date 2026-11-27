package com.example.autocorecarsimulator.data

import kotlinx.coroutines.flow.StateFlow

interface VehicleRepository {
    val vehicleState: StateFlow<VehicleUiState>

    val vehicleProperties: StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>>

    fun getProperty(key: VehiclePropertyKey): VehicleProperty<*>?

    fun connect()
    fun disconnect()

    fun updateVehicleData(data: VehicleData)
}
