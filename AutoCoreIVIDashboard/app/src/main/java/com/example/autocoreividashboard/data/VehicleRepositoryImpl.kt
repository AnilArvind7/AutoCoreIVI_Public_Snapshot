package com.example.autocoreividashboard.data

import android.content.Context
import com.example.autocoreividashboard.data.source.AaosVehicleDataSource
import com.example.autocoreividashboard.data.source.NativeVehicleDataSource
import com.example.autocoreividashboard.data.source.SimulationVehicleDataSource
import com.example.autocoreividashboard.data.source.VehicleDataSource
import com.example.autocoreividashboard.data.source.VehicleDataSourceType
import kotlinx.coroutines.flow.StateFlow

class VehicleRepositoryImpl(
    private val dataSource: VehicleDataSource =
        SimulationVehicleDataSource()
) : VehicleRepository {

    // AAOS datasource constructor
    constructor(
        context: Context,
        sourceType: VehicleDataSourceType
    ) : this(
        dataSource = when (sourceType) {
            VehicleDataSourceType.SIMULATION ->
                SimulationVehicleDataSource()

            VehicleDataSourceType.AAOS ->
                AaosVehicleDataSource(context.applicationContext)

            VehicleDataSourceType.NATIVE ->
                NativeVehicleDataSource()
        }
    )

    override val vehicleState: StateFlow<VehicleUiState> =
        dataSource.vehicleState

    override val vehicleProperties:
            StateFlow<Map<VehiclePropertyKey, VehicleProperty<*>>> =
        dataSource.vehicleProperties

    override fun getProperty(
        key: VehiclePropertyKey
    ): VehicleProperty<*>? {
        return vehicleProperties.value[key]
    }

    override fun connect() {
        dataSource.connect()
    }

    override fun disconnect() {
        dataSource.disconnect()
    }
}
