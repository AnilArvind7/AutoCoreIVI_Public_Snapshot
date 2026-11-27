
package com.example.autocoreividashboard.data.source

import com.example.autocoreividashboard.data.VehicleProperty
import com.example.autocoreividashboard.data.VehiclePropertyKey
import com.example.autocoreividashboard.data.VehicleUiState
import kotlinx.coroutines.flow.StateFlow

enum class VehicleDataSourceType {
    SIMULATION,
    AAOS,
    NATIVE
}

interface VehicleDataSource {
    val sourceType: VehicleDataSourceType

    val vehicleState: StateFlow<VehicleUiState>

    val vehicleProperties:
            StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>>

    fun connect()

    fun disconnect()
}
